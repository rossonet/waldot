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
package net.rossonet.waldot.gremlin.opcgraph;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.tinkerpop.gremlin.LoadGraphWith;
import org.apache.tinkerpop.gremlin.TestHelper;
import org.apache.tinkerpop.gremlin.features.World;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.traversal.strategy.decoration.VertexProgramStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.junit.AssumptionViolatedException;

import io.cucumber.java.Scenario;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputer;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcDegreeCentralityFactory;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcTextSearchFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;

/**
 * The abstract {@link World} implementation for AbstractOpcGraph.
 */
public abstract class OpcWorld implements World {
	/**
	 * Enables testing of GraphComputer functionality.
	 */
	public static class ComputerWorld implements World {
		private static final Random RANDOM = TestHelper.RANDOM;

		private static final List<String> TAGS_TO_IGNORE = Arrays.asList("@StepDrop", "@StepInject", "@StepV", "@StepE",
				"@GraphComputerVerificationOneBulk", "@GraphComputerVerificationStrategyNotSupported",
				"@GraphComputerVerificationMidVNotSupported", "@GraphComputerVerificationInjectionNotSupported",
				"@GraphComputerVerificationStarGraphExceeded", "@GraphComputerVerificationReferenceOnly",
				"@OpcServiceRegistry");

		private final World world;

		public ComputerWorld(World world) {
			this.world = world;
		}

		@Override
		public void afterEachScenario() {
			this.world.afterEachScenario();
		}

		@Override
		public void beforeEachScenario(final Scenario scenario) {
			final List<String> ignores = TAGS_TO_IGNORE.stream().filter(t -> scenario.getSourceTagNames().contains(t))
					.collect(Collectors.toList());
			if (!ignores.isEmpty()) {
				throw new AssumptionViolatedException(
						String.format("This scenario is not supported with GraphComputer: %s", ignores));
			}

			this.world.beforeEachScenario(scenario);
		}

		@Override
		public String changePathToDataFile(final String pathToFileFromGremlin) {
			return this.world.changePathToDataFile(pathToFileFromGremlin);
		}

		@Override
		public String convertIdToScript(final Object id, final Class<? extends Element> type) {
			return this.world.convertIdToScript(id, type);
		}

		@Override
		public GraphTraversalSource getGraphTraversalSource(final LoadGraphWith.GraphData graphData) {
			if (null == graphData) {
				throw new AssumptionViolatedException("GraphComputer does not support mutation");
			}

			return this.world.getGraphTraversalSource(graphData)
					.withStrategies(VertexProgramStrategy.create(new MapConfiguration(new HashMap<String, Object>() {
						{
							put(VertexProgramStrategy.WORKERS, Runtime.getRuntime().availableProcessors());
							put(VertexProgramStrategy.GRAPH_COMPUTER,
									RANDOM.nextBoolean() ? GraphComputer.class.getCanonicalName()
											: OpcGraphComputer.class.getCanonicalName());
						}
					})));
		}
	}

	/**
	 * Enables the storing of {@code null} property values when testing. This is a
	 * terminal decorator and shouldn't be used as an input into another decorator.
	 */
	public static class NullWorld implements World {
		private final OpcWorld world;

		public NullWorld(OpcWorld world) {
			this.world = world;
		}

		@Override
		public void afterEachScenario() {
			this.world.afterEachScenario();
		}

		@Override
		public void beforeEachScenario(final Scenario scenario) {
			this.world.beforeEachScenario(scenario);
		}

		@Override
		public String changePathToDataFile(final String pathToFileFromGremlin) {
			return this.world.changePathToDataFile(pathToFileFromGremlin);
		}

		@Override
		public String convertIdToScript(final Object id, final Class<? extends Element> type) {
			return this.world.convertIdToScript(id, type);
		}

