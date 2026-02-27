package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Graph.Variables;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.util.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Longs;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotMiloStrategy;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.api.models.base.GremlinElement;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputerView;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcEdge;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertexProperty;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;

@WaldotMiloStrategy
public class MiloSingleServerBaseStrategy implements MiloStrategy {

	private static final String EDGE_DIRECTORY_NODEID_PREFIX = "e";
	private static final String VERTEX_DIRECTORY_NODEID_PREFIX = "v";
	private UaFolderNode assetRootNode;
	private final Map<String, UaFolderNode> edgeDirectories = new HashMap<>();
	private final MiloSingleServerBaseFolderManager folderManager = new MiloSingleServerBaseFolderManager(this);
	private UaFolderNode interfaceRootNode;
	private final AtomicLong lastEventId = new AtomicLong(0);
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Graph.Variables opcGraphVariables;
	private UaFolderNode rootNode;

	private WaldotNamespace waldotNamespace;

	private void addDeletedCommand(GremlinElement vertex) {
		final AbstractOpcCommand deleteCommand = new AbstractOpcCommand(waldotNamespace.getGremlinGraph(),
				waldotNamespace, vertex.id().getIdentifier().toString() + ".delete", "delete",
				"Delete " + vertex.getBrowseName().getName(), null,
				waldotNamespace.getConfiguration().getWaldotCommandWriteMask(),
				waldotNamespace.getConfiguration().getWaldotCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getWaldotCommandExecutable(),
				waldotNamespace.getConfiguration().getWaldotCommandUserExecutable()) {

			@Override
			public Object[] runCommand(InvocationContext invocationContext, String[] inputValues) {
				String output = "";
				String error = "";
				try {
					output = waldotNamespace.getGremlinGraph().traversal().V(vertex.getNodeId().getIdentifier()).drop()
							.iterate() + " element deleted";
				} catch (final Exception e) {
					logger.error("Error deleting element {}: {}", vertex.getNodeId().getIdentifier(), e.getMessage());
					error = e.getMessage();
				}
				return new String[] { output, error };
			}
		};
		deleteCommand.addOutputArgument("output", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command output"));
		deleteCommand.addOutputArgument("error", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command error"));
		waldotNamespace.getStorageManager().addNode(deleteCommand);
		vertex.addComponent((UaNode) deleteCommand);
	}

	@Override
	public Edge addEdge(final WaldotVertex sourceVertex, final WaldotVertex targetVertex, final String label,
			final Object[] propertyKeyValues) {
		ElementHelper.legalPropertyKeyValueArray(propertyKeyValues);
		String type = MiloStrategy.getKeyValuesProperty(propertyKeyValues, TYPE_FIELD.toLowerCase());
		String elaboratedLabel = label;
		if (type == null || type.isEmpty()) {
			if (elaboratedLabel != null && !elaboratedLabel.isEmpty()) {
				type = elaboratedLabel;
			} else {
				type = DEFAULT_EDGE_TYPE;
			}
		}
		if (elaboratedLabel == null || elaboratedLabel.isEmpty()) {
			elaboratedLabel = type;
			logger.debug("Edge label not found in propertyKeyValues, using default label '{}'", elaboratedLabel);
		}
		NodeId nodeId = null;
		final Object readId = MiloStrategy.getIdValue(propertyKeyValues).orElse(null);
		if (readId != null) {
			nodeId = MiloStrategy.getNodeIdManager().convert(waldotNamespace.getGremlinGraph(), readId);
		} else {
			nodeId = waldotNamespace.generateNodeId(sourceVertex.getNodeId().getIdentifier() + ":" + elaboratedLabel
					+ ":" + targetVertex.getNodeId().getIdentifier());
		}
		String name = MiloStrategy.getKeyValuesProperty(propertyKeyValues, NAME_FIELD.toLowerCase());
		if (name == null) {
			name = elaboratedLabel;
			logger.debug(NAME_FIELD.toLowerCase() + " not found in propertyKeyValues, using default name '{}'", name);
		}
		String description = MiloStrategy.getKeyValuesProperty(propertyKeyValues, DESCRIPTION_PARAMETER.toLowerCase());
		if (description == null || description.isEmpty()) {
			description = sourceVertex.getDisplayName().getText() + " -- " + label + " -> "
					+ targetVertex.getDisplayName().getText();
			logger.debug(DESCRIPTION_PARAMETER + " not found in propertyKeyValues, using default '{}'", description);
		}
		final OpcEdge edge = new OpcEdge(waldotNamespace.getGremlinGraph(), nodeId, sourceVertex, targetVertex,
				elaboratedLabel, name, description,
				MiloSingleServerBaseReferenceNodeBuilder.getWriteMask(propertyKeyValues),
				MiloSingleServerBaseReferenceNodeBuilder.getUserWriteMask(propertyKeyValues),
				MiloSingleServerBaseReferenceNodeBuilder.getEventNotifier(propertyKeyValues),
				MiloSingleServerBaseReferenceNodeBuilder.getVersion(propertyKeyValues));
		waldotNamespace.getStorageManager().addNode(edge);
		checkDirectoryParameterAndLinkNode(propertyKeyValues, edge, folderManager.getEdgesFolderNode(), edgeDirectories,
				EDGE_DIRECTORY_NODEID_PREFIX);
		edge.addReference(new Reference(edge.getNodeId(), NodeIds.HasTypeDefinition,
				MiloSingleServerBaseReferenceNodeBuilder.edgeTypeNode.getNodeId().expanded(), true));
		final QualifiedProperty<String> labelProperty = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				LABEL_FIELD, MiloSingleServerBaseReferenceNodeBuilder.labelEdgeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		edge.setProperty(labelProperty, elaboratedLabel);
		final QualifiedProperty<String> typeProperty = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				TYPE_FIELD, MiloSingleServerBaseReferenceNodeBuilder.typeEdgeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		edge.setProperty(typeProperty, type);
		final QualifiedProperty<NodeId> sourceProperty = new QualifiedProperty<NodeId>(
				waldotNamespace.getNamespaceUri(), SOURCE_NODE,
				MiloSingleServerBaseReferenceNodeBuilder.sourceNodeTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				NodeId.class);
		edge.setProperty(sourceProperty, sourceVertex.getNodeId());
		final QualifiedProperty<NodeId> targetProperty = new QualifiedProperty<NodeId>(
				waldotNamespace.getNamespaceUri(), TARGET_NODE,
				MiloSingleServerBaseReferenceNodeBuilder.targetNodeTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				NodeId.class);
		edge.setProperty(targetProperty, targetVertex.getNodeId());
		edge.addReference(
				new Reference(edge.getNodeId(), MiloSingleServerBaseReferenceNodeBuilder.hasSourceNodeReferenceType,
						sourceVertex.getNodeId().expanded(), true));
		edge.addReference(
				new Reference(edge.getNodeId(), MiloSingleServerBaseReferenceNodeBuilder.hasTargetNodeReferenceType,
						targetVertex.getNodeId().expanded(), true));
		final Reference sourceReference = new Reference(sourceVertex.getNodeId(),
				MiloSingleServerBaseReferenceNodeBuilder.hasReferenceDescriptionReferenceType,
				edge.getNodeId().expanded(), true);
		edge.addRelatedReference(sourceReference);
		sourceVertex.addReference(sourceReference);
		final Reference targetReference = new Reference(targetVertex.getNodeId(),
				MiloSingleServerBaseReferenceNodeBuilder.hasReferenceDescriptionReferenceType,
				edge.getNodeId().expanded(), true);
		edge.addRelatedReference(targetReference);
		targetVertex.addReference(targetReference);
		// create the main reference type if it does not exist
		final NodeId mainReferenceTypeNodeId = getOrCreateReferenceType(type);
		final QualifiedProperty<NodeId> edgeReferenceType = new QualifiedProperty<NodeId>(
				waldotNamespace.getNamespaceUri(), REFERENCE_TYPE,
				MiloSingleServerBaseReferenceNodeBuilder.referenceTypeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		edge.setProperty(edgeReferenceType, mainReferenceTypeNodeId);
		final QualifiedProperty<Boolean> forward = new QualifiedProperty<Boolean>(waldotNamespace.getNamespaceUri(),
				IS_FORWARD, MiloSingleServerBaseReferenceNodeBuilder.isForwardTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, Boolean.class);
		edge.setProperty(forward, true);
		final Reference reference = new Reference(sourceVertex.getNodeId(), mainReferenceTypeNodeId,
				targetVertex.getNodeId().expanded(), true);
		edge.addRelatedReference(reference);
		sourceVertex.addReference(reference);
		popolateEdgePropertiesFromPropertyKeyValues(propertyKeyValues, edge);
		for (final PluginListener p : waldotNamespace.getPlugins()) {
			if (p.containsEdgeType(type)) {
				p.notifyAddEdge(edge, sourceVertex, targetVertex, elaboratedLabel, type, propertyKeyValues);
			}
		}
		addDeletedCommand(edge);
		addPropertyCommand(edge);
		return edge;
	}

	private void addPropertyCommand(GremlinElement vertex) {
		final AbstractOpcCommand propertyCommand = new AbstractOpcCommand(waldotNamespace.getGremlinGraph(),
				waldotNamespace, vertex.id().getIdentifier().toString() + ".property", "property",
				"Add or update vertex property" + vertex.getBrowseName().getName(), null,
				waldotNamespace.getConfiguration().getWaldotCommandWriteMask(),
				waldotNamespace.getConfiguration().getWaldotCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getWaldotCommandExecutable(),
				waldotNamespace.getConfiguration().getWaldotCommandUserExecutable()) {

			@Override
			public Object[] runCommand(InvocationContext invocationContext, String[] inputValues) {
				String output = "";
				String error = "";
				final String label = inputValues[0];
				final String value = inputValues[1];
				try {
					output = waldotNamespace.getGremlinGraph().traversal().V(vertex.getNodeId().getIdentifier())
							.property(label, value).iterate() + " property '" + label + "' updated with value '" + value
							+ "'";
				} catch (final Exception e) {
					logger.error("Error updating property '{}' of element {}: {}", label,
							vertex.getNodeId().getIdentifier(), e.getMessage());
					error = e.getMessage();
				}
				return new String[] { output, error };
			}
		};
		propertyCommand.addOutputArgument("output", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command output"));
		propertyCommand.addOutputArgument("error", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command error"));
		propertyCommand.addInputArgument("propertyLabel", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("property to add or update"));
		propertyCommand.addInputArgument("propertyValue", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("value of the property"));
		waldotNamespace.getStorageManager().addNode(propertyCommand);
		vertex.addComponent((UaNode) propertyCommand);
	}

	@Override
	public AbstractOpcVertex addVertex(final NodeId nodeId, final Object[] propertyKeyValues) {
		ElementHelper.legalPropertyKeyValueArray(propertyKeyValues);
		String label = MiloStrategy.getKeyValuesProperty(propertyKeyValues, LABEL_FIELD.toLowerCase());
		if (label == null) {
			label = DEFAULT_VERTEX_LABEL;
			logger.debug(LABEL_FIELD.toLowerCase() + " not found in propertyKeyValues, using default label '{}'",
					label);
		}
		String name = MiloStrategy.getKeyValuesProperty(propertyKeyValues, NAME_FIELD.toLowerCase());
		if (name == null) {
			name = label;
			logger.debug(NAME_FIELD.toLowerCase() + " not found in propertyKeyValues, using default name '{}'", name);
		}
		final QualifiedName browseName = waldotNamespace.generateQualifiedName(name);
		final LocalizedText displayName = new LocalizedText(name);
		String description = MiloStrategy.getKeyValuesProperty(propertyKeyValues, DESCRIPTION_PARAMETER.toLowerCase());
		if (description == null) {
			description = label;
			logger.debug(DESCRIPTION_PARAMETER + " not found in propertyKeyValues, using default '{}'", description);
		}
		NodeId typeDefinition = getVertexTypeNode(propertyKeyValues);
		if (typeDefinition == null) {
			typeDefinition = MiloSingleServerBaseReferenceNodeBuilder.vertexTypeNode.getNodeId();
			logger.debug(TYPE_FIELD.toLowerCase() + " not found in propertyKeyValues, using default type '{}'",
					typeDefinition);
		}
		final AbstractOpcVertex vertex = createVertex(nodeId, typeDefinition, label, description, browseName,
				displayName, propertyKeyValues,
				MiloSingleServerBaseReferenceNodeBuilder.getWriteMask(propertyKeyValues),
				MiloSingleServerBaseReferenceNodeBuilder.getUserWriteMask(propertyKeyValues),
				MiloSingleServerBaseReferenceNodeBuilder.getEventNotifier(propertyKeyValues),
				MiloSingleServerBaseReferenceNodeBuilder.getVersion(propertyKeyValues));
		addDeletedCommand(vertex);
		addPropertyCommand(vertex);
		return vertex;
	}

	private void checkDirectoryParameterAndLinkNode(final Object[] propertyKeyValues, final GremlinElement vertex,
			final UaFolderNode folderNode, final Map<String, UaFolderNode> directories, String suffix) {
		final String directory = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				DIRECTORY_PARAMETER.toLowerCase());
		if (directory == null || directory.isEmpty()) {
			folderNode.addOrganizes(vertex);
			return;
		}
		final String[] components = directory.split(DIRECTORY_SPLIT_SIMBOL);
		String actual = null;
		String last = actual;
		for (int counter = 0; counter < components.length; counter++) {
			actual = (actual == null ? "" : (actual + DIRECTORY_SPLIT_SIMBOL)) + components[counter].trim();
			if (!directories.containsKey(actual)) {
				directories.put(actual,
						new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
								waldotNamespace.generateNodeId(suffix + ":" + actual),
								waldotNamespace.generateQualifiedName(components[counter].trim()),
								LocalizedText.english(components[counter].trim())));
				waldotNamespace.getStorageManager().addNode(directories.get(actual));
				final UaFolderNode uaFolderNode = directories.get(actual);
				if (counter == 0) {
					folderNode.addOrganizes(uaFolderNode);
				} else {
					directories.get(last).addOrganizes(uaFolderNode);
				}
			}

			if (counter == components.length - 1) {
				directories.get(actual).addOrganizes(vertex);
			}
			last = actual;
		}
	}

	@Override
	public void close() throws Exception {
		resetNameSpace();
		// quando rivedi il codice, ricorda di chiudere le risorse aperte!

	}

	@Override
	public OpcGraphComputerView createGraphComputerView(final WaldotGraph graph, final GraphFilter graphFilter,
			final Set<VertexComputeKey> VertexComputeKey) {
		logger.info("createGraphComputerView: graph={}, graphFilter={}, VertexComputeKey={}", graph, graphFilter,
				VertexComputeKey);
		// TODO valutare come implementare GraphComputerView (per ora non si presuppone
		// l'uso di computazioni sul grafo)
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public <DATA_TYPE> OpcProperty<DATA_TYPE> createOrUpdateWaldotEdgeProperty(final WaldotEdge opcEdge,
			final String key, final DATA_TYPE value) {
		final NodeId nodeId = waldotNamespace
				.generateNodeId(opcEdge.getNodeId().getIdentifier().toString() + PROPERTY_SPLIT_SIMBOL_IN_NODEID + key);
		if (waldotNamespace.hasNodeId(nodeId)) {
			@SuppressWarnings("unchecked")
			final OpcProperty<DATA_TYPE> opcProperty = (OpcProperty<DATA_TYPE>) waldotNamespace.getStorageManager()
					.getNode(nodeId).get();
			final Variant variant = new Variant(value);
			final DataValue dataValue = DataValue.newValue().setStatus(StatusCode.GOOD).setSourceTime(DateTime.now())
					.setValue(variant).build();
			opcProperty.setValue(dataValue);
			return opcProperty;
		} else {
			final UaNodeContext context = opcEdge.getNodeContext();
			final LocalizedText description = LocalizedText
					.english(key + " of edge property " + opcEdge.getBrowseName().getName());
			final UInteger writeMask = MiloSingleServerBaseReferenceNodeBuilder.edgeVariableWriteMask;
			final UInteger userWriteMask = MiloSingleServerBaseReferenceNodeBuilder.edgeVariableUserWriteMask;
			final NodeId dataType = NodeIds.BaseDataType;
			final int valueRank = ValueRanks.Scalar;
			final UInteger[] arrayDimensions = null;
			final UByte accessLevel = MiloSingleServerBaseReferenceNodeBuilder.edgeVariableAccessLevel;
			final UByte userAccessLevel = MiloSingleServerBaseReferenceNodeBuilder.edgeVariableUserAccessLevel;
			final Double minimumSamplingInterval = -1.0;
			final boolean historizing = false;
			@SuppressWarnings({ "rawtypes", "unchecked" })
			final OpcProperty<DATA_TYPE> property = new OpcProperty(waldotNamespace.getGremlinGraph(), opcEdge, key,
					value, context, nodeId, description, writeMask, userWriteMask, dataType, valueRank, arrayDimensions,
					accessLevel, userAccessLevel, minimumSamplingInterval, historizing);
			waldotNamespace.getStorageManager().addNode(property);
			opcEdge.addRelatedProperty(property);
			opcEdge.addReference(new Reference(opcEdge.getNodeId(),
					MiloSingleServerBaseReferenceNodeBuilder.hasGremlinPropertyReferenceType,
					property.getNodeId().expanded(), true));
			opcEdge.notifyPropertyValueChanging(key, property.getValue());
			return property;
		}

	}

	@Override
	public <DATA_TYPE> OpcVertexProperty<DATA_TYPE> createOrUpdateWaldotVertexProperty(final WaldotVertex opcVertex,
			final String key, final DATA_TYPE value) {
		final NodeId nodeId = waldotNamespace.generateNodeId(
				opcVertex.getNodeId().getIdentifier().toString() + PROPERTY_SPLIT_SIMBOL_IN_NODEID + key);
		if (waldotNamespace.hasNodeId(nodeId)) {
			@SuppressWarnings("unchecked")
			final OpcVertexProperty<DATA_TYPE> vp = (OpcVertexProperty<DATA_TYPE>) waldotNamespace.getStorageManager()
					.get(nodeId);
			final Variant variant = new Variant(value);
			final DataValue dataValue = DataValue.newValue().setStatus(StatusCode.GOOD).setSourceTime(DateTime.now())
					.setValue(variant).build();
			vp.setValue(dataValue);
			return vp;
		} else {
			final UaNodeContext context = opcVertex.getNodeContext();
			final LocalizedText description = LocalizedText
					.english(key + " of vertex property " + opcVertex.getBrowseName().getName());
			final UInteger writeMask = MiloSingleServerBaseReferenceNodeBuilder.variableWriteMask;
			final UInteger userWriteMask = MiloSingleServerBaseReferenceNodeBuilder.variableUserWriteMask;
			final NodeId dataType = NodeIds.BaseDataType;
			final int valueRank = ValueRanks.Scalar;
			final UInteger[] arrayDimensions = null;
			final UByte accessLevel = MiloSingleServerBaseReferenceNodeBuilder.variableAccessLevel;
			final UByte userAccessLevel = MiloSingleServerBaseReferenceNodeBuilder.variableUserAccessLevel;
			final Double minimumSamplingInterval = -1.0;
			final boolean historizing = false;
			@SuppressWarnings({ "rawtypes", "unchecked" })
			final OpcVertexProperty<DATA_TYPE> property = new OpcVertexProperty(waldotNamespace.getGremlinGraph(),
					opcVertex, key, value, context, nodeId, description, writeMask, userWriteMask, dataType, valueRank,
					arrayDimensions, accessLevel, userAccessLevel, minimumSamplingInterval, historizing, false);
			waldotNamespace.getStorageManager().addNode(property);
			opcVertex.addReference(new Reference(opcVertex.getNodeId(),
					MiloSingleServerBaseReferenceNodeBuilder.hasGremlinPropertyReferenceType,
					property.getNodeId().expanded(), true));
			opcVertex.addRelatedProperty(property);
			opcVertex.notifyPropertyValueChanging(key, property.getValue());
			return property;
		}
	}

	private AbstractOpcVertex createVertex(final NodeId nodeId, final NodeId typeDefinition, final String label,
			final String description, final QualifiedName browseName, final LocalizedText displayName,
			final Object[] propertyKeyValues, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifierActive, final long version) {
		final AbstractOpcVertex vertex = createVertexObject(propertyKeyValues, typeDefinition,
				waldotNamespace.getGremlinGraph(), waldotNamespace.getOpcUaNodeContext(), nodeId, browseName,
				displayName, new LocalizedText(description), writeMask, userWriteMask, eventNotifierActive, version);
		waldotNamespace.getStorageManager().addNode(vertex);
		vertex.addReference(
				new Reference(vertex.getNodeId(), NodeIds.HasTypeDefinition, typeDefinition.expanded(), true));
		checkDirectoryParameterAndLinkNode(propertyKeyValues, vertex, folderManager.getVerticesFolderNode(),
				folderManager.getVertexDirectories(), VERTEX_DIRECTORY_NODEID_PREFIX);
		final QualifiedProperty<String> labelProperty = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				LABEL_FIELD, MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		vertex.setProperty(labelProperty, label);
		String type = MiloStrategy.getKeyValuesProperty(propertyKeyValues, TYPE_FIELD.toLowerCase());
		final QualifiedProperty<String> typeProperty = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				TYPE_FIELD, MiloSingleServerBaseReferenceNodeBuilder.vertexTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		if (type == null || type.isEmpty()) {
			type = "vertex";
			logger.debug(TYPE_FIELD.toLowerCase() + " not found in propertyKeyValues, using default type '{}'", type);
		}
		vertex.setProperty(typeProperty, type);
		popolateVertexPropertiesFromPropertyKeyValues(propertyKeyValues, vertex);
		return vertex;
	}

	private AbstractOpcVertex createVertexObject(Object[] propertyKeyValues, final NodeId typeDefinition,
			final WaldotGraph graph, final UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final UByte eventNotifier, final long version) {
		if (typeDefinition != null && waldotNamespace.hasNodeId(typeDefinition)) {
			for (final PluginListener p : waldotNamespace.getPlugins()) {
				if (p.containsVertexTypeNode(typeDefinition)) {
					return (AbstractOpcVertex) p.createVertex(typeDefinition, graph, context, nodeId, browseName,
							displayName, description, writeMask, userWriteMask, eventNotifier, version,
							propertyKeyValues);
				}
			}
		}
		return new OpcVertex(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask,
				eventNotifier, version);
	}

	@Override
	public String deleteOpcNodeId(String nodeId) {
		return waldotNamespace.getStorageManager()
				.removeNode(MiloStrategy.getNodeIdManager().convert(waldotNamespace.getGremlinGraph(), nodeId)).get()
				.getNodeId().toParseableString();
	}

	@Override
	public void dropGraphComputerView() {
		logger.info("dropGraphComputerView");
	}

	private boolean edgeLabelsIsValid(String[] edgeLabels, WaldotEdge edge) {
		if (edgeLabels == null || edgeLabels.length == 0) {
			return true;
		}
		try {
			if (edge.label() != null && !edge.label().isEmpty()) {
				for (final String label : edgeLabels) {
					if (edge.label().equals(label)) {
						return true;
					}
				}
			}
			return false;
		} catch (final Exception e) {
			logger.error("Error checking edge labels", e);
			return false;
		}
	}

	@Override
	public UaFolderNode getAssetRootFolderNode() {
		return assetRootNode;
	}

	@Override
	public WaldotVertex getEdgeInVertex(final WaldotEdge edge) {
		final QualifiedProperty<NodeId> target = new QualifiedProperty<NodeId>(waldotNamespace.getNamespaceUri(),
				TARGET_NODE, MiloSingleServerBaseReferenceNodeBuilder.targetNodeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		final NodeId nodeId = edge.getProperty(target).get();
		return (AbstractOpcVertex) waldotNamespace.getStorageManager().getNode(nodeId).get();
	}

	@Override
	public WaldotVertex getEdgeOutVertex(final WaldotEdge edge) {
		final QualifiedProperty<NodeId> source = new QualifiedProperty<NodeId>(waldotNamespace.getNamespaceUri(),
				SOURCE_NODE, MiloSingleServerBaseReferenceNodeBuilder.sourceNodeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		final NodeId nodeId = edge.getProperty(source).get();
		return (AbstractOpcVertex) waldotNamespace.getStorageManager().getNode(nodeId).get();
	}

	@Override
	public Map<NodeId, WaldotEdge> getEdges() {
		final Map<NodeId, WaldotEdge> result = new HashMap<>();
		for (final Node e : folderManager.getEdgesFolderNode().getOrganizesNodes()) {
			if (e instanceof WaldotEdge) {
				result.put(e.getNodeId(), (WaldotEdge) e);
			}
		}
		for (final UaFolderNode f : edgeDirectories.values()) {
			for (final Node e : f.getOrganizesNodes()) {
				if (e instanceof WaldotEdge) {
					result.put(e.getNodeId(), (WaldotEdge) e);
				}
			}
		}
		return result;
	}

	@Override
	public Map<NodeId, WaldotEdge> getEdges(final WaldotVertex opcVertex, final Direction direction,
			final String[] edgeLabels) {
		final Map<NodeId, WaldotEdge> result = new HashMap<>();
		for (final WaldotEdge e : getEdges().values()) {
			if (edgeLabelsIsValid(edgeLabels, e)) {
				final WaldotVertex inVertex = getEdgeInVertex(e);
				final WaldotVertex outVertex = getEdgeOutVertex(e);
				if ((direction == Direction.OUT || direction == Direction.BOTH)
						&& outVertex.getNodeId().equals(opcVertex.getNodeId())) {
					result.put(e.getNodeId(), e);
				}
				if ((direction == Direction.IN || direction == Direction.BOTH)
						&& inVertex.getNodeId().equals(opcVertex.getNodeId())) {
					result.put(e.getNodeId(), e);
				}
			}
		}
		return result;
	}

	public int getLastEventId() {
		return lastEventId.intValue();
	}

	private NodeId getOrCreateReferenceType(final String type) {
		for (final Tree<org.eclipse.milo.opcua.sdk.core.typetree.ReferenceType> r : waldotNamespace.getReferenceTypes()
				.getRoot().getChildren()) {
			if (r.getValue().getBrowseName() != null && r.getValue().getBrowseName().getName().equals(type)) {
				return r.getValue().getNodeId();
			}
		}
		final NodeId referenceTypeNode = MiloSingleServerBaseReferenceNodeBuilder.generateReferenceTypeNode(type,
				"is a " + type + " of ", type + " reference type", NodeIds.NonHierarchicalReferences, false, false,
				waldotNamespace);
		return referenceTypeNode;
	}

	private NodeId getParameterNodeId(final String key) {
		return waldotNamespace.generateNodeId(folderManager.getVariablesFolderNode().getNodeId().getIdentifier()
				+ PROPERTY_SPLIT_SIMBOL_IN_NODEID + key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getProperties(final WaldotEdge opcEdge) {
		final List<WaldotProperty<DATA_TYPE>> result = new ArrayList<>();
		for (final Reference p : opcEdge.getReferences()) {
			if (p.isForward() && p.getReferenceTypeId()
					.equals(MiloSingleServerBaseReferenceNodeBuilder.hasGremlinPropertyReferenceType)) {
				result.add((WaldotProperty<DATA_TYPE>) waldotNamespace.getStorageManager()
						.getNode(p.getTargetNodeId().toNodeId(waldotNamespace.getNamespaceTable()).get()).get());
			}
		}
		return result;
	}

	@Override
	public <DATA_TYPE> WaldotEdge getPropertyReference(final WaldotProperty<DATA_TYPE> property) {
		for (final Reference r : property.getReferences()) {
			if (r.getReferenceTypeId().equals(NodeIds.HasComponent) && r.isInverse()) {
				final NodeId nodeId = r.getSourceNodeId();
				return (OpcEdge) waldotNamespace.getStorageManager().get(nodeId);
			}
		}
		return null;
	}

	@Override
	public UaFolderNode getRootFolderNode() {
		return rootNode;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <DATA_TYPE> Map<String, WaldotVertexProperty<DATA_TYPE>> getVertexProperties(final WaldotVertex opcVertex) {
		final Map<String, WaldotVertexProperty<DATA_TYPE>> result = new HashMap<>();
		for (final Reference p : opcVertex.getReferences()) {
			if (p.isForward() && p.getReferenceTypeId()
					.equals(MiloSingleServerBaseReferenceNodeBuilder.hasGremlinPropertyReferenceType)) {
				final UaNode node = waldotNamespace.getStorageManager()
						.getNode(p.getTargetNodeId().toNodeId(waldotNamespace.getNamespaceTable()).get()).get();
				if (node instanceof WaldotVertexProperty) {
					result.put(node.getBrowseName().getName(), (WaldotVertexProperty<DATA_TYPE>) node);
				}
			}
		}
		return result;
	}

	@Override
	public <DATA_TYPE> WaldotVertex getVertexPropertyReference(
			final WaldotVertexProperty<DATA_TYPE> opcVertexProperty) {
		for (final Reference r : opcVertexProperty.getReferences()) {
			if (r.getReferenceTypeId().equals(NodeIds.HasComponent) && r.isInverse()) {
				final NodeId nodeId = r.getSourceNodeId();
				return (AbstractOpcVertex) waldotNamespace.getStorageManager().get(nodeId);
			}
		}
		return null;
	}

	private NodeId getVertexTypeNode(final Object[] propertyKeyValues) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && TYPE_FIELD.toLowerCase().equals(propertyKeyValues[i])) {
				if (DIRECTORY_PARAMETER.equals(propertyKeyValues[i + 1].toString())) {
					return NodeIds.FolderType;
				} else {
					for (final PluginListener p : waldotNamespace.getPlugins()) {
						if (p.containsVertexType(propertyKeyValues[i + 1].toString())) {
							return p.getVertexTypeNode(propertyKeyValues[i + 1].toString());
						}
					}
					final String requestType = propertyKeyValues[i + 1].toString();
					return NodeId.parse(requestType);
				}
			}
		}
		return null;
	}

	@Override
	public Map<NodeId, WaldotVertex> getVertices() {
		final Map<NodeId, WaldotVertex> result = new HashMap<>();
		for (final Node v : folderManager.getVerticesFolderNode().getOrganizesNodes()) {
			if (v instanceof Vertex) {
				result.put(v.getNodeId(), (WaldotVertex) v);
			}
		}
		for (final UaFolderNode f : folderManager.getVertexDirectories().values()) {
			for (final Node v : f.getOrganizesNodes()) {
				if (v instanceof Vertex) {
					result.put(v.getNodeId(), (WaldotVertex) v);
				}
			}
		}
		return result;
	}

	@Override
	public Map<NodeId, WaldotVertex> getVertices(final WaldotVertex opcVertex, final Direction direction,
			final String[] edgeLabels) {
		final Map<NodeId, WaldotVertex> result = new HashMap<>();
		for (final WaldotEdge e : getEdges().values()) {
			if (edgeLabelsIsValid(edgeLabels, e)) {
				final WaldotVertex inVertex = getEdgeInVertex(e);
				final WaldotVertex outVertex = getEdgeOutVertex(e);
				if ((direction == Direction.OUT || direction == Direction.BOTH)
						&& outVertex.getNodeId().equals(opcVertex.getNodeId())) {
					result.put(inVertex.getNodeId(), inVertex);
				}
				if ((direction == Direction.IN || direction == Direction.BOTH)
						&& inVertex.getNodeId().equals(opcVertex.getNodeId())) {
					result.put(outVertex.getNodeId(), outVertex);
				}
			}
		}
		return result;
	}

	@Override
	public WaldotNamespace getWaldotNamespace() {
		return waldotNamespace;
	}

	@Override
	public WaldotNamespace initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		MiloSingleServerBaseReferenceNodeBuilder.generateRefereceNodes(this);
		waldotNamespace.getOpcuaServer().updateReferenceTypeTree();
		rootNode = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace.getConfiguration().getRootNodeId()),
				waldotNamespace.generateQualifiedName(waldotNamespace.getConfiguration().getRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration().getRootNodeDisplayName()));
		interfaceRootNode = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace.getConfiguration().getInterfaceRootNodeId()),
				waldotNamespace
						.generateQualifiedName(waldotNamespace.getConfiguration().getInterfaceRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration().getInterfaceRootNodeDisplayName()));
		interfaceRootNode.addReference(new Reference(interfaceRootNode.getNodeId(), NodeIds.HasTypeDefinition,
				MiloSingleServerBaseReferenceNodeBuilder.interfaceTypeNode.getNodeId().expanded(), true));
		assetRootNode = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace.getConfiguration().getAssetRootNodeId()),
				waldotNamespace.generateQualifiedName(waldotNamespace.getConfiguration().getAssetRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration().getAssetRootNodeDisplayName()));
		waldotNamespace.getClientManagementStrategy().generateAssetFolders(assetRootNode);
		waldotNamespace.getStorageManager().addNode(getRootFolderNode());
		waldotNamespace.getStorageManager().addNode(assetRootNode);
		waldotNamespace.getStorageManager().addNode(interfaceRootNode);
		getRootFolderNode().addReference(new Reference(getRootFolderNode().getNodeId(), NodeIds.Organizes,
				NodeIds.ObjectsFolder.expanded(), false));
		assetRootNode.addReference(
				new Reference(assetRootNode.getNodeId(), NodeIds.Organizes, NodeIds.ObjectsFolder.expanded(), false));
		interfaceRootNode.addReference(new Reference(interfaceRootNode.getNodeId(), NodeIds.Organizes,
				NodeIds.ObjectsFolder.expanded(), false));
		folderManager.initialize();
		return waldotNamespace;
	}

	private void linkCommandDirectoryStructure(AbstractOpcCommand command) {
		final Map<String, UaFolderNode> commandDirectories = folderManager.getCommandDirectories();
		if (!commandDirectories.containsKey(command.getDirectory())) {
			final String[] components = command.getDirectory().split(DIRECTORY_SPLIT_SIMBOL);
			String actual = null;
			String last = actual;
			for (int counter = 0; counter < components.length; counter++) {
				actual = (actual == null ? "" : (actual + DIRECTORY_SPLIT_SIMBOL)) + components[counter].trim();
				if (!commandDirectories.containsKey(actual)) {
					commandDirectories.put(actual,
							new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
									waldotNamespace.generateNodeId(actual),
									waldotNamespace.generateQualifiedName(components[counter].trim()),
									LocalizedText.english(components[counter].trim())));
					waldotNamespace.getStorageManager().addNode(commandDirectories.get(actual));
					final UaFolderNode uaFolderNode = commandDirectories.get(actual);
					if (counter == 0) {
						interfaceRootNode.addOrganizes(uaFolderNode);
					} else {
						commandDirectories.get(last).addOrganizes(uaFolderNode);
					}
				}
				if (counter == components.length - 1) {
					commandDirectories.get(actual).addComponent(command);
				}
				last = actual;
			}
		} else {
			commandDirectories.get(command.getDirectory()).addComponent(command);
		}

	}

	@Override
	public Object namespaceParametersGet(final String key) {
		if (waldotNamespace.hasNodeId(getParameterNodeId(key))) {
			final UaVariableNode property = (UaVariableNode) waldotNamespace.getStorageManager()
					.getNode(getParameterNodeId(key)).get();
			return property.getValue().getValue().getValue();
		} else {
			return null;
		}
	}

	@Override
	public Set<String> namespaceParametersKeySet() {
		final Set<String> keySet = new HashSet<>();
		for (final Node n : folderManager.getVariablesFolderNode().getOrganizesNodes()) {
			keySet.add(n.getBrowseName().getName());
		}
		return keySet;
	}

	@Override
	public void namespaceParametersPut(final String key, final Object value) {
		if (waldotNamespace.hasNodeId(getParameterNodeId(key))) {
			final UaVariableNode property = (UaVariableNode) waldotNamespace.getStorageManager()
					.getNode(getParameterNodeId(key)).get();
			property.setValue(new DataValue(new Variant(value)));
		} else {
			final UaVariableNode property = new UaVariableNode(waldotNamespace.getOpcUaNodeContext(),
					getParameterNodeId(key), waldotNamespace.generateQualifiedName(key), LocalizedText.english(key),
					LocalizedText.english("variable " + key), UInteger.MIN, UInteger.MIN);
			waldotNamespace.getStorageManager().addNode(property);
			folderManager.getVariablesFolderNode().addOrganizes(property);
			property.setValue(new DataValue(new Variant(value)));
		}

	}

	@Override
	public void namespaceParametersRemove(final String key) {
		if (waldotNamespace.hasNodeId(getParameterNodeId(key))) {
			final UaVariableNode property = (UaVariableNode) waldotNamespace.getStorageManager()
					.getNode(getParameterNodeId(key)).get();
			folderManager.getVariablesFolderNode().removeOrganizes(property);
			waldotNamespace.getStorageManager().removeNode(property.getNodeId());
			property.delete();
		}

	}

	@Override
	public Variables namespaceParametersToVariables() {
		if (opcGraphVariables == null) {
			opcGraphVariables = new OpcGraphVariables(waldotNamespace);
		}
		return opcGraphVariables;
	}

	private void popolateEdgePropertiesFromPropertyKeyValues(final Object[] propertyKeyValues, final OpcEdge edge) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && propertyKeyValues[i] != null) {
				createOrUpdateWaldotEdgeProperty(edge, (String) propertyKeyValues[i], propertyKeyValues[i + 1]);
			}
		}
	}

	public void popolateVertexPropertiesFromPropertyKeyValues(final Object[] propertyKeyValues,
			final AbstractOpcVertex vertex) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && propertyKeyValues[i] != null) {
				createOrUpdateWaldotVertexProperty(vertex, (String) propertyKeyValues[i], propertyKeyValues[i + 1]);
			}
		}
	}

	@Override
	public void registerCommand(final WaldotCommand command) {
		linkCommandDirectoryStructure((AbstractOpcCommand) command);
	}

	@Override
	public void removeCommand(final WaldotCommand command) {
		interfaceRootNode.removeComponent((AbstractOpcCommand) command);
	}

	@Override
	public void removeEdge(final NodeId nodeId) {
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId).get();
		if (!(node instanceof WaldotEdge)) {
			logger.warn("NodeId {} is not an edge", nodeId);
			return;
		} else {
			final QualifiedProperty<String> typeProperty = new QualifiedProperty<String>(
					waldotNamespace.getNamespaceUri(), TYPE_FIELD,
					MiloSingleServerBaseReferenceNodeBuilder.labelEdgeTypeNode.getNodeId().expanded(),
					ValueRanks.Scalar, String.class);
			final WaldotEdge edge = (WaldotEdge) node;
			final String type = edge.getProperty(typeProperty).get();
			edge.removeRelatedOpcUaNodes();
			for (final PluginListener p : waldotNamespace.getPlugins()) {
				if (p.containsEdgeType(type)) {
					p.notifyRemoveEdge(edge);
				}
			}
		}
		waldotNamespace.getStorageManager().removeNode(nodeId);
		node.delete();
	}

	@Override
	public void removeReference(Reference reference) {
		waldotNamespace.getStorageManager().removeReference(reference);
	}

	@Override
	public void removeVertex(final NodeId nodeId) {
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId).get();
		if (!(node instanceof WaldotVertex)) {
			logger.warn("NodeId {} is not a vertex", nodeId);
			return;
		} else {
			((WaldotVertex) node).notifyRemoveVertex();
			((WaldotVertex) node).removeRelatedOpcUaNodes();
		}
		waldotNamespace.getStorageManager().removeNode(nodeId);
		node.delete();
		logger.info("Vertex with NodeId {} removed", nodeId);
	}

	@Override
	public void removeVertexProperty(final NodeId nodeId) {
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId).get();
		waldotNamespace.getStorageManager().removeNode(nodeId);
		node.delete();
	}

	@Override
	public void resetNameSpace() {
		logger.info("resetNameSpace");
		// TODO cancellare tutto il contenuto del namespace, compresi i folder

	}

	@Override
	public void updateEventGenerator(final Node sourceNode, String eventName, String eventDisplayName, String message,
			int severity) {
		try {
			final BaseEventTypeNode eventNode = waldotNamespace.getOpcuaServer().getServer().getEventFactory()
					.createEvent(waldotNamespace.generateNodeId(UUID.randomUUID()), NodeIds.BaseModelChangeEventType);
			eventNode.setBrowseName(new QualifiedName(1, eventName));
			eventNode.setDisplayName(LocalizedText.english(eventDisplayName));
			eventNode.setEventId(ByteString.of(Longs.toByteArray(lastEventId.incrementAndGet())));
			eventNode.setEventType(NodeIds.BaseEventType);
			eventNode.setSourceNode(sourceNode.getNodeId());
			eventNode.setSourceName(sourceNode.getDisplayName().getText());
			eventNode.setTime(DateTime.now());
			eventNode.setReceiveTime(DateTime.NULL_VALUE);
			eventNode.setMessage(LocalizedText.english(message));
			eventNode.setSeverity(ushort(severity));
			waldotNamespace.getEventBus().fire(eventNode);
			eventNode.delete();
		} catch (final Throwable e) {
			logger.error("Error creating EventNode: {}", e.getMessage(), e);
		}
	}

}
