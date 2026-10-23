package net.rossonet.waldot.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.jexl3.JexlEngine;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.jexl.ClonableMapContext;
import net.rossonet.waldot.jexl.JexlExecutor;
import net.rossonet.waldot.plugin.AbstractPluginListener;
import net.rossonet.waldot.rules.commands.AddComputeMonitoredEdgeCommand;
import net.rossonet.waldot.rules.commands.CreateComputeVertexCommand;
import net.rossonet.waldot.rules.commands.CreateRuleCommand;
import net.rossonet.waldot.rules.commands.CreateShowCaseArenaCommand;
import net.rossonet.waldot.rules.commands.CreateShowCaseEvolutionCommand;
import net.rossonet.waldot.rules.vertices.ComputableFireableAbstractOpcVertex;
import net.rossonet.waldot.rules.vertices.ComputeVertex;
import net.rossonet.waldot.rules.vertices.RuleVertex;

/**
 * WaldOT Rules Engine Plugin - Event-driven IF-THEN rule execution system.
 * <p>
 * The plugin lets you define rules that are triggered by events and property
 * changes in the OPC-UA graph. Conditions and actions are written in JEXL
 * (Java Expression Language) and executed by a managed pool of virtual threads.
 * </p>
 * 
 * <h2>Architecture Overview</h2>
 * <p>
 * The plugin provides two vertex types and one edge type:
 * <ul>
 *   <li><b>rules:rule</b> ({@link RuleVertex}): IF-THEN rule with a JEXL condition and action</li>
 *   <li><b>rules:compute</b> ({@link ComputeVertex}): thread manager that executes the actions of the connected rules</li>
 *   <li><b>rules:execute</b> edge: connects a compute vertex (source) to a rule (target)</li>
 * </ul>
 * The <code>fire</code> edge, provided by the namespace, connects any vertex to a rule and
 * triggers it when the source changes.
 * 
 * <h2>Event Flow</h2>
 * <pre>
 * [Source vertex] --fire--&gt; [RuleVertex] (queue) &lt;--rules:execute-- [ComputeVertex] --&gt; [virtual threads]
 * </pre>
 * <ol>
 *   <li>a property change or an event on the source vertex reaches the rule through the <code>fire</code> edge;</li>
 *   <li>the rule queues an action (with hysteresis deduplication) and publishes its <code>queue</code> size;</li>
 *   <li>the <code>rules:execute</code> edge notifies the compute vertex, which schedules the rule by weight;</li>
 *   <li>the compute vertex takes one event at a time, within the limit of its <code>threads</code>, and runs it;</li>
 *   <li>the action evaluates the <code>condition</code> and, when it is true, runs the <code>action</code>.</li>
 * </ol>
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Vertex compute = graph.addVertex("type", "rules:compute", "label", "main-compute", "threads", "4");
 * Vertex rule = graph.addVertex("type", "rules:rule", "label", "temp-alarm",
 *     "condition", "g.V().has('id','sensor').next().property('temperature').value() > 80.0",
 *     "action", "log.warn('Temperature alarm')");
 * 
 * // the compute vertex executes the rule (edge direction: compute -> rule)
 * compute.addEdge("runner", rule, "type", "rules:execute", "priority", "100");
 * 
 * // when the sensor changes, the rule is fired
 * sensor.addEdge("watch", rule, "type", "fire", "properties-to-monitor", "temperature");
 * }</pre>
 * 
 * <h2>JEXL Context Variables</h2>
 * <p>
 * Every execution gets its own context, so variables assigned by a script do not leak to the next one:
 * <ul>
 *   <li><code>log</code>: SLF4J logger</li>
 *   <li><code>g</code>: Gremlin traversal source</li>
 *   <li><code>graph</code>: the WaldOT graph</li>
 *   <li><code>cmd</code>: WaldOT commands</li>
 *   <li><code>self</code>: the {@link RuleVertex} being executed</li>
 *   <li><code>math</code>: {@link Math} class</li>
 *   <li><code>rand</code>: thread safe {@link java.util.Random}</li>
 *   <li>every console command, by its console name (including the commands of the plugins loaded later)</li>
 * </ul>
 * 
 * @see RuleVertex
 * @see ComputeVertex
 * @see ComputableFireableAbstractOpcVertex
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 * @since 0.4.0
 */
@WaldotPlugin
public class WaldotRulesEnginePlugin extends AbstractPluginListener {

