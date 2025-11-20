package net.rossonet.agent;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.zenoh.agent.Acme;

public class WaldOTServerTest {
	@Test
	public void runServer() throws Exception {
		// LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(120_000);
		g.getWaldotNamespace().getOpcuaServer().close();
	}

	@Test
	public void runServerAndTwoClient() throws Exception {
		// LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(5_000);
		final Acme client1 = new Acme("client1");
		client1.startAgent();
		Thread.sleep(60_000);
		final Acme client2 = new Acme("client2");
		client2.startAgent();
		Thread.sleep(120_000);
		g.getWaldotNamespace().getOpcuaServer().close();
	}

}
