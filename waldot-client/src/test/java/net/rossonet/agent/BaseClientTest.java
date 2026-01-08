package net.rossonet.agent;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient;
import net.rossonet.waldot.agent.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.agent.client.v1.DefaultWaldOTAgentClientConfigurationV1;
import net.rossonet.waldot.agent.client.v1.WaldOTAgentClientImplV1;

@TestMethodOrder(OrderAnnotation.class)
public class BaseClientTest {

	@Test
	public void runClient() {
		final WaldOTAgentClientConfiguration configuration = new DefaultWaldOTAgentClientConfigurationV1();
		final WaldOTAgentClient client = new WaldOTAgentClientImplV1(configuration);
		// TODO test client
	}

}