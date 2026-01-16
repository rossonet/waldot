package net.rossonet.waldot.jexl;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.commons.jexl3.JexlContext;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotVertex;

/**
 * The {@code Rule} interface represents a rule in the Waldot system. It extends
 * {@link WaldotVertex} and provides methods to manage rule execution, conditions,
 * actions, and listeners. Rules are a core component of the rule engine, enabling
 * dynamic evaluation and execution of logic based on facts and events.
 *
 * <p>Rules consist of conditions and actions. Conditions determine whether a rule
 * should be executed, while actions define the operations to perform when the rule
 * is triggered. The rule engine evaluates rules using the {@link RuleExecutor}
 * and manages their lifecycle, including registration, updates, and execution.</p>
 *
 * @see WaldotVertex
 * @see CachedRuleRecord
 * @see RuleExecutor
 * @see RuleListener
 * @see Fact
 */
/**
 * Rule interface represents a rule in the Waldot system. It extends
 * WaldotVertex and provides methods to manage rule execution, conditions,
 * actions, and listeners.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface Rule extends WaldotVertex, AutoCloseable, EventObserver, PropertyObserver {

	/**
	 * Clears all internal state and resets the rule.
	 */
	void clear();

	/**
	 * Retrieves the action associated with the rule.
	 *
	 * @return the action as a string.
	 */
	String getAction();

	/**
	 * Retrieves the condition associated with the rule.
	 *
	 * @return the condition as a string.
	 */
	String getCondition();

	/**
	 * Retrieves the default validity delay in milliseconds.
	 *
	 * @return the default validity delay.
	 */
	long getDefaultValidDelayMs();

	/**
	 * Retrieves the default validity expiration time in milliseconds.
	 *
	 * @return the default validity expiration time.
	 */
	long getDefaultValidUntilMs();

	/**
	 * Retrieves the delay before the rule is evaluated.
	 *
	 * @return the delay in milliseconds.
	 */
	int getDelayBeforeEvaluation();

	/**
	 * Retrieves the delay before the rule is executed.
	 *
	 * @return the delay in milliseconds.
	 */
	int getDelayBeforeExecute();

	/**
	 * Retrieves the execution timeout for the rule.
	 *
	 * @return the timeout in milliseconds.
	 */
	int getExecutionTimeout();

	/**
	 * Retrieves the collection of facts associated with the rule.
	 *
	 * @return a collection of {@link CachedRuleRecord} instances.
	 */
	Collection<CachedRuleRecord> getFacts();

	/**
	 * Retrieves the JEXL context for the rule, based on a given base context.
	 *
	 * @param baseJexlContext the base {@link ClonableMapContext}.
	 * @return the {@link JexlContext} for the rule.
	 */
	JexlContext getJexlContext(ClonableMapContext baseJexlContext);

	/**
	 * Retrieves the collection of listeners associated with the rule.
	 *
	 * @return a collection of {@link RuleListener} instances.
	 */
	Collection<RuleListener> getListeners();

	/**
	 * Retrieves a callable that creates a new runner for the rule.
	 *
	 * @return a {@link Callable} that produces a {@link WaldotStepLogger}.
	 */
	Callable<WaldotStepLogger> getNewRunner();

	/**
	 * Retrieves the priority of the rule.
	 *
	 * @return the priority as an integer.
	 */
	int getPriority();

	/**
	 * Retrieves the refractory period of the rule in milliseconds.
	 *
	 * @return the refractory period.
	 */
	int getRefractoryPeriodMs();

	/**
	 * Retrieves the number of runners currently associated with the rule.
	 *
	 * @return the number of runners.
	 */
	public int getRunners();

	/**
	 * Retrieves the thread name associated with the rule.
	 *
	 * @return the thread name as a string.
	 */
	String getThreadName();

	/**
	 * Checks whether facts are cleared after the rule is executed.
	 *
	 * @return {@code true} if facts are cleared, {@code false} otherwise.
	 */
	public boolean isClearFactsAfterExecution();

	/**
	 * Checks whether the rule is marked as dirty.
	 *
	 * @return {@code true} if the rule is dirty, {@code false} otherwise.
	 */
	public boolean isDirty();

	/**
	 * Checks whether the rule supports parallel execution.
	 *
	 * @return {@code true} if parallel execution is supported, {@code false}
	 *         otherwise.
	 */
	public boolean isParallelExecution();

	/**
	 * Sets whether facts should be cleared after the rule is executed.
	 *
	 * @param clearFactsAfterExecution {@code true} to clear facts, {@code false}
	 *                                 otherwise.
	 */
	void setClearFactsAfterExecution(boolean clearFactsAfterExecution);

	/**
	 * Sets the delay before the rule is evaluated.
	 *
	 * @param delayBeforeEvaluation the delay in milliseconds.
	 */
	void setDelayBeforeEvaluation(int delayBeforeEvaluation);

	/**
	 * Sets the delay before the rule is executed.
	 *
	 * @param delayBeforeExecute the delay in milliseconds.
	 */
	void setDelayBeforeExecute(int delayBeforeExecute);

	/**
	 * Sets whether the rule supports parallel execution.
	 *
	 * @param parallelExecution {@code true} to enable parallel execution,
	 *                          {@code false} otherwise.
	 */
	void setParallelExecution(boolean parallelExecution);

	/**
	 * Sets the refractory period of the rule in milliseconds.
	 *
	 * @param refractoryPeriodMs the refractory period.
	 */
	void setRefractoryPeriodMs(int refractoryPeriodMs);

}
