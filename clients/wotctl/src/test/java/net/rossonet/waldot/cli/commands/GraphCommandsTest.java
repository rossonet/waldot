package net.rossonet.waldot.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedEdge;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedProperty;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertex;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.cli.CliTestSupport.Result;
import net.rossonet.waldot.cli.CommandTestBase;
import net.rossonet.waldot.cli.support.ExitCodes;
import net.rossonet.waldot.client.api.command.ArgumentDescriptor;
import net.rossonet.waldot.client.api.command.CommandDescriptor;
import net.rossonet.waldot.client.api.command.CommandResult;
import net.rossonet.waldot.client.api.graph.ElementDescription;
import net.rossonet.waldot.client.api.graph.ElementDescription.CommandSummary;
import net.rossonet.waldot.client.api.graph.ElementDescription.EdgeSummary;
import net.rossonet.waldot.client.api.graph.ElementDescription.PropertyDescription;
import net.rossonet.waldot.client.api.graph.QueryResult;

/**
 * Commands on the graph: about, query, describe, commands, vertex, edge,
 * property (with a mock client).
 */
class GraphCommandsTest extends CommandTestBase {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final NodeId SENSOR = new NodeId(2, "sensor-1");

	private static ArgumentDescriptor argument(final String name) {
		return new ArgumentDescriptor(name, new NodeId(0, 12), -1, name);
	}

	private static CommandDescriptor command(final String name,
			final List<String> inputs, final List<String> outputs) {
		return new CommandDescriptor(new NodeId(2, "cmd"),
				new NodeId(2, "cmd/" + name), name, "cmd/" + name,
				"descrizione di " + name,
				inputs.stream().map(GraphCommandsTest::argument).toList(),
				outputs.stream().map(GraphCommandsTest::argument).toList());
	}

	private static ElementDescription description(final boolean edge) {
		return new ElementDescription(SENSOR, edge ? "edge" : "vertex",
				"sensor", "temperature", "sensor-1", "Sensore 1",
				"sensore di prova", 1, edge ? new NodeId(2, "a") : null,
				edge ? new NodeId(2, "b") : null,
				List.of(new PropertyDescription("temperature",
						new NodeId(2, "sensor-1/temperature"), 21.5, "Double"),
						new PropertyDescription("note",
								new NodeId(2, "sensor-1/note"), "a\nb", null)),
				edge ? List.of()
						: List.of(new EdgeSummary(new NodeId(2, "e1"), "feeds",
								null, SENSOR, new NodeId(2, "tank-1"))),
				List.of(),
				List.of(new CommandSummary("delete", new NodeId(2, "d"),
						"elimina", List.of(), List.of()),
						new CommandSummary("property", new NodeId(2, "p"), null,
								List.of("key", "value"), List.of("output"))));
	}

	private static CommandResult result(final CommandDescriptor command,
			final StatusCode status, final Object... values) {
		return new CommandResult(command, status, Arrays.asList(values));
	}

	private static DetachedVertex vertex(final String id, final String name) {
		final DetachedVertex vertex = DetachedVertex.build()
				.setId(new NodeId(2, id)).setLabel("sensor").create();
		return DetachedVertex.build().setId(new NodeId(2, id))
				.setLabel("sensor")
				.addProperty(DetachedVertexProperty.build().setId(1L)
						.setLabel("name").setValue(name).setV(vertex).create())
				.create();
	}

	@Test
	void aboutInEveryFormat() throws Exception {
		when(mock.commands.about())
				.thenReturn(List.of("WaldOT 1.0", "namespace urn:x"));
		final Result text = run("about");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals(List.of("WaldOT 1.0", "namespace urn:x"), text.lines());
		final Result json = run("about", "-o", "json");
		assertEquals("WaldOT 1.0", MAPPER.readTree(json.out()).get(0).asText());
		final Result jsonl = run("about", "-o", "jsonl");
		assertEquals(List.of("\"WaldOT 1.0\"", "\"namespace urn:x\""),
				jsonl.lines());
		final Result value = run("about", "-o", "value");
		assertEquals(text.out(), value.out());
		verify(mock.client, org.mockito.Mockito.times(4)).close();
	}

