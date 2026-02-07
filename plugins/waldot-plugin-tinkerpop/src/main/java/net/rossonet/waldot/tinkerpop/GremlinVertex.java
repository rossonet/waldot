package net.rossonet.waldot.tinkerpop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.server.Settings.SerializerSettings;
import org.apache.tinkerpop.gremlin.server.channel.WsAndHttpChannelizer;
import org.apache.tinkerpop.gremlin.util.ser.GraphBinaryMessageSerializerV1;
import org.apache.tinkerpop.gremlin.util.ser.GraphSONMessageSerializerV1;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
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
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;

public class GremlinVertex extends AbstractOpcVertex implements AutoCloseable {
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

	public GremlinVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId, QualifiedName browseName,
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
		return new GremlinVertex(graph, getNodeContext(), getNodeId(), getBrowseName(), getDisplayName(),
				getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(),
				getPropertiesAsStringArray());
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	public void start() {
		final Settings overriddenSettings = new Settings();
		overriddenSettings.host = host;
		overriddenSettings.port = port;
		overriddenSettings.channelizer = WsAndHttpChannelizer.class.getName();
		overriddenSettings.graphManager = WaldotGraphManager.class.getName();
		final Map<String, Object> arg = new HashMap<>();
		arg.put("graph", waldotNamespace.getGremlinGraph());
		arg.put("g", waldotNamespace.getGremlinGraph().traversal());
		final Map<String, Object> method2arg = new HashMap<>();
		method2arg.put("bindings", arg);
		overriddenSettings.scriptEngines.get("gremlin-groovy").plugins
				.put("org.apache.tinkerpop.gremlin.jsr223.BindingsGremlinPlugin", method2arg);
		final SerializerSettings settingSerializerJson = new SerializerSettings();
		settingSerializerJson.className = GraphSONMessageSerializerV1.class.getName();
		settingSerializerJson.config = Collections.singletonMap("ioRegistries",
				List.of("net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1",
						"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV2",
						"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3"));
		final SerializerSettings settingSerializerBin = new SerializerSettings();
		settingSerializerBin.className = GraphBinaryMessageSerializerV1.class.getName();
		settingSerializerBin.config = new HashMap<>();
		settingSerializerBin.config.put("ioRegistries",
				List.of("net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1"));
		settingSerializerBin.config.put("custom", List.of(
				"org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;net.rossonet.waldot.gremlin.opcgraph.ser.NodeIdCustomTypeSerializer"));
		overriddenSettings.serializers = new ArrayList<>();
		overriddenSettings.serializers.add(settingSerializerBin);
		overriddenSettings.serializers.add(settingSerializerJson);
		this.server = new WaldotGremlinServer(overriddenSettings);
		try {
			server.start();// .get();
			logger.info("Gremlin Server started: " + server.toString());
		} catch (final Exception e) {
			logger.error("Failed to start Gremlin Server", e);
		}
	}

	public void stop() {
		if (server != null) {
			try {
				server.stop();// .get();
				server = null;
				logger.info("Gremlin Server stopped");
			} catch (final Exception e) {
				logger.error("Failed to stop Gremlin Server", e);
			}
		}
	}
}
