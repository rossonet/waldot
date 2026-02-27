package net.rossonet.agent.zenoh.rpc;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.zenoh.agent.Acme;
import net.rossonet.zenoh.client.WaldotZenohClientImpl;
import net.rossonet.zenoh.exception.WaldotZenohException;

public class AgentRpcTests {

	@SuppressWarnings("resource")
	private static GenericContainer<?> zenohRouter = new GenericContainer<>(
			DockerImageName.parse("rossonet/linneo-zenoh:latest")).withExposedPorts(7447) // mappa la porta TCP
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

	private Acme client2;

	private WaldotGraph g;

	@Test
	public void checkRegistration() throws Exception {
		client1.waitForRegistration(20, TimeUnit.SECONDS);
		client2.waitForRegistration(20, TimeUnit.SECONDS);
		System.out.println("Clients registered");
		assert client1.isRegistered();
		assert client2.isRegistered();
		final GraphTraversal<Vertex, Vertex> vers = g.traversal().V();
		boolean found1 = false;
		boolean found2 = false;
		while (vers.hasNext()) {
			final Vertex v = vers.next();
			if (((NodeId) v.id()).toParseableString().equals("ns=2;s=acme1")) {
				found1 = true;
			}
			if (((NodeId) v.id()).toParseableString().equals("ns=2;s=acme2")) {
				found2 = true;
			}
			System.out.println("Vertex: " + v);
		}
		assert found1;
		assert found2;
	}

	@Test
	public void runAddConfigurationOnAgent() throws Exception {
		client1.waitForRegistration(10, TimeUnit.SECONDS);
		client2.waitForRegistration(10, TimeUnit.SECONDS);
		Thread.sleep(2000);
		final String name = UUID.randomUUID().toString();
		final String expression = "agents_acme1_add_firenewdata.exec(\"" + name + "\")";
		g.getWaldotNamespace().runExpression(expression);
		Thread.sleep(2000);
		final GraphTraversal<Vertex, Vertex> vers = g.traversal().V();
		while (vers.hasNext()) {
			final Vertex v = vers.next();
			System.out.println("Vertex: " + v);
		}
		Thread.sleep(2000000);

	}

	@Test
	public void runBaseCommand() throws Exception {
		client1.waitForRegistration(10, TimeUnit.SECONDS);
		client2.waitForRegistration(10, TimeUnit.SECONDS);
		Thread.sleep(2000);
		System.out.println(g.getWaldotNamespace().runExpression("cmd.echo(\"Ciao!\")"));
		System.out.println(g.getWaldotNamespace().runExpression("cmd.listCommands()"));
		System.out.println(g.getWaldotNamespace().runExpression("g"));
		System.out.println(g.getWaldotNamespace().runExpression("log.info(\"Starting command test...\")"));
	}

	@Test
	public void runCommandsOnAgent() throws Exception {
		client1.waitForRegistration(10, TimeUnit.SECONDS);
		client2.waitForRegistration(10, TimeUnit.SECONDS);
		Thread.sleep(2000);
		final String randomString1 = UUID.randomUUID().toString();
		final String expression1 = "agents_acme1_set_data_test.exec(\"" + randomString1 + "\")";
		g.getWaldotNamespace().runExpression(expression1);
		Thread.sleep(2000);
		final String randomString2 = UUID.randomUUID().toString();
		final String expression2 = "agents_acme2_set_data_test.exec(\"" + randomString2 + "\")";
		g.getWaldotNamespace().runExpression(expression2);
		Thread.sleep(2000);
		assert client1.getTestData().equals(randomString1);
		assert client2.getTestData().equals(randomString2);
	}

	@BeforeEach
	public void setupEach() throws InterruptedException, ExecutionException, WaldotZenohException {
		java.lang.System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
		WaldotZenohClientImpl.debugEnabled = false;
		g = OpcFactory.getOpcGraph("file:///tmp/boot.conf", new LoggerHistoryStrategy());
		client1 = new Acme("acme1");
		client2 = new Acme("acme2");
		client1.startAgent();
		client2.startAgent();
	}

	@AfterEach
	public void tearDown() throws InterruptedException, ExecutionException, WaldotZenohException {
		client1.stop();
		client2.stop();
		g.getWaldotNamespace().getOpcuaServer().close();

	}
}
