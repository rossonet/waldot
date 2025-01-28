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

import java.util.Collections;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.gremlin.opcgraph.process.traversal.strategy.optimization.OpcGraphCountStrategy;
import net.rossonet.waldot.gremlin.opcgraph.process.traversal.strategy.optimization.OpcGraphStepStrategy;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcServiceRegistry;

/**
 * @author Andrea Ambrosini ( - andrea DOT ambrosini AT rossonet DOT org - )
 */
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_INTEGRATE)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_COMPUTER)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_LIMITED_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_LIMITED_COMPUTER)
public class OpcGraph extends AbstractOpcGraph {

	public class OpcGraphFeatures implements Features {
		private final AbstractOpcGraph graph;
		private final OpcGraphGraphFeatures graphFeatures;
		private final OpcGraphEdgeFeatures edgeFeatures;
		private final OpcGraphVertexFeatures vertexFeatures;

		private OpcGraphFeatures(AbstractOpcGraph graph) {
			this.graph = graph;
			graphFeatures = new OpcGraphGraphFeatures();
			edgeFeatures = new OpcGraphEdgeFeatures(graph);
			vertexFeatures = new OpcGraphVertexFeatures(graph);
		}

		@Override
		public EdgeFeatures edge() {
			return edgeFeatures;
		}

		@Override
		public GraphFeatures graph() {
			return graphFeatures;
		}

		@Override
		public String toString() {
			return StringFactory.featureString(this);
		}

		@Override
		public VertexFeatures vertex() {
			return vertexFeatures;
		}

	}

	public class OpcGraphGraphFeatures implements Features.GraphFeatures {

		private OpcGraphGraphFeatures() {
		}

		@Override
		public boolean supportsConcurrentAccess() {
			return false;
		}

		@Override
		public boolean supportsServiceCall() {
			return true;
		}

		@Override
		public boolean supportsThreadedTransactions() {
			return false;
		}

		@Override
		public boolean supportsTransactions() {
			return false;
		}

	}

	static {
		TraversalStrategies.GlobalCache.registerStrategies(OpcGraph.class,
				TraversalStrategies.GlobalCache.getStrategies(Graph.class).clone()
						.addStrategies(OpcGraphStepStrategy.instance(), OpcGraphCountStrategy.instance()));
	}

	private static final Configuration EMPTY_CONFIGURATION = new BaseConfiguration() {
		{
			this.setProperty(Graph.GRAPH, OpcGraph.class.getName());
		}
	};

	public static OpcGraph open() {
		return open(EMPTY_CONFIGURATION);
	}

	public static OpcGraph open(final Configuration configuration) {
		return new OpcGraph(configuration);
	}

	private final OpcGraphFeatures features = new OpcGraphFeatures(this);

	private WaldotNamespace opcNamespace;

	OpcGraph(final Configuration configuration) {
		this.configuration = configuration;
		defaultVertexPropertyCardinality = VertexProperty.Cardinality.valueOf(configuration.getString(
				GREMLIN_OPCGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY, VertexProperty.Cardinality.single.name()));
		allowNullPropertyValues = configuration.getBoolean(GREMLIN_OPCGRAPH_ALLOW_NULL_PROPERTY_VALUES, false);
		serviceRegistry = new OpcServiceRegistry(this);
		configuration.getList(String.class, GREMLIN_OPCGRAPH_SERVICE, Collections.emptyList())
				.forEach(serviceClass -> serviceRegistry.registerService(instantiate(serviceClass)));
	}

	@Override
	public Vertex addVertex(final Object... keyValues) {
		ElementHelper.legalPropertyKeyValueArray(keyValues);
		NodeId nodeId = vertexIdManager.convert(this, ElementHelper.getIdValue(keyValues).orElse(null));
		if (null != nodeId) {
			if (opcNamespace.hasNodeId(nodeId)) {
				throw Exceptions.vertexWithIdAlreadyExists(nodeId);
			}
		} else {
			nodeId = vertexIdManager.getNextId(this);
		}
		final WaldotVertex vertex = opcNamespace.addVertex(nodeId, keyValues);
		return vertex;
	}

	@Override
	public void clear() {
		opcNamespace.resetNameSpace();
	}

	@Override
	public Features features() {
		return features;
	}

	@Override
	public WaldotGraphComputerView getGraphComputerView() {
		return opcNamespace.getGraphComputerView();
	}

	@Override
	public WaldotNamespace getOpcNamespace() {
		return opcNamespace;
	}

	@Override
	public OpcServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	@Override
	public void setNamespace(WaldotNamespace waldotNamespace) {
		this.opcNamespace = waldotNamespace;

	}

	@Override
	public Transaction tx() {
		throw Exceptions.transactionsNotSupported();
	}

}
