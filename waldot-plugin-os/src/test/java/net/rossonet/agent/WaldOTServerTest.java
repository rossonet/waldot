package net.rossonet.agent;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;

public class WaldOTServerTest {
	@Test
	public void runServer() throws Exception {
		LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(500);

		// Thread.sleep(120_000);
		g.getWaldotNamespace().getOpcuaServer().close();
	}
}
