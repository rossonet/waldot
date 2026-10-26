package net.rossonet.waldot.rules;

/**
 * Constants of the rules engine plugin: vertex and edge type labels, property
 * keys, default values and parameter descriptions.
 *
 * <p>
 * The <code>*_DESCRIPTION</code> constants are shared by the OPC UA commands
 * that create the vertices and by the <code>generateParameters</code> methods
 * of the vertices that register the parameters on the vertex type nodes.
 * </p>
 *
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public final class RulesEngineConstants {

	private static final String _RULES_TYPE_PRE_LABEL = "rules:";
	public static final String ACTION_EXECUTED_SIZE_LABEL = "executed";
	public static final String ACTION_EXECUTED_SIZE_LABEL_DESCRIPTION = "Number of actions of the rule that have been executed successfully. Actions that fail are counted in 'errors'.";
	public static final String ACTION_FIELD = "action";
	public static final String ACTION_FIELD_DESCRIPTION = "JEXL script executed by the connected compute vertex when the condition evaluates to true. It can contain any valid JEXL code and use the context variables log, g, graph, cmd, self, math, rand and every console command. Default: log.info('action fired')";
	public static final String BASE_CMD_DIRECTORY = "rules";
	public static final String COMPUTE_NODE_PARAMETER = _RULES_TYPE_PRE_LABEL
			+ "compute";
	public static final String COMPUTE_QUEUE_LABEL_DESCRIPTION = "Number of rules with events waiting to be executed by the compute vertex.";
	public static final String CONDITION_FIELD = "condition";
	public static final String CONDITION_FIELD_DESCRIPTION = "JEXL script evaluated every time the rule is fired; it must return a boolean and the action is executed only when the result is true. It can use the context variables log, g, graph, cmd, self, math, rand and every console command. Default: true";
	public static final String DEBUG_LEVEL_LABEL = "debug";
	public static final String DEBUG_LEVEL_LABEL_DESCRIPTION = "Debug level of the rule. 0 = no debug; greater than 0 = a debug event is published for each phase of the execution; greater than 1 = the phases are also logged. The level also sets the depth of the stack traces in the events. Default: 0";
	public static final String DEFAULT_ACTION_VALUE = "log.info('action fired')";
	public static final boolean DEFAULT_CLEAR_FACTS_AFTER_EXECUTION = false;
	public static final String DEFAULT_CONDITION_VALUE = "true";
	public static final int DEFAULT_DELAY_BEFORE_EVALUATION = 0;
	public static final int DEFAULT_DELAY_BEFORE_EXECUTE = 0;
	public static final long DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE = 120000; // 2
																				// minuti
	public static final double DEFAULT_PRIORITY_FACTOR_IN_COMPUTE = 100.0;
	public static final int DEFAULT_PRIORITY_VALUE = 100;
	public static final int DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE = 1;
	public static final String ERRORS_SIZE_LABEL = "errors";
	public static final String ERRORS_SIZE_LABEL_DESCRIPTION = "Number of failures of the rule: compile errors, exceptions, conditions that do not return a boolean. This is a counter maintained by the engine.";
	public static final String EXECUTE_EDGE_LABEL = _RULES_TYPE_PRE_LABEL
			+ "execute";
	public static final String EXECUTION_TIMEOUT_MS_FIELD = "execution-timeout-ms";
	public static final String EXECUTION_TIMEOUT_MS_FIELD_DESCRIPTION = "Maximum duration in milliseconds of an action executed by the compute vertex; after that the action is cancelled and counted as an error of the rule. Default: 120000 (2 minutes)";
	public static final String FIRE_EDGE_PARAMETER = "fire";
	public static final String HYSTERESIS_LABEL = "hysteresis";
	public static final String HYSTERESIS_LABEL_DESCRIPTION = "Time window in milliseconds in which repeated notifications from the same source (same vertex and property) are discarded. 0 = disabled (default).";
	public static final String PRIORITY_FACTOR_FIELD = "factor";
	public static final String PRIORITY_FACTOR_FIELD_DESCRIPTION = "Multiplier of the edge priority in the weight of a rule: weight = priority * factor + queue size. Rules with higher weight are executed first. Default: 100.0";
	public static final String PRIORITY_FIELD = "priority";
	public static final String QUEUE_SIZE_LABEL = "queue";
	public static final String QUEUE_SIZE_LABEL_DESCRIPTION = "Number of events of the rule waiting to be executed.";
	public static final String RULE_NODE_PARAMETER = _RULES_TYPE_PRE_LABEL
			+ "rule";
	public static final String THREAD_POOL_SIZE_FIELD = "threads";
	public static final String THREAD_POOL_SIZE_FIELD_DESCRIPTION = "Maximum number of actions executed at the same time (on virtual threads) by the compute vertex; values lower than 1 are replaced by the default. Default: 1";
	public static final String TOTAL_SIZE_LABEL = "total";
	public static final String TOTAL_SIZE_LABEL_DESCRIPTION = "Total number of events of the rule that have been evaluated.";
	public static final String TYPE_DEFINITION_PARAMETER = "type-node-id";

	private RulesEngineConstants() {
	}

}
