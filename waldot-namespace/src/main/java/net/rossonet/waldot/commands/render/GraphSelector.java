package net.rossonet.waldot.commands.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.api.exchange.GraphRenderOptions;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotElement;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;

/**
 * Applies the selection of {@link GraphRenderOptions} to the graph of a
 * namespace and builds the {@link RenderModel}. The command vertices are never
 * drawn; only the edges between drawn vertices are drawn.
 */
public final class GraphSelector {

	/** Categorical palette (Tableau 10), assigned to the sorted color keys. */
	static final String[] PALETTE = { "#4e79a7", "#f28e2b", "#e15759", "#76b7b2", "#59a14f", "#edc948", "#b07aa1",
			"#ff9da7", "#9c755f", "#bab0ac" };

	/** Keys shown in the header of a vertex (or meaningless in the box). */
	static final Set<String> RESERVED_KEYS = Set.of(MiloStrategy.ID_PARAMETER,
			MiloStrategy.LABEL_FIELD.toLowerCase(Locale.ROOT), MiloStrategy.TYPE_FIELD.toLowerCase(Locale.ROOT),
			MiloStrategy.DIRECTORY_PARAMETER, MiloStrategy.NAME_FIELD.toLowerCase(Locale.ROOT),
			MiloStrategy.EPHEMERAL_PARAMETER, MiloStrategy.DESCRIPTION_PARAMETER);

	private static final int TOOLTIP_VALUE_LENGTH = 200;

	private static final String TYPE_KEY = MiloStrategy.TYPE_FIELD.toLowerCase(Locale.ROOT);

