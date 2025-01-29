package net.rossonet.waldot.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.server.model.types.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.rules.ExecutorHelper.EvaluationType;
import net.rossonet.waldot.api.rules.Rule;
import net.rossonet.waldot.api.rules.WaldotRulesEngine;
import net.rossonet.waldot.jexl.JexlExecutorHelper;

public class DefaultRulesEngine implements WaldotRulesEngine {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final WaldotNamespace waldotNamespace;
	private final Map<NodeId, Rule> rules = new HashMap<>();
	private final List<RuleListener> listeners = new ArrayList<>();

	public DefaultRulesEngine(WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
	}

	@Override
	public void addListener(RuleListener listener) {
		listeners.add(listener);
	}

	@Override
	public void deregisterRule(NodeId ruleNodeId) {
		rules.remove(ruleNodeId);
	}

	private void evaluate(EvaluationType attribute, UaNode node, AttributeId attributeId, Object value) {
		try {
			for (final RuleListener l : listeners) {
				final boolean lr = l.beforeEvaluate(attribute, node, attributeId, value);
				if (!lr) {
					logger.info("Rule evaluation stopped by listener");
					return;
				}
			}
			boolean conditionPassed = false;
			try {
				final boolean condition = JexlExecutorHelper.getInstance().evaluateRule(waldotNamespace, attribute,
						node, attributeId, value);
				conditionPassed = condition;
				listeners.stream().forEach(l -> l.afterEvaluate(attribute, node, attributeId, value, condition));
			} catch (final Throwable e) {
				logger.error("Error evaluating rule", e);
				conditionPassed = false;
				listeners.stream().forEach(l -> l.onEvaluationError(attribute, node, attributeId, value, e));
			}
			if (conditionPassed) {
				listeners.stream().forEach(l -> l.beforeExecute(attribute, node, attributeId, value));
				final Object executionResult = JexlExecutorHelper.getInstance().executeRule(waldotNamespace, attribute,
						node, attributeId, value);
				listeners.stream().forEach(l -> l.afterExecute(attribute, node, attributeId, value, executionResult));
			}
			for (final RuleListener l : listeners) {
				l.onSuccess(attribute, node, attributeId, value, conditionPassed);
			}
		} catch (final Throwable e) {
			logger.error("Error evaluating rule", e);
			listeners.stream().forEach(l -> l.onFailure(attribute, node, attributeId, value, e));
		}
	}

	@Override
	public void evaluateRuleForAttributeChanged(Rule rule, UaNode node, AttributeId attributeId, Object value) {
		evaluate(EvaluationType.ATTRIBUTE, node, attributeId, value);

	}

	@Override
	public void evaluateRuleForEvent(Rule defaultRule, BaseEventType event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void evaluateRuleForPropertyChanged(Rule rule, UaNode node, AttributeId attributeId, Object value) {
		evaluate(EvaluationType.PROPERTY, node, attributeId, value);

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
	public void registerOrUpdateRule(NodeId ruleNodeId, String label, String condition, String action, int priority) {
		rules.put(ruleNodeId, new DefaultRule(this, ruleNodeId, label, condition, action, priority));
	}

	@Override
	public void removeListener(RuleListener listener) {
		listeners.remove(listener);
	}

}
