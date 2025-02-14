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
package net.rossonet.waldot.gremlin.opcgraph.process.traversal.step.sideEffect;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.WaldotOpcUaServer;
import net.rossonet.waldot.configuration.DefaultHomunculusConfiguration;
import net.rossonet.waldot.configuration.DefaultOpcUaConfiguration;

public class OpcGraphStepTest {

	private GraphTraversalSource g;
	private Graph graph;
	private WaldotOpcUaServer waldot;

	@Before
	public void setup() throws InterruptedException, ExecutionException {
		final DefaultHomunculusConfiguration configuration = DefaultHomunculusConfiguration.getDefault();
		final DefaultOpcUaConfiguration serverConfiguration = DefaultOpcUaConfiguration.getDefault();
		waldot = new WaldotOpcUaServer(configuration, serverConfiguration);
		waldot.startup().get();
		graph = waldot.getGremlinGraph();
		g = graph.traversal();
	}

	/**
	 * OpcGraphStepStrategy pulls `has("age", P.gt(1))` into a HasContainer within
	 * the initial OpcGraphStep. This requires that OpcGraphStep handles the
	 * reduction from Ternary Boolean logics (TRUE, FALSE, ERROR) to ordinary
	 * boolean logics which normally takes place within FilterStep's.
	 */
	@Test
	public void shouldHandleComparisonsWithNaN() {
		g.addV("v1").property("age", Double.NaN).next();
		g.addV("v1").property("age", 3).next();
		final int count = g.V().has("age", P.gt(1)).count().next().intValue();
		assertEquals(1, count);
	}

	@After
	public void tearDown() {
		if (waldot != null) {
			waldot.shutdown();
		}
	}

}