	private static boolean anyIgnoreCase(final List<String> values, final String value) {
		for (final String candidate : values) {
			if (candidate.equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}

	static Pattern glob(final String glob) {
		final StringBuilder regex = new StringBuilder();
		for (final char c : glob.toCharArray()) {
			switch (c) {
			case '*' -> regex.append(".*");
			case '?' -> regex.append('.');
			default -> regex.append(Pattern.quote(String.valueOf(c)));
			}
		}
		return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
	}

	private static boolean isRuntimeKey(final String key) {
		return key.toLowerCase(Locale.ROOT).startsWith(MiloStrategy.RUNTIME_DATA_PREFIX);
	}

	static String nodeId(final Object id) {
		return id instanceof NodeId ? ((NodeId) id).toParseableString() : String.valueOf(id);
	}

	private static String path(final String directory) {
		String result = directory == null ? "" : directory.trim();
		while (result.startsWith(MiloStrategy.DIRECTORY_SPLIT_SIMBOL)) {
			result = result.substring(1);
		}
		while (result.endsWith(MiloStrategy.DIRECTORY_SPLIT_SIMBOL)) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	static String stringProperty(final Element element, final String key) {
		try {
			final Property<Object> property = element.property(key);
			return property.isPresent() && property.value() != null ? valueText(property.value()) : null;
		} catch (final RuntimeException e) {
			return null;
		}
	}

	static String truncate(final String value, final int max) {
		return value.length() <= max ? value : value.substring(0, max - 1) + "…";
	}

	static String valueText(final Object value) {
		final String text;
		if (value == null) {
			text = "null";
		} else if (value instanceof NodeId) {
			text = ((NodeId) value).toParseableString();
		} else if (value instanceof Object[]) {
			text = Arrays.deepToString((Object[]) value);
		} else if (value.getClass().isArray()) {
			text = arrayText(value);
		} else {
			text = String.valueOf(value);
		}
		final StringBuilder clean = new StringBuilder(text.length());
		for (final char c : text.toCharArray()) {
			clean.append(Character.isISOControl(c) ? ' ' : c);
		}
		return clean.toString();
	}

	private static String arrayText(final Object array) {
		final int length = java.lang.reflect.Array.getLength(array);
		final List<String> items = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			items.add(String.valueOf(java.lang.reflect.Array.get(array, i)));
		}
		return items.toString();
	}

	private final Set<String> highlight = new HashSet<>();

	private final WaldotNamespace namespace;

	private final GraphRenderOptions options;

	private final List<Pattern> edgeKeys = new ArrayList<>();

	private final Pattern match;

	private final List<Pattern> vertexKeys = new ArrayList<>();

	private final List<String> warnings = new ArrayList<>();

	private final Map<String, Pattern> where = new LinkedHashMap<>();

	public GraphSelector(final WaldotNamespace namespace, final GraphRenderOptions options) {
		this.namespace = namespace;
		this.options = options;
		options.properties().vertex().forEach(g -> vertexKeys.add(glob(g)));
		options.properties().edge().forEach(g -> edgeKeys.add(glob(g)));
		match = options.vertices().match() == null ? null : Pattern.compile(options.vertices().match());
		options.vertices().where().forEach((k, v) -> where.put(k, Pattern.compile(v)));
	}

	private boolean acceptedEdge(final Edge edge) {
		final GraphRenderOptions.Edges filter = options.edges();
		if (!filter.labels().isEmpty() && !anyIgnoreCase(filter.labels(), edge.label())) {
			return false;
		}
		if (anyIgnoreCase(filter.excludeLabels(), edge.label())) {
			return false;
		}
		if (!filter.types().isEmpty()) {
			final String type = stringProperty(edge, TYPE_KEY);
			if (type == null || !anyIgnoreCase(filter.types(), type)) {
				return false;
			}
		}
		return options.vertices().includeEphemeral() || !isEphemeral(edge);
	}

	/** Attribute filters of the vertices (not applied to the roots). */
	private boolean acceptedVertex(final Vertex vertex, final Set<Object> candidates) {
		if (!isDrawable(vertex) || candidates != null && !candidates.contains(vertex.id())) {
			return false;
		}
		final GraphRenderOptions.Vertices filter = options.vertices();
		if (!filter.includeEphemeral() && isEphemeral(vertex)) {
			return false;
		}
		final String type = stringProperty(vertex, TYPE_KEY);
		if (!filter.types().isEmpty() && (type == null || !anyIgnoreCase(filter.types(), type))) {
			return false;
		}
		if (type != null && anyIgnoreCase(filter.excludeTypes(), type)) {
			return false;
		}
		if (!filter.labels().isEmpty() && !anyIgnoreCase(filter.labels(), vertex.label())) {
			return false;
		}
		if (!filter.directories().isEmpty()) {
			final String directory = path(stringProperty(vertex, MiloStrategy.DIRECTORY_PARAMETER));
			boolean found = false;
			for (final String prefix : filter.directories()) {
				final String wanted = path(prefix);
				if (directory.equals(wanted) || directory.startsWith(wanted + MiloStrategy.DIRECTORY_SPLIT_SIMBOL)
						|| wanted.isEmpty()) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		if (match != null) {
			final String name = stringProperty(vertex, "name");
			if (!match.matcher(nodeId(vertex.id())).find() && (name == null || !match.matcher(name).find())
					&& !match.matcher(displayName(vertex)).find()) {
				return false;
			}
		}
		for (final Map.Entry<String, Pattern> condition : where.entrySet()) {
			final String value = stringProperty(vertex, condition.getKey());
			if (value == null || !condition.getValue().matcher(value).find()) {
				return false;
			}
		}
		return true;
	}

	private String colorKey(final Vertex vertex) {
		return switch (options.style().colorBy()) {
		case TYPE -> typeOrLabel(vertex);
		case LABEL -> vertex.label();
		case DIRECTORY -> emptyToNull(path(stringProperty(vertex, MiloStrategy.DIRECTORY_PARAMETER)));
		case NONE -> null;
		};
	}

	/** @return the 'type' property, or the label when missing */
	private static String typeOrLabel(final Vertex vertex) {
		final String type = stringProperty(vertex, TYPE_KEY);
		return type == null ? vertex.label() : type;
	}

	private static String emptyToNull(final String value) {
		return value == null || value.isEmpty() ? null : value;
	}

	private String displayName(final Element element) {
		if (element instanceof WaldotElement) {
			final WaldotElement waldot = (WaldotElement) element;
			if (waldot.getDisplayName() != null && waldot.getDisplayName().getText() != null
					&& !waldot.getDisplayName().getText().isBlank()) {
				return waldot.getDisplayName().getText();
			}
		}
		final String name = stringProperty(element, "name");
		return name != null ? name : nodeId(element.id());
	}

	private Set<Object> expressionVertices() {
		final Object result;
		try {
			result = namespace.runExpression(options.vertices().expression());
		} catch (final RuntimeException e) {
			throw new IllegalArgumentException("vertices.expression failed: "
					+ (e.getMessage() == null ? e.toString() : e.getMessage()), e);
		}
		final Set<Object> ids = new HashSet<>();
		collect(result, ids);
		return ids;
	}

	private void collect(final Object value, final Set<Object> ids) {
		if (value instanceof Vertex) {
			ids.add(((Vertex) value).id());
		} else if (value instanceof Edge) {
			ids.add(((Edge) value).outVertex().id());
			ids.add(((Edge) value).inVertex().id());
		} else if (value instanceof Iterable<?>) {
			((Iterable<?>) value).forEach(v -> collect(v, ids));
		} else if (value instanceof Iterator<?>) {
			((Iterator<?>) value).forEachRemaining(v -> collect(v, ids));
		} else if (value instanceof Object[]) {
			for (final Object v : (Object[]) value) {
				collect(v, ids);
			}
		}
	}

	private boolean isDrawable(final Vertex vertex) {
		return vertex != null && !(vertex instanceof WaldotCommand);
	}

	private boolean isEphemeral(final Element element) {
		return Boolean.parseBoolean(stringProperty(element, MiloStrategy.EPHEMERAL_PARAMETER));
	}

	private boolean isHighlighted(final Element element) {
		return highlight.contains(nodeId(element.id()));
	}

	private List<RenderModel.Entry> properties(final Element element, final List<Pattern> shownKeys,
			final boolean tooltip) {
		final GraphRenderOptions.Properties settings = options.properties();
		final Map<String, String> values = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		try {
			element.properties().forEachRemaining(p -> {
				if (p.isPresent() && (settings.includeRuntime() || !isRuntimeKey(p.key()))) {
					values.putIfAbsent(p.key(), valueText(p.value()));
				}
			});
		} catch (final RuntimeException e) {
			warnings.add("properties of " + nodeId(element.id()) + " not readable: " + e.getMessage());
		}
		final List<RenderModel.Entry> result = new ArrayList<>();
		for (final Map.Entry<String, String> entry : values.entrySet()) {
			final String key = entry.getKey();
			if (tooltip) {
				result.add(new RenderModel.Entry(key, truncate(entry.getValue(), TOOLTIP_VALUE_LENGTH)));
			} else if (!(settings.hideReserved() && RESERVED_KEYS.contains(key.toLowerCase(Locale.ROOT)))
					&& shownKeys.stream().anyMatch(g -> g.matcher(key).matches())) {
				result.add(new RenderModel.Entry(key, truncate(entry.getValue(), settings.maxValueLength())));
			}
		}
		return result;
	}

	private NodeId resolve(final String id) {
		try {
			return MiloStrategy.getNodeIdManager().convert(namespace.getGremlinGraph(), id.trim());
		} catch (final RuntimeException e) {
			return null;
		}
	}

	private List<Vertex> resolveRoots() {
		final List<Vertex> roots = new ArrayList<>();
		for (final String root : options.vertices().roots()) {
			final NodeId nodeId = resolve(root);
			final WaldotVertex vertex = nodeId == null ? null : namespace.getVertexNode(nodeId);
			if (vertex == null || !isDrawable(vertex)) {
				throw new IllegalArgumentException("root vertex '" + root + "' not found");
			}
			roots.add(vertex);
		}
		return roots;
	}

	/** Selects the elements and builds the model. */
	public RenderModel select() {
		final GraphRenderOptions.Vertices filter = options.vertices();
		final int limit = Integer.getInteger(DataExchangeProtocol.MAX_RENDER_VERTICES_PROPERTY,
				DataExchangeProtocol.DEFAULT_MAX_RENDER_VERTICES);
		int max = filter.max();
		if (max > limit) {
			warnings.add("vertices.max " + max + " reduced to the server limit " + limit);
			max = limit;
		}
		for (final String id : options.style().highlight()) {
			final NodeId nodeId = resolve(id);
			highlight.add(nodeId == null ? id.trim() : nodeId.toParseableString());
		}
		final Set<Object> candidates = filter.expression() == null ? null : expressionVertices();
		final List<Vertex> roots = resolveRoots();
		roots.forEach(r -> highlight.add(nodeId(r.id())));
		final List<Vertex> selected = roots.isEmpty() ? selectAll(candidates, max) : visit(roots, candidates, max);
		final Map<Object, Vertex> drawn = new LinkedHashMap<>();
		selected.forEach(v -> drawn.put(v.id(), v));

		final List<Edge> edges = new ArrayList<>();
		int hidden = 0;
		for (final Vertex vertex : drawn.values()) {
			for (final Iterator<Edge> it = vertex.edges(Direction.OUT); it.hasNext();) {
				final Edge edge = it.next();
				if (acceptedEdge(edge)) {
					if (drawn.containsKey(edge.inVertex().id())) {
						edges.add(edge);
					} else if (isDrawable(edge.inVertex())) {
						hidden++;
					}
				}
			}
			for (final Iterator<Edge> it = vertex.edges(Direction.IN); it.hasNext();) {
				final Edge edge = it.next();
				if (acceptedEdge(edge) && !drawn.containsKey(edge.outVertex().id())
						&& isDrawable(edge.outVertex())) {
					hidden++;
				}
			}
		}
		if (hidden > 0) {
			warnings.add(hidden + " edges to vertices not drawn omitted");
		}
		if (!filter.includeIsolated()) {
			final Set<Object> connected = new HashSet<>();
			edges.forEach(e -> {
				connected.add(e.outVertex().id());
				connected.add(e.inVertex().id());
			});
			roots.forEach(r -> connected.add(r.id()));
			drawn.keySet().retainAll(connected);
		}
		return toModel(new ArrayList<>(drawn.values()), edges);
	}

	private List<Vertex> selectAll(final Set<Object> candidates, final int max) {
		final List<Vertex> result = new ArrayList<>();
		namespace.getGremlinGraph().vertices().forEachRemaining(v -> {
			if (acceptedVertex(v, candidates)) {
				result.add(v);
			}
		});
		result.sort(Comparator.comparing(v -> nodeId(v.id())));
		if (result.size() > max) {
			warnings.add("drawing limited to " + max + " of " + result.size() + " vertices (vertices.max)");
			return new ArrayList<>(result.subList(0, max));
		}
		return result;
	}

	private RenderModel toModel(final List<Vertex> vertices, final List<Edge> edges) {
		final Set<String> colorKeys = new TreeSet<>();
		vertices.forEach(v -> {
			final String key = colorKey(v);
			if (key != null) {
				colorKeys.add(key);
			}
		});
		final Map<String, String> legend = new LinkedHashMap<>();
		int index = 0;
		for (final String key : colorKeys) {
			legend.put(key, PALETTE[index++ % PALETTE.length]);
		}
		final Map<Object, String> ids = new LinkedHashMap<>();
		final List<RenderModel.Vertex> modelVertices = new ArrayList<>();
		for (final Vertex vertex : vertices) {
			final String id = "v" + ids.size();
			ids.put(vertex.id(), id);
			final String type = stringProperty(vertex, TYPE_KEY);
			final String colorKey = colorKey(vertex);
			final String group = switch (options.layout().groupBy()) {
			case DIRECTORY -> path(stringProperty(vertex, MiloStrategy.DIRECTORY_PARAMETER));
			case TYPE -> typeOrLabel(vertex);
			case NONE -> "";
			};
			final List<RenderModel.Entry> tooltip = new ArrayList<>();
			tooltip.add(new RenderModel.Entry("nodeId", nodeId(vertex.id())));
			tooltip.add(new RenderModel.Entry("label", vertex.label()));
			tooltip.addAll(properties(vertex, vertexKeys, true));
			modelVertices.add(new RenderModel.Vertex(id, nodeId(vertex.id()),
					truncate(displayName(vertex), Math.max(24, options.properties().maxValueLength())),
					type == null ? vertex.label() : type, properties(vertex, vertexKeys, false), tooltip,
					isEphemeral(vertex), isHighlighted(vertex), group, colorKey,
					colorKey == null ? RenderModel.NEUTRAL_COLOR : legend.get(colorKey)));
		}
		final List<RenderModel.Edge> modelEdges = new ArrayList<>();
		for (final Edge edge : edges) {
			final String source = ids.get(edge.outVertex().id());
			final String target = ids.get(edge.inVertex().id());
			if (source == null || target == null) {
				continue;
			}
			final List<RenderModel.Entry> tooltip = new ArrayList<>();
			tooltip.add(new RenderModel.Entry("nodeId", nodeId(edge.id())));
			tooltip.add(new RenderModel.Entry("label", edge.label()));
			tooltip.addAll(properties(edge, edgeKeys, true));
			modelEdges.add(new RenderModel.Edge("e" + modelEdges.size(), nodeId(edge.id()), source, target,
					edge.label(), properties(edge, edgeKeys, false), tooltip, isEphemeral(edge),
					isHighlighted(edge)));
		}
		return new RenderModel(modelVertices, modelEdges,
				options.style().colorBy() == GraphRenderOptions.ColorBy.NONE ? Map.of() : legend,
				options.style().title(), warnings);
	}

	/** Breadth first visit from the roots, following the accepted edges. */
	private List<Vertex> visit(final List<Vertex> roots, final Set<Object> candidates, final int max) {
		final Map<Object, Vertex> visited = new LinkedHashMap<>();
		final Deque<Map.Entry<Vertex, Integer>> queue = new ArrayDeque<>();
		for (final Vertex root : roots) {
			if (visited.putIfAbsent(root.id(), root) == null) {
				queue.add(Map.entry(root, 0));
			}
		}
		final int depth = options.vertices().effectiveDepth();
		final Direction direction = switch (options.vertices().direction()) {
		case OUT -> Direction.OUT;
		case IN -> Direction.IN;
		case BOTH -> Direction.BOTH;
		};
		boolean truncated = false;
		while (!queue.isEmpty() && !truncated) {
			final Map.Entry<Vertex, Integer> current = queue.poll();
			if (current.getValue() >= depth) {
				continue;
			}
			for (final Iterator<Edge> it = current.getKey().edges(direction); it.hasNext();) {
				final Edge edge = it.next();
				if (!acceptedEdge(edge)) {
					continue;
				}
				final Vertex other = edge.outVertex().id().equals(current.getKey().id()) ? edge.inVertex()
						: edge.outVertex();
				if (visited.containsKey(other.id()) || !acceptedVertex(other, candidates)) {
					continue;
				}
				if (visited.size() >= max) {
					truncated = true;
					break;
				}
				visited.put(other.id(), other);
				queue.add(Map.entry(other, current.getValue() + 1));
			}
		}
		if (truncated) {
			warnings.add("drawing limited to " + max + " vertices (vertices.max)");
		}
		return new ArrayList<>(visited.values());
	}
}
