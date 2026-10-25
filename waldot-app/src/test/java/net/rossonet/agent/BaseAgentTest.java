package net.rossonet.agent;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

@TestMethodOrder(OrderAnnotation.class)
public class BaseAgentTest {

	private WaldotGraph g;

	@AfterEach
	public void afterEach() {
		WaldotTestEnvironment.cleanup(g, null);
		g = null;
	}

	private void create() throws InterruptedException, ExecutionException {
		try {
			g = OpcFactory.getOpcGraph();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	// FIXME: import export graph
	@Disabled("At this time doesn't work")
	@Test
	public void runGratefulDeadOneMinutes() throws InterruptedException, ExecutionException {
		g = OpcFactory.createGratefulDead();
		Thread.sleep(1);
	}

	@Test
	public void runServerOneMinutes() throws InterruptedException, ExecutionException {
		create();
		Thread.sleep(1);
	}

}