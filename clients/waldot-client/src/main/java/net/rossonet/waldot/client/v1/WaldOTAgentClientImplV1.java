package net.rossonet.waldot.client.v1;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.OpcUaSession;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.client.api.WaldOTAgentClient;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.api.WaldotClientListener;
import net.rossonet.waldot.client.support.BaseOperationCalls;
import net.rossonet.waldot.client.support.OpcClientConfigurator;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.ThreadHelper;

/**
 * First version of the agent client: a control thread checks the session
 * every {@link WaldOTAgentClient#CONTROL_THREAD_SLEEP_TIME_MSEC} and
 * rebuilds the connection when it is faulted.
 */
public class WaldOTAgentClientImplV1 implements WaldOTAgentClient {

	private final static Logger logger = LoggerFactory.getLogger(WaldOTAgentClient.class);
	public static String SELFSIGNED_CERTIFICATE_ALIAS = OpcClientConfigurator.SELFSIGNED_CERTIFICATE_ALIAS;
	public static String SIGNED_CERTIFICATE_ALIAS = OpcClientConfigurator.SIGNED_CERTIFICATE_ALIAS;

	private volatile Status _status = Status.INIT;
	private volatile boolean activeConnectionRequest = false;
	private volatile OpcUaClient client = null;
	private int clientFaultCounter;

	private final WaldOTAgentClientConfiguration configuration;
	private final OpcClientConfigurator configurator;
	private final Thread controlThread = Thread.ofVirtual().unstarted(() -> {
		Thread.currentThread().setName("waldot-client");
		Thread.currentThread().setPriority(CONTROL_THREAD_PRIORITY);
		logger.info("Control thread started");
		while (!_status.equals(Status.CLOSED)) {
			try {
				periodicalCheck();
				Thread.sleep(CONTROL_THREAD_SLEEP_TIME_MSEC);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} catch (final Throwable t) {
				logger.warn("control thread error {}", LogHelper.stackTraceToString(t, 5));
			}
		}
		logger.info("Control thread terminated");
	});

	private ByteString lastNonce = null;

	private final List<WaldotClientListener> listeners = new CopyOnWriteArrayList<>();

	public WaldOTAgentClientImplV1(final WaldOTAgentClientConfiguration configuration) {
		this.configuration = configuration;
		this.configurator = new OpcClientConfigurator(configuration);
	}

