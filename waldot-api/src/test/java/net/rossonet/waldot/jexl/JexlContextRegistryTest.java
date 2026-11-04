package net.rossonet.waldot.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.jexl.JexlContextRegistry.Context;
import net.rossonet.waldot.jexl.JexlContextRegistry.Variable;

class JexlContextRegistryTest {

	@AfterEach
	void cleanup() {
		JexlContextRegistry.unregister("test-a");
		JexlContextRegistry.unregister("test-b");
	}

	@Test
	void contextIsImmutable() {
		final List<Variable> variables = new ArrayList<>(List.of(JexlContextRegistry.g()));
		final Context context = new Context("test-a", null, variables, false);
		variables.add(JexlContextRegistry.math());
		assertEquals(List.of("g"), context.variableNames());
		assertEquals("", context.description());
		assertThrows(UnsupportedOperationException.class, () -> context.variables().add(JexlContextRegistry.rand()));
	}

	@Test
	void registerGetAndSortedList() {
		JexlContextRegistry.register(new Context("test-b", "b", List.of(JexlContextRegistry.g()), false));
		JexlContextRegistry.register(new Context("test-a", "a", List.of(), true));
		assertTrue(JexlContextRegistry.get("test-a").orElseThrow().consoleCommands());
		final List<String> names = JexlContextRegistry.all().stream().map(Context::name).toList();
		assertTrue(names.indexOf("test-a") < names.indexOf("test-b"), names.toString());
	}

	@Test
	void registerReplacesTheContextWithTheSameName() {
		JexlContextRegistry.register(new Context("test-a", "first", List.of(), false));
		JexlContextRegistry.register(new Context("test-a", "second", List.of(JexlContextRegistry.math()), false));
		assertEquals("second", JexlContextRegistry.get("test-a").orElseThrow().description());
		assertEquals(1, JexlContextRegistry.all().stream().filter(c -> c.name().equals("test-a")).count());
	}

	@Test
	void unknownAndNull() {
		assertFalse(JexlContextRegistry.get("missing-context").isPresent());
		assertFalse(JexlContextRegistry.get(null).isPresent());
		assertThrows(NullPointerException.class, () -> JexlContextRegistry.register(null));
		assertThrows(NullPointerException.class, () -> new Context(null, "", List.of(), false));
		JexlContextRegistry.unregister(null);
	}

	@Test
	void variablesHaveDefaults() {
		final Variable v = new Variable("x", null, null);
		assertEquals("", v.type());
		assertEquals("", v.description());
		assertEquals("math", JexlContextRegistry.math().name());
		assertEquals("rand", JexlContextRegistry.rand().name());
		assertEquals("cmd", JexlContextRegistry.cmd().name());
	}
}
