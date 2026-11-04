package net.rossonet.waldot.api.changes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * Records the changes of the graph while a recording is open: vertices and
 * edges created and removed, properties changed. The namespace calls the
 * {@code on*} methods from its mutation entry points; they cost a volatile
 * read when nothing is recording.
 *
 * <p>
 * Every change carries its origin: {@code script} when it happens in the thread
 * that opened the recording, {@code async} otherwise (rules, plugins, OPC UA
 * clients working at the same time).
 * </p>
 */
public final class GraphChangeRecorder {

	/**
	 * A change of the graph.
	 *
	 * @param kind    {@code vertex-added}, {@code vertex-removed},
	 *                {@code edge-added}, {@code edge-removed},
	 *                {@code property}
	 * @param element id of the element (vertex or edge)
	 * @param target  {@code vertex} or {@code edge}
	 * @param key     property key, null for the other kinds
	 * @param oldValue value before the change (property), null if absent
	 * @param newValue value after the change (property), null when removed
	 * @param snapshot description of a removed element (properties and, for a
	 *                 vertex, its edges), null for the other kinds
	 * @param origin  {@code script} or {@code async}
	 * @param thread  name of the thread
	 * @param ref     the element itself (vertex or edge), to read its current
	 *                state
	 */
	public record Change(String kind, String element, String target, String key, Object oldValue, Object newValue,
			Map<String, Object> snapshot, String origin, String thread, Element ref) {
	}

	/** An open recording. Close it to stop recording. */
	public static final class Recording implements AutoCloseable {

		private final AtomicBoolean closed = new AtomicBoolean(false);
		private final ConcurrentLinkedQueue<Change> changes = new ConcurrentLinkedQueue<>();
		private final int maxChanges;
		private final Thread owner;
		private final AtomicInteger size = new AtomicInteger();
		private volatile boolean truncated = false;

		private Recording(final Thread owner, final int maxChanges) {
			this.owner = owner;
			this.maxChanges = maxChanges;
		}

		private void add(final String kind, final String element, final String target, final String key,
				final Object oldValue, final Object newValue, final Map<String, Object> snapshot, final Element ref) {
			if (closed.get()) {
				return;
			}
			if (size.incrementAndGet() > maxChanges) {
				truncated = true;
				return;
			}
			final Thread current = Thread.currentThread();
			changes.add(new Change(kind, element, target, key, oldValue, newValue, snapshot,
					current == owner ? ORIGIN_SCRIPT : ORIGIN_ASYNC, current.getName(), ref));
		}

		/** The changes recorded, in order. */
		public List<Change> changes() {
			return List.copyOf(changes);
		}

		@Override
		public void close() {
			if (closed.compareAndSet(false, true)) {
				RECORDINGS.remove(this);
			}
		}

		/** True after {@link #close()}. */
		public boolean isClosed() {
			return closed.get();
		}

		/** True if more than the maximum number of changes happened. */
		public boolean isTruncated() {
			return truncated;
		}
	}

	/** Default maximum number of changes kept by a recording. */
	public static final int DEFAULT_MAX_CHANGES = 200_000;

	public static final String ORIGIN_ASYNC = "async";

	public static final String ORIGIN_SCRIPT = "script";

	private static final CopyOnWriteArrayList<Recording> RECORDINGS = new CopyOnWriteArrayList<>();

	/** Number of open recordings (for the tests and the diagnostics). */
	public static int activeRecordings() {
		return RECORDINGS.size();
	}

	/**
	 * Describes an edge: id, label, type, endpoints and properties.
	 */
	public static Map<String, Object> describeEdge(final Edge edge) {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", id(edge));
		map.put("label", safeLabel(edge));
		map.put("from", id(edge.outVertex()));
		map.put("to", id(edge.inVertex()));
		map.put("properties", properties(edge));
		return map;
	}

