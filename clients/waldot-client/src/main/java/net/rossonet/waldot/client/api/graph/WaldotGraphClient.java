package net.rossonet.waldot.client.api.graph;

import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * Structured access to the graph of the server.
 * <p>
 * The queries are JEXL expressions run by the server (the same syntax of the
 * console, e.g. {@code g.V().has('name','marko').out('knows')}). With
 * {@link #queryObjects} the result travels as typed GraphSON v3 and is
 * rebuilt as Java objects: detached vertices and edges (ids are
 * {@link NodeId}s), paths, maps, lists, numbers, strings.
 * </p>
 */
public interface WaldotGraphClient {

	/**
	 * Describes a vertex or an edge (properties with the NodeId of their
	 * variables, adjacent edges, commands).
	 */
	ElementDescription describe(NodeId element);

	/** Same as {@link #describe(NodeId)} with the plain string id. */
	ElementDescription describe(String element);

	/** @return the edges returned by a query */
	List<Edge> edges(String expression);

	/** @return the NodeId of the OPC UA variable of a property */
	NodeId propertyNodeId(NodeId element, String key);

	/** Runs the text {@code query} command (result rendered as text). */
	QueryResult query(String expression);

	/** @return the raw typed GraphSON v3 of the result */
	String queryJson(String expression);

	/**
	 * Runs a query and rebuilds the result; a single object is returned as a
	 * list of one element, null as an empty list.
	 */
	List<Object> queryObjects(String expression);

	/** Same as {@link #queryObjects(String)} keeping the elements of a type. */
	<T> List<T> queryObjects(String expression, Class<T> type);

	/** Runs a query that returns a single value (or null). */
	Object queryValue(String expression);

	/** @return the vertices returned by a query */
	List<Vertex> vertices(String expression);
}
