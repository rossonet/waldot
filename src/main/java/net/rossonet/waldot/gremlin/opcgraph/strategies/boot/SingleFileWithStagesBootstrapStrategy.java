package net.rossonet.waldot.gremlin.opcgraph.strategies.boot;

import net.rossonet.waldot.api.BootstrapProcedureStrategy;
import net.rossonet.waldot.namespaces.HomunculusNamespace;

public class SingleFileWithStagesBootstrapStrategy implements BootstrapProcedureStrategy {

	private HomunculusNamespace waldotNamespace;

	@Override
	public AgentStatus getAgentStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize(HomunculusNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;

	}

	@Override
	public void runBootstrapProcedure() {
		// TODO Auto-generated method stub

	}

}
