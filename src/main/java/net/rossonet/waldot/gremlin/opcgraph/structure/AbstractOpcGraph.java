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

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.io.Io;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoVersion;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.OpcMappingStrategy;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputer;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcServiceRegistry;
import net.rossonet.waldot.namespaces.HomunculusNamespace;

/**
 * Base class for {@link OpcGraph} and {@link OpcTransactionGraph}. Contains
 * common methods, variables and constants, but leaves the work with elements
 * and indices to concrete implementations.
 *
 * @author Valentyn Kahamlyk
 */
public abstract class AbstractOpcGraph implements Graph {

	/**
	 * OpcGraph will use an implementation of this interface to generate identifiers
	 * when a user does not supply them and to handle identifier conversions when
	 * querying to provide better flexibility with respect to handling different
	 * data types that mean the same thing. For example, the
	 * {@link NodeIdManager#LONG} implementation will allow
	 * {@code g.vertices(1l, 2l)} and {@code g.vertices(1, 2)} to both return
	 * values.
	 *
	 * @param <T> the id type
	 */
	public interface IdManager<T> {
		/**
		 * Determine if an identifier is allowed by this manager given its type.
		 */
		boolean allow(AbstractOpcGraph graph, final Object id);

		/**
		 * Convert an identifier to the type required by the manager.
		 */
		T convert(AbstractOpcGraph graph, final Object id);

		/**
		 * Generate an identifier which should be unique to the {@link OpcGraph}
		 * instance.
		 */
		T getNextId(final AbstractOpcGraph graph);

	}

	public class OpcGraphEdgeFeatures implements Features.EdgeFeatures {

		private final AbstractOpcGraph graph;

		protected OpcGraphEdgeFeatures(AbstractOpcGraph graph) {
			this.graph = graph;
		}

		@Override
		public boolean supportsCustomIds() {
			return false;
		}

		@Override
		public boolean supportsNullPropertyValues() {
			return allowNullPropertyValues;
		}

		@Override
		public boolean willAllowId(final Object id) {
			return edgeIdManager.allow(graph, id);
		}
	}

	public class OpcGraphVertexFeatures implements Features.VertexFeatures {
		private final AbstractOpcGraph graph;

		private final OpcGraph.OpcGraphVertexPropertyFeatures vertexPropertyFeatures;

		protected OpcGraphVertexFeatures(AbstractOpcGraph graph) {
			this.graph = graph;
			vertexPropertyFeatures = new OpcGraph.OpcGraphVertexPropertyFeatures(graph);
		}

		@Override
		public VertexProperty.Cardinality getCardinality(final String key) {
			return defaultVertexPropertyCardinality;
		}

		@Override
		public Features.VertexPropertyFeatures properties() {
			return vertexPropertyFeatures;
		}

		@Override
		public boolean supportsCustomIds() {
			return false;
		}

		@Override
		public boolean supportsNullPropertyValues() {
			return allowNullPropertyValues;
		}

		@Override
		public boolean willAllowId(final Object id) {
			return vertexIdManager.allow(graph, id);
		}
	}

	public class OpcGraphVertexPropertyFeatures implements Features.VertexPropertyFeatures {
		private final AbstractOpcGraph graph;

		protected OpcGraphVertexPropertyFeatures(AbstractOpcGraph graph) {
			this.graph = graph;
		}

		@Override
		public boolean supportsCustomIds() {
			return false;
		}

		@Override
		public boolean supportsNullPropertyValues() {
			return allowNullPropertyValues;
		}

		@Override
		public boolean willAllowId(final Object id) {
			return vertexIdManager.allow(graph, id);
		}
	}

	public static final String EMPTY_PROPERTY = "p[empty]";
	public static final String EMPTY_VERTEX_PROPERTY = "vp[empty]";
	public static final String V = "v";
	public static final String P = "p";
	public static final String VP = "vp";
	public static final String E = "e";
	public static final String DASH = "-";
	public static final String ARROW = "->";
	public static final String L_BRACKET = "[";
	public static final String R_BRACKET = "]";

	// TODO verificare implementabilità con OPC
	public static final String GREMLIN_OPCGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY = "gremlin.opcgraph.defaultVertexPropertyCardinality";
	public static final String GREMLIN_OPCGRAPH_ALLOW_NULL_PROPERTY_VALUES = "gremlin.opcgraph.allowNullPropertyValues";
	public static final String GREMLIN_OPCGRAPH_SERVICE = "gremlin.opcgraph.service";

	protected AtomicLong generatedId = new AtomicLong(120000L);

