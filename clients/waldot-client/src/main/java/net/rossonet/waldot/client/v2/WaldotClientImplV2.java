package net.rossonet.waldot.client.v2;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.client.api.WaldOTAgentClient;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.WaldotClientException;
import net.rossonet.waldot.client.api.WaldotClientListener;
import net.rossonet.waldot.client.api.address.AddressSpaceBrowser;
import net.rossonet.waldot.client.api.command.WaldotCommandClient;
import net.rossonet.waldot.client.api.graph.GraphSnapshotClient;
import net.rossonet.waldot.client.api.graph.WaldotGraphClient;
import net.rossonet.waldot.client.api.subscription.WaldotSubscriptionClient;
import net.rossonet.waldot.client.support.BaseOperationCalls;
import net.rossonet.waldot.client.support.OpcClientConfigurator;
import net.rossonet.waldot.client.support.ReconnectBackoff;

/**
 * Second version of the WaldOT client.
 * <p>
 * Connection: the first connection and the connections rebuilt after a
 * failure are attempted on an internal thread with exponential backoff
 * ({@code reconnectInitialDelay}, {@code reconnectMaxDelay},
 * {@code reconnectMaxAttempts}); every failure moves the status to
 * {@link Status#FAULTED} and notifies
 * {@link WaldotClientListener#onReconnectScheduled}; after
 * {@code reconnectMaxAttempts} failures the client gives up
 * ({@link Status#UNRECOVERABLE_FAULTED}). When the session of an established
 * connection is lost the status is {@link Status#RECONNECTING} while the Milo
 * session state machine restores it (transferring the subscriptions); the
 * subscription groups that cannot be transferred are created again.
 * </p>
 */
public class WaldotClientImplV2 implements WaldotClient {

	private final class SessionListener implements SessionActivityListener {

		@Override
		public void onSessionActive(final UaSession session) {
			if (status == Status.RECONNECTING && connectionRequested) {
				changeStatus(Status.RUNNING);
			}
		}

		@Override
		public void onSessionInactive(final UaSession session) {
			if (status == Status.RUNNING && connectionRequested) {
				changeStatus(Status.RECONNECTING);
			}
		}
	}

	private static final int DEFAULT_NAMESPACE_INDEX = BaseOperationCalls.WALDOT_NAMESPACE_INDEX;

	private static final Logger logger = LoggerFactory.getLogger(WaldotClientImplV2.class);

	private final AddressSpaceBrowserV2 addressSpace;
	private final ReconnectBackoff backoff;
	private volatile OpcUaClient client;
	private final CommandClientV2 commands;
	private final WaldOTAgentClientConfiguration configuration;
	private volatile OpcClientConfigurator configurator;
	private volatile CompletableFuture<WaldOTAgentClient> connectFuture = new CompletableFuture<>();
	private final Object connectionLock = new Object();
	private volatile boolean connectionRequested;
	private final AtomicInteger failedAttempts = new AtomicInteger();
	private final GraphClientV2 graph;
	private final List<WaldotClientListener> listeners = new CopyOnWriteArrayList<>();
	private volatile int namespaceIndex = DEFAULT_NAMESPACE_INDEX;
	private ScheduledFuture<?> pendingAttempt;
	private final ScheduledExecutorService scheduler;
	private final SessionListener sessionListener = new SessionListener();
	private final GraphSnapshotClientV2 snapshots;
	private volatile Status status = Status.INIT;
	private final SubscriptionClientV2 subscriptions;

