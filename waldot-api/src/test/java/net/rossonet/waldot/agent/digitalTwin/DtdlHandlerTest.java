package net.rossonet.waldot.agent.digitalTwin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.dtdl.DtdlHandler;

class DtdlHandlerTest {

	@Test
	void testEmptyContents() throws IOException {
		final String emptyContentsDtdl = """
				{
				    "@context": "dtmi:dtdl:context;2",
				    "@type": "Interface",
				    "@id": "dtmi:example:Thermostat;1",
				    "contents": []
				}
				""";

		final DtdlHandler handler = DtdlHandler.newFromDtdlV2(emptyContentsDtdl);

		assertNotNull(handler);
		assertTrue(handler.getTelemetries().isEmpty());
		assertTrue(handler.getProperties().isEmpty());
		assertTrue(handler.getCommands().isEmpty());
	}

	@Test
	void testInvalidContext() {
		final String invalidContextDtdl = """
				{
				    "@context": "invalid:context",
				    "@type": "Interface",
				    "@id": "dtmi:example:Thermostat;1"
				}
				""";

		final Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			DtdlHandler.newFromDtdlV2(invalidContextDtdl);
		});

		assertTrue(exception.getMessage().contains("@context must be dtmi:dtdl:context;2"));
	}

	@Test
	void testInvalidType() {
		final String invalidTypeDtdl = """
				{
				    "@context": "dtmi:dtdl:context;2",
				    "@type": "InvalidType",
				    "@id": "dtmi:example:Thermostat;1"
				}
				""";

		final Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			DtdlHandler.newFromDtdlV2(invalidTypeDtdl);
		});

		assertTrue(exception.getMessage().contains("@type must be Interface"));
	}

	@Test
	void testMissingId() {
		final String missingIdDtdl = """
				{
				    "@context": "dtmi:dtdl:context;2",
				    "@type": "Interface"
				}
				""";

		final Exception exception = assertThrows(NullPointerException.class, () -> {
			DtdlHandler.newFromDtdlV2(missingIdDtdl);
		});

		assertTrue(exception.getMessage().contains("@id"));
	}

	@Test
	void testValidDtdlV2Input() throws IOException {
		final String validDtdl = """
				{
				    "@context": "dtmi:dtdl:context;2",
				    "@type": "Interface",
				    "@id": "dtmi:example:Thermostat;1",
				    "displayName": "Thermostat",
				    "description": "A thermostat example",
				    "contents": [
				        {
				            "@type": "Telemetry",
				            "name": "Temperature",
				            "schema": "double"
				        },
				        {
				            "@type": "Property",
				            "name": "SetPointTemperature",
				            "schema": "double"
				        },
				        {
				            "@type": "Command",
				            "name": "TurnOn",
				            "request": {
				                "name": "CommandRequest",
				                "schema": "boolean"
				            }
				        }
				    ]
				}
				""";

		final DtdlHandler handler = DtdlHandler.newFromDtdlV2(validDtdl);

		assertNotNull(handler);
		assertEquals("Thermostat", handler.getDisplayName());
		assertEquals("A thermostat example", handler.getDescription());
		assertEquals(1, handler.getTelemetries().size());
		assertEquals(1, handler.getProperties().size());
		assertEquals(1, handler.getCommands().size());
	}
}