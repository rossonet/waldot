package net.rossonet.waldot.api.changes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import net.rossonet.waldot.api.changes.GraphChangeRecorder.Change;

/**
 * Summarises the changes of a {@link GraphChangeRecorder.Recording} as a diff:
 * vertices and edges created and removed, properties modified (first old value,
 * last new value), grouped by type. Only the changes of the script thread make
 * the diff; the others are counted in {@code async}.
 *
 * <p>
 * The properties of the elements created by the script are not listed as
 * modified, and the runtime properties ({@code run!...}), written by the plugins,
 * are only counted.
 * </p>
 */
public final class GraphChangeDiff {

	/** Prefix of the runtime properties written by the plugins. */
	public static final String RUNTIME_PREFIX = "run!";

	private static final String NO_TYPE = "vertex";

	private static void count(final Map<String, Map<String, Integer>> byType, final String type,
			final String what) {
		byType.computeIfAbsent(type == null ? NO_TYPE : type, k -> new TreeMap<>()).merge(what, 1, Integer::sum);
	}

	/**
	 * Builds the diff.
	 *
	 * @param changes   the recorded changes, in order
	 * @param truncated true if the recording lost changes
	 * @return the diff as nested maps and lists (ready for JSON)
	 */
	public static Map<String, Object> diff(final List<Change> changes, final boolean truncated) {
		final Map<String, Element> refs = new LinkedHashMap<>();
		for (final Change c : changes) {
			if (c.ref() != null) {
				refs.putIfAbsent(c.target() + "\u0000" + c.element(), c.ref());
			}
		}
		final Function<String, Vertex> vertexResolver = id -> refs.get("vertex\u0000" + id) instanceof final Vertex v
				? v
				: null;
		final Function<String, Edge> edgeResolver = id -> refs.get("edge\u0000" + id) instanceof final Edge e ? e
				: null;
		final Set<String> createdVertices = new LinkedHashSet<>();
		final Set<String> createdEdges = new LinkedHashSet<>();
		// created by the script, even if removed later: their properties are not "modified"
		final Set<String> everCreated = new LinkedHashSet<>();
		final Map<String, Map<String, Object>> removedVertices = new LinkedHashMap<>();
		final Map<String, Map<String, Object>> removedEdges = new LinkedHashMap<>();
		final Map<String, Object[]> properties = new LinkedHashMap<>();
		final Map<String, String[]> propertyKeys = new LinkedHashMap<>();
		int runtimeChanges = 0;
		final Map<String, Integer> async = new TreeMap<>();
		for (final Change c : changes) {
			if (!GraphChangeRecorder.ORIGIN_SCRIPT.equals(c.origin())) {
				async.merge(c.kind(), 1, Integer::sum);
				continue;
			}
			switch (c.kind()) {
			case "vertex-added" -> {
				createdVertices.add(c.element());
				everCreated.add("vertex\u0000" + c.element());
			}
			case "edge-added" -> {
				createdEdges.add(c.element());
				everCreated.add("edge\u0000" + c.element());
			}
			case "vertex-removed" -> {
				if (!createdVertices.remove(c.element())) {
					removedVertices.put(c.element(), c.snapshot());
				}
				// the edges of the vertex go with it
				final Object edges = c.snapshot() == null ? null : c.snapshot().get("edges");
				if (edges instanceof final List<?> list) {
					for (final Object e : list) {
						if (e instanceof final Map<?, ?> m) {
							createdEdges.remove(String.valueOf(m.get("id")));
						}
					}
				}
			}
			case "edge-removed" -> {
				if (!createdEdges.remove(c.element())) {
					removedEdges.put(c.element(), c.snapshot());
				}
			}
			case "property" -> {
				if (c.key() != null && c.key().startsWith(RUNTIME_PREFIX)) {
					runtimeChanges++;
					continue;
				}
				final String key = c.target() + "\u0000" + c.element() + "\u0000" + c.key();
				final Object[] values = properties.get(key);
				if (values == null) {
					properties.put(key, new Object[] { c.oldValue(), c.newValue() });
					propertyKeys.put(key, new String[] { c.target(), c.element(), c.key() });
				} else {
					values[1] = c.newValue();
				}
			}
			default -> async.merge("unknown", 1, Integer::sum);
			}
		}
		final Map<String, Map<String, Integer>> byType = new TreeMap<>();
		final List<Map<String, Object>> vertexCreated = new ArrayList<>();
		for (final String id : createdVertices) {
			final Map<String, Object> d = describeCreated(id, vertexResolver.apply(id));
			vertexCreated.add(d);
			count(byType, (String) d.get("type"), "created");
		}
		final List<Map<String, Object>> vertexRemoved = new ArrayList<>();
		for (final Map.Entry<String, Map<String, Object>> e : removedVertices.entrySet()) {
			final Map<String, Object> snapshot = e.getValue() == null ? Map.of("id", e.getKey()) : e.getValue();
			vertexRemoved.add(snapshot);
			count(byType, typeOf(snapshot), "removed");
		}
		final List<Map<String, Object>> edgeCreated = new ArrayList<>();
		for (final String id : createdEdges) {
			edgeCreated.add(describeCreatedEdge(id, edgeResolver.apply(id)));
		}
		final List<Map<String, Object>> edgeRemoved = new ArrayList<>();
		for (final Map.Entry<String, Map<String, Object>> e : removedEdges.entrySet()) {
			edgeRemoved.add(e.getValue() == null ? Map.of("id", e.getKey()) : e.getValue());
		}
		final List<Map<String, Object>> modified = new ArrayList<>();
		for (final Map.Entry<String, Object[]> e : properties.entrySet()) {
			final String[] k = propertyKeys.get(e.getKey());
			final String target = k[0];
			final String element = k[1];
			if (everCreated.contains(target + "\u0000" + element)
					|| "vertex".equals(target) && removedVertices.containsKey(element)
					|| "edge".equals(target) && removedEdges.containsKey(element)) {
				continue;
			}
			final Object oldValue = e.getValue()[0];
			final Object newValue = e.getValue()[1];
			if (Objects.equals(String.valueOf(oldValue), String.valueOf(newValue))) {
				continue;
			}
			final Map<String, Object> m = new LinkedHashMap<>();
			m.put("target", target);
			m.put("element", element);
			m.put("key", k[2]);
			m.put("old", oldValue);
			m.put("new", newValue);
			modified.add(m);
			if ("vertex".equals(target)) {
				final Vertex v = vertexResolver.apply(element);
				count(byType, v == null ? null : stringProperty(v, "type"), "modified");
			}
		}
		final Map<String, Object> vertices = new LinkedHashMap<>();
		vertices.put("created", vertexCreated);
		vertices.put("removed", vertexRemoved);
		final Map<String, Object> edges = new LinkedHashMap<>();
		edges.put("created", edgeCreated);
		edges.put("removed", edgeRemoved);
		final Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("vertices_created", vertexCreated.size());
		summary.put("vertices_removed", vertexRemoved.size());
		summary.put("edges_created", edgeCreated.size());
		summary.put("edges_removed", edgeRemoved.size());
		summary.put("properties_modified", modified.size());
		summary.put("runtime_changes", runtimeChanges);
		final Map<String, Object> diff = new LinkedHashMap<>();
		diff.put("summary", summary);
		diff.put("by_type", byType);
		diff.put("vertices", vertices);
		diff.put("edges", edges);
		diff.put("properties", modified);
		diff.put("async", async);
		diff.put("truncated", truncated);
		return diff;
	}

