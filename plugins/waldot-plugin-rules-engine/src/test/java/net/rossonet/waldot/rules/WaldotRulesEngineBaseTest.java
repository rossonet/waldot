package net.rossonet.waldot.rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

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
import net.rossonet.waldot.rules.vertices.ComputeVertex;
import net.rossonet.waldot.test.WaldotTestEnvironment;
import net.rossonet.waldot.utils.LogHelper;

@TestMethodOrder(OrderAnnotation.class)
public class WaldotRulesEngineBaseTest {
	private static long longProperty(final Vertex vertex, final String key) {
		return vertex.property(key).isPresent()
				? ((Number) vertex.property(key).value()).longValue()
				: -1;
	}
	private static boolean waitFor(
			final java.util.function.BooleanSupplier condition,
			final long timeoutMs) {
		final long end = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < end) {
			try {
				if (condition.getAsBoolean()) {
					return true;
				}
				Thread.sleep(20);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return condition.getAsBoolean();
	}
	private int changes = 0;
	private Vertex compute;
	private WaldotGraph g;
	private Vertex inputA;
	private Vertex inputB;
	private Vertex inputPassive;

	private Vertex rule;

	private Vertex rule2;

	private WaldotTestClientHandler waldotTestClientHandler;

	@AfterEach
	public void afterEach() {
		clean();
		System.out.println("Test completed");

	}

	private void baseRulesGraph() throws ConfigurationException,
			InterruptedException, ExecutionException {
		simpleServerInit();
		changes = 0;
		inputA = g.addVertex("id", "a", "name", "input A", "value", "10");
		inputB = g.addVertex("id", "b", "name", "input B", "value", "20");
		inputPassive = g.addVertex("id", "p", "name", "input P", "value", "40");
		rule = g.addVertex("id", "rule", "type", "rules:rule", "name", "Regola",
				"counter", "0", "condition",
				"g.V().has('id', 'a').next().property('value').value() == 10",
				"action",
				"log.info('working with ' + cmd.getVerticesCount().toString() + ' vertices') ; counter = cmd.toNumber(g.V().has('id', 'b').next().property('value').value()).intValue() ; graph.addVertex('name','vertice N. ' + counter,'id',counter) ; self.property('counter',counter)",
				"debug", "6");
		rule2 = g.addVertex("id", "rule2", "type", "rules:rule", "name",
				"Regola Secondo", "counter", "10", "condition",
				"g.V().has('id', 'a').next().property('value').value() == 11",
				"action",
				"log.info('working with ' + cmd.getVerticesCount().toString() + ' vertices') ; counter = cmd.toNumber(g.V().has('id', 'b').next().property('value').value()).intValue(); graph.addVertex('name','vertice N. ' + counter,'id', counter + 1000) ; self.property('counter',counter)",
				"debug", "6");
		compute = g.addVertex("id", "compute", "type", "rules:compute", "name",
				"Compute");
		compute.addEdge("runner", rule, "type", "rules:execute");
		compute.addEdge("runner", rule2, "type", "rules:execute");
		inputA.addEdge("fireA", rule, "type", "fire");
		inputB.addEdge("fireB", rule, "type", "fire");
		inputA.addEdge("fireA", rule2, "type", "fire");
		inputB.addEdge("fireB", rule2, "type", "fire");
	}

	@Test
	public void baseRuleTest() throws InterruptedException, ExecutionException,
			ConfigurationException {
		LogHelper.changeJulLogLevel("fine");
		baseRulesGraph();
		Thread.sleep(1);
		change(inputB, "5");
		change(inputB, "9");
		change(inputB, "4");
		change(inputA, "11");
		change(inputB, "7");
		change(inputB, "3");
		change(inputB, "91");
		change(inputA, "10");
		change(inputB, "55");
		change(inputB, "99");
		// tutte le modifiche sono state elaborate (ogni modifica attende il
		// proprio completamento)
		assert waitFor(() -> longProperty(rule, "total") == 10
				&& longProperty(rule2, "total") == 10
				&& longProperty(rule, "queue") == 0
				&& longProperty(rule2, "queue") == 0, 15000);
		assert waitFor(() -> longProperty(rule, "executed") == 6
				&& longProperty(rule2, "executed") == 4, 15000);
		assert waldotTestClientHandler.checkOpcUaVertexExists(4);
		assert waldotTestClientHandler.checkVertexExists(4);
		assert waldotTestClientHandler.checkOpcUaVertexExists(5);
		assert waldotTestClientHandler.checkVertexExists(5);
		assert waldotTestClientHandler.checkOpcUaVertexExists(55);
		assert waldotTestClientHandler.checkVertexExists(55);
		assert waldotTestClientHandler.checkOpcUaVertexExists(9);
		assert waldotTestClientHandler.checkVertexExists(9);
		assert waldotTestClientHandler.checkOpcUaVertexExists(91);
		assert waldotTestClientHandler.checkVertexExists(91);
		assert waldotTestClientHandler.checkOpcUaVertexExists(99);
		assert waldotTestClientHandler.checkVertexExists(99);
		assert !waldotTestClientHandler.checkOpcUaVertexExists(7);
		assert !waldotTestClientHandler.checkVertexExists(7);
		assert !waldotTestClientHandler.checkOpcUaVertexExists(3);
		assert !waldotTestClientHandler.checkVertexExists(3);
		assert waldotTestClientHandler.checkOpcUaVertexExists(1003);
		assert waldotTestClientHandler.checkVertexExists(1003);
		assert waldotTestClientHandler.checkOpcUaVertexExists(1004);
		assert waldotTestClientHandler.checkVertexExists(1004);
		assert waldotTestClientHandler.checkOpcUaVertexExists(1007);
		assert waldotTestClientHandler.checkVertexExists(1007);
		assert waldotTestClientHandler.checkOpcUaVertexExists(1091);
		assert waldotTestClientHandler.checkVertexExists(1091);
		assert !waldotTestClientHandler.checkOpcUaVertexExists(1005);
		assert !waldotTestClientHandler.checkVertexExists(1005);
		assert !waldotTestClientHandler.checkOpcUaVertexExists(1055);
		assert !waldotTestClientHandler.checkVertexExists(1055);
		assert !waldotTestClientHandler.checkOpcUaVertexExists(1009);
		assert !waldotTestClientHandler.checkVertexExists(1009);
		assert !waldotTestClientHandler.checkOpcUaVertexExists(1099);
		assert !waldotTestClientHandler.checkVertexExists(1099);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule",
				"total", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule2",
				"total", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("rule", "total",
				10);
		assert waldotTestClientHandler.checkVertexValueEquals("rule2", "total",
				10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule",
				"queue", 0);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule2",
				"queue", 0);
		assert waldotTestClientHandler.checkVertexValueEquals("rule", "queue",
				0);
		assert waldotTestClientHandler.checkVertexValueEquals("rule2", "queue",
				0);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule",
				"counter", 99);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule2",
				"counter", 91);
		assert waldotTestClientHandler.checkVertexValueEquals("rule", "counter",
				99);
		assert waldotTestClientHandler.checkVertexValueEquals("rule2",
				"counter", 91);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule",
				"executed", 6);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule2",
				"executed", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("rule",
				"executed", 6);
		assert waldotTestClientHandler.checkVertexValueEquals("rule2",
				"executed", 4);
	}

	@BeforeEach
	public void beforeEach(final TestInfo testInfo) {
		System.out.println(
				"Starting test " + testInfo.getTestMethod().get().getName());
		clean();
	}

	private void bootstrapUrlServerInit(final String url)
			throws ConfigurationException, InterruptedException,
			ExecutionException {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.getOpcGraph(url, new LoggerHistoryStrategy());
		Thread.sleep(1);
		waldotTestClientHandler = new WaldotTestClientHandler(g);
	}

	/**
	 * Changes the value of an input and waits that both rules have processed the
	 * event: the actions read the live state of the graph, so the next change
	 * must not overlap with the execution of the previous one.
	 */
	private void change(final Vertex input, final String value) {
		input.property("value", value);
		changes++;
		final int expected = changes;
		assert waitFor(() -> longProperty(rule, "total") == expected
				&& longProperty(rule2, "total") == expected
				&& longProperty(rule, "queue") == 0
				&& longProperty(rule2, "queue") == 0
				&& ((ComputeVertex) compute).getRunningCount() == 0
				&& ((ComputeVertex) compute).getPendingRulesCount() == 0, 15000)
				: "events not processed for " + value;
	}

	private void clean() {
		try {
			Files.deleteIfExists(WaldotTestEnvironment.bootFile());
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
			WaldotTestEnvironment.awaitOpcPortReleased();
			Thread.sleep(1);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(WaldotTestEnvironment.clientKeystore());
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void simpleServerInit() throws ConfigurationException,
			InterruptedException, ExecutionException {
		bootstrapUrlServerInit(WaldotTestEnvironment.bootUrl());
	}
}