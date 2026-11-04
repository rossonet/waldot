package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_JSON;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_TAG;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.MANAGE_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.RUN_SCRIPT_COMMAND;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.changes.GraphChangeDiff;
import net.rossonet.waldot.api.changes.GraphChangeRecorder;
import net.rossonet.waldot.api.changes.GraphChangeRecorder.Change;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.commands.script.ScriptLog;
import net.rossonet.waldot.commands.script.ScriptResultValue;
import net.rossonet.waldot.jexl.JexlErrorFormatter;
import net.rossonet.waldot.jexl.JexlErrorFormatter.JexlErrorInfo;

/**
 * Runs a JEXL script of several statements in a copy of the console context
 * and returns, as JSON:
 * <ul>
 * <li>{@code ok} and {@code result} (the value of the last expression);</li>
 * <li>{@code log}: the lines written with {@code log.info/warn/...};</li>
 * <li>{@code diff}: vertices and edges created and removed, properties changed,
 * grouped by type (see {@link GraphChangeDiff});</li>
 * <li>{@code duration_ms};</li>
 * <li>{@code error} on failure: kind, message, line, column, excerpt.</li>
 * </ul>
 * The variables assigned by the script do not remain in the console. With the
 * input {@code tag} every vertex and edge created by the script gets the
 * property {@code created-by} with that value.
 */
public class RunScriptCommand extends AbstractExchangeCommand {

	/** Property set on the elements created by a script run with a tag. */
	public static final String CREATED_BY_PROPERTY = "created-by";

	private static final ObjectMapper JSON = new ObjectMapper();

	public RunScriptCommand(final WaldotNamespace namespace) {
		super(namespace, RUN_SCRIPT_COMMAND,
				"run a JEXL script (several statements) and return result, log lines and diff of the graph as JSON",
				MANAGE_DIRECTORY, new String[] { ARG_COMMAND, ARG_TAG }, new String[] { ARG_JSON, ARG_ERROR });
	}

	@Override
	public Object clone() {
		return new RunScriptCommand(waldotNamespace);
	}

	/**
	 * Runs the script and builds the report (also used by the tests).
	 */
	public Map<String, Object> run(final String script, final String tag) {
		final ConsoleStrategy console = getNamespace().getConsoleStrategy();
		final ScriptLog log = new ScriptLog(getNamespace().getConsoleLogger());
		final Map<String, Object> report = new LinkedHashMap<>();
		final long start = System.nanoTime();
		Object result = null;
		Throwable failure = null;
		final List<Change> changes;
		final boolean truncated;
		try (GraphChangeRecorder.Recording recording = GraphChangeRecorder.start()) {
			try {
				result = console.runIsolated(script, Map.of(ConsoleStrategy.LOG_LABEL, log));
			} catch (final Throwable e) {
				failure = e;
			}
			changes = recording.changes();
			truncated = recording.isTruncated();
		}
		final Map<String, Object> diff = GraphChangeDiff.diff(changes, truncated);
		if (tag != null && !tag.isBlank()) {
			tag(changes, diff, tag.strip());
		}
		report.put("ok", failure == null);
		if (failure == null) {
			report.put("result", ScriptResultValue.toJson(result));
		} else {
			final JexlErrorInfo info = JexlErrorFormatter.describe(script, failure);
			final Map<String, Object> error = new LinkedHashMap<>();
			error.put("kind", info.kind());
			error.put("message", info.toMessage());
			if (info.line() > 0) {
				error.put("line", info.line());
				error.put("column", info.column());
				error.put("excerpt", info.excerpt());
				error.put("caret", info.caret());
			}
			if (info.variable() != null) {
				error.put("variable", info.variable());
			}
			report.put("error", error);
		}
		report.put("log", log.lines());
		if (log.dropped() > 0) {
			report.put("log_dropped", log.dropped());
		}
		report.put("diff", diff);
		report.put("duration_ms", (System.nanoTime() - start) / 1_000_000L);
		return report;
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] output = newOutput();
		try {
			final String script = CommandArguments.requireFirst(inputValues, ARG_COMMAND);
			final Map<String, Object> report = run(script, stringArgument(inputValues, 1));
			output[0] = JSON.writeValueAsString(report);
			if (!Boolean.TRUE.equals(report.get("ok"))) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> error = (Map<String, Object>) report.get("error");
				output[1] = String.valueOf(error.get("message"));
			}
		} catch (final Throwable e) {
			output[1] = CommandArguments.errorMessage(e);
		}
		return output;
	}

	/** Sets created-by on the vertices and edges that the diff reports as created. */
	@SuppressWarnings("unchecked")
	private static void tag(final List<Change> changes, final Map<String, Object> diff, final String tag) {
		final java.util.Set<String> created = new java.util.HashSet<>();
		for (final String what : new String[] { "vertices", "edges" }) {
			for (final Map<String, Object> m : (List<Map<String, Object>>) ((Map<String, Object>) diff.get(what))
					.get("created")) {
				created.add((what.equals("vertices") ? "vertex" : "edge") + "\u0000" + m.get("id"));
			}
		}
		for (final Change c : changes) {
			if (c.ref() != null && (c.kind().equals("vertex-added") || c.kind().equals("edge-added"))
					&& created.remove(c.target() + "\u0000" + c.element())) {
				try {
					c.ref().property(CREATED_BY_PROPERTY, tag);
				} catch (final RuntimeException e) {
					// element removed meanwhile
				}
			}
		}
	}
}
