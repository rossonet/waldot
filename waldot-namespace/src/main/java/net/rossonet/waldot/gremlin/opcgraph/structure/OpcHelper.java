
package net.rossonet.waldot.gremlin.opcgraph.structure;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.models.WaldotEdge;

public final class OpcHelper {

	public static Map<NodeId, WaldotEdge> getEdges(final OpcGraph graph) {
		return graph.getWaldotNamespace().getEdges();
	}

	public static <E extends Element> Iterator<Property> search(final AbstractOpcGraph graph, final String regex,
			final Optional<Class<E>> type) {

		final Supplier<Iterator<Element>> vertices = () -> IteratorUtils.cast(graph.vertices());
		final Supplier<Iterator<Element>> edges = () -> IteratorUtils.cast(graph.edges());
		final Supplier<Iterator<Element>> vertexProperties = () -> IteratorUtils.flatMap(vertices.get(),
				v -> IteratorUtils.cast(v.properties()));

		Iterator it;
		final Class<E> requested = type.orElse(null);
		if (requested != null && Edge.class.isAssignableFrom(requested)) {
			it = edges.get();
		} else if (requested != null && Vertex.class.isAssignableFrom(requested)) {
			it = vertices.get();
		} else if (requested != null && VertexProperty.class.isAssignableFrom(requested)) {
			it = vertexProperties.get();
		} else {
			it = IteratorUtils.concat(vertices.get(), edges.get(), vertexProperties.get());
		}

		final Pattern pattern = Pattern.compile(regex);

		// get properties
		it = IteratorUtils.<Element, Property>flatMap(it, e -> IteratorUtils.cast(e.properties()));
		// filter by regex (properties without a value are skipped)
		it = IteratorUtils.<Property>filter(it,
				p -> p.isPresent() && p.value() != null && pattern.matcher(p.value().toString()).matches());

		return it;
	}

	/**
	 * Search for {@link Property}s attached to any {@link Element} using the
	 * supplied regex. This is a basic scan+filter operation, not a full text search
	 * against an index.
	 */
	public static Iterator<Property> search(final OpcGraph graph, final String regex) {
		return search(graph, regex, Optional.empty());
	}

	private OpcHelper() {
		throw new IllegalStateException("Utility class");
	}

}
