package net.rossonet.waldot.commands.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.elk.alg.force.options.ForceMetaDataProvider;
import org.eclipse.elk.alg.force.options.StressMetaDataProvider;
import org.eclipse.elk.alg.force.options.StressOptions;
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider;
import org.eclipse.elk.alg.layered.options.LayeredOptions;
import org.eclipse.elk.alg.mrtree.options.MrTreeMetaDataProvider;
import org.eclipse.elk.alg.radial.options.RadialMetaDataProvider;
import org.eclipse.elk.alg.radial.options.RadialOptions;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.math.ElkPadding;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.options.Direction;
import org.eclipse.elk.core.options.EdgeLabelPlacement;
import org.eclipse.elk.core.options.HierarchyHandling;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.graph.ElkBendPoint;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import net.rossonet.waldot.api.exchange.GraphRenderOptions;

/**
 * Computes the coordinates of a {@link RenderModel} with Eclipse ELK. The
 * sizes of the boxes are estimated from the text (monospace font, no AWT).
 */
public final class ElkLayouter {

	/** Position and size. */
	public record Box(double x, double y, double width, double height) {

		double centerX() {
			return x + width / 2;
		}

		double centerY() {
			return y + height / 2;
		}
	}

	/** Coordinates of the whole drawing. */
	public record Drawing(double width, double height, List<PlacedGroup> groups, List<PlacedVertex> vertices,
			List<PlacedEdge> edges, List<String> warnings) {
	}

	/** An edge with its polyline and the box of its label (or null). */
	public record PlacedEdge(RenderModel.Edge edge, List<double[]> points, Box label) {
	}

	/** A group (compound node). */
	public record PlacedGroup(String name, Box box) {
	}

	/** A vertex with its box. */
	public record PlacedVertex(RenderModel.Vertex vertex, Box box) {
	}

	/** Width of a character of the monospace font. */
	public static final double CHAR_WIDTH = 7.2;
	/** Size of the font. */
	public static final double FONT_SIZE = 12;
	/** Height of the header of a group. */
	public static final double GROUP_HEADER = 24;
	/** Height of a line of text. */
	public static final double LINE_HEIGHT = 16;
	/** Inner margin of a box. */
	public static final double PADDING = 8;
	/** Space between the header and the properties of a vertex. */
	public static final double SEPARATOR = 6;

	private static volatile boolean registered;

	private static Box absolute(final ElkNode node) {
		double x = node.getX();
		double y = node.getY();
		for (ElkNode parent = node.getParent(); parent != null; parent = parent.getParent()) {
			x += parent.getX();
			y += parent.getY();
		}
		return new Box(x, y, node.getWidth(), node.getHeight());
	}

	private static String algorithmId(final GraphRenderOptions.Algorithm algorithm) {
		return switch (algorithm) {
		case LAYERED -> "org.eclipse.elk.layered";
		case FORCE -> "org.eclipse.elk.force";
		case STRESS -> "org.eclipse.elk.stress";
		case MRTREE -> "org.eclipse.elk.mrtree";
		case RADIAL -> "org.eclipse.elk.radial";
		};
	}

	/** Clips the segment from the center of a box towards a point. */
	private static double[] clip(final Box box, final double towardsX, final double towardsY) {
		final double dx = towardsX - box.centerX();
		final double dy = towardsY - box.centerY();
		if (dx == 0 && dy == 0) {
			return new double[] { box.centerX(), box.centerY() };
		}
		final double scale = Math.min(dx == 0 ? Double.MAX_VALUE : box.width() / 2 / Math.abs(dx),
				dy == 0 ? Double.MAX_VALUE : box.height() / 2 / Math.abs(dy));
		return new double[] { box.centerX() + dx * scale, box.centerY() + dy * scale };
	}

	private static Direction direction(final GraphRenderOptions.LayoutDirection direction) {
		return switch (direction) {
		case RIGHT -> Direction.RIGHT;
		case DOWN -> Direction.DOWN;
		case LEFT -> Direction.LEFT;
		case UP -> Direction.UP;
		};
	}

	private static void ensureRegistered() {
		if (registered) {
			return;
		}
		synchronized (ElkLayouter.class) {
			final LayoutMetaDataService service = LayoutMetaDataService.getInstance();
			// the providers are found with the ServiceLoader, unless the service
			// files were lost (e.g. minimized fat jar)
			if (service.getAlgorithmData("org.eclipse.elk.layered") == null) {
				service.registerLayoutMetaDataProviders(new LayeredMetaDataProvider(), new ForceMetaDataProvider(),
						new StressMetaDataProvider(), new MrTreeMetaDataProvider(), new RadialMetaDataProvider());
			}
			registered = true;
		}
	}

