package net.rossonet.waldot.api.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * extends Thread to add a Rule and a WaldotStepLogger.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public final class WaldotRuleThread extends Thread {

	private static final Logger logger = LoggerFactory.getLogger("RE Thread");

	private Rule rule;
	private final WaldotStepLogger stepRegister;

	public WaldotRuleThread(final Runnable target, final WaldotStepLogger stepRegister) {
		super(target);
		this.stepRegister = stepRegister;
	}

	public Rule getRule() {
		return rule;
	}

	public WaldotStepLogger getStepRegister() {
		return stepRegister;
	}

	public void setRule(final Rule newRule) {
		this.rule = newRule;
		setPriority(newRule.getPriority());
		setName(newRule.getThreadName());
		logger.info("Rule {} execution on this thread with priority {}", newRule.label(), getPriority());
		stepRegister.onThreadRegistered(getName(), getPriority());
	}
}
