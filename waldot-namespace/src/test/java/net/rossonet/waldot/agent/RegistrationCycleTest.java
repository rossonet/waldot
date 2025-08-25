package net.rossonet.waldot.agent;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient;
import net.rossonet.waldot.agent.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.agent.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.agent.client.api.WaldotAgentClientObserver;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;

public class RegistrationCycleTest {

	@Test
	public void runClientTwoMinutes() throws Exception {
		final WaldotGraph d = OpcFactory.getOpcGraph();
		final WaldOTAgentClientConfiguration configuration = WaldOTAgentClientConfiguration.getDefaultConfiguration();
		final WaldOTAgentClient client = WaldOTAgentClient.withConfiguration(configuration);
		client.setStatusObserver(new WaldotAgentClientObserver() {

			@Override
			public void onStatusChanged(final Status status) {
				System.out.println("Status changed: " + status);
			}
		});
		client.startConnectionProcedure();
		final int limit = 120 / 5;
		for (int i = 0; i < limit; i++) {
			final WaldOTAgentClient.Status status = client.getStatus();
			Thread.sleep(5000);
		}
		client.stopConnectionProcedure();
	}

}