	@Test
	void commandsCall() throws Exception {
		final CommandDescriptor about = command("about", List.of(),
				List.of("output", "error"));
		when(mock.commands.invokeGlobal(eq("about"), any(Object[].class)))
				.thenReturn(result(about, StatusCode.GOOD, "versione 1", ""));
		final Result text = run("commands", "call", "about");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals("versione 1", text.out().trim());
		final JsonNode json = MAPPER
				.readTree(run("commands", "call", "about", "-o", "json").out());
		assertEquals("about", json.get("command").asText());
		assertEquals("Good", json.get("status").asText());
		assertEquals("versione 1", json.get("outputs").get("output").asText());
		assertEquals("versione 1",
				run("commands", "call", "about", "-o", "value").out().trim());

		final CommandDescriptor property = command("property",
				List.of("key", "value"), List.of("old", "new", "error"));
		when(mock.commands.invokeOnElement(eq(SENSOR), eq("property"),
				any(Object[].class)))
				.thenReturn(result(property, StatusCode.GOOD, 1, 2, null));
		final Result element = run("commands", "call", "property", "--element",
				"sensor-1", "setpoint", "2");
		assertEquals(0, element.exitCode(), element.err());
		assertEquals(List.of("old: 1", "new: 2"), element.lines());
		verify(mock.commands).invokeOnElement(SENSOR, "property", "setpoint",
				"2");

		when(mock.commands.invokeGlobal(eq("broken"), any(Object[].class)))
				.thenReturn(result(about, StatusCode.GOOD, "", "esploso"));
		final Result failed = run("commands", "call", "broken");
		assertEquals(ExitCodes.SERVER_ERROR, failed.exitCode());
		assertTrue(failed.err().contains("esploso"), failed.err());
		when(mock.commands.invokeGlobal(eq("bad"), any(Object[].class)))
				.thenReturn(result(about,
						new StatusCode(StatusCodes.Bad_InvalidArgument)));
		assertEquals(ExitCodes.SERVER_ERROR,
				run("commands", "call", "bad").exitCode());
		assertEquals(ExitCodes.USAGE, run("commands").exitCode());
		assertEquals(ExitCodes.USAGE, run("commands", "call").exitCode());
	}

	@Test
	void commandsList() throws Exception {
		when(mock.commands.globalCommands()).thenReturn(List.of(
				command("about", List.of(), List.of("output")),
				command("query", List.of("expression"), List.of())));
		final Result text = run("commands", "list");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals(List.of("about() -> output   descrizione di about",
				"query(expression)   descrizione di query"), text.lines());
		assertEquals(List.of("about", "query"),
				run("commands", "list", "-o", "value").lines());
		final JsonNode json = MAPPER
				.readTree(run("commands", "list", "-o", "json").out());
		assertEquals("ns=2;s=cmd/about", json.get(0).get("methodId").asText());
		assertEquals("expression",
				json.get(1).get("inputs").get(0).get("name").asText());
		when(mock.commands.elementCommands(SENSOR)).thenReturn(
				List.of(new CommandDescriptor(SENSOR, SENSOR, "delete", null,
						null, null, null)));
		assertEquals(List.of("delete()"),
				run("commands", "list", "-e", "sensor-1").lines());
	}

	@Test
	void describeEdge() {
		when(mock.graph.describe(SENSOR)).thenReturn(description(true));
		final Result text = run("describe", "ns=2;s=sensor-1");
		assertEquals(0, text.exitCode(), text.err());
		assertTrue(text.out().contains("origine:      ns=2;s=a"), text.out());
		assertTrue(text.out().contains("destinazione: ns=2;s=b"), text.out());
		assertTrue(text.out().contains("archi uscenti: nessuno"), text.out());
	}

