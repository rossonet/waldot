package net.rossonet.waldot.jexl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;
import org.apache.commons.jexl3.JexlScript;

import net.rossonet.waldot.jexl.JexlErrorFormatter.JexlErrorInfo;

/**
 * Checks a JEXL script for a context without running it: compilation errors
 * with excerpt, variables that the context does not provide, and a few static
 * checks on the traps of the language (fixed arrays modified with
 * {@code add}, numbers that OPC UA delivers as strings).
 */
public final class JexlChecker {

	/**
	 * A problem found in the script.
	 *
	 * @param kind     category (see {@link JexlErrorInfo#kind()}, plus
	 *                 {@code fixed-array}, {@code string-number})
	 * @param message  description, with location when known
	 * @param line     line (1-based), 0 if unknown
	 * @param column   column (1-based), 0 if unknown
	 * @param excerpt  text of the line around the column
	 * @param caret    caret under the column
	 * @param variable variable involved, may be null
	 */
	public record Problem(String kind, String message, int line, int column, String excerpt, String caret,
			String variable) {

		static Problem of(final JexlErrorInfo info) {
			return new Problem(info.kind(), info.cause(), info.line(), info.column(), info.excerpt(), info.caret(),
					info.variable());
		}

		/** Map for the JSON output. */
		public Map<String, Object> toMap() {
			final Map<String, Object> map = new LinkedHashMap<>();
			map.put("kind", kind);
			map.put("message", message);
			if (line > 0) {
				map.put("line", line);
				map.put("column", column);
				map.put("excerpt", excerpt);
				map.put("caret", caret);
			}
			if (variable != null) {
				map.put("variable", variable);
			}
			return map;
		}
	}

	/**
	 * Result of a check.
	 *
	 * @param context   name of the context
	 * @param errors    problems that make the script fail
	 * @param warnings  probable mistakes
	 * @param available variables provided by the context (without the console
	 *                  commands)
	 * @param used      free variables used by the script (roots only)
	 * @param locals    variables declared by the script
	 */
	public record Result(String context, List<Problem> errors, List<Problem> warnings, List<String> available,
			List<String> used, List<String> locals) {

		/** True if there are no errors. */
		public boolean ok() {
			return errors.isEmpty();
		}

		/** Map for the JSON output. */
		public Map<String, Object> toMap() {
			final Map<String, Object> map = new LinkedHashMap<>();
			map.put("ok", ok());
			map.put("context", context);
			map.put("errors", errors.stream().map(Problem::toMap).toList());
			map.put("warnings", warnings.stream().map(Problem::toMap).toList());
			map.put("available", available);
			map.put("used", used);
			map.put("locals", locals);
			return map;
		}
	}

	private static final Pattern FIXED_ARRAY = Pattern
			.compile("(?:\\b(?:var|let|const)\\s+|^\\s*|;\\s*)([A-Za-z_$][\\w$]*)\\s*=\\s*\\[([^\\[\\]]*)\\]");

	private static final String NUMBER_HINT = "può arrivare da OPC UA come stringa: per i numeri usa "
			+ "cmd.toNumber(%1$s).doubleValue(), per i booleani (''+%1$s)=='true'";

