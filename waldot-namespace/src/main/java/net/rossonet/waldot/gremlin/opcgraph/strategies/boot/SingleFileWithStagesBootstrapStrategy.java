package net.rossonet.waldot.gremlin.opcgraph.strategies.boot;

import net.rossonet.waldot.api.annotation.WaldotBootStrategy;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.BootstrapProcedureStrategy;

@WaldotBootStrategy
public class SingleFileWithStagesBootstrapStrategy implements BootstrapProcedureStrategy {

	@SuppressWarnings("unused")
	private WaldotNamespace waldotNamespace;

	@Override
	public AgentStatus getAgentStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize(WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;

	}

	@Override
	public void runBootstrapProcedure() {
		// TODO Auto-generated method stub

	}

}
