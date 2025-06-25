package net.rossonet.waldot.agent.client.v1;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient;
import net.rossonet.waldot.agent.exception.ProvisioningException;

public class ProvisioningLifeCycle {

	private final WaldOTAgentClient waldOTAgentClient;

	public ProvisioningLifeCycle(final WaldOTAgentClient waldOTAgentClient) {
		this.waldOTAgentClient = waldOTAgentClient;
	}

	public String getRequestUniqueCode() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isManualRequestCompleted() {
		// TODO Auto-generated method stub
		return false;
	}

	public void requestManualApprovation() throws ProvisioningException {
		// TODO Auto-generated method stub
	}

	public void tokenProvisioning() throws ProvisioningException {
		// TODO Auto-generated method stub

	}

}
