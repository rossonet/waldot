package net.rossonet.waldot.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Smoke tests of the native binary built by GraalVM ({@code nativeCompile}):
 * the binary runs as a separate process, without any JVM in the environment.
 * The path comes from the system property {@code wotctl.binary} (set by the
 * Gradle task {@code nativeSmokeTest}). With the environment variable
 * {@code WOTCTL_SMOKE_ENDPOINT} the tests with a real WaldOT server run too.
 */
class NativeBinarySmokeTest {

	/** Exit code and output of a process. */
	record Result(int exitCode, String out, String err) {
	}

	private static Path binary;

	private static final String CLOSED_ENDPOINT = "opc.tcp://127.0.0.1:1/waldot";

	private static final long TIMEOUT_SECONDS = 60;

	@BeforeAll
	static void binary() {
		final String path = System.getProperty("wotctl.binary");
		assertTrue(path != null && !path.isBlank(),
				"system property wotctl.binary not set");
		binary = Path.of(path);
		assertTrue(Files.isExecutable(binary), "not executable: " + binary);
	}

	private static String endpoint() {
		final String endpoint = System.getenv("WOTCTL_SMOKE_ENDPOINT");
		return endpoint == null || endpoint.isBlank() ? null : endpoint.trim();
	}

	private static String read(final InputStream input) {
		try {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (final IOException e) {
			return "";
		}
	}

	static Result run(final byte[] stdin, final String... args)
			throws Exception {
		final List<String> command = new ArrayList<>();
		command.add(binary.toString());
		command.addAll(List.of(args));
		final ProcessBuilder builder = new ProcessBuilder(command);
		final Map<String, String> environment = builder.environment();
		// no JVM and no configuration from the environment of the build
		environment.keySet().removeIf(key -> key.startsWith("WALDOT_")
				|| key.startsWith("JAVA") || key.startsWith("GRAALVM")
				|| key.equals("CLASSPATH") || key.startsWith("JDK_"));
		environment.put("PATH", "/usr/bin:/bin");
		final Process process = builder.start();
		final CompletableFuture<String> out = CompletableFuture
				.supplyAsync(() -> read(process.getInputStream()));
		final CompletableFuture<String> err = CompletableFuture
				.supplyAsync(() -> read(process.getErrorStream()));
		try (var input = process.getOutputStream()) {
			if (stdin != null) {
				input.write(stdin);
			}
		}
		if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			throw new AssertionError("timeout: " + String.join(" ", args));
		}
		return new Result(process.exitValue(),
				out.get(10, TimeUnit.SECONDS), err.get(10, TimeUnit.SECONDS));
	}

	static Result run(final String... args) throws Exception {
		return run(null, args);
	}

	@TempDir
	Path tmp;

	@Test
	void completionScript() throws Exception {
		final Result result = run("completion");
		assertEquals(0, result.exitCode(), result.err());
		assertTrue(result.out().contains("complete -F _complete_wotctl"),
				result.out());
		assertTrue(result.out().contains("_picocli_wotctl_snapshot_import"));
		assertTrue(result.out().contains("SignAndEncrypt"));
		assertTrue(result.out().contains("layered"));
	}

	@Test
	void connectionFailureIsExitCode3() throws Exception {
		// opens a real OPC UA connection: Milo, Netty and BouncyCastle
		// initialized at run time
		final Result result = run("about", "--endpoint", CLOSED_ENDPOINT,
				"--connect-timeout", "2s");
		assertEquals(3, result.exitCode(), result.err());
		assertTrue(result.out().isEmpty(), result.out());
		assertTrue(result.err().startsWith("wotctl: errore di connessione:"),
				result.err());
		assertFalse(result.err().contains("Exception in thread"),
				result.err());
	}

	@Test
	void helpAndResourceBundle() throws Exception {
		final Result root = run("--help");
		assertEquals(0, root.exitCode(), root.err());
		assertTrue(root.out().contains("Uso: wotctl [OPZIONI] COMANDO"),
				root.out());
		assertTrue(root.out().contains("Codici di uscita:"), root.out());
		assertTrue(root.err().isEmpty(), root.err());
		final Result nested = run("help", "snapshot", "import");
		assertEquals(0, nested.exitCode(), nested.err());
		assertTrue(
				nested.out().contains("Uso: wotctl snapshot import [OPZIONI]"),
				nested.out());
		final Result subscribe = run("subscribe", "--help");
		assertTrue(subscribe.out().contains("WALDOT_ENDPOINT"),
				subscribe.out());
	}

	@Test
	void identityWithoutKeyIsUsageError() throws Exception {
		final Path cert = Files.writeString(tmp.resolve("c.pem"), "x");
		final Result result = run("about", "--identity-cert", cert.toString(),
				"--endpoint", CLOSED_ENDPOINT);
		assertEquals(2, result.exitCode(), result.err());
		assertTrue(result.err().contains("--identity-key"), result.err());
	}

	@Test
	void queryFromEmptyStdinFailsCleanly() throws Exception {
		final Result result = run(new byte[0], "query", "-f", "-", "-o",
				"value", "--endpoint", CLOSED_ENDPOINT, "--connect-timeout",
				"1s");
		assertTrue(result.exitCode() == 2 || result.exitCode() == 3,
				result.exitCode() + ": " + result.err());
		assertTrue(result.err().startsWith("wotctl: "), result.err());
		assertFalse(result.err().contains("Exception in thread"),
				result.err());
	}

