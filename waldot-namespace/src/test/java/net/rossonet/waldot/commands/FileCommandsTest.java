package net.rossonet.waldot.commands;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.api.files.ServerFileRoots;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * file list / file get: roots, files inline and by blocks, paths that try to
 * leave the root.
 */
@TestInstance(Lifecycle.PER_CLASS)
class FileCommandsTest {

	private static final ObjectMapper JSON = new ObjectMapper();

	private WaldotGraph graph;

	@TempDir
	Path temp;

	private Path root;

	@AfterEach
	void unregister() {
		ServerFileRoots.unregister("test");
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
	void directoryIsNotAFile() throws Exception {
		Files.createDirectories(root.resolve("sub"));
		final Object[] out = call(DataExchangeProtocol.FILE_GET_COMMAND, "test", "sub", "");
		assertTrue(String.valueOf(out[5]).contains("non è un file"), String.valueOf(out[5]));
	}

	@Test
	void emptyFile() throws Exception {
		Files.write(root.resolve("empty.txt"), new byte[0]);
		final Object[] out = call(DataExchangeProtocol.FILE_GET_COMMAND, "test", "empty.txt", "");
		assertEquals("", out[5]);
		assertEquals("", out[0]);
		assertEquals("0", out[3]);
	}

	@Test
	void largeFileIsReadByBlocks() throws Exception {
		final byte[] content = new byte[5000];
		new Random(7).nextBytes(content);
		Files.write(root.resolve("big.bin"), content);
		final Object[] out = call(DataExchangeProtocol.FILE_GET_COMMAND, "test", "big.bin", "1024");
		assertEquals("", out[5], String.valueOf(out[5]));
		assertEquals("", out[0], "no inline data");
		final String transferId = (String) out[1];
		assertEquals("5", out[2], "5000 bytes in blocks of 1024");
		assertEquals("5000", out[3]);
		final ByteArrayOutputStream read = new ByteArrayOutputStream();
		for (int i = 0; i < 5; i++) {
			final Object[] block = call(DataExchangeProtocol.READ_GRAPH_BLOCK_COMMAND, transferId, String.valueOf(i));
			assertEquals("", block[2], String.valueOf(block[2]));
			read.write(Base64.getDecoder().decode((String) block[0]));
			assertEquals(String.valueOf(i == 4), block[1], "last block flag");
		}
		assertArrayEquals(content, read.toByteArray());
	}

	@Test
	void listFilesOfARoot() throws Exception {
		Files.writeString(root.resolve("a.json"), "{}");
		Files.createDirectories(root.resolve("sub"));
		Files.writeString(root.resolve("sub").resolve("b.json"), "{}");
		final Object[] out = call(DataExchangeProtocol.FILE_LIST_COMMAND, "test", "");
		assertEquals("", out[1], String.valueOf(out[1]));
		final JsonNode list = JSON.readTree((String) out[0]);
		assertEquals(2, list.get("files").size(), list.toString());
		assertEquals("a.json", list.get("files").get(0).get("name").asText());
		assertEquals(2, list.get("files").get(0).get("size").asInt());
		assertTrue(list.get("files").get(1).get("directory").asBoolean());
		final JsonNode sub = JSON.readTree((String) call(DataExchangeProtocol.FILE_LIST_COMMAND, "test", "sub")[0]);
		assertEquals("sub/b.json", sub.get("files").get(0).get("name").asText());
	}

	@Test
	void listRootsWithoutARoot() throws Exception {
		final JsonNode roots = JSON.readTree((String) call(DataExchangeProtocol.FILE_LIST_COMMAND, "", "")[0]);
		boolean found = false;
		for (final JsonNode r : roots.get("roots")) {
			if (r.get("root").asText().equals("test")) {
				found = true;
				assertTrue(r.get("exists").asBoolean());
			}
		}
		assertTrue(found, roots.toString());
	}

	@ParameterizedTest
	@ValueSource(strings = { "../outside.txt", "sub/../../outside.txt", "/etc/passwd" })
	void pathsOutsideTheRootAreRefused(final String path) throws Exception {
		Files.writeString(temp.resolve("outside.txt"), "secret");
		Files.createDirectories(root.resolve("sub"));
		final Object[] out = call(DataExchangeProtocol.FILE_GET_COMMAND, "test", path, "");
		assertFalse(String.valueOf(out[5]).isBlank(), "refused: " + path);
		assertEquals("", out[0]);
	}

	@BeforeEach
	void root() throws Exception {
		root = Files.createDirectories(temp.resolve("root"));
		ServerFileRoots.register("test", root);
	}

	@Test
	void smallFileIsInline() throws Exception {
		Files.writeString(root.resolve("dump.json"), "{\"vertices\":[]}");
		final Object[] out = call(DataExchangeProtocol.FILE_GET_COMMAND, "test", "dump.json", "");
		assertEquals("", out[5], String.valueOf(out[5]));
		assertEquals("{\"vertices\":[]}", new String(Base64.getDecoder().decode((String) out[0])));
		assertEquals("1", out[2]);
		assertEquals(root.resolve("dump.json").toRealPath().toString(), out[4]);
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
	void symbolicLinkOutsideTheRootIsRefused() throws Exception {
		Files.writeString(temp.resolve("outside.txt"), "secret");
		Files.createSymbolicLink(root.resolve("link.txt"), temp.resolve("outside.txt"));
		final Object[] out = call(DataExchangeProtocol.FILE_GET_COMMAND, "test", "link.txt", "");
		assertTrue(String.valueOf(out[5]).contains("link simbolico"), String.valueOf(out[5]));
	}

	@Test
	void unknownRootAndMissingFile() {
		final Object[] unknown = call(DataExchangeProtocol.FILE_GET_COMMAND, "nope", "x", "");
		assertTrue(String.valueOf(unknown[5]).contains("radice sconosciuta"), String.valueOf(unknown[5]));
		assertTrue(String.valueOf(unknown[5]).contains("test"), "lists the roots: " + unknown[5]);
		final Object[] missing = call(DataExchangeProtocol.FILE_GET_COMMAND, "test", "missing.json", "");
		assertTrue(String.valueOf(missing[5]).contains("non esiste"), String.valueOf(missing[5]));
	}
}
