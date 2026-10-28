package net.rossonet.waldot.cli.support;

import java.io.PrintWriter;
import java.util.List;
import java.util.function.Function;

/**
 * Writes the data of a command on the standard output in the chosen format.
 * Every line is flushed at once (the output can be read line by line by
 * another process); when the downstream process closed the pipe an
 * {@link OutputClosedException} stops the command cleanly.
 */
public final class Printer {

	private final OutputFormat format;
	private final PrintWriter out;

	public Printer(final PrintWriter out, final OutputFormat format) {
		this.out = out;
		this.format = format == null ? OutputFormat.text : format;
	}

	/** Prints a single object. */
	public <T> void single(final T item, final Function<? super T, Object> json,
			final Function<? super T, String> text, final Function<? super T, Object> value) {
		switch (format) {
		case json -> line(Json.pretty(Json.toNode(json.apply(item))));
		case jsonl -> line(Json.compact(Json.toNode(json.apply(item))));
		case value -> line(Json.scalar(value.apply(item)));
		default -> line(text.apply(item));
		}
	}

	/** Prints a list: a JSON array, a JSON line per item or a line per item. */
	public <T> void list(final List<T> items, final Function<? super T, Object> json,
			final Function<? super T, String> text, final Function<? super T, Object> value) {
		switch (format) {
		case json -> line(Json.pretty(Json.toNode(items.stream().map(item -> json.apply(item)).toList())));
		case jsonl -> items.forEach(item -> line(Json.compact(Json.toNode(json.apply(item)))));
		case value -> items.forEach(item -> line(Json.scalar(value.apply(item))));
		default -> items.forEach(item -> line(text.apply(item)));
		}
	}

	public OutputFormat format() {
		return format;
	}

	/**
	 * Writes a line and flushes it.
	 *
	 * @throws OutputClosedException if the standard output was closed
	 */
	public synchronized void line(final String text) {
		out.print(text == null ? "" : text);
		out.print('\n');
		out.flush();
		if (out.checkError()) {
			throw new OutputClosedException();
		}
	}
}
