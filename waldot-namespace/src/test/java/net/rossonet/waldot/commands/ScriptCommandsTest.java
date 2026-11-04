package net.rossonet.waldot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.changes.GraphChangeRecorder;
import net.rossonet.waldot.api.changes.GraphChangeRecorder.Change;
import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.jexl.JexlContextRegistry;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * {@code run script} and {@code jexl check} on a real namespace: result, log,
 * diff of the graph, tag, errors with excerpt, contexts.
 */
@TestInstance(Lifecycle.PER_CLASS)
class ScriptCommandsTest {

	private static final ObjectMapper JSON = new ObjectMapper();

	private WaldotGraph graph;

	@AfterEach
	void noRecordingLeft() {
		assertEquals(0, GraphChangeRecorder.activeRecordings(), "a recording is still open");
		JexlContextRegistry.unregister("test-action");
	}

	@Test
	void allTheContextVariablesOfTheConsoleWork() {
		final JsonNode r = script("math.max(1, 2) + rand.nextInt(1) + cmd.toNumber('3').intValue()");
		assertTrue(r.get("ok").asBoolean(), r.toString());
		assertEquals(5, r.get("result").asInt());
	}

	@Test
	void assignedVariablesDoNotStayInTheConsole() {
		assertTrue(script("leaked = 42; leaked").get("ok").asBoolean());
		final JsonNode second = script("leaked");
		assertFalse(second.get("ok").asBoolean(), second.toString());
		assertEquals("variable-undefined", second.get("error").get("kind").asText());
	}

	@Test
	void asyncChangesAreMarked() throws Exception {
		final Vertex a = graph.addVertex(T.id, "s-async", "name", "a");
		try (GraphChangeRecorder.Recording recording = GraphChangeRecorder.start()) {
			final CompletableFuture<Void> other = CompletableFuture.runAsync(() -> a.property("name", "b"));
			other.get(10, TimeUnit.SECONDS);
			a.property("name", "c");
			final List<Change> changes = recording.changes().stream().filter(c -> "name".equals(c.key())).toList();
			assertEquals(2, changes.size(), changes.toString());
			assertEquals(GraphChangeRecorder.ORIGIN_ASYNC, changes.get(0).origin());
			assertEquals("a", changes.get(0).oldValue());
			assertEquals(GraphChangeRecorder.ORIGIN_SCRIPT, changes.get(1).origin());
			assertEquals("b", changes.get(1).oldValue());
			assertEquals("c", changes.get(1).newValue());
		}
	}

	private Object[] call(final String browseName, final String... inputs) {
		return command(browseName).runCommand(null, inputs);
	}

	private WaldotCommand command(final String browseName) {
		return graph.getWaldotNamespace().getConsoleStrategy().getCommands().stream()
				.filter(c -> c.getBrowseName() != null && browseName.equals(c.getBrowseName().getName())).findFirst()
				.orElseThrow(() -> new AssertionError("command " + browseName + " not registered"));
	}

	@Test
	void commandsAreRegistered() {
		assertNotNull(command(DataExchangeProtocol.RUN_SCRIPT_COMMAND));
		assertNotNull(command(DataExchangeProtocol.JEXL_CHECK_COMMAND));
	}

	private JsonNode check(final String script, final String context) {
		final Object[] out = call(DataExchangeProtocol.JEXL_CHECK_COMMAND, script, context);
		assertEquals("", out[1], String.valueOf(out[1]));
		try {
			return JSON.readTree((String) out[0]);
		} catch (final Exception e) {
			throw new AssertionError(e);
		}
	}

	@Test
	void checkDoesNotTouchTheGraph() {
		final long vertices = graph.traversal().V().count().next();
		final long edges = graph.traversal().E().count().next();
		final JsonNode r = check("graph.addVertex('id', 'never-created'); g.V().drop()", "console");
		assertTrue(r.get("ok").asBoolean(), r.toString());
		assertEquals(vertices, graph.traversal().V().count().next());
		assertEquals(edges, graph.traversal().E().count().next());
	}

