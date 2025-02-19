package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Graph.Variables;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.ReferenceType;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
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
import net.rossonet.waldot.api.rules.WaldotRulesEngine;
import net.rossonet.waldot.api.strategies.WaldotMappingStrategy;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputerView;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcEdge;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertexProperty;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.rules.DefaultRule;

@WaldotMiloStrategy
public class MiloSingleServerBaseV0Strategy implements WaldotMappingStrategy {

	public static final String ACTION_FIELD = "Action";
	public static final Predicate<Reference> COMPONENT_OF_PREDICATE = (reference) -> reference.isInverse()
			&& Identifiers.HasComponent.equals(reference.getReferenceTypeId());
	public static final String CONDITION_FIELD = "Condition";
	public static final String DEFAULT_ACTION_VALUE = "log.info('action fired')";
	public static final String DEFAULT_CONDITION_VALUE = "true";
	public static final int DEFAULT_PRIORITY_VALUE = 100;
	public static final String DESCRIPTION_PARAMETER = "description";
	public static final String DIRECTORY_PARAMETER = "directory";
	public static final String HAS_WALDOT_RULE = "HasRule";
	public static final String LABEL_FIELD = "Label";
	public static final String OBSERVER_EDGE_PARAMETER = "fire";
	public static final String PRIORITY_FIELD = "Priority";
	public static final String RULE_NODE_PARAMETER = "rule";
	public static final String TYPE_DEFINITION_PARAMETER = "type-node-id";

	private UaFolderNode assetRootNode;
	private final Map<String, UaFolderNode> edgeDirectories = new HashMap<>();

	private final MiloSingleServerBaseV0FolderManager folderManager = new MiloSingleServerBaseV0FolderManager(this);
	private UaObjectNode interfaceRootNode;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Graph.Variables opcGraphVariables;
	private UaFolderNode rootNode;

	private WaldotNamespace waldotNamespace;

