package net.rossonet.waldot.api.rules;

public final class WaldotRuleThread extends Thread {

	private Rule rule;
	private final WaldotStepLogger stepRegister;

	public WaldotRuleThread(Runnable target, WaldotStepLogger stepRegister) {
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

	public void setRule(Rule rule) {
		this.rule = rule;
		setPriority(rule.getPriority());
		setName(rule.getThreadName());
	}
}
