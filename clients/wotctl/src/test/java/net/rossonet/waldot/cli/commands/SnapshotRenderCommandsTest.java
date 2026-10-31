package net.rossonet.waldot.cli.commands;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.api.exchange.GraphRenderOptions;
import net.rossonet.waldot.cli.CliTestSupport.Result;
import net.rossonet.waldot.cli.CommandTestBase;
import net.rossonet.waldot.cli.support.ExitCodes;
import net.rossonet.waldot.client.api.graph.GraphChangeResult;
import net.rossonet.waldot.client.api.graph.GraphRendering;
import net.rossonet.waldot.client.api.graph.GraphSnapshot;

/**
 * Backup and drawing of the graph: snapshot export/import/clear and render.
 */
class SnapshotRenderCommandsTest extends CommandTestBase {

	private static final byte[] GRAPHSON = "{\"vertices\":[]}"
			.getBytes(StandardCharsets.UTF_8);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private GraphRenderOptions renderOptions(final int blockSize) {
		final ArgumentCaptor<GraphRenderOptions> options = ArgumentCaptor
				.forClass(GraphRenderOptions.class);
		verify(mock.graph).render(options.capture(), eq(blockSize));
		return options.getValue();
	}

	private void stubRender(final String format) {
		when(mock.graph.render(any(GraphRenderOptions.class), anyInt()))
				.thenReturn(new GraphRendering(
						"<svg/>".getBytes(StandardCharsets.UTF_8), format, 3,
						2, List.of("troppi vertici"), 1));
	}

	@Test
	void renderAllFlags() {
		stubRender("dot");
		final Path file = tmp.resolve("impianto.dot");
		final Result result = run("render", "-f", file.toString(), "--root",
				"plc-1", "--root", "plc-2", "--depth", "2", "--direction",
				"out", "--type", "a,b", "--exclude-type", "c", "--label", "l",
				"--directory", "d1", "--match", "pump", "--where", "zone=A",
				"--expression", "g.V()", "--include-ephemeral", "--no-isolated",
				"--max", "50", "--edge-label", "feeds", "--exclude-edge-label",
				"link", "--props", "name,zone", "--edge-props", "w*",
				"--include-runtime", "--layout", "force",
				"--layout-direction", "DOWN", "--group-by", "directory",
				"--color-by", "label", "--theme", "dark", "--title", "Impianto",
				"--highlight", "x", "--block-size", "4096");
		assertEquals(0, result.exitCode(), result.err());
		final GraphRenderOptions options = renderOptions(4096);
		// format from the extension of the file
		assertEquals(GraphRenderOptions.Format.DOT, options.format());
		final GraphRenderOptions.Vertices vertices = options.vertices();
		assertEquals(List.of("plc-1", "plc-2"), vertices.roots());
		assertEquals(2, vertices.depth());
		assertEquals(GraphRenderOptions.Direction.OUT, vertices.direction());
		assertEquals(List.of("a", "b"), vertices.types());
		assertEquals(List.of("c"), vertices.excludeTypes());
		assertEquals(List.of("l"), vertices.labels());
		assertEquals(List.of("d1"), vertices.directories());
		assertEquals("pump", vertices.match());
		assertEquals(Map.of("zone", "A"), vertices.where());
		assertEquals("g.V()", vertices.expression());
		assertTrue(vertices.includeEphemeral());
		assertFalse(vertices.includeIsolated());
		assertEquals(50, vertices.max());
		assertEquals(List.of("feeds"), options.edges().labels());
		assertEquals(List.of("link"), options.edges().excludeLabels());
		assertEquals(List.of("name", "zone"), options.properties().vertex());
		assertEquals(List.of("w*"), options.properties().edge());
		assertTrue(options.properties().includeRuntime());
		assertEquals(GraphRenderOptions.Algorithm.FORCE,
				options.layout().algorithm());
		assertEquals(GraphRenderOptions.LayoutDirection.DOWN,
				options.layout().direction());
		assertEquals(GraphRenderOptions.GroupBy.DIRECTORY,
				options.layout().groupBy());
		assertEquals(GraphRenderOptions.ColorBy.LABEL,
				options.style().colorBy());
		assertEquals(GraphRenderOptions.Theme.DARK, options.style().theme());
		assertEquals("Impianto", options.style().title());
		assertEquals(List.of("x"), options.style().highlight());
	}

