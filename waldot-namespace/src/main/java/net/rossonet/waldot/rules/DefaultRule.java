package net.rossonet.waldot.rules;

import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.rules.Rule;

public class DefaultRule implements Rule {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final NodeId nodeId;
	private final String condition;
	private final String action;
	private final int priority;

	private final String label;

	private final DefaultRulesEngine waldotRulesEngine;

	public DefaultRule(DefaultRulesEngine waldotRulesEngine, NodeId rule, String label, String condition, String action,
			int priority) {
		this.waldotRulesEngine = waldotRulesEngine;
		this.nodeId = rule;
		this.label = label;
		this.condition = condition;
		this.action = action;
		this.priority = priority;
	}

	@Override
	public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
		logger.info("for rule " + label + " attribute changed node " + node.getNodeId() + " attributeId " + attributeId
				+ " value " + value);
		waldotRulesEngine.evaluateRuleForAttributeChanged(this, node, attributeId, value);
	}

	@Override
	public void fireEvent(BaseEventTypeNode event) {
		logger.info("for rule " + label + " event fired " + event);
		waldotRulesEngine.evaluateRuleForEvent(this, event);
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
	public String getLabel() {
		return label;
	}

	@Override
	public NodeId getNodeId() {
		return nodeId;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public void propertyChanged(UaNode node, AttributeId attributeId, Object value) {
		logger.info("for rule " + label + " property changed node " + node.getNodeId() + " attributeId " + attributeId
				+ " value " + value);
		waldotRulesEngine.evaluateRuleForPropertyChanged(this, node, attributeId, value);
	}

}
