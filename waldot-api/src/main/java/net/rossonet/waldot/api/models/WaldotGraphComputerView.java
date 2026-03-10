package net.rossonet.waldot.api.models;

import java.util.List;

import org.apache.tinkerpop.gremlin.process.computer.GraphComputer.Persist;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer.ResultGraph;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

/**
 * WaldotGraphComputerView is an interface that provides methods to interact
 * with the graph computer view of a Waldot graph. It allows adding properties,
 * retrieving properties, checking edge and vertex legality, processing result
 * graphs, and removing properties.
 * 
 * <p>WaldotGraphComputerView provides the interface for OLAP (Online Analytical
 * Processing) operations in the WaldOT graph. It is used during GraphComputer
 * execution to manage vertices and edges within the compute context.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Get graph computer view from graph
 * WaldotGraphComputerView view = graph.getGraphComputerView();
 * 
 * // Add property to vertex in compute context
 * VertexProperty<String> prop = view.addProperty(vertex, "computed", "value");
 * 
 * // Get all properties on vertex
 * List<Property> props = view.getProperties(vertex);
 * 
 * // Get specific property
 * List<VertexProperty<?>> tempProps = view.getProperty(vertex, "temperature");
 * 
 * // Check if vertex is legal in view
 * if (view.legalVertex(vertex)) {
 *     // process vertex
 * }
 * 
 * // Check if edge is legal in view
 * if (view.legalEdge(outVertex, edge)) {
 *     // process edge
 * }
 * 
 * // Process result graph
 * Graph result = view.processResultGraphPersist(
 *     ResultGraph.NEW, Persist.EDGES
 * );
 * 
 * // Remove property
 * view.removeProperty(vertex, "oldProperty", vertexProperty);
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotGraphComputerView {

	/**
	 * Adds a property to a vertex within the GraphComputer view.
	 * 
	 * <p>This is used during OLAP processing to add computed properties
	 * to vertices.</p>
	 * 
	 * @param <V> the type of the property value
	 * @param vertex the vertex to add the property to
	 * @param key the property key
	 * @param value the property value
	 * @return the created VertexProperty
	 * @see Vertex
	 * @see VertexProperty
	 */
	<V> VertexProperty<V> addProperty(Vertex vertex, String key, V value);

	/**
	 * Signals completion of the GraphComputer view processing.
	 * 
	 * <p>This should be called after all processing is complete to finalize
	 * the view and release resources.</p>
	 */
	void complete();

	/**
	 * Gets all properties on a vertex.
	 * 
	 * @param vertex the vertex to query
	 * @return list of Property objects
	 * @see Property
	 */
	List<Property> getProperties(Vertex vertex);

	/**
	 * Gets properties with a specific key on a vertex.
	 * 
	 * @param vertex the vertex to query
	 * @param key the property key
	 * @return list of matching VertexProperty objects
	 */
	List<VertexProperty<?>> getProperty(Vertex vertex, String key);

	/**
	 * Checks if an edge is legal within the GraphComputer view.
	 * 
	 * <p>This is used to filter edges during OLAP processing based on
	 * view-specific rules.</p>
	 * 
	 * @param vertex the out vertex of the edge
	 * @param edge the edge to check
	 * @return true if the edge is legal
	 * @see Edge
	 * @see Vertex
	 */
	boolean legalEdge(Vertex vertex, Edge edge);

	/**
	 * Checks if a vertex is legal within the GraphComputer view.
	 * 
	 * <p>This is used to filter vertices during OLAP processing based on
	 * view-specific rules.</p>
	 * 
	 * @param vertex the vertex to check
	 * @return true if the vertex is legal
	 * @see Vertex
	 */
	boolean legalVertex(Vertex vertex);

	/**
	 * Processes the result graph with persistence options.
	 * 
	 * <p>This finalizes the OLAP computation and returns a graph with
	 * the specified persistence settings.</p>
	 * 
	 * @param resultGraph how to handle result vertices
	 * @param persist how to handle result edges
	 * @return the processed Graph
	 * @see ResultGraph
	 * @see Persist
	 */
	Graph processResultGraphPersist(ResultGraph resultGraph, Persist persist);

	/**
	 * Removes a property from a vertex.
	 * 
	 * @param vertex the vertex to modify
	 * @param key the property key
	 * @param property the specific property to remove
	 */
	void removeProperty(Vertex vertex, String key, VertexProperty property);

}
