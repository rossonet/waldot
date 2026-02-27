package net.rossonet.waldot.rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.apache.commons.lang3.RandomUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

	@BeforeEach
	public void beforeEach(TestInfo testInfo) {
		System.out.println("Starting test " + testInfo.getTestMethod().get().getName());
		clean();
	}

	private void bootstrapUrlServerInit(String url)
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

	@Test
	public void fireEdgeTest() throws Exception {
		simpleServerInit();
	}

	@Disabled
	@Test
	public void runComplexRuleTest() throws InterruptedException, ExecutionException, ConfigurationException {
		LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		// ((TraceLogger)
		// g.getWaldotNamespace().getRulesLogger()).getDebugObservers().add(logger);
		final Vertex test1 = g.addVertex("label", "test1");
		final Vertex test2 = g.addVertex("label", "test2");
		final Vertex rule1 = g.addVertex("label", "rule1", "type-node-id", "rule", "condition", "true", "action",
				"let i = 33 ; log.info('!!! FireableAbstractOpcVertex 1 executed with ' + i);");
		final Vertex rule2 = g.addVertex("label", "rule2", "type-node-id", "rule", "condition", "true", "action",
				"cmd.echo(self)");
		test1.addEdge("fire", rule1);
		test2.addEdge("fire", rule2);
		rule1.addEdge("fire", rule2);
		for (int counter = 0; counter < 20; counter++) {
			test1.property("inc", counter);
			Thread.sleep(5000);
			test2.property("inc", counter * 2);
			Thread.sleep(5000);
			test1.addEdge("link_" + counter, test2);
			Thread.sleep(5000);
			test1.property("inc", RandomUtils.nextInt(0, 100));
			Thread.sleep(400);
			test1.property("inc", RandomUtils.nextInt(0, 100));
			Thread.sleep(200);
			test1.property("inc", RandomUtils.nextInt(0, 100));
			Thread.sleep(1000);
			test1.property("inc", RandomUtils.nextInt(0, 100));
		}
		Thread.sleep(4_000);
	}

	private void simpleServerInit() throws ConfigurationException, InterruptedException, ExecutionException {
		bootstrapUrlServerInit("file:///tmp/boot.conf");
	}
}