package net.rossonet.waldot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */

@WaldotPlugin
public class WaldotReinforcementLearningPlugin implements PluginListener {
	static WaldotNamespace mainNamespace;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private WaldotNamespace waldotNamespace;

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		logger.info("Initializing Waldot Reinforcement Learning Plugin");
	}

	@Override
	public void start() {
		// TODO: Reinforcement Learning Plugin Start Logic
	}

	@Override
	public void stop() {
		// TODO: Reinforcement Learning Plugin Stop Logic
	}

}
