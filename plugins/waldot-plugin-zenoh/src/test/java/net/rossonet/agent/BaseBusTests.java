package net.rossonet.agent;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.zenoh.agent.Acme;
import net.rossonet.zenoh.client.WaldotZenohClientImpl;
import net.rossonet.zenoh.irpc.mpl.ZenohHistoryStrategy;

public class BaseBusTests {
	@Test
	public void runServer() throws Exception {
		// LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(2_000);
		g.getWaldotNamespace().close();
	}

	@Test
	public void runServerAndTwoClient() throws Exception {
		java.lang.System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
		WaldotZenohClientImpl.debugEnabled = true;
		final WaldotGraph g = OpcFactory.getOpcGraph("file:///tmp/boot.conf", new ZenohHistoryStrategy());
		Thread.sleep(2_000);
		final Acme client1 = new Acme("acme1");
		client1.startAgent();
		Thread.sleep(3_000);
		final Acme client2 = new Acme("acme2");
		client2.startAgent();
		g.getWaldotNamespace().close();
	}

}
