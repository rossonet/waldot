package net.rossonet.waldot.rules.vertices;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.jexl.ClonableMapContext;
import net.rossonet.waldot.rules.RulesEngineConstants;
import net.rossonet.waldot.rules.WaldotRulesEnginePlugin;
import net.rossonet.waldot.rules.events.FireableAction;
import net.rossonet.waldot.utils.LogHelper;

/**
 * Represents an IF-THEN rule in the WaldOT graph.
 * <p>
 * RuleVertex è il cuore del sistema di regole. Rappresenta una regola di tipo
 * IF-THEN che viene attivata da eventi o modifiche di proprietà filtrati da
 * un FireMonitoredEdge. Quando sollecitato, valuta la condizione JEXL e se vera
 * esegue l'azione JEXL.
 * </p>
 * 
 * <h2>Architecture</h2>
 * <pre>
 * [Source Node] --fire--&gt; [RuleVertex] (queue) &lt;--rules:execute-- [ComputeVertex]
 * </pre>
 * 
 * <h2>Rule Execution Flow</h2>
 * <ol>
 *   <li><b>Event arrival</b>: the <code>fire</code> edge notifies the rule</li>
 *   <li><b>Enqueue</b>: the event is queued (with hysteresis); the <code>queue</code> property follows</li>
 *   <li><b>Execution</b>: the {@link ComputeVertex} takes the event and runs it in a virtual thread</li>
 *   <li><b>Condition</b>: the script is compiled (and cached until it changes) and executed; it must return a boolean</li>
 *   <li><b>Action</b>: if the condition is true, the action script is compiled and executed</li>
 *   <li><b>Metrics</b>: <code>total</code>, <code>executed</code> and <code>errors</code> are updated</li>
 * </ol>
 * 
 * <h2>JEXL Context</h2>
 * <p>
 * Every execution has its own context, copied from the plugin base context:
 * <ul>
 *   <li><code>log</code>: SLF4J logger</li>
 *   <li><code>g</code>: Gremlin traversal (graph queries)</li>
 *   <li><code>graph</code>: the WaldOT graph</li>
 *   <li><code>cmd</code>: WaldOT commands</li>
 *   <li><code>self</code>: this RuleVertex</li>
 *   <li><code>math</code>: Java Math class</li>
 *   <li><code>rand</code>: random number generator</li>
 *   <li>every console command by name</li>
 * </ul>
 * The condition and the action of one execution share the context.
 * 
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Vertex rule = graph.addVertex(
 *     "type", "rules:rule",
 *     "label", "temp-alarm",
 *     "condition", "g.V().has('id','sensor').next().property('temperature').value() > 80.0",
 *     "action", "log.warn('Temperature alarm')",
 *     "hysteresis", "5000",  // 5 seconds deduplication
 *     "debug", "1"           // publish debug events
 * );
 * 
 * // the compute vertex executes the rule
 * computeVertex.addEdge("runner", rule, "type", "rules:execute");
 * 
 * // fire the rule when the sensor changes
 * sensor.addEdge("watch", rule, "type", "fire", "properties-to-monitor", "temperature");
 * }</pre>
 * 
 * <h2>Properties</h2>
 * <ul>
 *   <li><b>condition</b>: JEXL script for the IF clause (must return a boolean, default <code>true</code>)</li>
 *   <li><b>action</b>: JEXL script for the THEN clause</li>
 *   <li><b>hysteresis</b>: time window (ms) for the deduplication of the notifications of the same source (default 0)</li>
 *   <li><b>debug</b>: debug level (0 = off, &gt;0 = events, &gt;1 = events + logs)</li>
 *   <li><b>queue</b>: current queue size (maintained by the engine)</li>
 *   <li><b>total</b>: events evaluated (maintained by the engine)</li>
 *   <li><b>executed</b>: actions executed successfully (maintained by the engine)</li>
 *   <li><b>errors</b>: compile errors, exceptions and conditions that do not return a boolean (maintained by the engine)</li>
 * </ul>
 * All the properties can be changed at runtime; a changed script is compiled again at the next execution.
 * 
 * <h2>Debug Events</h2>
 * <p>
 * When debug &gt; 0, RuleVertex publishes OPC-UA events for each execution phase:
 * BEFORE/AFTER condition compile/execute, BEFORE/AFTER action compile/execute.
 * Useful for debugging rule logic and monitoring execution.
 * </p>
 * 
 * @see ComputableFireableAbstractOpcVertex
 * @see ComputeVertex
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 * @since 0.4.0
 */
