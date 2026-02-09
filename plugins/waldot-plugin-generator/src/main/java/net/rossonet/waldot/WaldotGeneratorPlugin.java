package net.rossonet.waldot;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

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
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex;
import net.rossonet.waldot.dataGenerator.DataGeneratorVertex.Algorithm;
import net.rossonet.waldot.utils.ThreadHelper;

/**
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotGeneratorPlugin implements AutoCloseable, PluginListener {
	public static final String ALGORITHM_FIELD = "Algorithm";
	public static final String DATA_GENERATOR_OBJECT_TYPE_LABEL = "generator";
	public static final String DEFAULT_ALGORITHM_FIELD = Algorithm.incremental.toString();
	public static final Long DEFAULT_DELAY_FIELD = 1000L;
	public static final Long DEFAULT_MAX_VALUE_FIELD = 20000L;
	public static final Long DEFAULT_MIN_VALUE_FIELD = 0L;
	public static final String DELAY_FIELD = "Delay";
	private final static ExecutorService executor = ThreadHelper.newVirtualThreadExecutor();
	private final static Logger logger = LoggerFactory.getLogger(WaldotGeneratorPlugin.class);
	public static final String MAX_VALUE_FIELD = "Max";
	public static final String MIN_VALUE_FIELD = "Min";

	private UaObjectTypeNode dataGeneratorTypeNode;
	protected WaldotNamespace waldotNamespace;

	@Override
	public void close() throws Exception {
		executor.shutdownNow();
		logger.info("WaldotGeneratorPlugin closed");
	}

	@Override
	public boolean containsVertexType(String typeDefinitionLabel) {
		return DATA_GENERATOR_OBJECT_TYPE_LABEL.equals(typeDefinitionLabel);
	}

	@Override
	public boolean containsVertexTypeNode(NodeId typeDefinitionNodeId) {
		return dataGeneratorTypeNode.getNodeId().equals(typeDefinitionNodeId);
	}

	private void createDataGeneratorTypeNode() {
		dataGeneratorTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTDataGeneratorObjectType"))
				.setBrowseName(waldotNamespace.generateQualifiedName("WaldOTDataGeneratorObjectType"))
				.setDisplayName(LocalizedText.english("WaldOT Data Generator")).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, dataGeneratorTypeNode, MiloStrategy.LABEL_FIELD,
				NodeIds.String);
		DataGeneratorVertex.generateParameters(waldotNamespace, dataGeneratorTypeNode);
		// add definition to the address space
		waldotNamespace.getStorageManager().addNode(dataGeneratorTypeNode);
		dataGeneratorTypeNode.addReference(new Reference(dataGeneratorTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(dataGeneratorTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);
	}

	private WaldotVertex createDataGeneratorVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		return new DataGeneratorVertex(executor, graph, context, nodeId, browseName, displayName, description,
				writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	@Override
	public WaldotVertex createVertex(NodeId typeDefinitionNodeId, WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		if (!containsVertexTypeNode(typeDefinitionNodeId)) {
			return null;
		}
		if (dataGeneratorTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			return createDataGeneratorVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
					userWriteMask, eventNotifier, version, propertyKeyValues);
		} else {
			return null;
		}
	}

	@Override
	public Collection<WaldotCommand> getCommands() {
		return Arrays.asList();
	}

	@Override
	public NodeId getVertexTypeNode(String typeDefinitionLabel) {
		if (DATA_GENERATOR_OBJECT_TYPE_LABEL.equals(typeDefinitionLabel)) {
			return dataGeneratorTypeNode.getNodeId();
		} else {
			return null;
		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		createDataGeneratorTypeNode();
	}

}
