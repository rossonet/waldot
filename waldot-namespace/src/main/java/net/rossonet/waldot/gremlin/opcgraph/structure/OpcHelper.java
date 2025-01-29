
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

	/**
	 * Allows direct access to a OpcGraph's storage which can be helpful for
	 * advanced use cases.
	 */
	public static Map<NodeId, WaldotEdge> getEdges(final OpcGraph graph) {
		return graph.getWaldotNamespace().getEdges();
	}

	/*
	 * public static Iterator<OpcEdge> getEdges(final OpcVertex vertex, final
	 * Direction direction, final String... edgeLabels) { final List<Edge> edges =
	 * new ArrayList<>(); if (direction.equals(Direction.OUT) ||
	 * direction.equals(Direction.BOTH)) { if (vertex.outEdges != null) { if
	 * (edgeLabels.length == 0) { vertex.outEdges.values().forEach(edges::addAll); }
	 * else if (edgeLabels.length == 1) {
	 * edges.addAll(vertex.outEdges.getOrDefault(edgeLabels[0],
	 * Collections.emptySet())); } else {
	 * Stream.of(edgeLabels).map(vertex.outEdges::get).filter(Objects::nonNull).
	 * forEach(edges::addAll); } } } if (direction.equals(Direction.IN) ||
	 * direction.equals(Direction.BOTH)) { if (vertex.inEdges != null) { if
	 * (edgeLabels.length == 0) { vertex.inEdges.values().forEach(edges::addAll); }
	 * else if (edgeLabels.length == 1) {
	 * edges.addAll(vertex.inEdges.getOrDefault(edgeLabels[0],
	 * Collections.emptySet())); } else {
	 * Stream.of(edgeLabels).map(vertex.inEdges::get).filter(Objects::nonNull).
	 * forEach(edges::addAll); } } } return (Iterator) edges.iterator(); }
	 * 
	 * public static Iterator<OpcEdge> getEdgesTx(final OpcVertex vertex, final
	 * Direction direction, final String... edgeLabels) { final List<Object>
	 * outEdgeIds = new ArrayList<>(); if (direction.equals(Direction.OUT) ||
	 * direction.equals(Direction.BOTH)) { if (vertex.outEdgesId != null) { if
	 * (edgeLabels.length == 0) {
	 * vertex.outEdgesId.values().forEach(outEdgeIds::addAll); } else if
	 * (edgeLabels.length == 1) {
	 * outEdgeIds.addAll(vertex.outEdgesId.getOrDefault(edgeLabels[0],
	 * Collections.emptySet())); } else {
	 * Stream.of(edgeLabels).map(vertex.outEdgesId::get).filter(Objects::nonNull)
	 * .forEach(outEdgeIds::addAll); } } }
	 * 
	 * final List<Object> inEdgeIds = new ArrayList<>(); if
	 * (direction.equals(Direction.IN) || direction.equals(Direction.BOTH)) { if
	 * (vertex.inEdgesId != null) { if (edgeLabels.length == 0) {
	 * vertex.inEdgesId.values().forEach(inEdgeIds::addAll); } else if
	 * (edgeLabels.length == 1) {
	 * inEdgeIds.addAll(vertex.inEdgesId.getOrDefault(edgeLabels[0],
	 * Collections.emptySet())); } else {
	 * Stream.of(edgeLabels).map(vertex.inEdgesId::get).filter(Objects::nonNull)
	 * .forEach(inEdgeIds::addAll); } } }
	 * 
	 * return outEdgeIds.size() == 0 && inEdgeIds.size() == 0 ?
	 * Collections.emptyIterator() : Stream.concat(outEdgeIds.stream(),
	 * inEdgeIds.stream()) .map(id -> ((AbstractOpcGraph)
	 * vertex.graph()).edge(id)).filter(v -> v != null) .map(v -> (OpcEdge)
	 * v).iterator(); }
	 * 
	 * public static OpcGraphComputerView getGraphComputerView(final
	 * AbstractOpcGraph graph) { return graph.getGraphComputerView(); }
	 * 
	 * public static Map<String, List<VertexProperty>> getProperties(final OpcVertex
	 * vertex) { return null == vertex.properties ? Collections.emptyMap() :
	 * vertex.properties; }
	 * 
	 * 
	 * public static Map<Object, Vertex> getVertices(final OpcGraph graph) { return
	 * graph.getOpcNamespace().getVertices(); }
	 * 
	 * public static Iterator<OpcVertex> getVertices(final OpcVertex vertex, final
	 * Direction direction, final String... edgeLabels) { final List<Vertex>
	 * vertices = new ArrayList<>(); if (direction.equals(Direction.OUT) ||
	 * direction.equals(Direction.BOTH)) { if (vertex.outEdges != null) { if
	 * (edgeLabels.length == 0) { vertex.outEdges.values() .forEach(set ->
	 * set.forEach(edge -> vertices.add(((OpcEdge) edge).inVertex))); } else if
	 * (edgeLabels.length == 1) { vertex.outEdges.getOrDefault(edgeLabels[0],
	 * Collections.emptySet()) .forEach(edge -> vertices.add(((OpcEdge)
	 * edge).inVertex)); } else {
	 * Stream.of(edgeLabels).map(vertex.outEdges::get).filter(Objects::nonNull).
	 * flatMap(Set::stream) .forEach(edge -> vertices.add(((OpcEdge)
	 * edge).inVertex)); } } } if (direction.equals(Direction.IN) ||
	 * direction.equals(Direction.BOTH)) { if (vertex.inEdges != null) { if
	 * (edgeLabels.length == 0) { vertex.inEdges.values() .forEach(set ->
	 * set.forEach(edge -> vertices.add(((OpcEdge) edge).outVertex))); } else if
	 * (edgeLabels.length == 1) { vertex.inEdges.getOrDefault(edgeLabels[0],
	 * Collections.emptySet()) .forEach(edge -> vertices.add(((OpcEdge)
	 * edge).outVertex)); } else {
	 * Stream.of(edgeLabels).map(vertex.inEdges::get).filter(Objects::nonNull).
	 * flatMap(Set::stream) .forEach(edge -> vertices.add(((OpcEdge)
	 * edge).outVertex)); } } } return (Iterator) vertices.iterator(); }
	 * 
	 * public static Iterator<OpcVertex> getVerticesTx(final OpcVertex vertex, final
	 * Direction direction, final String... edgeLabels) { final Set<Object>
	 * inEdgesIds = new HashSet<>(); if (direction.equals(Direction.OUT) ||
	 * direction.equals(Direction.BOTH)) { if (vertex.outEdgesId != null) { if
	 * (edgeLabels.length == 0) { vertex.outEdgesId.values().forEach(set ->
	 * set.forEach(edge -> inEdgesIds.add(edge))); } else if (edgeLabels.length ==
	 * 1) { vertex.outEdgesId.getOrDefault(edgeLabels[0], Collections.emptySet())
	 * .forEach(edge -> inEdgesIds.add(edge)); } else {
	 * Stream.of(edgeLabels).map(vertex.outEdgesId::get).filter(Objects::nonNull).
	 * flatMap(Set::stream) .forEach(edge -> inEdgesIds.add(edge)); } } } final
	 * Set<Object> outEdgesIds = new HashSet<>(); if (direction.equals(Direction.IN)
	 * || direction.equals(Direction.BOTH)) { if (vertex.inEdgesId != null) { if
	 * (edgeLabels.length == 0) { vertex.inEdgesId.values().forEach(set ->
	 * set.forEach(edge -> outEdgesIds.add(edge))); } else if (edgeLabels.length ==
	 * 1) { vertex.inEdgesId.getOrDefault(edgeLabels[0], Collections.emptySet())
	 * .forEach(edge -> outEdgesIds.add(edge)); } else {
	 * Stream.of(edgeLabels).map(vertex.inEdgesId::get).filter(Objects::nonNull).
	 * flatMap(Set::stream) .forEach(edge -> outEdgesIds.add(edge)); } } }
	 * 
	 * final List<Vertex> vertices = new ArrayList<>();if(inEdgesIds.size()!=0) {
	 * vertex.graph().edges(inEdgesIds.toArray()).forEachRemaining(edge ->
	 * vertices.add(edge.inVertex())); }if(outEdgesIds.size()!=0) {
	 * vertex.graph().edges(outEdgesIds.toArray()).forEachRemaining(edge ->
	 * vertices.add(edge.outVertex())); }
	 * 
	 * return
	 * vertices.size()==0?Collections.emptyIterator():vertices.stream().map(v->(
	 * OpcVertex)v).iterator(); }
	 * 
	 * public static boolean inComputerMode(final AbstractOpcGraph graph) { return
	 * null != graph.getGraphComputerView(); }
	 * 
	 */

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
	}

}
