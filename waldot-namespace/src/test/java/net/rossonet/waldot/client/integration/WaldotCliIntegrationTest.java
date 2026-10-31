package net.rossonet.waldot.client.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.cli.WaldotCli;
import net.rossonet.waldot.cli.support.ExitCodes;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;
import picocli.CommandLine;

/**
 * The {@code wotctl} command line interface against an
 * embedded WaldOT server: queries, graph changes, snapshots, address space
 * and subscriptions, with the exit codes.
 */
@TestInstance(Lifecycle.PER_CLASS)
class WaldotCliIntegrationTest {

	private record Result(int exitCode, String out, String err) {

		List<String> lines() {
			return out.isEmpty() ? List.of() : Arrays.asList(out.split("\n"));
		}
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private String endpoint;
	private WaldotGraph graph;

	@TempDir
	Path tmp;

	@BeforeEach
	void resetGraph() {
		graph.getWaldotNamespace().resetNameSpace();
		final Vertex marko = graph.addVertex(T.id, "x-marko", T.label, "person", "name", "marko", "age", 29);
		final Vertex vadas = graph.addVertex(T.id, "x-vadas", T.label, "person", "name", "vadas", "age", 27);
		final Vertex lop = graph.addVertex(T.id, "x-lop", T.label, "software", "name", "lop", "lang", "java");
		marko.addEdge("knows", vadas, T.id, "x-e7", "weight", 0.5d);
		marko.addEdge("created", lop, T.id, "x-e9", "weight", 0.4d);
	}

	private Result run(final String... args) {
		final List<String> arguments = new ArrayList<>(Arrays.asList(args));
		arguments.add("--endpoint");
		arguments.add(endpoint);
		final StringWriter out = new StringWriter();
		final StringWriter err = new StringWriter();
		final CommandLine commandLine = WaldotCli.newCommandLine(new PrintWriter(out), new PrintWriter(err));
		final int exitCode = commandLine.execute(arguments.toArray(new String[0]));
		return new Result(exitCode, out.toString(), err.toString());
	}

	@BeforeAll
	void startServer() throws Exception {
		WaldotTestEnvironment.awaitOpcPortReleased();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
		endpoint = WaldotTestEnvironment.opcEndpoint();
	}

	@AfterAll
	void stopServer() {
		WaldotTestEnvironment.cleanup(graph, null);
	}

	@Test
	void aboutAndCommands() {
		final Result about = run("about");
		assertEquals(0, about.exitCode(), about.err());
		assertFalse(about.out().isBlank());
		final Result commands = run("commands", "list", "-o", "value");
		assertEquals(0, commands.exitCode(), commands.err());
		assertTrue(commands.lines().contains("about"), commands.out());
		final Result elementCommands = run("commands", "list", "--element", "x-marko", "-o", "value");
		assertEquals(0, elementCommands.exitCode(), elementCommands.err());
		assertTrue(elementCommands.lines().contains("property"), elementCommands.out());
		final Result missing = run("commands", "call", "no such command");
		assertEquals(ExitCodes.SERVER_ERROR, missing.exitCode());
		assertTrue(missing.out().isEmpty());
	}

	@Test
	void addressSpace() throws Exception {
		final Result read = run("read", "x-marko/age", "-o", "value");
		assertEquals(0, read.exitCode(), read.err());
		assertEquals("29", read.out().trim());
		final Result write = run("write", "x-marko/age", "31");
		assertEquals(0, write.exitCode(), write.err());
		assertEquals(31, graph.traversal().V(new NodeId(2, "x-marko")).values("age").next());
		final Result jsonl = run("read", "x-marko/age", "x-marko/name", "-o", "jsonl");
		assertEquals(2, jsonl.lines().size(), jsonl.out());
		assertEquals("marko", MAPPER.readTree(jsonl.lines().get(1)).get("value").asText());
		final Result unknown = run("read", "missing/node", "-o", "value");
		assertEquals(ExitCodes.SERVER_ERROR, unknown.exitCode());
		final Result browse = run("browse", "-o", "value");
		assertEquals(0, browse.exitCode(), browse.err());
		assertTrue(browse.lines().contains("i=2253"), browse.out());
		final Result resolve = run("resolve", "Server", "ServerStatus", "-o", "value");
		assertEquals(0, resolve.exitCode(), resolve.err());
		assertEquals("i=2256", resolve.out().trim());
		assertEquals(ExitCodes.SERVER_ERROR, run("resolve", "Server", "Nessuno").exitCode());
	}

