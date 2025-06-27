package net.rossonet.waldot.agent.client.v1;

import java.util.UUID;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient;
import net.rossonet.waldot.agent.exception.ProvisioningException;

public class ProvisioningLifeCycle {

	private final WaldOTAgentClient waldOTAgentClient;
	private final String requestUniqueCode = UUID.randomUUID().toString();

	public ProvisioningLifeCycle(final WaldOTAgentClient waldOTAgentClient) {
		this.waldOTAgentClient = waldOTAgentClient;
	}

	public String getRequestUniqueCode() {
		return requestUniqueCode;
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
