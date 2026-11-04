package net.rossonet.waldot.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.commons.jexl3.JexlEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.rossonet.waldot.jexl.JexlChecker.Problem;
import net.rossonet.waldot.jexl.JexlChecker.Result;
import net.rossonet.waldot.jexl.JexlContextRegistry.Context;
import net.rossonet.waldot.jexl.JexlContextRegistry.Variable;

class JexlCheckerTest {

	private static final JexlEngine ENGINE = JexlExecutor.generateEngine();

	private static final Context CONSOLE = new Context("console", "console",
			List.of(JexlContextRegistry.g(), JexlContextRegistry.graph(), JexlContextRegistry.cmd(),
					JexlContextRegistry.log("log"), JexlContextRegistry.math(), JexlContextRegistry.rand()),
			true);

	private static final Context ACTION = new Context("iec104-command", "azione",
			List.of(JexlContextRegistry.g(), JexlContextRegistry.self("comando"), new Variable("value", "Object", "")),
			false);

	private static Result check(final Context context, final String script) {
		return JexlChecker.check(ENGINE, context, List.of("about", "help"), script);
	}

	private static Problem only(final List<Problem> problems, final String kind) {
		final List<Problem> found = problems.stream().filter(p -> p.kind().equals(kind)).toList();
		assertEquals(1, found.size(), problems.toString());
		return found.get(0);
	}

	@Test
	void consoleCommandsAreVariablesOnlyWhenTheContextSaysSo() {
		assertTrue(check(CONSOLE, "about").ok());
		final Result action = check(ACTION, "about");
		assertFalse(action.ok());
		assertEquals("about", only(action.errors(), "variable-undefined").variable());
	}

	@Test
	void fixedArrayWithAddIsAWarning() {
		final Result r = check(CONSOLE, "var l = [1, 2];\nl.add(3);");
		assertTrue(r.ok());
		final Problem p = only(r.warnings(), "fixed-array");
		assertEquals("l", p.variable());
		assertTrue(p.message().contains("[a, b, ...]"), p.message());
	}

	@Test
	void growableArrayHasNoWarning() {
		assertTrue(check(CONSOLE, "var l = [1, 2, ...];\nl.add(3);").warnings().isEmpty());
		assertTrue(check(CONSOLE, "var l = [1, 2];\nl.size()").warnings().isEmpty());
	}

	@Test
	void globalAssignmentIsAWarningNotAnError() {
		final Result r = check(CONSOLE, "x = 1; x + 1");
		assertTrue(r.ok(), r.errors().toString());
		assertEquals("x", only(r.warnings(), "global-assignment").variable());
	}

	@Test
	void localsAndParametersAreKnown() {
		final Result r = check(CONSOLE, "var f = function(a) { var z = a * 2; z }; f(3)");
		assertTrue(r.ok(), r.errors().toString());
		assertTrue(r.locals().contains("f"), r.locals().toString());
	}

	@Test
	void mapOutputHasAllFields() {
		final Map<String, Object> map = check(ACTION, "var x = missing + 1").toMap();
		assertEquals(false, map.get("ok"));
		assertEquals("iec104-command", map.get("context"));
		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> errors = (List<Map<String, Object>>) map.get("errors");
		assertEquals("variable-undefined", errors.get(0).get("kind"));
		assertEquals(1, errors.get(0).get("line"));
		assertEquals(9, errors.get(0).get("column"));
		assertEquals("        ^", errors.get(0).get("caret"));
	}

	@Test
	void missingVariableHasTheLocationOfItsFirstUse() {
		final Result r = check(ACTION, "var a = 1;\nvar b = 'cmd';\nvar c = cmd.toNumber(value);");
		final Problem p = only(r.errors(), "variable-undefined");
		assertEquals("cmd", p.variable());
		assertEquals(3, p.line());
		assertEquals(9, p.column());
		assertTrue(p.message().contains("iec104-command"), p.message());
	}

	@Test
	void numberFromOpcUaIsAWarningOnlyWithoutToNumber() {
		final Result plain = check(ACTION, "self.property('setpoint', value * 2)");
		assertEquals("value", only(plain.warnings(), "string-number").variable());
		final Context withCmd = new Context("x", "",
				List.of(JexlContextRegistry.cmd(), JexlContextRegistry.self(""), new Variable("value", "", "")), false);
		assertTrue(check(withCmd, "self.property('setpoint', cmd.toNumber(value).doubleValue() * 2)").warnings()
				.isEmpty());
	}

	@Test
	void numberWarningNeedsAValueVariable() {
		assertTrue(check(CONSOLE, "var value = 1; value * 2").warnings().isEmpty());
	}

	@Test
	void propertyPathsReportOnlyTheRoot() {
		final Result r = check(ACTION, "self.property('a').value() + g.V().count().next()");
		assertTrue(r.ok(), r.errors().toString());
		assertEquals(List.of("g", "self"), r.used());
	}

	@ParameterizedTest
	@ValueSource(strings = { "mod", "div", "eq", "ne", "lt", "gt", "le", "ge", "and", "or", "not" })
	void reservedWordsAreSyntaxErrors(final String word) {
		final Result r = check(CONSOLE, "var " + word + " = 3; " + "1");
		assertFalse(r.ok());
		final Problem p = only(r.errors(), "syntax");
		assertEquals(word, p.variable());
		assertTrue(p.message().contains("parola riservata"), p.message());
	}

	@Test
	void stringsDoNotCountAsUses() {
		final Result r = check(ACTION, "var s = 'missing'; var t = missing;");
		final Problem p = only(r.errors(), "variable-undefined");
		assertEquals(28, p.column());
	}

	@Test
	void syntaxErrorHasExcerpt() {
		final Result r = check(CONSOLE, "var x = (1 + ;");
		final Problem p = only(r.errors(), "syntax");
		assertEquals(1, p.line());
		assertFalse(p.excerpt().isEmpty());
	}

	@Test
	void validScript() {
		final Result r = check(CONSOLE, "var x = math.max(1, 2) + rand.nextInt(3);\nlog.info('x ' + x);\nx");
		assertTrue(r.ok(), r.errors().toString());
		assertTrue(r.warnings().isEmpty(), r.warnings().toString());
		assertEquals(List.of("log", "math", "rand"), r.used());
		assertEquals(List.of("x"), r.locals());
	}
}
