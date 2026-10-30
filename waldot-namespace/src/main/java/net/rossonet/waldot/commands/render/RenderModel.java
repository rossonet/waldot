package net.rossonet.waldot.commands.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The part of the graph to draw, independent of the output format: the
 * selected vertices and edges with the text to show, groups and colors.
 */
public final class RenderModel {

	/** Edge between two drawn vertices. */
	public record Edge(String id, String nodeId, String source, String target, String label, List<Entry> shown,
			List<Entry> tooltip, boolean ephemeral, boolean highlighted) {

		/** @return label and shown properties, one per line */
		public List<String> lines() {
			final List<String> lines = new ArrayList<>();
			lines.add(label);
			shown.forEach(e -> lines.add(e.line()));
			return lines;
		}
	}

	/** A property shown in the drawing. */
	public record Entry(String key, String value) {

		public String line() {
			return key + " = " + value;
		}
	}

	/** A drawn vertex. */
	public record Vertex(String id, String nodeId, String title, String subtitle, List<Entry> shown,
			List<Entry> tooltip, boolean ephemeral, boolean highlighted, String group, String colorKey,
			String color) {
	}

	/** Color of the vertices without a color key. */
	public static final String NEUTRAL_COLOR = "#8c959f";

	private final List<Edge> edges;
	private final Map<String, String> legend;
	private final String title;
	private final List<Vertex> vertices;
	private final List<String> warnings;

	RenderModel(final List<Vertex> vertices, final List<Edge> edges, final Map<String, String> legend,
			final String title, final List<String> warnings) {
		this.vertices = List.copyOf(vertices);
		this.edges = List.copyOf(edges);
		this.legend = Map.copyOf(legend);
		this.title = title;
		this.warnings = List.copyOf(warnings);
	}

	public List<Edge> edges() {
		return edges;
	}

	/** @return color key to color, sorted by key */
	public Map<String, String> legend() {
		return new java.util.TreeMap<>(legend);
	}

	/** @return title of the drawing, null for none */
	public String title() {
		return title;
	}

	public List<Vertex> vertices() {
		return vertices;
	}

	public List<String> warnings() {
		return warnings;
	}
}