	@Test
	void describeAndProperties() throws Exception {
		final Result describe = run("describe", "x-marko", "-o", "json");
		assertEquals(0, describe.exitCode(), describe.err());
		final JsonNode description = MAPPER.readTree(describe.out());
		assertEquals("ns=2;s=x-marko", description.get("id").asText());
		assertEquals(2, description.get("outEdges").size());
		final Result set = run("property", "set", "x-marko", "city", "Torino");
		assertEquals(0, set.exitCode(), set.err());
		final Result get = run("property", "get", "x-marko", "city", "-o", "value");
		assertEquals(0, get.exitCode(), get.err());
		assertEquals("Torino", get.out().trim());
		assertEquals(ExitCodes.SERVER_ERROR, run("property", "get", "x-marko", "nessuna").exitCode());
	}

	@Test
	void graphChanges() throws Exception {
		final Result add = run("vertex", "add", "--id", "x-new", "--label", "thing", "-P", "color=red", "-o",
				"value");
		assertEquals(0, add.exitCode(), add.err());
		assertEquals("x-new", add.out().trim());
		assertTrue(graph.traversal().V(new NodeId(2, "x-new")).hasNext());
		final Result edge = run("edge", "add", "--label", "uses", "--from", "x-marko", "--to", "x-new");
		assertEquals(0, edge.exitCode(), edge.err());
		assertEquals(1L, graph.traversal().V(new NodeId(2, "x-marko")).outE("uses").count().next());
		final Result edges = run("query", "-o", "value", "g.V('x-marko').outE('uses')");
		assertEquals(0, edges.exitCode(), edges.err());
		assertEquals(1, edges.lines().size(), edges.out());
		final Result deleteEdge = run("edge", "delete", edges.lines().get(0));
		assertEquals(0, deleteEdge.exitCode(), deleteEdge.err());
		assertEquals(0L, graph.traversal().V(new NodeId(2, "x-marko")).outE("uses").count().next());
		final Result delete = run("vertex", "delete", "x-new");
		assertEquals(0, delete.exitCode(), delete.err());
		assertFalse(graph.traversal().V(new NodeId(2, "x-new")).hasNext());
	}

	@Test
	void queries() throws Exception {
		final Result jsonl = run("query", "-o", "jsonl", "g.V().hasLabel('person')");
		assertEquals(0, jsonl.exitCode(), jsonl.err());
		assertEquals(2, jsonl.lines().size(), jsonl.out());
		for (final String line : jsonl.lines()) {
			final JsonNode vertex = MAPPER.readTree(line);
			assertEquals("vertex", vertex.get("type").asText());
			assertEquals("person", vertex.get("label").asText());
			assertTrue(vertex.get("id").asText().startsWith("ns=2;s=x-"));
		}
		final Result count = run("query", "-o", "value", "g.V().count()");
		assertEquals(0, count.exitCode(), count.err());
		assertEquals("3", count.out().trim());
		final Result names = run("query", "-o", "json", "g.V().hasLabel('person').values('name')");
		assertEquals(0, names.exitCode(), names.err());
		assertEquals(2, MAPPER.readTree(names.out()).size());
		final Result text = run("query", "--text", "g.V().count().next()");
		assertEquals(0, text.exitCode(), text.err());
		assertTrue(text.out().contains("3"), text.out());
		final Path file = tmp.resolve("query.txt");
		Files.writeString(file, "g.V().hasLabel('software').values('name')\n");
		final Result fromFile = run("query", "-f", file.toString(), "-o", "value");
		assertEquals("lop", fromFile.out().trim());
		final Result wrong = run("query", "--text", "g.V().nonExistingStep()");
		assertEquals(ExitCodes.SERVER_ERROR, wrong.exitCode(), wrong.out());
	}

