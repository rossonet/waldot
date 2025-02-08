package net.rossonet.waldot.api.rules;

import org.apache.commons.jexl3.JexlContext;

import net.rossonet.waldot.api.RuleListener;

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