	@Test
	void describeVertex() throws Exception {
		when(mock.graph.describe(SENSOR)).thenReturn(description(false));
		final Result text = run("describe", "sensor-1");
		assertEquals(0, text.exitCode(), text.err());
		assertTrue(text.out().contains("id:           ns=2;s=sensor-1"),
				text.out());
		assertTrue(text.out().contains(
				"temperature = 21.5   (Double, ns=2;s=sensor-1/temperature)"),
				text.out());
		// multi-line values on a single line
		assertTrue(text.out().contains("note = \"a\\nb\"   (?,"), text.out());
		assertTrue(
				text.out().contains(
						"ns=2;s=e1 feeds ns=2;s=sensor-1 -> ns=2;s=tank-1"),
				text.out());
		assertTrue(text.out().contains("archi entranti: nessuno"), text.out());
		assertTrue(text.out().contains("delete()   elimina"), text.out());
		assertTrue(text.out().contains("property(key, value) -> output"),
				text.out());
		assertEquals(List.of("temperature=21.5", "note=\"a\\nb\""),
				run("describe", "sensor-1", "-o", "value").lines());
		final JsonNode json = MAPPER
				.readTree(run("describe", "sensor-1", "-o", "json").out());
		assertEquals("vertex", json.get("kind").asText());
		assertEquals(21.5,
				json.get("properties").get(0).get("value").asDouble());
		assertEquals(1, run("describe", "sensor-1", "-o", "jsonl").lines()
				.size());
		assertEquals(ExitCodes.USAGE, run("describe").exitCode());
	}

	@Test
	void edgeAddAndDelete() throws Exception {
		when(mock.client.createEdge(anyString(), anyString(), anyString(),
				any(String[].class)))
				.thenReturn(new String[]{"arco e-1 creato", ""});
		final Result added = run("edge", "add", "-l", "feeds", "-s", "pump-3",
				"-d", "ns=2;s=tank-1", "-P", "weight=0.5");
		assertEquals(0, added.exitCode(), added.err());
		assertEquals("arco e-1 creato", added.out().trim());
		verify(mock.client).createEdge("feeds", "ns=2;s=pump-3",
				"ns=2;s=tank-1", new String[]{"weight", "0.5"});
		final JsonNode json = MAPPER.readTree(run("edge", "add", "-l", "x",
				"-s", "a", "-d", "b", "-o", "jsonl").out());
		assertEquals("edge", json.get("kind").asText());
		when(mock.client.createEdge(eq("bad"), anyString(), anyString(),
				any(String[].class)))
				.thenReturn(new String[]{"", "vertice inesistente"});
		final Result failed = run("edge", "add", "-l", "bad", "-s", "a", "-d",
				"b");
		assertEquals(ExitCodes.SERVER_ERROR, failed.exitCode());
		assertTrue(failed.err().contains("vertice inesistente"), failed.err());
		assertEquals(ExitCodes.USAGE,
				run("edge", "add", "-l", "x", "-s", "a").exitCode());

		final CommandDescriptor delete = command("delete", List.of(),
				List.of("output", "error"));
		when(mock.commands.deleteElement(any(NodeId.class)))
				.thenReturn(result(delete, StatusCode.GOOD, "ok", ""));
		final Result deleted = run("edge", "delete", "e-1", "e-2");
		assertEquals(0, deleted.exitCode(), deleted.err());
		assertEquals(List.of("eliminato e-1", "eliminato e-2"),
				deleted.lines());
		assertEquals(List.of("e-1"),
				run("edge", "delete", "e-1", "-o", "value").lines());
		assertEquals(ExitCodes.USAGE, run("edge").exitCode());
	}

	@Test
	void propertyGet() throws Exception {
		when(mock.graph.describe(SENSOR)).thenReturn(description(false));
		final Result text = run("property", "get", "sensor-1", "temperature");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals("21.5", text.out().trim());
		assertEquals("21.5", run("property", "get", "sensor-1", "temperature",
				"-o", "value").out().trim());
		final JsonNode json = MAPPER.readTree(run("property", "get",
				"sensor-1", "temperature", "-o", "json").out());
		assertEquals("Double", json.get("valueType").asText());
		assertEquals("ns=2;s=sensor-1/temperature",
				json.get("nodeId").asText());
		final Result missing = run("property", "get", "sensor-1", "nessuna");
		assertEquals(ExitCodes.SERVER_ERROR, missing.exitCode());
		assertTrue(missing.err().contains("non esiste"), missing.err());
		assertEquals(ExitCodes.USAGE, run("property").exitCode());
	}

