package net.rossonet.waldot.client.v2;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemServiceOperationResult;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.client.api.WaldotClientException;
import net.rossonet.waldot.client.api.WaldotClientListener;
import net.rossonet.waldot.client.api.subscription.DataChangeListener;
import net.rossonet.waldot.client.api.subscription.DataItemOptions;
import net.rossonet.waldot.client.api.subscription.EventItemOptions;
import net.rossonet.waldot.client.api.subscription.MonitoredDataItem;
import net.rossonet.waldot.client.api.subscription.MonitoredEventItem;
import net.rossonet.waldot.client.api.subscription.MonitoredItemHandle;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroup;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupListener;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupOptions;
import net.rossonet.waldot.client.api.subscription.WaldotEventListener;

/**
 * A subscription group of the client v2: an {@link OpcUaSubscription} bound
 * to the current connection, created again with all its items when the
 * connection changes or the transfer to a new session fails.
 */
final class SubscriptionGroupV2 implements SubscriptionGroup {

	private final class MiloListener implements OpcUaSubscription.SubscriptionListener {

		@Override
		public void onKeepAliveReceived(final OpcUaSubscription subscription) {
			notifyListeners(l -> l.onKeepAlive(SubscriptionGroupV2.this));
		}

		@Override
		public void onNotificationDataLost(final OpcUaSubscription subscription) {
			notifyListeners(l -> l.onNotificationDataLost(SubscriptionGroupV2.this));
		}

		@Override
		public void onStatusChanged(final OpcUaSubscription subscription, final StatusCode status) {
			notifyListeners(l -> l.onStatusChanged(SubscriptionGroupV2.this, status));
		}

		@Override
		public void onTransferFailed(final OpcUaSubscription subscription, final StatusCode status) {
			logger.info("transfer of subscription group '{}' failed ({}), creating it again", name, status);
			notifyListeners(l -> l.onTransferFailed(SubscriptionGroupV2.this, status));
			owner.execute(() -> recreate(subscription));
		}

		@Override
		public void onWatchdogTimerElapsed(final OpcUaSubscription subscription) {
			notifyListeners(l -> l.onWatchdogTimerElapsed(SubscriptionGroupV2.this));
		}
	}

	private interface Notification {
		void send(SubscriptionGroupListener listener);
	}

	private static final Logger logger = LoggerFactory.getLogger(SubscriptionGroupV2.class);

	private static void checkResults(final List<MonitoredItemServiceOperationResult> results, final String operation) {
		for (final MonitoredItemServiceOperationResult result : results) {
			if (result.serviceResult() != null && result.serviceResult().isBad()) {
				throw new WaldotClientException(operation + " failed: " + result.serviceResult(),
						result.serviceResult(), null);
			}
			if (result.operationResult().isPresent() && result.operationResult().get().isBad()) {
				throw new WaldotClientException(operation + " failed: " + result.operationResult().get(),
						result.operationResult().get(), null);
			}
		}
	}

	private volatile OpcUaClient boundClient;
	private volatile boolean closed;
	private boolean everBound;
	private int retryAttempt;
	private ScheduledFuture<?> retryTask;
	private final List<AbstractMonitoredItemV2> items = new CopyOnWriteArrayList<>();
	private final List<SubscriptionGroupListener> listeners = new CopyOnWriteArrayList<>();
	private final String name;
	private volatile SubscriptionGroupOptions options;
	private final WaldotClientImplV2 owner;
	private final SubscriptionClientV2 parent;
	private volatile OpcUaSubscription subscription;

	SubscriptionGroupV2(final WaldotClientImplV2 owner, final SubscriptionClientV2 parent, final String name,
			final SubscriptionGroupOptions options) {
		this.owner = owner;
		this.parent = parent;
		this.name = name;
		this.options = options;
	}

