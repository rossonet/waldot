package net.rossonet.waldot.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.cli.commands.SubscribeCommand;
import net.rossonet.waldot.cli.support.ExitCodes;
import picocli.CommandLine;

/**
 * Parsing, help and exit codes of the command line (no server: the
 * connection tests use a closed port).
 */
class WaldotCliTest {

	private record Result(int exitCode, String out, String err) {
	}

	private static Result run(final String... args) {
		final StringWriter out = new StringWriter();
		final StringWriter err = new StringWriter();
		final CommandLine commandLine = WaldotCli.newCommandLine(new PrintWriter(out), new PrintWriter(err));
		final int exitCode = commandLine.execute(args);
		return new Result(exitCode, out.toString(), err.toString());
	}

	@Test
	void connectionFailureIsExitCode3() {
		final Result result = run("about", "--endpoint", "opc.tcp://127.0.0.1:1/waldot", "--connect-timeout", "1s");
		assertEquals(ExitCodes.CONNECTION, result.exitCode(), result.err());
		assertTrue(result.out().isEmpty(), "stdout must be empty: " + result.out());
		assertTrue(result.err().contains("errore di connessione"), result.err());
	}

	@Test
	void helpIsItalianOnStdout() {
		final Result root = run("--help");
		assertEquals(0, root.exitCode());
		assertTrue(root.out().contains("Uso: waldot [OPZIONI] COMANDO"), root.out());
		assertTrue(root.out().contains("Codici di uscita:"));
		assertTrue(root.out().contains("subscribe"));
		final Result subscribe = run("help", "subscribe");
		assertEquals(0, subscribe.exitCode());
		assertTrue(subscribe.out().contains("Uso: waldot subscribe [OPZIONI]"), subscribe.out());
		assertTrue(subscribe.out().contains("mosquitto_pub -t waldot/sensor-1 -l"));
		assertTrue(subscribe.out().contains("WALDOT_ENDPOINT"));
		final Result nested = run("help", "snapshot", "import");
		assertEquals(0, nested.exitCode());
		assertTrue(nested.out().contains("Uso: waldot snapshot import [OPZIONI]"), nested.out());
		assertEquals(ExitCodes.USAGE, run("help", "nessuno").exitCode());
		final Result version = run("--version");
		assertEquals(0, version.exitCode());
		assertTrue(version.out().startsWith("waldot "));
	}

	@Test
	void topicLevels() {
		assertEquals("a_b", SubscribeCommand.topicLevel("a b"));
		assertEquals("x_y_z", SubscribeCommand.topicLevel("x+y#z"));
		assertEquals("sensor-1/value", SubscribeCommand.topicLevel("sensor-1/value"));
	}

	@Test
	void usageErrorsAreExitCode2() {
		assertEquals(ExitCodes.USAGE, run().exitCode());
		assertEquals(ExitCodes.USAGE, run("nonesiste").exitCode());
		assertEquals(ExitCodes.USAGE, run("snapshot").exitCode());
		final Result clear = run("snapshot", "clear");
		assertEquals(ExitCodes.USAGE, clear.exitCode());
		assertTrue(clear.err().contains("--yes"), clear.err());
		assertEquals(ExitCodes.USAGE, run("subscribe").exitCode());
		assertEquals(ExitCodes.USAGE, run("subscribe", "-p", "senza-chiave").exitCode());
		assertEquals(ExitCodes.USAGE, run("subscribe", "-p", "a:b", "--count", "0").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("subscribe", "-p", "a:b", "--deadband-absolute", "1", "--deadband-percent", "1").exitCode());
		assertEquals(ExitCodes.USAGE, run("query", "g.V()", "-o", "topic").exitCode());
		assertEquals(ExitCodes.USAGE, run("query", "g.V()", "-o", "xml").exitCode());
		assertEquals(ExitCodes.USAGE, run("query").exitCode());
		assertEquals(ExitCodes.USAGE, run("write", "n", "x", "--type", "bool").exitCode());
		assertEquals(ExitCodes.USAGE, run("call", "o", "m", "int32:abc").exitCode());
		assertEquals(ExitCodes.USAGE, run("about", "--connect-timeout", "presto").exitCode());
		assertEquals(ExitCodes.USAGE, run("about", "--security-policy", "Nessuna").exitCode());
		assertEquals(ExitCodes.USAGE, run("about", "--identity-cert", "/tmp/c.pem").exitCode());
		assertEquals(ExitCodes.USAGE, run("vertex", "add", "--id", "x").exitCode());
		final Result unknown = run("about", "--sconosciuta");
		assertEquals(ExitCodes.USAGE, unknown.exitCode());
		assertTrue(unknown.err().startsWith("waldot: uso errato:"), unknown.err());
		assertTrue(unknown.out().isEmpty());
	}

	@Test
	void verbosityCount() {
		assertEquals(0, WaldotCli.verbosity(new String[] { "query", "x" }));
		assertEquals(1, WaldotCli.verbosity(new String[] { "-v", "query" }));
		assertEquals(3, WaldotCli.verbosity(new String[] { "query", "-vv", "--verbose" }));
		assertEquals(0, WaldotCli.verbosity(new String[] { "call", "--", "-v" }));
	}
}
