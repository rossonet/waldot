package net.rossonet.waldot.rules;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.types.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.rules.ExecutorHelper;
import net.rossonet.waldot.api.rules.ExecutorHelper.EvaluationType;
import net.rossonet.waldot.api.rules.Rule;
import net.rossonet.waldot.api.rules.WaldotRulesEngine;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.jexl.JexlExecutorHelper;

public class DefaultRulesEngine implements WaldotRulesEngine {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final WaldotNamespace waldotNamespace;
	private final Map<NodeId, Rule> rules = new HashMap<>();
	private final List<RuleListener> listeners = new ArrayList<>();
	private final ExecutorHelper jexlEngine = new JexlExecutorHelper();

	public DefaultRulesEngine(WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		jexlEngine.setFunctionObject(ConsoleStrategy.G_LABEL, waldotNamespace.getGremlinGraph());
		jexlEngine.setFunctionObject(ConsoleStrategy.LOG_LABEL, waldotNamespace.getRulesLogger());
		jexlEngine.setFunctionObject(ConsoleStrategy.COMMANDS_LABEL, waldotNamespace.getCommandsAsFunction());
	}

	@Override
	public void addListener(RuleListener listener) {
		listeners.add(listener);
	}

	@Override
	public void deregisterRule(NodeId ruleNodeId) {
		rules.remove(ruleNodeId);
	}

	private void evaluate(Rule rule, EvaluationType evalutationType, UaNode node, AttributeId attributeId, Object value,
			BaseEventType event) {
		try {
			for (final RuleListener l : listeners) {
				final boolean lr = l.beforeEvaluate(evalutationType, node, attributeId, value, event);
				if (!lr) {
					logger.info("Rule evaluation stopped by listener");
					return;
				}
			}
			boolean conditionPassed = false;
			try {
				final boolean condition = jexlEngine.evaluateRule(waldotNamespace, rule, evalutationType, node,
						attributeId, value, event);
				conditionPassed = condition;
				listeners.stream()
						.forEach(l -> l.afterEvaluate(evalutationType, node, attributeId, value, event, condition));
			} catch (final Throwable e) {
				logger.error("Error evaluating rule", e);
				conditionPassed = false;
				listeners.stream()
						.forEach(l -> l.onEvaluationError(evalutationType, node, attributeId, value, event, e));
			}
			if (conditionPassed) {
				final UUID randomUUID = UUID.randomUUID();
				final BaseEventTypeNode eventNode = waldotNamespace.getEventFactory()
						.createEvent(waldotNamespace.generateNodeId(randomUUID), Identifiers.BaseEventType);
				eventNode.setBrowseName(waldotNamespace.generateQualifiedName("RuleFiredEvent"));
				eventNode.setDisplayName(LocalizedText.english("RuleFiredEvent"));
				eventNode.setEventId(ByteString.of(randomUUID.toString().getBytes()));
				eventNode.setEventType(Identifiers.BaseEventType);
				eventNode.setSourceNode(rule.getNodeId());
				eventNode.setSourceName(rule.getLabel());
				eventNode.setTime(DateTime.now());
				eventNode.setReceiveTime(DateTime.NULL_VALUE);
				if (node != null) {
					eventNode.setMessage(LocalizedText
							.english("rule " + rule.getLabel() + " fired evaluating " + evalutationType + " on "
									+ node.getNodeId() + " with attribute " + attributeId + " and value " + value));
				} else if (event != null) {
					eventNode.setMessage(LocalizedText.english(
							"rule " + rule.getLabel() + " fired evaluating " + evalutationType + " on event " + event));
				} else {
					eventNode.setMessage(LocalizedText.english("rule " + rule.getLabel() + " fired"));
				}
				eventNode.setSeverity(ushort(2));
				rule.fireEvent(eventNode);
				listeners.stream().forEach(l -> l.beforeExecute(evalutationType, node, attributeId, value, event));
				final Object executionResult = jexlEngine.executeRule(waldotNamespace, rule, evalutationType, node,
						attributeId, value, event);
				listeners.stream().forEach(
						l -> l.afterExecute(evalutationType, node, attributeId, value, event, executionResult));
			}
			for (final RuleListener l : listeners) {
				l.onSuccess(evalutationType, node, attributeId, value, event, conditionPassed);
			}

		} catch (final Throwable e) {
			logger.error("Error evaluating rule", e);
			listeners.stream().forEach(l -> l.onFailure(evalutationType, node, attributeId, value, event, e));
		}

	}

	@Override
	public void evaluateRuleForAttributeChanged(Rule rule, UaNode node, AttributeId attributeId, Object value) {
		evaluate(rule, EvaluationType.ATTRIBUTE, node, attributeId, value, null);

	}

	@Override
	public void evaluateRuleForEvent(Rule rule, BaseEventType event) {
		evaluate(rule, EvaluationType.EVENT, null, null, null, event);

	}

	@Override
	public void evaluateRuleForPropertyChanged(Rule rule, UaNode node, AttributeId attributeId, Object value) {
		evaluate(rule, EvaluationType.PROPERTY, node, attributeId, value, null);

	}

	@Override
	public Collection<RuleListener> getListeners() {
		return listeners;
	}

	@Override
	public void registerObserver(WaldotVertex eventVertex, NodeId ruleNodeId) {
		if (rules.containsKey(ruleNodeId)) {
			eventVertex.addAttributeObserver(rules.get(ruleNodeId));
			eventVertex.addPropertyObserver(rules.get(ruleNodeId));
			eventVertex.addEventObserver(rules.get(ruleNodeId));
		} else {
			throw new IllegalArgumentException("Rule not found");

		}
	}

	@Override
	public void registerOrUpdateRule(NodeId ruleNodeId, String label, String description, String condition,
			String action, int priority) {
		rules.put(ruleNodeId, new DefaultRule(this, ruleNodeId, label, description, condition, action, priority));
	}

	@Override
	public void removeListener(RuleListener listener) {
		listeners.remove(listener);
	}

}
