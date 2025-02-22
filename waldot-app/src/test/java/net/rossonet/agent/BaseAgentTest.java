package net.rossonet.agent;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;

@TestMethodOrder(OrderAnnotation.class)
public class BaseAgentTest {

	private void create() throws InterruptedException, ExecutionException {
		try {
			OpcFactory.getOpcGraph();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void runGratefulDeadOneMinutes() throws InterruptedException, ExecutionException {
		OpcFactory.createGratefulDead();
		Thread.sleep(60_000);
	}

	@Test
	public void runServerOneMinutes() throws InterruptedException, ExecutionException {
		create();
		Thread.sleep(60_000);
	}

}