		@Override
		public GraphTraversalSource getGraphTraversalSource(final LoadGraphWith.GraphData graphData) {
			if (graphData != null) {
				throw new UnsupportedOperationException("GraphData not supported: " + graphData.name());
			}

			final Configuration conf = getConfiguration();
			conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_ALLOW_NULL_PROPERTY_VALUES, true);
			return world.open(conf).traversal();
		}
	}

	/**
	 * The concrete {@link World} implementation for OpcGraph that provides the
	 * {@link GraphTraversalSource} instances required by the Gherkin test suite.
	 */
	public static class TinkerGraphWorld extends OpcWorld {
		private static final AbstractOpcGraph modern = registerTestServices(OpcFactory.createModern());
		private static final AbstractOpcGraph classic = registerTestServices(OpcFactory.createClassic());
		private static final AbstractOpcGraph crew = registerTestServices(OpcFactory.createTheCrew());
		private static final AbstractOpcGraph sink = registerTestServices(OpcFactory.createKitchenSink());
		private static final AbstractOpcGraph grateful = registerTestServices(OpcFactory.createGratefulDead());

		@Override
		public GraphTraversalSource getGraphTraversalSource(final LoadGraphWith.GraphData graphData) {
			if (null == graphData) {
				return registerTestServices(OpcGraph.open(getConfiguration())).traversal();
			} else if (graphData == LoadGraphWith.GraphData.CLASSIC) {
				return classic.traversal();
			} else if (graphData == LoadGraphWith.GraphData.CREW) {
				return crew.traversal();
			} else if (graphData == LoadGraphWith.GraphData.MODERN) {
				return modern.traversal();
			} else if (graphData == LoadGraphWith.GraphData.SINK) {
				return sink.traversal();
			} else if (graphData == LoadGraphWith.GraphData.GRATEFUL) {
				return grateful.traversal();
			} else {
				throw new UnsupportedOperationException("GraphData not supported: " + graphData.name());
			}
		}

		@Override
		public AbstractOpcGraph open(final Configuration configuration) {
			return OpcGraph.open(configuration);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * The {@link World} implementation for OpcTransactionGraph that provides the
	 * {@link GraphTraversalSource} instances required by the Gherkin test suite.
	 */
	public static class TinkerTransactionGraphWorld extends OpcWorld {
		private static final AbstractOpcGraph modern;
		private static final AbstractOpcGraph classic;
		private static final AbstractOpcGraph crew;
		private static final AbstractOpcGraph sink;
		private static final AbstractOpcGraph grateful;

		static {
			modern = OpcGraph.open();
			OpcFactory.generateModern(modern);
			modern.tx().commit();
			registerTestServices(modern);

			classic = OpcGraph.open();
			OpcFactory.generateClassic(classic);
			classic.tx().commit();
			registerTestServices(classic);

			crew = OpcGraph.open();
			OpcFactory.generateTheCrew(crew);
			crew.tx().commit();
			registerTestServices(crew);

			sink = OpcGraph.open();
			OpcFactory.generateKitchenSink(sink);
			sink.tx().commit();
			registerTestServices(sink);

			grateful = OpcGraph.open();
			OpcFactory.generateGratefulDead(grateful);
			grateful.tx().commit();
			registerTestServices(grateful);
		}

		@Override
		public GraphTraversalSource getGraphTraversalSource(final LoadGraphWith.GraphData graphData) {
			if (null == graphData) {
				return registerTestServices(OpcGraph.open()).traversal();
			} else if (graphData == LoadGraphWith.GraphData.CLASSIC) {
				return classic.traversal();
			} else if (graphData == LoadGraphWith.GraphData.CREW) {
				return crew.traversal();
			} else if (graphData == LoadGraphWith.GraphData.MODERN) {
				return modern.traversal();
			} else if (graphData == LoadGraphWith.GraphData.SINK) {
				return sink.traversal();
			} else if (graphData == LoadGraphWith.GraphData.GRATEFUL) {
				return grateful.traversal();
			} else {
				throw new UnsupportedOperationException("GraphData not supported: " + graphData.name());
			}
		}

		@Override
		public AbstractOpcGraph open(final Configuration configuration) {
			return OpcGraph.open();
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static Configuration getConfiguration() {
		final Configuration conf = new BaseConfiguration();
		/*
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_VERTEX_ID_MANAGER,
		 * AbstractOpcGraph.NodeIdManager.INTEGER.name());
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_EDGE_ID_MANAGER,
		 * AbstractOpcGraph.NodeIdManager.INTEGER.name());
		 * conf.setProperty(OpcGraph.GREMLIN_OPCGRAPH_VERTEX_PROPERTY_ID_MANAGER,
		 * AbstractOpcGraph.NodeIdManager.LONG.name());
		 */
		return conf;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static AbstractOpcGraph registerTestServices(final AbstractOpcGraph graph) {
		graph.getServiceRegistry().registerService(new OpcTextSearchFactory(graph));
		graph.getServiceRegistry().registerService(new OpcDegreeCentralityFactory(graph));
		return graph;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public String changePathToDataFile(final String pathToFileFromGremlin) {
		return ".." + File.separator + pathToFileFromGremlin;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Get an instance of the underlying AbstractOpcGraph with the provided
	 * configuration.
	 */
	public abstract AbstractOpcGraph open(final Configuration configuration);
}
