package net.rossonet.zenoh.irpc.mpl;

import java.util.HashMap;
import java.util.Map;

import net.rossonet.zenoh.rpc.controller.AgentLifeCycleManager;
import net.rossonet.zenoh.rpc.controller.AgentStore;
import net.rossonet.zenoh.rpc.controller.ZenohAgent;

public class BaseAgentStore implements AgentStore {

	private AgentLifeCycleManager agentLifeCycleManager;
	private final Map<String, ZenohAgent> managedAgents = new HashMap<>();

	public AgentLifeCycleManager getAgentLifeCycleManager() {
		return agentLifeCycleManager;
	}

	@Override
	public long getPeriodicallyCheckIntervalMs() {
		// TODO renderere configurabile
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
	public void setAgentLifeCycleManager(AgentLifeCycleManager agentLifeCycleManager) {
		this.agentLifeCycleManager = agentLifeCycleManager;
	}

}
