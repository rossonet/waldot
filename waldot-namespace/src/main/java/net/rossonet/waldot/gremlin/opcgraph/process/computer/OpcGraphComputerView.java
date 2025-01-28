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
package net.rossonet.waldot.gremlin.opcgraph.process.computer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.PropertiesStep;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotGraphComputerView;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
// TODO capire come migliorare le performance su opc
public final class OpcGraphComputerView implements WaldotGraphComputerView {

	private final WaldotGraph graph;
	protected final Map<String, VertexComputeKey> computeKeys;
	private final Map<Element, Map<String, List<VertexProperty<?>>>> computeProperties;
	private final Set<Object> legalVertices = new HashSet<>();
	private final Map<Object, Set<Object>> legalEdges = new HashMap<>();
	private final GraphFilter graphFilter;
	private final Set<String> retainVertexProperties;

	public OpcGraphComputerView(final WaldotGraph graph, final GraphFilter graphFilter,
			final Set<VertexComputeKey> computeKeys) {
		this.graph = graph;
		this.computeKeys = new HashMap<>();
		computeKeys.forEach(key -> this.computeKeys.put(key.getKey(), key));
		this.computeProperties = new ConcurrentHashMap<>();
		this.graphFilter = graphFilter;
		if (this.graphFilter.hasFilter()) {
			graph.vertices().forEachRemaining(vertex -> {
				boolean legalVertex = false;
				if (this.graphFilter.hasVertexFilter() && this.graphFilter.legalVertex(vertex)) {
					this.legalVertices.add(vertex.id());
					legalVertex = true;
				}
				if ((legalVertex || !this.graphFilter.hasVertexFilter()) && this.graphFilter.hasEdgeFilter()) {
					final Set<Object> edges = new HashSet<>();
					this.legalEdges.put(vertex.id(), edges);
					this.graphFilter.legalEdges(vertex).forEachRemaining(edge -> edges.add(edge.id()));
				}
			});
		}
		if (this.graphFilter.hasVertexPropertyFilter()) {
			retainVertexProperties = new HashSet<>(Arrays
					.asList(((PropertiesStep) graphFilter.getVertexPropertyFilter().getStartStep()).getPropertyKeys()));
		} else {
			retainVertexProperties = null;
		}
	}

	private void addPropertiesToOriginalGraph() {
		graph.getOpcNamespace().dropGraphComputerView();
		this.computeProperties.forEach((element, properties) -> {
			properties.forEach((key, vertexProperties) -> {
				vertexProperties.forEach(vertexProperty -> {
					final VertexProperty<?> newVertexProperty = ((Vertex) element).property(
							VertexProperty.Cardinality.list, vertexProperty.key(), vertexProperty.value(), T.id,
							vertexProperty.id());
					vertexProperty.properties().forEachRemaining(property -> {
						newVertexProperty.property(property.key(), property.value());
					});
				});
			});
		});
		this.computeProperties.clear();
	}

	@Override
	public <V> VertexProperty<V> addProperty(Vertex opcVertex, String key, V value) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * public <V> Property<V> addProperty(final OpcVertex vertex, final String key,
	 * final V value) { ElementHelper.validateProperty(key, value); if
	 * (isComputeKey(key)) { // TODO: attenzione al nodeId final
	 * OpcVertexProperty<V> property = new OpcVertexProperty<V>(graph, null, vertex,
	 * key, value) {
	 * 
	 * @Override public void remove() { removeProperty(vertex, key, this); } };
	 * this.addValue(vertex, key, property); return property; } else { throw
	 * GraphComputer.Exceptions.providedKeyIsNotAnElementComputeKey(key); } }
	 */
	private void addValue(final Vertex vertex, final String key, final VertexProperty property) {
		final Map<String, List<VertexProperty<?>>> elementProperties = this.computeProperties.computeIfAbsent(vertex,
				k -> new HashMap<>());
		elementProperties.compute(key, (k, v) -> {
			if (null == v) {
				v = new ArrayList<>();
			}
			v.add(property);
			return v;
		});
	}

	@Override
	public void complete() {
		// remove all transient properties from the vertices
		for (final VertexComputeKey computeKey : this.computeKeys.values()) {
			if (computeKey.isTransient()) {
				for (final Map<String, List<VertexProperty<?>>> properties : this.computeProperties.values()) {
					properties.remove(computeKey.getKey());
				}
			}
		}
	}

	@Override
	public List<Property> getProperties(final Vertex vertex) {
		final List<Property> list = new ArrayList<>();
		for (final VertexProperty properties : getPropertiesMap(vertex).values()) {
			list.add(properties);
		}
		for (final List<VertexProperty<?>> properties : this.computeProperties
				.getOrDefault(vertex, Collections.emptyMap()).values()) {
			list.addAll(properties);
		}
		return list;
	}

