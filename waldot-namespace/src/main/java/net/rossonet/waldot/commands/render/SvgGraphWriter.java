package net.rossonet.waldot.commands.render;

import static net.rossonet.waldot.commands.render.ElkLayouter.CHAR_WIDTH;
import static net.rossonet.waldot.commands.render.ElkLayouter.FONT_SIZE;
import static net.rossonet.waldot.commands.render.ElkLayouter.LINE_HEIGHT;
import static net.rossonet.waldot.commands.render.ElkLayouter.PADDING;
import static net.rossonet.waldot.commands.render.ElkLayouter.SEPARATOR;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.rossonet.waldot.api.exchange.GraphRenderOptions;
import net.rossonet.waldot.commands.render.ElkLayouter.Box;
import net.rossonet.waldot.commands.render.ElkLayouter.Drawing;
import net.rossonet.waldot.commands.render.ElkLayouter.PlacedEdge;
import net.rossonet.waldot.commands.render.ElkLayouter.PlacedGroup;
import net.rossonet.waldot.commands.render.ElkLayouter.PlacedVertex;

/**
 * Writes a laid out {@link RenderModel} as a standalone SVG document: CSS
 * classes for the light and dark themes, tooltips ({@code <title>}) with every
 * property and {@code data-node-id} attributes for the web consoles.
 */
public final class SvgGraphWriter {

	private static final String DARK = """
			.bg{fill:#0d1117}.fg{fill:#e6edf3}.muted{fill:#9198a1}
			.v-box{fill:#151b23}.v-sep{stroke:#3d444d}
			.e-line{stroke:#9198a1}.e-arrow{fill:#9198a1}.e-label-bg{fill:#0d1117}
			.group-box{fill:#10151c;stroke:#3d444d}
			""";

	private static final String LIGHT = """
			.bg{fill:#ffffff}.fg{fill:#1f2328}.muted{fill:#59636e}
			.v-box{fill:#f6f8fa}.v-sep{stroke:#d1d9e0}
			.e-line{stroke:#59636e}.e-arrow{fill:#59636e}.e-label-bg{fill:#ffffff}
			.group-box{fill:#f0f3f6;stroke:#d1d9e0}
			""";

	private static final String COMMON = """
			text{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,'DejaVu Sans Mono',monospace;font-size:12px;white-space:pre}
			.title{font-size:16px;font-weight:bold}
			.v-box{stroke-width:1.5}.v-title{font-weight:bold}.v-sub{font-style:italic}
			.ephemeral .v-box{stroke-dasharray:5 3}.highlight .v-box{stroke-width:3.5}
			.e-line{fill:none;stroke-width:1.2}.ephemeral .e-line{stroke-dasharray:5 3}
			.highlight .e-line{stroke-width:2.5}.e-label-bg{fill-opacity:0.85}
			.group-box{stroke-dasharray:4 2}.group-label{font-weight:bold}
			""";

	private static final double LEGEND_ROW = 22;

	private static final double MARGIN = 16;

	private static final double TITLE_HEIGHT = 32;

	private static String css(final GraphRenderOptions.Theme theme) {
		return switch (theme) {
		case LIGHT -> COMMON + LIGHT;
		case DARK -> COMMON + DARK;
		case AUTO -> COMMON + LIGHT + "@media (prefers-color-scheme: dark){" + DARK + "}";
		};
	}

	/** Escapes the text for XML, dropping the characters XML does not allow. */
	static String escape(final String text) {
		if (text == null) {
			return "";
		}
		final StringBuilder out = new StringBuilder(text.length() + 16);
		for (final char c : text.toCharArray()) {
			switch (c) {
			case '&' -> out.append("&amp;");
			case '<' -> out.append("&lt;");
			case '>' -> out.append("&gt;");
			case '"' -> out.append("&quot;");
			case '\'' -> out.append("&apos;");
			default -> {
				if (c >= 0x20 || c == '\n' || c == '\t') {
					out.append(c);
				}
			}
			}
		}
		return out.toString();
	}

	private static String num(final double value) {
		final double rounded = Math.round(value * 10) / 10.0;
		return rounded == Math.rint(rounded) ? String.valueOf((long) rounded)
				: String.format(Locale.ROOT, "%.1f", rounded);
	}

	private static void text(final StringBuilder svg, final double x, final double y, final String cssClass,
			final String text) {
		svg.append("<text x=\"").append(num(x)).append("\" y=\"").append(num(y)).append("\" class=\"")
				.append(cssClass).append("\">").append(escape(text)).append("</text>\n");
	}

