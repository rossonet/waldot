package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;

public interface WaldotRulesEngine {

	void deregister(NodeId ruleNodeId);

	void evaluateRuleForAttributeChanged(Rule defaultRule, UaNode node, AttributeId attributeId, Object value);

	void evaluateRuleForPropertyChanged(Rule defaultRule, UaNode node, AttributeId attributeId, Object value);

	void registerObserver(OpcVertex eventVertex, NodeId ruleNodeId);

	void registerOrUpdate(NodeId ruleNodeId, String label, String condition, String action, int priority);

}
