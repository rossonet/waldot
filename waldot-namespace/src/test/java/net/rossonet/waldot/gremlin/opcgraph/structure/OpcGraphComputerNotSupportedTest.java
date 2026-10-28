package net.rossonet.waldot.gremlin.opcgraph.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputer;
import net.rossonet.waldot.testutil.FakeNamespaces;

/**
 * GraphComputer (OLAP) is not supported: the graph must say so in its features
 * and fail immediately, without starting any computation.
 */
class OpcGraphComputerNotSupportedTest {

	private OpcGraph graph;

	@BeforeEach
	void setUp() {
		graph = OpcGraph.open();
		graph.setNamespace(FakeNamespaces.empty());
	}

	@AfterEach
	void tearDown() {
		graph.close();
	}

	@Test
	void featuresDoNotAdvertiseTheComputer() {
		assertFalse(graph.features().graph().supportsComputer());
	}

	@Test
	void computeFailsWithTheStandardException() {
		final UnsupportedOperationException expected = Graph.Exceptions.graphComputerNotSupported();
		final UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
				graph::compute);
		assertEquals(expected.getMessage(), thrown.getMessage());
	}

	@Test
	void computeWithAClassFailsWithTheStandardException() {
		final String expected = Graph.Exceptions.graphComputerNotSupported().getMessage();
		assertEquals(expected,
				assertThrows(UnsupportedOperationException.class, () -> graph.compute(OpcGraphComputer.class))
						.getMessage());
		assertEquals(expected,
				assertThrows(UnsupportedOperationException.class, () -> graph.compute(GraphComputer.class)).getMessage());
	}

	@Test
	void withComputerTraversalFailsImmediately() {
		assertThrows(UnsupportedOperationException.class, () -> graph.traversal().withComputer().V().count().next());
	}

	@Test
	void noComputerTestSuiteIsOptedIn() {
		final Graph.OptIn[] optIns = OpcGraph.class.getAnnotationsByType(Graph.OptIn.class);
		assertTrue(optIns.length > 0);
		assertTrue(Arrays.stream(optIns).noneMatch(optIn -> optIn.value().contains("Computer")),
				Arrays.toString(optIns));
	}
}
