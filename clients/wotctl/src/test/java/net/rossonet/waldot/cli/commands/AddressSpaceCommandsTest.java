package net.rossonet.waldot.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.cli.CliTestSupport.Result;
import net.rossonet.waldot.cli.CommandTestBase;
import net.rossonet.waldot.cli.support.ExitCodes;
import net.rossonet.waldot.client.api.address.BrowseFilter;
import net.rossonet.waldot.client.api.address.ReferenceInfo;

/**
 * Generic access to the OPC UA address space: browse, read, write, call,
 * resolve.
 */
class AddressSpaceCommandsTest extends CommandTestBase {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final NodeId SENSOR = new NodeId(2, "sensor-1");

	private static final NodeId TEMPERATURE = new NodeId(2,
			"sensor-1/temperature");

	private static ReferenceInfo reference(final NodeId target,
			final String name, final NodeClass nodeClass,
			final boolean forward) {
		return new ReferenceInfo(NodeIds.Organizes, forward, target, name,
				name, nodeClass, null);
	}

	private static DataValue value(final Object value) {
		return new DataValue(new Variant(value), StatusCode.GOOD, null);
	}

	@Test
	void browseDepthAndFilters() throws Exception {
		when(mock.address.browse(eq(NodeIds.ObjectsFolder),
				any(BrowseFilter.class))).thenReturn(List.of(
						reference(SENSOR, "sensor-1", NodeClass.Object, true),
						reference(NodeIds.Server, "Server", NodeClass.Object,
								false)));
		when(mock.address.browse(eq(SENSOR), any(BrowseFilter.class)))
				.thenReturn(List.of(new ReferenceInfo(NodeIds.HasProperty,
						true, TEMPERATURE, "temperature", "Temperatura",
						NodeClass.Variable, null),
						// cycle back to the start: visited once
						reference(NodeIds.ObjectsFolder, "Objects",
								NodeClass.Object, true)));
		final Result oneLevel = run("browse");
		assertEquals(0, oneLevel.exitCode(), oneLevel.err());
		assertEquals(List.of("Object sensor-1 ns=2;s=sensor-1",
				"Object Server i=2253  [inverso]"), oneLevel.lines());
		verify(mock.address, never()).browse(eq(SENSOR),
				any(BrowseFilter.class));

		final Result twoLevels = run("browse", "--depth", "3");
		assertEquals(List.of("Object sensor-1 ns=2;s=sensor-1",
				"  Variable temperature ns=2;s=sensor-1/temperature  (Temperatura)",
				"  Object Objects i=85", "Object Server i=2253  [inverso]"),
				twoLevels.lines());
		assertEquals(List.of("ns=2;s=sensor-1", "ns=2;s=sensor-1/temperature",
				"i=85", "i=2253"),
				run("browse", "--depth", "2", "-o", "value").lines());
		final JsonNode json = MAPPER.readTree(
				run("browse", "--depth", "2", "-o", "json").out());
		assertEquals(2, json.get(1).get("depth").asInt());
		assertEquals("Variable", json.get(1).get("nodeClass").asText());
		assertEquals("ns=2;s=sensor-1", json.get(1).get("parent").asText());

		final ArgumentCaptor<BrowseFilter> filter = ArgumentCaptor
				.forClass(BrowseFilter.class);
		final Result filtered = run("browse", "sensor-1", "--references",
				"all", "--direction", "both", "--node-class",
				"Variable,Method");
		assertEquals(0, filtered.exitCode(), filtered.err());
		verify(mock.address, org.mockito.Mockito.atLeastOnce())
				.browse(eq(SENSOR), filter.capture());
		final BrowseFilter last = filter.getValue();
		assertEquals(BrowseDirection.Both, last.direction());
		assertEquals(NodeIds.References, last.referenceTypeId());
		assertEquals(List.of(NodeClass.Variable, NodeClass.Method),
				List.of(last.nodeClasses()));
	}

	@Test
	void browseUsageErrors() {
		assertEquals(ExitCodes.USAGE, run("browse", "--depth", "0").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("browse", "--direction", "invalid").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("browse", "--references", "qualcuna").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("browse", "--node-class", "Nessuna").exitCode());
	}

	@Test
	void callMethod() throws Exception {
		when(mock.address.call(eq(NodeIds.Server), eq(new NodeId(0, 11492)),
				any(Object[].class)))
				.thenReturn(List.of(UInteger.valueOf(1), "testo"));
		final Result text = run("call", "i=2253", "i=11492", "uint32:1",
				"ciao");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals(List.of("1", "testo"), text.lines());
		verify(mock.address).call(NodeIds.Server, new NodeId(0, 11492),
				UInteger.valueOf(1), "ciao");
		final JsonNode json = MAPPER.readTree(
				run("call", "i=2253", "i=11492", "-o", "json").out());
		assertEquals(1, json.get(0).asInt());
		assertEquals(ExitCodes.USAGE,
				run("call", "o", "m", "double:abc").exitCode());
		assertEquals(ExitCodes.USAGE, run("call", "o").exitCode());
	}

