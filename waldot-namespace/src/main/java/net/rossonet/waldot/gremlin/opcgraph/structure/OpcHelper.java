
package net.rossonet.waldot.gremlin.opcgraph.structure;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
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
		if (!type.isPresent()) {
			it = IteratorUtils.concat(vertices.get(), edges.get(), vertexProperties.get());
		} else {
			switch (type.get().getSimpleName()) {
			case "Edge":
				it = edges.get();
				break;
			case "Vertex":
				it = vertices.get();
				break;
			case "VertexProperty":
				it = vertexProperties.get();
				break;
			default:
				it = IteratorUtils.concat(vertices.get(), edges.get(), vertexProperties.get());
			}
		}

		final Pattern pattern = Pattern.compile(regex);

		// get properties
		it = IteratorUtils.<Element, Property>flatMap(it, e -> IteratorUtils.cast(e.properties()));
		// filter by regex
		it = IteratorUtils.<Property>filter(it, p -> pattern.matcher(p.value().toString()).matches());

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
