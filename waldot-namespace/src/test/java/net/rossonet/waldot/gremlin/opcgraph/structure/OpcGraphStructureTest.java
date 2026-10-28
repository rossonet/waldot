package net.rossonet.waldot.gremlin.opcgraph.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.gremlin.opcgraph.services.OpcDegreeCentralityFactory;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcTextSearchFactory;
import net.rossonet.waldot.testutil.FakeNamespaces;

/**
 * Structure API of {@link OpcGraph} that does not need the OPC UA server.
 */
class OpcGraphStructureTest {

	private OpcGraph graph;

	@AfterEach
	void tearDown() {
		if (graph != null) {
			graph.close();
		}
	}

	private OpcGraph graphWithFakeNamespace(final Map<String, Object> parameters) {
		graph = OpcGraph.open();
		graph.setNamespace(FakeNamespaces.empty(parameters));
		return graph;
	}

	@Test
	void addVertexWithoutNamespaceFails() {
		graph = OpcGraph.open();
		assertThrows(IllegalArgumentException.class, () -> graph.addVertex("name", "x"));
		assertThrows(IllegalArgumentException.class, () -> graph.getGraphComputerView());
		assertNull(graph.getWaldotNamespace());
	}

	@Test
	void nullNamespaceIsRejected() {
		graph = OpcGraph.open();
		assertThrows(IllegalArgumentException.class, () -> graph.setNamespace(null));
	}

	@Test
	void defaultConfiguration() {
		graphWithFakeNamespace(new HashMap<>());
		assertEquals(OpcGraph.class.getName(), graph.configuration().getString(Graph.GRAPH));
		assertEquals(VertexProperty.Cardinality.single, graph.features().vertex().getCardinality("any"));
		assertFalse(graph.features().vertex().supportsNullPropertyValues());
		assertFalse(graph.features().edge().supportsNullPropertyValues());
		assertFalse(graph.features().vertex().properties().supportsNullPropertyValues());
	}

	@Test
	void customConfigurationIsRead() {
		final Configuration conf = new BaseConfiguration();
		conf.setProperty(AbstractOpcGraph.GREMLIN_OPCGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY,
				VertexProperty.Cardinality.list.name());
		conf.setProperty(AbstractOpcGraph.GREMLIN_OPCGRAPH_ALLOW_NULL_PROPERTY_VALUES, true);
		graph = OpcGraph.open(conf);
		assertSame(conf, graph.configuration());
		assertEquals(VertexProperty.Cardinality.list, graph.features().vertex().getCardinality("any"));
		assertTrue(graph.features().vertex().supportsNullPropertyValues());
		assertTrue(graph.features().edge().supportsNullPropertyValues());
		assertTrue(graph.features().vertex().properties().supportsNullPropertyValues());
	}

	@Test
	void graphFeatures() {
		graphWithFakeNamespace(new HashMap<>());
		final Graph.Features.GraphFeatures features = graph.features().graph();
		assertFalse(features.supportsTransactions());
		assertFalse(features.supportsThreadedTransactions());
		assertFalse(features.supportsConcurrentAccess());
		assertFalse(features.supportsServiceCall());
		assertFalse(graph.features().vertex().supportsMultiProperties());
		assertFalse(graph.features().vertex().supportsCustomIds());
		assertFalse(graph.features().edge().supportsCustomIds());
		assertFalse(graph.features().vertex().properties().supportsCustomIds());
		assertNotNull(graph.features().toString());
	}

	@Test
	void transactionsAreNotSupported() {
		graphWithFakeNamespace(new HashMap<>());
		assertThrows(UnsupportedOperationException.class, () -> graph.tx());
	}

	@Test
	void emptyNamespaceHasNoElements() {
		graphWithFakeNamespace(new HashMap<>());
		assertFalse(graph.vertices().hasNext());
		assertFalse(graph.edges().hasNext());
		assertFalse(graph.vertices("ns=1;s=missing").hasNext());
		assertFalse(graph.edges("ns=1;s=missing").hasNext());
		assertEquals(0, graph.getVerticesCount());
		assertEquals(0, graph.getEdgesCount());
		assertEquals(0L, graph.traversal().V().count().next());
		assertTrue(graph.toString().contains("vertices:0 edges:0"), graph.toString());
	}

	@Test
	void generatedIdsAreIncreasing() {
		graphWithFakeNamespace(new HashMap<>());
		final long first = graph.getGeneratedId();
		assertEquals(first + 1, graph.getGeneratedId());
	}

	@Test
	void variablesAreStoredInTheNamespace() {
		final Map<String, Object> parameters = new HashMap<>();
		graphWithFakeNamespace(parameters);
		final Graph.Variables variables = graph.variables();
		variables.set("creator", "marko");
		assertEquals("marko", parameters.get("creator"));
		assertEquals(Optional.of("marko"), variables.get("creator"));
		assertTrue(variables.keys().contains("creator"));
		assertTrue(variables.toString().contains("variables"), variables.toString());
		variables.remove("creator");
		assertTrue(variables.get("creator").isEmpty());
		assertThrows(IllegalArgumentException.class, () -> variables.set(null, "x"));
		assertThrows(IllegalArgumentException.class, () -> variables.set("k", null));
	}

	@Test
	void servicesAreRegisteredFromTheConfiguration() {
		final Configuration conf = new BaseConfiguration();
		conf.setProperty(AbstractOpcGraph.GREMLIN_OPCGRAPH_SERVICE,
				java.util.List.of(OpcTextSearchFactory.class.getName(), OpcDegreeCentralityFactory.class.getName()));
		graph = OpcGraph.open(conf);
		assertNotNull(graph.getServiceRegistry().get(OpcTextSearchFactory.NAME, true, java.util.Map.of()));
		assertNotNull(graph.getServiceRegistry().get(OpcDegreeCentralityFactory.NAME, false, java.util.Map.of()));
	}

	@Test
	void unknownServiceClassFails() {
		final Configuration conf = new BaseConfiguration();
		conf.setProperty(AbstractOpcGraph.GREMLIN_OPCGRAPH_SERVICE, "net.rossonet.DoesNotExist");
		assertThrows(RuntimeException.class, () -> OpcGraph.open(conf));
	}
}
