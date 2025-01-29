package net.rossonet.waldot.api.rules;

import java.util.Collection;

import org.eclipse.milo.opcua.sdk.server.model.types.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotVertex;

public interface WaldotRulesEngine {

	void addListener(RuleListener listener);

	void deregisterRule(NodeId ruleNodeId);

	void evaluateRuleForAttributeChanged(Rule rule, UaNode node, AttributeId attributeId, Object value);

	void evaluateRuleForEvent(Rule rule, BaseEventType event);

	void evaluateRuleForPropertyChanged(Rule rule, UaNode node, AttributeId attributeId, Object value);

	Collection<RuleListener> getListeners();

	void registerObserver(WaldotVertex eventVertex, NodeId ruleNodeId);

	void registerOrUpdateRule(NodeId ruleNodeId, String label, String condition, String action, int priority);

	void removeListener(RuleListener listener);

}
