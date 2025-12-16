package net.rossonet.waldot.api.rules;

import java.util.Collection;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;

/**
 * The {@code WaldotRulesEngine} interface defines methods for managing rules in the Waldot system.
 * It provides functionality for adding and removing listeners, registering and updating rules,
 * and managing observers. The rules engine integrates with other components such as
 * {@link Rule}, {@link CachedRuleRecord}, and {@link RuleExecutorHelper} to evaluate and execute rules.
 *
 * <p>Rules in the Waldot system are defined using the {@link Rule} interface, which includes
 * conditions, actions, and execution details. The rules engine facilitates the lifecycle
 * of these rules, including registration, updates, and deregistration.</p>
 *
 * @see Rule
 * @see CachedRuleRecord
 * @see RuleExecutorHelper
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotRulesEngine {

	/**
	 * Adds a listener to the rules engine.
	 *
	 * @param listener the {@link RuleListener} to be added.
	 */
	void addListener(RuleListener listener);

	/**
	 * Deregisters a rule from the rules engine.
	 *
	 * @param ruleNodeId the {@link NodeId} of the rule to be deregistered.
	 */
	void deregisterRule(NodeId ruleNodeId);

	/**
	 * Retrieves the {@link RuleExecutorHelper} used by the rules engine for evaluating rules
	 * and executing expressions.
	 *
	 * @return the {@link RuleExecutorHelper} instance.
	 */
	RuleExecutorHelper getJexlEngine();

	/**
	 * Retrieves all listeners currently registered with the rules engine.
	 *
	 * @return a collection of {@link RuleListener} instances.
	 */
	Collection<RuleListener> getListeners();

	/**
	 * Retrieves the {@link WaldotNamespace} associated with the rules engine.
	 *
	 * @return the {@link WaldotNamespace} instance.
	 */
	WaldotNamespace getNamespace();

	/**
	 * Registers an observer for a specific event vertex and associates it with a rule.
	 *
	 * @param eventVertex the {@link WaldotVertex} representing the event to observe.
	 * @param ruleNodeId the {@link NodeId} of the rule to associate with the observer.
	 */
	void registerObserver(WaldotVertex eventVertex, NodeId ruleNodeId);

	/**
	 * Registers or updates a rule in the rules engine.
	 *
	 * @param rule the {@link Rule} to be registered or updated.
	 */
	void registerOrUpdateRule(Rule rule);

	/**
	 * Removes a listener from the rules engine.
	 *
	 * @param listener the {@link RuleListener} to be removed.
	 */
	void removeListener(RuleListener listener);

}