public class RuleVertex extends ComputableFireableAbstractOpcVertex {

	/** A compiled script with the source it comes from, to know when it is stale. */
	private static final class CompiledScript {
		private final JexlScript script;
		private final String source;

		private CompiledScript(String source, JexlScript script) {
			this.source = source;
			this.script = script;
		}
	}

	/**
	 * Debug event types published during rule execution.
	 * <p>
	 * Tipi di eventi debug pubblicati durante l'esecuzione della regola
	 * quando debug &gt; 0.
	 * </p>
	 */
	public enum DebugEventType {
		AFTER_ACTION_COMPILE, AFTER_ACTION_COMPILE_EXCEPTION, AFTER_ACTION_EXECUTION, AFTER_ACTION_EXECUTION_EXCEPTION, AFTER_CONDITION_COMPILE, AFTER_CONDITION_COMPILE_EXCEPTION, AFTER_CONDITION_EXECUTION, AFTER_CONDITION_EXECUTION_EXCEPTION, BEFORE_ACTION_COMPILE, BEFORE_ACTION_EXECUTION, BEFORE_CONDITION_COMPILE, BEFORE_CONDITION_EXECUTION
	}

	public final class RuleVertexFireableAction extends FireableAction {

		@Override
		public void onTimeout() {
			sendDebugEvent(DebugEventType.AFTER_ACTION_EXECUTION_EXCEPTION,
					"Action cancelled: execution timeout reached");
			logger.warn("Rule action timed out for {}", getNodeId());
			updateThrowable();
		}

		@Override
		public void run() {
			Thread.currentThread().setName(getNodeId().toParseableString());
			if (closed) {
				return;
			}
			try {
				updateTotal();
				final String conditionSource = getCondition();
				CompiledScript condition = compiledCondition;
				if (condition == null
						|| !condition.source.equals(conditionSource)) {
					try {
						sendDebugEvent(DebugEventType.BEFORE_CONDITION_COMPILE,
								null);
						condition = new CompiledScript(conditionSource,
								engine().createScript(conditionSource));
						compiledCondition = condition;
						sendDebugEvent(DebugEventType.AFTER_CONDITION_COMPILE,
								null);
					} catch (final Throwable e) {
						sendDebugEvent(
								DebugEventType.AFTER_CONDITION_COMPILE_EXCEPTION,
								LogHelper.stackTraceToString(e,
										2 + getDebug()));
						logger.error("Error compiling rule vertex condition",
								e);
						updateThrowable();
						return;
					}
				}
				final ClonableMapContext context = newContext();
				boolean conditionResult = false;
				try {
					sendDebugEvent(DebugEventType.BEFORE_CONDITION_EXECUTION,
							null);
					final Object conditionResultObject = condition.script
							.execute(context);
					if (conditionResultObject instanceof Boolean) {
						conditionResult = (Boolean) conditionResultObject;
						sendDebugEvent(DebugEventType.AFTER_CONDITION_EXECUTION,
								conditionResultObject.toString());
					} else {
						logger.warn(
								"Condition script did not return a boolean value, result: '{}'",
								conditionResultObject);
						sendDebugEvent(
								DebugEventType.AFTER_CONDITION_EXECUTION_EXCEPTION,
								"Condition script did not return a boolean value, result: '"
										+ conditionResultObject + "'");
						updateThrowable();
						return;
					}
				} catch (final Throwable e) {
					sendDebugEvent(
							DebugEventType.AFTER_CONDITION_EXECUTION_EXCEPTION,
							LogHelper.stackTraceToString(e, 2 + getDebug()));
					logger.error("Error executing rule vertex condition", e);
					updateThrowable();
					return;
				}
				if (!conditionResult) {
					return;
				}
				final String actionSource = getAction();
				CompiledScript actionScript = compiledAction;
				if (actionScript == null
						|| !actionScript.source.equals(actionSource)) {
					try {
						sendDebugEvent(DebugEventType.BEFORE_ACTION_COMPILE,
								null);
						actionScript = new CompiledScript(actionSource,
								engine().createScript(actionSource));
						compiledAction = actionScript;
						sendDebugEvent(DebugEventType.AFTER_ACTION_COMPILE,
								null);
					} catch (final Throwable e) {
						sendDebugEvent(
								DebugEventType.AFTER_ACTION_COMPILE_EXCEPTION,
								LogHelper.stackTraceToString(e,
										2 + getDebug()));
						logger.error("Error compiling rule vertex action", e);
						updateThrowable();
						return;
					}
				}
				try {
					sendDebugEvent(DebugEventType.BEFORE_ACTION_EXECUTION,
							null);
					final Object actionResultObject = actionScript.script
							.execute(context);
					updateExecuted();
					sendDebugEvent(DebugEventType.AFTER_ACTION_EXECUTION,
							"Action executed successfully, result: '"
									+ actionResultObject + "'");
				} catch (final Throwable e) {
					sendDebugEvent(
							DebugEventType.AFTER_ACTION_EXECUTION_EXCEPTION,
							LogHelper.stackTraceToString(e, 2 + getDebug()));
					if (!isTimedOut()) {
						updateThrowable();
					}
					logger.error("Error executing rule vertex action", e);
				}
			} catch (final Throwable t) {
				logger.error("Error executing rule vertex", t);
				if (!isTimedOut()) {
					updateThrowable();
				}
			}
		}

	}

