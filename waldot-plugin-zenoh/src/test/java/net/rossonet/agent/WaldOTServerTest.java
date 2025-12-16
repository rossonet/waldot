package net.rossonet.agent;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.zenoh.agent.Acme;
import net.rossonet.zenoh.client.WaldotZenohClientImpl;
import net.rossonet.zenoh.impl.ZenohHistoryStrategy;

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
		java.lang.System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
		WaldotZenohClientImpl.debugEnabled = true;
		final WaldotGraph g = OpcFactory.getOpcGraph(new ZenohHistoryStrategy());
		Thread.sleep(5_000);
		final Acme client1 = new Acme("acme1");
		client1.startAgent();
		Thread.sleep(60_000);
		final Acme client2 = new Acme("acme2");
		client2.startAgent();
		Thread.sleep(120_000 * 10);
		g.getWaldotNamespace().getOpcuaServer().close();
	}

}
