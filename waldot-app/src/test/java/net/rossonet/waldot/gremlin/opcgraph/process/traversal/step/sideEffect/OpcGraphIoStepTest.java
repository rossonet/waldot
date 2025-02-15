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
import static org.junit.Assert.fail;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.tinkerpop.gremlin.TestHelper;
import org.apache.tinkerpop.gremlin.process.traversal.IO;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.util.CustomId;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.WaldotOpcUaServer;
import net.rossonet.waldot.configuration.DefaultHomunculusConfiguration;
import net.rossonet.waldot.configuration.DefaultOpcUaConfiguration;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;

/**
 * It was hard to test the {@link IO#registry} configuration as a generic test.
 * Opted to test it as a bit of a standalone test with OpcGraph.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class OpcGraphIoStepTest {

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

	@Test
	public void shouldWriteReadWithCustomIoRegistryGraphSON() throws Exception {
		final UUID uuid = UUID.randomUUID();
		g.addV("person").property("name", "stephen").property("custom", new CustomId("a", uuid)).iterate();

		final File file = TestHelper.generateTempFile(OpcGraphIoStepTest.class,
				"shouldWriteReadWithCustomIoRegistryGraphSON", ".json");
		g.io(file.getAbsolutePath()).with(IO.registry, CustomId.CustomIdIoRegistry.class.getName()).write().iterate();

		final Graph emptyGraph = OpcGraph.open();
		final GraphTraversalSource emptyG = emptyGraph.traversal();

		try {
			emptyG.io(file.getAbsolutePath()).read().iterate();
			fail("Can't read without a registry");
		} catch (final Exception ignored) {
			// do nothing
		}

		emptyG.io(file.getAbsolutePath()).with(IO.registry, CustomId.CustomIdIoRegistry.instance()).read().iterate();

		assertEquals(1, emptyG.V().has("custom", new CustomId("a", uuid)).count().next().intValue());
	}

	@Test
	public void shouldWriteReadWithCustomIoRegistryGryo() throws Exception {
		final UUID uuid = UUID.randomUUID();
		g.addV("person").property("name", "stephen").property("custom", new CustomId("a", uuid)).iterate();

		final File file = TestHelper.generateTempFile(OpcGraphIoStepTest.class,
				"shouldWriteReadWithCustomIoRegistryGryo", ".kryo");
		g.io(file.getAbsolutePath()).with(IO.registry, CustomId.CustomIdIoRegistry.class.getName()).write().iterate();

		final Graph emptyGraph = OpcGraph.open();
		final GraphTraversalSource emptyG = emptyGraph.traversal();

		try {
			emptyG.io(file.getAbsolutePath()).read().iterate();
			fail("Can't read without a registry");
		} catch (final Exception ignored) {
			// do nothing
		}

		emptyG.io(file.getAbsolutePath()).with(IO.registry, CustomId.CustomIdIoRegistry.instance()).read().iterate();

		assertEquals(1, emptyG.V().has("custom", new CustomId("a", uuid)).count().next().intValue());
	}

	@After
	public void tearDown() {
		if (waldot != null) {
			waldot.shutdown();
		}
	}

}
