package net.rossonet.zenoh.controller;

public interface LifeCycleStrategy {

	long getPeriodicallyCheckIntervalMs();

	void periodicallyCheck();

	boolean registerNewAgent(ZenohAgent agent);

	void setLifeCycleManager(AgentLifeCycleManager agentLifeCycleManager);

}