	private static void tooltip(final StringBuilder svg, final String header, final List<RenderModel.Entry> entries) {
		svg.append("<title>").append(escape(header));
		for (final RenderModel.Entry entry : entries) {
			svg.append('\n').append(escape(entry.line()));
		}
		svg.append("</title>\n");
	}

	/** Writes the SVG document. */
	public static String write(final Drawing drawing, final RenderModel model, final GraphRenderOptions options) {
		final boolean tooltips = options.properties().tooltips();
		final Map<String, String> legend = options.style().legend() ? model.legend() : Map.of();
		final double top = model.title() == null ? 0 : TITLE_HEIGHT;
		double width = Math.max(drawing.width(), model.title() == null ? 0
				: model.title().length() * CHAR_WIDTH * 16 / FONT_SIZE + 2 * MARGIN);
		// legend: one row of entries, wrapped to the width of the drawing
		final double legendWidth = Math.max(width, 320) - 2 * MARGIN;
		int rows = 0;
		double cursor = Double.MAX_VALUE;
		for (final String key : legend.keySet()) {
			final double entry = 24 + key.length() * CHAR_WIDTH + 16;
			if (cursor + entry > legendWidth) {
				rows++;
				cursor = 0;
			}
			cursor += entry;
		}
		width = Math.max(width, legend.isEmpty() ? 0 : 320);
		final double legendHeight = rows == 0 ? 0 : rows * LEGEND_ROW + MARGIN;
		final double height = top + drawing.height() + legendHeight;

		final StringBuilder svg = new StringBuilder(4096 + drawing.vertices().size() * 512);
		svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"waldot-graph\" width=\"").append(num(width))
				.append("\" height=\"").append(num(height)).append("\" viewBox=\"0 0 ").append(num(width)).append(' ')
				.append(num(height)).append("\">\n");
		svg.append("<title>").append(escape(model.title() == null ? "WaldOT graph" : model.title()))
				.append("</title>\n");
		svg.append("<desc>").append(model.vertices().size()).append(" vertices, ").append(model.edges().size())
				.append(" edges</desc>\n");
		svg.append("<style>\n").append(css(options.style().theme())).append("</style>\n");
		svg.append("<defs><marker id=\"arrow\" viewBox=\"0 0 10 10\" refX=\"10\" refY=\"5\" markerWidth=\"8\" ")
				.append("markerHeight=\"8\" markerUnits=\"userSpaceOnUse\" orient=\"auto-start-reverse\">")
				.append("<path d=\"M0,0 L10,5 L0,10 z\" class=\"e-arrow\"/></marker></defs>\n");
		svg.append("<rect class=\"bg\" x=\"0\" y=\"0\" width=\"").append(num(width)).append("\" height=\"")
				.append(num(height)).append("\"/>\n");
		if (model.title() != null) {
			text(svg, MARGIN, 22, "fg title", model.title());
		}
		svg.append("<g transform=\"translate(0,").append(num(top)).append(")\">\n");
		for (final PlacedGroup group : drawing.groups()) {
			writeGroup(svg, group);
		}
		for (final PlacedEdge edge : drawing.edges()) {
			writeEdge(svg, edge, tooltips);
		}
		for (final PlacedVertex vertex : drawing.vertices()) {
			writeVertex(svg, vertex, tooltips);
		}
		svg.append("</g>\n");
		if (!legend.isEmpty()) {
			writeLegend(svg, legend, top + drawing.height(), legendWidth);
		}
		svg.append("</svg>\n");
		return svg.toString();
	}

	private static void writeEdge(final StringBuilder svg, final PlacedEdge placed, final boolean tooltips) {
		final RenderModel.Edge edge = placed.edge();
		svg.append("<g class=\"edge").append(edge.ephemeral() ? " ephemeral" : "")
				.append(edge.highlighted() ? " highlight" : "").append("\" id=\"").append(edge.id())
				.append("\" data-node-id=\"").append(escape(edge.nodeId())).append("\" data-source=\"")
				.append(edge.source()).append("\" data-target=\"").append(edge.target()).append("\">\n");
		if (tooltips) {
			tooltip(svg, edge.label(), edge.tooltip());
		}
		svg.append("<path class=\"e-line\" marker-end=\"url(#arrow)\" d=\"");
		for (int i = 0; i < placed.points().size(); i++) {
			final double[] point = placed.points().get(i);
			svg.append(i == 0 ? "M" : " L").append(num(point[0])).append(',').append(num(point[1]));
		}
		svg.append("\"/>\n");
		final Box label = placed.label();
		if (label != null) {
			svg.append("<rect class=\"e-label-bg\" x=\"").append(num(label.x())).append("\" y=\"")
					.append(num(label.y())).append("\" width=\"").append(num(label.width())).append("\" height=\"")
					.append(num(label.height())).append("\" rx=\"3\"/>\n");
			final List<String> lines = edge.lines();
			for (int i = 0; i < lines.size(); i++) {
				text(svg, label.x() + 3, label.y() + 13 + i * LINE_HEIGHT, i == 0 ? "fg" : "muted", lines.get(i));
			}
		}
		svg.append("</g>\n");
	}

	private static void writeGroup(final StringBuilder svg, final PlacedGroup group) {
		final Box box = group.box();
		svg.append("<g class=\"group\" data-group=\"").append(escape(group.name())).append("\">\n");
		svg.append("<rect class=\"group-box\" x=\"").append(num(box.x())).append("\" y=\"").append(num(box.y()))
				.append("\" width=\"").append(num(box.width())).append("\" height=\"").append(num(box.height()))
				.append("\" rx=\"6\"/>\n");
		text(svg, box.x() + 10, box.y() + 18, "muted group-label", group.name());
		svg.append("</g>\n");
	}

	private static void writeLegend(final StringBuilder svg, final Map<String, String> legend, final double top,
			final double legendWidth) {
		svg.append("<g class=\"legend\">\n");
		double x = MARGIN;
		double y = top;
		boolean first = true;
		for (final Map.Entry<String, String> entry : legend.entrySet()) {
			final double width = 24 + entry.getKey().length() * CHAR_WIDTH + 16;
			if (first || x - MARGIN + width > legendWidth) {
				x = MARGIN;
				y += LEGEND_ROW;
				first = false;
			}
			svg.append("<rect x=\"").append(num(x)).append("\" y=\"").append(num(y - 11))
					.append("\" width=\"14\" height=\"14\" rx=\"3\" fill=\"").append(entry.getValue())
					.append("\"/>\n");
			text(svg, x + 20, y, "fg", entry.getKey());
			x += width;
		}
		svg.append("</g>\n");
	}

	private static void writeVertex(final StringBuilder svg, final PlacedVertex placed, final boolean tooltips) {
		final RenderModel.Vertex vertex = placed.vertex();
		final Box box = placed.box();
		svg.append("<g class=\"vertex").append(vertex.ephemeral() ? " ephemeral" : "")
				.append(vertex.highlighted() ? " highlight" : "").append("\" id=\"").append(vertex.id())
				.append("\" data-node-id=\"").append(escape(vertex.nodeId())).append("\">\n");
		if (tooltips) {
			tooltip(svg, vertex.title(), vertex.tooltip());
		}
		final double header = 2 * LINE_HEIGHT + PADDING + SEPARATOR / 2;
		svg.append("<rect class=\"v-box\" x=\"").append(num(box.x())).append("\" y=\"").append(num(box.y()))
				.append("\" width=\"").append(num(box.width())).append("\" height=\"").append(num(box.height()))
				.append("\" rx=\"4\" stroke=\"").append(vertex.color()).append("\"/>\n");
		svg.append("<rect x=\"").append(num(box.x() + 1)).append("\" y=\"").append(num(box.y() + 1))
				.append("\" width=\"").append(num(box.width() - 2)).append("\" height=\"")
				.append(num(Math.min(header, box.height()) - 1)).append("\" rx=\"3\" fill=\"")
				.append(vertex.color()).append("\" fill-opacity=\"0.2\"/>\n");
		final double x = box.x() + PADDING;
		double y = box.y() + PADDING + FONT_SIZE;
		text(svg, x, y, "fg v-title", vertex.title());
		y += LINE_HEIGHT;
		text(svg, x, y, "muted v-sub", vertex.subtitle());
		if (!vertex.shown().isEmpty()) {
			final double line = box.y() + header;
			svg.append("<line class=\"v-sep\" x1=\"").append(num(box.x())).append("\" y1=\"").append(num(line))
					.append("\" x2=\"").append(num(box.x() + box.width())).append("\" y2=\"").append(num(line))
					.append("\"/>\n");
			y += SEPARATOR;
			for (final RenderModel.Entry entry : vertex.shown()) {
				y += LINE_HEIGHT;
				svg.append("<text x=\"").append(num(x)).append("\" y=\"").append(num(y))
						.append("\"><tspan class=\"muted\">").append(escape(entry.key()))
						.append(" = </tspan><tspan class=\"fg\">").append(escape(entry.value()))
						.append("</tspan></text>\n");
			}
		}
		svg.append("</g>\n");
	}

	private SvgGraphWriter() {
	}
}
