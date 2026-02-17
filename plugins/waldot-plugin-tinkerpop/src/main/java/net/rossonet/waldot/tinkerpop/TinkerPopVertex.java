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
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
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
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;

public class TinkerPopVertex extends AbstractOpcVertex implements AutoCloseable {
	public static void generateParameters(WaldotNamespace waldotNamespace, UaObjectTypeNode dockerTypeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode, WaldotTinkerPopPlugin.PORT_FIELD,
				NodeIds.Int16);
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode, WaldotTinkerPopPlugin.BIND_HOST_FIELD,
				NodeIds.String);
	}

	private String baseDirectory;

	private String host;
	private final QualifiedProperty<String> hostProperty;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private int port = 1025;

	private final QualifiedProperty<Integer> portProperty;

	private GremlinServer server;
	private final WaldotNamespace waldotNamespace;

	public TinkerPopVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId, QualifiedName browseName,
			LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			UByte eventNotifier, long version, Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		waldotNamespace = graph.getWaldotNamespace();
		baseDirectory = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				MiloStrategy.DIRECTORY_PARAMETER.toLowerCase());
		if (baseDirectory == null || baseDirectory.isEmpty()) {
			baseDirectory = "gremlin";
		}
		final String keyValuesPropertyPort = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotTinkerPopPlugin.PORT_FIELD.toLowerCase());
		if (keyValuesPropertyPort != null && !keyValuesPropertyPort.isEmpty()) {
			try {
				port = Integer.valueOf(keyValuesPropertyPort);
			} catch (final NumberFormatException e) {
				logger.warn("Invalid port number provided: " + keyValuesPropertyPort + ". Using default port " + port);
			}
		}
		portProperty = new QualifiedProperty<Integer>(getNamespace().getNamespaceUri(),
				WaldotTinkerPopPlugin.PORT_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				Integer.class);
		if (port == 0) {
			port = 1025;
		}
		setProperty(portProperty, port);
		host = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotTinkerPopPlugin.BIND_HOST_FIELD.toLowerCase());
		hostProperty = new QualifiedProperty<String>(getNamespace().getNamespaceUri(),
				WaldotTinkerPopPlugin.BIND_HOST_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				String.class);
		if (host == null || host.isEmpty()) {
			host = "0.0.0.0";
		}
		setProperty(hostProperty, host);
		start();
	}

	@Override
	public Object clone() {
		return new TinkerPopVertex(graph, getNodeContext(), getNodeId(), getBrowseName(), getDisplayName(),
				getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(),
				getPropertiesAsStringArray());
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	@Override
	public void notifyPropertyValueChanging(String label, DataValue value) {
		super.notifyPropertyValueChanging(label, value);
		boolean restart = false;
		if (label.equals(WaldotTinkerPopPlugin.PORT_FIELD.toLowerCase())) {
			port = Integer.valueOf(value.getValue().getValue().toString());
			setProperty(portProperty, port);
			restart = true;
		}
		if (label.equals(WaldotTinkerPopPlugin.BIND_HOST_FIELD.toLowerCase())) {
			host = value.getValue().getValue().toString();
			setProperty(hostProperty, host);
			restart = true;
		}
		if (restart) {
			stop(true);
			start();
		}
	}

	public void start() {
		final Settings overriddenSettings = new Settings();
		overriddenSettings.host = host;
		overriddenSettings.port = port;
		overriddenSettings.channelizer = WsAndHttpChannelizer.class.getName();
		overriddenSettings.graphManager = WaldotGraphManager.class.getName();
		/* bindings */
		final Map<String, Object> arg = new HashMap<>();
		arg.put("graph", waldotNamespace.getGremlinGraph());
		arg.put("g", waldotNamespace.getGremlinGraph().traversal());
		final Map<String, Object> method2arg = new HashMap<>();
		method2arg.put("bindings", arg);
		overriddenSettings.scriptEngines.get("gremlin-groovy").plugins
				.put("org.apache.tinkerpop.gremlin.jsr223.BindingsGremlinPlugin", method2arg);
		/* === SERIALIZERS === */
		// 1) GraphSON v3 – richiesto da Graph-Explorer
		final SerializerSettings graphsonV3 = new SerializerSettings();
		graphsonV3.className = GraphSONMessageSerializerV3.class.getName();
		graphsonV3.config = Map.of("serializeResultToString", false, "ioRegistries",
				List.of("net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3",
						"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV2",
						"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1"));

		// 2) (facoltativo) GraphBinary v1 per client WS high-performance
		final SerializerSettings graphBinary = new SerializerSettings();
		graphBinary.className = GraphBinaryMessageSerializerV1.class.getName();
		graphBinary.config = Map.of("ioRegistries",
				List.of("net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3",
						"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV2",
						"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1"));
		overriddenSettings.serializers = new ArrayList<>();
		overriddenSettings.serializers.add(graphsonV3); // deve essere PRIMO
		overriddenSettings.serializers.add(graphBinary);
//		overriddenSettings.serializers.add(graphsonV1);

		this.server = new WaldotGremlinServer(overriddenSettings);
		// avvio server
		try {
			server.start();// .get();
			logger.info("Gremlin Server started: " + server.toString());
		} catch (final Exception e) {
			logger.error("Failed to start Gremlin Server", e);
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
				logger.info("Gremlin Server stopped");
			} catch (final Exception e) {
				logger.error("Failed to stop Gremlin Server", e);
			}
		}
	}
}
