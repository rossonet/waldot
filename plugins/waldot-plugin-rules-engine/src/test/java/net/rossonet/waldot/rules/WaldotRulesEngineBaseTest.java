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
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

@TestMethodOrder(OrderAnnotation.class)
public class WaldotRulesEngineBaseTest {
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

	private void baseRulesGraph() throws ConfigurationException, InterruptedException, ExecutionException {
		simpleServerInit();
		inputA = g.addVertex("id", "a", "name", "input A", "value", "10");
		inputB = g.addVertex("id", "b", "name", "input B", "value", "20");
		inputPassive = g.addVertex("id", "p", "name", "input P", "value", "40");
		rule = g.addVertex("id", "rule", "type", "rule", "name", "Regola", "counter", "0", "condition",
				"g.V().has('id', 'a').next().property('value').value() == 10", "action",
				"log.info('working with ' + cmd.getVerticesCount().toString() + ' vertices') ; counter = cmd.toNumber(g.V().has('id', 'b').next().property('value').value()).intValue() ; graph.addVertex('name','vertice N. ' + counter,'id',counter) ; self.property('counter',counter)",
				"debug", "6");
		rule2 = g.addVertex("id", "rule2", "type", "rule", "name", "Regola Secondo", "counter", "10", "condition",
				"g.V().has('id', 'a').next().property('value').value() == 11", "action",
				"log.info('working with ' + cmd.getVerticesCount().toString() + ' vertices') ; counter = cmd.toNumber(g.V().has('id', 'b').next().property('value').value()).intValue(); graph.addVertex('name','vertice N. ' + counter,'id', counter + 1000) ; self.property('counter',counter)",
				"debug", "6");
		compute = g.addVertex("id", "compute", "type", "compute", "name", "Compute");
		compute.addEdge("runner", rule, "type", "execute");
		compute.addEdge("runner", rule2, "type", "execute");
		inputA.addEdge("fireA", rule, "type", "fire");
		inputB.addEdge("fireB", rule, "type", "fire");
		inputA.addEdge("fireA", rule2, "type", "fire");
		inputB.addEdge("fireB", rule2, "type", "fire");
	}

	@Test
	public void baseRuleTest() throws InterruptedException, ExecutionException, ConfigurationException {
		LogHelper.changeJulLogLevel("fine");
		baseRulesGraph();
		Thread.sleep(500);
		inputB.property("value", "5");
		Thread.sleep(500);
		inputB.property("value", "9");
		Thread.sleep(500);
		inputB.property("value", "4");
		Thread.sleep(500);
		inputA.property("value", "11");
		Thread.sleep(500);
		inputB.property("value", "7");
		Thread.sleep(500);
		inputB.property("value", "3");
		Thread.sleep(500);
		inputB.property("value", "91");
		Thread.sleep(500);
		inputA.property("value", "10");
		Thread.sleep(500);
		inputB.property("value", "55");
		Thread.sleep(500);
		inputB.property("value", "99");
		Thread.sleep(500);
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
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule", "total", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule2", "total", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("rule", "total", 10);
		assert waldotTestClientHandler.checkVertexValueEquals("rule2", "total", 10);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule", "queue", 0);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule2", "queue", 0);
		assert waldotTestClientHandler.checkVertexValueEquals("rule", "queue", 0);
		assert waldotTestClientHandler.checkVertexValueEquals("rule2", "queue", 0);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule", "counter", 99);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule2", "counter", 91);
		assert waldotTestClientHandler.checkVertexValueEquals("rule", "counter", 99);
		assert waldotTestClientHandler.checkVertexValueEquals("rule2", "counter", 91);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule", "executed", 6);
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("rule2", "executed", 4);
		assert waldotTestClientHandler.checkVertexValueEquals("rule", "executed", 6);
		assert waldotTestClientHandler.checkVertexValueEquals("rule2", "executed", 4);
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