	private static final Logger logger = LoggerFactory
			.getLogger(RuleVertex.class);

	public static void generateParameters(WaldotNamespace waldotNamespace,
			UaObjectTypeNode typeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.CONDITION_FIELD, NodeIds.String,
				RulesEngineConstants.CONDITION_FIELD_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.ACTION_FIELD, NodeIds.String,
				RulesEngineConstants.ACTION_FIELD_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.DEBUG_LEVEL_LABEL, NodeIds.Int32,
				RulesEngineConstants.DEBUG_LEVEL_LABEL_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.HYSTERESIS_LABEL, NodeIds.Int64,
				RulesEngineConstants.HYSTERESIS_LABEL_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.QUEUE_SIZE_LABEL, NodeIds.Int64,
				RulesEngineConstants.QUEUE_SIZE_LABEL_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.TOTAL_SIZE_LABEL, NodeIds.Int64,
				RulesEngineConstants.TOTAL_SIZE_LABEL_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.ACTION_EXECUTED_SIZE_LABEL, NodeIds.Int64,
				RulesEngineConstants.ACTION_EXECUTED_SIZE_LABEL_DESCRIPTION);
		PluginListener.addParameterToTypeNode(waldotNamespace, typeNode,
				RulesEngineConstants.ERRORS_SIZE_LABEL, NodeIds.Int64,
				RulesEngineConstants.ERRORS_SIZE_LABEL_DESCRIPTION);
	}

	private volatile boolean closed = false;
	private volatile CompiledScript compiledAction;
	private volatile CompiledScript compiledCondition;
	private final Object counterLock = new Object();

	private final Object debugEventLock = new Object();

	private volatile boolean debugEventPublishingEnabled = true;

	private final AtomicLong errors = new AtomicLong();
	private final AtomicLong executed = new AtomicLong();
	private final AtomicLong total = new AtomicLong();
	protected final WaldotNamespace waldotNamespace;
	private final WaldotRulesEnginePlugin waldotRulesEnginePlugin;

	/**
	 * Standard vertex constructor, used by the plugin to create the vertex. The
	 * rules engine plugin is looked up in the namespace of the graph.
	 */
	public RuleVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName,
			LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version,
			Object[] propertyKeyValues) {
		this(WaldotRulesEnginePlugin.getInstance(graph.getWaldotNamespace()),
				graph, context, nodeId, browseName, displayName, description,
				writeMask, userWriteMask, eventNotifier, version,
				propertyKeyValues);
	}

	public RuleVertex(WaldotRulesEnginePlugin waldotRulesEnginePlugin,
			WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName,
			LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version,
			Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description,
				writeMask, userWriteMask, eventNotifier, version);
		waldotNamespace = graph.getWaldotNamespace();
		this.waldotRulesEnginePlugin = waldotRulesEnginePlugin;
		addManagedProperty(RulesEngineConstants.CONDITION_FIELD, NodeIds.String,
				String.class, false,
				RulesEngineConstants.DEFAULT_CONDITION_VALUE, null, false,
				propertyKeyValues);
		addManagedProperty(RulesEngineConstants.ACTION_FIELD, NodeIds.String,
				String.class, false, RulesEngineConstants.DEFAULT_ACTION_VALUE,
				null, false, propertyKeyValues);
		addManagedProperty(RulesEngineConstants.DEBUG_LEVEL_LABEL,
				NodeIds.Int32, Integer.class, false, 0, null, false,
				propertyKeyValues);
		addManagedProperty(RulesEngineConstants.HYSTERESIS_LABEL, NodeIds.Int64,
				Long.class, false, 0L, hysteresisCallback(), false,
				propertyKeyValues);
		addManagedProperty(RulesEngineConstants.QUEUE_SIZE_LABEL, NodeIds.Int64,
				Long.class, false, 0L, null, false, propertyKeyValues);
		addManagedProperty(RulesEngineConstants.TOTAL_SIZE_LABEL, NodeIds.Int64,
				Long.class, false, 0L, null, false, propertyKeyValues);
		addManagedProperty(RulesEngineConstants.ACTION_EXECUTED_SIZE_LABEL,
				NodeIds.Int64, Long.class, false, 0L, null, false,
				propertyKeyValues);
		addManagedProperty(RulesEngineConstants.ERRORS_SIZE_LABEL,
				NodeIds.Int64, Long.class, false, 0L, null, false,
				propertyKeyValues);
		// i contatori ripartono dai valori presenti nelle proprietà (es.
		// vertice ricreato)
		total.set(readCounter(RulesEngineConstants.TOTAL_SIZE_LABEL));
		executed.set(
				readCounter(RulesEngineConstants.ACTION_EXECUTED_SIZE_LABEL));
		errors.set(readCounter(RulesEngineConstants.ERRORS_SIZE_LABEL));
	}

	private void applyHysteresis(Object newValue)
			throws PropertyChangeException {
		final long hysteresis;
		try {
			hysteresis = newValue instanceof Number
					? ((Number) newValue).longValue()
					: Long.parseLong(String.valueOf(newValue));
		} catch (final NumberFormatException e) {
			throw new PropertyChangeException(
					"Invalid hysteresis '" + newValue + "'", e);
		}
		if (hysteresis < 0) {
			throw new PropertyChangeException(
					"Hysteresis must be non-negative, but it is " + hysteresis);
		}
		setHysteresisTimeMs(hysteresis);
	}

	@Override
	public Object clone() {
		return new RuleVertex(waldotRulesEnginePlugin, graph, getNodeContext(),
				getNodeId(), getBrowseName(), getDisplayName(),
				getDescription(), getWriteMask(), getUserWriteMask(),
				getEventNotifier(), version(), getPropertiesAsStringArray());
	}

	@Override
	public void close() throws Exception {
		closed = true;
		super.close();
	}

	private JexlEngine engine() {
		if (waldotRulesEnginePlugin == null
				|| waldotRulesEnginePlugin.getJexlEngine() == null) {
			throw new IllegalStateException(
					"The rules engine plugin is not initialized");
		}
		return waldotRulesEnginePlugin.getJexlEngine();
	}

	private FireableAction generateRunnableEvent() {
		return new RuleVertexFireableAction();
	}

	/**
	 * Returns the JEXL script of the THEN clause; an empty value gives the default.
	 * 
	 * @return action script
	 */
	public String getAction() {
		final String value = getRuntimeParameter(
				RulesEngineConstants.ACTION_FIELD);
		return value == null || value.isBlank()
				? RulesEngineConstants.DEFAULT_ACTION_VALUE
				: value;
	}

	/**
	 * Returns the JEXL script of the IF clause; an empty value gives the default.
	 * 
	 * @return condition script
	 */
	public String getCondition() {
		final String value = getRuntimeParameter(
				RulesEngineConstants.CONDITION_FIELD);
		return value == null || value.isBlank()
				? RulesEngineConstants.DEFAULT_CONDITION_VALUE
				: value;
	}

	/**
	 * Returns the debug level; a missing or negative value means 0.
	 * 
	 * @return debug level
	 */
	public int getDebug() {
		final Integer value = getRuntimeParameter(
				RulesEngineConstants.DEBUG_LEVEL_LABEL);
		return value == null || value < 0 ? 0 : value;
	}

	public long getErrors() {
		return errors.get();
	}

	public long getExecuted() {
		return executed.get();
	}

	@Override
	protected FireableAction getRunnableEvent(UaNode node,
			BaseEventType event) {
		return generateRunnableEvent();
	}

	@Override
	protected FireableAction getRunnablePropertyEvent(UaNode node,
			String propertyLabel) {
		return generateRunnableEvent();
	}

	public long getTotal() {
		return total.get();
	}

	/**
	 * The callback receives the old value of the property as it is in the graph, that can be a
	 * String when the vertex is created from the key values, so it is declared on Object.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private ChangedPropertyCallback<Long> hysteresisCallback() {
		final ChangedPropertyCallback<Object> callback = (label, oldValue,
				newValue) -> applyHysteresis(newValue);
		return (ChangedPropertyCallback) callback;
	}

	private ClonableMapContext newContext() {
		if (waldotRulesEnginePlugin == null) {
			throw new IllegalStateException(
					"The rules engine plugin is not initialized");
		}
		return waldotRulesEnginePlugin.newJexlContext(this);
	}

	private long readCounter(String label) {
		final Long value = getRuntimeParameter(label);
		return value == null || value < 0 ? 0L : value;
	}

	public void sendDebugEvent(DebugEventType eventType, String message) {
		final int debug = getDebug();
		if (debug > 0 && debugEventPublishingEnabled) {
			synchronized (debugEventLock) {
				if (!debugEventPublishingEnabled) {
					return;
				}
				try {
					final UUID randomUUID = UUID.randomUUID();
					final BaseEventTypeNode eventNode = waldotNamespace
							.getEventFactory().createEvent(
									waldotNamespace.generateNodeId(randomUUID),
									NodeIds.SystemEventType);
					eventNode.setBrowseName(waldotNamespace
							.generateQualifiedName(eventType.name()));
					eventNode.setDisplayName(
							LocalizedText.english(eventType.name()));
					eventNode.setEventId(
							ByteString.of(randomUUID.toString().getBytes()));
					eventNode.setEventType(NodeIds.SystemEventType);
					eventNode.setSourceNode(getNodeId());
					eventNode.setSourceName(getBrowseName().getName());
					eventNode.setTime(DateTime.now());
					eventNode.setReceiveTime(DateTime.NULL_VALUE);
					if (message == null || message.isEmpty()) {
						eventNode.setMessage(
								LocalizedText.english(eventType.name()));
					} else {
						eventNode.setMessage(LocalizedText.english(message));
					}
					eventNode.setSeverity(ushort(2));
					postEvent(eventNode);
				} catch (final Throwable e) {
					debugEventPublishingEnabled = false;
					logger.error("Error sending debug event", e);
					logger.warn(
							"Disabling OPC-UA debug event publishing for {} after an event factory failure",
							getNodeId());
				}
			}
		}
		if (debug > 1) {
			logger.info("Debug event, type: {}, message: {}", eventType,
					message);
		}
	}

	// l'incremento e la pubblicazione sono nello stesso blocco: gli
	// aggiornamenti concorrenti non possono lasciare nella proprietà un valore
	// vecchio
	private void updateCounter(String label, AtomicLong counter) {
		synchronized (counterLock) {
			final long value = counter.incrementAndGet();
			if (!isRemoved()) {
				property(label, Long.valueOf(value));
			}
		}
	}

	/**
	 * Counts one action executed successfully.
	 */
	public void updateExecuted() {
		updateCounter(RulesEngineConstants.ACTION_EXECUTED_SIZE_LABEL,
				executed);
	}

	/**
	 * Counts one failure of the rule.
	 */
	public void updateThrowable() {
		updateCounter(RulesEngineConstants.ERRORS_SIZE_LABEL, errors);
	}

	/**
	 * Counts one event evaluated.
	 */
	public void updateTotal() {
		updateCounter(RulesEngineConstants.TOTAL_SIZE_LABEL, total);
	}

}
