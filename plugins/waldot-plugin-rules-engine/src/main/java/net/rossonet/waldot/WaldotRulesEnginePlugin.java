package net.rossonet.waldot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.base.GremlinElement;
import net.rossonet.waldot.api.rules.WaldotRulesEngine;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.logger.TraceLogger;
import net.rossonet.waldot.logger.TraceLogger.ContexLogger;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;
import net.rossonet.waldot.rules.DefaultRule;

/**
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotRulesEnginePlugin implements PluginListener {
	public static final String ACTION_FIELD = "Action";
	public static final String ALIAS_EDGE_PARAMETER = "alias";
	public static final Predicate<Reference> COMPONENT_OF_PREDICATE = (reference) -> reference.isInverse()
			&& Identifiers.HasComponent.equals(reference.getReferenceTypeId());
	public static final String CONDITION_FIELD = "Condition";
	public static final String DEFAULT_ACTION_VALUE = "log.info('action fired')";
	public static final boolean DEFAULT_CLEAR_FACTS_AFTER_EXECUTION = false;
	public static final String DEFAULT_CONDITION_VALUE = "true";
	public static final int DEFAULT_DELAY_BEFORE_EVALUATION = 0;
	public static final int DEFAULT_DELAY_BEFORE_EXECUTE = 0;
	public static final boolean DEFAULT_PARALLEL_EXECUTION = false;
	public static final int DEFAULT_PRIORITY_VALUE = 100;
	public static final int DEFAULT_REFACTORY_PERIOD_MS = 0;
	public static final String DESCRIPTION_PARAMETER = "description";
	public static final String DIRECTORY_PARAMETER = "directory";
	public static final String GENERAL_CMD_DIRECTORY = "general";
	public static final String HAS_WALDOT_ALIAS = "HasAlias";
	public static final String HAS_WALDOT_RULE = "HasRule";
	public static final String ID_PARAMETER = "id";
	public static final String LABEL_FIELD = "Label";
	static WaldotNamespace mainNamespace;
	public static final String OBSERVER_EDGE_PARAMETER = "fire";
	public static final String PRIORITY_FIELD = "Priority";
	public static final String RULE_NODE_PARAMETER = "rule";
	public static final String TYPE_DEFINITION_PARAMETER = "type-node-id";
	private UaVariableNode actionRuleTypeNode;
	private final Map<NodeId, Map<String, NodeId>> aliasTable = new ConcurrentHashMap<>();
	private UaVariableNode conditionRuleTypeNode;
	private UaVariableNode labelRuleTypeNode;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private UaVariableNode priorityRuleTypeNode;

	private final Map<String, UaFolderNode> rulesDirectories = new HashMap<>();

	private UaFolderNode rulesFolderNode;

	private final Logger rulesLogger = new TraceLogger(ContexLogger.RULES);

	private UaObjectTypeNode ruleTypeNode;
	private WaldotNamespace waldotNamespace;

	private void checkDirectoryParameterAndLinkNode(final Object[] propertyKeyValues, final GremlinElement vertex,
			final UaFolderNode folderNode, final Map<String, UaFolderNode> directories) {
		final String directory = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				DIRECTORY_PARAMETER.toLowerCase());
		if (directory == null || directory.isEmpty()) {
			folderNode.addOrganizes(vertex);
			return;
		}
		final String[] components = directory.split(MiloStrategy.DIRECTORY_SPLIT_SIMBOL);
		String actual = null;
		String last = actual;
		for (int counter = 0; counter < components.length; counter++) {
			actual = (actual == null ? "" : (actual + MiloStrategy.DIRECTORY_SPLIT_SIMBOL))
					+ components[counter].trim();
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

	private UaFolderNode createRulesFolder() {
		return new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(rulesFolderNode.getNodeId().getIdentifier().toString() + "/Rules"),
				waldotNamespace.generateQualifiedName("Rules"), LocalizedText.english("WaldoOT Rules"));
	}

	private AbstractOpcVertex createVertexRule(final NodeId nodeId, final NodeId typeDefinition, final String label,
			final String description, final QualifiedName browseName, final LocalizedText displayName,
			final Object[] propertyKeyValues) {
		String action = MiloStrategy.getKeyValuesProperty(propertyKeyValues, ACTION_FIELD.toLowerCase());
		// get rule specific fields
		if (action == null) {
			action = DEFAULT_ACTION_VALUE;
			logger.info(ACTION_FIELD.toLowerCase() + " not found in propertyKeyValues, using default action '{}'",
					action);
		}
		String condition = MiloStrategy.getKeyValuesProperty(propertyKeyValues, CONDITION_FIELD.toLowerCase());
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
				MiloSingleServerBaseReferenceNodeBuilder.version, getRulesEngine(), condition1, action1, priority1,
				waldotNamespace.getConfiguration().getDefaultFactsValidUntilMs(),
				waldotNamespace.getConfiguration().getDefaultFactsValidDelayMs());
		// TODO parametrizzare tutto per la creazione della regola
		rule.setDelayBeforeEvaluation(DEFAULT_DELAY_BEFORE_EVALUATION);
		rule.setDelayBeforeExecute(DEFAULT_DELAY_BEFORE_EXECUTE);
		rule.setClearFactsAfterExecution(DEFAULT_CLEAR_FACTS_AFTER_EXECUTION);
		rule.setRefractoryPeriodMs(DEFAULT_REFACTORY_PERIOD_MS);
		rule.setParallelExecution(DEFAULT_PARALLEL_EXECUTION);
		waldotNamespace.getStorageManager().addNode(rule);
		checkDirectoryParameterAndLinkNode(propertyKeyValues, rule, rulesFolderNode, rulesDirectories);
		// popolate rule properties
		final QualifiedProperty<String> ACTION = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				ACTION_FIELD, actionRuleTypeNode.getNodeId().expanded(), ValueRanks.Scalar, String.class);
		rule.setProperty(ACTION, action);

		final QualifiedProperty<String> CONDITION = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				CONDITION_FIELD, conditionRuleTypeNode.getNodeId().expanded(), ValueRanks.Scalar, String.class);
		rule.setProperty(CONDITION, condition);

		final QualifiedProperty<Integer> PRIORITY = new QualifiedProperty<Integer>(waldotNamespace.getNamespaceUri(),
				PRIORITY_FIELD, priorityRuleTypeNode.getNodeId().expanded(), ValueRanks.Scalar, Integer.class);
		rule.setProperty(PRIORITY, priority);

		final QualifiedProperty<String> LABEL = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				LABEL_FIELD, labelRuleTypeNode.getNodeId().expanded(), ValueRanks.Scalar, String.class);
		rule.setProperty(LABEL, label);
		// register rule in rules engine
		getRulesEngine().registerOrUpdateRule(rule);
		popolateVertexPropertiesFromPropertyKeyValues(propertyKeyValues, rule);
		return rule;
	}

	void generateRefereceNodes() {
		generateRulesTypeNode();
		MiloSingleServerBaseReferenceNodeBuilder.generateReferenceTypeNode(HAS_WALDOT_RULE, "IsFiredBy",
				"A rule fired by the events", Identifiers.HasComponent, false, false, waldotNamespace);
		MiloSingleServerBaseReferenceNodeBuilder.generateReferenceTypeNode(HAS_WALDOT_ALIAS, "IsAliasFor",
				"A rule fired by the events", Identifiers.HasComponent, false, false, waldotNamespace);
	}

	private void generateRulesTypeNode() {
		ruleTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTRuleObjectType"))
				.setBrowseName(waldotNamespace.generateQualifiedName("WaldOTRuleObjectType"))
				.setDisplayName(LocalizedText.english("WaldOT Rule Node")).setIsAbstract(false).build();
		labelRuleTypeNode = new UaVariableNode.UaVariableNodeBuilder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTRuleObjectType." + LABEL_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(LABEL_FIELD))
				.setDisplayName(LocalizedText.english(LABEL_FIELD)).setDataType(Identifiers.String)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();
		labelRuleTypeNode.addReference(new Reference(labelRuleTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		labelRuleTypeNode.setValue(new DataValue(new Variant("NaN")));
		ruleTypeNode.addComponent(labelRuleTypeNode);

		conditionRuleTypeNode = new UaVariableNode.UaVariableNodeBuilder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTRuleObjectType." + CONDITION_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(CONDITION_FIELD))
				.setDisplayName(LocalizedText.english(CONDITION_FIELD)).setDataType(Identifiers.String)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();
		conditionRuleTypeNode.addReference(new Reference(conditionRuleTypeNode.getNodeId(),
				Identifiers.HasModellingRule, Identifiers.ModellingRule_Mandatory.expanded(), true));
		conditionRuleTypeNode.setValue(new DataValue(new Variant(DEFAULT_CONDITION_VALUE)));
		ruleTypeNode.addComponent(conditionRuleTypeNode);

		actionRuleTypeNode = new UaVariableNode.UaVariableNodeBuilder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTRuleObjectType." + ACTION_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(ACTION_FIELD))
				.setDisplayName(LocalizedText.english(ACTION_FIELD)).setDataType(Identifiers.String)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();
		actionRuleTypeNode.addReference(new Reference(actionRuleTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		actionRuleTypeNode.setValue(new DataValue(new Variant(DEFAULT_ACTION_VALUE)));
		ruleTypeNode.addComponent(actionRuleTypeNode);

		priorityRuleTypeNode = new UaVariableNode.UaVariableNodeBuilder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId("ObjectTypes/WaldOTRuleObjectType." + PRIORITY_FIELD))
				.setAccessLevel(AccessLevel.READ_WRITE)
				.setBrowseName(waldotNamespace.generateQualifiedName(PRIORITY_FIELD))
				.setDisplayName(LocalizedText.english(PRIORITY_FIELD)).setDataType(Identifiers.Int32)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();
		priorityRuleTypeNode.addReference(new Reference(priorityRuleTypeNode.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
		priorityRuleTypeNode.setValue(new DataValue(new Variant(DEFAULT_PRIORITY_VALUE)));
		ruleTypeNode.addComponent(priorityRuleTypeNode);

		waldotNamespace.getStorageManager().addNode(labelRuleTypeNode);
		waldotNamespace.getStorageManager().addNode(conditionRuleTypeNode);
		waldotNamespace.getStorageManager().addNode(actionRuleTypeNode);
		waldotNamespace.getStorageManager().addNode(priorityRuleTypeNode);
		waldotNamespace.getStorageManager().addNode(ruleTypeNode);
		ruleTypeNode.addReference(new Reference(ruleTypeNode.getNodeId(), Identifiers.HasSubtype,
				Identifiers.BaseObjectType.expanded(), false));

		waldotNamespace.getObjectTypeManager().registerObjectType(ruleTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);
		// .registerObjectType(ruleTypeNode.getNodeId(), UaObjectNode.class,
		// UaObjectNode::new);
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
	/*
	 * // check if typeDefinition is a rule if
	 * (typeDefinition.equals(MiloSingleServerBaseReferenceNodeBuilder.ruleTypeNode.
	 * getNodeId())) { return createVertexRule(nodeId, typeDefinition, label,
	 * description, browseName, displayName, propertyKeyValues); } else {
	 */

	private WaldotRulesEngine getRulesEngine() {
		// TODO Auto-generated method stub
		return null;
	}

	private NodeId getTypeDefinition(final Object[] propertyKeyValues) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && TYPE_DEFINITION_PARAMETER.equals(propertyKeyValues[i])) {
				if (RULE_NODE_PARAMETER.equals(propertyKeyValues[i + 1].toString())) {
					return ruleTypeNode.getNodeId();
				} else if (DIRECTORY_PARAMETER.equals(propertyKeyValues[i + 1].toString())) {
					return Identifiers.FolderType;
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
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		logger.info("Initializing Rules Engine Plugin...");
	}

	public void popolateVertexPropertiesFromPropertyKeyValues(final Object[] propertyKeyValues,
			final AbstractOpcVertex vertex) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String && propertyKeyValues[i] != null) {
				waldotNamespace.createOrUpdateWaldotVertexProperty(vertex, (String) propertyKeyValues[i],
						propertyKeyValues[i + 1]);
			}
		}
	}

	public void registerAlias(NodeId forNode, String alias, NodeId targetNode) {
		if (forNode == null || alias == null || targetNode == null) {
			throw new IllegalArgumentException("forNode, alias and targetNode cannot be null");
		}
		if (aliasTable.containsKey(forNode)) {
			aliasTable.get(forNode).put(alias, targetNode);
		} else {
			final Map<String, NodeId> aliasMap = new HashMap<>();
			aliasMap.put(alias, targetNode);
			aliasTable.put(forNode, aliasMap);
		}
	}

	@Override
	public void start() {
//TODO behaviour tree plugin
	}

	@Override
	public void stop() {
		// TODO behaviour tree plugin
	}

}
