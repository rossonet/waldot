package net.rossonet.waldot.api.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiloStrategyKeyValuesTest {

	@Test
	void nullArrayAndLabelAreTolerated() {
		assertNull(MiloStrategy.getKeyValuesProperty(null, "a"));
		assertNull(MiloStrategy.getKeyValuesProperty(new Object[] { "a", "b" }, null));
		assertTrue(MiloStrategy.getIdValue((Object[]) null).isEmpty());
	}

	@Test
	void nullValueReturnsNull() {
		assertNull(MiloStrategy.getKeyValuesProperty(new Object[] { "a", null }, "a"));
		assertTrue(MiloStrategy.getIdValue("id", null).isEmpty());
	}

	@Test
	void oddLengthArrayDoesNotThrow() {
		assertNull(MiloStrategy.getKeyValuesProperty(new Object[] { "a", "b", "c" }, "c"));
		assertEquals("b", MiloStrategy.getKeyValuesProperty(new Object[] { "a", "b", "c" }, "a"));
	}

	@Test
	void idIsFound() {
		assertEquals("42", MiloStrategy.getIdValue("name", "x", MiloStrategy.ID_PARAMETER, "42").get());
	}
}
