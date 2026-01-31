package net.rossonet.zenoh.controller;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.handlers.Callback;
import io.zenoh.sample.Sample;

public class AgentDiscoveryHandler implements Callback<Sample> {
	private static final Logger logger = LoggerFactory.getLogger(AgentDiscoveryHandler.class);
	private final AgentLifeCycleManager lifeCycleManager;

	public AgentDiscoveryHandler(AgentLifeCycleManager lifeCycleManager) {
		this.lifeCycleManager = lifeCycleManager;
	}

	@Override
	public void run(Sample sample) {
		JSONObject payloadJson = null;
		try {
			payloadJson = new JSONObject(sample.getPayload().toString());
		} catch (final Exception e) {
			logger.error("Error parsing discovery message payload: {}", sample.getPayload(), e);
		}
		if (payloadJson == null) {
			logger.warn("Received invalid discovery message: {}", sample.getPayload());
			return;
		} else {
			lifeCycleManager.discoveryFromAgentReceived(sample.getKeyExpr().toString(), payloadJson);
		}
	}

}
