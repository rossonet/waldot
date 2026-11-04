package net.rossonet.waldot.jexl;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlInfo;

/**
 * Turns a JEXL exception into a message that points into the user's script:
 * line, column, the cause in plain words and an excerpt of the line with a
 * caret under the column.
 *
 * <pre>
 * riga 2, colonna 9: variabile 'value' nulla
 *   var y = value + 2;
 *           ^
 * </pre>
 *
 * The Java location that JEXL adds in debug mode (for example
 * {@code JexlExecutor.execute:84@1:153}) never reaches the message.
 */
public final class JexlErrorFormatter {

	/**
	 * Structured description of a JEXL error.
	 *
	 * @param kind     category: {@code variable-null}, {@code variable-undefined},
	 *                 {@code property}, {@code method}, {@code syntax},
	 *                 {@code null-operand}, {@code call}, {@code error}
	 * @param cause    the cause in plain words, without location
	 * @param line     line in the script (1-based), 0 if unknown
	 * @param column   column in the line (1-based), 0 if unknown
	 * @param excerpt  the text of the line around the column, empty if unknown
	 * @param caret    spaces and a {@code ^} under the column of the excerpt
	 * @param variable the variable, property or method involved, may be null
	 */
	public record JexlErrorInfo(String kind, String cause, int line, int column, String excerpt, String caret,
			String variable) {

		/** Multi-line message: location and cause, then excerpt and caret. */
		public String toMessage() {
			final StringBuilder sb = new StringBuilder();
			if (line > 0) {
				sb.append("riga ").append(line).append(", colonna ").append(column).append(": ");
			}
			sb.append(cause);
			if (!excerpt.isEmpty()) {
				sb.append('\n').append(INDENT).append(excerpt).append('\n').append(INDENT).append(caret);
			}
			return sb.toString();
		}
	}

	private static final String ELLIPSIS = "…";

	private static final int HALF_WINDOW = 40;

	private static final String INDENT = "  ";

	private static final Pattern LOCATION_PREFIX = Pattern.compile("^[^\\s@]*@\\d+(?::\\d+)*\\s*");

	private static final Pattern NULL_OPERAND = Pattern.compile("JEXL error : (\\S+) error caused by null operand");

	private static final Pattern OPERAND_AT_COLUMN = Pattern
			.compile("[A-Za-z_$][\\w$]*(?:\\s*\\.\\s*[A-Za-z_$][\\w$]*|\\[[^\\]]*\\]|\\([^()]*\\))*");

	private static final Pattern PARSING_TOKEN = Pattern.compile("parsing error in '([^']*)'");

	private static final Pattern RESERVED_DECLARATION = Pattern.compile("\\b(?:var|let|const)\\s+([A-Za-z_$][\\w$]*)");

	/**
	 * Words that JEXL reserves as operators or keywords and that cannot be used as
	 * variable names.
	 */
	public static final Set<String> RESERVED_WORDS = Set.of("and", "or", "not", "eq", "ne", "lt", "gt", "le", "ge",
			"div", "mod", "empty", "size", "new", "return", "function", "var", "let", "const", "if", "else", "for",
			"while", "do", "break", "continue", "null", "true", "false", "NaN");

	private static final Pattern GENERIC_ERROR = Pattern.compile("^JEXL error : ");

