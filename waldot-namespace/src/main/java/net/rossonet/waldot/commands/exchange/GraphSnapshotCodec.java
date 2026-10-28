package net.rossonet.waldot.commands.exchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONReader;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3;

/**
 * Serialization of a WaldOT graph as typed GraphSON v3 in adjacency list
 * format (one vertex with its edges per line) and restore of such a snapshot.
 * <p>
 * The restore does not use {@code GraphReader.readGraph()}: TinkerPop creates
 * the elements first and adds the properties later, while WaldOT needs the
 * reserved keys ({@code type}, {@code directory}, {@code name}, ...) at
 * creation time to choose the plugin vertex types and the active edges
 * ({@code link-to}, {@code fire}, ...). Here every vertex and every edge is
 * created with all its properties in a single {@code addVertex}/{@code addEdge}
 * call, the vertices first and then the edges.
 * </p>
 */
public final class GraphSnapshotCodec {

	/** Result of an export. */
	public record ExportResult(byte[] data, int vertices, int edges) {
	}

	/** Result of an import. */
	public record ImportResult(int vertices, int edges, List<String> errors) {
	}

	private record EdgeData(Object id, String label, Object outId, Object inId, Map<String, Object> properties) {
	}

	private record VertexData(Object id, String label, Map<String, Object> properties) {
	}

	private static final Logger logger = LoggerFactory.getLogger(GraphSnapshotCodec.class);

	private static final int MAX_REPORTED_ERRORS = 20;

	private static final GraphSONMapper MAPPER = GraphSONMapper.build().version(GraphSONVersion.V3_0)
			.addRegistry(OpcIoRegistryV3.instance()).create();

