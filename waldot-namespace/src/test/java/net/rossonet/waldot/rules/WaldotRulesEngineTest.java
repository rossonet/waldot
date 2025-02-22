package net.rossonet.waldot.rules;

import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.LogHelper;

@TestMethodOrder(OrderAnnotation.class)
public class WaldotRulesEngineTest {

	@Test
	public void runSimpleQueryAndWaitTwoMinutes()
			throws InterruptedException, ExecutionException, ConfigurationException {
		LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(2_000);
		final Vertex test1 = g.addVertex("label", "test1");
		final Vertex test2 = g.addVertex("label", "test1");
		final Vertex rule1 = g.addVertex("label", "rule1", "type-node-id", "rule");
		final Vertex rule2 = g.addVertex("label", "rule1", "type-node-id", "rule");
		test1.addEdge("fire", rule1);
		test2.addEdge("fire", rule2);

	}

}