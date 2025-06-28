package net.rossonet.waldot.api.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ClonableMapContextTest {

	@Test
	void testClear() {
		final ClonableMapContext context = new ClonableMapContext();
		context.set("key", "value");
		assertTrue(context.has("key"));

		context.clear();
		assertFalse(context.has("key"));
	}

	@Test
	void testCopyConstructor() {
		final ClonableMapContext original = new ClonableMapContext();
		original.set("key", "value");

		final ClonableMapContext copy = new ClonableMapContext(original);
		assertNotNull(copy);
		assertTrue(copy.has("key"));
		assertEquals("value", copy.get("key"));
	}

	@Test
	void testDefaultConstructor() {
		final ClonableMapContext context = new ClonableMapContext();
		assertNotNull(context);
		assertFalse(context.has("key"));
	}

	@Test
	void testHas() {
		final ClonableMapContext context = new ClonableMapContext();
		assertFalse(context.has("key"));

		context.set("key", "value");
		assertTrue(context.has("key"));
	}

	@Test
	void testMapConstructor() {
		final Map<String, Object> map = new HashMap<>();
		map.put("key", "value");

		final ClonableMapContext context = new ClonableMapContext(map);
		assertNotNull(context);
		assertTrue(context.has("key"));
		assertEquals("value", context.get("key"));
	}

	@Test
	void testSetAndGet() {
		final ClonableMapContext context = new ClonableMapContext();
		context.set("key", "value");

		assertTrue(context.has("key"));
		assertEquals("value", context.get("key"));
	}
}