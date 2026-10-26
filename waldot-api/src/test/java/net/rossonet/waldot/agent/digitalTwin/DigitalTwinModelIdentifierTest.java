package net.rossonet.waldot.agent.digitalTwin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.dtdl.DigitalTwinModelIdentifier;

class DigitalTwinModelIdentifierTest {

	@Test
	void testValidIdentifier() {
		final DigitalTwinModelIdentifier id = DigitalTwinModelIdentifier.fromString("dtmi:example:Thermostat;1");
		assertEquals("dtmi", id.getScheme());
		assertEquals("example:thermostat", id.getPath());
		assertEquals(1, id.getVersion());
		assertTrue(id.isValid());
	}

	@Test
	void testNullIdentifierRejected() {
		assertThrows(IllegalArgumentException.class, () -> DigitalTwinModelIdentifier.fromString(null));
	}

	@Test
	void testMalformedIdentifierRejected() {
		assertThrows(IllegalArgumentException.class, () -> DigitalTwinModelIdentifier.fromString("invalid-format"));
		assertThrows(IllegalArgumentException.class, () -> DigitalTwinModelIdentifier.fromString("dtmi:example:no-version"));
	}
}
