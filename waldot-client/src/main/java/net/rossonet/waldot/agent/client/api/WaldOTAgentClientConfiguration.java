package net.rossonet.waldot.agent.client.api;

import java.io.Serializable;

import net.rossonet.waldot.agent.client.v1.DefaultWaldOTAgentClientConfigurationV1;

public interface WaldOTAgentClientConfiguration extends Serializable {

	static WaldOTAgentClientConfiguration getDefaultConfiguration() {

		return new DefaultWaldOTAgentClientConfigurationV1();
	}

	boolean hasCertificateAuthentication();

	boolean hasProvisioningToken();

}
