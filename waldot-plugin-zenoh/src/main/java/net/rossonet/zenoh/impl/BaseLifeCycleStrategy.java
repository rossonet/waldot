package net.rossonet.zenoh.impl;

import java.util.HashMap;
import java.util.Map;

import net.rossonet.zenoh.controller.AgentLifeCycleManager;
import net.rossonet.zenoh.controller.LifeCycleStrategy;
import net.rossonet.zenoh.controller.ZenohAgent;

public class BaseLifeCycleStrategy implements LifeCycleStrategy {

	private AgentLifeCycleManager agentLifeCycleManager;
	private final Map<String, ZenohAgent> managedAgents = new HashMap<>();

	@Override
	public long getPeriodicallyCheckIntervalMs() {
		// TODO Auto-generated method stub
		return 1000;
	}

	@Override
	public void periodicallyCheck() {
		for (final ZenohAgent agent : managedAgents.values()) {
			agent.checkAgentStatus();
		}
	}

	@Override
	public boolean registerNewAgent(ZenohAgent agent) {
		managedAgents.put(agent.getUniqueId(), agent);
		return true;
	}

	@Override
	public void setLifeCycleManager(AgentLifeCycleManager agentLifeCycleManager) {
		this.agentLifeCycleManager = agentLifeCycleManager;

	}

}
