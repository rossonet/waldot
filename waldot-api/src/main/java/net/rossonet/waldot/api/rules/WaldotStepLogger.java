package net.rossonet.waldot.api.rules;

import org.apache.commons.jexl3.JexlContext;

import net.rossonet.waldot.api.RuleListener;

/**
 * WaldotStepLogger is an interface that defines methods for logging various
 * steps in the execution of rules within the Waldot framework. It provides
 * hooks for logging actions and conditions, including compilation, execution,
 * exceptions, and listener interactions.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotStepLogger {

	void onActionCompiled(long executionTimeMs);

	void onActionExecutionException(JexlContext jexlContext, Exception exception);

	void onAfterActionExecution(long executionTimeMs, Object result);

	void onAfterConditionExecution(long executionTimeMs, Object result);

	void onBeforeActionExecution(JexlContext jexlContext);

	void onBeforeConditionExecution(JexlContext jexlContext);

	void onConditionCompiled(long executionTimeMs);

	void onConditionExecutionException(JexlContext jexlContext, Exception exception);

	void onEvaluateStoppedByListener(RuleListener listener);

	void onThreadRegistered();
}
