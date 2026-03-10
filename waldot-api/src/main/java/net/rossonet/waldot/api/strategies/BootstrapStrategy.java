package net.rossonet.waldot.api.strategies;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * BootstrapStrategy is an interface that defines the strategy for
 * bootstrapping a Waldot agent. It provides methods to initialize the agent,
 * run the bootstrap procedure, and check the agent's status.
 * 
 * <p>BootstrapStrategy handles the initialization of the WaldOT agent, including:
 * Loading configuration, setting up the OPC UA server, registering types,
 * and preparing the graph for operation.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Initialize bootstrap strategy
 * BootstrapStrategy bootstrap = new MyBootstrapStrategy();
 * bootstrap.initialize(waldotNamespace);
 * 
 * // Check status before running
 * if (bootstrap.getAgentStatus() == BootstrapStrategy.AgentStatus.INIT) {
 *     bootstrap.runBootstrapProcedure();
 * }
 * 
 * // After bootstrap completes
 * switch (bootstrap.getAgentStatus()) {
 *     case READY: System.out.println("Agent ready"); break;
 *     case FAULT: System.out.println("Bootstrap failed"); break;
 * }
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface BootstrapStrategy extends AutoCloseable {

	/**
	 * Enumeration of possible agent status values.
	 */
	public enum AgentStatus {
		/** Agent is starting up */
		BOOT,
		/** Agent encountered a fault */
		FAULT,
		/** Agent is initializing */
		INIT,
		/** Agent is ready for operation */
		READY
	}

	/**
	 * Returns the current agent status.
	 * 
	 * <p>This indicates the state of the bootstrap procedure and whether
	 * the agent is ready for operation.</p>
	 * 
	 * @return the current AgentStatus
	 */
	AgentStatus getAgentStatus();

	/**
	 * Initializes the bootstrap strategy with a namespace.
	 * 
	 * <p>This should be called before runBootstrapProcedure(). It prepares
	 * the strategy with necessary configuration and resources.</p>
	 * 
	 * @param waldotNamespace the namespace to use for initialization
	 * @see WaldotNamespace
	 */
	void initialize(WaldotNamespace waldotNamespace);

	/**
	 * Runs the bootstrap procedure.
	 * 
	 * <p>This performs all initialization steps needed to start the agent,
	 * including loading configuration, setting up OPC UA nodes, and preparing
	 * the graph. The status can be checked via getAgentStatus().</p>
	 * 
	 * @see #getAgentStatus()
	 */
	void runBootstrapProcedure();

}
