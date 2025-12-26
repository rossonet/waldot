package net.rossonet.agent.zenoh.rpc;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.zenoh.agent.Acme;
import net.rossonet.zenoh.client.WaldotZenohClientImpl;
import net.rossonet.zenoh.exception.WaldotZenohException;
import net.rossonet.zenoh.impl.ZenohHistoryStrategy;

public class AgentRpcTests {

	private static GenericContainer<?> zenohRouter = new GenericContainer<>(
			DockerImageName.parse("eclipse/zenoh:latest")).withExposedPorts(7447) // mappa la porta TCP
			.waitingFor(org.testcontainers.containers.wait.strategy.Wait.forListeningPort());

	@BeforeAll
	public static void setupAll() throws InterruptedException, ExecutionException {
		zenohRouter.start();

	}

	@AfterAll
	public static void tearDownAll() throws InterruptedException, ExecutionException {
		zenohRouter.stop();
	}

	private Acme client1;

	private WaldotGraph g;

	@Test
	public void checkRegistration() throws Exception {
		client1.startAgent();
		client1.waitForRegistration(10, TimeUnit.SECONDS);
		System.out.println("Client registered");
		final GraphTraversal<Vertex, Vertex> vers = g.traversal().V();
		while (vers.hasNext()) {
			final Vertex v = vers.next();
			System.out.println("Vertex: " + v);
		}
	}

	@BeforeEach
	public void setupEach() throws InterruptedException, ExecutionException, WaldotZenohException {
		java.lang.System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
		WaldotZenohClientImpl.debugEnabled = true;
		g = OpcFactory.getOpcGraph(new ZenohHistoryStrategy());
		client1 = new Acme("acme1");
	}

	@AfterEach
	public void tearDown() throws InterruptedException, ExecutionException, WaldotZenohException {
		client1.stop();
		g.getWaldotNamespace().getOpcuaServer().close();

	}
}