	@Test
	void renderOptionsFileAndErrors() throws Exception {
		stubRender("svg");
		final Path base = tmp.resolve("base.json");
		final GraphRenderOptions json = new GraphRenderOptions();
		json.vertices().where(Map.of("zone", "B", "kind", "pump"));
		json.style().title("Base");
		Files.writeString(base, json.toJson());
		final Result result = run("render", "--options-file", base.toString(),
				"--where", "zone=A", "--format", "svg", "-f",
				tmp.resolve("x.dot").toString());
		assertEquals(0, result.exitCode(), result.err());
		final GraphRenderOptions options = renderOptions(
				DataExchangeProtocol.DEFAULT_BLOCK_SIZE);
		// --format wins over the extension, --where merged with the file
		assertEquals(GraphRenderOptions.Format.SVG, options.format());
		assertEquals("A", options.vertices().where().get("zone"));
		assertEquals("pump", options.vertices().where().get("kind"));
		assertEquals("Base", options.style().title());

		final Result missing = run("render", "--options-file",
				tmp.resolve("nessuno.json").toString());
		assertEquals(ExitCodes.USAGE, missing.exitCode());
		assertTrue(missing.err().contains("impossibile leggere"),
				missing.err());
		final Path invalid = tmp.resolve("invalid.json");
		Files.writeString(invalid, "{\"layout\":{\"algorithm\":\"spirale\"}}");
		final Result invalidOptions = run("render", "--options-file",
				invalid.toString());
		assertEquals(ExitCodes.USAGE, invalidOptions.exitCode(),
				invalidOptions.err());
		assertEquals(ExitCodes.USAGE,
				run("render", "--block-size", "10").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("render", "--layout", "spirale").exitCode());
	}

	@Test
	void renderToFileAndStdout() throws Exception {
		stubRender("svg");
		final Path file = tmp.resolve("impianto.svg");
		final Result text = run("render", "-f", file.toString());
		assertEquals(0, text.exitCode(), text.err());
		assertEquals("<svg/>", Files.readString(file));
		assertTrue(text.out().startsWith("disegnati 3 vertici e 2 archi"),
				text.out());
		assertEquals(GraphRenderOptions.Format.SVG,
				renderOptions(DataExchangeProtocol.DEFAULT_BLOCK_SIZE)
						.format());
		final JsonNode json = MAPPER.readTree(
				run("render", "-f", file.toString(), "-o", "json").out());
		assertEquals("svg", json.get("format").asText());
		assertEquals("troppi vertici", json.get("warnings").get(0).asText());
		assertEquals(file.toString(),
				run("render", "-f", file.toString(), "-o", "value").out()
						.trim());
		final Result stdout = run("render");
		assertEquals(0, stdout.exitCode(), stdout.err());
		assertTrue(stdout.out().isEmpty());
		assertEquals("<svg/>", binaryText());
		binaryOut.reset();
		run("render", "-f", "-");
		assertEquals("<svg/>", binaryText());
	}

	@Test
	void snapshotClear() throws Exception {
		final Result refused = run("snapshot", "clear");
		assertEquals(ExitCodes.USAGE, refused.exitCode());
		verifyNoInteractions(mock.client);
		when(mock.snapshots.clearGraph())
				.thenReturn(new GraphChangeResult(5, 4, null));
		final Result cleared = run("snapshot", "clear", "--yes");
		assertEquals(0, cleared.exitCode(), cleared.err());
		assertEquals("eliminati 5 vertici e 4 archi", cleared.out().trim());
		assertEquals("5 4",
				run("snapshot", "clear", "--yes", "-o", "value").out().trim());
		final JsonNode json = MAPPER.readTree(
				run("snapshot", "clear", "--yes", "-o", "jsonl").out());
		assertEquals(5, json.get("vertices").asInt());
	}