	private <T extends AbstractMonitoredItemV2> T add(final T item) {
		checkOpen();
		synchronized (this) {
			items.add(item);
			final OpcUaSubscription current = subscription;
			if (current != null) {
				final OpcUaMonitoredItem monitored = item.newMonitoredItem();
				current.addMonitoredItem(monitored);
				try {
					current.synchronizeMonitoredItems();
				} catch (final MonitoredItemSynchronizationException e) {
					logger.debug("synchronization of group '{}' reported errors: {}", name, e.getMessage());
				}
				final Optional<StatusCode> created = monitored.getCreateResult();
				if (created.isEmpty() || created.get().isBad()) {
					current.removeMonitoredItem(monitored);
					items.remove(item);
					item.detach();
					final StatusCode status = created.orElse(StatusCode.BAD);
					throw new WaldotClientException("monitoring of " + item.nodeId() + " failed: " + status, status,
							null);
				}
			}
		}
		return item;
	}

	@Override
	public void addListener(final SubscriptionGroupListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	/** Creates the subscription and its items on a connection. */
	synchronized void bind(final OpcUaClient client) {
		if (closed || client == boundClient && subscription != null) {
			return;
		}
		final boolean recreated = everBound;
		final OpcUaSubscription created = newSubscription(client);
		try {
			created.create();
			if (!options.publishingEnabled()) {
				created.setPublishingMode(false);
			}
		} catch (final UaException e) {
			throw WaldotClientException.of("creation of subscription group '" + name + "' failed", e);
		}
		for (final AbstractMonitoredItemV2 item : items) {
			created.addMonitoredItem(item.newMonitoredItem());
		}
		try {
			created.synchronizeMonitoredItems();
		} catch (final MonitoredItemSynchronizationException e) {
			logger.warn("some items of group '{}' were not created: {}", name, e.getMessage());
		}
		subscription = created;
		boundClient = client;
		everBound = true;
		if (recreated) {
			notifyRecreated();
		}
		scheduleRetryIfNeeded();
	}

	private void cancelRetry() {
		final ScheduledFuture<?> task = retryTask;
		retryTask = null;
		retryAttempt = 0;
		if (task != null) {
			task.cancel(false);
		}
	}

	/**
	 * Creates again the items refused by the server (e.g. nodes not yet
	 * created after a restart of the server).
	 */
	private synchronized void retryFailedItems() {
		retryTask = null;
		final OpcUaSubscription current = subscription;
		if (closed || current == null) {
			return;
		}
		for (final AbstractMonitoredItemV2 item : items) {
			if (item.creationFailed()) {
				current.removeMonitoredItem(item.monitoredItem());
				current.addMonitoredItem(item.newMonitoredItem());
			}
		}
		try {
			current.synchronizeMonitoredItems();
		} catch (final MonitoredItemSynchronizationException e) {
			logger.debug("items of group '{}' still refused: {}", name, e.getMessage());
		}
		scheduleRetryIfNeeded();
	}

	private synchronized void scheduleRetryIfNeeded() {
		if (closed || subscription == null || items.stream().noneMatch(AbstractMonitoredItemV2::creationFailed)) {
			retryAttempt = 0;
			return;
		}
		if (retryTask != null && !retryTask.isDone() || owner.scheduler().isShutdown()) {
			return;
		}
		final Duration delay = owner.backoff().delay(++retryAttempt);
		logger.info("some items of group '{}' were refused by the server, new attempt in {} ms", name,
				delay.toMillis());
		retryTask = owner.scheduler().schedule(() -> {
			try {
				retryFailedItems();
			} catch (final RuntimeException e) {
				logger.warn("retry of the items of group '{}' failed: {}", name, e.getMessage());
			}
		}, delay.toMillis(), TimeUnit.MILLISECONDS);
	}

	private void checkOpen() {
		if (closed) {
			throw new WaldotClientException("subscription group '" + name + "' closed");
		}
	}

	@Override
	public void close() {
		final OpcUaSubscription current;
		synchronized (this) {
			if (closed) {
				return;
			}
			closed = true;
			cancelRetry();
			current = subscription;
			subscription = null;
			boundClient = null;
			for (final AbstractMonitoredItemV2 item : items) {
				item.detach();
			}
			items.clear();
		}
		parent.removed(this);
		if (current != null) {
			try {
				current.delete();
			} catch (final Exception e) {
				logger.debug("error deleting subscription group '{}': {}", name, e.getMessage());
			}
		}
	}

	@Override
	public List<MonitoredDataItem> dataItems() {
		final List<MonitoredDataItem> result = new ArrayList<>();
		for (final AbstractMonitoredItemV2 item : items) {
			if (item instanceof MonitoredDataItem data) {
				result.add(data);
			}
		}
		return result;
	}

	@Override
	public List<MonitoredEventItem> eventItems() {
		final List<MonitoredEventItem> result = new ArrayList<>();
		for (final AbstractMonitoredItemV2 item : items) {
			if (item instanceof MonitoredEventItem event) {
				result.add(event);
			}
		}
		return result;
	}

	@Override
	public boolean isActive() {
		return !closed;
	}

	@Override
	public boolean isPublishingEnabled() {
		final OpcUaSubscription current = subscription;
		return current == null ? options.publishingEnabled()
				: current.isPublishingEnabled().orElse(options.publishingEnabled());
	}

	/** Applies the new parameters of an item to the server. */
	synchronized void modifyItem(final AbstractMonitoredItemV2 item, final MonitoringMode previousMode) {
		final OpcUaSubscription current = subscription;
		final OpcUaMonitoredItem monitored = item.monitoredItem();
		if (current == null || monitored == null) {
			return;
		}
		checkResults(current.modifyMonitoredItems(), "modify of " + item.nodeId());
		if (item.requestedMode() != previousMode) {
			checkResults(current.setMonitoringMode(item.requestedMode(), List.of(monitored)),
					"monitoring mode of " + item.nodeId());
		}
	}

	@Override
	public void modify(final SubscriptionGroupOptions newOptions) {
		checkOpen();
		final SubscriptionGroupOptions previous;
		final OpcUaSubscription current;
		synchronized (this) {
			previous = options;
			options = newOptions;
			current = subscription;
			if (current != null) {
				applyOptions(current, newOptions);
				try {
					current.modify();
					if (newOptions.publishingEnabled() != previous.publishingEnabled()) {
						current.setPublishingMode(newOptions.publishingEnabled());
					}
				} catch (final UaException e) {
					throw WaldotClientException.of("modify of subscription group '" + name + "' failed", e);
				}
			}
		}
	}

	private static void applyOptions(final OpcUaSubscription subscription, final SubscriptionGroupOptions options) {
		subscription.setPublishingInterval(options.publishingInterval());
		if (options.maxKeepAliveCount() > 0) {
			subscription.setMaxKeepAliveCount(uint(options.maxKeepAliveCount()));
		}
		if (options.lifetimeCount() > 0) {
			subscription.setLifetimeCount(uint(options.lifetimeCount()));
		}
		subscription.setMaxNotificationsPerPublish(uint(options.maxNotificationsPerPublish()));
		subscription.setPriority(ubyte(options.priority()));
	}

	@Override
	public String name() {
		return name;
	}

	private OpcUaSubscription newSubscription(final OpcUaClient client) {
		final OpcUaSubscription created = new OpcUaSubscription(client, options.publishingInterval());
		applyOptions(created, options);
		created.setSubscriptionListener(new MiloListener());
		return created;
	}

	private void notifyListeners(final Notification notification) {
		for (final SubscriptionGroupListener listener : listeners) {
			try {
				notification.send(listener);
			} catch (final RuntimeException e) {
				logger.warn("subscription group listener error: {}", e.getMessage());
			}
		}
	}

	private void notifyRecreated() {
		notifyListeners(l -> l.onRecreated(this));
		for (final WaldotClientListener listener : owner.listeners()) {
			try {
				listener.onSubscriptionGroupRecreated(name);
			} catch (final RuntimeException e) {
				logger.warn("client listener error: {}", e.getMessage());
			}
		}
	}

	@Override
	public SubscriptionGroupOptions options() {
		return options;
	}

	private synchronized void recreate(final OpcUaSubscription failed) {
		if (closed || subscription != failed) {
			return;
		}
		try {
			// the subscription was reset by Milo: the items are created again
			applyOptions(failed, options);
			failed.create();
			if (!options.publishingEnabled()) {
				failed.setPublishingMode(false);
			}
			try {
				failed.synchronizeMonitoredItems();
			} catch (final MonitoredItemSynchronizationException e) {
				logger.warn("some items of group '{}' were not created again: {}", name, e.getMessage());
			}
			notifyRecreated();
			scheduleRetryIfNeeded();
		} catch (final UaException e) {
			logger.error("subscription group '{}' cannot be created again: {}", name, e.getMessage());
		}
	}

	/** Removes an item from the group and from the server. */
	synchronized void remove(final AbstractMonitoredItemV2 item) {
		if (!items.remove(item)) {
			return;
		}
		final OpcUaSubscription current = subscription;
		final OpcUaMonitoredItem monitored = item.monitoredItem();
		item.detach();
		if (current != null && monitored != null) {
			current.removeMonitoredItem(monitored);
			try {
				current.synchronizeMonitoredItems();
			} catch (final MonitoredItemSynchronizationException e) {
				logger.debug("error deleting item {}: {}", item.nodeId(), e.getMessage());
			}
		}
	}

	@Override
	public void removeListener(final SubscriptionGroupListener listener) {
		listeners.remove(listener);
	}

	@Override
	public Optional<Double> revisedPublishingInterval() {
		final OpcUaSubscription current = subscription;
		return current == null ? Optional.empty() : current.getRevisedPublishingInterval();
	}

	@Override
	public void setMonitoringMode(final MonitoringMode mode, final List<? extends MonitoredItemHandle> handles) {
		checkOpen();
		synchronized (this) {
			final List<OpcUaMonitoredItem> monitored = new ArrayList<>();
			for (final MonitoredItemHandle handle : handles) {
				if (!(handle instanceof AbstractMonitoredItemV2 item) || item.group() != this) {
					throw new IllegalArgumentException("item " + handle + " does not belong to group " + name);
				}
				item.requestedMode(mode);
				if (item.monitoredItem() != null) {
					monitored.add(item.monitoredItem());
				}
			}
			final OpcUaSubscription current = subscription;
			if (current != null && !monitored.isEmpty()) {
				checkResults(current.setMonitoringMode(mode, monitored), "monitoring mode");
			}
		}
	}

	@Override
	public void setPublishingEnabled(final boolean enabled) {
		modify(new SubscriptionGroupOptions(options.publishingInterval(), options.maxKeepAliveCount(),
				options.lifetimeCount(), options.maxNotificationsPerPublish(), options.priority(), enabled));
	}

	@Override
	public MonitoredDataItem subscribeData(final NodeId nodeId, final DataItemOptions itemOptions,
			final DataChangeListener listener) {
		return add(new DataItemV2(this, nodeId,
				itemOptions == null ? DataItemOptions.sampling(owner.getConfiguration().getDefaultSamplingInterval())
						: itemOptions,
				listener));
	}

	@Override
	public MonitoredEventItem subscribeEvents(final NodeId nodeId, final EventItemOptions itemOptions,
			final WaldotEventListener listener) {
		return add(new EventItemV2(this, nodeId, itemOptions == null ? EventItemOptions.defaults() : itemOptions,
				listener));
	}

	@Override
	public Optional<UInteger> subscriptionId() {
		final OpcUaSubscription current = subscription;
		return current == null ? Optional.empty() : current.getSubscriptionId();
	}

	@Override
	public String toString() {
		return "SubscriptionGroup[" + name + ", items=" + items.size() + ", " + options + "]";
	}

	/** Forgets the subscription of a closed connection (kept for a new bind). */
	synchronized void unbind() {
		cancelRetry();
		final OpcUaSubscription current = subscription;
		subscription = null;
		boundClient = null;
		for (final AbstractMonitoredItemV2 item : items) {
			item.detach();
		}
		if (current != null) {
			current.reset();
		}
	}
}