	private static Map<String, Object> describeCreated(final String id, final Vertex vertex) {
		final Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", id);
		if (vertex != null) {
			m.put("label", safeLabel(vertex));
			m.put("type", stringProperty(vertex, "type"));
			final String directory = stringProperty(vertex, "directory");
			if (directory != null) {
				m.put("directory", directory);
			}
		}
		return m;
	}

	private static Map<String, Object> describeCreatedEdge(final String id, final Edge edge) {
		final Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", id);
		if (edge != null) {
			m.put("label", safeLabel(edge));
			final String type = stringProperty(edge, "type");
			if (type != null) {
				m.put("type", type);
			}
			try {
				m.put("from", GraphChangeRecorder.id(edge.outVertex()));
				m.put("to", GraphChangeRecorder.id(edge.inVertex()));
			} catch (final RuntimeException e) {
				// endpoints not available
			}
		}
		return m;
	}

	private static String safeLabel(final Element element) {
		try {
			return element.label();
		} catch (final RuntimeException e) {
			return null;
		}
	}

	private static String stringProperty(final Element element, final String key) {
		try {
			final Property<Object> p = element.property(key);
			return p.isPresent() && p.value() != null ? String.valueOf(p.value()) : null;
		} catch (final RuntimeException e) {
			return null;
		}
	}

	private static String typeOf(final Map<String, Object> snapshot) {
		final Object props = snapshot.get("properties");
		if (props instanceof final Map<?, ?> m && m.get("type") != null) {
			return String.valueOf(m.get("type"));
		}
		return null;
	}

	private GraphChangeDiff() {
	}
}