	/** Lays out the model. */
	public static Drawing layout(final RenderModel model, final GraphRenderOptions options) {
		ensureRegistered();
		final GraphRenderOptions.Layout settings = options.layout();
		final List<String> warnings = new ArrayList<>();
		final boolean layered = settings.algorithm() == GraphRenderOptions.Algorithm.LAYERED;
		final boolean tree = settings.algorithm() == GraphRenderOptions.Algorithm.MRTREE
				|| settings.algorithm() == GraphRenderOptions.Algorithm.RADIAL;
		boolean grouped = settings.groupBy() != GraphRenderOptions.GroupBy.NONE;
		if (grouped && tree) {
			warnings.add("layout.groupBy is ignored by the " + settings.algorithm().name().toLowerCase()
					+ " layout");
			grouped = false;
		}
		final ElkNode root = ElkGraphUtil.createGraph();
		configure(root, settings, 20);
		if (grouped && layered) {
			root.setProperty(CoreOptions.HIERARCHY_HANDLING, HierarchyHandling.INCLUDE_CHILDREN);
		}
		final Map<String, ElkNode> groups = new LinkedHashMap<>();
		final Map<String, ElkNode> nodes = new LinkedHashMap<>();
		double diagonal = 0;
		for (final RenderModel.Vertex vertex : model.vertices()) {
			ElkNode parent = root;
			if (grouped && !vertex.group().isEmpty()) {
				parent = groups.computeIfAbsent(vertex.group(), name -> {
					final ElkNode group = ElkGraphUtil.createNode(root);
					configure(group, settings, 12);
					group.setProperty(CoreOptions.PADDING, new ElkPadding(GROUP_HEADER + 12, 12, 12, 12));
					return group;
				});
			}
			final ElkNode node = ElkGraphUtil.createNode(parent);
			final double[] size = vertexSize(vertex);
			node.setDimensions(size[0], size[1]);
			nodes.put(vertex.id(), node);
			diagonal = Math.max(diagonal, Math.hypot(size[0], size[1]));
		}
		// the defaults of radius and edge length are far smaller than the boxes
		root.setProperty(RadialOptions.RADIUS, (diagonal + 30) * settings.spacing());
		root.setProperty(StressOptions.DESIRED_EDGE_LENGTH, (diagonal + 30) * settings.spacing());
		// the tree algorithms get a spanning tree under a hidden root, the other
		// edges are drawn straight
		final Set<RenderModel.Edge> laidOut = tree ? spanningTree(model, nodes)
				: new HashSet<>(model.edges());
		final Map<RenderModel.Edge, ElkEdge> elkEdges = new LinkedHashMap<>();
		for (final RenderModel.Edge edge : model.edges()) {
			if (!laidOut.contains(edge)) {
				continue;
			}
			final ElkEdge elkEdge = ElkGraphUtil.createSimpleEdge(nodes.get(edge.source()), nodes.get(edge.target()));
			ElkGraphUtil.updateContainment(elkEdge);
			if (options.style().edgeLabels()) {
				final double[] size = labelSize(edge);
				ElkGraphUtil.createLabel(edge.label(), elkEdge).setDimensions(size[0], size[1]);
			}
			elkEdges.put(edge, elkEdge);
		}
		new RecursiveGraphLayoutEngine().layout(root, new BasicProgressMonitor());

		final List<PlacedGroup> placedGroups = new ArrayList<>();
		groups.forEach((name, group) -> placedGroups.add(new PlacedGroup(name, absolute(group))));
		final List<PlacedVertex> placedVertices = new ArrayList<>();
		final Map<String, Box> boxes = new LinkedHashMap<>();
		for (final RenderModel.Vertex vertex : model.vertices()) {
			final Box box = absolute(nodes.get(vertex.id()));
			boxes.put(vertex.id(), box);
			placedVertices.add(new PlacedVertex(vertex, box));
		}
		final List<PlacedEdge> placedEdges = new ArrayList<>();
		for (final RenderModel.Edge edge : model.edges()) {
			placedEdges.add(placeEdge(elkEdges.get(edge), edge, boxes.get(edge.source()), boxes.get(edge.target()),
					layered, options.style().edgeLabels()));
		}
		return crop(placedGroups, placedVertices, placedEdges, warnings);
	}