	@Override
	public Edge addEdge(WaldotVertex sourceVertex, WaldotVertex targetVertex, String label,
			Object[] propertyKeyValues) {
		String elaboratedLabel = label;
		if (OBSERVER_EDGE_PARAMETER.equals(label)) {
			elaboratedLabel = HAS_WALDOT_RULE;
			waldotNamespace.getRulesEngine().registerObserver(sourceVertex, targetVertex.getNodeId());
		}
		final NodeId nodeId = waldotNamespace.generateNodeId(sourceVertex.getNodeId().getIdentifier() + ":"
				+ elaboratedLabel + ":" + targetVertex.getNodeId().getIdentifier());
		String description = getKeyValuesProperty(propertyKeyValues, DESCRIPTION_PARAMETER.toLowerCase());
		if (description == null || description.isEmpty()) {
			description = sourceVertex.getDescription().getText() + " -- " + label + " -> "
					+ targetVertex.getDescription().getText();
			logger.info(DESCRIPTION_PARAMETER + " not found in propertyKeyValues, using default '{}'", description);
		}
		final OpcEdge edge = new OpcEdge(waldotNamespace.getGremlinGraph(), nodeId, sourceVertex, targetVertex,
				elaboratedLabel, description, MiloSingleServerBaseV0ReferenceNodeBuilder.writeMask,
				MiloSingleServerBaseV0ReferenceNodeBuilder.userWriteMask,
				MiloSingleServerBaseV0ReferenceNodeBuilder.eventNotifierDisable,
				MiloSingleServerBaseV0ReferenceNodeBuilder.version);
		waldotNamespace.getStorageManager().addNode(edge);
		final String directory = getKeyValuesProperty(propertyKeyValues, DIRECTORY_PARAMETER.toLowerCase());
		if (directory == null || directory.isEmpty()) {
			folderManager.getEdgesFolderNode().addOrganizes(edge);
		} else {
			if (!edgeDirectories.containsKey(directory)) {
				edgeDirectories
						.put(directory,
								new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
										waldotNamespace.generateNodeId(folderManager.getEdgesFolderNode().getNodeId()
												.getIdentifier().toString() + "/" + directory),
										waldotNamespace.generateQualifiedName(directory),
										LocalizedText.english(directory)));
				waldotNamespace.getStorageManager().addNode(edgeDirectories.get(directory));
				folderManager.getEdgesFolderNode().addOrganizes(edgeDirectories.get(directory));
			}
			edgeDirectories.get(directory).addOrganizes(edge);
		}
		edge.addReference(new Reference(edge.getNodeId(), Identifiers.HasTypeDefinition,
				MiloSingleServerBaseV0ReferenceNodeBuilder.edgeTypeNode.getNodeId().expanded(), true));
		final QualifiedProperty<String> LABEL = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				LABEL_FIELD, MiloSingleServerBaseV0ReferenceNodeBuilder.labelEdgeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, String.class);
		edge.setProperty(LABEL, elaboratedLabel);
		final QualifiedProperty<NodeId> SOURCE = new QualifiedProperty<NodeId>(waldotNamespace.getNamespaceUri(),
				"SourceNode", MiloSingleServerBaseV0ReferenceNodeBuilder.sourceNodeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		edge.setProperty(SOURCE, sourceVertex.getNodeId());
		final QualifiedProperty<NodeId> TARGET = new QualifiedProperty<NodeId>(waldotNamespace.getNamespaceUri(),
				"TargetNode", MiloSingleServerBaseV0ReferenceNodeBuilder.targetNodeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		edge.setProperty(TARGET, targetVertex.getNodeId());
		edge.addReference(
				new Reference(edge.getNodeId(), MiloSingleServerBaseV0ReferenceNodeBuilder.hasSourceNodeReferenceType,
						sourceVertex.getNodeId().expanded(), true));
		edge.addReference(
				new Reference(edge.getNodeId(), MiloSingleServerBaseV0ReferenceNodeBuilder.hasTargetNodeReferenceType,
						targetVertex.getNodeId().expanded(), true));
		sourceVertex.addReference(new Reference(sourceVertex.getNodeId(),
				MiloSingleServerBaseV0ReferenceNodeBuilder.hasReferenceDescriptionReferenceType,
				edge.getNodeId().expanded(), true));
		targetVertex.addReference(new Reference(targetVertex.getNodeId(),
				MiloSingleServerBaseV0ReferenceNodeBuilder.hasReferenceDescriptionReferenceType,
				edge.getNodeId().expanded(), true));
		final NodeId mainReferenceTypeNodeId = getOrCreateReferenceType(elaboratedLabel);
		final QualifiedProperty<NodeId> REFERENCE_TYPE = new QualifiedProperty<NodeId>(
				waldotNamespace.getNamespaceUri(), "ReferenceType",
				MiloSingleServerBaseV0ReferenceNodeBuilder.referenceTypeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		edge.setProperty(REFERENCE_TYPE, mainReferenceTypeNodeId);
		final QualifiedProperty<Boolean> FORWARD = new QualifiedProperty<Boolean>(waldotNamespace.getNamespaceUri(),
				"IsForward", MiloSingleServerBaseV0ReferenceNodeBuilder.isForwardTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, Boolean.class);
		edge.setProperty(FORWARD, true);
		sourceVertex.addReference(new Reference(sourceVertex.getNodeId(), mainReferenceTypeNodeId,
				targetVertex.getNodeId().expanded(), true));
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && propertyKeyValues[i] != null) {
				createOrUpdateWaldotEdgeProperty(edge, (String) propertyKeyValues[i], propertyKeyValues[i + 1]);
			}
		}
		return edge;
	}

	@Override
	public OpcVertex addVertex(NodeId nodeId, Object[] propertyKeyValues) {
		ElementHelper.legalPropertyKeyValueArray(propertyKeyValues);
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
			logger.info(DESCRIPTION_PARAMETER + " not found in propertyKeyValues, using default '{}'", description);
		}
		NodeId typeDefinition = getTypeDefinition(propertyKeyValues);
		if (typeDefinition == null) {
			typeDefinition = MiloSingleServerBaseV0ReferenceNodeBuilder.vertexTypeNode.getNodeId();
			logger.info(TYPE_DEFINITION_PARAMETER + " not found in propertyKeyValues, using default type '{}'",
					typeDefinition);
		}
		if (typeDefinition.equals(MiloSingleServerBaseV0ReferenceNodeBuilder.ruleTypeNode.getNodeId())) {
			String action = getKeyValuesProperty(propertyKeyValues, ACTION_FIELD.toLowerCase());
			if (action == null) {
				action = DEFAULT_ACTION_VALUE;
				logger.warn(ACTION_FIELD.toLowerCase() + " not found in propertyKeyValues, using default action '{}'",
						action);
			}
			String condition = getKeyValuesProperty(propertyKeyValues, CONDITION_FIELD.toLowerCase());
			if (condition == null) {
				condition = DEFAULT_CONDITION_VALUE;
				logger.warn(
						CONDITION_FIELD.toLowerCase() + " not found in propertyKeyValues, using default condition '{}'",
						condition);
			}
			Integer priority = getPriorityDefinition(propertyKeyValues);
			if (priority == null) {
				priority = DEFAULT_PRIORITY_VALUE;
				logger.warn(
						PRIORITY_FIELD.toLowerCase() + " not found in propertyKeyValues, using default priority '{}'",
						priority);
			}
			final DefaultRule rule = createRuleObject(typeDefinition, waldotNamespace.getGremlinGraph(),
					waldotNamespace.getOpcUaNodeContext(), nodeId, browseName, displayName,
					new LocalizedText(description), MiloSingleServerBaseV0ReferenceNodeBuilder.writeMask,
					MiloSingleServerBaseV0ReferenceNodeBuilder.userWriteMask,
					MiloSingleServerBaseV0ReferenceNodeBuilder.eventNotifierActive,
					MiloSingleServerBaseV0ReferenceNodeBuilder.version, waldotNamespace.getRulesEngine(), condition,
					action, priority);
			waldotNamespace.getStorageManager().addNode(rule);
			final String directory = getKeyValuesProperty(propertyKeyValues, DIRECTORY_PARAMETER.toLowerCase());
			if (directory == null || directory.isEmpty()) {
				folderManager.getRulesFolderNode().addOrganizes(rule);
			} else {
				if (!folderManager.getRulesDirectories().containsKey(directory)) {
					folderManager.getRulesDirectories()
							.put(directory,
									new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
											waldotNamespace.generateNodeId(folderManager.getRulesFolderNode()
													.getNodeId().getIdentifier().toString() + "/" + directory),
											waldotNamespace.generateQualifiedName(directory),
											LocalizedText.english(directory)));
					waldotNamespace.getStorageManager().addNode(folderManager.getRulesDirectories().get(directory));
					folderManager.getRulesFolderNode().addOrganizes(folderManager.getRulesDirectories().get(directory));
				}
				folderManager.getRulesDirectories().get(directory).addOrganizes(rule);
			}

			final QualifiedProperty<String> ACTION = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
					ACTION_FIELD, MiloSingleServerBaseV0ReferenceNodeBuilder.actionRuleTypeNode.getNodeId().expanded(),
					ValueRanks.Scalar, String.class);
			rule.setProperty(ACTION, action);

			final QualifiedProperty<String> CONDITION = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
					CONDITION_FIELD,
					MiloSingleServerBaseV0ReferenceNodeBuilder.conditionRuleTypeNode.getNodeId().expanded(),
					ValueRanks.Scalar, String.class);
			rule.setProperty(CONDITION, condition);

			final QualifiedProperty<Integer> PRIORITY = new QualifiedProperty<Integer>(
					waldotNamespace.getNamespaceUri(), PRIORITY_FIELD,
					MiloSingleServerBaseV0ReferenceNodeBuilder.priorityRuleTypeNode.getNodeId().expanded(),
					ValueRanks.Scalar, Integer.class);
			rule.setProperty(PRIORITY, priority);

			final QualifiedProperty<String> LABEL = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
					LABEL_FIELD, MiloSingleServerBaseV0ReferenceNodeBuilder.labelRuleTypeNode.getNodeId().expanded(),
					ValueRanks.Scalar, String.class);
			rule.setProperty(LABEL, label);

			waldotNamespace.getRulesEngine().registerOrUpdateRule(rule);
			for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
				if (propertyKeyValues[i] instanceof String && propertyKeyValues[i] != null) {
					createOrUpdateWaldotVertexProperty(rule, (String) propertyKeyValues[i], propertyKeyValues[i + 1]);
				}
			}
			return rule;
		} else {
			final OpcVertex vertex = createVertexObject(typeDefinition, waldotNamespace.getGremlinGraph(),
					waldotNamespace.getOpcUaNodeContext(), nodeId, browseName, displayName,
					new LocalizedText(description), MiloSingleServerBaseV0ReferenceNodeBuilder.writeMask,
					MiloSingleServerBaseV0ReferenceNodeBuilder.userWriteMask,
					MiloSingleServerBaseV0ReferenceNodeBuilder.eventNotifierActive,
					MiloSingleServerBaseV0ReferenceNodeBuilder.version);
			waldotNamespace.getStorageManager().addNode(vertex);
			vertex.addReference(
					new Reference(vertex.getNodeId(), Identifiers.HasTypeDefinition, typeDefinition.expanded(), true));

			final String directory = getKeyValuesProperty(propertyKeyValues, DIRECTORY_PARAMETER.toLowerCase());
			if (directory == null || directory.isEmpty()) {
				folderManager.getVerticesFolderNode().addOrganizes(vertex);
			} else {
				if (!folderManager.getVertexDirectories().containsKey(directory)) {
					folderManager.getVertexDirectories()
							.put(directory,
									new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
											waldotNamespace.generateNodeId(folderManager.getVerticesFolderNode()
													.getNodeId().getIdentifier().toString() + "/" + directory),
											waldotNamespace.generateQualifiedName(directory),
											LocalizedText.english(directory)));
					waldotNamespace.getStorageManager().addNode(folderManager.getVertexDirectories().get(directory));
					folderManager.getVerticesFolderNode()
							.addOrganizes(folderManager.getVertexDirectories().get(directory));
				}
				folderManager.getVertexDirectories().get(directory).addOrganizes(vertex);
			}
			final QualifiedProperty<String> LABEL = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
					LABEL_FIELD, MiloSingleServerBaseV0ReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(),
					ValueRanks.Scalar, String.class);
			vertex.setProperty(LABEL, label);

			for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
				if (propertyKeyValues[i] instanceof String && propertyKeyValues[i] != null) {
					createOrUpdateWaldotVertexProperty(vertex, (String) propertyKeyValues[i], propertyKeyValues[i + 1]);
				}
			}
			return vertex;
		}

	}

	@Override
	public OpcGraphComputerView createGraphComputerView(WaldotGraph graph, GraphFilter graphFilter,
			Set<VertexComputeKey> VertexComputeKey) {
		logger.info("createGraphComputerView: graph={}, graphFilter={}, VertexComputeKey={}", graph, graphFilter,
				VertexComputeKey);
		// TODO GraphComputerView
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <DATA_TYPE> OpcProperty<DATA_TYPE> createOrUpdateWaldotEdgeProperty(WaldotEdge opcEdge, String key,
			DATA_TYPE value) {
		final NodeId nodeId = waldotNamespace
				.generateNodeId(opcEdge.getNodeId().getIdentifier().toString() + "/" + key);
		if (waldotNamespace.hasNodeId(nodeId)) {
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
			final UInteger writeMask = UInteger.MIN;
			final UInteger userWriteMask = UInteger.MIN;
			final NodeId dataType = Identifiers.BaseDataType;
			final int valueRank = ValueRanks.Scalar;
			final UInteger[] arrayDimensions = null;
			final UByte accessLevel = AccessLevel.toValue(AccessLevel.CurrentRead);
			final UByte userAccessLevel = AccessLevel.toValue(AccessLevel.CurrentRead);
			final Double minimumSamplingInterval = -1.0;
			final boolean historizing = false;
			@SuppressWarnings({ "rawtypes" })
			final OpcProperty<DATA_TYPE> property = new OpcProperty(waldotNamespace.getGremlinGraph(), opcEdge, key,
					value, context, nodeId, description, writeMask, userWriteMask, dataType, valueRank, arrayDimensions,
					accessLevel, userAccessLevel, minimumSamplingInterval, historizing);
			waldotNamespace.getStorageManager().addNode(property);
			opcEdge.addReference(new Reference(opcEdge.getNodeId(),
					MiloSingleServerBaseV0ReferenceNodeBuilder.hasPropertyReferenceType,
					property.getNodeId().expanded(), true));
			return property;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <DATA_TYPE> OpcVertexProperty<DATA_TYPE> createOrUpdateWaldotVertexProperty(WaldotVertex opcVertex,
			String key, DATA_TYPE value) {
		final NodeId nodeId = waldotNamespace
				.generateNodeId(opcVertex.getNodeId().getIdentifier().toString() + "/" + key);
		if (waldotNamespace.hasNodeId(nodeId)) {
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
			final UInteger writeMask = UInteger.MIN;
			final UInteger userWriteMask = UInteger.MIN;
			final NodeId dataType = Identifiers.BaseDataType;
			final int valueRank = ValueRanks.Scalar;
			final UInteger[] arrayDimensions = null;
			final UByte accessLevel = AccessLevel.toValue(AccessLevel.CurrentRead);
			final UByte userAccessLevel = AccessLevel.toValue(AccessLevel.CurrentRead);
			final Double minimumSamplingInterval = -1.0;
			final boolean historizing = false;
			@SuppressWarnings({ "rawtypes" })
			final OpcVertexProperty<DATA_TYPE> property = new OpcVertexProperty(waldotNamespace.getGremlinGraph(),
					opcVertex, key, value, context, nodeId, description, writeMask, userWriteMask, dataType, valueRank,
					arrayDimensions, accessLevel, userAccessLevel, minimumSamplingInterval, historizing, false);
			waldotNamespace.getStorageManager().addNode(property);
			property.addAttributeObserver(opcVertex);
			opcVertex.addReference(new Reference(opcVertex.getNodeId(),
					MiloSingleServerBaseV0ReferenceNodeBuilder.hasPropertyReferenceType,
					property.getNodeId().expanded(), true));
			return property;
		}
	}

	private DefaultRule createRuleObject(NodeId typeDefinition, final WaldotGraph graph, UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version, WaldotRulesEngine ruleEngine,
			String condition, String action, int priority) {
		return new DefaultRule(typeDefinition, graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, ruleEngine, condition, action, priority,
				waldotNamespace.getConfiguration().getDefaultFactsValidUntilMs(),
				waldotNamespace.getConfiguration().getDefaultFactsValidDelayMs());
	}

	private OpcVertex createVertexObject(NodeId typeDefinition, final WaldotGraph graph, UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version) {
		if (typeDefinition != null && waldotNamespace.hasNodeId(typeDefinition)) {
			for (final PluginListener p : waldotNamespace.getPlugins()) {
				if (p.containsObjectDefinition(typeDefinition)) {
					return (OpcVertex) p.createVertexObject(typeDefinition, graph, context, nodeId, browseName,
							displayName, description, writeMask, userWriteMask, eventNotifier, version);
				}
			}
		}
		return new OpcVertex(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask,
				eventNotifier, version);
	}

	@Override
	public void dropGraphComputerView() {
		logger.info("dropGraphComputerView");
		// TODO GraphComputerView
	}

	@Override
	public WaldotVertex getEdgeInVertex(WaldotEdge edge) {
		final QualifiedProperty<NodeId> TARGET = new QualifiedProperty<NodeId>(waldotNamespace.getNamespaceUri(),
				"TargetNode", MiloSingleServerBaseV0ReferenceNodeBuilder.targetNodeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		final NodeId nodeId = edge.getProperty(TARGET).get();
		return (OpcVertex) waldotNamespace.getStorageManager().getNode(nodeId).get();
	}

	@Override
	public WaldotVertex getEdgeOutVertex(WaldotEdge edge) {
		final QualifiedProperty<NodeId> SOURCE = new QualifiedProperty<NodeId>(waldotNamespace.getNamespaceUri(),
				"SourceNode", MiloSingleServerBaseV0ReferenceNodeBuilder.sourceNodeTypeNode.getNodeId().expanded(),
				ValueRanks.Scalar, NodeId.class);
		final NodeId nodeId = edge.getProperty(SOURCE).get();
		return (OpcVertex) waldotNamespace.getStorageManager().getNode(nodeId).get();
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
	public Map<NodeId, WaldotEdge> getEdges(WaldotVertex opcVertex, Direction direction, String[] edgeLabels) {
		logger.info("getEdges: opcVertex={}, direction={}, edgeLabels={}", opcVertex, direction, edgeLabels);
		// TODO da verificare come usare questa chiamata
		return null;
	}

	private String getKeyValuesProperty(Object[] propertyKeyValues, String label) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && label.equals(propertyKeyValues[i])) {
				return propertyKeyValues[i + 1].toString();
			}
		}
		return null;
	}

	private NodeId getOrCreateReferenceType(String label) {
		for (final ReferenceType r : waldotNamespace.getReferenceTypes().values()) {
			if (r.getBrowseName().getName().equals(label)) {
				return r.getNodeId();
			}
		}
		return MiloSingleServerBaseV0ReferenceNodeBuilder.generateReferenceTypeNode(label, "is a " + label + " of ",
				label + " reference type", Identifiers.NonHierarchicalReferences, false, false, waldotNamespace);
	}

	private NodeId getParameterNodeId(String key) {
		return waldotNamespace
				.generateNodeId(folderManager.getVariablesFolderNode().getNodeId().getIdentifier() + ":" + key);
	}

	private Integer getPriorityDefinition(Object[] propertyKeyValues) {
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
	public <DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getProperties(WaldotEdge opcEdge) {
		final List<WaldotProperty<DATA_TYPE>> result = new ArrayList<>();
		for (final Reference p : opcEdge.getReferences()) {
			if (p.isForward() && p.getReferenceTypeId()
					.equals(MiloSingleServerBaseV0ReferenceNodeBuilder.hasPropertyReferenceType)) {
				result.add((WaldotProperty<DATA_TYPE>) waldotNamespace.getStorageManager()
						.getNode(p.getTargetNodeId().toNodeId(waldotNamespace.getNamespaceTable()).get()).get());
			}
		}
		return result;
	}

	@Override
	public <DATA_TYPE> WaldotEdge getPropertyReference(WaldotProperty<DATA_TYPE> property) {
		for (final Reference r : property.getReferences()) {
			if (r.getReferenceTypeId().equals(Identifiers.HasComponent) && r.isInverse()) {
				final NodeId nodeId = r.getSourceNodeId();
				return (OpcEdge) waldotNamespace.getStorageManager().get(nodeId);
			}
		}
		return null;
	}

	public UaFolderNode getRootNode() {
		return rootNode;
	}

	private NodeId getTypeDefinition(Object[] propertyKeyValues) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && TYPE_DEFINITION_PARAMETER.equals(propertyKeyValues[i])) {
				if (RULE_NODE_PARAMETER.equals(propertyKeyValues[i + 1].toString())) {
					return MiloSingleServerBaseV0ReferenceNodeBuilder.ruleTypeNode.getNodeId();
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
	public <DATA_TYPE> Map<String, WaldotVertexProperty<DATA_TYPE>> getVertexProperties(WaldotVertex opcVertex) {
		final Map<String, WaldotVertexProperty<DATA_TYPE>> result = new HashMap<>();
		for (final Reference p : opcVertex.getReferences()) {
			if (p.isForward() && p.getReferenceTypeId()
					.equals(MiloSingleServerBaseV0ReferenceNodeBuilder.hasPropertyReferenceType)) {
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
	public <DATA_TYPE> WaldotVertex getVertexPropertyReference(WaldotVertexProperty<DATA_TYPE> opcVertexProperty) {
		for (final Reference r : opcVertexProperty.getReferences()) {
			if (r.getReferenceTypeId().equals(Identifiers.HasComponent) && r.isInverse()) {
				final NodeId nodeId = r.getSourceNodeId();
				return (OpcVertex) waldotNamespace.getStorageManager().get(nodeId);
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
	public Map<NodeId, WaldotVertex> getVertices(WaldotVertex opcVertex, Direction direction, String[] edgeLabels) {
		logger.info("getVertices: opcVertex={}, direction={}, edgeLabels={}", opcVertex, direction, edgeLabels);
		// TODO da verificare come usare questa chiamata
		return null;
	}

	public WaldotNamespace getWaldotNamespace() {
		return waldotNamespace;
	}

	@Override
	public WaldotNamespace initialize(WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		MiloSingleServerBaseV0ReferenceNodeBuilder.generateRefereceNodes(this);
		rootNode = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace.getConfiguration().getRootNodeId()),
				waldotNamespace.generateQualifiedName(waldotNamespace.getConfiguration().getRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration().getRootNodeDisplayName()));

		interfaceRootNode = new UaObjectNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace.getConfiguration().getInterfaceRootNodeId()),
				waldotNamespace
						.generateQualifiedName(waldotNamespace.getConfiguration().getInterfaceRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration().getInterfaceRootNodeDisplayName()));
		interfaceRootNode.addReference(new Reference(interfaceRootNode.getNodeId(), Identifiers.HasTypeDefinition,
				MiloSingleServerBaseV0ReferenceNodeBuilder.interfaceTypeNode.getNodeId().expanded(), true));
		assetRootNode = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace.getConfiguration().getAssetRootNodeId()),
				waldotNamespace.generateQualifiedName(waldotNamespace.getConfiguration().getAssetRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration().getAssetRootNodeDisplayName()));
		waldotNamespace.getStorageManager().addNode(getRootNode());
		waldotNamespace.getStorageManager().addNode(assetRootNode);
		waldotNamespace.getStorageManager().addNode(interfaceRootNode);
		getRootNode().addReference(new Reference(getRootNode().getNodeId(), Identifiers.Organizes,
				Identifiers.ObjectsFolder.expanded(), false));
		assetRootNode.addReference(new Reference(assetRootNode.getNodeId(), Identifiers.Organizes,
				Identifiers.ObjectsFolder.expanded(), false));
		interfaceRootNode.addReference(new Reference(interfaceRootNode.getNodeId(), Identifiers.Organizes,
				Identifiers.ObjectsFolder.expanded(), false));
		folderManager.initialize();
		return waldotNamespace;
	}

	@Override
	public Object namespaceParametersGet(String key) {
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
	public void namespaceParametersPut(String key, Object value) {
		if (waldotNamespace.hasNodeId(getParameterNodeId(key))) {
			final UaVariableNode property = (UaVariableNode) waldotNamespace.getStorageManager()
					.getNode(getParameterNodeId(key)).get();
			property.setValue(new DataValue(new Variant(value)));
		} else {
			final UaVariableNode property = new UaVariableNode(waldotNamespace.getOpcUaNodeContext(),
					getParameterNodeId(key), waldotNamespace.generateQualifiedName(key), LocalizedText.english(key));
			waldotNamespace.getStorageManager().addNode(property);
			folderManager.getVariablesFolderNode().addOrganizes(property);
			property.setValue(new DataValue(new Variant(value)));
		}

	}

	@Override
	public void namespaceParametersRemove(String key) {
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

	@Override
	public void registerCommand(WaldotCommand command) {
		waldotNamespace.getStorageManager().addNode((AbstractOpcCommand) command);
		interfaceRootNode.addComponent((AbstractOpcCommand) command);

	}

	@Override
	public void removeCommand(WaldotCommand command) {
		interfaceRootNode.removeComponent((AbstractOpcCommand) command);
		waldotNamespace.getStorageManager().removeNode(command.getNodeId());
	}

	@Override
	public void removeEdge(NodeId nodeId) {
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId).get();
		// FIXME rimuovere dalla cartella giusta
		waldotNamespace.getStorageManager().removeNode(nodeId);
		node.delete();
		// FIXME rimuovere tutti i nodi GremlinProperty collegati

	}

	@Override
	public void removeVertex(NodeId nodeId) {
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId).get();
		// FIXME rimuovere dalla cartella giusta
		waldotNamespace.getStorageManager().removeNode(nodeId);
		waldotNamespace.getRulesEngine().deregisterRule(node.getNodeId());
		node.delete();
		// FIXME rimuovere tutti i nodi OpcVertexProperty collegati
	}

	@Override
	public void removeVertexProperty(NodeId nodeId) {
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

}
