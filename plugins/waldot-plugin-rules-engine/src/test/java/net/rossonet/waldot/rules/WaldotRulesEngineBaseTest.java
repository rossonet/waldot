package net.rossonet.waldot.rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

@TestMethodOrder(OrderAnnotation.class)
public class WaldotRulesEngineBaseTest {
	private WaldotGraph g;
	private WaldotTestClientHandler waldotTestClientHandler;

	@AfterEach
	public void afterEach() {
		clean();
		System.out.println("Test completed");

	}

	private void baseRulesGraph() throws ConfigurationException, InterruptedException, ExecutionException {
		simpleServerInit();
		final Vertex inputA = g.addVertex("id", "a", "name", "input A", "value", "10");
		final Vertex inputB = g.addVertex("id", "b", "name", "input B", "value", "20");
		final Vertex inputPassive = g.addVertex("id", "p", "name", "input P", "value", "40");
		final Vertex rule = g.addVertex("id", "rule", "type", "rule", "name", "Regola", "condition", "", "action", "");
		final Vertex compute = g.addVertex("id", "compute", "type", "compute", "name", "Compute", "condition", "",
				"action", "");
		final Edge executeEdge = compute.addEdge("runner", rule, "type", "execute");
		final Edge fireEdgeA = inputA.addEdge("fireA", rule, "type", "fire");
		final Edge fireEdgeB = inputB.addEdge("fireB", rule, "type", "fire");
	}

	@Test
	public void baseRuleTest() throws InterruptedException, ExecutionException, ConfigurationException {
		LogHelper.changeJulLogLevel("fine");
		baseRulesGraph();
		Thread.sleep(4_000);
	}

	@BeforeEach
	public void beforeEach(final TestInfo testInfo) {
		System.out.println("Starting test " + testInfo.getTestMethod().get().getName());
		clean();
	}

	private void bootstrapUrlServerInit(final String url)
			throws ConfigurationException, InterruptedException, ExecutionException {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.getOpcGraph(url, new LoggerHistoryStrategy());
		Thread.sleep(500);
		waldotTestClientHandler = new WaldotTestClientHandler(g);
	}

	private void clean() {
		try {
			Files.deleteIfExists(Path.of("/tmp/boot.conf"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(Path.of("/tmp/index.txt"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		if (waldotTestClientHandler != null) {
			try {
				waldotTestClientHandler.disconnect();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		if (g != null && g.getWaldotNamespace() != null) {
			try {

				g.getWaldotNamespace().close();
				System.out.println("Graph namespace closed");
				g = null;
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		try {
			while (!NetworkHelper.checkLocalPortAvailable(12686)) {
				System.out.println("Waiting for server shutdown");
				Thread.sleep(5_000);
			}
			Thread.sleep(500);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(Path.of("/tmp/waldot-client.ks"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void simpleServerInit() throws ConfigurationException, InterruptedException, ExecutionException {
		bootstrapUrlServerInit("file:///tmp/boot.conf");
	}
}