	private Map<String, WaldotVertexProperty<Object>> getPropertiesMap(final Vertex vertex) {
		final Map<String, WaldotVertexProperty<Object>> propertiesMap = ((WaldotVertex) vertex).getVertexProperties();
		if (retainVertexProperties != null) {
			propertiesMap.keySet().retainAll(retainVertexProperties);
		}
		return propertiesMap;
	}

	@Override
	public List<VertexProperty<?>> getProperty(final Vertex vertex, final String key) {
		// if the vertex property is already on the vertex, use that.
		final List<VertexProperty<?>> vertexProperty = this.getValue(vertex, key);
		return vertexProperty;
	}

	//////////////////////

	private List<VertexProperty<?>> getValue(final Vertex vertex, final String key) {
		return this.computeProperties.getOrDefault(vertex, Collections.emptyMap()).getOrDefault(key,
				Collections.emptyList());
	}

	private boolean isComputeKey(final String key) {
		return this.computeKeys.containsKey(key);
	}

	//////////////////////

	@Override
	public boolean legalEdge(final Vertex vertex, final Edge edge) {
		return !this.graphFilter.hasEdgeFilter() || this.legalEdges.get(vertex.id()).contains(edge.id());
	}

	@Override
	public boolean legalVertex(final Vertex vertex) {
		return !this.graphFilter.hasVertexFilter() || this.legalVertices.contains(vertex.id());
	}

	private WaldotGraph open() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Graph processResultGraphPersist(final GraphComputer.ResultGraph resultGraph,
			final GraphComputer.Persist persist) {
		if (GraphComputer.Persist.NOTHING == persist) {
			if (GraphComputer.ResultGraph.ORIGINAL == resultGraph) {
				return this.graph;
			} else {
				return EmptyGraph.instance();
			}
		} else if (GraphComputer.Persist.VERTEX_PROPERTIES == persist) {
			if (GraphComputer.ResultGraph.ORIGINAL == resultGraph) {
				this.addPropertiesToOriginalGraph();
				return this.graph;
			} else {
				// TODO capire come gestire
				final WaldotGraph newGraph = open();
				this.graph.vertices().forEachRemaining(vertex -> {
					final Vertex newVertex = newGraph.addVertex(T.id, vertex.id(), T.label, vertex.label());
					vertex.properties().forEachRemaining(vertexProperty -> {
						final VertexProperty<?> newVertexProperty = newVertex.property(VertexProperty.Cardinality.list,
								vertexProperty.key(), vertexProperty.value(), T.id, vertexProperty.id());
						vertexProperty.properties().forEachRemaining(property -> {
							newVertexProperty.property(property.key(), property.value());
						});
					});
				});
				return newGraph;
			}
		} else { // Persist.EDGES
			if (GraphComputer.ResultGraph.ORIGINAL == resultGraph) {
				this.addPropertiesToOriginalGraph();
				return this.graph;
			} else {
				final OpcGraph newGraph = OpcGraph.open();
				this.graph.vertices().forEachRemaining(vertex -> {
					final Vertex newVertex = newGraph.addVertex(T.id, vertex.id(), T.label, vertex.label());
					vertex.properties().forEachRemaining(vertexProperty -> {
						final VertexProperty<?> newVertexProperty = newVertex.property(VertexProperty.Cardinality.list,
								vertexProperty.key(), vertexProperty.value(), T.id, vertexProperty.id());
						vertexProperty.properties().forEachRemaining(property -> {
							newVertexProperty.property(property.key(), property.value());
						});
					});
				});
				this.graph.edges().forEachRemaining(edge -> {
					final Vertex outVertex = newGraph.vertices(edge.outVertex().id()).next();
					final Vertex inVertex = newGraph.vertices(edge.inVertex().id()).next();
					final Edge newEdge = outVertex.addEdge(edge.label(), inVertex, T.id, edge.id());
					edge.properties().forEachRemaining(property -> newEdge.property(property.key(), property.value()));
				});
				return newGraph;
			}
		}
	}

	@Override
	public void removeProperty(final Vertex vertex, final String key, final VertexProperty property) {
		if (isComputeKey(key)) {
			this.removeValue(vertex, key, property);
		} else {
			throw GraphComputer.Exceptions.providedKeyIsNotAnElementComputeKey(key);
		}
	}

	private void removeValue(final Vertex vertex, final String key, final VertexProperty property) {
		this.computeProperties.<List<Map<String, VertexProperty<?>>>>getOrDefault(vertex, Collections.emptyMap())
				.get(key).remove(property);
	}
}