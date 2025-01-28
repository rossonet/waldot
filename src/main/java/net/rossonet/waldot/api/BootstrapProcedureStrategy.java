package net.rossonet.waldot.api;

import net.rossonet.waldot.namespaces.HomunculusNamespace;

public interface BootstrapProcedureStrategy {

	public enum AgentStatus {
		INIT, BOOTING, ERROR, READY
	}

	AgentStatus getAgentStatus();

	void initialize(HomunculusNamespace waldotNamespace);

	void runBootstrapProcedure();

}