	@Test
	void snapshotExportImportClear() throws Exception {
		final Path backup = tmp.resolve("graph.json.gz");
		final Result export = run("snapshot", "export", "-f", backup.toString(), "-o", "json");
		assertEquals(0, export.exitCode(), export.err());
		assertEquals(3, MAPPER.readTree(export.out()).get("vertices").asInt());
		assertTrue(Files.size(backup) > 0);
		assertEquals(ExitCodes.USAGE, run("snapshot", "clear").exitCode());
		final Result clear = run("snapshot", "clear", "--yes", "-o", "value");
		assertEquals(0, clear.exitCode(), clear.err());
		assertEquals(0L, graph.traversal().V().count().next());
		final Result imported = run("snapshot", "import", "--clear", "-f", backup.toString(), "-o", "value");
		assertEquals(0, imported.exitCode(), imported.err());
		assertEquals("3 2", imported.out().trim());
		assertEquals(3L, graph.traversal().V().count().next());
		assertEquals(2L, graph.traversal().E().count().next());
	}

	@Test
	void renderToFile() throws Exception {
		final Path svg = tmp.resolve("graph.svg");
		final Result rendered = run("render", "-f", svg.toString(), "--root", "x-marko", "--layout", "MRTREE",
				"--props", "age", "-o", "json");
		assertEquals(0, rendered.exitCode(), rendered.err());
		final JsonNode json = MAPPER.readTree(rendered.out());
		assertEquals("svg", json.get("format").asText());
		assertEquals(3, json.get("vertices").asInt());
		assertTrue(Files.readString(svg).contains("age = 29"));

		final Path dot = tmp.resolve("graph.dot");
		final Result dotResult = run("render", "-f", dot.toString(), "--edge-label", "knows", "--no-isolated", "-o",
				"value");
		assertEquals(0, dotResult.exitCode(), dotResult.err());
		assertEquals(dot.toString(), dotResult.out().trim());
		assertTrue(Files.readString(dot).startsWith("digraph waldot {"));

		final Path options = tmp.resolve("options.json");
		Files.writeString(options, "{\"vertices\":{\"labels\":[\"software\"]}}");
		final Result fromFile = run("render", "--options-file", options.toString(), "-f",
				tmp.resolve("software.svg").toString(), "-o", "value");
		assertEquals(0, fromFile.exitCode(), fromFile.err());

		Files.writeString(options, "{\"vertices\":{\"lables\":[]}}");
		assertEquals(ExitCodes.USAGE, run("render", "--options-file", options.toString()).exitCode());
		assertEquals(ExitCodes.USAGE, run("render", "--layout", "spiral").exitCode());
	}

	@Test
	void subscribeWithCount() throws Exception {
		final AtomicBoolean running = new AtomicBoolean(true);
		final CompletableFuture<Result> subscription = CompletableFuture.supplyAsync(() -> run("subscribe", "-p",
				"x-marko:age", "--count", "3", "--sampling", "10", "--publishing", "50", "-o", "value"));
		// changes the value until the command collects its notifications
		final CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
			int age = 40;
			while (running.get() && !subscription.isDone()) {
				graph.traversal().V(new NodeId(2, "x-marko")).next().property("age", age++);
				try {
					Thread.sleep(150);
				} catch (final InterruptedException e) {
					return;
				}
			}
		});
		try {
			final Result result = subscription.get(60, TimeUnit.SECONDS);
			assertEquals(0, result.exitCode(), result.err());
			assertEquals(3, result.lines().size(), result.out());
			for (final String line : result.lines()) {
				Integer.parseInt(line.trim());
			}
		} finally {
			running.set(false);
			writer.get(10, TimeUnit.SECONDS);
		}
		final Result topic = run("subscribe", "-p", "x-lop:name", "--count", "1", "-o", "topic", "--topic-prefix",
				"impianto");
		assertEquals(0, topic.exitCode(), topic.err());
		assertEquals("impianto/x-lop/name lop", topic.out().trim());
		final Result jsonl = run("subscribe", "-n", "x-vadas/name", "--count", "1", "-o", "jsonl");
		assertEquals(0, jsonl.exitCode(), jsonl.err());
		final JsonNode notification = MAPPER.readTree(jsonl.out());
		assertEquals("data", notification.get("type").asText());
		assertEquals("vadas", notification.get("value").asText());
		assertEquals("Good", notification.get("status").asText());
		final Result duration = run("subscribe", "-p", "x-lop:name", "--duration", "1s", "-o", "value");
		assertEquals(0, duration.exitCode(), duration.err());
		assertEquals("lop", duration.out().trim());
	}
}