	/**
	 * Describes a vertex: id, label, properties and edges (in both directions).
	 */
	public static Map<String, Object> describeVertex(final Vertex vertex) {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("id", id(vertex));
		map.put("label", safeLabel(vertex));
		map.put("properties", properties(vertex));
		final List<Map<String, Object>> edges = new ArrayList<>();
		try {
			final Iterator<Edge> it = vertex.edges(Direction.BOTH);
			while (it.hasNext()) {
				edges.add(describeEdge(it.next()));
			}
		} catch (final RuntimeException e) {
			// a vertex being removed can fail to list its edges: the description stays partial
		}
		map.put("edges", edges);
		return map;
	}

	/**
	 * Id of an element as text (the identifier of the NodeId).
	 */
	public static String id(final Element element) {
		if (element == null) {
			return null;
		}
		final Object id = element.id();
		if (id instanceof final NodeId nodeId) {
			return String.valueOf(nodeId.getIdentifier());
		}
		return String.valueOf(id);
	}

	/** True if at least one recording is open. */
	public static boolean isRecording() {
		return !RECORDINGS.isEmpty();
	}

	/** An edge has been created. */
	public static void onEdgeAdded(final Edge edge) {
		if (RECORDINGS.isEmpty() || edge == null) {
			return;
		}
		final String id = id(edge);
		for (final Recording r : RECORDINGS) {
			r.add("edge-added", id, "edge", null, null, null, null, edge);
		}
	}

	/** An edge is going to be removed (call before the removal). */
	public static void onEdgeRemoving(final Edge edge) {
		if (RECORDINGS.isEmpty() || edge == null) {
			return;
		}
		final Map<String, Object> snapshot = describeEdge(edge);
		final String id = id(edge);
		for (final Recording r : RECORDINGS) {
			r.add("edge-removed", id, "edge", null, null, null, snapshot, edge);
		}
	}

	/** A property of a vertex or of an edge is going to change. */
	public static void onPropertyChanging(final Element element, final String key, final Object oldValue,
			final Object newValue) {
		if (RECORDINGS.isEmpty() || element == null || key == null) {
			return;
		}
		final String id = id(element);
		final String target = element instanceof Edge ? "edge" : "vertex";
		for (final Recording r : RECORDINGS) {
			r.add("property", id, target, key, plain(oldValue), plain(newValue), null, element);
		}
	}

	/** A vertex has been created. */
	public static void onVertexAdded(final Vertex vertex) {
		if (RECORDINGS.isEmpty() || vertex == null) {
			return;
		}
		final String id = id(vertex);
		for (final Recording r : RECORDINGS) {
			r.add("vertex-added", id, "vertex", null, null, null, null, vertex);
		}
	}

	/** A vertex is going to be removed (call before the removal). */
	public static void onVertexRemoving(final Vertex vertex) {
		if (RECORDINGS.isEmpty() || vertex == null) {
			return;
		}
		final Map<String, Object> snapshot = describeVertex(vertex);
		final String id = id(vertex);
		for (final Recording r : RECORDINGS) {
			r.add("vertex-removed", id, "vertex", null, null, null, snapshot, vertex);
		}
	}

	/**
	 * Value usable in JSON: numbers, booleans and strings as they are, the rest
	 * as text.
	 */
	public static Object plain(final Object value) {
		if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
			return value;
		}
		return String.valueOf(value);
	}

	private static Map<String, Object> properties(final Element element) {
		final Map<String, Object> map = new LinkedHashMap<>();
		try {
			final Iterator<? extends Property<Object>> it = element.properties();
			while (it.hasNext()) {
				final Property<Object> p = it.next();
				if (p.isPresent()) {
					map.put(p.key(), plain(p.value()));
				}
			}
		} catch (final RuntimeException e) {
			// partial description
		}
		return map;
	}

	private static String safeLabel(final Element element) {
		try {
			return element.label();
		} catch (final RuntimeException e) {
			return null;
		}
	}

	/**
	 * Opens a recording for the current thread.
	 *
	 * @return the recording, to close when done
	 */
	public static Recording start() {
		return start(DEFAULT_MAX_CHANGES);
	}

	/**
	 * Opens a recording for the current thread with a limit on the changes kept.
	 */
	public static Recording start(final int maxChanges) {
		final Recording recording = new Recording(Thread.currentThread(), maxChanges);
		RECORDINGS.add(Objects.requireNonNull(recording));
		return recording;
	}

	private GraphChangeRecorder() {
	}
}
