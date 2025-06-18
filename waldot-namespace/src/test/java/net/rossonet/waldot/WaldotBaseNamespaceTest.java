package net.rossonet.waldot;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

@TestMethodOrder(OrderAnnotation.class)
public class WaldotBaseNamespaceTest {

	@AfterEach
	public void afterEach() {
		try {
			while (!NetworkHelper.checkLocalPortAvailable(12686)) {
				System.out.println("Waiting for server shutdown");
				Thread.sleep(5_000);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void runKitchenSink() throws Exception {
		final WaldotGraph d = OpcFactory.createKitchenSink();
		Thread.sleep(4_000);
		d.getWaldotNamespace().getOpcuaServer().close();
	}

	@Test
	public void runLarge() throws Exception {
		LogHelper.changeJulLogLevel("warning");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(1_000);
		final Vertex b = g.addVertex("label", "link_all", "directory", "base");
		for (int i = 1; i < 10_000; i++) {
			final Vertex c = g.addVertex("label", "nodo " + i, "number-test", i, "directory", "catalogo " + i % 7);
			final Vertex r = g.addVertex("label", "rule_" + i, "type-node-id", "rule", "directory",
					"catalogo " + i % 7);
			c.addEdge("fire", r);
			b.addEdge("link", c);
			Thread.sleep(4);
		}
		Thread.sleep(4_000);
		g.getWaldotNamespace().getOpcuaServer().close();
	}

	@Test
	public void runModern() throws Exception {
		final WaldotGraph d = OpcFactory.createModern();
		Thread.sleep(4_000);
		d.getWaldotNamespace().getOpcuaServer().close();
	}

	@Test
	public void runOneQuery() throws Exception {
		LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(500);
		g.addVertex("label", "PrimoVertice", "id", "ciao", "custom-data", "variable1", "number-test", 10);
		g.addVertex("label", "SecondoVertice", "id", 111, "custom-data", "variable1", "number-test", 10);
		Thread.sleep(4_000);
		g.getWaldotNamespace().getOpcuaServer().close();
	}

	@Test
	public void runServerTwoMinutes() throws Exception {
		final WaldotGraph d = OpcFactory.getOpcGraph();
		Thread.sleep(120_000);
		d.getWaldotNamespace().getOpcuaServer().close();
	}

	@Test
	public void runSimpleQuery() throws Exception {
		LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(500);
		final Vertex v1 = g.addVertex("label", "PrimoVertice", "custom-data", "variable1", "number-test", 10);
		System.out.println("1 - " + v1);
		final Vertex v2 = g.addVertex("label", "SecondoVertice", "custom-data", "variable2", "directory",
				"archivio directory", "number-test", 10.9, "p1", "1", "description",
				"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
		final Vertex v3 = g.addVertex("label", "Custom Vertice", "directory", "archivio directory", "type-node-id",
				"i=2013");
		final Vertex rule = g.addVertex("label", "Test Rule 1", "type-node-id", "rule", "directory",
				"archivio directory");
		v3.addEdge("fire", rule);
		v1.addEdge("fire", rule);
		System.out.println("2a - " + v2);
		System.out.println("2 - " + v3 + " ");
		System.out.println("3 - " + v1.addEdge("example edge", v2, "variabile1", "testo", "variabile2", 34L,
				"directory", "prova directory"));
		System.out.println("4 - " + g.traversal().V().toList());
		System.out.println("5 - " + g.traversal().V().hasLabel("SecondoVertice").toSet());
		System.out.println("6 - " + g.traversal().V().hasLabel("SecondoVertice").next().property("custom-data", "terzo",
				"p1", "2", "p2", "ok"));
		System.out.println("7 - " + g.traversal().E().toList());
		System.out.println("8 - " + g.traversal().V().hasLabel("SecondoVertice").next().getClass().toGenericString());
		System.out.println("9 - " + g.traversal().V().hasLabel("SecondoVertice").next().value("number-test"));
		System.out.println("10 - " + g.traversal().E().hasLabel("example edge").next().value("variabile2"));
		System.out
				.println("11 - " + IteratorUtils.asList(g.traversal().V().hasLabel("SecondoVertice").next().values()));
		System.out.println("12 - " + IteratorUtils.asList(g.traversal().E().hasLabel("example edge").next().values()));
		System.out.println("13 - " + IteratorUtils.asList(g.traversal().V().hasLabel("SecondoVertice").next().keys()));
		System.out.println("14 - " + IteratorUtils.asList(g.traversal().E().hasLabel("example edge").next().keys()));
		System.out.println(g.getWaldotNamespace().runExpression("'15 - primo script!'"));
		System.out.println(g.getWaldotNamespace().runExpression("'16 - ' + g.traversal().V().toList()"));
		g.getWaldotNamespace().runExpression("log.info('17 - prova log')");
		System.out.flush();
		Thread.sleep(500);
		for (int i = 1; i < 100; i++) {
			final Vertex c = g.addVertex("label", "nodo " + i, "number-test", i, "directory", "catalogo " + i % 7);
			System.out.println("a1" + i + " - " + c);
			final Vertex r = g.addVertex("label", "rule_" + i, "type-node-id", "rule", "directory",
					"catalogo " + i % 7);
			c.addEdge("fire", r);
			System.out.println("a2" + i + " - " + c.addEdge("edge numero " + i % 8, v1, "contatore", i, "description",
					"la variabile generata in automatico numero " + i));
			System.out.println("a3 - " + g.traversal().V().hasLabel("SecondoVertice").next().value("number-test"));
			Thread.sleep(5);
		}
		for (int i = 1; i < 5; i++) {
			Thread.sleep(250);
			System.out.println(
					"b1" + i + " - " + g.traversal().V().hasLabel("PrimoVertice").next().property("number-test", i));
		}
		g.getWaldotNamespace().getOpcuaServer().close();
	}

	@Test
	public void runTheCrew() throws Exception {
		final WaldotGraph d = OpcFactory.createTheCrew();
		Thread.sleep(4_000);
		d.getWaldotNamespace().getOpcuaServer().close();
	}

}