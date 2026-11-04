package net.rossonet.waldot.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.rossonet.waldot.jexl.JexlErrorFormatter.JexlErrorInfo;

class JexlErrorFormatterTest {

	public static class Helper {
		public Number toNumber(final Object value) {
			return Double.valueOf(value.toString());
		}
	}

	private static WaldotJexlException fail(final String script) {
		return fail(script, Map.of());
	}

	private static WaldotJexlException fail(final String script, final Map<String, Object> variables) {
		final JexlExecutor executor = new JexlExecutor("test");
		final ClonableMapContext context = new ClonableMapContext(new HashMap<>(variables));
		return assertThrows(WaldotJexlException.class, () -> executor.execute(script, context));
	}

	/** The caret is under the given column of the line. */
	private static void assertCaretUnder(final JexlErrorInfo info, final String expectedTextAtCaret) {
		final int caret = info.caret().indexOf('^');
		assertTrue(caret >= 0, info.caret());
		assertTrue(info.excerpt().startsWith(expectedTextAtCaret, caret),
				"caret at " + caret + " in '" + info.excerpt() + "', expected '" + expectedTextAtCaret + "'");
	}

	@Test
	void callErrorReportsJavaCause() {
		final Map<String, Object> vars = new HashMap<>();
		vars.put("u", new Helper());
		final JexlErrorInfo info = fail("var b = u.toNumber('x')", vars).getInfo();
		assertEquals("call", info.kind());
		assertTrue(info.cause().contains("toNumber"), info.cause());
		assertTrue(info.cause().contains("NumberFormatException"), info.cause());
	}

	@Test
	void columnBeyondTheLineKeepsTheCaretAtTheEnd() {
		final String[] window = JexlErrorFormatter.window("abc", 50);
		assertEquals("abc", window[0]);
		assertEquals("   ^", window[1]);
	}

	@Test
	void errorAtTheStartOfTheLine() {
		final JexlErrorInfo info = fail("value + 1", nullValue()).getInfo();
		assertEquals(1, info.column());
		assertCaretUnder(info, "value");
	}

	@Test
	void errorInTheMiddleOfALongLineShowsAWindow() {
		final String prefix = "var a = 1; ".repeat(10);
		final String script = prefix + "var b = value + 2; " + "var c = 3; ".repeat(10);
		final JexlErrorInfo info = fail(script, nullValue()).getInfo();
		assertTrue(info.excerpt().startsWith("…"), info.excerpt());
		assertTrue(info.excerpt().endsWith("…"), info.excerpt());
		assertTrue(info.excerpt().length() <= 84, info.excerpt());
		assertCaretUnder(info, "value");
	}

	@Test
	void errorOfAnotherKindIsDescribedWithoutLocation() {
		final JexlErrorInfo info = JexlErrorFormatter.describe("x", new IllegalStateException("boom"));
		assertEquals("error", info.kind());
		assertEquals("IllegalStateException: boom", info.cause());
		assertEquals(0, info.line());
		assertEquals("IllegalStateException: boom", info.toMessage());
	}

	@Test
	void fixedArrayAddIsACallError() {
		final WaldotJexlException e = fail("var l = [1, 2]; l.add(3)");
		assertTrue(e.getMessage().contains("add"), e.getMessage());
	}

	@Test
	void messageHasNoJavaLocation() {
		final WaldotJexlException e = fail("var t = self.x + 1", Map.of("self", new HashMap<>()));
		assertFalse(e.getMessage().contains("JexlExecutor"), e.getMessage());
		assertFalse(e.getMessage().contains("@1:"), e.getMessage());
		assertTrue(e.getMessage().startsWith("riga 1, colonna 9: "), e.getMessage());
	}

	@Test
	void multiLineScriptReportsTheRightLine() {
		final JexlErrorInfo info = fail("var x = 1;\nvar y = 2;\nvar z = value + x;", nullValue()).getInfo();
		assertEquals(3, info.line());
		assertEquals("var z = value + x;", info.excerpt());
		assertCaretUnder(info, "value");
	}

	@Test
	void nonAsciiTextKeepsTheCaretAligned() {
		final JexlErrorInfo info = fail("var s = 'àèìòù°'; var z = value + 1", nullValue()).getInfo();
		assertCaretUnder(info, "value");
	}

	private Map<String, Object> nullValue() {
		final Map<String, Object> vars = new HashMap<>();
		vars.put("value", null);
		return vars;
	}

	@Test
	void nullOperandNamesTheExpression() {
		final JexlErrorInfo info = fail("var t = self.x + 1", Map.of("self", new HashMap<>())).getInfo();
		assertEquals("null-operand", info.kind());
		assertEquals("self.x", info.variable());
		assertTrue(info.cause().contains("'+'"), info.cause());
		assertTrue(info.cause().contains("self.x"), info.cause());
	}

	@Test
	void nullVariable() {
		final JexlErrorInfo info = fail("var y = value + 2", nullValue()).getInfo();
		assertEquals("variable-null", info.kind());
		assertEquals("value", info.variable());
		assertEquals("variabile 'value' nulla", info.cause());
		assertEquals(1, info.line());
		assertEquals(9, info.column());
	}

	@Test
	void originalExceptionIsTheCause() {
		final WaldotJexlException e = fail("var x = undefinedVar + 1");
		assertInstanceOf(org.apache.commons.jexl3.JexlException.class, e.getCause());
		assertEquals(e.getCause(), e.getJexlException());
	}

	@ParameterizedTest
	@ValueSource(strings = { "mod", "div", "eq", "ne", "lt", "gt", "le", "ge", "and", "or", "not" })
	void reservedWordAsVariableName(final String word) {
		final JexlErrorInfo info = fail("var x = 1;\nvar " + word + " = 3;\nx").getInfo();
		assertEquals("syntax", info.kind());
		assertEquals(2, info.line());
		assertEquals(word, info.variable());
		assertTrue(info.cause().contains("parola riservata"), info.cause());
	}

	@Test
	void syntaxError() {
		final JexlErrorInfo info = fail("var x = (1 + ;").getInfo();
		assertEquals("syntax", info.kind());
		assertTrue(info.cause().startsWith("errore di sintassi"), info.cause());
		assertNull(info.variable());
	}

	@Test
	void tabsBecomeSpaces() {
		final JexlErrorInfo info = fail("\tvar y =\tvalue + 2", nullValue()).getInfo();
		assertFalse(info.excerpt().contains("\t"));
		assertCaretUnder(info, "value");
	}

	@Test
	void toMessageHasLocationExcerptAndCaret() {
		final String message = fail("var y = value + 2", nullValue()).getMessage();
		assertEquals("riga 1, colonna 9: variabile 'value' nulla\n  var y = value + 2\n          ^", message);
	}

	@Test
	void undefinedProperty() {
		final JexlErrorInfo info = fail("x = 1; x.foo").getInfo();
		assertEquals("property", info.kind());
		assertEquals("foo", info.variable());
	}

	@Test
	void undefinedVariable() {
		final JexlErrorInfo info = fail("var x = undefinedVar + 1").getInfo();
		assertEquals("variable-undefined", info.kind());
		assertEquals("variabile 'undefinedVar' non definita", info.cause());
	}
}