	/** Moves the drawing to the origin, with a margin, and computes its size. */
	private static Drawing crop(final List<PlacedGroup> groups, final List<PlacedVertex> vertices,
			final List<PlacedEdge> edges, final List<String> warnings) {
		final double margin = 20;
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = 0;
		double maxY = 0;
		final List<Box> boxes = new ArrayList<>();
		groups.forEach(g -> boxes.add(g.box()));
		vertices.forEach(v -> boxes.add(v.box()));
		for (final PlacedEdge edge : edges) {
			if (edge.label() != null) {
				boxes.add(edge.label());
			}
			edge.points().forEach(p -> boxes.add(new Box(p[0], p[1], 0, 0)));
		}
		for (final Box box : boxes) {
			minX = Math.min(minX, box.x());
			minY = Math.min(minY, box.y());
			maxX = Math.max(maxX, box.x() + box.width());
			maxY = Math.max(maxY, box.y() + box.height());
		}
		if (boxes.isEmpty()) {
			return new Drawing(2 * margin, 2 * margin, groups, vertices, edges, warnings);
		}
		final double dx = margin - minX;
		final double dy = margin - minY;
		final List<PlacedGroup> movedGroups = new ArrayList<>();
		groups.forEach(g -> movedGroups.add(new PlacedGroup(g.name(), move(g.box(), dx, dy))));
		final List<PlacedVertex> movedVertices = new ArrayList<>();
		vertices.forEach(v -> movedVertices.add(new PlacedVertex(v.vertex(), move(v.box(), dx, dy))));
		final List<PlacedEdge> movedEdges = new ArrayList<>();
		for (final PlacedEdge edge : edges) {
			final List<double[]> points = new ArrayList<>();
			edge.points().forEach(p -> points.add(new double[] { p[0] + dx, p[1] + dy }));
			movedEdges.add(new PlacedEdge(edge.edge(), points,
					edge.label() == null ? null : move(edge.label(), dx, dy)));
		}
		return new Drawing(maxX - minX + 2 * margin, maxY - minY + 2 * margin, movedGroups, movedVertices,
				movedEdges, warnings);
	}

	private static double[] labelSize(final RenderModel.Edge edge) {
		final List<String> lines = edge.lines();
		return new double[] { textWidth(lines) + 6, lines.size() * LINE_HEIGHT + 2 };
	}

	private static Box move(final Box box, final double dx, final double dy) {
		return new Box(box.x() + dx, box.y() + dy, box.width(), box.height());
	}

	/**
	 * Breadth first spanning forest of the model (edges as undirected); the
	 * roots of the components are attached to the first one (with edges not
	 * drawn) so that the tree algorithms get a single tree. The highlighted
	 * vertices, then the vertices with more edges, are preferred as roots.
	 *
	 * @return the edges of the forest
	 */
	private static Set<RenderModel.Edge> spanningTree(final RenderModel model,
			final Map<String, ElkNode> nodes) {
		final Map<String, List<RenderModel.Edge>> adjacent = new LinkedHashMap<>();
		for (final RenderModel.Edge edge : model.edges()) {
			adjacent.computeIfAbsent(edge.source(), k -> new ArrayList<>()).add(edge);
			adjacent.computeIfAbsent(edge.target(), k -> new ArrayList<>()).add(edge);
		}
		final List<RenderModel.Vertex> order = new ArrayList<>(model.vertices());
		order.sort(Comparator.comparing(RenderModel.Vertex::highlighted).reversed().thenComparing(
				v -> adjacent.getOrDefault(v.id(), List.of()).size(), Comparator.reverseOrder()));
		final Set<String> visited = new HashSet<>();
		final Set<RenderModel.Edge> tree = new HashSet<>();
		final List<String> componentRoots = new ArrayList<>();
		for (final RenderModel.Vertex start : order) {
			if (!visited.add(start.id())) {
				continue;
			}
			componentRoots.add(start.id());
			final Deque<String> queue = new ArrayDeque<>();
			queue.add(start.id());
			while (!queue.isEmpty()) {
				final String current = queue.poll();
				for (final RenderModel.Edge edge : adjacent.getOrDefault(current, List.of())) {
					final String other = edge.source().equals(current) ? edge.target() : edge.source();
					if (visited.add(other)) {
						tree.add(edge);
						queue.add(other);
					}
				}
			}
		}
		for (final String componentRoot : componentRoots.subList(Math.min(1, componentRoots.size()),
				componentRoots.size())) {
			ElkGraphUtil.updateContainment(
					ElkGraphUtil.createSimpleEdge(nodes.get(componentRoots.get(0)), nodes.get(componentRoot)));
		}
		return tree;
	}

