package net.rossonet.waldot.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Regression (step 1.6): {@code OpcFactory.getOpcGraph(Configuration)} ignored
 * the configuration and {@code createTheCrew()} set the cardinality after the
 * creation of the graph, so the "list" cardinality was never applied.
 */
class OpcFactoryConfigurationRegressionTest {

	private WaldotGraph graph;

	private void cleanup() throws Exception {
		if (graph != null) {
			graph.getWaldotNamespace().close();
			graph = null;
		}
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
	}

	@Test
	void defaultCardinalityIsSingle() throws Exception {
		graph = OpcFactory.getOpcGraph(new BaseConfiguration());
		assertEquals(VertexProperty.Cardinality.single, graph.features().vertex().getCardinality("location"));
	}

	@Test
	void gremlinConfigurationIsApplied() throws Exception {
		final Configuration conf = new BaseConfiguration();
		conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY,
				VertexProperty.Cardinality.list.name());
		graph = OpcFactory.getOpcGraph(conf);
		assertEquals(VertexProperty.Cardinality.list, graph.features().vertex().getCardinality("location"));
	}

	@Test
	void theCrewUsesListCardinality() throws Exception {
		graph = OpcFactory.createTheCrew();
		assertEquals(VertexProperty.Cardinality.list, graph.features().vertex().getCardinality("location"));
	}

	@BeforeEach
	void setUp() throws Exception {
		cleanup();
	}

	@AfterEach
	void tearDown() throws Exception {
		cleanup();
	}
}