	public WaldotClientImplV2(final WaldOTAgentClientConfiguration configuration) {
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.backoff = new ReconnectBackoff(Math.max(1, configuration.getReconnectInitialDelay()),
				Math.max(Math.max(1, configuration.getReconnectInitialDelay()), configuration.getReconnectMaxDelay()),
				Math.max(0, configuration.getReconnectMaxAttempts()));
		this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
			final Thread thread = new Thread(runnable, "waldot-client-v2");
			thread.setDaemon(true);
			return thread;
		});
		addressSpace = new AddressSpaceBrowserV2(this);
		commands = new CommandClientV2(this);
		graph = new GraphClientV2(this);
		snapshots = new GraphSnapshotClientV2(this);
		subscriptions = new SubscriptionClientV2(this);
	}

	@Override
	public void addListener(final WaldotClientListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	@Override
	public AddressSpaceBrowser addressSpace() {
		return addressSpace;
	}

	private void attemptConnection() {
		synchronized (connectionLock) {
			pendingAttempt = null;
			if (!connectionRequested || status == Status.CLOSED) {
				return;
			}
			OpcUaClient created = null;
			try {
				if (configurator == null) {
					configurator = new OpcClientConfigurator(configuration);
				}
				created = OpcUaClient.create(configurator.build());
				created.addFaultListener(this);
				created.addSessionActivityListener(sessionListener);
				created.connect();
				client = created;
				namespaceIndex = resolveNamespaceIndex(created);
				failedAttempts.set(0);
				commands.refresh();
				changeStatus(Status.RUNNING);
				logger.info("connected to {}", configuration.getBaseWaldOTOpcEndpoint());
				subscriptions.bindAll(created);
				connectFuture.complete(this);
			} catch (final Exception e) {
				disconnectQuietly(created);
				final int failures = failedAttempts.incrementAndGet();
				logger.warn("connection attempt {} to {} failed: {}", failures,
						configuration.getBaseWaldOTOpcEndpoint(), e.getMessage());
				if (backoff.exhausted(failures)) {
					connectionRequested = false;
					changeStatus(Status.UNRECOVERABLE_FAULTED);
					connectFuture.completeExceptionally(WaldotClientException
							.of("connection failed after " + failures + " attempts", e));
					return;
				}
				changeStatus(Status.FAULTED);
				scheduleAttempt(failures, backoff.delay(failures));
			}
		}
	}

	/** @return the reconnection policy */
	public ReconnectBackoff backoff() {
		return backoff;
	}

	AddressSpaceBrowserV2 browser() {
		return addressSpace;
	}

	CommandClientV2 commandClient() {
		return commands;
	}

	private void changeStatus(final Status newStatus) {
		final Status previous = status;
		if (previous == newStatus) {
			return;
		}
		status = newStatus;
		logger.debug("status {} -> {}", previous, newStatus);
		for (final WaldotClientListener listener : listeners) {
			try {
				listener.onStatusChanged(previous, newStatus);
			} catch (final RuntimeException e) {
				logger.warn("status listener error: {}", e.getMessage());
			}
		}
	}

	@Override
	public void close() {
		if (status == Status.CLOSED) {
			return;
		}
		stopConnectionProcedure();
		subscriptions.closeAll();
		changeStatus(Status.CLOSED);
		scheduler.shutdownNow();
	}

	@Override
	public WaldotCommandClient commands() {
		return commands;
	}

	@Override
	public WaldotClient connect(final Duration timeout) {
		final CompletableFuture<WaldOTAgentClient> future = startConnectionProcedure();
		try {
			future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
			return this;
		} catch (final TimeoutException e) {
			throw new WaldotClientException("not connected within " + timeout + " (status " + status + ")",
					new StatusCode(StatusCodes.Bad_Timeout), e);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw WaldotClientException.of("interrupted while connecting", e);
		} catch (final ExecutionException e) {
			throw WaldotClientException.of("connection failed", e.getCause());
		}
	}

	@Override
	public String[] createEdge(final String label, final String sourceNodeId, final String destinationNodeId,
			final String[] keyValues) throws UaException {
		return BaseOperationCalls.createEdge(client, label, sourceNodeId, destinationNodeId, keyValues);
	}

	@Override
	public String[] createVertex(final String id, final String label, final String type, final String[] keyValues)
			throws UaException {
		return BaseOperationCalls.createVertex(client, id, label, type, keyValues);
	}

	@Override
	public String[] deleteVertex(final int id) throws UaException {
		return BaseOperationCalls.deleteVertex(client, id);
	}

	@Override
	public String[] deleteVertex(final String id) throws UaException {
		return BaseOperationCalls.deleteVertex(client, id);
	}

	private void disconnectQuietly(final OpcUaClient opcClient) {
		if (opcClient == null) {
			return;
		}
		try {
			opcClient.removeSessionActivityListener(sessionListener);
			opcClient.removeFaultListener(this);
			opcClient.disconnect();
		} catch (final Exception e) {
			logger.debug("error disconnecting: {}", e.getMessage());
		}
	}

	/** Runs a task on the internal thread of the client. */
	void execute(final Runnable task) {
		if (!scheduler.isShutdown()) {
			scheduler.execute(() -> {
				try {
					task.run();
				} catch (final RuntimeException e) {
					logger.warn("client task error: {}", e.getMessage(), e);
				}
			});
		}
	}

	@Override
	public WaldOTAgentClientConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public OpcUaClient getOpcUaClient() {
		return client;
	}

	@Override
	public List<String> getServerInfo() throws UaException {
		return BaseOperationCalls.getServerInfo(client);
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public WaldotGraphClient graph() {
		return graph;
	}

	List<WaldotClientListener> listeners() {
		return listeners;
	}

	@Override
	public int namespaceIndex() {
		return namespaceIndex;
	}

	@Override
	public void onServiceFault(final ServiceFault serviceFault) {
		logger.warn("service fault: {}", serviceFault.getResponseHeader().getServiceResult());
		for (final WaldotClientListener listener : listeners) {
			try {
				listener.onServiceFault(serviceFault);
			} catch (final RuntimeException e) {
				logger.warn("service fault listener error: {}", e.getMessage());
			}
		}
	}

	@Override
	public void removeListener(final WaldotClientListener listener) {
		listeners.remove(listener);
	}

	/**
	 * @return the Milo client of the active connection
	 * @throws WaldotClientException if the client is not connected
	 */
	OpcUaClient requireClient() {
		final OpcUaClient current = client;
		if (current == null || status == Status.CLOSED || status == Status.STOPPED
				|| status == Status.UNRECOVERABLE_FAULTED) {
			throw new WaldotClientException("client not connected (status " + status + ")",
					new StatusCode(StatusCodes.Bad_NotConnected), null);
		}
		return current;
	}

	private int resolveNamespaceIndex(final OpcUaClient opcClient) {
		try {
			final UShort index = opcClient.readNamespaceTable().getIndex(configuration.getWaldotNamespaceUri());
			if (index != null) {
				return index.intValue();
			}
			logger.warn("namespace {} not found on the server, using index {}",
					configuration.getWaldotNamespaceUri(), DEFAULT_NAMESPACE_INDEX);
		} catch (final Exception e) {
			logger.warn("error reading the namespace table: {}", e.getMessage());
		}
		return DEFAULT_NAMESPACE_INDEX;
	}

	@Override
	public List<String> runExpression(final String expression) throws UaException {
		return BaseOperationCalls.runExpression(client, expression);
	}

	private void scheduleAttempt(final int failures, final Duration delay) {
		for (final WaldotClientListener listener : listeners) {
			try {
				listener.onReconnectScheduled(failures, delay);
			} catch (final RuntimeException e) {
				logger.warn("reconnect listener error: {}", e.getMessage());
			}
		}
		if (!scheduler.isShutdown()) {
			pendingAttempt = scheduler.schedule(this::attemptConnection, delay.toMillis(), TimeUnit.MILLISECONDS);
		}
	}

	ScheduledExecutorService scheduler() {
		return scheduler;
	}

	@Override
	public GraphSnapshotClient snapshots() {
		return snapshots;
	}

	@Override
	public CompletableFuture<WaldOTAgentClient> startConnectionProcedure() {
		synchronized (connectionLock) {
			if (status == Status.CLOSED) {
				return CompletableFuture.failedFuture(new WaldotClientException("client closed"));
			}
			if (connectionRequested) {
				return connectFuture;
			}
			if (connectFuture.isDone()) {
				connectFuture = new CompletableFuture<>();
			}
			connectionRequested = true;
			failedAttempts.set(0);
			changeStatus(Status.INIT);
			pendingAttempt = scheduler.schedule(this::attemptConnection, 0, TimeUnit.MILLISECONDS);
			return connectFuture;
		}
	}

	@Override
	public CompletableFuture<WaldOTAgentClient> stopConnectionProcedure() {
		final OpcUaClient current;
		synchronized (connectionLock) {
			connectionRequested = false;
			if (pendingAttempt != null) {
				pendingAttempt.cancel(false);
				pendingAttempt = null;
			}
			current = client;
			client = null;
			if (status != Status.CLOSED) {
				changeStatus(Status.STOPPED);
			}
			if (!connectFuture.isDone()) {
				connectFuture.completeExceptionally(new WaldotClientException("connection stopped"));
			}
		}
		subscriptions.unbindAll();
		disconnectQuietly(current);
		return CompletableFuture.completedFuture(this);
	}

	@Override
	public WaldotSubscriptionClient subscriptions() {
		return subscriptions;
	}

	@Override
	public String toString() {
		return "WaldotClientImplV2[" + configuration.getBaseWaldOTOpcEndpoint() + ", " + status + "]";
	}
}
