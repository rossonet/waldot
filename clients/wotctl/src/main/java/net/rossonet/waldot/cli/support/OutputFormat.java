package net.rossonet.waldot.cli.support;

/**
 * Output formats ({@code -o}). The names are lower case because they are
 * shown as they are in the help and typed on the command line.
 */
public enum OutputFormat {
	/** one JSON document (an array for the lists) */
	json,
	/** one compact JSON object per line (JSON Lines) */
	jsonl,
	/** human readable text (default) */
	text,
	/** {@code <prefix>/<vertex>/<key> <payload>} lines (only subscribe) */
	topic,
	/** only the values, one per line */
	value
}
