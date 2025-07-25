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

import static org.apache.tinkerpop.gremlin.structure.io.IoCore.gryo;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.auth.DefaultAnonymousValidator;
import net.rossonet.waldot.auth.DefaultIdentityValidator;
import net.rossonet.waldot.auth.DefaultX509IdentityValidator;
import net.rossonet.waldot.configuration.DefaultHomunculusConfiguration;
import net.rossonet.waldot.configuration.DefaultOpcUaConfiguration;
import net.rossonet.waldot.gremlin.opcgraph.strategies.agent.BaseAgentManagementStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.boot.SingleFileBootstrapStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.console.BaseConsoleStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerBaseStrategy;
import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.opc.WaldotOpcUaServer;

/**
 * Helps create a variety of different toy graphs for testing and learning
 * purposes.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public final class OpcFactory {

	/**
	 * Create the "classic" graph which was the original toy graph from TinkerPop
	 * 2.x.
	 * 
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static WaldotGraph createClassic() throws InterruptedException, ExecutionException {
		final WaldotGraph g = getOpcGraph();
		generateClassic(g);
		return g;
	}

	/**
	 * Creates the "grateful dead" graph which is a larger graph than most of the
	 * toy graphs but has real-world structure and application and is therefore
	 * useful for demonstrating more complex traversals.
	 * 
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static WaldotGraph createGratefulDead() throws InterruptedException, ExecutionException {
		final WaldotGraph g = getOpcGraph();
		generateGratefulDead(g);
		return g;
	}

	/**
	 * Creates the "kitchen sink" graph which is a collection of structures (e.g.
	 * self-loops) that aren't represented in other graphs and are useful for
	 * various testing scenarios.
	 * 
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static WaldotGraph createKitchenSink() throws InterruptedException, ExecutionException {
		final WaldotGraph g = getOpcGraph();
		generateKitchenSink(g);
		return g;
	}

	/**
	 * Create the "modern" graph which has the same structure as the "classic" graph
	 * from TinkerPop 2.x but includes 3.x features like vertex labels.
	 * 
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static WaldotGraph createModern() throws InterruptedException, ExecutionException {
		final WaldotGraph g = getOpcGraph();
		generateModern(g);
		return g;
	}

	/**
	 * Create the "the crew" graph which is a TinkerPop 3.x toy graph showcasing
	 * many 3.x features like meta-properties, multi-properties and graph variables.
	 * 
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static WaldotGraph createTheCrew() throws InterruptedException, ExecutionException {
		final Configuration conf = new BaseConfiguration();
		conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY,
				VertexProperty.Cardinality.list.name());
		final WaldotGraph g = getOpcGraph(conf);
		generateTheCrew(g);
		return g;
	}

	/**
	 * Generate the graph in {@link #createClassic()} into an existing graph.
	 */
	public static void generateClassic(final WaldotGraph g) {
		final Vertex marko = g.addVertex(T.id, 1, "name", "marko", "age", 29);
		final Vertex vadas = g.addVertex(T.id, 2, "name", "vadas", "age", 27);
		final Vertex lop = g.addVertex(T.id, 3, "name", "lop", "lang", "java");
		final Vertex josh = g.addVertex(T.id, 4, "name", "josh", "age", 32);
		final Vertex ripple = g.addVertex(T.id, 5, "name", "ripple", "lang", "java");
		final Vertex peter = g.addVertex(T.id, 6, "name", "peter", "age", 35);
		marko.addEdge("knows", vadas, T.id, 7, "weight", 0.5f);
		marko.addEdge("knows", josh, T.id, 8, "weight", 1.0f);
		marko.addEdge("created", lop, T.id, 9, "weight", 0.4f);
		josh.addEdge("created", ripple, T.id, 10, "weight", 1.0f);
		josh.addEdge("created", lop, T.id, 11, "weight", 0.4f);
		peter.addEdge("created", lop, T.id, 12, "weight", 0.2f);
	}

	/**
	 * Generate the graph in {@link #createGratefulDead()} into an existing graph.
	 */
	public static void generateGratefulDead(final WaldotGraph graph) {
		final InputStream stream = OpcFactory.class.getResourceAsStream("grateful-dead.kryo");
		try {
			graph.io(gryo()).reader().create().readGraph(stream, graph);
		} catch (final Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Generate the graph in {@link #createKitchenSink()} into an existing graph.
	 */
	public static void generateKitchenSink(final WaldotGraph graph) {
		final GraphTraversalSource g = graph.traversal();
		g.addV("loops").property(T.id, 1000).property("name", "loop").as("me").addE("self").to("me")
				.property(T.id, 1001).iterate();
		g.addV("message").property(T.id, 2000).property("name", "a").as("a").addV("message").property(T.id, 2001)
				.property("name", "b").as("b").addE("link").from("a").to("b").property(T.id, 2002).addE("link")
				.from("a").to("a").property(T.id, 2003).iterate();
	}

	/**
	 * Generate the graph in {@link #createModern()} into an existing graph.
	 */
	public static void generateModern(final WaldotGraph g) {
		final Vertex marko = g.addVertex(T.id, 1, T.label, "person");
		marko.property("name", "marko", T.id, 0l);
		marko.property("age", 29, T.id, 1l);
		final Vertex vadas = g.addVertex(T.id, 2, T.label, "person");
		vadas.property("name", "vadas", T.id, 2l);
		vadas.property("age", 27, T.id, 3l);
		final Vertex lop = g.addVertex(T.id, 3, T.label, "software");
		lop.property("name", "lop", T.id, 4l);
		lop.property("lang", "java", T.id, 5l);
		final Vertex josh = g.addVertex(T.id, 4, T.label, "person");
		josh.property("name", "josh", T.id, 6l);
		josh.property("age", 32, T.id, 7l);
		final Vertex ripple = g.addVertex(T.id, 5, T.label, "software");
		ripple.property("name", "ripple", T.id, 8l);
		ripple.property("lang", "java", T.id, 9l);
		final Vertex peter = g.addVertex(T.id, 6, T.label, "person");
		peter.property("name", "peter", T.id, 10l);
		peter.property("age", 35, T.id, 11l);

		marko.addEdge("knows", vadas, T.id, 7, "weight", 0.5d);
		marko.addEdge("knows", josh, T.id, 8, "weight", 1.0d);
		marko.addEdge("created", lop, T.id, 9, "weight", 0.4d);
		josh.addEdge("created", ripple, T.id, 10, "weight", 1.0d);
		josh.addEdge("created", lop, T.id, 11, "weight", 0.4d);
		peter.addEdge("created", lop, T.id, 12, "weight", 0.2d);
	}

	/**
	 * Generate the graph in {@link #createTheCrew()} into an existing graph.
	 */
	public static void generateTheCrew(final WaldotGraph g) {
		final Vertex marko = g.addVertex(T.id, 1, T.label, "person", "name", "marko");
		final Vertex stephen = g.addVertex(T.id, 7, T.label, "person", "name", "stephen");
		final Vertex matthias = g.addVertex(T.id, 8, T.label, "person", "name", "matthias");
		final Vertex daniel = g.addVertex(T.id, 9, T.label, "person", "name", "daniel");
		final Vertex gremlin = g.addVertex(T.id, 10, T.label, "software", "name", "gremlin");
		final Vertex tinkergraph = g.addVertex(T.id, 11, T.label, "software", "name", "tinkergraph");

		marko.property(VertexProperty.Cardinality.list, "location", "san diego", "startTime", 1997, "endTime", 2001);
		marko.property(VertexProperty.Cardinality.list, "location", "santa cruz", "startTime", 2001, "endTime", 2004);
		marko.property(VertexProperty.Cardinality.list, "location", "brussels", "startTime", 2004, "endTime", 2005);
		marko.property(VertexProperty.Cardinality.list, "location", "santa fe", "startTime", 2005);

		stephen.property(VertexProperty.Cardinality.list, "location", "centreville", "startTime", 1990, "endTime",
				2000);
		stephen.property(VertexProperty.Cardinality.list, "location", "dulles", "startTime", 2000, "endTime", 2006);
		stephen.property(VertexProperty.Cardinality.list, "location", "purcellville", "startTime", 2006);

		matthias.property(VertexProperty.Cardinality.list, "location", "bremen", "startTime", 2004, "endTime", 2007);
		matthias.property(VertexProperty.Cardinality.list, "location", "baltimore", "startTime", 2007, "endTime", 2011);
		matthias.property(VertexProperty.Cardinality.list, "location", "oakland", "startTime", 2011, "endTime", 2014);
		matthias.property(VertexProperty.Cardinality.list, "location", "seattle", "startTime", 2014);

		daniel.property(VertexProperty.Cardinality.list, "location", "spremberg", "startTime", 1982, "endTime", 2005);
		daniel.property(VertexProperty.Cardinality.list, "location", "kaiserslautern", "startTime", 2005, "endTime",
				2009);
		daniel.property(VertexProperty.Cardinality.list, "location", "aachen", "startTime", 2009);

		marko.addEdge("develops", gremlin, T.id, 13, "since", 2009);
		marko.addEdge("develops", tinkergraph, T.id, 14, "since", 2010);
		marko.addEdge("uses", gremlin, T.id, 15, "skill", 4);
		marko.addEdge("uses", tinkergraph, T.id, 16, "skill", 5);

		stephen.addEdge("develops", gremlin, T.id, 17, "since", 2010);
		stephen.addEdge("develops", tinkergraph, T.id, 18, "since", 2011);
		stephen.addEdge("uses", gremlin, T.id, 19, "skill", 5);
		stephen.addEdge("uses", tinkergraph, T.id, 20, "skill", 4);

		matthias.addEdge("develops", gremlin, T.id, 21, "since", 2012);
		matthias.addEdge("uses", gremlin, T.id, 22, "skill", 3);
		matthias.addEdge("uses", tinkergraph, T.id, 23, "skill", 3);

		daniel.addEdge("uses", gremlin, T.id, 24, "skill", 5);
		daniel.addEdge("uses", tinkergraph, T.id, 25, "skill", 3);

		gremlin.addEdge("traverses", tinkergraph, T.id, 26);

		g.variables().set("creator", "marko");
		g.variables().set("lastModified", 2014);
		g.variables().set("comment",
				"this graph was created to provide examples and test coverage for tinkerpop3 api advances");
	}

	public static WaldotGraph getOpcGraph() throws InterruptedException, ExecutionException {
		final DefaultHomunculusConfiguration configuration = DefaultHomunculusConfiguration.getDefault();
		final DefaultOpcUaConfiguration serverConfiguration = DefaultOpcUaConfiguration.getDefault();
		final WaldotOpcUaServer waldot = new WaldotOpcUaServer(configuration, serverConfiguration,
				new DefaultAnonymousValidator(configuration), new DefaultIdentityValidator(configuration),
				new DefaultX509IdentityValidator(configuration));
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					if (waldot != null && waldot.getServer() != null) {
						waldot.getServer().shutdown();
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
		final HomunculusNamespace namespace = new HomunculusNamespace(waldot, new MiloSingleServerBaseStrategy(),
				new BaseConsoleStrategy(), configuration, new SingleFileBootstrapStrategy(),
				new BaseAgentManagementStrategy(), "file:///tmp/boot.conf");
		waldot.startup(namespace).get();
		return waldot.getGremlinGraph();
	}

	private static WaldotGraph getOpcGraph(final Configuration conf) throws InterruptedException, ExecutionException {
		// TODO verificare come usare la configurazione originale
		return getOpcGraph();
	}

	private OpcFactory() {
	}
}