	/**
	 * Describes a JEXL exception thrown by a script.
	 *
	 * @param source the script source, used for the excerpt; may be null
	 * @param error  the exception (a {@link JexlException} or anything else)
	 * @return the structured description, never null
	 */
	public static JexlErrorInfo describe(final String source, final Throwable error) {
		final JexlException jexl = findJexlException(error);
		if (jexl == null) {
			final String message = error == null ? "errore sconosciuto" : plain(error);
			return new JexlErrorInfo("error", message, 0, 0, "", "", null);
		}
		final JexlInfo info = jexl.getInfo();
		final int line = info == null ? 0 : info.getLine();
		final int column = info == null ? 0 : info.getColumn();
		final String lineText = lineText(source, line);
		String kind;
		String cause;
		String variable = null;
		final String raw = stripLocation(rawMessage(jexl));
		if (jexl instanceof final JexlException.Variable v) {
			variable = v.getVariable();
			if (v.isUndefined()) {
				kind = "variable-undefined";
				cause = "variabile '" + variable + "' non definita";
			} else {
				kind = "variable-null";
				cause = "variabile '" + variable + "' nulla";
			}
		} else if (jexl instanceof final JexlException.Property p) {
			variable = p.getProperty();
			kind = "property";
			cause = "proprietà '" + variable + "' non definita";
		} else if (jexl instanceof final JexlException.Method m) {
			variable = m.getMethod();
			kind = "method";
			cause = "metodo '" + variable + "' non trovato o con argomenti non compatibili";
		} else if (jexl instanceof JexlException.Parsing || jexl instanceof JexlException.Tokenization
				|| jexl instanceof JexlException.Ambiguous || jexl instanceof JexlException.Assignment
				|| jexl instanceof JexlException.Feature) {
			kind = "syntax";
			final Matcher token = PARSING_TOKEN.matcher(raw);
			cause = token.find() ? "errore di sintassi vicino a '" + token.group(1) + "'" : "errore di sintassi: " + raw;
			final String reserved = reservedDeclaration(lineText, column);
			if (reserved != null) {
				variable = reserved;
				cause += " ('" + reserved + "' è una parola riservata di JEXL: usa un altro nome)";
			}
		} else {
			final Matcher nullOperand = NULL_OPERAND.matcher(raw);
			if (nullOperand.find()) {
				kind = "null-operand";
				final String operand = operandAt(lineText, column);
				variable = operand;
				cause = "operatore '" + nullOperand.group(1) + "' con un operando nullo"
						+ (operand == null ? "" : " (espressione che inizia con '" + operand + "')");
			} else if (jexl.getCause() != null) {
				kind = "call";
				final String what = GENERIC_ERROR.matcher(raw).replaceFirst("");
				variable = what.isBlank() ? null : what;
				cause = (what.isBlank() ? "errore" : "errore nella chiamata di '" + what + "'") + ": "
						+ plain(jexl.getCause());
			} else {
				kind = "error";
				cause = GENERIC_ERROR.matcher(raw).replaceFirst("");
				if (cause.isBlank()) {
					cause = jexl.getClass().getSimpleName();
				}
			}
		}
		String excerpt = "";
		String caret = "";
		if (lineText != null) {
			final String[] window = window(lineText, column);
			excerpt = window[0];
			caret = window[1];
		}
		return new JexlErrorInfo(kind, cause, line, column, excerpt, caret, variable);
	}

	/**
	 * Formats a JEXL exception as a multi-line message (see
	 * {@link JexlErrorInfo#toMessage()}).
	 */
	public static String format(final String source, final Throwable error) {
		return describe(source, error).toMessage();
	}

	private static JexlException findJexlException(final Throwable error) {
		Throwable current = error;
		int depth = 0;
		while (current != null && depth++ < 10) {
			if (current instanceof final WaldotJexlException w) {
				return w.getJexlException();
			}
			if (current instanceof final JexlException j) {
				return j;
			}
			current = current.getCause();
		}
		return null;
	}

	private static String lineText(final String source, final int line) {
		if (source == null || line <= 0) {
			return null;
		}
		final String[] lines = source.split("\r\n|\r|\n", -1);
		return line <= lines.length ? lines[line - 1] : null;
	}

	private static String operandAt(final String lineText, final int column) {
		if (lineText == null || column <= 0 || column > lineText.length()) {
			return null;
		}
		final Matcher m = OPERAND_AT_COLUMN.matcher(lineText);
		m.region(column - 1, lineText.length());
		return m.lookingAt() ? m.group().replaceAll("\\s+", "") : null;
	}

	private static String plain(final Throwable error) {
		final String message = error.getMessage();
		final String name = error.getClass().getSimpleName();
		return message == null || message.isBlank() ? name : name + ": " + message;
	}

	private static String rawMessage(final JexlException jexl) {
		final String message = jexl.getMessage();
		return message == null ? "" : message;
	}

	private static String reservedDeclaration(final String lineText, final int column) {
		if (lineText == null) {
			return null;
		}
		final Matcher m = RESERVED_DECLARATION.matcher(lineText);
		final int from = Math.max(0, column - 1);
		while (m.find()) {
			if (m.end() > from && RESERVED_WORDS.contains(m.group(1).toLowerCase(Locale.ROOT))) {
				return m.group(1);
			}
		}
		return null;
	}

	private static String stripLocation(final String message) {
		return LOCATION_PREFIX.matcher(message.strip()).replaceFirst("");
	}

	/**
	 * Returns the excerpt of the line around the column and the caret line. Tabs
	 * become single spaces so that the caret stays aligned.
	 */
	static String[] window(final String lineText, final int column) {
		final String text = lineText.replace('\t', ' ');
		final int index = Math.max(0, Math.min(text.length(), column - 1));
		int start = 0;
		int end = text.length();
		if (text.length() > 2 * HALF_WINDOW) {
			start = Math.max(0, index - HALF_WINDOW);
			end = Math.min(text.length(), index + HALF_WINDOW);
		}
		final StringBuilder excerpt = new StringBuilder();
		int caretOffset = index - start;
		if (start > 0) {
			excerpt.append(ELLIPSIS);
			caretOffset += ELLIPSIS.length();
		}
		excerpt.append(text, start, end);
		if (end < text.length()) {
			excerpt.append(ELLIPSIS);
		}
		final String excerptText = excerpt.toString().stripTrailing();
		return new String[] { excerptText, " ".repeat(Math.max(0, caretOffset)) + "^" };
	}

	private JexlErrorFormatter() {
	}
}
