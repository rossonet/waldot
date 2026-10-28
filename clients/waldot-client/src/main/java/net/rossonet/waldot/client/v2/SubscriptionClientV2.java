package net.rossonet.waldot.client.v2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.client.api.subscription.DataChangeListener;
import net.rossonet.waldot.client.api.subscription.DataItemOptions;
import net.rossonet.waldot.client.api.subscription.EventItemOptions;
import net.rossonet.waldot.client.api.subscription.MonitoredDataItem;
import net.rossonet.waldot.client.api.subscription.MonitoredEventItem;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroup;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupOptions;
import net.rossonet.waldot.client.api.subscription.WaldotEventListener;
import net.rossonet.waldot.client.api.subscription.WaldotSubscriptionClient;

/** Subscription groups of the client v2. */
final class SubscriptionClientV2 implements WaldotSubscriptionClient {

	private final Map<String, SubscriptionGroupV2> groups = new LinkedHashMap<>();
	private final WaldotClientImplV2 owner;

	SubscriptionClientV2(final WaldotClientImplV2 owner) {
		this.owner = owner;
	}

	/** Binds every group to a new connection. */
	void bindAll(final OpcUaClient client) {
		for (final SubscriptionGroupV2 group : snapshot()) {
			owner.execute(() -> group.bind(client));
		}
	}

	void closeAll() {
		for (final SubscriptionGroupV2 group : snapshot()) {
			group.close();
		}
	}

	@Override
	public SubscriptionGroup createGroup(final String name, final SubscriptionGroupOptions options) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("group name is empty");
		}
		final SubscriptionGroupV2 group;
		synchronized (groups) {
			if (groups.containsKey(name)) {
				throw new IllegalArgumentException("group '" + name + "' already exists");
			}
			group = new SubscriptionGroupV2(owner, this, name,
					options == null ? SubscriptionGroupOptions
							.withPublishingInterval(owner.getConfiguration().getDefaultPublishingInterval()) : options);
			groups.put(name, group);
		}
		final OpcUaClient client = owner.getOpcUaClient();
		if (client != null && owner.isConnected()) {
			try {
				group.bind(client);
			} catch (final RuntimeException e) {
				removed(group);
				throw e;
			}
		}
		return group;
	}

	@Override
	public SubscriptionGroup defaultGroup() {
		synchronized (groups) {
			final SubscriptionGroupV2 group = groups.get(DEFAULT_GROUP);
			if (group != null) {
				return group;
			}
		}
		try {
			return createGroup(DEFAULT_GROUP, null);
		} catch (final IllegalArgumentException e) {
			// created by another thread
			return group(DEFAULT_GROUP).orElseThrow(() -> e);
		}
	}

	@Override
	public Optional<SubscriptionGroup> group(final String name) {
		synchronized (groups) {
			return Optional.ofNullable(groups.get(name));
		}
	}

	@Override
	public List<SubscriptionGroup> groups() {
		return new ArrayList<>(snapshot());
	}

	void removed(final SubscriptionGroupV2 group) {
		synchronized (groups) {
			groups.remove(group.name(), group);
		}
	}

	private List<SubscriptionGroupV2> snapshot() {
		synchronized (groups) {
			return new ArrayList<>(groups.values());
		}
	}

	@Override
	public MonitoredEventItem subscribeEvents(final NodeId nodeId, final EventItemOptions options,
			final WaldotEventListener listener) {
		return defaultGroup().subscribeEvents(nodeId, options, listener);
	}

	@Override
	public MonitoredDataItem subscribeProperty(final NodeId element, final String key, final DataItemOptions options,
			final DataChangeListener listener) {
		return subscribeValue(owner.graph().propertyNodeId(element, key), options, listener);
	}

	@Override
	public MonitoredDataItem subscribeValue(final NodeId nodeId, final DataItemOptions options,
			final DataChangeListener listener) {
		return defaultGroup().subscribeData(nodeId,
				options == null ? DataItemOptions.sampling(owner.getConfiguration().getDefaultSamplingInterval())
						: options,
				listener);
	}

	void unbindAll() {
		for (final SubscriptionGroupV2 group : snapshot()) {
			group.unbind();
		}
	}
}