	private static final String _RULES_TYPE_PRE_LABEL = "rules:";
	public static final String ACTION_EXECUTED_SIZE_LABEL = "executed";
	public static final String ACTION_EXECUTED_SIZE_LABEL_DESCRIPTION = "Number of actions of the rule that have been executed successfully. Actions that fail are counted in 'errors'.";
	public static final String ACTION_FIELD = "action";
	public static final String ACTION_FIELD_DESCRIPTION = "JEXL script that is executed when the condition evaluates to true. Can contain any valid JEXL code, including logging, graph traversal, and commands.";
	public static final String BASE_CMD_DIRECTORY = "rules";
	public static final String COMPUTE_NODE_PARAMETER = _RULES_TYPE_PRE_LABEL
			+ "compute";
	public static final String COMPUTE_QUEUE_LABEL_DESCRIPTION = "Number of rules with events waiting to be executed by the compute vertex.";
	public static final String CONDITION_FIELD = "condition";
	public static final String CONDITION_FIELD_DESCRIPTION = "JEXL script that must evaluate to a boolean. If true, the action is executed.";
	public static final String DEBUG_LEVEL_LABEL = "debug";
	public static final String DEBUG_LEVEL_LABEL_DESCRIPTION = "Debug level of the rule. 0 = no debug; greater than 0 = a debug event is published for each phase of the execution; greater than 1 = the phases are also logged. The level also sets the depth of the stack traces in the events.";
	public static final String DEFAULT_ACTION_VALUE = "log.info('action fired')";
	public static final boolean DEFAULT_CLEAR_FACTS_AFTER_EXECUTION = false;
	public static final String DEFAULT_CONDITION_VALUE = "true";
	public static final int DEFAULT_DELAY_BEFORE_EVALUATION = 0;
	public static final int DEFAULT_DELAY_BEFORE_EXECUTE = 0;
	public static final long DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE = 120000; // 2
	// minuti;
	public static final double DEFAULT_PRIORITY_FACTOR_IN_COMPUTE = 100.0;
	public static final int DEFAULT_PRIORITY_VALUE = 100;
	public static final int DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE = 1;
	public static final String ERRORS_SIZE_LABEL = "errors";
	public static final String ERRORS_SIZE_LABEL_DESCRIPTION = "Number of failures of the rule: compile errors, exceptions, conditions that do not return a boolean. This is a counter maintained by the engine.";
	public static final String EXECUTE_EDGE_LABEL = _RULES_TYPE_PRE_LABEL
			+ "execute";
	public static final String EXECUTION_TIMEOUT_MS_FIELD = "execution-timeout-ms";
	public static final String EXECUTION_TIMEOUT_MS_FIELD_DESCRIPTION = "Maximum duration in milliseconds of an action; after that the action is cancelled.";
	public static final String FIRE_EDGE_PARAMETER = "fire";
	public static final String HYSTERESIS_LABEL = "hysteresis";
	public static final String HYSTERESIS_LABEL_DESCRIPTION = "Time window in milliseconds in which repeated notifications from the same source (same vertex and property) are discarded. 0 = disabled.";
	private static volatile WaldotRulesEnginePlugin instance;
	public static final String PRIORITY_FACTOR_FIELD = "factor";
	public static final String PRIORITY_FACTOR_FIELD_DESCRIPTION = "Multiplier of the edge priority in the weight of a rule: weight = priority * factor + queue size. Rules with higher weight are executed first.";
	public static final String PRIORITY_FIELD = "priority";
	public static final String QUEUE_SIZE_LABEL = "queue";
	public static final String QUEUE_SIZE_LABEL_DESCRIPTION = "Number of events of the rule waiting to be executed.";
	public static final String RULE_NODE_PARAMETER = _RULES_TYPE_PRE_LABEL
			+ "rule";
	public static final String THREAD_POOL_SIZE_FIELD = "threads";
	public static final String THREAD_POOL_SIZE_FIELD_DESCRIPTION = "Maximum number of actions executed at the same time by the compute vertex.";
	public static final String TOTAL_SIZE_LABEL = "total";
	public static final String TOTAL_SIZE_LABEL_DESCRIPTION = "Total number of events of the rule that have been evaluated.";
	public static final String TYPE_DEFINITION_PARAMETER = "type-node-id";
	private static final String WALD_OT_COMPUTE_NAME = "Thread Manager";
	private static final String WALD_OT_COMPUTE_OBJECT_TYPE = "WaldOTComputeObjectType";
	private static final String WALD_OT_RULE_NAME = "Rule";
	private static final String WALD_OT_RULE_OBJECT_TYPE = "WaldOTRuleObjectType";

	/**
	 * Returns the last initialized plugin.
	 * 
	 * @return the plugin instance, or null if it is not initialized
	 */
	public static WaldotRulesEnginePlugin getInstance() {
		return instance;
	}

