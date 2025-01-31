package net.rossonet.waldot.api.rules;

import java.util.Collection;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;

public interface WaldotRulesEngine {

	void addListener(RuleListener listener);

	void deregisterRule(NodeId ruleNodeId);

	ExecutorHelper getJexlEngine();

	Collection<RuleListener> getListeners();

	WaldotNamespace getNamespace();

	void registerObserver(WaldotVertex eventVertex, NodeId ruleNodeId);

	void registerOrUpdateRule(Rule rule);

	void removeListener(RuleListener listener);

}