	/**
	 * Checks a script.
	 *
	 * @param engine          engine used to compile (the WaldOT engine of
	 *                        {@link JexlExecutor#generateEngine()})
	 * @param context         descriptor of the context
	 * @param consoleCommands names of the console commands, used when the context
	 *                        provides them
	 * @param source          the script
	 * @return the result, never null
	 */
	public static Result check(final JexlEngine engine, final JexlContextRegistry.Context context,
			final Collection<String> consoleCommands, final String source) {
		final List<Problem> errors = new ArrayList<>();
		final List<Problem> warnings = new ArrayList<>();
		final List<String> available = context.variableNames();
		final Set<String> known = new LinkedHashSet<>(available);
		if (context.consoleCommands() && consoleCommands != null) {
			known.addAll(consoleCommands);
		}
		final String text = source == null ? "" : source;
		JexlScript script = null;
		try {
			script = engine.createScript(new JexlInfo(context.name(), 1, 1), text);
		} catch (final JexlException e) {
			errors.add(Problem.of(JexlErrorFormatter.describe(text, e)));
		}
		final Set<String> used = new TreeSet<>();
		final Set<String> locals = new TreeSet<>();
		if (script != null) {
			for (final List<String> path : script.getVariables()) {
				if (!path.isEmpty()) {
					used.add(path.get(0));
				}
			}
			final String[] declared = script.getLocalVariables();
			if (declared != null) {
				locals.addAll(List.of(declared));
			}
			final String[] parameters = script.getParameters();
			if (parameters != null) {
				locals.addAll(List.of(parameters));
			}
			for (final String variable : used) {
				if (!known.contains(variable) && !locals.contains(variable)) {
					if (Pattern.compile("(?<![\\w$.])" + Pattern.quote(variable) + "\\s*=(?!=)").matcher(text)
							.find()) {
						warnings.add(at(text, "global-assignment", variable, "'" + variable
								+ "' è assegnata senza var: diventa una variabile globale del contesto (nella console resta"
								+ " tra una query e l'altra); dichiarala con var"));
						continue;
					}
					errors.add(at(text, "variable-undefined", variable,
							"variabile '" + variable + "' non disponibile nel contesto '" + context.name() + "'"));
				}
			}
			fixedArrays(text, warnings);
			stringNumbers(text, context, used, warnings);
		}
		return new Result(context.name(), List.copyOf(errors), List.copyOf(warnings), available, List.copyOf(used),
				List.copyOf(locals));
	}

	/** Problem at the first free occurrence of a name. */
	private static Problem at(final String source, final String kind, final String name, final String message) {
		final Pattern p = Pattern.compile("(?<![\\w$.])" + Pattern.quote(name) + "(?![\\w$])");
		final String[] lines = source.split("\r\n|\r|\n", -1);
		for (int i = 0; i < lines.length; i++) {
			final Matcher m = p.matcher(lines[i]);
			while (m.find()) {
				if (!insideString(lines[i], m.start())) {
					final String[] window = JexlErrorFormatter.window(lines[i], m.start() + 1);
					return new Problem(kind, message, i + 1, m.start() + 1, window[0], window[1], name);
				}
			}
		}
		return new Problem(kind, message, 0, 0, "", "", name);
	}

	private static void fixedArrays(final String source, final List<Problem> warnings) {
		final Matcher m = FIXED_ARRAY.matcher(source);
		final Set<String> reported = new LinkedHashSet<>();
		while (m.find()) {
			final String name = m.group(1);
			final String content = m.group(2).strip();
			if (content.endsWith("...") || reported.contains(name)) {
				continue;
			}
			final Pattern change = Pattern.compile("(?<![\\w$.])" + Pattern.quote(name)
					+ "\\s*\\.\\s*(add|addAll|remove|removeIf|clear|set)\\s*\\(");
			final Matcher c = change.matcher(source);
			if (c.find(m.end())) {
				reported.add(name);
				warnings.add(at(source, "fixed-array", name, "'" + name
						+ "' è un array fisso ([a, b]): per una lista modificabile scrivi [a, b, ...] (o [...] se vuota)"));
			}
		}
	}

	private static boolean insideString(final String line, final int index) {
		boolean single = false;
		boolean dbl = false;
		for (int i = 0; i < index; i++) {
			final char ch = line.charAt(i);
			if (ch == '\\') {
				i++;
			} else if (ch == '\'' && !dbl) {
				single = !single;
			} else if (ch == '"' && !single) {
				dbl = !dbl;
			}
		}
		return single || dbl;
	}

	private static void stringNumbers(final String source, final JexlContextRegistry.Context context,
			final Set<String> used, final List<Problem> warnings) {
		if (!used.contains("value") || !context.variableNames().contains("value")) {
			return;
		}
		final Pattern arithmetic = Pattern.compile(
				"(?<![\\w$.])value\\s*(?:[-+*/%<>]|==|!=)|(?:[-+*/%<>]|==|!=)\\s*value(?![\\w$(])");
		if (arithmetic.matcher(source).find() && !source.matches("(?s).*toNumber\\s*\\(\\s*value.*")) {
			warnings.add(at(source, "string-number", "value", "'value' " + String.format(NUMBER_HINT, "value")));
		}
	}

	private JexlChecker() {
	}
}