	@Test
	void checkListsTheContextsWithoutAScript() {
		JexlContextRegistry.register(new JexlContextRegistry.Context("test-action", "azione di prova",
				List.of(JexlContextRegistry.self("vertice"), new JexlContextRegistry.Variable("value", "Object", "")),
				false));
		final JsonNode r = check("", "");
		final List<String> names = new ArrayList<>();
		r.get("contexts").forEach(c -> names.add(c.get("name").asText()));
		assertTrue(names.contains("console"), names.toString());
		assertTrue(names.contains("test-action"), names.toString());
		JsonNode console = null;
		for (final JsonNode c : r.get("contexts")) {
			if (c.get("name").asText().equals("console")) {
				console = c;
			}
		}
		assertTrue(console.get("console_commands").asBoolean());
		final List<String> vars = new ArrayList<>();
		console.get("variables").forEach(v -> vars.add(v.get("name").asText()));
		assertTrue(vars.containsAll(List.of("g", "graph", "cmd", "log", "math", "rand")), vars.toString());
	}

	@Test
	void checkOfAnotherContext() {
		JexlContextRegistry.register(new JexlContextRegistry.Context("test-action", "azione di prova",
				List.of(JexlContextRegistry.self("vertice"), new JexlContextRegistry.Variable("value", "Object", "")),
				false));
		assertTrue(check("self.property('x', value)", "test-action").get("ok").asBoolean());
		final JsonNode r = check("self.property('x', cmd.toNumber(value))", "test-action");
		assertFalse(r.get("ok").asBoolean());
		assertEquals("cmd", r.get("errors").get(0).get("variable").asText());
		final JsonNode console = check("about", "console");
		assertTrue(console.get("ok").asBoolean(), "console commands are variables of the console: " + console);
	}

	@ParameterizedTest
	@ValueSource(strings = { "mod", "div", "eq", "ne", "lt", "gt", "le", "ge", "and", "or", "not" })
	void checkReservedWords(final String word) {
		final JsonNode r = check("var " + word + " = 1;", "console");
		assertFalse(r.get("ok").asBoolean());
		assertEquals("syntax", r.get("errors").get(0).get("kind").asText());
		assertEquals(word, r.get("errors").get(0).get("variable").asText());
	}

	@Test
	void checkUnknownContextIsAnError() {
		final Object[] out = call(DataExchangeProtocol.JEXL_CHECK_COMMAND, "1", "no-such-context");
		assertTrue(String.valueOf(out[1]).contains("contesto sconosciuto"), String.valueOf(out[1]));
		assertTrue(String.valueOf(out[1]).contains("console"), String.valueOf(out[1]));
	}

	@Test
	void createdEdgesAndRemovedEdges() {
		graph.addVertex(T.id, "s-a");
		graph.addVertex(T.id, "s-b");
		final JsonNode r = script("""
				var a = g.V('s-a').next();
				var b = g.V('s-b').next();
				var e = a.addEdge('link', b, 'type', 'link-to');
				e""");
		assertTrue(r.get("ok").asBoolean(), r.toString());
		final JsonNode created = r.get("diff").get("edges").get("created");
		assertEquals(1, created.size(), r.toString());
		assertEquals("s-a", created.get(0).get("from").asText());
		assertEquals("s-b", created.get(0).get("to").asText());
		assertEquals("link-to", created.get(0).get("type").asText());
		final String edgeId = created.get(0).get("id").asText();
		final JsonNode removed = script("g.E().hasLabel('link').drop().iterate(); 1");
		assertEquals(1, removed.get("diff").get("edges").get("removed").size(), removed.toString());
		assertEquals(edgeId, removed.get("diff").get("edges").get("removed").get(0).get("id").asText());
	}

	@Test
	void createdThenRemovedElementsAreNotInTheDiff() {
		final JsonNode r = script("""
				var v = graph.addVertex('id', 's-temp', 'kind', 'temp');
				v.property('x', 1);
				v.remove();
				1""");
		assertTrue(r.get("ok").asBoolean(), r.toString());
		assertEquals(0, r.get("diff").get("vertices").get("created").size(), r.toString());
		assertEquals(0, r.get("diff").get("vertices").get("removed").size(), r.toString());
		assertEquals(0, r.get("diff").get("properties").size(), r.toString());
	}

