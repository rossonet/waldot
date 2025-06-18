package net.rossonet.waldot.api.rules;

/**
 * extends Thread to add a Rule and a WaldotStepLogger.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public final class WaldotRuleThread extends Thread {

	private Rule rule;
	private final WaldotStepLogger stepRegister;

	public WaldotRuleThread(final Runnable target, final WaldotStepLogger stepRegister) {
		super(target);
		this.stepRegister = stepRegister;
		stepRegister.onThreadRegistered();
	}

	public Rule getRule() {
		return rule;
	}

	public WaldotStepLogger getStepRegister() {
		return stepRegister;
	}

	public void setRule(final Rule rule) {
		this.rule = rule;
		setPriority(rule.getPriority());
		setName(rule.getThreadName());
	}
}
