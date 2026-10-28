package net.rossonet.waldot.client.api.graph;

/**
 * Result of {@code import graph} or {@code clear graph}.
 *
 * @param vertices number of vertices created (import) or removed (clear)
 * @param edges    number of edges created (import) or removed (clear)
 * @param warnings elements not restored (empty if none)
 */
public record GraphChangeResult(int vertices, int edges, String warnings) {

	public GraphChangeResult {
		warnings = warnings == null ? "" : warnings;
	}

	/** @return true if some elements were not restored */
	public boolean hasWarnings() {
		return !warnings.isBlank();
	}
}
