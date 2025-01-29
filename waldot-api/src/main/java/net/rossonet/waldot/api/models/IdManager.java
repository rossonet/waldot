package net.rossonet.waldot.api.models;

public interface IdManager<T> {

	boolean allow(WaldotGraph graph, final Object id);

	T convert(WaldotGraph graph, final Object id);

	T getNextId(final WaldotGraph graph);

}