/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.rossonet.waldot.gremlin.opcgraph.structure;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.GraphHelper;
import org.apache.tinkerpop.gremlin.TestHelper;
import org.apache.tinkerpop.gremlin.process.computer.Computer;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.IdentityRemovalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.verification.ReservedKeysVerificationStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.util.Metrics;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalMetrics;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.io.GraphReader;
import org.apache.tinkerpop.gremlin.structure.io.GraphWriter;
import org.apache.tinkerpop.gremlin.structure.io.Io;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.structure.io.IoTest;
import org.apache.tinkerpop.gremlin.structure.io.Mapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONReader;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter;
import org.apache.tinkerpop.gremlin.structure.io.graphson.TypeInfo;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoClassResolverV1;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoVersion;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoWriter;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.apache.tinkerpop.shaded.kryo.ClassResolver;
import org.apache.tinkerpop.shaded.kryo.Kryo;
import org.apache.tinkerpop.shaded.kryo.Registration;
import org.apache.tinkerpop.shaded.kryo.Serializer;
import org.apache.tinkerpop.shaded.kryo.io.Input;
import org.apache.tinkerpop.shaded.kryo.io.Output;
import org.junit.jupiter.api.Test;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class OpcGraphTest {

	/**
	 * Coerces a {@code Color} to a {@link OpcGraph} during serialization.
	 * Demonstrates how custom serializers can be developed that can coerce one
	 * value to another during serialization.
	 */
	public final static class ColorToTinkerGraphSerializer extends Serializer<Color> {
		private static void generate(final Graph graph) {
			final int size = 100;
			final List<Object> ids = new ArrayList<>();
			final Vertex v = graph.addVertex("sin", 0.0f, "cos", 1.0f, "ii", 0f);
			ids.add(v.id());

			final GraphTraversalSource g = graph.traversal();

			final Random rand = new Random();
			for (int ii = 1; ii < size; ii++) {
				final Vertex t = graph.addVertex("ii", ii, "sin", Math.sin(ii / 5.0f), "cos", Math.cos(ii / 5.0f));
				final Vertex u = g.V(ids.get(rand.nextInt(ids.size()))).next();
				t.addEdge("linked", u);
				ids.add(u.id());
				ids.add(v.id());
			}
		}

		public ColorToTinkerGraphSerializer() {
		}

		@Override
		public Color read(final Kryo kryo, final Input input, final Class<Color> colorClass) {
			throw new UnsupportedOperationException("IoX writes to DetachedVertex and can't be read back in as IoX");
		}

		@Override
		public void write(final Kryo kryo, final Output output, final Color color) {
			final OpcGraph graph = OpcGraph.open();
			final Vertex v = graph.addVertex(T.id, 1, T.label, "color", "name", color.toString());
			final Vertex vRed = graph.addVertex(T.id, 2, T.label, "primary", "name", "red");
			final Vertex vGreen = graph.addVertex(T.id, 3, T.label, "primary", "name", "green");
			final Vertex vBlue = graph.addVertex(T.id, 4, T.label, "primary", "name", "blue");

			v.addEdge("hasComponent", vRed, "amount", color.getRed());
			v.addEdge("hasComponent", vGreen, "amount", color.getGreen());
			v.addEdge("hasComponent", vBlue, "amount", color.getBlue());

			// make some junk so the graph is kinda big
			generate(graph);

			try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
				GryoWriter.build().mapper(() -> kryo).create().writeGraph(stream, graph);
				final byte[] bytes = stream.toByteArray();
				output.writeInt(bytes.length);
				output.write(bytes);
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public static class CustomClassResolver extends GryoClassResolverV1 {
		private final ColorToTinkerGraphSerializer colorToGraphSerializer = new ColorToTinkerGraphSerializer();

		@Override
		public Registration getRegistration(final Class clazz) {
			if (Color.class.isAssignableFrom(clazz)) {
				final Registration registration = super.getRegistration(OpcGraph.class);
				return new Registration(registration.getType(), colorToGraphSerializer, registration.getId());
			} else {
				return super.getRegistration(clazz);
			}
		}
	}

	public static class CustomClassResolverSupplier implements Supplier<ClassResolver> {
		@Override
		public ClassResolver get() {
			return new CustomClassResolver();
		}
	}

	public static class TestIoBuilder implements Io.Builder {

		static int calledGraph, calledCreate, calledOnMapper;

		public TestIoBuilder() {
			// Looks awkward to reset static vars inside a constructor, but makes sense from
			// testing perspective
			calledGraph = 0;
			calledCreate = 0;
			calledOnMapper = 0;
		}

		@Override
		public Io create() {
			calledCreate++;
			return mock(Io.class);
		}

		@Override
		public Io.Builder<? extends Io> graph(final Graph graph) {
			calledGraph++;
			return this;
		}

		@Override
		public Io.Builder<? extends Io> onMapper(final Consumer onMapper) {
			calledOnMapper++;
			return this;
		}

		@Override
		public boolean requiresVersion(final Object version) {
			return false;
		}
	}

	@Test
	public void shouldAllowHeterogeneousIdsWithAnyManager() {
		final Configuration anyManagerConfig = new BaseConfiguration();
		/*
		 * anyManagerConfig.addProperty(OpcGraph.GREMLIN_OPCGRAPH_EDGE_ID_MANAGER,
		 * OpcGraph.NodeIdManager.ANY.name());
		 * anyManagerConfig.addProperty(OpcGraph.GREMLIN_OPCGRAPH_VERTEX_ID_MANAGER,
		 * OpcGraph.NodeIdManager.ANY.name()); anyManagerConfig.addProperty(OpcGraph.
		 * GREMLIN_OPCGRAPH_VERTEX_PROPERTY_ID_MANAGER,
		 * OpcGraph.NodeIdManager.ANY.name());
		 */
		final Graph graph = OpcGraph.open(anyManagerConfig);
		final GraphTraversalSource g = traversal().withEmbedded(graph);

		final UUID uuid = UUID.fromString("0E939658-ADD2-4598-A722-2FC178E9B741");
		g.addV("person").property(T.id, 100).addV("person").property(T.id, "1000").addV("person").property(T.id, "1001")
				.addV("person").property(T.id, uuid).iterate();

		assertEquals(3, g.V(100, "1000", uuid).count().next().intValue());
	}

	@Test
	public void shouldApplyStrategiesRecursivelyWithGraph() {
		final Graph graph = OpcGraph.open();
		final GraphTraversalSource g = traversal().withEmbedded(graph)
				.withStrategies(new TraversalStrategy.ProviderOptimizationStrategy() {
					@Override
					public void apply(final Traversal.Admin<?, ?> traversal) {
						final Graph graph = traversal.getGraph().get();
						graph.addVertex("person");
					}
				});

		// adds one person by way of the strategy
		g.inject(0).iterate();
		assertEquals(1, traversal().withEmbedded(graph).V().hasLabel("person").count().next().intValue());

		// adds two persons by way of the strategy one for the parent and one for the
		// child
		g.inject(0).sideEffect(__.addV()).iterate();
		assertEquals(3, traversal().withEmbedded(graph).V().hasLabel("person").count().next().intValue());
	}

	/**
	 * Validating that mid-traversal hasId() also unwraps ids in lists in addition
	 * to ids in arrays as per TINKERPOP-2863
	 */
	@Test
	public void shouldCheckWithinListsOfIdsForMidTraversalHasId() {
		final GraphTraversalSource g = OpcFactory.createModern().traversal();

		final List<Vertex> expectedMidTraversal = g.V().has("name", "marko").outE("knows").inV().hasId(2, 4).toList();

		assertEquals(expectedMidTraversal,
				g.V().has("name", "marko").outE("knows").inV().hasId(new Integer[] { 2, 4 }).toList());
		assertEquals(expectedMidTraversal,
				g.V().has("name", "marko").outE("knows").inV().hasId(Arrays.asList(2, 4)).toList());
	}

	/**
	 * Validating that start-step hasId() unwraps ids in lists in addition to ids in
	 * arrays as per TINKERPOP-2863
	 */
	@Test
	public void shouldCheckWithinListsOfIdsForStartStepHasId() {
		final GraphTraversalSource g = OpcFactory.createModern().traversal();

		final List<Vertex> expectedStartTraversal = g.V().hasId(1, 2).toList();

		assertEquals(expectedStartTraversal, g.V().hasId(new Integer[] { 1, 2 }).toList());
		assertEquals(expectedStartTraversal, g.V().hasId(Arrays.asList(1, 2)).toList());
	}

	@Test
	public void shouldCloneTinkergraph() {
		final OpcGraph original = OpcGraph.open();
		final OpcGraph clone = OpcGraph.open();

		final Vertex marko = original.addVertex("name", "marko", "age", 29);
		final Vertex stephen = original.addVertex("name", "stephen", "age", 35);
		marko.addEdge("knows", stephen);
		GraphHelper.cloneElements(original, clone);

		final Vertex michael = clone.addVertex("name", "michael");
		michael.addEdge("likes", marko);
		michael.addEdge("likes", stephen);
		clone.traversal().V().property("newProperty", "someValue").toList();
		clone.traversal().E().property("newProperty", "someValue").toList();

		assertEquals("original graph should be unchanged", new Long(2), original.traversal().V().count().next());
		assertEquals("original graph should be unchanged", new Long(1), original.traversal().E().count().next());
		assertEquals("original graph should be unchanged", new Long(0),
				original.traversal().V().has("newProperty").count().next());

		assertEquals("cloned graph should contain new elements", new Long(3), clone.traversal().V().count().next());
		assertEquals("cloned graph should contain new elements", new Long(3), clone.traversal().E().count().next());
		assertEquals("cloned graph should contain new property", new Long(3),
				clone.traversal().V().has("newProperty").count().next());
		assertEquals("cloned graph should contain new property", new Long(3),
				clone.traversal().E().has("newProperty").count().next());

		assertNotSame("cloned elements should reference to different objects",
				original.traversal().V().has("name", "stephen").next(),
				clone.traversal().V().has("name", "stephen").next());
	}

	/**
	 * Just validating that property folding works nicely given TINKERPOP-2112
	 */
	@Test
	public void shouldFoldPropertyStepForTokens() {
		final GraphTraversalSource g = OpcGraph.open().traversal();

		g.addV("person").property(VertexProperty.Cardinality.single, "k", "v").property(T.id, "id")
				.property(VertexProperty.Cardinality.list, "l", 1).property("x", "y")
				.property(VertexProperty.Cardinality.list, "l", 2).property("m", "m", "mm", "mm").property("y", "z")
				.iterate();

		assertThat(g.V("id").hasNext(), is(true));
	}

	@Test // (expected = IllegalStateException.class)
	public void shouldNotAddEdgeToAVertexThatWasRemoved() {
		final OpcGraph graph = OpcGraph.open();
		final Vertex v = graph.addVertex();
		v.property("name", "stephen");

		assertEquals("stephen", v.value("name"));
		v.remove();
		v.addEdge("self", v);
	}

	@Test // (expected = IllegalStateException.class)
	public void shouldNotModifyAVertexThatWasRemoved() {
		final OpcGraph graph = OpcGraph.open();
		final Vertex v = graph.addVertex();
		v.property("name", "stephen");

		assertEquals("stephen", v.value("name"));
		v.remove();

		v.property("status", 1);
	}

	@Test // (expected = IllegalStateException.class)
	public void shouldNotReadValueOfPropertyOnVertexThatWasRemoved() {
		final OpcGraph graph = OpcGraph.open();
		final Vertex v = graph.addVertex();
		v.property("name", "stephen");

		assertEquals("stephen", v.value("name"));
		v.remove();
		v.value("name");
	}

	@Test
	public void shouldOptionalUsingWithComputer() {
		// not all systems will have 3+ available processors (e.g. travis)
		assumeThat(Runtime.getRuntime().availableProcessors(), greaterThan(2));

		// didn't add this as a general test as it basically was only failing under a
		// specific condition for
		// OpcGraphComputer - see more here:
		// https://issues.apache.org/jira/browse/TINKERPOP-1619
		final GraphTraversalSource g = OpcFactory.createModern().traversal();

		final List<Edge> expected = g.E(7, 7, 8, 9).order().by(T.id).toList();
		assertEquals(expected, g.withComputer(Computer.compute().workers(3)).V(1, 2).optional(__.bothE().dedup())
				.order().by(T.id).toList());
		assertEquals(expected, g.withComputer(Computer.compute().workers(4)).V(1, 2).optional(__.bothE().dedup())
				.order().by(T.id).toList());
	}

	@Test
	public void shouldPersistToAnyGraphFormat() {
		final String graphLocation = TestHelper.makeTestDataFile(OpcGraphTest.class,
				"shouldPersistToAnyGraphFormat.dat");
		final File f = new File(graphLocation);
		if (f.exists() && f.isFile()) {
			f.delete();
		}

		final Configuration conf = new BaseConfiguration();
		/*
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_FORMAT,
		 * TestIoBuilder.class.getName());
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_LOCATION, graphLocation);
		 */
		final OpcGraph graph = OpcGraph.open(conf);
		OpcFactory.generateModern(graph);

		// Test write graph
		graph.close();
		assertEquals(TestIoBuilder.calledOnMapper, 1);
		assertEquals(TestIoBuilder.calledGraph, 1);
		assertEquals(TestIoBuilder.calledCreate, 1);

		try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(f))) {
			os.write("dummy string".getBytes());
		} catch (final Exception e) {
			e.printStackTrace();
		}

		// Test read graph
		final OpcGraph readGraph = OpcGraph.open(conf);
		assertEquals(TestIoBuilder.calledOnMapper, 1);
		assertEquals(TestIoBuilder.calledGraph, 1);
		assertEquals(TestIoBuilder.calledCreate, 1);
	}

	@Test
	public void shouldPersistToGraphML() {
		final String graphLocation = TestHelper.makeTestDataFile(OpcGraphTest.class, "shouldPersistToGraphML.xml");
		final File f = new File(graphLocation);
		if (f.exists() && f.isFile()) {
			f.delete();
		}

		final Configuration conf = new BaseConfiguration();
		/*
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_FORMAT, "graphml");
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_LOCATION, graphLocation);
		 */
		final OpcGraph graph = OpcGraph.open(conf);
		OpcFactory.generateModern(graph);
		graph.close();

		final OpcGraph reloadedGraph = OpcGraph.open(conf);
		IoTest.assertModernGraph(reloadedGraph, true, true);
		reloadedGraph.close();
	}

	@Test
	public void shouldPersistToGraphSON() {
		final String graphLocation = TestHelper.makeTestDataFile(OpcGraphTest.class, "shouldPersistToGraphSON.json");
		final File f = new File(graphLocation);
		if (f.exists() && f.isFile()) {
			f.delete();
		}

		final Configuration conf = new BaseConfiguration();
		/*
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_FORMAT, "graphson");
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_LOCATION, graphLocation);
		 */
		final OpcGraph graph = OpcGraph.open(conf);
		OpcFactory.generateModern(graph);
		graph.close();

		final OpcGraph reloadedGraph = OpcGraph.open(conf);
		IoTest.assertModernGraph(reloadedGraph, true, false);
		reloadedGraph.close();
	}

	@Test
	public void shouldPersistToGryo() {
		final String graphLocation = TestHelper.makeTestDataFile(OpcGraphTest.class, "shouldPersistToGryo.kryo");
		final File f = new File(graphLocation);
		if (f.exists() && f.isFile()) {
			f.delete();
		}

		final Configuration conf = new BaseConfiguration();
		/*
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_FORMAT, "gryo");
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_LOCATION, graphLocation);
		 */
		final OpcGraph graph = OpcGraph.open(conf);
		OpcFactory.generateModern(graph);
		graph.close();

		final OpcGraph reloadedGraph = OpcGraph.open(conf);
		IoTest.assertModernGraph(reloadedGraph, true, false);
		reloadedGraph.close();
	}

	@Test
	public void shouldPersistToGryoAndHandleMultiProperties() {
		final String graphLocation = TestHelper.makeTestDataFile(OpcGraphTest.class, "shouldPersistToGryoMulti.kryo");
		final File f = new File(graphLocation);
		if (f.exists() && f.isFile()) {
			f.delete();
		}

		final Configuration conf = new BaseConfiguration();
		/*
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_FORMAT, "gryo");
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_LOCATION, graphLocation);
		 */
		final OpcGraph graph = OpcGraph.open(conf);
		OpcFactory.generateTheCrew(graph);
		graph.close();

		conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY,
				VertexProperty.Cardinality.list.toString());
		final OpcGraph reloadedGraph = OpcGraph.open(conf);
		IoTest.assertCrewGraph(reloadedGraph, false);
		reloadedGraph.close();
	}

	@Test
	public void shouldPersistWithRelativePath() {
		final String graphLocation = TestHelper.convertToRelative(OpcGraphTest.class,
				TestHelper.makeTestDataPath(OpcGraphTest.class)) + "shouldPersistToGryoRelative.kryo";
		final File f = new File(graphLocation);
		if (f.exists() && f.isFile()) {
			f.delete();
		}

		final Configuration conf = new BaseConfiguration();
		/*
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_FORMAT, "gryo");
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_LOCATION, graphLocation);
		 */
		final OpcGraph graph = OpcGraph.open(conf);
		OpcFactory.generateModern(graph);
		graph.close();

		final OpcGraph reloadedGraph = OpcGraph.open(conf);
		IoTest.assertModernGraph(reloadedGraph, true, false);
		reloadedGraph.close();
	}

	/**
	 * This isn't a OpcGraph specific test, but OpcGraph is probably best suited for
	 * the testing of this particular problem originally noted in TINKERPOP-1992.
	 */
	@Test
	public void shouldProperlyTimeReducingBarrierForProfile() {
		final GraphTraversalSource g = OpcFactory.createModern().traversal();

		TraversalMetrics m = g.V().group().by().by(__.bothE().count()).profile().next();
		for (final Metrics i : m.getMetrics(1).getNested()) {
			assertThat(i.getDuration(TimeUnit.NANOSECONDS), greaterThan(0L));
		}

		m = g.withComputer().V().group().by().by(__.bothE().count()).profile().next();
		for (final Metrics i : m.getMetrics(1).getNested()) {
			assertThat(i.getDuration(TimeUnit.NANOSECONDS), greaterThan(0L));
		}
	}

	@Test
	public void shouldProvideClearErrorWhenFromOrToDoesNotResolveToVertex() {
		final GraphTraversalSource g = OpcFactory.createModern().traversal();

		try {
			g.addE("link").property(VertexProperty.Cardinality.single, "k", 100).to(__.V(1)).iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals("The value given to addE(link).from() must resolve to a Vertex but null was specified instead",
					ise.getMessage());
		}

		try {
			g.addE("link").property(VertexProperty.Cardinality.single, "k", 100).from(__.V(1)).iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals("The value given to addE(link).to() must resolve to a Vertex but null was specified instead",
					ise.getMessage());
		}

		try {
			g.addE("link").property("k", 100).from(__.V(1)).iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals("The value given to addE(link).to() must resolve to a Vertex but null was specified instead",
					ise.getMessage());
		}

		try {
			g.V(1).values("name").as("a").addE("link").property(VertexProperty.Cardinality.single, "k", 100).from("a")
					.iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals("The value given to addE(link).to() must resolve to a Vertex but String was specified instead",
					ise.getMessage());
		}

		try {
			g.V(1).values("name").as("a").addE("link").property(VertexProperty.Cardinality.single, "k", 100).to("a")
					.iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals("The value given to addE(link).to() must resolve to a Vertex but String was specified instead",
					ise.getMessage());
		}

		try {
			g.V(1).as("v").values("name").as("a").addE("link").property(VertexProperty.Cardinality.single, "k", 100)
					.to("v").from("a").iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals("The value given to addE(link).to() must resolve to a Vertex but String was specified instead",
					ise.getMessage());
		}
	}

	@Test
	public void shouldProvideClearErrorWhenPuttingFromToInWrongSpot() {
		final GraphTraversalSource g = OpcFactory.createModern().traversal();

		try {
			g.addE("link").property(VertexProperty.Cardinality.single, "k", 100).out().to(__.V(1)).from(__.V(1))
					.iterate();
			fail("Should have thrown an error");
		} catch (final IllegalArgumentException ise) {
			assertEquals("The to() step cannot follow VertexStep", ise.getMessage());
		}

		try {
			g.addE("link").property("k", 100).out().from(__.V(1)).to(__.V(1)).iterate();
			fail("Should have thrown an error");
		} catch (final IllegalArgumentException ise) {
			assertEquals("The from() step cannot follow VertexStep", ise.getMessage());
		}
	}

	@Test
	public void shouldProvideClearErrorWhenTryingToMutateEdgeWithCardinality() {
		final GraphTraversalSource g = OpcFactory.createModern().traversal();

		try {
			g.E().property(VertexProperty.Cardinality.single, "k", 100).iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals(
					"Property cardinality can only be set for a Vertex but the traversal encountered OpcEdge for key: k",
					ise.getMessage());
		}

		try {
			g.E().property(VertexProperty.Cardinality.list, "k", 100).iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals(
					"Property cardinality can only be set for a Vertex but the traversal encountered OpcEdge for key: k",
					ise.getMessage());
		}

		try {
			g.addE("link").to(__.V(1)).from(__.V(1)).property(VertexProperty.Cardinality.list, "k", 100).iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals(
					"Multi-property cardinality of [list] can only be set for a Vertex but is being used for addE() with key: k",
					ise.getMessage());
		}
	}

	@Test
	public void shouldProvideClearErrorWhenTryingToMutateT() {
		final GraphTraversalSource g = OpcGraph.open().traversal();
		g.addV("person").property(T.id, 100).iterate();

		try {
			g.V(100).property(T.label, "software").iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals("T.label is immutable on existing elements", ise.getMessage());
		}

		try {
			g.V(100).property(T.id, 101).iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals("T.id is immutable on existing elements", ise.getMessage());
		}

		try {
			g.V(100).property("name", "marko").property(T.label, "software").iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals("T.label is immutable on existing elements", ise.getMessage());
		}

		try {
			g.V(100).property(T.id, 101).property("name", "marko").iterate();
			fail("Should have thrown an error");
		} catch (final IllegalStateException ise) {
			assertEquals("T.id is immutable on existing elements", ise.getMessage());
		}
	}

	@Test
	public void shouldRemoveAVertexFromAnIndex() {
		final OpcGraph g = OpcGraph.open();
		// g.createIndex("name", Vertex.class);

		g.addVertex("name", "marko", "age", 29);
		g.addVertex("name", "stephen", "age", 35);
		final Vertex v = g.addVertex("name", "stephen", "age", 35);

		// a tricky way to evaluate if indices are actually being used is to pass a fake
		// BiPredicate to has()
		// to get into the Pipeline and evaluate what's going through it. in this case,
		// we know that at index
		// is used because only "stephen" ages should pass through the pipeline due to
		// the inclusion of the
		// key index lookup on "name". If there's an age of something other than 35 in
		// the pipeline being evaluated
		// then something is wrong.
		assertEquals(new Long(2), g.traversal().V().has("age", P.test((t, u) -> {
			assertEquals(35, t);
			return true;
		}, 35)).has("name", "stephen").count().next());

		v.remove();
		assertEquals(new Long(1), g.traversal().V().has("age", P.test((t, u) -> {
			assertEquals(35, t);
			return true;
		}, 35)).has("name", "stephen").count().next());
	}

	@Test
	public void shouldRemoveEdgeFromAnIndex() {
		final OpcGraph g = OpcGraph.open();
		// g.createIndex("oid", Edge.class);

		final Vertex v = g.addVertex();
		v.addEdge("friend", v, "oid", "1", "weight", 0.5f);
		final Edge e = v.addEdge("friend", v, "oid", "1", "weight", 0.5f);
		v.addEdge("friend", v, "oid", "2", "weight", 0.6f);

		// a tricky way to evaluate if indices are actually being used is to pass a fake
		// BiPredicate to has()
		// to get into the Pipeline and evaluate what's going through it. in this case,
		// we know that at index
		// is used because only oid 1 should pass through the pipeline due to the
		// inclusion of the
		// key index lookup on "oid". If there's an weight of something other than 0.5f
		// in the pipeline being
		// evaluated then something is wrong.
		assertEquals(new Long(2), g.traversal().E().has("weight", P.test((t, u) -> {
			assertEquals(0.5f, t);
			return true;
		}, 0.5)).has("oid", "1").count().next());

		e.remove();
		assertEquals(new Long(1), g.traversal().E().has("weight", P.test((t, u) -> {
			assertEquals(0.5f, t);
			return true;
		}, 0.5)).has("oid", "1").count().next());
	}

	@Test // (expected = IllegalStateException.class)
	public void shouldRequireGraphFormatIfLocationIsSet() {
		final Configuration conf = new BaseConfiguration();
		/*
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_LOCATION,
		 * TestHelper.makeTestDataDirectory(OpcGraphTest.class));
		 * 
		 */
		OpcGraph.open(conf);
	}

	@Test // (expected = IllegalStateException.class)
	public void shouldRequireGraphLocationIfFormatIsSet() {
		final Configuration conf = new BaseConfiguration();
		// conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_FORMAT, "graphml");
		OpcGraph.open(conf);
	}

	@Test
	public void shouldReservedKeyVerify() {
		final Set<String> reserved = new HashSet<>(Arrays.asList("something", "id", "label"));
		final GraphTraversalSource g = OpcGraph.open().traversal().withStrategies(
				ReservedKeysVerificationStrategy.build().reservedKeys(reserved).throwException().create());

		g.addV("person").property(T.id, 123).iterate();

		try {
			g.addV("person").property("id", 123).iterate();
			fail("Verification exception expected");
		} catch (final IllegalStateException ve) {
			assertThat(ve.getMessage(), containsString("that is setting a property key to a reserved word"));
		}

		try {
			g.addV("person").property("something", 123).iterate();
			fail("Verification exception expected");
		} catch (final IllegalStateException ve) {
			assertThat(ve.getMessage(), containsString("that is setting a property key to a reserved word"));
		}
	}

	@Test
	public void shouldSerializeTinkerGraphToGraphSON() throws Exception {
		final OpcGraph graph = OpcFactory.createModern();
		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			graph.io(IoCore.graphson()).writer().create().writeObject(out, graph);
			try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray())) {
				final OpcGraph target = graph.io(IoCore.graphson()).reader().create().readObject(inputStream,
						OpcGraph.class);
				IoTest.assertModernGraph(target, true, false);
			}
		}
	}

	@Test
	public void shouldSerializeTinkerGraphToGraphSONWithTypes() throws Exception {
		final OpcGraph graph = OpcFactory.createModern();
		final Mapper<ObjectMapper> mapper = graph.io(IoCore.graphson()).mapper().typeInfo(TypeInfo.PARTIAL_TYPES)
				.create();
		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			final GraphWriter writer = GraphSONWriter.build().mapper(mapper).create();
			writer.writeObject(out, graph);
			try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray())) {
				final GraphReader reader = GraphSONReader.build().mapper(mapper).create();
				final OpcGraph target = reader.readObject(inputStream, OpcGraph.class);
				IoTest.assertModernGraph(target, true, false);
			}
		}
	}

	@Test
	public void shouldSerializeTinkerGraphToGryo() throws Exception {
		final OpcGraph graph = OpcFactory.createModern();
		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			graph.io(IoCore.gryo()).writer().create().writeObject(out, graph);
			final byte[] b = out.toByteArray();
			try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(b)) {
				final OpcGraph target = graph.io(IoCore.gryo()).reader().create().readObject(inputStream,
						OpcGraph.class);
				IoTest.assertModernGraph(target, true, false);
			}
		}
	}

	@Test
	public void shouldSerializeTinkerGraphWithMultiPropertiesToGraphSON() throws Exception {
		final OpcGraph graph = OpcFactory.createTheCrew();
		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			graph.io(IoCore.graphson()).writer().create().writeObject(out, graph);
			try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray())) {
				final OpcGraph target = graph.io(IoCore.graphson()).reader().create().readObject(inputStream,
						OpcGraph.class);
				IoTest.assertCrewGraph(target, false);
			}
		}
	}

	@Test
	public void shouldSerializeTinkerGraphWithMultiPropertiesToGryo() throws Exception {
		final OpcGraph graph = OpcFactory.createTheCrew();
		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			graph.io(IoCore.gryo()).writer().create().writeObject(out, graph);
			final byte[] b = out.toByteArray();
			try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(b)) {
				final OpcGraph target = graph.io(IoCore.gryo()).reader().create().readObject(inputStream,
						OpcGraph.class);
				IoTest.assertCrewGraph(target, false);
			}
		}
	}

	@Test
	public void shouldSerializeWithColorClassResolverToTinkerGraph() throws Exception {
		final Map<String, Color> colors = new HashMap<>();
		colors.put("red", Color.RED);
		colors.put("green", Color.GREEN);

		final ArrayList<Color> colorList = new ArrayList<>(Arrays.asList(Color.RED, Color.GREEN));

		final Supplier<ClassResolver> classResolver = new CustomClassResolverSupplier();
		final GryoMapper mapper = GryoMapper.build().version(GryoVersion.V3_0).addRegistry(OpcIoRegistryV3.instance())
				.classResolver(classResolver).create();
		final Kryo kryo = mapper.createMapper();
		try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			final Output out = new Output(stream);

			kryo.writeObject(out, colorList);
			out.flush();
			final byte[] b = stream.toByteArray();

			try (final InputStream inputStream = new ByteArrayInputStream(b)) {
				final Input input = new Input(inputStream);
				final List m = kryo.readObject(input, ArrayList.class);
				final OpcGraph readX = (OpcGraph) m.get(0);
				assertEquals(104, IteratorUtils.count(readX.vertices()));
				assertEquals(102, IteratorUtils.count(readX.edges()));
			}
		}
	}

	@Test
	public void shouldSerializeWithColorClassResolverToTinkerGraphUsingDeprecatedTinkerIoRegistry() throws Exception {
		final Map<String, Color> colors = new HashMap<>();
		colors.put("red", Color.RED);
		colors.put("green", Color.GREEN);

		final ArrayList<Color> colorList = new ArrayList<>(Arrays.asList(Color.RED, Color.GREEN));

		final Supplier<ClassResolver> classResolver = new CustomClassResolverSupplier();
		final GryoMapper mapper = GryoMapper.build().version(GryoVersion.V3_0).addRegistry(OpcIoRegistryV3.instance())
				.classResolver(classResolver).create();
		final Kryo kryo = mapper.createMapper();
		try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			final Output out = new Output(stream);

			kryo.writeObject(out, colorList);
			out.flush();
			final byte[] b = stream.toByteArray();

			try (final InputStream inputStream = new ByteArrayInputStream(b)) {
				final Input input = new Input(inputStream);
				final List m = kryo.readObject(input, ArrayList.class);
				final OpcGraph readX = (OpcGraph) m.get(0);
				assertEquals(104, IteratorUtils.count(readX.vertices()));
				assertEquals(102, IteratorUtils.count(readX.edges()));
			}
		}
	}

	@Test
	public void shouldUpdateEdgeIndicesInExistingGraph() {
		final OpcGraph g = OpcGraph.open();

		final Vertex v = g.addVertex();
		v.addEdge("friend", v, "oid", "1", "weight", 0.5f);
		v.addEdge("friend", v, "oid", "2", "weight", 0.6f);

		// a tricky way to evaluate if indices are actually being used is to pass a fake
		// BiPredicate to has()
		// to get into the Pipeline and evaluate what's going through it. in this case,
		// we know that at index
		// is not used because "1" and "2" weights both pass through the pipeline.
		assertEquals(new Long(1), g.traversal().E().has("weight", P.test((t, u) -> {
			assertTrue(t.equals(0.5f) || t.equals(0.6f));
			return true;
		}, 0.5)).has("oid", "1").count().next());

		// g.createIndex("oid", Edge.class);

		// another spy into the pipeline for index check. in this case, we know that at
		// index
		// is used because only oid 1 should pass through the pipeline due to the
		// inclusion of the
		// key index lookup on "oid". If there's an weight of something other than 0.5f
		// in the pipeline being
		// evaluated then something is wrong.
		assertEquals(new Long(1), g.traversal().E().has("weight", P.test((t, u) -> {
			assertEquals(0.5f, t);
			return true;
		}, 0.5)).has("oid", "1").count().next());
	}

	@Test
	public void shouldUpdateEdgeIndicesInNewGraph() {
		final OpcGraph g = OpcGraph.open();
		// g.createIndex("oid", Edge.class);

		final Vertex v = g.addVertex();
		v.addEdge("friend", v, "oid", "1", "weight", 0.5f);
		v.addEdge("friend", v, "oid", "2", "weight", 0.6f);

		// a tricky way to evaluate if indices are actually being used is to pass a fake
		// BiPredicate to has()
		// to get into the Pipeline and evaluate what's going through it. in this case,
		// we know that at index
		// is used because only oid 1 should pass through the pipeline due to the
		// inclusion of the
		// key index lookup on "oid". If there's an weight of something other than 0.5f
		// in the pipeline being
		// evaluated then something is wrong.
		assertEquals(new Long(1), g.traversal().E().has("weight", P.test((t, u) -> {
			assertEquals(0.5f, t);
			return true;
		}, 0.5)).has("oid", "1").count().next());
	}

	@Test
	public void shouldUpdateVertexIndicesInExistingGraph() {
		final OpcGraph g = OpcGraph.open();

		g.addVertex("name", "marko", "age", 29);
		g.addVertex("name", "stephen", "age", 35);

		// a tricky way to evaluate if indices are actually being used is to pass a fake
		// BiPredicate to has()
		// to get into the Pipeline and evaluate what's going through it. in this case,
		// we know that at index
		// is not used because "stephen" and "marko" ages both pass through the
		// pipeline.
		assertEquals(new Long(1), g.traversal().V().has("age", P.test((t, u) -> {
			assertTrue(t.equals(35) || t.equals(29));
			return true;
		}, 35)).has("name", "stephen").count().next());

		// g.createIndex("name", Vertex.class);

		// another spy into the pipeline for index check. in this case, we know that at
		// index
		// is used because only "stephen" ages should pass through the pipeline due to
		// the inclusion of the
		// key index lookup on "name". If there's an age of something other than 35 in
		// the pipeline being evaluated
		// then something is wrong.
		assertEquals(new Long(1), g.traversal().V().has("age", P.test((t, u) -> {
			assertEquals(35, t);
			return true;
		}, 35)).has("name", "stephen").count().next());
	}

	@Test
	public void shouldUpdateVertexIndicesInNewGraph() {
		final OpcGraph g = OpcGraph.open();
		// g.createIndex("name", Vertex.class);

		g.addVertex("name", "marko", "age", 29);
		g.addVertex("name", "stephen", "age", 35);

		// a tricky way to evaluate if indices are actually being used is to pass a fake
		// BiPredicate to has()
		// to get into the Pipeline and evaluate what's going through it. in this case,
		// we know that at index
		// is used because only "stephen" ages should pass through the pipeline due to
		// the inclusion of the
		// key index lookup on "name". If there's an age of something other than 35 in
		// the pipeline being evaluated
		// then something is wrong.
		assertEquals(new Long(1), g.traversal().V().has("age", P.test((t, u) -> {
			assertEquals(35, t);
			return true;
		}, 35)).has("name", "stephen").count().next());
	}

	@Test
	public void shouldWorkWithoutIdentityStrategy() {
		final Graph graph = OpcFactory.createModern();
		final GraphTraversalSource g = traversal().withEmbedded(graph).withoutStrategies(IdentityRemovalStrategy.class);
		final List<Map<String, Object>> result = g.V().match(__.as("a").out("knows").values("name").as("b")).identity()
				.toList();
		assertEquals(2, result.size());
		result.stream().forEach(m -> {
			assertEquals(2, m.size());
			assertThat(m.containsKey("a"), is(true));
			assertThat(m.containsKey("b"), is(true));
		});
	}
}