	@Override
	public void addListener(final WaldotClientListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	public void changeStatus(final Status newStatus) {
		final Status previous = _status;
		_status = newStatus;
		for (final WaldotClientListener listener : listeners) {
			try {
				listener.onStatusChanged(previous, newStatus);
			} catch (final RuntimeException e) {
				logger.warn("status listener error: {}", e.getMessage());
			}
		}
	}

	private boolean checkClientConnected(final OpcUaClient clientOpc) throws Exception {
		return clientOpc != null && getClientSessionWithTimeout(clientOpc).getServerNonce() != null;
	}

	private void checkFaultCounter() {
		clientFaultCounter++;
		logger.warn("Control cycle faulted, counter: {}", clientFaultCounter);
		if (!configuration.isIgnoreServiceFault() && clientFaultCounter > configuration.getMaxClientFaults()) {
			logger.error("Client faulted after {} attempts", clientFaultCounter);
			changeStatus(Status.FAULTED);
		}
	}

	private void cleanConnectionObjects() throws UaException {
		activeConnectionRequest = false;
		if (client != null) {
			client.disconnect();
			logger.info("Disconnected from server");
		}
	}

	@Override
	public void close() throws Exception {
		stopConnectionProcedure();
		changeStatus(Status.CLOSED);
		controlThread.interrupt();
	}

	@Override
	public String[] createEdge(final String label, final String sourceNodeId, final String destinationNodeId,
			final String[] keyValues) throws UaException {
		logger.info("Creating edge with label: {}, sourceNodeId: {}, destinationNodeId: {}, keyValues: {}", label,
				sourceNodeId, destinationNodeId, String.join(",", keyValues));
		return BaseOperationCalls.createEdge(client, label, sourceNodeId, destinationNodeId, keyValues);
	}

	private OpcUaClient createOpcClient(final OpcUaClientConfig actualConfiguration) throws UaException {
		final OpcUaClient createdClient = OpcUaClient.create(actualConfiguration);
		createdClient.addFaultListener(this);
		createdClient.connect();
		return createdClient;
	}

	@Override
	public String[] createVertex(final String id, final String label, final String type, final String[] keyValues)
			throws UaException {
		logger.info("Creating vertex with id: {}, label: {}, type: {}, keyValues: {}", id, label, type,
				String.join(",", keyValues));
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

	private void doActionClientStart() {
		try {
			if (activeConnectionRequest) {
				if (checkClientConnected(client)) {
					changeStatus(Status.RUNNING);
					logger.info("Connected to server");
				} else {
					client = createOpcClient(configurator.build());
					// connect() is synchronous: if the session is already available the client
					// can be considered running without waiting for the next control cycle
					if (checkClientConnected(client)) {
						changeStatus(Status.RUNNING);
						logger.info("Connected to server");
					} else {
						logger.info("Waiting for connection to server...");
					}
				}
			}
		} catch (final Exception e) {
			logger.error("Error during control thread check: {}", LogHelper.stackTraceToString(e, 5));
			changeStatus(Status.FAULTED);
		}
	}

	private void doConnectionCheck() {
		try {
			if (checkClientConnected(client)) {
				lastNonce = getClientSessionWithTimeout(client).getServerNonce();
				if (clientFaultCounter > 0) {
					logger.info("Client reconnected after {} control cycle in faulted _status", clientFaultCounter);
					clientFaultCounter = 0;
				}
			} else {
				logger.warn("Client is not connected, trying to reconnect...");
				checkFaultCounter();
			}
		} catch (final Exception e) {
			logger.error("Error during control thread check: {}", LogHelper.stackTraceToString(e, 5));
			changeStatus(Status.FAULTED);
		}
	}

	private OpcUaSession getClientSessionWithTimeout(final OpcUaClient clientOpc) throws Exception {
		final Callable<OpcUaSession> callable = new Callable<OpcUaSession>() {
			@Override
			public OpcUaSession call() throws Exception {
				final OpcUaSession session = clientOpc.getSession();
				return Optional.ofNullable(session).orElseThrow(() -> new UaException(-1, "No session"));
			}
		};
		ThreadHelper.runWithTimeout(callable, TIMEOUT_GET_SESSION_SEC, TimeUnit.SECONDS);
		return callable.call();
	}

	@Override
	public WaldOTAgentClientConfiguration getConfiguration() {
		return configuration;
	}

	public ByteString getLastNonce() {
		return lastNonce;
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
		return _status;
	}

	@Override
	public void onServiceFault(final ServiceFault serviceFault) {
		logger.error("Service fault received: {}", serviceFault);
		for (final WaldotClientListener listener : listeners) {
			try {
				listener.onServiceFault(serviceFault);
			} catch (final RuntimeException e) {
				logger.warn("service fault listener error: {}", e.getMessage());
			}
		}
		changeStatus(Status.FAULTED);
	}

	private void periodicalCheck() {
		switch (_status) {
		case RUNNING:
			doConnectionCheck();
			break;
		case CLOSED:
			logger.info("Client is closed, the control thread will terminate");
			break;
		case FAULTED:
			logger.error("Client is in faulted _status, resetting connection");
			try {
				cleanConnectionObjects();
			} catch (final UaException e) {
				logger.error("Error during cleaning connection objects: {}", LogHelper.stackTraceToString(e, 5));
			}
			activeConnectionRequest = true;
			changeStatus(Status.INIT);
			break;
		case INIT:
			doActionClientStart();
			logger.info("Client is initializing");
			break;
		case STOPPED:
			logger.trace("Client is stopped");
			break;
		default:
			break;
		}
	}

	@Override
	public void removeListener(final WaldotClientListener listener) {
		listeners.remove(listener);
	}

	@Override
	public List<String> runExpression(final String expression) throws UaException {
		return BaseOperationCalls.runExpression(client, expression);
	}

	@Override
	public CompletableFuture<WaldOTAgentClient> startConnectionProcedure() {
		// the flag must be set before starting the control thread, otherwise the first
		// control cycle could be skipped
		activeConnectionRequest = true;
		if (_status == Status.STOPPED) {
			changeStatus(Status.INIT);
		}
		if (controlThread.getState() == Thread.State.NEW) {
			controlThread.start();
		}
		return CompletableFuture.completedFuture(this);

	}

	@Override
	public CompletableFuture<WaldOTAgentClient> stopConnectionProcedure() {
		changeStatus(Status.STOPPED);
		try {
			cleanConnectionObjects();
		} catch (final UaException e) {
			logger.error("Error during cleaning connection objects: {}", LogHelper.stackTraceToString(e, 5));
		}
		return CompletableFuture.completedFuture(this);
	}

}