	/**
	 * Returns the plugin registered in a namespace. Falls back to the last
	 * initialized instance when the namespace does not list the plugin.
	 * 
	 * @param namespace the namespace of the vertex
	 * @return the plugin, or null if it is not initialized
	 */
	public static WaldotRulesEnginePlugin getInstance(
			WaldotNamespace namespace) {
		if (namespace != null) {
			for (final PluginListener plugin : namespace.getPlugins()) {
				if (plugin instanceof WaldotRulesEnginePlugin) {
					return (WaldotRulesEnginePlugin) plugin;
				}
			}
		}
		return instance;
	}

	private final ClonableMapContext baseJexlContext = new ClonableMapContext();

	private final Object contextLock = new Object();

	private JexlEngine jexlEngine;

	private int registeredConsoleCommands = -1;

	/**
	 * Returns the base JEXL context shared by all rules. It holds the global
	 * variables (log, g, graph, cmd, math, rand). Rules do not use it directly:
	 * they get a copy through {@link #newJexlContext(Object)}.
	 * 
	 * @return base JEXL context
	 */
	public ClonableMapContext baseJexlContext() {
		return baseJexlContext;
	}

	@Override
	public void close() throws Exception {
		if (instance == this) {
			instance = null;
		}
		super.close();
	}

	public JexlEngine getJexlEngine() {
		return jexlEngine;
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		super.initialize(waldotNamespace);
		commands.clear();
		edgeTypeNodes.clear();
		registerJexlEngine(waldotNamespace);
		registerTypeNode(RuleVertex.class, WALD_OT_RULE_OBJECT_TYPE,
				WALD_OT_RULE_NAME, RULE_NODE_PARAMETER);
		registerTypeNode(ComputeVertex.class, WALD_OT_COMPUTE_OBJECT_TYPE,
				WALD_OT_COMPUTE_NAME, COMPUTE_NODE_PARAMETER);
		registerTypeEdge(EXECUTE_EDGE_LABEL);
		registerCommand(new AddComputeMonitoredEdgeCommand(waldotNamespace));
		registerCommand(new CreateRuleCommand(waldotNamespace));
		registerCommand(new CreateComputeVertexCommand(waldotNamespace));
		registerCommand(new CreateShowCaseArenaCommand(waldotNamespace));
		registerCommand(new CreateShowCaseEvolutionCommand(waldotNamespace));
		instance = this;
	}

	/**
	 * Creates the JEXL context for one execution of a rule: a copy of the base
	 * context, with <code>self</code> and with all the console commands
	 * registered at this moment (so the commands of the plugins loaded after
	 * this one are available too).
	 * 
	 * @param self the vertex executing the script
	 * @return a new context, owned by the caller
	 */
	public ClonableMapContext newJexlContext(Object self) {
		synchronized (contextLock) {
			refreshConsoleCommands();
			final ClonableMapContext context = new ClonableMapContext(
					baseJexlContext);
			context.set(ConsoleStrategy.SELF_LABEL, self);
			return context;
		}
	}

	private void refreshConsoleCommands() {
		final WaldotNamespace namespace = getWaldotNamespace();
		if (namespace == null || namespace.getConsoleStrategy() == null) {
			return;
		}
		try {
			final List<WaldotCommand> consoleCommands = new ArrayList<>(
					namespace.getConsoleStrategy().getCommands());
			if (consoleCommands.size() != registeredConsoleCommands) {
				for (final WaldotCommand command : consoleCommands) {
					baseJexlContext.set(command.getConsoleCommand(), command);
				}
				registeredConsoleCommands = consoleCommands.size();
			}
		} catch (final RuntimeException e) {
			logger.warn(
					"Cannot refresh the console commands in the JEXL context",
					e);
		}
	}

	/**
	 * Registers JEXL engine and populates base context with global variables.
	 * 
	 * @param waldotNamespace WaldOT namespace instance
	 */
	private void registerJexlEngine(final WaldotNamespace waldotNamespace) {
		synchronized (contextLock) {
			jexlEngine = JexlExecutor.generateEngine();
			baseJexlContext.clear();
			registeredConsoleCommands = -1;
			baseJexlContext.set(ConsoleStrategy.LOG_LABEL, logger);
			baseJexlContext.set(ConsoleStrategy.TRAVERSE_LABEL,
					waldotNamespace.getGremlinGraph().traversal());
			baseJexlContext.set(ConsoleStrategy.GRAPH_LABEL,
					waldotNamespace.getGremlinGraph());
			baseJexlContext.set(ConsoleStrategy.COMMANDS_LABEL,
					waldotNamespace.getCommandsAsFunction());
			baseJexlContext.set(ConsoleStrategy.MATH, Math.class);
			// ThreadLocalRandom must not be shared between threads
			baseJexlContext.set(ConsoleStrategy.RANDOM, new Random());
		}
	}
}
