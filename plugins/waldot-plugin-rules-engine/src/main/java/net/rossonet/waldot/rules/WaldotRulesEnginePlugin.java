package net.rossonet.waldot.rules;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.jexl3.JexlEngine;
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
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.jexl.ClonableMapContext;
import net.rossonet.waldot.jexl.JexlExecutor;
import net.rossonet.waldot.rules.edges.ComputeMonitoredEdge;
import net.rossonet.waldot.rules.edges.FireMonitoredEdge;
import net.rossonet.waldot.rules.edges.LinkMonitoredEdge;
import net.rossonet.waldot.rules.edges.LinkMonitoredEdge.LinkDirection;
import net.rossonet.waldot.rules.edges.MonitoredEdge;
import net.rossonet.waldot.rules.vertices.ComputeVertex;
import net.rossonet.waldot.rules.vertices.RuleVertex;
import net.rossonet.waldot.utils.ThreadHelper;

/**
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotRulesEnginePlugin implements PluginListener, AutoCloseable {

	public static final String ACTION_FIELD = "Action";
	private static final String COMPUTE_NODE_PARAMETER = "compute";
	public static final String CONDITION_FIELD = "Condition";
	public static final String DEBUG_LEVEL_LABEL = "Debug";
	public static final String DEFAULT_ACTION_VALUE = "log.info('action fired')";
	public static final boolean DEFAULT_CLEAR_FACTS_AFTER_EXECUTION = false;
	public static final String DEFAULT_CONDITION_VALUE = "true";
	public static final int DEFAULT_DELAY_BEFORE_EVALUATION = 0;
	public static final int DEFAULT_DELAY_BEFORE_EXECUTE = 0;
	public static final long DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE = 120000; // 2 minuti;
	public static final double DEFAULT_PRIORITY_FACTOR_IN_COMPUTE = 100.0;
	public static final int DEFAULT_PRIORITY_VALUE = 100;
	public static final int DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE = 4;
	private static final String EXECUTE_EDGE_LABEL = "execute";
	public static final String EXECUTION_TIMEOUT_MS_FIELD = "execution-timeout-ms";
	private static final String FIRE_EDGE_LABEL = "fire";
	public static final String FIRE_EDGE_PARAMETER = "fire";
	private static final String LINK_FROM_EDGE_LABEL = "link-from";
	private static final String LINK_TO_EDGE_LABEL = "link-to";
	private final static Logger logger = LoggerFactory.getLogger(WaldotRulesEnginePlugin.class);
	public static final String PRIORITY_FACTOR_FIELD = "Factor";
	public static final String PRIORITY_FIELD = "Priority";
	public static final String QUEUE_SIZE_LABEL = "Queue-size";
	public static final String RULE_NODE_PARAMETER = "rule";
	public static final String THREAD_POOL_SIZE_FIELD = "Threads";
	public static final String TYPE_DEFINITION_PARAMETER = "type-node-id";
	private static final String WALD_OT_COMPUTE_NAME = "Thread Manager";
	private static final String WALD_OT_COMPUTE_OBJECT_TYPE = "WaldOTComputeObjectType";
	private static final String WALD_OT_RULE_NAME = "Rule";
	private static final String WALD_OT_RULE_OBJECT_TYPE = "WaldOTRuleObjectType";
	private final Map<NodeId, MonitoredEdge> activeEdges = Collections.synchronizedMap(new HashMap<>());
	protected final ClonableMapContext baseJexlContext = new ClonableMapContext();
	private UaObjectTypeNode computeTypeNode;
	private JexlEngine jexlEngine;
	private UaObjectTypeNode ruleTypeNode;
	private final ScheduledExecutorService timer = ThreadHelper.newVirtualSchedulerExecutor("link delay timer");
	protected WaldotNamespace waldotNamespace;

	public ClonableMapContext baseJexlContext() {
		return baseJexlContext;
	}

	@Override
	public void close() throws Exception {
		timer.shutdownNow();
		for (final MonitoredEdge edge : activeEdges.values()) {
			edge.remove();
		}
		logger.info("WaldotRulesEnginePlugin closed");
	}

	@Override
	public boolean containsEdgeType(String typeDefinitionLabel) {
		return FIRE_EDGE_LABEL.equals(typeDefinitionLabel) || LINK_TO_EDGE_LABEL.equals(typeDefinitionLabel)
				|| LINK_FROM_EDGE_LABEL.equals(typeDefinitionLabel) || EXECUTE_EDGE_LABEL.equals(typeDefinitionLabel);
	}

	@Override
	public boolean containsVertexType(String typeDefinitionLabel) {
		return RULE_NODE_PARAMETER.equals(typeDefinitionLabel) || COMPUTE_NODE_PARAMETER.equals(typeDefinitionLabel);
	}

	@Override
	public boolean containsVertexTypeNode(NodeId typeDefinitionNodeId) {
		return ruleTypeNode.getNodeId().equals(typeDefinitionNodeId)
				|| computeTypeNode.getNodeId().equals(typeDefinitionNodeId);
	}

	private void createComputeTypeNode() {
		computeTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + WALD_OT_COMPUTE_OBJECT_TYPE))
				.setBrowseName(waldotNamespace.generateQualifiedName(WALD_OT_COMPUTE_OBJECT_TYPE))
				.setDisplayName(LocalizedText.english(WALD_OT_COMPUTE_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, computeTypeNode, MiloStrategy.LABEL_FIELD,
				NodeIds.String);
		ComputeVertex.generateParameters(waldotNamespace, computeTypeNode);
		// add definition to the address space
		waldotNamespace.getStorageManager().addNode(computeTypeNode);
		computeTypeNode.addReference(new Reference(computeTypeNode.getNodeId(), NodeIds.HasSubtype,
				NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(computeTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);

	}

	private WaldotVertex createComputeVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		return new ComputeVertex(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask,
				eventNotifier, version, propertyKeyValues);
	}

	private void createRuleTypeNode() {
		ruleTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
				.setNodeId(waldotNamespace.generateNodeId(OBJECT_TYPES + WALD_OT_RULE_OBJECT_TYPE))
				.setBrowseName(waldotNamespace.generateQualifiedName(WALD_OT_RULE_OBJECT_TYPE))
				.setDisplayName(LocalizedText.english(WALD_OT_RULE_NAME)).setIsAbstract(false).build();
		PluginListener.addParameterToTypeNode(waldotNamespace, ruleTypeNode, MiloStrategy.LABEL_FIELD, NodeIds.String);
		RuleVertex.generateParameters(waldotNamespace, ruleTypeNode);
		// add definition to the address space
		waldotNamespace.getStorageManager().addNode(ruleTypeNode);
		ruleTypeNode.addReference(
				new Reference(ruleTypeNode.getNodeId(), NodeIds.HasSubtype, NodeIds.BaseObjectType.expanded(), false));
		waldotNamespace.getObjectTypeManager().registerObjectType(ruleTypeNode.getNodeId(), UaObjectNode.class,
				objectNodeConstructor);
	}

	private WaldotVertex createRuleVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		return new RuleVertex(this, graph, context, nodeId, browseName, displayName, description, writeMask,
				userWriteMask, eventNotifier, version, propertyKeyValues);
	}

	@Override
	public WaldotVertex createVertex(NodeId typeDefinitionNodeId, WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		if (!containsVertexTypeNode(typeDefinitionNodeId)) {
			return null;
		}
		if (ruleTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			return createRuleVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
					userWriteMask, eventNotifier, version, propertyKeyValues);
		} else if (computeTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
			return createComputeVertex(graph, context, nodeId, browseName, displayName, description, writeMask,
					userWriteMask, eventNotifier, version, propertyKeyValues);
		} else {
			return null;
		}
	}

	public JexlEngine getJexlEngine() {
		return jexlEngine;
	}

	public ScheduledExecutorService getTimer() {
		return timer;
	}

	@Override
	public NodeId getVertexTypeNode(String typeDefinitionLabel) {
		if (RULE_NODE_PARAMETER.equals(typeDefinitionLabel)) {
			return ruleTypeNode.getNodeId();
		} else if (COMPUTE_NODE_PARAMETER.equals(typeDefinitionLabel)) {
			return computeTypeNode.getNodeId();
		} else {
			return null;

		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		createRuleTypeNode();
		createComputeTypeNode();
		registerJexlEngine(waldotNamespace);
	}

	@Override
	public void notifyAddEdge(WaldotEdge edge, WaldotVertex sourceVertex, WaldotVertex targetVertex, String label,
			String type, Object[] propertyKeyValues) {
		if (activeEdges.containsKey(edge.getNodeId())) {
			logger.warn("Edge {} already monitored, skipping", edge.getNodeId());
		} else {
			if (type == null) {
				logger.warn("Edge {} has no type, skipping", edge.getNodeId());
				return;
			}
			switch (type) {
			case FIRE_EDGE_LABEL:
				activeEdges.put(edge.getNodeId(), new FireMonitoredEdge(this, edge, sourceVertex, targetVertex));
				break;
			case LINK_TO_EDGE_LABEL:
				activeEdges.put(edge.getNodeId(),
						new LinkMonitoredEdge(this, LinkDirection.TO, edge, sourceVertex, targetVertex));
				break;
			case LINK_FROM_EDGE_LABEL:
				activeEdges.put(edge.getNodeId(),
						new LinkMonitoredEdge(this, LinkDirection.FROM, edge, sourceVertex, targetVertex));
				break;
			case EXECUTE_EDGE_LABEL:
				activeEdges.put(edge.getNodeId(), new ComputeMonitoredEdge(this, edge, sourceVertex, targetVertex));
				break;
			default:
				logger.warn("Edge {} has unknown type {}, skipping", edge.getNodeId(), type);
			}
		}
	}

	@Override
	public void notifyRemoveEdge(WaldotEdge edge) {
		if (!activeEdges.containsKey(edge.getNodeId())) {
			logger.warn("Edge {} not monitored, skipping", edge.getNodeId());
		} else {
			activeEdges.get(edge.getNodeId()).remove();
			activeEdges.remove(edge.getNodeId());
		}
	}

	private void registerJexlEngine(final WaldotNamespace waldotNamespace) {
		jexlEngine = JexlExecutor.generateEngine();
		baseJexlContext.set(ConsoleStrategy.LOG_LABEL, logger);
		baseJexlContext.set(ConsoleStrategy.G_LABEL, waldotNamespace.getGremlinGraph());
		baseJexlContext.set(ConsoleStrategy.COMMANDS_LABEL, waldotNamespace.getCommandsAsFunction());
		for (final WaldotCommand command : waldotNamespace.getConsoleStrategy().getCommands()) {
			logger.info("Registering console command: {}", command.getConsoleCommand());
			baseJexlContext.set(command.getConsoleCommand(), command);
		}
	}
}
