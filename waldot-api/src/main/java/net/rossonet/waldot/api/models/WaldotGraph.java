package net.rossonet.waldot.api.models;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * WaldotGraph is an interface that extends Graph and provides methods to manage
 * the Waldot graph structure, including retrieving vertices and edges, managing
 * namespaces, and handling graph computer views.
 * 
 * <p>WaldotGraph is the main interface for the WaldOT graph database. It combines
 * Apache TinkerPop's Graph interface with WaldOT-specific functionality for
 * OPC UA integration. Each vertex and edge in the graph corresponds to an OPC UA
 * node in the address space.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Open the graph (using GraphFactory or direct instantiation)
 * WaldotGraph graph = WaldotGraph.open();
 * 
 * // Add a vertex
 * Vertex v = graph.addVertex();
 * v.property("label", "temperatureSensor");
 * v.property("value", 25.5);
 * 
 * // Add another vertex and connect with an edge
 * Vertex v2 = graph.addVertex();
 * Edge e = v.addEdge("controls", v2);
 * e.property("priority", 5);
 * 
 * // Query vertices
 * for (Vertex vertex : graph.vertices()) {
 *     System.out.println(vertex.id() + ": " + vertex.label());
 * }
 * 
 * // Query edges
 * for (Edge edge : graph.edges()) {
 *     System.out.println(edge.label());
 * }
 * 
 * // Get namespace for advanced operations
 * WaldotNamespace ns = graph.getWaldotNamespace();
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see Graph
 * @see Vertex
 * @see Edge
 */
public interface WaldotGraph extends Graph, AutoCloseable {
	/**
	 * Arrow symbol for toString representation of edges.
	 */
	public static final String ARROW = "->";
	/**
	 * Dash symbol for toString representation of edges.
	 */
	public static final String DASH = "-";
	/**
	 * Prefix for edge identifiers in toString.
	 */
	public static final String E = "e";
	/**
	 * Representation of an empty property.
	 */
	public static final String EMPTY_PROPERTY = "p[empty]";
	/**
	 * Representation of an empty vertex property.
	 */
	public static final String EMPTY_VERTEX_PROPERTY = "vp[empty]";
	/**
	 * Left bracket for toString representation.
	 */
	public static final String L_BRACKET = "[";
	/**
	 * Prefix for property identifiers in toString.
	 */
	public static final String P = "p";
	/**
	 * Right bracket for toString representation.
	 */
	public static final String R_BRACKET = "]";
	/**
	 * Prefix for vertex identifiers in toString.
	 */
	public static final String V = "v";
	/**
	 * Prefix for vertex property identifiers in toString.
	 */
	public static final String VP = "vp";

	/**
	 * Creates an iterator for graph elements with optional filtering.
	 * 
	 * <p>This is a helper method used internally for vertex and edge iteration.
	 * It handles conversion between NodeId and native element IDs, and applies
	 * GraphComputer filtering when in OLAP mode.</p>
	 * 
	 * @param <T> the type of element to iterate
	 * @param <W> the type of WaldotElement in the map
	 * @param cast the class to cast elements to
	 * @param clazz the class of WaldotElement
	 * @param elements map of NodeId to WaldotElement
	 * @param idManager the IdManager for ID conversion
	 * @param ids optional array of specific IDs to iterate (empty = all)
	 * @return iterator of elements
	 * @see Iterator
	 * @see IdManager
	 */
	default <T extends Element, W extends WaldotElement> Iterator<T> createElementIterator(final Class<T> cast,
			final Class<W> clazz, final Map<NodeId, W> elements, final IdManager<?> idManager, final NodeId[] ids) {
		final Iterator<T> iterator;
		final Collection<Entry<NodeId, W>> originalList = elements.entrySet();
		final Map<NodeId, T> convertedList = new HashMap<>();
		for (final Entry<NodeId, W> data : originalList) {
			convertedList.put(data.getKey(), (T) data.getValue());
		}
		if (0 == ids.length) {
			iterator = new WaldotGraphIterator<>(convertedList.values().iterator());
		} else {
			final List<Object> idList = Arrays.asList((Object[]) ids);
			return new WaldotGraphIterator<>(IteratorUtils.filter(IteratorUtils.map(idList, id -> {
				if (null == id) {
					return null;
				}
				final Object iid = cast.isAssignableFrom(id.getClass()) ? cast.cast(id).id()
						: idManager.convert(this, id);
				return convertedList.get(idManager.convert(this, iid));
			}).iterator(), Objects::nonNull));
		}
		return (getWaldotNamespace().inComputerMode()) ? (Iterator<T>) (cast.equals(Vertex.class)
				? IteratorUtils.filter((Iterator<Vertex>) iterator, t -> getGraphComputerView().legalVertex(t))
				: IteratorUtils.filter((Iterator<Edge>) iterator,
						t -> getGraphComputerView().legalEdge(t.outVertex(), t)))
				: iterator;
	}

	/**
	 * Returns the total count of edges in the graph.
	 * 
	 * @return the number of edges
	 */
	int getEdgesCount();

	/**
	 * Returns the next generated ID for vertex/edge creation.
	 * 
	 * <p>This is used internally to generate unique NodeIds for new
	 * graph elements.</p>
	 * 
	 * @return the next generated ID
	 */
	Long getGeneratedId();

	/**
	 * Returns the GraphComputer view for OLAP processing.
	 * 
	 * <p>When in computer mode, this provides access to the graph as
	 * seen during OLAP processing.</p>
	 * 
	 * @return the WaldotGraphComputerView, or null if not in computer mode
	 * @see WaldotGraphComputerView
	 */
	WaldotGraphComputerView getGraphComputerView();

	/**
	 * Returns the total count of vertices in the graph.
	 * 
	 * @return the number of vertices
	 */
	int getVerticesCount();

	/**
	 * Returns the WaldotNamespace associated with this graph.
	 * 
	 * <p>The namespace provides access to OPC UA functionality, strategies,
	 * and management operations.</p>
	 * 
	 * @return the WaldotNamespace
	 * @see WaldotNamespace
	 */
	WaldotNamespace getWaldotNamespace();

	/**
	 * Removes a vertex by its NodeId.
	 * 
	 * @param nodeId the NodeId of the vertex to remove
	 * @see Vertex#remove()
	 */
	void removeVertex(NodeId nodeId);

	/**
	 * Sets the namespace for this graph.
	 * 
	 * <p>This is typically called during graph initialization to associate
	 * the graph with its namespace.</p>
	 * 
	 * @param waldotNamespace the namespace to set
	 * @see #getWaldotNamespace()
	 */
	void setNamespace(WaldotNamespace waldotNamespace);

	/**
	 * Gets a vertex by its NodeId.
	 * 
	 * <p>This is a convenience method that finds a vertex using its
	 * OPC UA NodeId rather than the native graph ID.</p>
	 * 
	 * @param vertexId the NodeId of the vertex
	 * @return the Vertex, or null if not found
	 * @see Vertex
	 * @see NodeId
	 */
	Vertex vertex(NodeId vertexId);

}
