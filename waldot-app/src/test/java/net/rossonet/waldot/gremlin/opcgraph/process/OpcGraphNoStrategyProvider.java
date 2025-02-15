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
package net.rossonet.waldot.gremlin.opcgraph.process;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tinkerpop.gremlin.GraphProvider;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.ConnectiveStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.SideEffectStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.finalization.ProfileStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.ProductiveByStrategy;
import org.apache.tinkerpop.gremlin.structure.Graph;

import net.rossonet.waldot.gremlin.opcgraph.OpcGraphProvider;
import net.rossonet.waldot.gremlin.opcgraph.process.traversal.strategy.optimization.OpcGraphStepStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;

/**
 * A {@link GraphProvider} that constructs a {@link TraversalSource} with no
 * default strategies applied. This allows the process tests to be executed
 * without strategies applied.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class OpcGraphNoStrategyProvider extends OpcGraphProvider {

	private static final HashSet<Class<? extends TraversalStrategy>> REQUIRED_STRATEGIES = new HashSet<>(
			Arrays.asList(OpcGraphStepStrategy.class, ProfileStrategy.class, ProductiveByStrategy.class, // this
																											// strategy
																											// is
																											// required
																											// to
																											// maintain
																											// 3.5.x
																											// null
																											// behaviors
																											// defined
																											// in tests
					ConnectiveStrategy.class, SideEffectStrategy.class));

	@Override
	public GraphTraversalSource traversal(final Graph graph) {
		final List<Class> toRemove = TraversalStrategies.GlobalCache.getStrategies(OpcGraph.class).toList().stream()
				.map(TraversalStrategy::getClass).filter(clazz -> !REQUIRED_STRATEGIES.contains(clazz))
				.collect(Collectors.toList());
		return graph.traversal().withoutStrategies(toRemove.toArray(new Class[toRemove.size()]));
	}
}
