package net.rossonet.waldot.api.strategies;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * BootstrapProcedureStrategy is an interface that defines the strategy for
 * bootstrapping a Waldot agent. It provides methods to initialize the agent,
 * run the bootstrap procedure, and check the agent's status.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface BootstrapProcedureStrategy {

	public enum AgentStatus {
		INIT, BOOT, FAULT, READY
	}

	AgentStatus getAgentStatus();

	void initialize(WaldotNamespace waldotNamespace);

	void runBootstrapProcedure();

}
