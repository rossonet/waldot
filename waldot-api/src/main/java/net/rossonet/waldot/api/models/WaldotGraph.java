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
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface WaldotGraph extends Graph, AutoCloseable {
	public static final String ARROW = "->";
	public static final String DASH = "-";
	public static final String E = "e";
	public static final String EMPTY_PROPERTY = "p[empty]";
	public static final String EMPTY_VERTEX_PROPERTY = "vp[empty]";
	public static final String L_BRACKET = "[";
	public static final String P = "p";
	public static final String R_BRACKET = "]";
	public static final String V = "v";
	public static final String VP = "vp";

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
			final List<Object> idList = Arrays.asList(ids);
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

	int getEdgesCount();

	Long getGeneratedId();

	WaldotGraphComputerView getGraphComputerView();

	int getVerticesCount();

	WaldotNamespace getWaldotNamespace();

	void removeVertex(NodeId nodeId);

	void setNamespace(WaldotNamespace waldotNamespace);

	Vertex vertex(NodeId vertexId);

}
