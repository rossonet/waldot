package net.rossonet.waldot.rules;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.jexl3.JexlContext;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.rules.CachedRuleRecord;
import net.rossonet.waldot.api.rules.ClonableMapContext;
import net.rossonet.waldot.api.rules.Rule;
import net.rossonet.waldot.api.rules.WaldotRulesEngine;
import net.rossonet.waldot.api.rules.WaldotStepLogger;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;

public class DefaultRule extends OpcVertex implements Rule {
	private class RuleRunner implements Callable<WaldotStepLogger> {

		@Override
		public WaldotStepLogger call() throws Exception {
			dirty.set(false);
			evaluate();
			return stepRegister;
		}

	}

	private final String action;
	private JexlContext cacheJexlContext;
	private boolean clearFactsAfterExecution = true;
	private final String condition;
	private int delayBeforeEvaluation = 0;
	private int delayBeforeExecute = 0;
	private final AtomicBoolean dirty = new AtomicBoolean(false);
	private final int executionTimeout = 1000;
	private final List<TimerCachedMemory> factsMemory = Collections.synchronizedList(new ArrayList<>());
	private long factsValidDelayMs = 0;
	private long factsValidUntilMs = 0;
	private long lastRun = 0;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private boolean parallelExecution = false;
	private int priority = 5;
	private int refractoryPeriodMs = 1000;
	final WaldotStepLogger stepRegister;
	private final AtomicInteger threadCounter = new AtomicInteger(0);
	private final WaldotRulesEngine waldotRulesEngine;

	public DefaultRule(final NodeId typeDefinition, final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final WaldotRulesEngine waldotRulesEngine,
			final String condition, final String action, final int priority, final long factsValidUntilMs,
			final long factsValidDelayMs) {
		this(typeDefinition, graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask,
				eventNotifier, version, waldotRulesEngine, new DefaultWaldotStepLogger(), condition, action, priority,
				factsValidUntilMs, factsValidDelayMs);
	}

