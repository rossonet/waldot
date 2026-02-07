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
package net.rossonet.agent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.AbstractGraphProvider;
import org.apache.tinkerpop.gremlin.LoadGraphWith.GraphData;
import org.apache.tinkerpop.gremlin.structure.Graph;

import net.rossonet.waldot.gremlin.opcgraph.structure.OpcEdge;
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
			add(OpcGraph.class);
			add(OpcGraphVariables.class);
			add(OpcProperty.class);
			add(OpcVertex.class);
			add(OpcVertexProperty.class);
		}
	};

	@Override
	public void clear(Graph graph, Configuration configuration) throws Exception {

	}

	@Override
	public Map<String, Object> getBaseConfiguration(String graphName, Class<?> test, String testMethodName,
			GraphData loadGraphWith) {

		return Collections.emptyMap();
	}

	@Override
	public Set<Class> getImplementations() {
		return IMPLEMENTATION;
	}

}
