package net.rossonet.waldot.api.strategies;

import net.rossonet.waldot.api.models.WaldotNamespace;

public interface BootstrapProcedureStrategy {

	public enum AgentStatus {
		INIT, BOOTING, ERROR, READY
	}

	AgentStatus getAgentStatus();

	void initialize(WaldotNamespace waldotNamespace);

	void runBootstrapProcedure();

}