	@Test
	void propertySet() throws Exception {
		final CommandDescriptor property = command("property", List.of(),
				List.of("output", "error"));
		when(mock.commands.setProperty(any(NodeId.class), anyString(), any()))
				.thenReturn(result(property, StatusCode.GOOD, "ok", ""));
		final Result text = run("property", "set", "sensor-1", "setpoint",
				"21.5");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals("sensor-1/setpoint = 21.5", text.out().trim());
		verify(mock.commands).setProperty(SENSOR, "setpoint", "21.5");
		final Result typed = run("property", "set", "pump-3", "speed", "1450",
				"--type", "int32", "-o", "json");
		assertEquals(1450, MAPPER.readTree(typed.out()).get("value").asInt());
		verify(mock.commands).setProperty(new NodeId(2, "pump-3"), "speed",
				1450);
		assertEquals("true", run("property", "set", "v", "open", "si", "-t",
				"bool", "-o", "value").out().trim());
		final Result invalid = run("property", "set", "v", "open", "forse",
				"-t", "bool");
		assertEquals(ExitCodes.USAGE, invalid.exitCode());
		when(mock.commands.setProperty(eq(new NodeId(2, "ro")), anyString(),
				any()))
				.thenReturn(result(property, StatusCode.GOOD, "", "sola lettura"));
		final Result failed = run("property", "set", "ro", "k", "v");
		assertEquals(ExitCodes.SERVER_ERROR, failed.exitCode());
		assertTrue(failed.err().contains("sola lettura"), failed.err());
	}

	@Test
	void queryFromFileAndStdin() throws Exception {
		when(mock.graph.queryObjects("g.V().count()"))
				.thenReturn(List.of(3L));
		final Path file = tmp.resolve("q.txt");
		Files.writeString(file, "  g.V().count()\n", StandardCharsets.UTF_8);
		final Result fromFile = run("query", "-f", file.toString(), "-o",
				"value");
		assertEquals(0, fromFile.exitCode(), fromFile.err());
		assertEquals("3", fromFile.out().trim());
		final Result fromStdin = runWithStdin("g.V().count()\n", "query", "-f",
				"-", "-o", "value");
		assertEquals("3", fromStdin.out().trim());
		final Result both = run("query", "x", "-f", file.toString());
		assertEquals(ExitCodes.USAGE, both.exitCode());
		assertTrue(both.err().contains("non entrambi"), both.err());
		assertEquals(ExitCodes.USAGE,
				run("query", "x", "--text", "--graphson").exitCode());
		final Result missingFile = run("query", "-f",
				tmp.resolve("nessuno").toString());
		assertEquals(ExitCodes.SERVER_ERROR, missingFile.exitCode());
	}

	@Test
	void queryObjectsInEveryFormat() throws Exception {
		final DetachedVertex marko = vertex("marko", "marko");
		final DetachedEdge edge = DetachedEdge.build()
				.setId(new NodeId(2, "e-7")).setLabel("knows")
				.setOutV(marko).setInV(vertex("vadas", "vadas"))
				.addProperty(new DetachedProperty<>("weight", 0.5)).create();
		when(mock.graph.queryObjects("g.V()"))
				.thenReturn(List.of(marko, edge, "testo", 42));
		final Result text = run("query", "g.V()");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals(List.of("v[ns=2;s=marko] sensor {\"name\":\"marko\"}",
				"e[ns=2;s=e-7] knows ns=2;s=marko -> ns=2;s=vadas {\"weight\":0.5}",
				"testo", "42"), text.lines());
		assertEquals(List.of("ns=2;s=marko", "ns=2;s=e-7", "testo", "42"),
				run("query", "g.V()", "-o", "value").lines());
		final List<String> jsonl = run("query", "g.V()", "-o", "jsonl")
				.lines();
		assertEquals("vertex", MAPPER.readTree(jsonl.get(0)).get("type")
				.asText());
		final JsonNode edgeJson = MAPPER.readTree(jsonl.get(1));
		assertEquals("ns=2;s=vadas", edgeJson.get("inV").asText());
		assertEquals(0.5, edgeJson.get("properties").get("weight").asDouble());
		assertEquals(4, MAPPER.readTree(run("query", "g.V()", "-o", "json")
				.out()).size());
	}

