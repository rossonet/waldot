package old;

public interface AgentStore {

	long getPeriodicallyCheckIntervalMs();

	void periodicallyCheck();

	boolean registerNewAgent(ZenohAgent agent);

	void setAgentLifeCycleManager(AgentLifeCycleManager agentLifeCycleManager);

}