	@Test
	void usageErrorsAreExitCode2() throws Exception {
		final Result unknown = run("nonesiste");
		assertEquals(2, unknown.exitCode());
		assertTrue(unknown.err().startsWith("wotctl: uso errato:"),
				unknown.err());
		final Result format = run("query", "-o", "xml", "g.V()");
		assertEquals(2, format.exitCode());
		assertTrue(format.err().startsWith("wotctl: uso errato:"),
				format.err());
		assertEquals(2, run("snapshot", "clear").exitCode());
		assertEquals(2, run("about", "--connect-timeout", "presto").exitCode());
	}

	@Test
	void verboseLogsOnStderr() throws Exception {
		final Result result = run("-vv", "about", "--endpoint",
				CLOSED_ENDPOINT, "--connect-timeout", "1s");
		assertEquals(3, result.exitCode(), result.err());
		// slf4j-simple configured at run time: debug messages with the date
		assertTrue(result.err().contains(" DEBUG "), result.err());
		assertTrue(result.out().isEmpty(), result.out());
	}

	@Test
	void versionOfTheBuild() throws Exception {
		final Result result = run("--version");
		assertEquals(0, result.exitCode(), result.err());
		final String expected = System.getProperty("wotctl.version");
		assertTrue(result.out().startsWith("wotctl "
				+ (expected == null ? "" : expected + "\n")), result.out());
		assertFalse(result.out().contains("(sviluppo)"), result.out());
		assertTrue(result.out().contains("binario nativo GraalVM"),
				result.out());
	}

	@Test
	void withServer() throws Exception {
		final String endpoint = endpoint();
		assumeTrue(endpoint != null,
				"WOTCTL_SMOKE_ENDPOINT not set: no WaldOT server");
		final String[] connection = {"--endpoint", endpoint};
		final Result about = run(with(connection, "about", "-o", "json"));
		assertEquals(0, about.exitCode(), about.err());
		assertTrue(about.out().trim().startsWith("["), about.out());

		final Result commands = run(
				with(connection, "commands", "list", "-o", "jsonl"));
		assertEquals(0, commands.exitCode(), commands.err());
		assertTrue(commands.out().contains("\"name\":\"about\""),
				commands.out());

		final String id = "smoke-" + UUID.randomUUID();
		final Result added = run(with(connection, "vertex", "add", "--id", id,
				"-l", "smoke", "-P", "zone=A", "-o", "json"));
		assertEquals(0, added.exitCode(), added.err());
		final Result property = run(
				with(connection, "property", "get", id, "zone", "-o", "value"));
		assertEquals(0, property.exitCode(), property.err());
		assertEquals("A", property.out().trim());
		final Result query = run(with(connection, "query", "-o", "jsonl",
				"g.V('" + id + "')"));
		assertEquals(0, query.exitCode(), query.err());
		assertTrue(query.out().contains("\"type\":\"vertex\""), query.out());
		final Result count = run(
				with(connection, "query", "g.V().count()", "-o", "value"));
		assertEquals(0, count.exitCode(), count.err());
		assertTrue(Long.parseLong(count.out().trim()) >= 1, count.out());
		final Result describe = run(
				with(connection, "describe", id, "-o", "json"));
		assertEquals(0, describe.exitCode(), describe.err());
		assertTrue(describe.out().contains("\"zone\""), describe.out());

		final Path backup = tmp.resolve("grafo.json");
		final Result export = run(with(connection, "snapshot", "export",
				"--no-compress", "-f", backup.toString(), "-o", "json"));
		assertEquals(0, export.exitCode(), export.err());
		assertTrue(Files.readString(backup).contains(id));
		final Result gzip = run(with(connection, "snapshot", "export", "-f",
				tmp.resolve("grafo.json.gz").toString()));
		assertEquals(0, gzip.exitCode(), gzip.err());

		final Result render = run(with(connection, "render", "-f",
				tmp.resolve("grafo.svg").toString(), "--root", id));
		assertEquals(0, render.exitCode(), render.err());
		assertTrue(Files.readString(tmp.resolve("grafo.svg")).contains("<svg"));

		final Result browse = run(with(connection, "browse", "-o", "value"));
		assertEquals(0, browse.exitCode(), browse.err());
		assertTrue(browse.out().contains("i=2253"), browse.out());
		final Result time = run(with(connection, "read", "i=2258", "-o",
				"jsonl"));
		assertEquals(0, time.exitCode(), time.err());
		assertTrue(time.out().contains("\"status\":\"Good\""), time.out());

		final Result subscribe = run(with(connection, "subscribe", "-n",
				"i=2258", "--count", "1", "-o", "jsonl"));
		assertEquals(0, subscribe.exitCode(), subscribe.err());
		assertTrue(subscribe.out().contains("\"type\":\"data\""),
				subscribe.out());

		final Result deleted = run(with(connection, "vertex", "delete", id));
		assertEquals(0, deleted.exitCode(), deleted.err());
	}

	private static String[] with(final String[] connection,
			final String... args) {
		final String[] all = new String[args.length + connection.length];
		System.arraycopy(args, 0, all, 0, args.length);
		System.arraycopy(connection, 0, all, args.length, connection.length);
		return all;
	}
}
