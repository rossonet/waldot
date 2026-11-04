package net.rossonet.waldot.commands.script;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

/**
 * The {@code log} variable of a script run with {@code run script}: every line
 * goes to the console logger of the server and is also kept, to be returned to
 * the client. The methods mirror the most used ones of SLF4J ({@code info},
 * {@code warn}, {@code error}, {@code debug}, {@code trace}, with
 * {@code {}} placeholders).
 */
public final class ScriptLog {

	/** Maximum number of lines kept; the following are only counted. */
	public static final int MAX_LINES = 1000;

	private final Logger delegate;
	private final List<Map<String, Object>> lines = new ArrayList<>();
	private int dropped = 0;

	public ScriptLog(final Logger delegate) {
		this.delegate = delegate;
	}

	private synchronized void add(final String level, final String message) {
		if (lines.size() >= MAX_LINES) {
			dropped++;
			return;
		}
		final Map<String, Object> line = new LinkedHashMap<>();
		line.put("level", level);
		line.put("message", message);
		lines.add(line);
	}

	public void debug(final Object format, final Object... arguments) {
		final String text = format(String.valueOf(format), arguments);
		delegate.debug(text);
		add("debug", text);
	}

	/** Number of lines not kept because of {@link #MAX_LINES}. */
	public synchronized int dropped() {
		return dropped;
	}

	public void error(final Object format, final Object... arguments) {
		final String text = format(String.valueOf(format), arguments);
		delegate.error(text);
		add("error", text);
	}

	private static String format(final String format, final Object[] arguments) {
		if (arguments == null || arguments.length == 0) {
			return format;
		}
		return MessageFormatter.arrayFormat(format, arguments).getMessage();
	}

	public void info(final Object format, final Object... arguments) {
		final String text = format(String.valueOf(format), arguments);
		delegate.info(text);
		add("info", text);
	}

	public boolean isDebugEnabled() {
		return true;
	}

	public boolean isInfoEnabled() {
		return true;
	}

	public boolean isTraceEnabled() {
		return true;
	}

	/** Lines kept, in order. */
	public synchronized List<Map<String, Object>> lines() {
		return List.copyOf(lines);
	}

	@Override
	public String toString() {
		return "script log (" + delegate.getName() + ")";
	}

	public void trace(final Object format, final Object... arguments) {
		final String text = format(String.valueOf(format), arguments);
		delegate.trace(text);
		add("trace", text);
	}

	public void warn(final Object format, final Object... arguments) {
		final String text = format(String.valueOf(format), arguments);
		delegate.warn(text);
		add("warn", text);
	}
}
