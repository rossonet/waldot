package net.rossonet.waldot.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.rules.WaldotRuleThread;
import net.rossonet.waldot.api.rules.WaldotThreadFactory;

public class DefaultThreadFactory implements WaldotThreadFactory {

	private static final Logger logger = LoggerFactory.getLogger("RE Thread Factory");

	@Override
	public Thread newThread(final Runnable target) {
		final WaldotRuleThread thread = new WaldotRuleThread(target, new DefaultWaldotStepLogger());
		logger.debug("Creating new thread");
		return thread;
	}

}