	@Test
	void diffGroupsByTypeAndListsModifiedProperties() {
		graph.addVertex(T.id, "s-existing", "speed", 10);
		final JsonNode r = script("""
				graph.addVertex('id', 's-n1', 'label', 'N1', 'directory', 'impianto/valvole');
				graph.addVertex('id', 's-n2');
				var p = g.V('s-existing').next();
				p.property('speed', 20);
				p.property('speed', 30);
				p.property('mode', 'auto');
				'done'""");
		assertTrue(r.get("ok").asBoolean(), r.toString());
		assertEquals("done", r.get("result").asText());
		final JsonNode diff = r.get("diff");
		assertEquals(2, diff.get("summary").get("vertices_created").asInt());
		assertEquals(2, diff.get("by_type").get("vertex").get("created").asInt(), diff.toString());
		assertEquals(2, diff.get("by_type").get("vertex").get("modified").asInt(), diff.toString());
		assertTrue(diff.get("vertices").get("created").get(0).get("type").isNull(), "no type property");
		final JsonNode first = diff.get("vertices").get("created").get(0);
		assertEquals("s-n1", first.get("id").asText());
		assertEquals("N1", first.get("label").asText());
		assertEquals("impianto/valvole", first.get("directory").asText());
		final JsonNode props = diff.get("properties");
		assertEquals(2, props.size(), props.toString());
		JsonNode speed = null;
		JsonNode mode = null;
		for (final JsonNode p : props) {
			if (p.get("key").asText().equals("speed")) {
				speed = p;
			} else if (p.get("key").asText().equals("mode")) {
				mode = p;
			}
		}
		assertEquals(10, speed.get("old").asInt(), "first old value");
		assertEquals(30, speed.get("new").asInt(), "last new value");
		assertTrue(mode.get("old").isNull());
		assertEquals("auto", mode.get("new").asText());
		assertEquals("vertex", speed.get("target").asText());
		assertEquals("s-existing", speed.get("element").asText());
	}

	@Test
	void unknownTypeHasAReadableError() {
		final JsonNode r = script("graph.addVertex('id', 's-bad', 'type', 'nessuno:tipo')");
		assertFalse(r.get("ok").asBoolean());
		final String message = r.get("error").get("message").asText();
		assertTrue(message.contains("'nessuno:tipo' unknown"), message);
		assertTrue(message.contains("Known types"), message);
	}

	@Test
	void emptyScriptIsAnError() {
		final Object[] out = call(DataExchangeProtocol.RUN_SCRIPT_COMMAND, "", "");
		assertFalse(String.valueOf(out[1]).isBlank());
	}

	@Test
	void errorInTheMiddleKeepsThePartialDiffAndHasAnExcerpt() {
		final JsonNode r = script("""
				graph.addVertex('id', 's-before');
				var y = missing + 1;
				graph.addVertex('id', 's-after');""");
		assertFalse(r.get("ok").asBoolean());
		final JsonNode error = r.get("error");
		assertEquals("variable-undefined", error.get("kind").asText());
		assertEquals(2, error.get("line").asInt());
		assertEquals(9, error.get("column").asInt());
		assertEquals("var y = missing + 1;", error.get("excerpt").asText());
		assertTrue(error.get("message").asText().startsWith("riga 2, colonna 9"), error.toString());
		assertEquals(1, r.get("diff").get("vertices").get("created").size(), r.toString());
		assertEquals("s-before", r.get("diff").get("vertices").get("created").get(0).get("id").asText());
	}

	@Test
	void logLinesAreReturnedInOrder() {
		final JsonNode r = script("""
				log.info('primo');
				log.warn('secondo {} {}', 1, 'due');
				log.error('terzo');
				log.debug('quarto');
				3""");
		assertTrue(r.get("ok").asBoolean(), r.toString());
		final JsonNode log = r.get("log");
		assertEquals(4, log.size(), log.toString());
		assertEquals("info", log.get(0).get("level").asText());
		assertEquals("primo", log.get(0).get("message").asText());
		assertEquals("secondo 1 due", log.get(1).get("message").asText());
		assertEquals("warn", log.get(1).get("level").asText());
		assertEquals("error", log.get(2).get("level").asText());
		assertEquals("debug", log.get(3).get("level").asText());
		assertTrue(r.get("duration_ms").asLong() >= 0);
	}

	@Test
	void manyLogLinesAreCapped() {
		final JsonNode r = script("for (var i = 0; i < 1100; i++) { log.info('riga ' + i); } 1");
		assertEquals(1000, r.get("log").size());
		assertEquals(100, r.get("log_dropped").asInt());
	}

