package net.rossonet.waldot;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
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
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.olivetti.SmartGatewayInputTelemetryVertex;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.zenoh.ZenohClientVertex;

/**
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotZenohPlugin implements AutoCloseable, PluginListener {
	private static final String _ZENOH_TYPE_PRE_LABEL = "zenoh:";
	public static final String BASE_SMARTGATEWAY_INPUT_DIRECTORY_FIELD = "Directory";
	public static final String BASE_SMARTGATEWAY_ZENOH_PATH_FIELD = "Topic";
	private static final Object BIND_TO_LABEL = "bind";
	private static final String SMARTGATEWAY_INPUT_TYPE_DISPLAY_NAME = "SmartGateway Input Telemetry";
	private static final String SMARTGATEWAY_INPUT_TYPE_LABEL = _ZENOH_TYPE_PRE_LABEL + "sg-input";
	private static final String SMARTGATEWAY_INPUT_TYPE_NODE_ID = "SmartGatewayInputObjectType";
	public static final String ZENOH_CLIENT_CONFIGURATION_JSON_FIELD = "configuration-json";
	public static final String ZENOH_CLIENT_CONFIGURATION_PATH_FIELD = "configuration-path";
	private static final String ZENOH_TYPE_DISPLAY_NAME = "Zenoh client";
	private static final String ZENOH_TYPE_LABEL = _ZENOH_TYPE_PRE_LABEL + "client";
	private static final String ZENOH_TYPE_NODE_ID = "ZenohClientObjectType";

	private final Map<String, UaObjectTypeNode> labelTypeNodes = new HashMap<>();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<UaObjectTypeNode, Class<? extends AbstractOpcVertex>> typeClassNodes = new HashMap<>();
	private WaldotNamespace waldotNamespace;

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean containsEdgeType(String typeDefinitionLabel) {
		return BIND_TO_LABEL.equals(typeDefinitionLabel);
	}

	@Override
	public boolean containsVertexType(String typeDefinitionLabel) {
		return typeDefinitionLabel.startsWith(_ZENOH_TYPE_PRE_LABEL);
	}

	@Override
	public boolean containsVertexTypeNode(NodeId typeDefinitionNodeId) {
		boolean result = false;
		for (final UaObjectTypeNode typeNode : typeClassNodes.keySet()) {
			if (typeNode.getNodeId().equals(typeDefinitionNodeId)) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public WaldotVertex createVertex(NodeId typeDefinitionNodeId, WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		if (!containsVertexTypeNode(typeDefinitionNodeId)) {
			logger.warn("TypeDefinitionNodeId: {} not managed by WaldOT Zenoh Plugin", typeDefinitionNodeId);
			return null;
		}
		for (final Map.Entry<UaObjectTypeNode, Class<? extends AbstractOpcVertex>> entry : typeClassNodes.entrySet()) {
			if (entry.getKey().getNodeId().equals(typeDefinitionNodeId)) {
				try {
					final Constructor<? extends AbstractOpcVertex> builder = entry.getValue().getConstructor(
							WaldotGraph.class, UaNodeContext.class, NodeId.class, QualifiedName.class,
							LocalizedText.class, LocalizedText.class, UInteger.class, UInteger.class, UByte.class,
							long.class, Object[].class);
					return builder.newInstance(graph, context, nodeId, browseName, displayName, description, writeMask,
							userWriteMask, eventNotifier, version, propertyKeyValues);
				} catch (final Throwable e) {
					logger.error("Error creating vertex for typeDefinitionNodeId: {}", typeDefinitionNodeId, e);
				}
			}
		}
		logger.warn("TypeDefinitionNodeId: {} not managed by WaldOT Zenoh Plugin", typeDefinitionNodeId);
		return null;
	}

	private UaObjectTypeNode generateSmartGatewayInputTelemetryTypeNode() {
		final UaObjectTypeNode type = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + SMARTGATEWAY_INPUT_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(SMARTGATEWAY_INPUT_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(SMARTGATEWAY_INPUT_TYPE_DISPLAY_NAME)).setIsAbstract(false)
				.build();
		PluginListener.addParameterToTypeNode(waldotNamespace, type, MiloStrategy.LABEL_FIELD, NodeIds.String);
		SmartGatewayInputTelemetryVertex.generateParameters(waldotNamespace, type);
		waldotNamespace.getStorageManager().addNode(type);
		type.addReference(
				new Reference(type.getNodeId(), NodeIds.HasSubtype, NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(type.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);
		return type;
	}

	private UaObjectTypeNode generateZenohClientTypeNode() {
		final UaObjectTypeNode type = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + ZENOH_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(ZENOH_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(ZENOH_TYPE_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, type, MiloStrategy.LABEL_FIELD, NodeIds.String);
		ZenohClientVertex.generateParameters(waldotNamespace, type);
		waldotNamespace.getStorageManager().addNode(type);
		type.addReference(
				new Reference(type.getNodeId(), NodeIds.HasSubtype, NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(type.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);
		return type;
	}

	@Override
	public NodeId getVertexTypeNode(String typeDefinitionLabel) {
		if (labelTypeNodes.containsKey(typeDefinitionLabel)) {
			return labelTypeNodes.get(typeDefinitionLabel).getNodeId();
		} else {
			logger.warn("TypeDefinitionLabel: {} not managed by WaldOT Zenoh Plugin", typeDefinitionLabel);
			return null;
		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		final UaObjectTypeNode zenohClientTypeNode = generateZenohClientTypeNode();
		labelTypeNodes.put(ZENOH_TYPE_LABEL, zenohClientTypeNode);
		typeClassNodes.put(zenohClientTypeNode, ZenohClientVertex.class);
		final UaObjectTypeNode smartGatewayInputTelemetryTypeNode = generateSmartGatewayInputTelemetryTypeNode();
		labelTypeNodes.put(SMARTGATEWAY_INPUT_TYPE_LABEL, smartGatewayInputTelemetryTypeNode);
		typeClassNodes.put(smartGatewayInputTelemetryTypeNode, SmartGatewayInputTelemetryVertex.class);
		logger.info("Initializing WaldOT Zenoh Plugin");
	}

	@Override
	public void notifyAddEdge(WaldotEdge edge, WaldotVertex sourceVertex, WaldotVertex targetVertex, String label,
			String type, Object[] propertyKeyValues) {
		// TODO
	}

	@Override
	public void notifyRemoveEdge(WaldotEdge edge) {
		// TODO
	}

}
