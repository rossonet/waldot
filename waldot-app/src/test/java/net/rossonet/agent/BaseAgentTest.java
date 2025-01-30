package net.rossonet.agent;

import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.WaldotOpcUaServer;
import net.rossonet.waldot.configuration.HomunculusConfiguration;
import net.rossonet.waldot.configuration.OpcUaConfiguration;
import net.rossonet.waldot.utils.LogHelper;

@TestMethodOrder(OrderAnnotation.class)
public class BaseAgentTest {

	private WaldotOpcUaServer waldot;

	@Test
	public void runServerTwominutes() throws InterruptedException, ExecutionException {
		final HomunculusConfiguration configuration = HomunculusConfiguration.getDefault();
		final OpcUaConfiguration serverConfiguration = OpcUaConfiguration.getDefault();
		waldot = new WaldotOpcUaServer(configuration, serverConfiguration);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					if (waldot != null) {
						waldot.shutdown();
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
		waldot.startup().get();
		Thread.sleep(120_000);
	}

	@Test
	public void runSimpleQueryAndWaitTwominutes()
			throws InterruptedException, ExecutionException, ConfigurationException {
		LogHelper.changeJulLogLevel("fine");
		final HomunculusConfiguration configuration = HomunculusConfiguration.getDefault();
		final OpcUaConfiguration serverConfiguration = OpcUaConfiguration.getDefault();
		waldot = new WaldotOpcUaServer(configuration, serverConfiguration);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					if (waldot != null) {
						waldot.shutdown();
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
		waldot.startup().get();
		Thread.sleep(2_000);
		final Vertex v1 = waldot.getGremlinGraph().addVertex("label", "PrimoVertice", "custom-data", "variable1",
				"number-test", 10);
		System.out.println("1 - " + v1);
		final Vertex v2 = waldot.getGremlinGraph().addVertex("label", "SecondoVertice", "custom-data", "variable2",
				"directory", "archivio directory", "number-test", 10.9, "p1", "1", "description",
				"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
		final Vertex v3 = waldot.getGremlinGraph().addVertex("label", "Custom Vertice", "directory",
				"archivio directory", "type-node-id", "i=2013");
		final Vertex rule = waldot.getGremlinGraph().addVertex("label", "Test Rule 1", "type-node-id", "rule",
				"directory", "archivio directory");
		v3.addEdge("fire", rule);
		v1.addEdge("fire", rule);
		System.out.println("2a - " + v2);
		System.out.println("2 - " + v3 + " ");
		System.out.println("3 - " + v1.addEdge("example edge", v2, "variabile1", "testo", "variabile2", 34L,
				"directory", "prova directory"));
		System.out.println("4 - " + waldot.getGremlinGraph().traversal().V().toList());
		System.out.println("5 - " + waldot.getGremlinGraph().traversal().V().hasLabel("SecondoVertice").toSet());
		System.out.println("6 - " + waldot.getGremlinGraph().traversal().V().hasLabel("SecondoVertice").next()
				.property("custom-data", "terzo", "p1", "2", "p2", "ok"));
		System.out.println("7 - " + waldot.getGremlinGraph().traversal().E().toList());
		System.out.println("8 - " + waldot.getGremlinGraph().traversal().V().hasLabel("SecondoVertice").next()
				.getClass().toGenericString());
		System.out.println("9 - "
				+ waldot.getGremlinGraph().traversal().V().hasLabel("SecondoVertice").next().value("number-test"));
		System.out.println(
				"10 - " + waldot.getGremlinGraph().traversal().E().hasLabel("example edge").next().value("variabile2"));
		System.out.println("11 - " + IteratorUtils
				.asList(waldot.getGremlinGraph().traversal().V().hasLabel("SecondoVertice").next().values()));
		System.out.println("12 - " + IteratorUtils
				.asList(waldot.getGremlinGraph().traversal().E().hasLabel("example edge").next().values()));
		System.out.println("13 - " + IteratorUtils
				.asList(waldot.getGremlinGraph().traversal().V().hasLabel("SecondoVertice").next().keys()));
		System.out.println("14 - " + IteratorUtils
				.asList(waldot.getGremlinGraph().traversal().E().hasLabel("example edge").next().keys()));
		System.out.println(waldot.runExpression("'15 - primo script!'"));
		System.out.println(waldot.runExpression("'16 - ' + g.traversal().V().toList()"));
		waldot.runExpression("log.info('17 - prova log')");
		System.out.flush();
		Thread.sleep(5_000);
		for (int i = 1; i < 100; i++) {
			final Vertex c = waldot.getGremlinGraph().addVertex("label", "nodo " + i, "number-test", i, "directory",
					"catalogo " + i % 7);
			System.out.println("a1" + i + " - " + c);
			final Vertex r = waldot.getGremlinGraph().addVertex("label", "rule_" + i, "type-node-id", "rule",
					"directory", "catalogo " + i % 7);
			c.addEdge("fire", r);
			System.out.println("a2" + i + " - " + c.addEdge("edge numero " + i % 8, v1, "contatore", i, "description",
					"la variabile generata in automatico numero " + i));
			System.out.println("a3 - "
					+ waldot.getGremlinGraph().traversal().V().hasLabel("SecondoVertice").next().value("number-test"));
		}
		for (int i = 1; i < 12 * 5; i++) {
			Thread.sleep(5_000);
			System.out.println("b1" + i + " - " + waldot.getGremlinGraph().traversal().V().hasLabel("PrimoVertice")
					.next().property("number-test", i));
		}
	}

}