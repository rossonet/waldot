package net.rossonet.waldot.tinkerpop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

import net.rossonet.waldot.WaldotTinkerPopPlugin;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractOpcVertex;

/**
 * Vertex that embeds Apache TinkerPop Gremlin Server for native client connections.
 * Exposes WaldOT graph via WebSocket+HTTP with GraphSON v3 and GraphBinary v1 serializers.
 * Supports standard TinkerPop clients (Console, drivers, Graph-Explorer).
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 * @since 0.4.0
 */
public class TinkerPopVertex extends AbstractOpcVertex
		implements
			AutoCloseable {
	private static final String DEFAULT_BIND_HOST = "0.0.0.0";

	private static final String DEFAULT_CATEGORY_EVENT = "TinkerPopVertex";

	private static final int DEFAULT_PORT = 1025;

	/**
	 * Registers OPC-UA properties for Gremlin Server type node.
	 * Aggiunge le proprietà Port, Bind, Status al tipo OPC-UA.
	 */
	public static void generateParameters(WaldotNamespace waldotNamespace,
			UaObjectTypeNode dockerTypeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				WaldotTinkerPopPlugin.PORT_FIELD, NodeIds.Int16);
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				WaldotTinkerPopPlugin.BIND_HOST_FIELD, NodeIds.String);
	}

	private final Logger logger = LoggerFactory.getLogger(getClass());

	// Istanza Gremlin Server embedded
	private GremlinServer server;

	public TinkerPopVertex(WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName,
			LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version,
			Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description,
				writeMask, userWriteMask, eventNotifier, version);
		addManagedProperty(WaldotTinkerPopPlugin.PORT_FIELD, NodeIds.Int16,
				Integer.class, true, DEFAULT_PORT, null, true,
				propertyKeyValues);
		addManagedProperty(WaldotTinkerPopPlugin.BIND_HOST_FIELD,
				NodeIds.String, String.class, true, DEFAULT_BIND_HOST, null,
				true, propertyKeyValues);
		start();
	}

	@Override
	public Object clone() {
		return new TinkerPopVertex(graph, getNodeContext(), getNodeId(),
				getBrowseName(), getDisplayName(), getDescription(),
				getWriteMask(), getUserWriteMask(), getEventNotifier(),
				version(), getPropertiesAsStringArray());
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	/**
	 * Starts the Gremlin Server with configured settings.
	 * Configura e avvia il server con GraphSON v3 e GraphBinary v1 serializers,
	 * bindings per graph e g, e custom OpcIoRegistry.
	 */
	public void start() {
		final Settings overriddenSettings = new Settings();
		overriddenSettings.host = getRuntimeParameter(
				WaldotTinkerPopPlugin.BIND_HOST_FIELD);
		overriddenSettings.port = getRuntimeParameter(
				WaldotTinkerPopPlugin.PORT_FIELD);
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

		this.server = new WaldotGremlinServer(overriddenSettings);
		// avvio server
		try {
			server.start();// .get();
			logger.info("Gremlin Server started: " + server.toString());
			updatePropertyIfChanged(
					WaldotTinkerPopPlugin.STATUS_FIELD.toLowerCase(),
					WaldotTinkerPopPlugin.STATUS_RUNNING);
			sendLogEvent(DEFAULT_CATEGORY_EVENT,
					"Gremlin Server started: " + server.toString(), 0);
		} catch (final Exception e) {
			logger.error("Failed to start Gremlin Server", e);
			updatePropertyIfChanged(
					WaldotTinkerPopPlugin.STATUS_FIELD.toLowerCase(),
					WaldotTinkerPopPlugin.STATUS_FAILED);
			sendLogEvent(DEFAULT_CATEGORY_EVENT,
					"Failed to start Gremlin Server: " + e.getMessage(), 100);
		}
	}

	public void stop() {
		stop(false);
	}

	public void stop(boolean get) {
		if (server != null) {
			try {
				final CompletableFuture<Void> f = server.stop();
				if (get) {
					f.get();
				}
				server = null;
				updatePropertyIfChanged(
						WaldotTinkerPopPlugin.STATUS_FIELD.toLowerCase(),
						WaldotTinkerPopPlugin.STATUS_STOPPED);
				logger.info("Gremlin Server stopped");
			} catch (final Exception e) {
				logger.error("Failed to stop Gremlin Server", e);
				updatePropertyIfChanged(
						WaldotTinkerPopPlugin.STATUS_FIELD.toLowerCase(),
						WaldotTinkerPopPlugin.STATUS_FAILED);
			}
		}

	}
}
