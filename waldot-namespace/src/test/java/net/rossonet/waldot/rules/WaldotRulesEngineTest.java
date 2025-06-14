package net.rossonet.waldot.rules;

import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.api.LoggerListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.logger.TraceLogger;
import net.rossonet.waldot.utils.LogHelper;

@TestMethodOrder(OrderAnnotation.class)
public class WaldotRulesEngineTest {

	private final LoggerListener logger = new LoggerListener() {

		@Override
		public void onEvent(String messagePattern, Object[] arguments, Throwable throwable) {
			System.out.println("LOGGER: " + messagePattern + " -> " + arguments + " [ "
					+ (throwable != null ? throwable.getMessage() : "") + " ] ");

		}
	};

	@Test
	public void runSimpleQueryAndWaitTwoMinutes()
			throws InterruptedException, ExecutionException, ConfigurationException {
		LogHelper.changeJulLogLevel("fine");
		final WaldotGraph g = OpcFactory.getOpcGraph();
		((TraceLogger) g.getWaldotNamespace().getRulesLogger()).getDebugObservers().add(logger);
		Thread.sleep(2_000);
		final Vertex test1 = g.addVertex("label", "test1");
		final Vertex test2 = g.addVertex("label", "test2");
		final Vertex rule1 = g.addVertex("label", "rule1", "type-node-id", "rule", "condition", "true");
		final Vertex rule2 = g.addVertex("label", "rule2", "type-node-id", "rule");
		test1.addEdge("fire", rule1);
		test2.addEdge("fire", rule2);
		rule1.addEdge("fire", rule2);
		for (int counter = 0; counter < 100; counter++) {
			Thread.sleep(5_000);
			test1.property("inc", counter);
			Thread.sleep(5_000);
			test2.property("inc", counter * 2);
			Thread.sleep(5_000);
			test1.addEdge("link_" + counter, test2);
		}
		Thread.sleep(60_000);
	}

}