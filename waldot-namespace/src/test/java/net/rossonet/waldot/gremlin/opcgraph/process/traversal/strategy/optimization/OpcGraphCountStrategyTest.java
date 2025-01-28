/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package net.rossonet.waldot.gremlin.opcgraph.process.traversal.strategy.optimization;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.select;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.util.DefaultTraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.util.EmptyTraversal;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import net.rossonet.waldot.gremlin.opcgraph.process.traversal.step.map.OpcCountGlobalStep;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@RunWith(Parameterized.class)
public class OpcGraphCountStrategyTest {

	private static Traversal.Admin<?, ?> countStep(final Class<? extends Element> elementClass) {
		return new DefaultGraphTraversal<>().addStep(new OpcCountGlobalStep(EmptyTraversal.instance(), elementClass));

	}

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> generateTestParameters() {
		return Arrays.asList(new Object[][] { { __.V().count(), countStep(Vertex.class), Collections.emptyList() },
				{ __.V().count(), countStep(Vertex.class),
						TraversalStrategies.GlobalCache.getStrategies(OpcGraph.class).toList() },
				{ __.V().as("a").count(), countStep(Vertex.class),
						TraversalStrategies.GlobalCache.getStrategies(OpcGraph.class).toList() },
				{ __.V().count().as("a"), countStep(Vertex.class),
						TraversalStrategies.GlobalCache.getStrategies(OpcGraph.class).toList() },
				{ __.V().map(out()).count().as("a"), null,
						TraversalStrategies.GlobalCache.getStrategies(OpcGraph.class).toList() },
				{ __.V().map(out()).identity().count().as("a"), null,
						TraversalStrategies.GlobalCache.getStrategies(OpcGraph.class).toList() },
				{ __.V().map(out().groupCount()).identity().count().as("a"), null,
						TraversalStrategies.GlobalCache.getStrategies(OpcGraph.class).toList() },
				{ __.V().label().map(s -> s.get().length()).count(), null,
						TraversalStrategies.GlobalCache.getStrategies(OpcGraph.class).toList() },
				{ __.V().as("a").map(select("a")).count(), null,
						TraversalStrategies.GlobalCache.getStrategies(OpcGraph.class).toList() },
				//
				{ __.V(), null, Collections.emptyList() }, { __.V().out().count(), null, Collections.emptyList() },
				{ __.V(1).count(), null, Collections.emptyList() }, { __.count(), null, Collections.emptyList() },
				{ __.V().map(out().groupCount("m")).identity().count().as("a"), null, Collections.emptyList() }, });
	}

	@Parameterized.Parameter(value = 0)
	public Traversal original;

	@Parameterized.Parameter(value = 1)
	public Traversal optimized;

	@Parameterized.Parameter(value = 2)
	public Collection<TraversalStrategy> otherStrategies;

	@Test
	public void doTest() {
		final TraversalStrategies strategies = new DefaultTraversalStrategies();
		strategies.addStrategies(OpcGraphCountStrategy.instance());
		for (final TraversalStrategy strategy : this.otherStrategies) {
			strategies.addStrategies(strategy);
		}
		if (this.optimized == null) {
			this.optimized = this.original.asAdmin().clone();
			this.optimized.asAdmin().setStrategies(strategies);
			this.optimized.asAdmin().applyStrategies();
		}
		this.original.asAdmin().setStrategies(strategies);
		this.original.asAdmin().applyStrategies();
		assertEquals(this.optimized, this.original);
	}
}
