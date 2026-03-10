package net.rossonet.waldot.api.models;

/**
 * Interface for managing IDs in a WaldotGraph. This interface defines methods
 * to check if an ID is allowed, convert an ID to a specific type, and get the
 * next ID.
 *
 * <p>IdManager provides abstraction over ID handling in the WaldOT graph.
 * Different implementations can handle different ID types (e.g., NodeId,
 * String, Integer) with custom conversion and validation logic.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a NodeId IdManager
 * IdManager<NodeId> nodeIdManager = MiloStrategy.getNodeIdManager();
 * 
 * // Check if an ID is allowed
 * boolean allowed = nodeIdManager.allow(graph, someId);
 * 
 * // Convert an ID to the managed type
 * NodeId converted = nodeIdManager.convert(graph, "ns=2;i=123");
 * 
 * // Get the next ID for a new element
 * NodeId nextId = nodeIdManager.getNextId(graph);
 * }</pre>
 *
 * @param <T> the type of ID managed by this IdManager
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see WaldotGraph
 */
public interface IdManager<T> {

	/**
	 * Checks if an ID is allowed for use in the graph.
	 * 
	 * <p>This validation method determines whether the given ID can be used
	 * to identify an element in the graph. Implementations can apply custom
	 * rules for ID validation.</p>
	 * 
	 * @param graph the WaldotGraph context
	 * @param id the ID to check
	 * @return true if the ID is allowed, false otherwise
	 * @see WaldotGraph
	 */
	boolean allow(WaldotGraph graph, final Object id);

	/**
	 * Converts an ID to the managed type.
	 * 
	 * <p>This method handles conversion from various ID formats (String,
	 * Number, etc.) to the type T managed by this IdManager.</p>
	 * 
	 * @param graph the WaldotGraph context
	 * @param id the ID to convert
	 * @return the converted ID, or null if conversion fails
	 */
	T convert(WaldotGraph graph, final Object id);

	/**
	 * Gets the next available ID for a new graph element.
	 * 
	 * <p>This is used when creating new vertices or edges to generate
	 * unique IDs. The implementation should ensure uniqueness.</p>
	 * 
	 * @param graph the WaldotGraph context
	 * @return the next available ID
	 */
	T getNextId(final WaldotGraph graph);

}