	/** Compresses with gzip. */
	public static byte[] compress(final byte[] data) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, data.length / 4));
		try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
			gzip.write(data);
		}
		return out.toByteArray();
	}

	/**
	 * Decompresses the data when it starts with the gzip magic number,
	 * otherwise returns it as is.
	 */
	public static byte[] decompressIfNeeded(final byte[] data) throws IOException {
		if (!isGzip(data)) {
			return data;
		}
		try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
			return in.readAllBytes();
		}
	}

	private static String errorOf(final Object id, final Throwable e) {
		return id + ": " + (e.getMessage() == null ? e.toString() : e.getMessage());
	}

	/**
	 * Writes the graph as typed GraphSON v3, one vertex (with its in and out
	 * edges) per line. The command vertices are never exported, the ephemeral
	 * vertices only when requested.
	 */
	public static ExportResult exportGraph(final WaldotGraph graph, final boolean includeEphemeral)
			throws IOException {
		final List<Vertex> vertices = new ArrayList<>();
		graph.vertices().forEachRemaining(v -> {
			if (isExportable(v, includeEphemeral)) {
				vertices.add(v);
			}
		});
		final AtomicInteger edges = new AtomicInteger();
		for (final Vertex v : vertices) {
			v.edges(Direction.OUT).forEachRemaining(e -> {
				if (isExportable(e.inVertex(), includeEphemeral)) {
					edges.incrementAndGet();
				}
			});
		}
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		writer().writeVertices(nonClosing(out), vertices.iterator(), Direction.BOTH);
		return new ExportResult(out.toByteArray(), vertices.size(), edges.get());
	}

	private static Object firstValue(final Iterator<? extends Property<Object>> properties) {
		return properties.hasNext() ? properties.next().value() : null;
	}

	/**
	 * Reads a snapshot written by {@link #exportGraph} (optionally gzipped) and
	 * creates its elements in the graph.
	 *
	 * @param clearBefore removes every vertex and edge of the graph before the
	 *                    import
	 */
	public static ImportResult importGraph(final WaldotGraph graph, final byte[] snapshot, final boolean clearBefore)
			throws IOException {
		final byte[] data = decompressIfNeeded(snapshot);
		final List<VertexData> vertices = new ArrayList<>();
		final List<EdgeData> edges = new ArrayList<>();
		final Iterator<Vertex> read = reader().readVertices(new ByteArrayInputStream(data), attachable -> {
			vertices.add(toVertexData(attachable.get()));
			return null;
		}, attachable -> {
			edges.add(toEdgeData(attachable.get()));
			return null;
		}, Direction.OUT);
		while (read.hasNext()) {
			read.next();
		}
		if (clearBefore) {
			graph.getWaldotNamespace().resetNameSpace();
		}
		final List<String> errors = new ArrayList<>();
		int createdVertices = 0;
		for (final VertexData vertex : vertices) {
			try {
				graph.addVertex(keyValues(vertex.properties(), T.id, vertex.id(), T.label, vertex.label()));
				createdVertices++;
			} catch (final RuntimeException e) {
				logger.warn("vertex {} not restored: {}", vertex.id(), e.getMessage());
				report(errors, errorOf(vertex.id(), e));
			}
		}
		int createdEdges = 0;
		for (final EdgeData edge : edges) {
			try {
				final Vertex out = vertex(graph, edge.outId());
				final Vertex in = vertex(graph, edge.inId());
				out.addEdge(edge.label(), in, keyValues(edge.properties(), T.id, edge.id()));
				createdEdges++;
			} catch (final RuntimeException e) {
				logger.warn("edge {} not restored: {}", edge.id(), e.getMessage());
				report(errors, errorOf(edge.id(), e));
			}
		}
		return new ImportResult(createdVertices, createdEdges, Collections.unmodifiableList(errors));
	}

	private static boolean isEphemeral(final Vertex vertex) {
		try {
			final VertexProperty<Object> property = vertex.property(MiloStrategy.EPHEMERAL_PARAMETER);
			return property.isPresent() && Boolean.parseBoolean(String.valueOf(property.value()));
		} catch (final RuntimeException e) {
			return false;
		}
	}

	private static boolean isExportable(final Vertex vertex, final boolean includeEphemeral) {
		return vertex != null && !(vertex instanceof WaldotCommand) && (includeEphemeral || !isEphemeral(vertex));
	}

	/** @return true if the data starts with the gzip magic number */
	public static boolean isGzip(final byte[] data) {
		return data != null && data.length > 2 && (data[0] & 0xff) == 0x1f && (data[1] & 0xff) == 0x8b;
	}

	private static Object[] keyValues(final Map<String, Object> properties, final Object... head) {
		final List<Object> keyValues = new ArrayList<>(head.length + properties.size() * 2);
		Collections.addAll(keyValues, head);
		for (final Map.Entry<String, Object> property : properties.entrySet()) {
			// the head (T.id, T.label) comes first, so it wins over the
			// "id"/"label" properties that are restored as plain properties
			if (property.getValue() == null) {
				continue;
			}
			keyValues.add(property.getKey());
			keyValues.add(property.getValue());
		}
		return keyValues.toArray();
	}

	/**
	 * The writer must not close the target stream: the result is read after
	 * the write.
	 */
	private static OutputStream nonClosing(final OutputStream out) {
		return new OutputStream() {
			@Override
			public void close() throws IOException {
				out.flush();
			}

			@Override
			public void flush() throws IOException {
				out.flush();
			}

			@Override
			public void write(final byte[] b, final int off, final int len) throws IOException {
				out.write(b, off, len);
			}

			@Override
			public void write(final int b) throws IOException {
				out.write(b);
			}
		};
	}

	/**
	 * Normalizes the result of a JEXL expression for the serialization:
	 * traversals and iterators are drained into lists.
	 */
	public static Object normalizeResult(final Object result) {
		if (result instanceof Traversal<?, ?>) {
			return ((Traversal<?, ?>) result).toList();
		}
		if (result instanceof Iterator<?>) {
			final List<Object> list = new ArrayList<>();
			((Iterator<?>) result).forEachRemaining(list::add);
			return list;
		}
		return result;
	}

	private static GraphSONReader reader() {
		return GraphSONReader.build().mapper(MAPPER).create();
	}

	private static void report(final List<String> errors, final String error) {
		if (errors.size() < MAX_REPORTED_ERRORS) {
			errors.add(error);
		}
	}

	/** Serializes any value (also elements and traversal results) as typed GraphSON v3. */
	public static String toGraphSon(final Object value) throws IOException {
		return MAPPER.createMapper().writeValueAsString(normalizeResult(value));
	}

	private static EdgeData toEdgeData(final Edge edge) {
		final Map<String, Object> properties = new LinkedHashMap<>();
		edge.properties().forEachRemaining(p -> properties.put(p.key(), p.value()));
		return new EdgeData(edge.id(), edge.label(), edge.outVertex().id(), edge.inVertex().id(), properties);
	}

	private static VertexData toVertexData(final Vertex vertex) {
		final Map<String, Object> properties = new LinkedHashMap<>();
		vertex.keys().forEach(key -> properties.put(key, firstValue(vertex.properties(key))));
		return new VertexData(vertex.id(), vertex.label(), properties);
	}

	private static Vertex vertex(final WaldotGraph graph, final Object id) {
		final Iterator<Vertex> found = graph.vertices(id);
		if (!found.hasNext()) {
			throw new NoSuchElementException("vertex " + id + " not found");
		}
		return found.next();
	}

	private static GraphSONWriter writer() {
		return GraphSONWriter.build().mapper(MAPPER).wrapAdjacencyList(false).create();
	}

	private GraphSnapshotCodec() {
	}
}