	@Test
	void queryTextAndGraphson() throws Exception {
		when(mock.graph.query("g.V().count().next()"))
				.thenReturn(new QueryResult("3", null));
		final Result text = run("query", "--text", "g.V().count().next()");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals("3", text.out().trim());
		final JsonNode json = MAPPER.readTree(run("query", "--text",
				"g.V().count().next()", "-o", "json").out());
		assertEquals("3", json.get("output").asText());
		when(mock.graph.query("rotta"))
				.thenReturn(new QueryResult(null, "sintassi"));
		final Result failed = run("query", "--text", "rotta");
		assertEquals(ExitCodes.SERVER_ERROR, failed.exitCode());
		assertTrue(failed.err().startsWith("wotctl: errore: errore nella query"),
				failed.err());
		when(mock.graph.queryJson("g.E()"))
				.thenReturn("{\"@type\":\"g:List\"}");
		assertEquals("{\"@type\":\"g:List\"}",
				run("query", "--graphson", "g.E()").out().trim());
		verify(mock.graph, never()).queryObjects("g.E()");
	}

	@Test
	void usageErrorsDoNotConnect() {
		assertEquals(ExitCodes.USAGE, run("query").exitCode());
		assertEquals(ExitCodes.USAGE, run("query", "x", "-o", "topic")
				.exitCode());
		assertEquals(ExitCodes.USAGE, run("vertex", "add").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("vertex", "add", "-l", "x", "-P", "a=b,c").exitCode());
		verifyNoInteractions(mock.client);
	}

	@Test
	void vertexAddAndDelete() throws Exception {
		when(mock.client.createVertex(anyString(), anyString(), anyString(),
				any(String[].class)))
				.thenReturn(new String[]{"vertice creato"});
		final Result added = run("vertex", "add", "--id", "sensor-1", "-l",
				"sensor", "-t", "temperature", "-P", " zone = A ");
		assertEquals(0, added.exitCode(), added.err());
		assertEquals("vertice creato", added.out().trim());
		verify(mock.client).createVertex("sensor-1", "sensor", "temperature",
				new String[]{"zone", "A"});
		assertEquals("sensor-1", run("vertex", "add", "--id", "sensor-1", "-l",
				"sensor", "-o", "value").out().trim());
		final JsonNode json = MAPPER.readTree(
				run("vertex", "add", "-l", "sensor", "-o", "json").out());
		assertEquals("vertex", json.get("kind").asText());
		assertTrue(json.get("id").isNull());
		assertEquals("vertice creato", run("vertex", "add", "-l", "sensor",
				"-o", "value").out().trim());
		when(mock.client.createVertex(eq("dup"), anyString(), anyString(),
				any(String[].class))).thenReturn(new String[0]);
		assertEquals(ExitCodes.SERVER_ERROR,
				run("vertex", "add", "--id", "dup", "-l", "x").exitCode());

		final CommandDescriptor delete = command("delete", List.of(),
				List.of("output", "error"));
		when(mock.commands.deleteElement(any(NodeId.class)))
				.thenReturn(result(delete, StatusCode.GOOD, "ok", ""));
		when(mock.commands.deleteElement(new NodeId(2, "locked")))
				.thenReturn(result(delete, StatusCode.GOOD, "", "bloccato"));
		final JsonNode deleted = MAPPER.readTree(
				run("vertex", "delete", "sensor-1", "-o", "json").out());
		assertEquals("sensor-1", deleted.get(0).get("id").asText());
		assertTrue(deleted.get(0).get("deleted").asBoolean());
		final Result failed = run("vertex", "delete", "sensor-1", "locked");
		assertEquals(ExitCodes.SERVER_ERROR, failed.exitCode());
		assertTrue(failed.err().contains("eliminazione di 'locked'"),
				failed.err());
		final Result invalidId = run("vertex", "delete", "ns=x;q");
		assertEquals(ExitCodes.USAGE, invalidId.exitCode());
		assertTrue(invalidId.err().contains("node id non valido"),
				invalidId.err());
	}
}