	public DefaultRule(final NodeId typeDefinition, final WaldotGraph graph, final UaNodeContext context,
			final NodeId nodeId, final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifier, final long version, final WaldotRulesEngine waldotRulesEngine,
			WaldotStepLogger stepRegister, final String condition, final String action, final int priority,
			final long factsValidUntilMs, final long factsValidDelayMs) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		this.waldotRulesEngine = waldotRulesEngine;
		this.stepRegister = stepRegister;
		this.condition = condition;
		this.action = action;
		this.priority = (priority > 0 && priority < 11) ? priority : 5;
		this.factsValidUntilMs = factsValidUntilMs;
		this.factsValidDelayMs = factsValidDelayMs;
	}

	@Override
	public void attributeChanged(final UaNode node, final AttributeId attributeId, final Object value) {
		logger.info("for rule " + getBrowseName().getName() + " attribute changed node " + node.getNodeId()
				+ " attributeId " + attributeId + " value " + value);
		factsMemory.add(createCachedRuleRecord(node, attributeId, value));
		changeState();
		for (final RuleListener l : getListeners()) {
			l.onAttributeChange(node, attributeId, value);
		}
	}

	private void changeState() {
		if (isParallelExecution() || getRunners() < 1) {
			if (lastRun != 0 && (lastRun + getRefractoryPeriodMs() > System.currentTimeMillis())) {
				logger.info("rule " + getBrowseName().getName() + " is in refractory period, last run at " + lastRun);
				return;
			}
			dirty.set(true);
		} else {
			logger.info("rule " + getBrowseName().getName() + " is running");
		}
	}

	@Override
	public void clear() {
		factsMemory.clear();
	}

	@Override
	public void close() throws Exception {
		clear();
	}

	private TimerCachedMemory createCachedRuleRecord(final UaNode node, final AttributeId attributeId,
			final Object value) {
		return new TimerCachedMemory(node.getNodeId(), getDefaultValidDelayMs(), getDefaultValidUntilMs(),
				new DataUpdateFact(attributeId, value));
	}

	private TimerCachedMemory createCachedRuleRecord(final UaNode node, final BaseEventType event) {
		return new TimerCachedMemory(node.getNodeId(), getDefaultValidDelayMs(), getDefaultValidUntilMs(),
				new EventFact(event));
	}

	private void evaluate() {
		threadCounter.incrementAndGet();
		try {
			for (final RuleListener l : getListeners()) {
				final boolean lr = l.beforeEvaluate(stepRegister);
				if (!lr) {
					logger.info("Rule evaluation stopped by listener");
					stepRegister.onEvaluateStoppedByListener(l);
					return;
				}
			}
			if (getDelayBeforeEvaluation() > 0) {
				Thread.sleep(getDelayBeforeEvaluation());
			}
			boolean conditionPassed = false;
			try {
				conditionPassed = runCheck(stepRegister);
			} catch (final Throwable e) {
				logger.error("Error evaluating rule", e);
				conditionPassed = false;
				getListeners().stream().forEach(l -> l.onEvaluationError(stepRegister, e));
			}
			final boolean resultCondition = conditionPassed;
			getListeners().stream().forEach(l -> l.afterEvaluate(stepRegister, resultCondition));
			if (resultCondition) {
				if (getDelayBeforeExecute() > 0) {
					Thread.sleep(getDelayBeforeExecute());
				}
				getListeners().stream().forEach(l -> l.beforeExecute(stepRegister));
				try {
					final Object executionResult = runAction(stepRegister);
					getListeners().stream().forEach(l -> l.afterExecute(stepRegister, executionResult));
					generateEvent(executionResult);
					for (final RuleListener l : getListeners()) {
						l.onSuccess(stepRegister, resultCondition, executionResult);
					}
				} catch (final Throwable e) {
					logger.error("Error executing rule action", e);
					getListeners().stream().forEach(l -> l.onActionError(stepRegister, e));
				}
			}
			if (isClearFactsAfterExecution()) {
				clear();
			}
			lastRun = System.currentTimeMillis();
		} catch (final Throwable e) {
			logger.error("Error evaluating rule", e);
			getListeners().stream().forEach(l -> l.onFailure(stepRegister, e));
		}
		threadCounter.decrementAndGet();
	}

	@Override
	public void fireEvent(final UaNode node, final BaseEventType event) {
		logger.info("for rule " + getBrowseName().getName() + " event fired " + event);
		factsMemory.add(createCachedRuleRecord(node, event));
		changeState();
		for (final RuleListener l : getListeners()) {
			l.onEventFired(node, event);
		}
	}

	private void generateEvent(final Object executionResult) throws UaException {
		final UUID randomUUID = UUID.randomUUID();
		final BaseEventTypeNode eventNode = waldotRulesEngine.getNamespace().getEventFactory()
				.createEvent(waldotRulesEngine.getNamespace().generateNodeId(randomUUID), Identifiers.BaseEventType);
		eventNode.setBrowseName(waldotRulesEngine.getNamespace().generateQualifiedName("RuleFiredEvent"));
		eventNode.setDisplayName(LocalizedText.english("RuleFiredEvent"));
		eventNode.setEventId(ByteString.of(randomUUID.toString().getBytes()));
		eventNode.setEventType(Identifiers.BaseEventType);
		eventNode.setSourceNode(getNodeId());
		eventNode.setSourceName(getBrowseName().getName());
		eventNode.setTime(DateTime.now());
		eventNode.setReceiveTime(DateTime.NULL_VALUE);
		if (executionResult == null) {
			eventNode.setMessage(LocalizedText.english("rule " + getBrowseName().getName() + " fired"));
		} else {
			eventNode.setMessage(LocalizedText
					.english("rule " + getBrowseName().getName() + " fired with result " + executionResult));
		}
		eventNode.setSeverity(ushort(2));
		postEvent(eventNode);
	}

	@Override
	public String getAction() {
		return action;
	}

	@Override
	public String getCondition() {
		return condition;
	}

	@Override
	public long getDefaultValidDelayMs() {
		return factsValidDelayMs;
	}

	@Override
	public long getDefaultValidUntilMs() {
		return factsValidUntilMs;
	}

	@Override
	public int getDelayBeforeEvaluation() {
		return delayBeforeEvaluation;
	}

	@Override
	public int getDelayBeforeExecute() {
		return delayBeforeExecute;
	}

	@Override
	public int getExecutionTimeout() {
		return executionTimeout;
	}

	@Override
	public Collection<CachedRuleRecord> getFacts() {
		final Collection<CachedRuleRecord> reply = new ArrayList<>();
		final List<TimerCachedMemory> toDelete = new ArrayList<>();
		for (final TimerCachedMemory memoryFact : factsMemory) {
			if (memoryFact.isValidNow()) {
				reply.add(memoryFact);
			}
			if (memoryFact.isExpired()) {
				toDelete.add(memoryFact);
			}
		}
		for (final TimerCachedMemory expiredFact : toDelete) {
			factsMemory.remove(expiredFact);
			for (final RuleListener l : getListeners()) {
				l.onFactExpired(expiredFact);
			}
		}
		return reply;
	}

	@Override
	public JexlContext getJexlContext(final ClonableMapContext baseJexlContext) {
		if (cacheJexlContext == null) {
			cacheJexlContext = new ClonableMapContext(baseJexlContext);
			cacheJexlContext.set(ConsoleStrategy.SELF_LABEL, this);
			logger.info("Created new JexlContext for rule {}", getNodeId());
		}
		return cacheJexlContext;
	}

	@Override
	public Collection<RuleListener> getListeners() {
		return waldotRulesEngine.getListeners();
	}

	@Override
	public RuleRunner getNewRunner() {
		return new RuleRunner();
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public int getRefractoryPeriodMs() {
		return refractoryPeriodMs;
	}

	@Override
	public int getRunners() {
		return threadCounter.get();
	}

	@Override
	public String getThreadName() {
		return "R[" + getNodeId() + "]";
	}

	@Override
	public boolean isClearFactsAfterExecution() {
		return clearFactsAfterExecution;
	}

	@Override
	public boolean isDirty() {
		return dirty.get();
	}

	@Override
	public boolean isParallelExecution() {
		return parallelExecution;
	}

	@Override
	public void propertyChanged(final UaNode node, final AttributeId attributeId, final Object value) {
		logger.info("for rule " + getBrowseName().getName() + " property changed node " + node.getNodeId()
				+ " attributeId " + attributeId + " value " + value);
		factsMemory.add(createCachedRuleRecord(node, attributeId, value));
		changeState();
	}

	@Override
	protected void propertyUpdateValueEvent(UaNode node, AttributeId attributeId, Object value) {
		super.propertyUpdateValueEvent(node, attributeId, value);
		// TODO aggiornare se necessario le label e i comprtamenti legati alle property
	}

	private Object runAction(final WaldotStepLogger stepRegister) throws UaException {
		return waldotRulesEngine.getJexlEngine().executeRule(waldotRulesEngine.getNamespace(), this, stepRegister);
	}

	private boolean runCheck(final WaldotStepLogger stepRegister) {
		return waldotRulesEngine.getJexlEngine().evaluateRule(waldotRulesEngine.getNamespace(), this, stepRegister);
	}

	@Override
	public void setClearFactsAfterExecution(final boolean clearFactsAfterExecution) {
		this.clearFactsAfterExecution = clearFactsAfterExecution;
	}

	@Override
	public void setDelayBeforeEvaluation(final int delayBeforeEvaluation) {
		this.delayBeforeEvaluation = delayBeforeEvaluation;
	}

	@Override
	public void setDelayBeforeExecute(final int delayBeforeExecute) {
		this.delayBeforeExecute = delayBeforeExecute;
	}

	@Override
	public void setParallelExecution(final boolean parallelExecution) {
		this.parallelExecution = parallelExecution;
	}

	@Override
	public void setRefractoryPeriodMs(final int refractoryPeriodMs) {
		this.refractoryPeriodMs = refractoryPeriodMs;
	}

}
