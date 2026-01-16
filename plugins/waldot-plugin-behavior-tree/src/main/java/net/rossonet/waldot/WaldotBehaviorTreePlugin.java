package net.rossonet.waldot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotBehaviorTreePlugin implements PluginListener {
	static WaldotNamespace mainNamespace;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private WaldotNamespace waldotNamespace;

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		logger.info("Initializing Behavior Tree Plugin...");
	}

	@Override
	public void start() {
//TODO behaviour tree plugin
	}

	@Override
	public void stop() {
		// TODO behaviour tree plugin
	}

}