	protected IdManager<NodeId> vertexIdManager = OpcMappingStrategy.getNodeIdManager();
	protected IdManager<NodeId> edgeIdManager = OpcMappingStrategy.getNodeIdManager();
	protected IdManager<NodeId> vertexPropertyIdManager = OpcMappingStrategy.getNodeIdManager();

	protected VertexProperty.Cardinality defaultVertexPropertyCardinality;

	protected boolean allowNullPropertyValues;

	protected OpcServiceRegistry serviceRegistry;

	protected Configuration configuration;

	@Override
	public abstract Vertex addVertex(final Object... keyValues);

	public abstract void clear();

	@Override
	public void close() {
		clear();
		serviceRegistry.close();
	}

	@Override
	public GraphComputer compute() {
		return new OpcGraphComputer(this);
	}

	////////////// STRUCTURE API METHODS //////////////////
	@SuppressWarnings("unchecked")
	@Override
	public <C extends GraphComputer> C compute(final Class<C> graphComputerClass) {
		if (!graphComputerClass.equals(OpcGraphComputer.class)) {
			throw Exceptions.graphDoesNotSupportProvidedGraphComputer(graphComputerClass);
		}
		return (C) new OpcGraphComputer(this);
	}

	@Override
	public Configuration configuration() {
		return configuration;
	}

	protected abstract <T extends Element> Iterator<T> createElementIterator(final Class<T> clazz,
			final Map<NodeId, T> elements, final IdManager<?> idManager, final NodeId[] ids);

	@Override
	public Iterator<Edge> edges(final Object... edgeIds) {
		final NodeId[] nodeIds = new NodeId[edgeIds.length];
		for (int i = 0; i < edgeIds.length; i++) {
			nodeIds[i] = vertexIdManager.convert(this, edgeIds[i]);
		}
		return createElementIterator(Edge.class, getOpcNamespace().getEdges(), edgeIdManager, nodeIds);
	}

	public int getEdgesCount() {
		return getOpcNamespace().getEdgesCount();
	}

	public AtomicLong getGeneratedId() {
		return generatedId;
	}

	public abstract HomunculusNamespace getOpcNamespace();

	public int getVerticesCount() {
		return getOpcNamespace().getVerticesCount();
	}

	///////////// GRAPH SPECIFIC INDEXING METHODS ///////////////

	@SuppressWarnings({ "rawtypes" })
	protected OpcServiceRegistry.OpcServiceFactory instantiate(final String className) {
		try {
			return (OpcServiceRegistry.OpcServiceFactory) Class.forName(className)
					.getConstructor(AbstractOpcGraph.class).newInstance(this);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
				| InvocationTargetException ex) {
			throw new RuntimeException(ex);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <I extends Io> I io(final Io.Builder<I> builder) {
		if (builder.requiresVersion(GryoVersion.V1_0) || builder.requiresVersion(GraphSONVersion.V1_0)) {
			return (I) builder.graph(this).onMapper(mapper -> mapper.addRegistry(OpcIoRegistryV1.instance())).create();
		} else if (builder.requiresVersion(GraphSONVersion.V2_0)) { // there is no gryo v2
			return (I) builder.graph(this).onMapper(mapper -> mapper.addRegistry(OpcIoRegistryV2.instance())).create();
		} else { // there is no gryo v2
			return (I) builder.graph(this).onMapper(mapper -> mapper.addRegistry(OpcIoRegistryV3.instance())).create();
		}
	}

	public void removeVertex(NodeId nodeId) {
		getOpcNamespace().removeVertex(nodeId);

	}

	public abstract void setNamespace(HomunculusNamespace waldotNamespace);

	@Override
	public String toString() {
		return StringFactory.graphString(this, "vertices:" + this.getOpcNamespace().getVerticesCount() + " edges:"
				+ this.getOpcNamespace().getEdgesCount());
	}

	@Override
	public abstract Transaction tx();

	@Override
	public Variables variables() {
		return getOpcNamespace().getVariables().variables();
	}

	public Vertex vertex(final NodeId vertexId) {
		return getOpcNamespace().getVertexNode(edgeIdManager.convert(this, vertexId));
	}

	@Override
	public Iterator<Vertex> vertices(final Object... vertexIds) {
		final NodeId[] nodeIds = new NodeId[vertexIds.length];
		for (int i = 0; i < vertexIds.length; i++) {
			nodeIds[i] = vertexIdManager.convert(this, vertexIds[i]);
		}
		return createElementIterator(Vertex.class, getOpcNamespace().getVertices(), vertexIdManager, nodeIds);
	}
}
