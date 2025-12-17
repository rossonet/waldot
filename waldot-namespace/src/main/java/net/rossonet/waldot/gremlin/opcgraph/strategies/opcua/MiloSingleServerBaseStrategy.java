package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
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
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.rules.DefaultRule;

@WaldotMiloStrategy
public class MiloSingleServerBaseStrategy implements MiloStrategy {

	private UaFolderNode assetRootNode;

	private final Map<String, UaFolderNode> edgeDirectories = new HashMap<>();

	private final MiloSingleServerBaseFolderManager folderManager = new MiloSingleServerBaseFolderManager(this);

	private UaFolderNode interfaceRootNode;
	private final AtomicInteger lastEventId = new AtomicInteger(0);
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Graph.Variables opcGraphVariables;
	private UaFolderNode rootNode;

	private WaldotNamespace waldotNamespace;

	@Override
	public Edge addEdge(final WaldotVertex sourceVertex, final WaldotVertex targetVertex, final String label,
			final Object[] propertyKeyValues) {
		String elaboratedLabel = label;
		// register the edge in the rules engine if it is an observer edge
		if (OBSERVER_EDGE_PARAMETER.equals(label)) {
			elaboratedLabel = HAS_WALDOT_RULE;
			waldotNamespace.getRulesEngine().registerObserver(sourceVertex, targetVertex.getNodeId());
		}
		// generate the edge label and description
		final NodeId nodeId = waldotNamespace.generateNodeId(sourceVertex.getNodeId().getIdentifier() + ":"
				+ elaboratedLabel + ":" + targetVertex.getNodeId().getIdentifier());
		String description = getKeyValuesProperty(propertyKeyValues, DESCRIPTION_PARAMETER.toLowerCase());
		if (description == null || description.isEmpty()) {
			description = sourceVertex.getDescription().getText() + " -- " + label + " -> "
					+ targetVertex.getDescription().getText();
			logger.debug(DESCRIPTION_PARAMETER + " not found in propertyKeyValues, using default '{}'", description);
		}
		// create the edge
		// TODO parametrizzare tutto per la creazione dell'edge
		final OpcEdge edge = new OpcEdge(waldotNamespace.getGremlinGraph(), nodeId, sourceVertex, targetVertex,
				elaboratedLabel, description, MiloSingleServerBaseReferenceNodeBuilder.vertexWriteMask,
				MiloSingleServerBaseReferenceNodeBuilder.vertexUserWriteMask,
				MiloSingleServerBaseReferenceNodeBuilder.eventNotifierDisable,
				MiloSingleServerBaseReferenceNodeBuilder.version);
		waldotNamespace.getStorageManager().addNode(edge);
		checkDirectoryParameterAndLinkNode(propertyKeyValues, edge, folderManager.getEdgesFolderNode(),
				edgeDirectories);
		// add references and properties to the edge
		edge.addReference(new Reference(edge.getNodeId(), Identifiers.HasTypeDefinition,
				MiloSingleServerBaseReferenceNodeBuilder.edgeTypeNode.getNodeId().expanded(), true));
		final QualifiedProperty<String> LABEL = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				LABEL_FIELD, MiloSingleServerBaseReferenceNodeBuilder.labelEdgeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		edge.setProperty(LABEL, elaboratedLabel);
		final QualifiedProperty<NodeId> SOURCE = new QualifiedProperty<NodeId>(waldotNamespace.getNamespaceUri(),
				"SourceNode", MiloSingleServerBaseReferenceNodeBuilder.sourceNodeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		edge.setProperty(SOURCE, sourceVertex.getNodeId());
		final QualifiedProperty<NodeId> TARGET = new QualifiedProperty<NodeId>(waldotNamespace.getNamespaceUri(),
				"TargetNode", MiloSingleServerBaseReferenceNodeBuilder.targetNodeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		edge.setProperty(TARGET, targetVertex.getNodeId());
		edge.addReference(
				new Reference(edge.getNodeId(), MiloSingleServerBaseReferenceNodeBuilder.hasSourceNodeReferenceType,
						sourceVertex.getNodeId().expanded(), true));
		edge.addReference(
				new Reference(edge.getNodeId(), MiloSingleServerBaseReferenceNodeBuilder.hasTargetNodeReferenceType,
						targetVertex.getNodeId().expanded(), true));
		sourceVertex.addReference(new Reference(sourceVertex.getNodeId(),
				MiloSingleServerBaseReferenceNodeBuilder.hasReferenceDescriptionReferenceType,
				edge.getNodeId().expanded(), true));
		targetVertex.addReference(new Reference(targetVertex.getNodeId(),
				MiloSingleServerBaseReferenceNodeBuilder.hasReferenceDescriptionReferenceType,
				edge.getNodeId().expanded(), true));
		// create the main reference type if it does not exist
		final NodeId mainReferenceTypeNodeId = getOrCreateReferenceType(elaboratedLabel);
		final QualifiedProperty<NodeId> REFERENCE_TYPE = new QualifiedProperty<NodeId>(
				waldotNamespace.getNamespaceUri(), "ReferenceType",
				MiloSingleServerBaseReferenceNodeBuilder.referenceTypeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		edge.setProperty(REFERENCE_TYPE, mainReferenceTypeNodeId);
		final QualifiedProperty<Boolean> FORWARD = new QualifiedProperty<Boolean>(waldotNamespace.getNamespaceUri(),
				"IsForward", MiloSingleServerBaseReferenceNodeBuilder.isForwardTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, Boolean.class);
		edge.setProperty(FORWARD, true);
		sourceVertex.addReference(new Reference(sourceVertex.getNodeId(), mainReferenceTypeNodeId,
				targetVertex.getNodeId().expanded(), true));
		popolateEdgePropertiesFromPropertyKeyValues(propertyKeyValues, edge);
		return edge;
	}

	@Override
	public AbstractOpcVertex addVertex(final NodeId nodeId, final Object[] propertyKeyValues) {
		// check propertyKeyValues
		ElementHelper.legalPropertyKeyValueArray(propertyKeyValues);
		// get basic fields
		String label = getKeyValuesProperty(propertyKeyValues, LABEL_FIELD.toLowerCase());
		if (label == null) {
			label = "vertex";
			logger.warn(LABEL_FIELD.toLowerCase() + " not found in propertyKeyValues, using default label '{}'", label);
		}
		final QualifiedName browseName = waldotNamespace.generateQualifiedName(label);
		final LocalizedText displayName = new LocalizedText(label);
		String description = getKeyValuesProperty(propertyKeyValues, DESCRIPTION_PARAMETER.toLowerCase());
		if (description == null) {
			description = label;
			logger.debug(DESCRIPTION_PARAMETER + " not found in propertyKeyValues, using default '{}'", description);
		}
		NodeId typeDefinition = getTypeDefinition(propertyKeyValues);
		if (typeDefinition == null) {
			typeDefinition = MiloSingleServerBaseReferenceNodeBuilder.vertexTypeNode.getNodeId();
			logger.debug(TYPE_DEFINITION_PARAMETER + " not found in propertyKeyValues, using default type '{}'",
					typeDefinition);
		}
		// check if typeDefinition is a rule
		if (typeDefinition.equals(MiloSingleServerBaseReferenceNodeBuilder.ruleTypeNode.getNodeId())) {
			return createVertexRule(nodeId, typeDefinition, label, description, browseName, displayName,
					propertyKeyValues);
		} else {
			// if typeDefinition is not a rule, create a vertex
			// TODO parametrizzare tutto per la creazione del vertice
			return createVertex(nodeId, typeDefinition, label, description, browseName, displayName, propertyKeyValues,
					MiloSingleServerBaseReferenceNodeBuilder.vertexWriteMask,
					MiloSingleServerBaseReferenceNodeBuilder.vertexUserWriteMask,
					MiloSingleServerBaseReferenceNodeBuilder.eventNotifierActive,
					MiloSingleServerBaseReferenceNodeBuilder.version);
		}

	}

	private void checkDirectoryParameterAndLinkNode(final Object[] propertyKeyValues, final GremlinElement vertex,
			final UaFolderNode folderNode, final Map<String, UaFolderNode> directories) {
		final String directory = getKeyValuesProperty(propertyKeyValues, DIRECTORY_PARAMETER.toLowerCase());
		if (directory == null || directory.isEmpty()) {
			folderNode.addOrganizes(vertex);
			return;
		}
		final String[] components = directory.split("/");
		String actual = null;
		String last = actual;
		for (int counter = 0; counter < components.length; counter++) {
			actual = (actual == null ? "" : (actual + "/")) + components[counter].trim();
			if (!directories.containsKey(actual)) {
				directories.put(actual,
						new UaFolderNode(waldotNamespace.getOpcUaNodeContext(), waldotNamespace.generateNodeId(actual),
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
	public OpcGraphComputerView createGraphComputerView(final WaldotGraph graph, final GraphFilter graphFilter,
			final Set<VertexComputeKey> VertexComputeKey) {
		logger.info("createGraphComputerView: graph={}, graphFilter={}, VertexComputeKey={}", graph, graphFilter,
				VertexComputeKey);
		// TODO GraphComputerView
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public <DATA_TYPE> OpcProperty<DATA_TYPE> createOrUpdateWaldotEdgeProperty(final WaldotEdge opcEdge,
			final String key, final DATA_TYPE value) {
		final NodeId nodeId = waldotNamespace
				.generateNodeId(opcEdge.getNodeId().getIdentifier().toString() + "/" + key);
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
					.english(key + " of edge property " + opcEdge.getBrowseName());
			final UInteger writeMask = MiloSingleServerBaseReferenceNodeBuilder.edgeVariableWriteMask;
			final UInteger userWriteMask = MiloSingleServerBaseReferenceNodeBuilder.edgeVariableUserWriteMask;
			final NodeId dataType = Identifiers.BaseDataType;
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
			opcEdge.addReference(new Reference(opcEdge.getNodeId(),
					MiloSingleServerBaseReferenceNodeBuilder.hasPropertyReferenceType, property.getNodeId().expanded(),
					true));
			return property;
		}
	}

	@Override
	public <DATA_TYPE> OpcVertexProperty<DATA_TYPE> createOrUpdateWaldotVertexProperty(final WaldotVertex opcVertex,
			final String key, final DATA_TYPE value) {
		final NodeId nodeId = waldotNamespace
				.generateNodeId(opcVertex.getNodeId().getIdentifier().toString() + "/" + key);
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
					.english(key + " of vertex property " + opcVertex.getBrowseName());
			final UInteger writeMask = MiloSingleServerBaseReferenceNodeBuilder.variableWriteMask;
			final UInteger userWriteMask = MiloSingleServerBaseReferenceNodeBuilder.variableUserWriteMask;
			final NodeId dataType = Identifiers.BaseDataType;
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
			property.addAttributeObserver(opcVertex);
			opcVertex.addReference(new Reference(opcVertex.getNodeId(),
					MiloSingleServerBaseReferenceNodeBuilder.hasPropertyReferenceType, property.getNodeId().expanded(),
					true));
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
				new Reference(vertex.getNodeId(), Identifiers.HasTypeDefinition, typeDefinition.expanded(), true));
		checkDirectoryParameterAndLinkNode(propertyKeyValues, vertex, folderManager.getVerticesFolderNode(),
				folderManager.getVertexDirectories());
		final QualifiedProperty<String> LABEL = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				LABEL_FIELD, MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		vertex.setProperty(LABEL, label);
		popolateVertexPropertiesFromPropertyKeyValues(propertyKeyValues, vertex);
		return vertex;
	}

	private AbstractOpcVertex createVertexObject(Object[] propertyKeyValues, final NodeId typeDefinition,
			final WaldotGraph graph, final UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final UByte eventNotifier, final long version) {
		if (typeDefinition != null && waldotNamespace.hasNodeId(typeDefinition)) {
			for (final PluginListener p : waldotNamespace.getPlugins()) {
				if (p.containsObjectDefinition(typeDefinition)) {
					return (AbstractOpcVertex) p.createVertexObject(typeDefinition, graph, context, nodeId, browseName,
							displayName, description, writeMask, userWriteMask, eventNotifier, version,
							propertyKeyValues);
				}
			}
		}
		return new OpcVertex(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask,
				eventNotifier, version);
	}

	private AbstractOpcVertex createVertexRule(final NodeId nodeId, final NodeId typeDefinition, final String label,
			final String description, final QualifiedName browseName, final LocalizedText displayName,
			final Object[] propertyKeyValues) {
		String action = getKeyValuesProperty(propertyKeyValues, ACTION_FIELD.toLowerCase());
		// get rule specific fields
		if (action == null) {
			action = DEFAULT_ACTION_VALUE;
			logger.info(ACTION_FIELD.toLowerCase() + " not found in propertyKeyValues, using default action '{}'",
					action);
		}
		String condition = getKeyValuesProperty(propertyKeyValues, CONDITION_FIELD.toLowerCase());
		if (condition == null) {
			condition = DEFAULT_CONDITION_VALUE;
			logger.info(CONDITION_FIELD.toLowerCase() + " not found in propertyKeyValues, using default condition '{}'",
					condition);
		}
		Integer priority = getPriorityDefinition(propertyKeyValues);
		if (priority == null) {
			priority = DEFAULT_PRIORITY_VALUE;
			logger.info(PRIORITY_FIELD.toLowerCase() + " not found in propertyKeyValues, using default priority '{}'",
					priority);
		}
		final String condition1 = condition;
		final String action1 = action;
		final int priority1 = priority;

		// create rule node
		final DefaultRule rule = new DefaultRule(typeDefinition, waldotNamespace.getGremlinGraph(),
				waldotNamespace.getOpcUaNodeContext(), nodeId, browseName, displayName, new LocalizedText(description),
				MiloSingleServerBaseReferenceNodeBuilder.vertexWriteMask,
				MiloSingleServerBaseReferenceNodeBuilder.vertexUserWriteMask,
				MiloSingleServerBaseReferenceNodeBuilder.eventNotifierActive,
				MiloSingleServerBaseReferenceNodeBuilder.version, waldotNamespace.getRulesEngine(), condition1, action1,
				priority1, waldotNamespace.getConfiguration().getDefaultFactsValidUntilMs(),
				waldotNamespace.getConfiguration().getDefaultFactsValidDelayMs());
		// TODO parametrizzare tutto per la creazione della regola
		rule.setDelayBeforeEvaluation(DEFAULT_DELAY_BEFORE_EVALUATION);
		rule.setDelayBeforeExecute(DEFAULT_DELAY_BEFORE_EXECUTE);
		rule.setClearFactsAfterExecution(DEFAULT_CLEAR_FACTS_AFTER_EXECUTION);
		rule.setRefractoryPeriodMs(DEFAULT_REFACTORY_PERIOD_MS);
		rule.setParallelExecution(DEFAULT_PARALLEL_EXECUTION);
		waldotNamespace.getStorageManager().addNode(rule);
		checkDirectoryParameterAndLinkNode(propertyKeyValues, rule, folderManager.getRulesFolderNode(),
				folderManager.getRulesDirectories());
		// popolate rule properties
		final QualifiedProperty<String> ACTION = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				ACTION_FIELD, MiloSingleServerBaseReferenceNodeBuilder.actionRuleTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		rule.setProperty(ACTION, action);

		final QualifiedProperty<String> CONDITION = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				CONDITION_FIELD, MiloSingleServerBaseReferenceNodeBuilder.conditionRuleTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		rule.setProperty(CONDITION, condition);

		final QualifiedProperty<Integer> PRIORITY = new QualifiedProperty<Integer>(waldotNamespace.getNamespaceUri(),
				PRIORITY_FIELD, MiloSingleServerBaseReferenceNodeBuilder.priorityRuleTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, Integer.class);
		rule.setProperty(PRIORITY, priority);

		final QualifiedProperty<String> LABEL = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				LABEL_FIELD, MiloSingleServerBaseReferenceNodeBuilder.labelRuleTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		rule.setProperty(LABEL, label);
		// register rule in rules engine
		waldotNamespace.getRulesEngine().registerOrUpdateRule(rule);
		popolateVertexPropertiesFromPropertyKeyValues(propertyKeyValues, rule);
		return rule;
	}

	@Override
	public void dropGraphComputerView() {
		logger.info("dropGraphComputerView");
		// TODO GraphComputerView
	}

	@Override
	public UaFolderNode getAssetRootFolderNode() {
		return assetRootNode;
	}

	@Override
	public WaldotVertex getEdgeInVertex(final WaldotEdge edge) {
		final QualifiedProperty<NodeId> TARGET = new QualifiedProperty<NodeId>(waldotNamespace.getNamespaceUri(),
				"TargetNode", MiloSingleServerBaseReferenceNodeBuilder.targetNodeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		final NodeId nodeId = edge.getProperty(TARGET).get();
		return (AbstractOpcVertex) waldotNamespace.getStorageManager().getNode(nodeId).get();
	}

	@Override
	public WaldotVertex getEdgeOutVertex(final WaldotEdge edge) {
		final QualifiedProperty<NodeId> SOURCE = new QualifiedProperty<NodeId>(waldotNamespace.getNamespaceUri(),
				"SourceNode", MiloSingleServerBaseReferenceNodeBuilder.sourceNodeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		final NodeId nodeId = edge.getProperty(SOURCE).get();
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
		logger.error("getEdges: opcVertex={}, direction={}, edgeLabels={}", opcVertex, direction, edgeLabels);
		// TODO da verificare come usare questa chiamata
		throw new UnsupportedOperationException("Not implemented yet");
	}

	private String getKeyValuesProperty(final Object[] propertyKeyValues, final String label) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && label.equals(propertyKeyValues[i])) {
				return propertyKeyValues[i + 1].toString();
			}
		}
		return null;
	}

	private NodeId getOrCreateReferenceType(final String label) {
		for (final Tree<org.eclipse.milo.opcua.sdk.core.typetree.ReferenceType> r : waldotNamespace.getReferenceTypes()
				.getRoot().getChildren()) {
			if (r.getValue().getBrowseName().getName().equals(label)) {
				return r.getValue().getNodeId();
			}
		}
		return MiloSingleServerBaseReferenceNodeBuilder.generateReferenceTypeNode(label, "is a " + label + " of ",
				label + " reference type", Identifiers.NonHierarchicalReferences, false, false, waldotNamespace);
	}

	private NodeId getParameterNodeId(final String key) {
		return waldotNamespace
				.generateNodeId(folderManager.getVariablesFolderNode().getNodeId().getIdentifier() + ":" + key);
	}

	private Integer getPriorityDefinition(final Object[] propertyKeyValues) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && PRIORITY_FIELD.toLowerCase().equals(propertyKeyValues[i])) {
				try {
					return Integer.parseInt(propertyKeyValues[i + 1].toString());
				} catch (final NumberFormatException e) {
					return null;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getProperties(final WaldotEdge opcEdge) {
		final List<WaldotProperty<DATA_TYPE>> result = new ArrayList<>();
		for (final Reference p : opcEdge.getReferences()) {
			if (p.isForward() && p.getReferenceTypeId()
					.equals(MiloSingleServerBaseReferenceNodeBuilder.hasPropertyReferenceType)) {
				result.add((WaldotProperty<DATA_TYPE>) waldotNamespace.getStorageManager()
						.getNode(p.getTargetNodeId().toNodeId(waldotNamespace.getNamespaceTable()).get()).get());
			}
		}
		return result;
	}

	@Override
	public <DATA_TYPE> WaldotEdge getPropertyReference(final WaldotProperty<DATA_TYPE> property) {
		for (final Reference r : property.getReferences()) {
			if (r.getReferenceTypeId().equals(Identifiers.HasComponent) && r.isInverse()) {
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

	private NodeId getTypeDefinition(final Object[] propertyKeyValues) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && TYPE_DEFINITION_PARAMETER.equals(propertyKeyValues[i])) {
				if (RULE_NODE_PARAMETER.equals(propertyKeyValues[i + 1].toString())) {
					return MiloSingleServerBaseReferenceNodeBuilder.ruleTypeNode.getNodeId();
				} else if (DIRECTORY_PARAMETER.equals(propertyKeyValues[i + 1].toString())) {
					return Identifiers.FolderType;
				} else {
					for (final PluginListener p : waldotNamespace.getPlugins()) {
						if (p.containsObjectLabel(propertyKeyValues[i + 1].toString())) {
							return p.getObjectLabel(propertyKeyValues[i + 1].toString());
						}
					}
					final String requestType = propertyKeyValues[i + 1].toString();
					return NodeId.parse(requestType);
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <DATA_TYPE> Map<String, WaldotVertexProperty<DATA_TYPE>> getVertexProperties(final WaldotVertex opcVertex) {
		final Map<String, WaldotVertexProperty<DATA_TYPE>> result = new HashMap<>();
		for (final Reference p : opcVertex.getReferences()) {
			if (p.isForward() && p.getReferenceTypeId()
					.equals(MiloSingleServerBaseReferenceNodeBuilder.hasPropertyReferenceType)) {
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
			if (r.getReferenceTypeId().equals(Identifiers.HasComponent) && r.isInverse()) {
				final NodeId nodeId = r.getSourceNodeId();
				return (AbstractOpcVertex) waldotNamespace.getStorageManager().get(nodeId);
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
		for (final Node v : folderManager.getRulesFolderNode().getOrganizesNodes()) {
			if (v instanceof Vertex) {
				result.put(v.getNodeId(), (WaldotVertex) v);
			}
		}
		for (final UaFolderNode f : folderManager.getRulesDirectories().values()) {
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
		logger.error("getVertices: opcVertex={}, direction={}, edgeLabels={}", opcVertex, direction, edgeLabels);
		// TODO da verificare come usare questa chiamata
		return null;
	}

	public WaldotNamespace getWaldotNamespace() {
		return waldotNamespace;
	}

	@Override
	public WaldotNamespace initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		MiloSingleServerBaseReferenceNodeBuilder.generateRefereceNodes(this);
		rootNode = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace.getConfiguration().getRootNodeId()),
				waldotNamespace.generateQualifiedName(waldotNamespace.getConfiguration().getRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration().getRootNodeDisplayName()));
		interfaceRootNode = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace.getConfiguration().getInterfaceRootNodeId()),
				waldotNamespace
						.generateQualifiedName(waldotNamespace.getConfiguration().getInterfaceRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration().getInterfaceRootNodeDisplayName()));
		interfaceRootNode.addReference(new Reference(interfaceRootNode.getNodeId(), Identifiers.HasTypeDefinition,
				MiloSingleServerBaseReferenceNodeBuilder.interfaceTypeNode.getNodeId().expanded(), true));
		assetRootNode = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace.getConfiguration().getAssetRootNodeId()),
				waldotNamespace.generateQualifiedName(waldotNamespace.getConfiguration().getAssetRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration().getAssetRootNodeDisplayName()));
		waldotNamespace.getAgentManagementStrategy().generateAssetFolders(assetRootNode);
		waldotNamespace.getStorageManager().addNode(getRootFolderNode());
		waldotNamespace.getStorageManager().addNode(assetRootNode);
		waldotNamespace.getStorageManager().addNode(interfaceRootNode);
		getRootFolderNode().addReference(new Reference(getRootFolderNode().getNodeId(), Identifiers.Organizes,
				Identifiers.ObjectsFolder.expanded(), false));
		assetRootNode.addReference(new Reference(assetRootNode.getNodeId(), Identifiers.Organizes,
				Identifiers.ObjectsFolder.expanded(), false));
		interfaceRootNode.addReference(new Reference(interfaceRootNode.getNodeId(), Identifiers.Organizes,
				Identifiers.ObjectsFolder.expanded(), false));
		folderManager.initialize();
		return waldotNamespace;
	}

	private void linkCommandDirectoryStructure(AbstractOpcCommand command) {
		final Map<String, UaFolderNode> commandDirectories = folderManager.getCommandDirectories();
		if (!commandDirectories.containsKey(command.getDirectory())) {
			final String[] components = command.getDirectory().split("/");
			String actual = null;
			String last = actual;
			for (int counter = 0; counter < components.length; counter++) {
				actual = (actual == null ? "" : (actual + "/")) + components[counter].trim();
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

	private void popolateVertexPropertiesFromPropertyKeyValues(final Object[] propertyKeyValues,
			final AbstractOpcVertex vertex) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && propertyKeyValues[i] != null) {
				createOrUpdateWaldotVertexProperty(vertex, (String) propertyKeyValues[i], propertyKeyValues[i + 1]);
			}
		}
	}

	@Override
	public void registerCommand(final WaldotCommand command) {
		waldotNamespace.getStorageManager().addNode((AbstractOpcCommand) command);
		linkCommandDirectoryStructure((AbstractOpcCommand) command);

	}

	@Override
	public void removeCommand(final WaldotCommand command) {
		interfaceRootNode.removeComponent((AbstractOpcCommand) command);
		waldotNamespace.getStorageManager().removeNode(command.getNodeId());
	}

	@Override
	public void removeEdge(final NodeId nodeId) {
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId).get();
		// FIXME rimuovere dalla cartella giusta
		waldotNamespace.getStorageManager().removeNode(nodeId);
		node.delete();
		// FIXME rimuovere tutti i nodi GremlinProperty collegati

	}

	@Override
	public void removeVertex(final NodeId nodeId) {
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId).get();
		// FIXME rimuovere dalla cartella giusta
		waldotNamespace.getStorageManager().removeNode(nodeId);
		waldotNamespace.getRulesEngine().deregisterRule(node.getNodeId());
		node.delete();
		// FIXME rimuovere tutti i nodi OpcVertexProperty collegati
	}

	@Override
	public void removeVertexProperty(final NodeId nodeId) {
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId).get();
		waldotNamespace.getStorageManager().removeNode(nodeId);
		node.delete();
		// FIXME rimuovere tutti i nodi GremlinProperty collegati
	}

	@Override
	public void resetNameSpace() {
		logger.info("resetNameSpace");
		// FIXME cancellare tutto...

	}

	@Override
	public void updateEventGenerator(final Node sourceNode, String eventName, String eventDisplayName, String message,
			int severity) {
		try {
			final BaseEventTypeNode eventNode = waldotNamespace.getOpcuaServer().getServer().getEventFactory()
					.createEvent(waldotNamespace.generateNodeId(UUID.randomUUID()),
							Identifiers.BaseModelChangeEventType);
			eventNode.setBrowseName(new QualifiedName(1, eventName));
			eventNode.setDisplayName(LocalizedText.english(eventDisplayName));
			eventNode.setEventId(ByteString.of(UUID.randomUUID().toString().getBytes()));
			eventNode.setEventType(Identifiers.BaseEventType);
			eventNode.setSourceNode(sourceNode.getNodeId());
			eventNode.setSourceName(sourceNode.getDisplayName().getText());
			eventNode.setTime(DateTime.now());
			eventNode.setReceiveTime(DateTime.NULL_VALUE);
			eventNode.setMessage(LocalizedText.english(message));
			eventNode.setSeverity(ushort(severity));
			waldotNamespace.getOpcuaServer().getServer().getInternalEventBus().post(eventNode);
			eventNode.delete();
		} catch (final Throwable e) {
			logger.error("Error creating EventNode: {}", e.getMessage(), e);
		}
	}

}
