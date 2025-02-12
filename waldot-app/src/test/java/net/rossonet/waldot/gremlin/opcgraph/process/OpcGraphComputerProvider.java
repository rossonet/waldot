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

import java.util.HashMap;
import java.util.Random;

import org.apache.commons.configuration2.MapConfiguration;
import org.apache.tinkerpop.gremlin.GraphProvider;
import org.apache.tinkerpop.gremlin.TestHelper;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.traversal.strategy.decoration.VertexProgramStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;

import net.rossonet.waldot.gremlin.opcgraph.OpcGraphProvider;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputer;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@GraphProvider.Descriptor(computer = OpcGraphComputer.class)
public class OpcGraphComputerProvider extends OpcGraphProvider {

	private static final Random RANDOM = TestHelper.RANDOM;

	@Override
	public GraphTraversalSource traversal(final Graph graph) {
		return graph.traversal()
				.withStrategies(VertexProgramStrategy.create(new MapConfiguration(new HashMap<String, Object>() {
					{
						put(VertexProgramStrategy.WORKERS,
								RANDOM.nextInt(Runtime.getRuntime().availableProcessors()) + 1);
						put(VertexProgramStrategy.GRAPH_COMPUTER,
								RANDOM.nextBoolean() ? GraphComputer.class.getCanonicalName()
										: OpcGraphComputer.class.getCanonicalName());
					}
				})));
	}
}