	private static void configure(final ElkNode node, final GraphRenderOptions.Layout settings,
			final double padding) {
		final double spacing = settings.spacing();
		node.setProperty(CoreOptions.ALGORITHM, algorithmId(settings.algorithm()));
		node.setProperty(CoreOptions.DIRECTION, direction(settings.direction()));
		node.setProperty(CoreOptions.PADDING, new ElkPadding(padding));
		node.setProperty(CoreOptions.SPACING_NODE_NODE, 30 * spacing);
		node.setProperty(CoreOptions.SPACING_EDGE_NODE, 15 * spacing);
		node.setProperty(CoreOptions.SPACING_COMPONENT_COMPONENT, 40 * spacing);
		node.setProperty(CoreOptions.EDGE_LABELS_PLACEMENT, EdgeLabelPlacement.CENTER);
		node.setProperty(CoreOptions.RANDOM_SEED, 1);
		node.setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, 50 * spacing);
		node.setProperty(LayeredOptions.SPACING_EDGE_NODE_BETWEEN_LAYERS, 15 * spacing);
	}

	private static PlacedEdge placeEdge(final ElkEdge elkEdge, final RenderModel.Edge edge, final Box source,
			final Box target, final boolean layered, final boolean withLabel) {
		final List<double[]> points = new ArrayList<>();
		Box offset = new Box(0, 0, 0, 0);
		if (elkEdge != null && layered) {
			final ElkNode container = elkEdge.getContainingNode();
			if (container != null && container.getParent() != null) {
				offset = absolute(container);
			}
			for (final ElkEdgeSection section : elkEdge.getSections()) {
				points.add(new double[] { offset.x() + section.getStartX(), offset.y() + section.getStartY() });
				for (final ElkBendPoint bend : section.getBendPoints()) {
					points.add(new double[] { offset.x() + bend.getX(), offset.y() + bend.getY() });
				}
				points.add(new double[] { offset.x() + section.getEndX(), offset.y() + section.getEndY() });
			}
		}
		final boolean routed = points.size() >= 2;
		if (!routed) {
			points.clear();
			points.addAll(straight(source, target));
		}
		if (!withLabel) {
			return new PlacedEdge(edge, points, null);
		}
		if (routed && !elkEdge.getLabels().isEmpty()) {
			final ElkLabel elkLabel = elkEdge.getLabels().get(0);
			if (elkLabel.getX() != 0 || elkLabel.getY() != 0) {
				return new PlacedEdge(edge, points, new Box(offset.x() + elkLabel.getX(),
						offset.y() + elkLabel.getY(), elkLabel.getWidth(), elkLabel.getHeight()));
			}
		}
		final double[] size = labelSize(edge);
		final double[] middle = middle(points);
		return new PlacedEdge(edge, points, new Box(middle[0] - size[0] / 2, middle[1] - size[1] / 2, size[0],
				size[1]));
	}

	/** @return the point at half the length of a polyline */
	private static double[] middle(final List<double[]> points) {
		double length = 0;
		for (int i = 1; i < points.size(); i++) {
			length += Math.hypot(points.get(i)[0] - points.get(i - 1)[0], points.get(i)[1] - points.get(i - 1)[1]);
		}
		double remaining = length / 2;
		for (int i = 1; i < points.size(); i++) {
			final double[] a = points.get(i - 1);
			final double[] b = points.get(i);
			final double segment = Math.hypot(b[0] - a[0], b[1] - a[1]);
			if (segment >= remaining && segment > 0) {
				final double t = remaining / segment;
				return new double[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t };
			}
			remaining -= segment;
		}
		return points.get(0);
	}

	/** Straight segment between the borders of two boxes (a loop if equal). */
	private static List<double[]> straight(final Box source, final Box target) {
		final List<double[]> points = new ArrayList<>();
		if (source.equals(target)) {
			final double top = source.y();
			points.add(new double[] { source.x() + source.width() * 0.7, top });
			points.add(new double[] { source.x() + source.width() * 0.7, top - 20 });
			points.add(new double[] { source.x() + source.width() * 0.3, top - 20 });
			points.add(new double[] { source.x() + source.width() * 0.3, top });
			return points;
		}
		points.add(clip(source, target.centerX(), target.centerY()));
		points.add(clip(target, source.centerX(), source.centerY()));
		return points;
	}

	static double textWidth(final List<String> lines) {
		int longest = 0;
		for (final String line : lines) {
			longest = Math.max(longest, line.codePointCount(0, line.length()));
		}
		return longest * CHAR_WIDTH;
	}

	/** @return width and height of the box of a vertex */
	static double[] vertexSize(final RenderModel.Vertex vertex) {
		final List<String> lines = new ArrayList<>();
		lines.add(vertex.title());
		lines.add(vertex.subtitle());
		vertex.shown().forEach(e -> lines.add(e.line()));
		final double width = Math.max(80, textWidth(lines) + 2 * PADDING);
		double height = 2 * LINE_HEIGHT + 2 * PADDING;
		if (!vertex.shown().isEmpty()) {
			height += SEPARATOR + vertex.shown().size() * LINE_HEIGHT;
		}
		return new double[] { width, height };
	}

	private ElkLayouter() {
	}
}
