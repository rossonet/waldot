package net.rossonet.waldot.client.api.graph;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A drawing of the graph produced by {@code render graph}: an SVG document or
 * a Graphviz DOT text, already decompressed.
 *
 * @param data     the document (UTF-8)
 * @param format   {@code svg} or {@code dot}
 * @param vertices number of drawn vertices
 * @param edges    number of drawn edges
 * @param warnings notes of the server (truncated selection, omitted edges,
 *                 ignored options)
 * @param blocks   number of blocks used by the transfer
 */
public record GraphRendering(byte[] data, String format, int vertices, int edges, List<String> warnings,
		int blocks) {

	public GraphRendering {
		data = data == null ? new byte[0] : data.clone();
		warnings = warnings == null ? List.of() : List.copyOf(warnings);
	}

	/** @return the document as text */
	public String asString() {
		return new String(data, StandardCharsets.UTF_8);
	}

	/** @return the document (a copy) */
	@Override
	public byte[] data() {
		return data.clone();
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof GraphRendering rendering && format.equals(rendering.format)
				&& vertices == rendering.vertices && edges == rendering.edges && blocks == rendering.blocks
				&& warnings.equals(rendering.warnings) && java.util.Arrays.equals(data, rendering.data);
	}

	/** @return true if the server reported warnings */
	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}

	@Override
	public int hashCode() {
		return java.util.Arrays.hashCode(data) * 31 + vertices * 7 + edges;
	}

	/** @return size of the document in bytes */
	public int size() {
		return data.length;
	}

	@Override
	public String toString() {
		return "GraphRendering[format=" + format + ", size=" + data.length + ", vertices=" + vertices + ", edges="
				+ edges + ", warnings=" + warnings.size() + ", blocks=" + blocks + "]";
	}
}
