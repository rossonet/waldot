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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.BeforeClass;
import org.junit.experimental.runners.Enclosed;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@RunWith(Enclosed.class)
public class TinkerGraphIdManagerTest {

	@RunWith(Parameterized.class)
	public static class NumberIdManagerTest {
		private static final Configuration longIdManagerConfig = new BaseConfiguration();
		private static final Configuration integerIdManagerConfig = new BaseConfiguration();

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> data() {
			return Arrays.asList(new Object[][] { { "coerceLong", 100l, 200l, 300l }, { "coerceInt", 100, 200, 300 },
					{ "coerceDouble", 100d, 200d, 300d }, { "coerceFloat", 100f, 200f, 300f },
					{ "coerceString", "100", "200", "300" }, { "coerceMixed", 100d, 200f, "300" } });
		}

		@BeforeClass
		public static void setup() {
			/*
			 * longIdManagerConfig.addProperty(OpcGraph.GREMLIN_OPCGRAPH_EDGE_ID_MANAGER,
			 * OpcGraph.NodeIdManager.LONG.name());
			 * longIdManagerConfig.addProperty(OpcGraph.GREMLIN_OPCGRAPH_VERTEX_ID_MANAGER,
			 * OpcGraph.NodeIdManager.LONG.name());
			 * longIdManagerConfig.addProperty(OpcGraph.
			 * GREMLIN_OPCGRAPH_VERTEX_PROPERTY_ID_MANAGER,
			 * OpcGraph.NodeIdManager.LONG.name());
			 * 
			 * integerIdManagerConfig.addProperty(OpcGraph.GREMLIN_OPCGRAPH_EDGE_ID_MANAGER,
			 * OpcGraph.NodeIdManager.INTEGER.name());
			 * integerIdManagerConfig.addProperty(OpcGraph.
			 * GREMLIN_OPCGRAPH_VERTEX_ID_MANAGER, OpcGraph.NodeIdManager.INTEGER.name());
			 * integerIdManagerConfig.addProperty(OpcGraph.
			 * GREMLIN_OPCGRAPH_VERTEX_PROPERTY_ID_MANAGER,
			 * OpcGraph.NodeIdManager.INTEGER.name());
			 */
		}

		@Parameterized.Parameter(value = 0)
		public String name;

		@Parameterized.Parameter(value = 1)
		public Object vertexIdValue;

		@Parameterized.Parameter(value = 2)
		public Object edgeIdValue;

		@Parameterized.Parameter(value = 3)
		public Object vertexPropertyIdValue;

		@Test
		public void shouldUseIntegerIdManagerToCoerceTypes() {
			final Graph graph = OpcGraph.open(integerIdManagerConfig);
			final Vertex v = graph.addVertex(T.id, vertexIdValue);
			final VertexProperty vp = v.property(VertexProperty.Cardinality.single, "test", "value", T.id,
					vertexPropertyIdValue);
			final Edge e = v.addEdge("self", v, T.id, edgeIdValue);

			assertEquals(100, v.id());
			assertEquals(200, e.id());
			assertEquals(300, vp.id());
		}

		@Test
		public void shouldUseLongIdManagerToCoerceTypes() {
			final Graph graph = OpcGraph.open(longIdManagerConfig);
			final Vertex v = graph.addVertex(T.id, vertexIdValue);
			final VertexProperty vp = v.property(VertexProperty.Cardinality.single, "test", "value", T.id,
					vertexPropertyIdValue);
			final Edge e = v.addEdge("self", v, T.id, edgeIdValue);

			assertEquals(100l, v.id());
			assertEquals(200l, e.id());
			assertEquals(300l, vp.id());
		}
	}

	@RunWith(Parameterized.class)
	public static class UuidIdManagerTest {
		private static final Configuration idManagerConfig = new BaseConfiguration();

		private static final UUID vertexId = UUID.fromString("0E939658-ADD2-4598-A722-2FC178E9B741");
		private static final UUID edgeId = UUID.fromString("748179AA-E319-8C36-41AE-F3576B73E05C");
		private static final UUID vertexPropertyId = UUID.fromString("EC27384C-39A0-923D-9410-271B585683B6");

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> data() {
			return Arrays.asList(new Object[][] { { "coerceUuid", vertexId, edgeId, vertexPropertyId },
					{ "coerceString", vertexId.toString(), edgeId.toString(), vertexPropertyId.toString() },
					{ "coerceMixed", vertexId, edgeId, vertexPropertyId.toString() } });
		}

		@BeforeClass
		public static void setup() {
			/*
			 * idManagerConfig.addProperty(OpcGraph.GREMLIN_OPCGRAPH_EDGE_ID_MANAGER,
			 * OpcGraph.NodeIdManager.UUID.name());
			 * idManagerConfig.addProperty(OpcGraph.GREMLIN_OPCGRAPH_VERTEX_ID_MANAGER,
			 * OpcGraph.NodeIdManager.UUID.name()); idManagerConfig.addProperty(OpcGraph.
			 * GREMLIN_OPCGRAPH_VERTEX_PROPERTY_ID_MANAGER,
			 * OpcGraph.NodeIdManager.UUID.name());
			 */
		}

		@Parameterized.Parameter(value = 0)
		public String name;

		@Parameterized.Parameter(value = 1)
		public Object vertexIdValue;

		@Parameterized.Parameter(value = 2)
		public Object edgeIdValue;

		@Parameterized.Parameter(value = 3)
		public Object vertexPropertyIdValue;

		@Test
		public void shouldUseIdManagerToCoerceTypes() {
			final Graph graph = OpcGraph.open(idManagerConfig);
			final Vertex v = graph.addVertex(T.id, vertexIdValue);
			final VertexProperty vp = v.property(VertexProperty.Cardinality.single, "test", "value", T.id,
					vertexPropertyIdValue);
			final Edge e = v.addEdge("self", v, T.id, edgeIdValue);

			assertEquals(vertexId, v.id());
			assertEquals(edgeId, e.id());
			assertEquals(vertexPropertyId, vp.id());
		}
	}
}
