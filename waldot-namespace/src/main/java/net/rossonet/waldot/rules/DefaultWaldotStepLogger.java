package net.rossonet.waldot.rules;

import java.time.Instant;

import org.apache.commons.jexl3.JexlContext;

import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.rules.WaldotStepLogger;

public class DefaultWaldotStepLogger implements WaldotStepLogger {

	private final StringBuilder log = new StringBuilder();

	@Override
	public void onActionCompiled(long executionTimeMs) {
		log.append(Instant.now().toString() + " - action compiled in ").append(executionTimeMs).append("ms\n");
	}

	@Override
	public void onActionExecutionException(JexlContext jexlContext, Exception exception) {
		log.append(Instant.now().toString() + " - action execution exception: ").append(exception.getMessage())
				.append("\n");
	}

	@Override
	public void onAfterActionExecution(long executionTimeMs, Object result) {
		log.append(Instant.now().toString() + " - action executed in ").append(executionTimeMs).append("ms\n");
	}

	@Override
	public void onAfterConditionExecution(long executionTimeMs, Object result) {
		log.append(Instant.now().toString() + " - condition executed in ").append(executionTimeMs).append("ms\n");
	}

	@Override
	public void onBeforeActionExecution(JexlContext jexlContext) {
		log.append(Instant.now().toString() + " - before action execution\n");
	}

	@Override
	public void onBeforeConditionExecution(JexlContext jexlContext) {
		log.append(Instant.now().toString() + " - before condition execution\n");
	}

	@Override
	public void onConditionCompiled(long executionTimeMs) {
		log.append(Instant.now().toString() + " - condition compiled in ").append(executionTimeMs).append("ms\n");
	}

	@Override
	public void onConditionExecutionException(JexlContext jexlContext, Exception exception) {
		log.append(Instant.now().toString() + " - condition execution exception: ").append(exception.getMessage())
				.append("\n");
	}

	@Override
	public void onEvaluateStoppedByListener(RuleListener listener) {
		log.append(Instant.now().toString() + " - evaluate stopped by listener " + listener + "\n");
	}

	@Override
	public void onThreadRegistered() {
		log.append(Instant.now().toString() + " - thread registered\n");
	}

	@Override
	public String toString() {
		return log.toString();
	}

}
