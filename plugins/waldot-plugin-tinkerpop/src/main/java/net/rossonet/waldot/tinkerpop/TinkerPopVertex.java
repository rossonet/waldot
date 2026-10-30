package net.rossonet.waldot.tinkerpop;

import static net.rossonet.waldot.tinkerpop.TinkerPopConstants.BIND_HOST_FIELD;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants.BIND_HOST_FIELD_DESCRIPTION;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants.DEFAULT_PORT;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants.PORT_FIELD;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants.PORT_FIELD_DESCRIPTION;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants.START_TIMEOUT_SEC;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants.STATUS_FAILED;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants.STATUS_FIELD;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants.STATUS_FIELD_DESCRIPTION;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants.STOP_TIMEOUT_SEC;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants._DEFAULT_BIND_HOST;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants._STATUS_RUNNING;
import static net.rossonet.waldot.tinkerpop.TinkerPopConstants._STATUS_STOPPED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.tinkerpop.gremlin.server.GraphManager;
import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.server.Settings.SerializerSettings;
import org.apache.tinkerpop.gremlin.server.channel.WsAndHttpChannelizer;
import org.apache.tinkerpop.gremlin.util.ser.GraphBinaryMessageSerializerV1;
import org.apache.tinkerpop.gremlin.util.ser.GraphSONMessageSerializerV3;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractOpcVertex;

/**
 * Vertex that embeds Apache TinkerPop Gremlin Server for native client connections.
 * Exposes WaldOT graph via WebSocket+HTTP with GraphSON v3 and GraphBinary v1 serializers.
 * Supports standard TinkerPop clients (Console, drivers, Graph-Explorer).
 * <p>
 * A change of Port or Bind restarts the server on a separate thread: the
 * change can be written by a client of the server itself, whose worker thread
 * must not wait for the stop of its own pool.
 * </p>
 *
 * @author Andrea Ambrosini - Zona Autonoma srl
 * @since 0.4.0
 */