	@Test
	void readAttributes() throws Exception {
		when(mock.address.readAttributes(eq(SENSOR), any(AttributeId[].class)))
				.thenReturn(Map.of(AttributeId.DisplayName,
						value(LocalizedText.english("Sensore 1")),
						AttributeId.Description,
						value(LocalizedText.english("prova"))));
		final Result text = run("read", "sensor-1", "-a", "DisplayName",
				"--attribute", "Description");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals(List.of("ns=2;s=sensor-1 [DisplayName] = Sensore 1",
				"ns=2;s=sensor-1 [Description] = prova"), text.lines());
		verify(mock.address).readAttributes(SENSOR, AttributeId.DisplayName,
				AttributeId.Description);
		assertEquals(ExitCodes.USAGE,
				run("read", "sensor-1", "-a", "Colore").exitCode());
		assertEquals(ExitCodes.USAGE, run("read").exitCode());
	}

	@Test
	void readValues() throws Exception {
		when(mock.address.readValues(anyList()))
				.thenReturn(List.of(value(21.5), value("a\nb")));
		final Result text = run("read", "sensor-1/temperature",
				"ns=2;s=sensor-1/note");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals(List.of("ns=2;s=sensor-1/temperature = 21.5",
				"ns=2;s=sensor-1/note = \"a\\nb\""), text.lines());
		verify(mock.address).readValues(
				List.of(TEMPERATURE, new NodeId(2, "sensor-1/note")));
		assertEquals(List.of("21.5", "a", "b"),
				run("read", "a", "b", "-o", "value").lines());
		final List<String> jsonl = run("read", "a", "b", "-o", "jsonl")
				.lines();
		final JsonNode first = MAPPER.readTree(jsonl.get(0));
		assertEquals("ns=2;s=a", first.get("nodeId").asText());
		assertEquals("Value", first.get("attribute").asText());
		assertEquals(21.5, first.get("value").asDouble());
		assertEquals("Good", first.get("status").asText());

		when(mock.address.readValues(List.of(new NodeId(2, "x"))))
				.thenReturn(List.of(new DataValue(Variant.NULL_VALUE,
						new StatusCode(StatusCodes.Bad_NodeIdUnknown), null)));
		final Result bad = run("read", "x");
		assertEquals(ExitCodes.SERVER_ERROR, bad.exitCode());
		// the value is printed anyway, the error goes to stderr
		assertTrue(bad.out().contains("(Bad_NodeIdUnknown)"), bad.out());
		assertTrue(bad.err().contains("lettura di ns=2;s=x non riuscita"),
				bad.err());
	}

	@Test
	void resolvePath() throws Exception {
		when(mock.address.resolvePath(NodeIds.ObjectsFolder, "Server",
				"ServerStatus", "CurrentTime"))
				.thenReturn(Optional.of(new NodeId(0, 2258)));
		final Result text = run("resolve", "Server", "ServerStatus",
				"CurrentTime");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals("i=2258", text.out().trim());
		final JsonNode json = MAPPER.readTree(run("resolve", "Server",
				"ServerStatus", "CurrentTime", "-o", "json").out());
		assertEquals("i=85", json.get("start").asText());
		assertEquals("ServerStatus", json.get("path").get(1).asText());
		assertEquals("i=2258", json.get("nodeId").asText());
		when(mock.address.resolvePath(SENSOR, "nessuno"))
				.thenReturn(Optional.empty());
		final Result missing = run("resolve", "-s", "sensor-1", "nessuno");
		assertEquals(ExitCodes.SERVER_ERROR, missing.exitCode());
		assertTrue(missing.err()
				.contains("percorso non trovato: nessuno da ns=2;s=sensor-1"),
				missing.err());
	}

	@Test
	void writeWithExplicitAndInferredType() throws Exception {
		when(mock.address.writeValue(any(NodeId.class), any()))
				.thenReturn(StatusCode.GOOD);
		final Result typed = run("write", "sensor-1/setpoint", "22", "--type",
				"int32");
		assertEquals(0, typed.exitCode(), typed.err());
		assertEquals("ns=2;s=sensor-1/setpoint <- 22 (Good)",
				typed.out().trim());
		verify(mock.address).writeValue(new NodeId(2, "sensor-1/setpoint"), 22);
		verify(mock.address, never()).readValue(any(NodeId.class));

		// without --type: the type of the current value
		when(mock.address.readValue(TEMPERATURE)).thenReturn(value(20.0));
		final JsonNode json = MAPPER.readTree(run("write",
				"sensor-1/temperature", "21.5", "-o", "json").out());
		assertEquals(21.5, json.get("value").asDouble());
		assertEquals("Good", json.get("status").asText());
		verify(mock.address).writeValue(TEMPERATURE, 21.5);
		// not convertible to the current type: as text
		run("write", "sensor-1/temperature", "alto");
		verify(mock.address).writeValue(TEMPERATURE, "alto");
		// current value not readable: as text
		when(mock.address.readValue(SENSOR))
				.thenThrow(new IllegalStateException("non leggibile"));
		run("write", "sensor-1", "x");
		verify(mock.address).writeValue(SENSOR, "x");

		when(mock.address.writeValue(eq(new NodeId(2, "ro")), any()))
				.thenReturn(new StatusCode(StatusCodes.Bad_NotWritable));
		final Result refused = run("write", "ro", "1", "-t", "int32", "-o",
				"value");
		assertEquals(ExitCodes.SERVER_ERROR, refused.exitCode());
		assertTrue(refused.err().contains("Bad_NotWritable"), refused.err());
		when(mock.address.writeValue(eq(new NodeId(2, "n")), any()))
				.thenReturn(null);
		assertEquals("Good",
				run("write", "n", "1", "-t", "bool", "-o", "value").out()
						.trim());
		assertEquals(ExitCodes.USAGE,
				run("write", "n", "x", "--type", "complex").exitCode());
	}
}
