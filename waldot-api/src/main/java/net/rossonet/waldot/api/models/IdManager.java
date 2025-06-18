package net.rossonet.waldot.api.models;

/**
 * Interface for managing IDs in a WaldotGraph. This interface defines methods
 * to check if an ID is allowed, convert an ID to a specific type, and get the
 * next ID.
 *
 * @param <T> the type of ID managed by this IdManager
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface IdManager<T> {

	boolean allow(WaldotGraph graph, final Object id);

	T convert(WaldotGraph graph, final Object id);

	T getNextId(final WaldotGraph graph);

}