package net.rossonet.waldot;

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
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.tinkerpop.GremlinVertex;

/**
 * WaldotTinkerPopPlugin is a plugin for the Waldot framework that integrates
 * with TinkerPop's Gremlin Server. It initializes the server with specific
 * settings and serializers, allowing for interaction with Waldot's graph
 * structure.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotTinkerPopPlugin implements PluginListener {
	public static final String BIND_HOST_FIELD = "bind";
	private static final String GREMLIN_TYPE_DISPLAY_NAME = "implementing the Gremlin Server protocol on a specific port";
	private static final String GREMLIN_TYPE_LABEL = "gremlin";
	private static final String GREMLIN_TYPE_NODE_ID = "GremlinServerObjectType";
	public static final String PORT_FIELD = "port";

	private UaObjectTypeNode gremlinTypeNode;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private WaldotNamespace waldotNamespace;

	@Override
	public boolean containsVertexType(String typeDefinitionLabel) {
		return GREMLIN_TYPE_LABEL.equals(typeDefinitionLabel);
	}

	@Override
	public boolean containsVertexTypeNode(NodeId typeDefinitionNodeId) {
		return gremlinTypeNode.getNodeId().equals(typeDefinitionNodeId);
	}

	private WaldotVertex createGremlinVertexObject(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		return new GremlinVertex(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask,
				eventNotifier, version, propertyKeyValues);
	}

	@Override
	public WaldotVertex createVertex(NodeId typeDefinitionNodeId, WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		if (!containsVertexTypeNode(typeDefinitionNodeId)) {
			logger.warn("TypeDefinitionNodeId: {} not managed by TinkerPopPlugin", typeDefinitionNodeId);
			return null;
		}
		final WaldotVertex vertexObject = createGremlinVertexObject(graph, context, nodeId, browseName, displayName,
				description, writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
		return vertexObject;
	}

	private void generateGremlinTypeNode() {
		gremlinTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + GREMLIN_TYPE_NODE_ID))
				.setBrowseName(waldotNamespace.generateQualifiedName(GREMLIN_TYPE_NODE_ID))
				.setDisplayName(LocalizedText.english(GREMLIN_TYPE_DISPLAY_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, gremlinTypeNode, MiloStrategy.LABEL_FIELD,
				NodeIds.String);
		GremlinVertex.generateParameters(waldotNamespace, gremlinTypeNode);
		waldotNamespace.getStorageManager().addNode(gremlinTypeNode);
		gremlinTypeNode.addReference(new Reference(gremlinTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(gremlinTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);

	}

	@Override
	public NodeId getVertexTypeNode(String typeDefinitionLabel) {
		switch (typeDefinitionLabel) {
		case GREMLIN_TYPE_LABEL:
			return gremlinTypeNode.getNodeId();
		default:
			logger.warn("TypeDefinitionLabel: {} not managed by TinkerPopPlugin", typeDefinitionLabel);
			return null;
		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		generateGremlinTypeNode();
		logger.info("Initializing Waldot TinkerPop Plugin");
	}

}
