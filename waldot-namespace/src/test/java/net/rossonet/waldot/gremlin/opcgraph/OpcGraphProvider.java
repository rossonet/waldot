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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.AbstractGraphProvider;
import org.apache.tinkerpop.gremlin.LoadGraphWith;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.GraphTest;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedGraphTest;
import org.apache.tinkerpop.gremlin.structure.util.star.StarGraphTest;

import net.rossonet.waldot.gremlin.opcgraph.structure.OpcEdge;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcElement;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertexProperty;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class OpcGraphProvider extends AbstractGraphProvider {

	private static final Set<Class> IMPLEMENTATION = new HashSet<Class>() {
		{
			add(OpcEdge.class);
			add(OpcElement.class);
			add(OpcGraph.class);
			add(OpcGraphVariables.class);
			add(OpcProperty.class);
			add(OpcVertex.class);
			add(OpcVertexProperty.class);
		}
	};

	/**
	 * Determines if a test requires a different cardinality as the default or not.
	 */
	protected static boolean requiresListCardinalityAsDefault(final LoadGraphWith.GraphData loadGraphWith,
			final Class<?> test, final String testMethodName) {
		return loadGraphWith == LoadGraphWith.GraphData.CREW
				|| (test == StarGraphTest.class && testMethodName.equals("shouldAttachWithCreateMethod"))
				|| (test == DetachedGraphTest.class && testMethodName.equals("testAttachableCreateMethod"));
	}

	/**
	 * Determines if a test requires OpcGraph persistence to be configured with
	 * graph location and format.
	 */
	protected static boolean requiresPersistence(final Class<?> test, final String testMethodName) {
		return test == GraphTest.class && testMethodName.equals("shouldPersistDataOnClose");
	}

	@Override
	public void clear(final Graph graph, final Configuration configuration) throws Exception {
		if (graph != null) {
			graph.close();
		}
	}

	@Override
	public Map<String, Object> getBaseConfiguration(final String graphName, final Class<?> test,
			final String testMethodName, final LoadGraphWith.GraphData loadGraphWith) {
		// final OpcGraph.DefaultIdManager idManager =
		// selectIdMakerFromGraphData(loadGraphWith);
		/*
		 * final String idMaker = (idManager.equals(OpcGraph.NodeIdManager.ANY) ?
		 * selectIdMakerFromTest(test, testMethodName) : idManager).name();
		 */
		return new HashMap<String, Object>() {
			{
				/*
				 * put(Graph.GRAPH, OpcGraph.class.getName());
				 * put(OpcGraph.GREMLIN_OPCGRAPH_VERTEX_ID_MANAGER, idMaker);
				 * put(OpcGraph.GREMLIN_OPCGRAPH_EDGE_ID_MANAGER, idMaker);
				 * put(OpcGraph.GREMLIN_OPCGRAPH_VERTEX_PROPERTY_ID_MANAGER, idMaker); if
				 * (requiresListCardinalityAsDefault(loadGraphWith, test, testMethodName)) {
				 * put(OpcGraph.GREMLIN_OPCGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY,
				 * VertexProperty.Cardinality.list.name()); } if (requiresPersistence(test,
				 * testMethodName)) { put(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_FORMAT, "gryo");
				 * put(OpcGraph.GREMLIN_OPCGRAPH_GRAPH_LOCATION,
				 * TestHelper.makeTestDataFile(test, "temp", testMethodName + ".kryo")); }
				 */
			}
		};
	}

	@Override
	public Set<Class> getImplementations() {
		return IMPLEMENTATION;
	}

}
