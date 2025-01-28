package net.rossonet.waldot.api.models;

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
	boolean allow(WaldotGraph graph, final Object id);

	/**
	 * Convert an identifier to the type required by the manager.
	 */
	T convert(WaldotGraph graph, final Object id);

	/**
	 * Generate an identifier which should be unique to the {@link WaldotGraph}
	 * instance.
	 */
	T getNextId(final WaldotGraph graph);

}