	@Test
	void snapshotExportToFileAndStdout() throws Exception {
		when(mock.snapshots.exportGraph(anyBoolean(), anyBoolean(), anyInt()))
				.thenReturn(new GraphSnapshot(GRAPHSON, false, 2, 1, 1));
		final Path file = tmp.resolve("grafo.json");
		final Result text = run("snapshot", "export", "--no-compress",
				"--include-ephemeral", "--block-size", "2048", "-f",
				file.toString());
		assertEquals(0, text.exitCode(), text.err());
		assertArrayEquals(GRAPHSON, Files.readAllBytes(file));
		assertTrue(text.out().startsWith("esportati 2 vertici e 1 archi"),
				text.out());
		verify(mock.snapshots).exportGraph(false, true, 2048);
		final JsonNode json = MAPPER.readTree(
				run("snapshot", "export", "-f", file.toString(), "-o", "json")
						.out());
		assertEquals(file.toString(), json.get("file").asText());
		assertEquals(2, json.get("vertices").asInt());
		verify(mock.snapshots).exportGraph(true, false,
				DataExchangeProtocol.DEFAULT_BLOCK_SIZE);
		assertEquals(file.toString(), run("snapshot", "export", "-f",
				file.toString(), "-o", "value").out().trim());
		final Result stdout = run("snapshot", "export");
		assertEquals(0, stdout.exitCode(), stdout.err());
		assertTrue(stdout.out().isEmpty());
		assertArrayEquals(GRAPHSON, binaryOut.toByteArray());
		assertEquals(ExitCodes.USAGE,
				run("snapshot", "export", "--block-size", "1").exitCode());
		assertEquals(ExitCodes.USAGE, run("snapshot", "export",
				"--block-size", String.valueOf(Integer.MAX_VALUE)).exitCode());
	}

	@Test
	void snapshotImportFromFileAndStdin() throws Exception {
		when(mock.snapshots.importGraph(any(GraphSnapshot.class), anyBoolean(),
				anyInt())).thenReturn(new GraphChangeResult(2, 1, "saltato x"));
		final Path file = tmp.resolve("grafo.json");
		Files.write(file, GRAPHSON);
		final Result text = run("snapshot", "import", "--clear", "-f",
				file.toString(), "--block-size", "8192");
		assertEquals(0, text.exitCode(), text.err());
		assertEquals("importati 2 vertici e 1 archi", text.out().trim());
		verify(mock.snapshots).importGraph(GraphSnapshot.of(GRAPHSON), true,
				8192);
		final Result stdin = runWithStdin(GRAPHSON, "snapshot", "import");
		assertEquals(0, stdin.exitCode(), stdin.err());
		verify(mock.snapshots).importGraph(GraphSnapshot.of(GRAPHSON), false,
				DataExchangeProtocol.DEFAULT_BLOCK_SIZE);

		final Result empty = runWithStdin(new byte[0], "snapshot", "import",
				"-f", "-");
		assertEquals(ExitCodes.USAGE, empty.exitCode());
		assertTrue(empty.err().contains("vuoto"), empty.err());
		final Result missing = run("snapshot", "import", "-f",
				tmp.resolve("nessuno").toString());
		assertEquals(ExitCodes.USAGE, missing.exitCode());
		assertTrue(missing.err().contains("impossibile leggere"),
				missing.err());
		assertEquals(ExitCodes.USAGE, run("snapshot", "import", "-f",
				file.toString(), "--block-size", "0").exitCode());
		verify(mock.snapshots, never()).clearGraph();
	}
}