public class TinkerPopVertex extends AbstractOpcVertex
		implements
			AutoCloseable {
	private static final String DEFAULT_CATEGORY_EVENT = "TinkerPopVertex";

	/**
	 * Registers OPC-UA properties for Gremlin Server type node.
	 * Aggiunge le proprietà Port, Bind, Status al tipo OPC-UA.
	 */
	public static void generateParameters(WaldotNamespace waldotNamespace,
			UaObjectTypeNode dockerTypeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				PORT_FIELD, NodeIds.Int32, PORT_FIELD_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				BIND_HOST_FIELD, NodeIds.String, BIND_HOST_FIELD_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				STATUS_FIELD.toLowerCase(), NodeIds.String,
				STATUS_FIELD_DESCRIPTION);
	}

	private volatile String bindHost = _DEFAULT_BIND_HOST;

	/** true once the vertex has been closed: no more (re)starts */
	private volatile boolean closed = false;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private volatile int port = DEFAULT_PORT;

	/** true while a restart is scheduled and not yet begun */
	private final AtomicBoolean restartPending = new AtomicBoolean(false);

	// Istanza Gremlin Server embedded
	private GremlinServer server;

	/** Guards {@link #server}: start, stop and restart are serialized */
	private final Object serverLock = new Object();

	/** true once the constructor has started the server the first time */
	private volatile boolean started = false;

	public TinkerPopVertex(WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName,
			LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version,
			Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description,
				writeMask, userWriteMask, eventNotifier, version);
		addManagedProperty(PORT_FIELD, NodeIds.Int32, Integer.class, true,
				DEFAULT_PORT, (label, oldValue, newValue) -> {
					if (newValue == null || newValue < 1
							|| newValue > 65535) {
						throw new PropertyChangeException(
								"the port must be between 1 and 65535: "
										+ newValue);
					}
					port = newValue;
					scheduleRestart();
				}, true, propertyKeyValues);
		addManagedProperty(BIND_HOST_FIELD, NodeIds.String, String.class, true,
				_DEFAULT_BIND_HOST, (label, oldValue, newValue) -> {
					if (newValue == null || newValue.isBlank()) {
						throw new PropertyChangeException(
								"the bind address can't be empty");
					}
					bindHost = newValue;
					scheduleRestart();
				}, true, propertyKeyValues);
		// the callbacks are not called for the default values and for the
		// values replaced by the defaults
		final Integer configuredPort = getRuntimeParameter(PORT_FIELD);
		if (configuredPort != null) {
			port = configuredPort;
		}
		final String configuredBindHost = getRuntimeParameter(BIND_HOST_FIELD);
		if (configuredBindHost != null) {
			bindHost = configuredBindHost;
		}
		start();
		started = true;
	}

	@Override
	public Object clone() {
		return new TinkerPopVertex(graph, getNodeContext(), getNodeId(),
				getBrowseName(), getDisplayName(), getDescription(),
				getWriteMask(), getUserWriteMask(), getEventNotifier(),
				version(), getPropertiesAsStringArray());
	}

	/**
	 * Stops the server and waits for the release of its port. Idempotent.
	 */
	@Override
	public void close() throws Exception {
		closed = true;
		stop(true);
	}

	private void restart() {
		synchronized (serverLock) {
			// a change arriving from now on schedules a new restart
			restartPending.set(false);
			if (closed) {
				return;
			}
			stop(true);
			if (!closed) {
				start();
			}
		}
	}

	/**
	 * Restarts the server with the current Port and Bind. During the
	 * construction the values are only applied: the first start happens at the
	 * end of the constructor. The changes arriving before the restart begins
	 * are applied by a single restart.
	 */
	private void scheduleRestart() {
		if (!started || closed) {
			return;
		}
		if (restartPending.compareAndSet(false, true)) {
			Thread.ofVirtual()
					.name("gremlin-restart-"
							+ getNodeId().toParseableString())
					.start(this::restart);
		}
	}

	/**
	 * Starts the Gremlin Server with configured settings.
	 * Configura e avvia il server con GraphSON v3 e GraphBinary v1 serializers,
	 * bindings per graph e g, e custom OpcIoRegistry. Attende il bind della
	 * porta: se fallisce, il server viene fermato e lo stato diventa Failed.
	 * Does nothing if the server is already running.
	 */
	public void start() {
		synchronized (serverLock) {
			if (server != null || closed) {
				return;
			}
			GremlinServer newServer = null;
			try {
				newServer = new WaldotGremlinServer(createSettings());
				newServer.start().get(START_TIMEOUT_SEC, TimeUnit.SECONDS);
				server = newServer;
				logger.info("Gremlin Server started: " + server.toString());
				updatePropertyIfChanged(STATUS_FIELD.toLowerCase(),
						_STATUS_RUNNING);
				sendLogEvent(DEFAULT_CATEGORY_EVENT,
						"Gremlin Server started: " + server.toString(), 0);
			} catch (final Exception e) {
				if (e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
				logger.error("Failed to start Gremlin Server on " + bindHost
						+ ":" + port, e);
				if (newServer != null) {
					try {
						stopServer(newServer, true);
					} catch (final Exception stopException) {
						logger.warn("Failed to release the Gremlin Server",
								stopException);
					}
				}
				updatePropertyIfChanged(STATUS_FIELD.toLowerCase(),
						STATUS_FAILED);
				sendLogEvent(DEFAULT_CATEGORY_EVENT,
						"Failed to start Gremlin Server: " + e.getMessage(),
						100);
			}
		}
	}

	private Settings createSettings() {
		final Settings overriddenSettings = new Settings();
		overriddenSettings.host = bindHost;
		overriddenSettings.port = port;
		overriddenSettings.channelizer = WsAndHttpChannelizer.class.getName();
		overriddenSettings.graphManager = WaldotGraphManager.class.getName();
		/* bindings */
		final Map<String, Object> arg = new HashMap<>();
		arg.put("graph", getNamespace().getGremlinGraph());
		arg.put("g", getNamespace().getGremlinGraph().traversal());
		final Map<String, Object> method2arg = new HashMap<>();
		method2arg.put("bindings", arg);
		overriddenSettings.scriptEngines.get("gremlin-groovy").plugins.put(
				"org.apache.tinkerpop.gremlin.jsr223.BindingsGremlinPlugin",
				method2arg);
		overriddenSettings.scriptEngines.get("gremlin-groovy").plugins.put(
				"net.rossonet.waldot.gremlin.opcgraph.jsr223.OpcGraphGremlinPlugin",
				method2arg);
		/* === SERIALIZERS === */
		// 1) GraphSON v3 – richiesto da Graph-Explorer
		final SerializerSettings graphsonV3 = new SerializerSettings();
		graphsonV3.className = GraphSONMessageSerializerV3.class.getName();
		graphsonV3.config = Map.of("serializeResultToString", false,
				"ioRegistries",
				List.of("net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3",
						"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV2",
						"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1"));

		// 2) (facoltativo) GraphBinary v1 per client WS high-performance
		final SerializerSettings graphBinary = new SerializerSettings();
		graphBinary.className = GraphBinaryMessageSerializerV1.class.getName();
		graphBinary.config = Map.of("ioRegistries", List.of(
				"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3",
				"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV2",
				"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1"));
		overriddenSettings.serializers = new ArrayList<>();
		overriddenSettings.serializers.add(graphsonV3); // deve essere PRIMO
		overriddenSettings.serializers.add(graphBinary);
		return overriddenSettings;
	}

	public void stop() {
		stop(false);
	}

	/**
	 * Stops the server.
	 *
	 * @param get true to wait (at most {@value TinkerPopConstants#STOP_TIMEOUT_SEC}
	 *            seconds) for the end of the stop and the release of the port
	 */
	public void stop(boolean get) {
		synchronized (serverLock) {
			if (server != null) {
				try {
					stopServer(server, get);
					updatePropertyIfChanged(STATUS_FIELD.toLowerCase(),
							_STATUS_STOPPED);
					logger.info("Gremlin Server stopped");
				} catch (final Exception e) {
					if (e instanceof InterruptedException) {
						Thread.currentThread().interrupt();
					}
					logger.error("Failed to stop Gremlin Server", e);
					updatePropertyIfChanged(STATUS_FIELD.toLowerCase(),
							STATUS_FAILED);
				} finally {
					server = null;
				}
			}
		}
	}

	/**
	 * Stops a server without closing the WaldOT graph: {@code GremlinServer.stop()}
	 * closes every graph left in its graph manager, and closing the WaldOT graph
	 * closes the whole namespace.
	 */
	private void stopServer(GremlinServer gremlinServer, boolean wait)
			throws Exception {
		if (gremlinServer.getServerGremlinExecutor() != null) {
			final GraphManager graphManager = gremlinServer
					.getServerGremlinExecutor().getGraphManager();
			if (graphManager instanceof final WaldotGraphManager waldotGraphManager) {
				waldotGraphManager.detachAll();
			}
		}
		final var stopped = gremlinServer.stop();
		if (wait) {
			stopped.get(STOP_TIMEOUT_SEC, TimeUnit.SECONDS);
		}
	}
}
