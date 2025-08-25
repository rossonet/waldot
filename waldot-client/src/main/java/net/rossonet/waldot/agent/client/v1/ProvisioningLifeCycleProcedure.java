package net.rossonet.waldot.agent.client.v1;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient;
import net.rossonet.waldot.agent.exception.ProvisioningException;

public class ProvisioningLifeCycleProcedure {
	private final static Logger logger = LoggerFactory.getLogger(ProvisioningLifeCycleProcedure.class);
	private boolean manualRequestCompleted = false;
	private final String requestUniqueCode = UUID.randomUUID().toString();
	private final WaldOTAgentClient waldOTAgentClient;

	public ProvisioningLifeCycleProcedure(final WaldOTAgentClient waldOTAgentClient) {
		this.waldOTAgentClient = waldOTAgentClient;
	}

	private void checkManualRequestCompleted() {
		// TODO Auto-generated method stub
		manualRequestCompleted = false;
	}

	public String getRequestUniqueCode() {
		return requestUniqueCode;
	}

	public boolean isManualRequestCompleted() {
		if (!manualRequestCompleted) {
			checkManualRequestCompleted();
		}
		return manualRequestCompleted;
	}

	public void requestManualApprovation() throws ProvisioningException {
		// TODO Auto-generated method stub
	}

	public void tokenProvisioning() throws ProvisioningException {
		// TODO Auto-generated method stub

	}

}