	@Test
	void removedVertexKeepsItsPropertiesAndEdges() {
		final Vertex a = graph.addVertex(T.id, "s-rm", "x", 7);
		final Vertex b = graph.addVertex(T.id, "s-other");
		a.addEdge("link", b, T.id, "s-rm-edge", "weight", 2);
		final JsonNode r = script("g.V('s-rm').next().remove(); 1");
		assertTrue(r.get("ok").asBoolean(), r.toString());
		final JsonNode removed = r.get("diff").get("vertices").get("removed");
		assertEquals(1, removed.size(), r.toString());
		assertEquals("s-rm", removed.get(0).get("id").asText());
		assertEquals(7, removed.get(0).get("properties").get("x").asInt());
		assertEquals(1, removed.get(0).get("edges").size());
		assertEquals("s-rm-edge", removed.get(0).get("edges").get(0).get("id").asText());
		assertEquals(1, r.get("diff").get("by_type").get("vertex").get("removed").asInt());
	}

	@BeforeEach
	void resetGraph() {
		graph.getWaldotNamespace().resetNameSpace();
	}

	@Test
	void resultOfATraversalIsAListOfIds() {
		graph.addVertex(T.id, "s-r1", "kind", "r");
		graph.addVertex(T.id, "s-r2", "kind", "r");
		final JsonNode r = script("g.V('s-r1', 's-r2')");
		assertTrue(r.get("ok").asBoolean(), r.toString());
		assertTrue(r.get("result").isArray(), r.toString());
		assertEquals(2, r.get("result").size());
		final JsonNode map = script("var m = {'a': 1, 'b': [1, 2]}; m");
		assertEquals(1, map.get("result").get("a").asInt());
		assertEquals(2, map.get("result").get("b").size(), map.toString());
		final JsonNode array = script("var a = [1, 2, 3]; a");
		assertEquals(3, array.get("result").size(), "primitive arrays become lists: " + array);
		assertEquals(2, array.get("result").get(1).asInt());
	}

	private JsonNode script(final String script) {
		return script(script, "");
	}

	private JsonNode script(final String script, final String tag) {
		final Object[] out = call(DataExchangeProtocol.RUN_SCRIPT_COMMAND, script, tag);
		try {
			final JsonNode node = JSON.readTree((String) out[0]);
			if (node.get("ok").asBoolean()) {
				assertEquals("", out[1]);
			} else {
				assertEquals(node.get("error").get("message").asText(), out[1]);
			}
			return node;
		} catch (final Exception e) {
			throw new AssertionError(String.valueOf(out[1]), e);
		}
	}

	@BeforeAll
	void startServer() throws Exception {
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
	}

	@AfterAll
	void stopServer() {
		WaldotTestEnvironment.cleanup(graph, null);
	}

	@Test
	void tagMarksOnlyTheCreatedElements() {
		graph.addVertex(T.id, "s-old");
		final JsonNode r = script("""
				var a = graph.addVertex('id', 's-t1');
				var b = graph.addVertex('id', 's-t2');
				a.addEdge('link', b);
				g.V('s-old').next().property('touched', true);
				1""", "mcp:test");
		assertTrue(r.get("ok").asBoolean(), r.toString());
		assertEquals("mcp:test", graph.traversal().V("s-t1").values("created-by").next());
		assertEquals("mcp:test", graph.traversal().V("s-t2").values("created-by").next());
		assertEquals("mcp:test", graph.traversal().V("s-t1").outE("link").values("created-by").next());
		assertFalse(graph.traversal().V("s-old").has("created-by").hasNext());
	}

	@Test
	void twoConcurrentScriptsDoNotMixTheirDiffs() throws Exception {
		final CountDownLatch start = new CountDownLatch(1);
		final CompletableFuture<JsonNode> first = CompletableFuture.supplyAsync(() -> {
			await(start);
			return script("for (var i = 0; i < 20; i++) { graph.addVertex('id', 'c1-' + i); } 1");
		});
		final CompletableFuture<JsonNode> second = CompletableFuture.supplyAsync(() -> {
			await(start);
			return script("for (var i = 0; i < 20; i++) { graph.addVertex('id', 'c2-' + i); } 1");
		});
		start.countDown();
		for (final Map.Entry<String, CompletableFuture<JsonNode>> e : Map.of("c1-", first, "c2-", second)
				.entrySet()) {
			final JsonNode r = e.getValue().get(60, TimeUnit.SECONDS);
			final JsonNode created = r.get("diff").get("vertices").get("created");
			assertEquals(20, created.size(), r.toString());
			created.forEach(v -> assertTrue(v.get("id").asText().startsWith(e.getKey()), v.toString()));
		}
	}

	private static void await(final CountDownLatch latch) {
		try {
			latch.await(10, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
