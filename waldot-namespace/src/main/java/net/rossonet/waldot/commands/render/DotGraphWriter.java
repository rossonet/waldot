package net.rossonet.waldot.commands.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.rossonet.waldot.api.exchange.GraphRenderOptions;

/**
 * Writes a {@link RenderModel} in the Graphviz DOT language (no layout: it is
 * left to Graphviz). Groups become {@code cluster} subgraphs.
 */
public final class DotGraphWriter {

	private static String attributes(final String label, final String tooltip, final boolean ephemeral,
			final boolean highlighted, final String color) {
		final StringBuilder out = new StringBuilder(" [label=").append(quote(label));
		if (tooltip != null) {
			out.append(", tooltip=").append(quote(tooltip));
		}
		if (color != null) {
			out.append(", color=").append(quote(color));
		}
		final List<String> style = new ArrayList<>();
		if (ephemeral) {
			style.add("dashed");
		}
		if (highlighted) {
			style.add("bold");
		}
		if (color != null) {
			style.add("rounded");
		}
		if (!style.isEmpty()) {
			out.append(", style=").append(quote(String.join(",", style)));
		}
		if (highlighted) {
			out.append(", penwidth=3");
		}
		return out.append("];\n").toString();
	}

	/** Lines of a label, left aligned. */
	private static String lines(final List<String> lines) {
		final StringBuilder out = new StringBuilder();
		for (final String line : lines) {
			out.append(line).append('\u0000');
		}
		return out.toString();
	}

	/** Quotes a DOT string; the NUL marks are left aligned line breaks. */
	static String quote(final String text) {
		final StringBuilder out = new StringBuilder("\"");
		for (final char c : text.toCharArray()) {
			switch (c) {
			case '"' -> out.append("\\\"");
			case '\\' -> out.append("\\\\");
			case '\u0000' -> out.append("\\l");
			case '\n' -> out.append("\\n");
			default -> {
				if (c >= 0x20) {
					out.append(c);
				}
			}
			}
		}
		return out.append('"').toString();
	}

	private static String rankdir(final GraphRenderOptions.LayoutDirection direction) {
		return switch (direction) {
		case RIGHT -> "LR";
		case DOWN -> "TB";
		case LEFT -> "RL";
		case UP -> "BT";
		};
	}

	private static String tooltip(final String header, final List<RenderModel.Entry> entries) {
		final StringBuilder out = new StringBuilder(header);
		entries.forEach(e -> out.append('\n').append(e.line()));
		return out.toString();
	}

	private static void writeVertex(final StringBuilder dot, final RenderModel.Vertex vertex,
			final boolean tooltips, final String indent) {
		final List<String> lines = new ArrayList<>();
		lines.add(vertex.title());
		lines.add("«" + vertex.subtitle() + "»");
		vertex.shown().forEach(e -> lines.add(e.line()));
		dot.append(indent).append(vertex.id()).append(attributes(lines(lines),
				tooltips ? tooltip(vertex.title(), vertex.tooltip()) : null, vertex.ephemeral(),
				vertex.highlighted(), vertex.color()));
	}

	/** Writes the DOT document. */
	public static String write(final RenderModel model, final GraphRenderOptions options) {
		final boolean tooltips = options.properties().tooltips();
		final StringBuilder dot = new StringBuilder(2048);
		dot.append("digraph waldot {\n");
		dot.append("  graph [rankdir=").append(rankdir(options.layout().direction())).append(", fontname=\"monospace\"");
		if (model.title() != null) {
			dot.append(", label=").append(quote(model.title())).append(", labelloc=t");
		}
		dot.append("];\n");
		dot.append("  node [shape=box, fontname=\"monospace\", fontsize=10];\n");
		dot.append("  edge [fontname=\"monospace\", fontsize=9];\n");
		final Map<String, List<RenderModel.Vertex>> groups = new LinkedHashMap<>();
		for (final RenderModel.Vertex vertex : model.vertices()) {
			groups.computeIfAbsent(vertex.group(), g -> new ArrayList<>()).add(vertex);
		}
		int cluster = 0;
		for (final Map.Entry<String, List<RenderModel.Vertex>> group : groups.entrySet()) {
			if (group.getKey().isEmpty()) {
				group.getValue().forEach(v -> writeVertex(dot, v, tooltips, "  "));
			} else {
				dot.append("  subgraph cluster_").append(cluster++).append(" {\n    label=")
						.append(quote(group.getKey())).append(";\n    style=\"rounded,dashed\";\n");
				group.getValue().forEach(v -> writeVertex(dot, v, tooltips, "    "));
				dot.append("  }\n");
			}
		}
		for (final RenderModel.Edge edge : model.edges()) {
			dot.append("  ").append(edge.source()).append(" -> ").append(edge.target())
					.append(attributes(options.style().edgeLabels() ? lines(edge.lines()) : "",
							tooltips ? tooltip(edge.label(), edge.tooltip()) : null, edge.ephemeral(),
							edge.highlighted(), null));
		}
		if (options.style().legend() && !model.legend().isEmpty()) {
			dot.append("  subgraph cluster_legend {\n    label=\"legend\";\n    style=dotted;\n");
			int index = 0;
			for (final Map.Entry<String, String> entry : model.legend().entrySet()) {
				dot.append("    legend_").append(index++).append(" [label=").append(quote(entry.getKey()))
						.append(", color=").append(quote(entry.getValue())).append(", style=\"rounded\"];\n");
			}
			dot.append("  }\n");
		}
		dot.append("}\n");
		return dot.toString();
	}

	private DotGraphWriter() {
	}
}
