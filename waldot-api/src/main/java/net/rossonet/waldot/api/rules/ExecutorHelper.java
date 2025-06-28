package net.rossonet.waldot.api.rules;

import org.apache.commons.jexl3.JexlContext;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * The {@code ExecutorHelper} interface defines methods for evaluating rules,
 * executing expressions, and managing contexts and function objects within a
 * {@link WaldotNamespace}. It serves as a core component of the rule engine,
 * enabling dynamic evaluation and execution of logic based on conditions and
 * actions.
 *
 * <p>
 * Rules in the Waldot system are evaluated using the
 * {@link #evaluateRule(WaldotNamespace, Rule, WaldotStepLogger)} method, which
 * processes a {@link Rule} within a specific namespace and logs execution steps
 * using a {@link WaldotStepLogger}. The interface also provides methods for
 * executing expressions with or without a {@link JexlContext}, and for managing
 * custom contexts and function objects.
 * </p>
 *
 * @see WaldotNamespace
 * @see Rule
 * @see WaldotStepLogger
 * @see JexlContext
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface ExecutorHelper extends AutoCloseable {

	public enum EvaluationType {
		ATTRIBUTE, EVENT, PROPERTY
	}

	/**
	 * Evaluates a rule within a specific namespace and logs execution steps.
	 *
	 * @param waldotNamespace the {@link WaldotNamespace} in which the rule is
	 *                        evaluated.
	 * @param rule            the {@link Rule} to be evaluated.
	 * @param stepRegister    the {@link WaldotStepLogger} used to log execution
	 *                        steps.
	 * @return {@code true} if the rule evaluation succeeds, {@code false}
	 *         otherwise.
	 */
	boolean evaluateRule(WaldotNamespace waldotNamespace, Rule rule, WaldotStepLogger stepRegister);

	/**
	 * Executes an expression without a context.
	 *
	 * @param expression the expression to execute.
	 * @return the result of the expression execution.
	 */
	Object execute(String expression);

	/**
	 * Executes an expression with a specified context.
	 *
	 * @param expression  the expression to execute.
	 * @param jexlContext the {@link JexlContext} providing variables and functions
	 *                    for the execution.
	 * @return the result of the expression execution.
	 */
	Object execute(String expression, JexlContext jexlContext);

	/**
	 * Executes a rule within a specific namespace and logs execution steps.
	 *
	 * @param waldotNamespace the {@link WaldotNamespace} in which the rule is
	 *                        executed.
	 * @param rule            the {@link Rule} to be executed.
	 * @param stepRegister    the {@link WaldotStepLogger} used to log execution
	 *                        steps.
	 * @return the result of the rule execution.
	 */
	Object executeRule(WaldotNamespace waldotNamespace, Rule rule, WaldotStepLogger stepRegister);

	/**
	 * Sets a custom context object identified by an ID.
	 *
	 * @param id      the unique identifier for the context.
	 * @param context the context object to set.
	 */
	void setContext(String id, Object context);

	/**
	 * Sets a custom function object identified by an ID.
	 *
	 * @param id       the unique identifier for the function.
	 * @param function the function object to set.
	 */
	void setFunctionObject(String id, Object function);

}