package net.rossonet.waldot.api.exchange;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Options of the {@value DataExchangeProtocol#RENDER_GRAPH_COMMAND} command,
 * travelling as JSON in the {@value DataExchangeProtocol#ARG_OPTIONS}
 * argument. Every key is optional; unknown keys are rejected so that typos
 * are reported.
 *
 * <pre>
 * {
 *   "format": "svg",
 *   "vertices": { "roots": [], "depth": 1, "direction": "both", "types": [], "excludeTypes": [],
 *                 "labels": [], "directories": [], "match": null, "where": {}, "expression": null,
 *                 "includeEphemeral": false, "includeIsolated": true, "max": 500 },
 *   "edges": { "labels": [], "excludeLabels": [], "types": [] },
 *   "properties": { "vertex": ["*"], "edge": [], "includeRuntime": false, "hideReserved": true,
 *                   "maxValueLength": 40, "tooltips": true },
 *   "layout": { "algorithm": "layered", "direction": "right", "groupBy": "none", "spacing": 1.0 },
 *   "style": { "theme": "auto", "colorBy": "type", "legend": true, "edgeLabels": true, "title": null,
 *              "highlight": [] }
 * }
 * </pre>
 */
public final class GraphRenderOptions {

	/** Layout algorithm (Eclipse ELK). */
	public enum Algorithm {
		FORCE, LAYERED, MRTREE, RADIAL, STRESS
	}

	/** Color of the vertices. */
	public enum ColorBy {
		DIRECTORY, LABEL, NONE, TYPE
	}

	/** Edges followed from the roots. */
	public enum Direction {
		BOTH, IN, OUT
	}

	/** Selection of the edges. */
	public static final class Edges {
		private List<String> excludeLabels = new ArrayList<>();
		private List<String> labels = new ArrayList<>();
		private List<String> types = new ArrayList<>();

		/** @return edge labels never drawn */
		public List<String> excludeLabels() {
			return excludeLabels;
		}

		public Edges excludeLabels(final List<String> value) {
			excludeLabels = copy(value);
			return this;
		}

		/** @return edge labels drawn (empty = all) */
		public List<String> labels() {
			return labels;
		}

		public Edges labels(final List<String> value) {
			labels = copy(value);
			return this;
		}

		/** @return values of the edge property 'type' drawn (empty = all) */
		public List<String> types() {
			return types;
		}

		public Edges types(final List<String> value) {
			types = copy(value);
			return this;
		}
	}

	/** Output format. */
	public enum Format {
		DOT, SVG
	}

	/** Compound nodes of the layout. */
	public enum GroupBy {
		DIRECTORY, NONE, TYPE
	}

	/** Layout of the drawing. */
	public static final class Layout {
		private Algorithm algorithm = Algorithm.LAYERED;
		private LayoutDirection direction = LayoutDirection.RIGHT;
		private GroupBy groupBy = GroupBy.NONE;
		private double spacing = 1.0;

		public Algorithm algorithm() {
			return algorithm;
		}

		public Layout algorithm(final Algorithm value) {
			algorithm = require(value, "layout.algorithm");
			return this;
		}

		public LayoutDirection direction() {
			return direction;
		}

		public Layout direction(final LayoutDirection value) {
			direction = require(value, "layout.direction");
			return this;
		}

		public GroupBy groupBy() {
			return groupBy;
		}

		public Layout groupBy(final GroupBy value) {
			groupBy = require(value, "layout.groupBy");
			return this;
		}

		/** @return multiplier of the default spacing between the elements */
		public double spacing() {
			return spacing;
		}

		public Layout spacing(final double value) {
			if (!(value >= 0.1 && value <= 10.0)) {
				throw new IllegalArgumentException(
						"layout.spacing must be between 0.1 and 10: " + value);
			}
			spacing = value;
			return this;
		}
	}

	/** Main direction of the edges (layered and mrtree algorithms). */
	public enum LayoutDirection {
		DOWN, LEFT, RIGHT, UP
	}

	/** Properties shown in the drawing. */
	public static final class Properties {
		private List<String> edge = new ArrayList<>();
		private boolean hideReserved = true;
		private boolean includeRuntime = false;
		private int maxValueLength = 40;
		private boolean tooltips = true;
		private List<String> vertex = new ArrayList<>(List.of("*"));

		/** @return glob patterns of the edge keys shown under the edge label */
		public List<String> edge() {
			return edge;
		}

		public Properties edge(final List<String> value) {
			edge = copy(value);
			return this;
		}

		/**
		 * @return true to hide in the box the reserved keys (type, directory,
		 *         name, label, ...) already shown in the header
		 */
		public boolean hideReserved() {
			return hideReserved;
		}

		public Properties hideReserved(final boolean value) {
			hideReserved = value;
			return this;
		}

		/** @return true to show the runtime keys ('run!...') */
		public boolean includeRuntime() {
			return includeRuntime;
		}

		public Properties includeRuntime(final boolean value) {
			includeRuntime = value;
			return this;
		}

		/** @return longest value shown, longer values are truncated */
		public int maxValueLength() {
			return maxValueLength;
		}

		public Properties maxValueLength(final int value) {
			if (value < 4) {
				throw new IllegalArgumentException(
						"properties.maxValueLength must be at least 4: "
								+ value);
			}
			maxValueLength = value;
			return this;
		}

		/** @return true to add tooltips with every property and the NodeId */
		public boolean tooltips() {
			return tooltips;
		}

		public Properties tooltips(final boolean value) {
			tooltips = value;
			return this;
		}

		/** @return glob patterns of the vertex keys shown in the box */
		public List<String> vertex() {
			return vertex;
		}

		public Properties vertex(final List<String> value) {
			vertex = copy(value);
			return this;
		}
	}

	/** Look of the drawing. */
	public static final class Style {
		private ColorBy colorBy = ColorBy.TYPE;
		private boolean edgeLabels = true;
		private List<String> highlight = new ArrayList<>();
		private boolean legend = true;
		private Theme theme = Theme.AUTO;
		private String title;

		public ColorBy colorBy() {
			return colorBy;
		}

		public Style colorBy(final ColorBy value) {
			colorBy = require(value, "style.colorBy");
			return this;
		}

		public boolean edgeLabels() {
			return edgeLabels;
		}

		public Style edgeLabels(final boolean value) {
			edgeLabels = value;
			return this;
		}

		/** @return ids of the elements drawn with a thicker border */
		public List<String> highlight() {
			return highlight;
		}

		public Style highlight(final List<String> value) {
			highlight = copy(value);
			return this;
		}

		public boolean legend() {
			return legend;
		}

		public Style legend(final boolean value) {
			legend = value;
			return this;
		}

		public Theme theme() {
			return theme;
		}

		public Style theme(final Theme value) {
			theme = require(value, "style.theme");
			return this;
		}

		/** @return title of the drawing, null for none */
		public String title() {
			return title;
		}

		public Style title(final String value) {
			title = blankToNull(value);
			return this;
		}
	}

	/** Colors of the drawing; auto follows the viewer preference. */
	public enum Theme {
		AUTO, DARK, LIGHT
	}

	/** Selection of the vertices. */
	public static final class Vertices {
		private Integer depth;
		private Direction direction = Direction.BOTH;
		private List<String> directories = new ArrayList<>();
		private List<String> excludeTypes = new ArrayList<>();
		private String expression;
		private boolean includeEphemeral = false;
		private boolean includeIsolated = true;
		private List<String> labels = new ArrayList<>();
		private String match;
		private int max = DEFAULT_MAX_VERTICES;
		private List<String> roots = new ArrayList<>();
		private List<String> types = new ArrayList<>();
		private Map<String, String> where = new LinkedHashMap<>();

		/** @return hops from the roots, null for the default (1) */
		public Integer depth() {
			return depth;
		}

		public Vertices depth(final Integer value) {
			if (value != null && value < 0) {
				throw new IllegalArgumentException(
						"vertices.depth must not be negative: " + value);
			}
			depth = value;
			return this;
		}

		public Direction direction() {
			return direction;
		}

		public Vertices direction(final Direction value) {
			direction = require(value, "vertices.direction");
			return this;
		}

		/** @return prefixes of the 'directory' property (empty = all) */
		public List<String> directories() {
			return directories;
		}

		public Vertices directories(final List<String> value) {
			directories = copy(value);
			return this;
		}

		/** @return the depth used from the roots */
		public int effectiveDepth() {
			return depth == null ? 1 : depth;
		}

		/** @return values of the 'type' property never drawn */
		public List<String> excludeTypes() {
			return excludeTypes;
		}

		public Vertices excludeTypes(final List<String> value) {
			excludeTypes = copy(value);
			return this;
		}

		/**
		 * @return JEXL expression (console syntax) returning the candidate
		 *         vertices, null for every vertex
		 */
		public String expression() {
			return expression;
		}

		public Vertices expression(final String value) {
			expression = blankToNull(value);
			return this;
		}

		/** @return true to draw the ephemeral vertices (dashed border) */
		public boolean includeEphemeral() {
			return includeEphemeral;
		}

		public Vertices includeEphemeral(final boolean value) {
			includeEphemeral = value;
			return this;
		}

		/** @return true to draw the vertices without drawn edges */
		public boolean includeIsolated() {
			return includeIsolated;
		}

		public Vertices includeIsolated(final boolean value) {
			includeIsolated = value;
			return this;
		}

		/** @return vertex labels drawn (empty = all) */
		public List<String> labels() {
			return labels;
		}

		public Vertices labels(final List<String> value) {
			labels = copy(value);
			return this;
		}

		/** @return regular expression on id, name or display name, or null */
		public String match() {
			return match;
		}

		public Vertices match(final String value) {
			match = checkRegex(blankToNull(value), "vertices.match");
			return this;
		}

		/** @return most vertices drawn, the others are dropped with a warning */
		public int max() {
			return max;
		}

		public Vertices max(final int value) {
			if (value < 1) {
				throw new IllegalArgumentException(
						"vertices.max must be positive: " + value);
			}
			max = value;
			return this;
		}

		/** @return ids (NodeId or plain id) of the starting vertices */
		public List<String> roots() {
			return roots;
		}

		public Vertices roots(final List<String> value) {
			roots = copy(value);
			return this;
		}

		/** @return values of the 'type' property drawn (empty = all) */
		public List<String> types() {
			return types;
		}

		public Vertices types(final List<String> value) {
			types = copy(value);
			return this;
		}

		/** @return property key to regular expression on its value, all in AND */
		public Map<String, String> where() {
			return where;
		}

		public Vertices where(final Map<String, String> value) {
			final Map<String, String> checked = new LinkedHashMap<>();
			if (value != null) {
				value.forEach((k, v) -> checked.put(k,
						checkRegex(v == null ? "" : v, "vertices.where." + k)));
			}
			where = checked;
			return this;
		}
	}

	/** Default of {@code vertices.max}. */
	public static final int DEFAULT_MAX_VERTICES = 50000;

	private static final ObjectMapper JSON = new ObjectMapper();

	private static String blankToNull(final String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private static boolean booleanValue(final JsonNode node,
			final String name) {
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		if (node.isTextual()
				&& ("true".equalsIgnoreCase(node.textValue().trim())
						|| "false".equalsIgnoreCase(node.textValue().trim()))) {
			return Boolean.parseBoolean(node.textValue().trim());
		}
		throw new IllegalArgumentException("'" + name + "' must be a boolean");
	}

	private static void checkKeys(final JsonNode node, final String path,
			final String... allowed) {
		if (node == null || node.isNull()) {
			return;
		}
		if (!node.isObject()) {
			throw new IllegalArgumentException(
					"'" + path + "' must be a JSON object");
		}
		final Set<String> keys = Set.of(allowed);
		final Iterator<String> names = node.fieldNames();
		while (names.hasNext()) {
			final String name = names.next();
			if (!keys.contains(name)) {
				throw new IllegalArgumentException("unknown option '"
						+ (path.isEmpty() ? name : path + "." + name) + "'");
			}
		}
	}

	private static String checkRegex(final String value, final String name) {
		if (value != null) {
			try {
				Pattern.compile(value);
			} catch (final PatternSyntaxException e) {
				throw new IllegalArgumentException(
						name + " is not a valid regular expression: " + value,
						e);
			}
		}
		return value;
	}

	private static List<String> copy(final List<String> value) {
		final List<String> result = new ArrayList<>();
		if (value != null) {
			for (final String item : value) {
				if (item != null && !item.isBlank()) {
					result.add(item.trim());
				}
			}
		}
		return result;
	}

	/**
	 * Parses the options; null or blank text gives the defaults.
	 *
	 * @throws IllegalArgumentException if the JSON is invalid, a key is
	 *                                  unknown or a value is out of range
	 */
	public static GraphRenderOptions fromJson(final String json) {
		final GraphRenderOptions options = new GraphRenderOptions();
		if (json == null || json.isBlank()) {
			return options;
		}
		final JsonNode root;
		try {
			root = JSON.readTree(json);
		} catch (final JsonProcessingException e) {
			throw new IllegalArgumentException(
					"invalid options JSON: " + e.getOriginalMessage(), e);
		}
		checkKeys(root, "", "format", "vertices", "edges", "properties",
				"layout", "style");
		if (root.has("format")) {
			options.format(
					parseEnum(Format.class, root.get("format"), "format"));
		}
		final JsonNode v = root.get("vertices");
		checkKeys(v, "vertices", "roots", "depth", "direction", "types",
				"excludeTypes", "labels", "directories", "match", "where",
				"expression", "includeEphemeral", "includeIsolated", "max");
		if (v != null && !v.isNull()) {
			final Vertices vertices = options.vertices();
			if (v.has("roots")) {
				vertices.roots(stringList(v.get("roots"), "vertices.roots"));
			}
			if (v.has("depth")) {
				vertices.depth(v.get("depth").isNull()
						? null
						: intValue(v.get("depth"), "vertices.depth"));
			}
			if (v.has("direction")) {
				vertices.direction(parseEnum(Direction.class,
						v.get("direction"), "vertices.direction"));
			}
			if (v.has("types")) {
				vertices.types(stringList(v.get("types"), "vertices.types"));
			}
			if (v.has("excludeTypes")) {
				vertices.excludeTypes(stringList(v.get("excludeTypes"),
						"vertices.excludeTypes"));
			}
			if (v.has("labels")) {
				vertices.labels(stringList(v.get("labels"), "vertices.labels"));
			}
			if (v.has("directories")) {
				vertices.directories(stringList(v.get("directories"),
						"vertices.directories"));
			}
			if (v.has("match")) {
				vertices.match(stringValue(v.get("match"), "vertices.match"));
			}
			if (v.has("where")) {
				vertices.where(stringMap(v.get("where"), "vertices.where"));
			}
			if (v.has("expression")) {
				vertices.expression(stringValue(v.get("expression"),
						"vertices.expression"));
			}
			if (v.has("includeEphemeral")) {
				vertices.includeEphemeral(
						booleanValue(v.get("includeEphemeral"),
								"vertices.includeEphemeral"));
			}
			if (v.has("includeIsolated")) {
				vertices.includeIsolated(booleanValue(v.get("includeIsolated"),
						"vertices.includeIsolated"));
			}
			if (v.has("max")) {
				vertices.max(intValue(v.get("max"), "vertices.max"));
			}
		}
		final JsonNode e = root.get("edges");
		checkKeys(e, "edges", "labels", "excludeLabels", "types");
		if (e != null && !e.isNull()) {
			if (e.has("labels")) {
				options.edges()
						.labels(stringList(e.get("labels"), "edges.labels"));
			}
			if (e.has("excludeLabels")) {
				options.edges().excludeLabels(stringList(e.get("excludeLabels"),
						"edges.excludeLabels"));
			}
			if (e.has("types")) {
				options.edges()
						.types(stringList(e.get("types"), "edges.types"));
			}
		}
		final JsonNode p = root.get("properties");
		checkKeys(p, "properties", "vertex", "edge", "includeRuntime",
				"hideReserved", "maxValueLength", "tooltips");
		if (p != null && !p.isNull()) {
			final Properties properties = options.properties();
			if (p.has("vertex")) {
				properties.vertex(
						stringList(p.get("vertex"), "properties.vertex"));
			}
			if (p.has("edge")) {
				properties.edge(stringList(p.get("edge"), "properties.edge"));
			}
			if (p.has("includeRuntime")) {
				properties.includeRuntime(booleanValue(p.get("includeRuntime"),
						"properties.includeRuntime"));
			}
			if (p.has("hideReserved")) {
				properties.hideReserved(booleanValue(p.get("hideReserved"),
						"properties.hideReserved"));
			}
			if (p.has("maxValueLength")) {
				properties.maxValueLength(intValue(p.get("maxValueLength"),
						"properties.maxValueLength"));
			}
			if (p.has("tooltips")) {
				properties.tooltips(
						booleanValue(p.get("tooltips"), "properties.tooltips"));
			}
		}
		final JsonNode l = root.get("layout");
		checkKeys(l, "layout", "algorithm", "direction", "groupBy", "spacing");
		if (l != null && !l.isNull()) {
			if (l.has("algorithm")) {
				options.layout().algorithm(parseEnum(Algorithm.class,
						l.get("algorithm"), "layout.algorithm"));
			}
			if (l.has("direction")) {
				options.layout().direction(parseEnum(LayoutDirection.class,
						l.get("direction"), "layout.direction"));
			}
			if (l.has("groupBy")) {
				options.layout().groupBy(parseEnum(GroupBy.class,
						l.get("groupBy"), "layout.groupBy"));
			}
			if (l.has("spacing")) {
				if (!l.get("spacing").isNumber()) {
					throw new IllegalArgumentException(
							"'layout.spacing' must be a number");
				}
				options.layout().spacing(l.get("spacing").asDouble());
			}
		}
		final JsonNode s = root.get("style");
		checkKeys(s, "style", "theme", "colorBy", "legend", "edgeLabels",
				"title", "highlight");
		if (s != null && !s.isNull()) {
			final Style style = options.style();
			if (s.has("theme")) {
				style.theme(
						parseEnum(Theme.class, s.get("theme"), "style.theme"));
			}
			if (s.has("colorBy")) {
				style.colorBy(parseEnum(ColorBy.class, s.get("colorBy"),
						"style.colorBy"));
			}
			if (s.has("legend")) {
				style.legend(booleanValue(s.get("legend"), "style.legend"));
			}
			if (s.has("edgeLabels")) {
				style.edgeLabels(
						booleanValue(s.get("edgeLabels"), "style.edgeLabels"));
			}
			if (s.has("title")) {
				style.title(stringValue(s.get("title"), "style.title"));
			}
			if (s.has("highlight")) {
				style.highlight(
						stringList(s.get("highlight"), "style.highlight"));
			}
		}
		return options;
	}

	private static int intValue(final JsonNode node, final String name) {
		if (node.isIntegralNumber() && node.canConvertToInt()) {
			return node.intValue();
		}
		if (node.isTextual()) {
			try {
				return Integer.parseInt(node.textValue().trim());
			} catch (final NumberFormatException e) {
				// reported below
			}
		}
		throw new IllegalArgumentException("'" + name + "' must be an integer");
	}

	private static String lower(final Enum<?> value) {
		return value.name().toLowerCase(Locale.ROOT);
	}

	private static <E extends Enum<E>> E parseEnum(final Class<E> type,
			final JsonNode node, final String name) {
		return parseEnum(type, stringValue(node, name), name);
	}

	/** Parses an enum constant ignoring the case. */
	public static <E extends Enum<E>> E parseEnum(final Class<E> type,
			final String value, final String name) {
		if (value != null) {
			for (final E constant : type.getEnumConstants()) {
				if (constant.name().equalsIgnoreCase(value.trim())) {
					return constant;
				}
			}
		}
		final List<String> allowed = new ArrayList<>();
		for (final E constant : type.getEnumConstants()) {
			allowed.add(constant.name().toLowerCase(Locale.ROOT));
		}
		throw new IllegalArgumentException(
				"'" + name + "' must be one of " + allowed + ": " + value);
	}

	private static <T> T require(final T value, final String name) {
		if (value == null) {
			throw new IllegalArgumentException(
					"'" + name + "' must not be null");
		}
		return value;
	}

	private static List<String> stringList(final JsonNode node,
			final String name) {
		final List<String> result = new ArrayList<>();
		if (node == null || node.isNull()) {
			return result;
		}
		if (node.isTextual()) {
			result.add(node.textValue());
			return result;
		}
		if (!node.isArray()) {
			throw new IllegalArgumentException(
					"'" + name + "' must be a list of strings");
		}
		for (final JsonNode item : node) {
			if (!item.isValueNode() || item.isNull()) {
				throw new IllegalArgumentException(
						"'" + name + "' must be a list of strings");
			}
			result.add(item.asText());
		}
		return result;
	}

	private static Map<String, String> stringMap(final JsonNode node,
			final String name) {
		final Map<String, String> result = new LinkedHashMap<>();
		if (node == null || node.isNull()) {
			return result;
		}
		if (!node.isObject()) {
			throw new IllegalArgumentException(
					"'" + name + "' must be a JSON object");
		}
		node.fields().forEachRemaining(entry -> {
			if (!entry.getValue().isValueNode() || entry.getValue().isNull()) {
				throw new IllegalArgumentException("'" + name + "."
						+ entry.getKey() + "' must be a string");
			}
			result.put(entry.getKey(), entry.getValue().asText());
		});
		return result;
	}

	private static void strings(final ArrayNode array,
			final List<String> values) {
		values.forEach(array::add);
	}

	private static String stringValue(final JsonNode node, final String name) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (!node.isValueNode()) {
			throw new IllegalArgumentException(
					"'" + name + "' must be a string");
		}
		return node.asText();
	}

	private final Edges edges = new Edges();

	private Format format = Format.SVG;

	private final Layout layout = new Layout();

	private final Properties properties = new Properties();

	private final Style style = new Style();

	private final Vertices vertices = new Vertices();

	public Edges edges() {
		return edges;
	}

	public Format format() {
		return format;
	}

	public GraphRenderOptions format(final Format value) {
		format = require(value, "format");
		return this;
	}

	public Layout layout() {
		return layout;
	}

	public Properties properties() {
		return properties;
	}

	public Style style() {
		return style;
	}

	/** @return the options as JSON (every key, defaults included) */
	public String toJson() {
		final ObjectNode root = JSON.createObjectNode();
		root.put("format", lower(format));
		final ObjectNode v = root.putObject("vertices");
		strings(v.putArray("roots"), vertices.roots);
		if (vertices.depth != null) {
			v.put("depth", vertices.depth);
		}
		v.put("direction", lower(vertices.direction));
		strings(v.putArray("types"), vertices.types);
		strings(v.putArray("excludeTypes"), vertices.excludeTypes);
		strings(v.putArray("labels"), vertices.labels);
		strings(v.putArray("directories"), vertices.directories);
		if (vertices.match != null) {
			v.put("match", vertices.match);
		}
		final ObjectNode where = v.putObject("where");
		vertices.where.forEach(where::put);
		if (vertices.expression != null) {
			v.put("expression", vertices.expression);
		}
		v.put("includeEphemeral", vertices.includeEphemeral);
		v.put("includeIsolated", vertices.includeIsolated);
		v.put("max", vertices.max);
		final ObjectNode e = root.putObject("edges");
		strings(e.putArray("labels"), edges.labels);
		strings(e.putArray("excludeLabels"), edges.excludeLabels);
		strings(e.putArray("types"), edges.types);
		final ObjectNode p = root.putObject("properties");
		strings(p.putArray("vertex"), properties.vertex);
		strings(p.putArray("edge"), properties.edge);
		p.put("includeRuntime", properties.includeRuntime);
		p.put("hideReserved", properties.hideReserved);
		p.put("maxValueLength", properties.maxValueLength);
		p.put("tooltips", properties.tooltips);
		final ObjectNode l = root.putObject("layout");
		l.put("algorithm", lower(layout.algorithm));
		l.put("direction", lower(layout.direction));
		l.put("groupBy", lower(layout.groupBy));
		l.put("spacing", layout.spacing);
		final ObjectNode s = root.putObject("style");
		s.put("theme", lower(style.theme));
		s.put("colorBy", lower(style.colorBy));
		s.put("legend", style.legend);
		s.put("edgeLabels", style.edgeLabels);
		if (style.title != null) {
			s.put("title", style.title);
		}
		strings(s.putArray("highlight"), style.highlight);
		try {
			return JSON.writeValueAsString(root);
		} catch (final JsonProcessingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public String toString() {
		return toJson();
	}

	public Vertices vertices() {
		return vertices;
	}
}
