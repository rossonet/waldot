package net.rossonet.waldot.rules;

import net.rossonet.waldot.api.rules.WaldotRuleThread;
import net.rossonet.waldot.api.rules.WaldotThreadFactory;

public class DefaultThreadFactory implements WaldotThreadFactory {

	@Override
	public Thread newThread(Runnable target) {
		final WaldotRuleThread thread = new WaldotRuleThread(target, new DefaultWaldotStepLogger());
		return thread;
	}

}
