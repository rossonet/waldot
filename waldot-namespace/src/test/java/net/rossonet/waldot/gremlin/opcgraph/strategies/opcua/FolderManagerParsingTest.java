package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class FolderManagerParsingTest {

	@Test
	void parseKeyValuesTrimsItems() {
		assertEquals(List.of("a", "b", "c", "d"), MiloSingleServerBaseFolderManager.parseKeyValues(" a, b ,c,d "));
	}

	@Test
	void parseKeyValuesRejectsOddCount() {
		assertThrows(IllegalArgumentException.class, () -> MiloSingleServerBaseFolderManager.parseKeyValues("a,b,c"));
	}

	@Test
	void parseKeyValuesEmpty() {
		assertTrue(MiloSingleServerBaseFolderManager.parseKeyValues("").isEmpty());
		assertTrue(MiloSingleServerBaseFolderManager.parseKeyValues(null).isEmpty());
	}

	@Test
	void containsKeyChecksOnlyKeys() {
		final List<String> kv = List.of("name", "id", "other", "x");
		assertTrue(MiloSingleServerBaseFolderManager.containsKey(kv, "ID".equals("ID") ? "name" : ""));
		assertTrue(MiloSingleServerBaseFolderManager.containsKey(kv, "OTHER"));
		assertFalse(MiloSingleServerBaseFolderManager.containsKey(kv, "id"));
		assertFalse(MiloSingleServerBaseFolderManager.containsKey(kv, "x"));
	}
}
