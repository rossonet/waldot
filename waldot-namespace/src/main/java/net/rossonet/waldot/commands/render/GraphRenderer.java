package net.rossonet.waldot.commands.render;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.rossonet.waldot.api.exchange.GraphRenderOptions;
import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * Draws the graph of a namespace: selection ({@link GraphSelector}), layout
 * ({@link ElkLayouter}, SVG only) and output ({@link SvgGraphWriter},
 * {@link DotGraphWriter}).
 */
public final class GraphRenderer {

	/** A drawing ready to be sent. */
	public record Result(byte[] data, String format, int vertices, int edges, List<String> warnings) {
	}

	public static Result render(final WaldotNamespace namespace, final GraphRenderOptions options) {
		final RenderModel model = new GraphSelector(namespace, options).select();
		final List<String> warnings = new ArrayList<>(model.warnings());
		final String text;
		if (options.format() == GraphRenderOptions.Format.SVG) {
			final ElkLayouter.Drawing drawing = ElkLayouter.layout(model, options);
			warnings.addAll(drawing.warnings());
			text = SvgGraphWriter.write(drawing, model, options);
		} else {
			text = DotGraphWriter.write(model, options);
		}
		return new Result(text.getBytes(StandardCharsets.UTF_8), options.format().name().toLowerCase(Locale.ROOT),
				model.vertices().size(), model.edges().size(), warnings);
	}

	private GraphRenderer() {
	}
}
