package net.rossonet.waldot.gremlin.opcgraph.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;

import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONIo;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoIo;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoVersion;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.apache.tinkerpop.shaded.kryo.Kryo;
import org.apache.tinkerpop.shaded.kryo.io.Input;
import org.apache.tinkerpop.shaded.kryo.io.Output;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.rossonet.waldot.testutil.FakeNamespaces;

/**
 * GraphSON and Gryo registries of {@link OpcGraph}: a graph can be shipped
 * whole (as Gremlin Server does with subgraphs) and read back.
 */
class OpcIoRegistryTest {

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

	private ObjectMapper graphsonMapper(final GraphSONVersion version) {
		final GraphSONMapper.Builder builder = GraphSONMapper.build().version(version);
		switch (version) {
		case V1_0 -> builder.addRegistry(OpcIoRegistryV1.instance());
		case V2_0 -> builder.addRegistry(OpcIoRegistryV2.instance());
		default -> builder.addRegistry(OpcIoRegistryV3.instance());
		}
		return builder.create().createMapper();
	}

	@ParameterizedTest
	@EnumSource(value = GraphSONVersion.class, names = { "V1_0", "V2_0", "V3_0" })
	void graphRoundTripsThroughGraphSON(final GraphSONVersion version) throws Exception {
		final ObjectMapper mapper = graphsonMapper(version);
		final String json = mapper.writeValueAsString(graph);
		assertTrue(json.contains("vertices") && json.contains("edges"), json);
		final OpcGraph read = mapper.readValue(json, OpcGraph.class);
		try {
			assertNotNull(read);
			assertEquals(OpcGraph.class.getName(), read.configuration().getString("gremlin.graph", OpcGraph.class.getName()));
		} finally {
			read.close();
		}
	}

	@ParameterizedTest
	@EnumSource(value = GraphSONVersion.class, names = { "V2_0", "V3_0" })
	void nodeIdIsWrittenAsTypedGraphSON(final GraphSONVersion version) throws Exception {
		final ObjectMapper mapper = graphsonMapper(version);
		final NodeId nodeId = new NodeId(2, "waldot/vertex");
		final String json = mapper.writeValueAsString(nodeId);
		assertTrue(json.contains(nodeId.toParseableString()), json);
		assertTrue(json.contains("nodeId"), json);
	}


	@ParameterizedTest
	@EnumSource(value = GraphSONVersion.class, names = { "V1_0", "V2_0", "V3_0" })
	void ioBuilderSelectsTheRegistryOfTheVersion(final GraphSONVersion version) throws Exception {
		final GraphSONIo io = graph.io(GraphSONIo.build(version));
		assertNotNull(io);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		io.writer().create().writeGraph(out, graph);
		assertEquals("", out.toString().trim(), "an empty graph has no adjacency lines");
	}


	@ParameterizedTest
	@EnumSource(value = GryoVersion.class, names = { "V1_0", "V3_0" })
	void graphRoundTripsThroughGryo(final GryoVersion version) {
		final Kryo kryo = GryoMapper.build().version(version)
				.addRegistry(version == GryoVersion.V1_0 ? OpcIoRegistryV1.instance() : OpcIoRegistryV3.instance())
				.create().createMapper();
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (Output output = new Output(bytes)) {
			kryo.writeObject(output, graph);
		}
		try (Input input = new Input(bytes.toByteArray())) {
			final OpcGraph read = kryo.readObject(input, OpcGraph.class);
			try {
				assertNotNull(read);
				assertFalse(read.isClosed());
			} finally {
				read.close();
			}
		}
	}

	@ParameterizedTest
	@EnumSource(value = GryoVersion.class, names = { "V1_0", "V3_0" })
	void gryoIoWritesAndReadsAnEmptyGraph(final GryoVersion version) throws Exception {
		final GryoIo io = graph.io(GryoIo.build(version));
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		io.writer().create().writeGraph(out, graph);
		final OpcGraph target = OpcGraph.open();
		try {
			io.reader().create().readGraph(new java.io.ByteArrayInputStream(out.toByteArray()), target);
			assertFalse(target.isClosed());
		} finally {
			target.close();
		}
	}
}
