package net.rossonet.waldot.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.cli.CliTestSupport.Result;
import net.rossonet.waldot.cli.commands.SubscribeCommand;
import net.rossonet.waldot.cli.support.CliException;
import net.rossonet.waldot.cli.support.ExitCodes;
import net.rossonet.waldot.cli.support.OutputClosedException;
import net.rossonet.waldot.client.api.WaldotClientException;

/**
 * Parsing, help, logging and exit codes of the command line (the connection
 * test without mock uses a closed port).
 */
class WaldotCliTest extends CommandTestBase {

	private static final String CLIENT_LOGGER = "org.slf4j.simpleLogger.log.net.rossonet.waldot.client";

	private static final String DEFAULT_LEVEL = "org.slf4j.simpleLogger.defaultLogLevel";

	private static final String NETTY_LOGGER = "org.slf4j.simpleLogger.log.io.netty";

	private Properties savedProperties;

	@Test
	void connectionFailureIsExitCode3() {
		net.rossonet.waldot.cli.support.ClientCommand.resetTestHooks();
		final Result result = run("about", "--endpoint",
				"opc.tcp://127.0.0.1:1/waldot", "--connect-timeout", "1s");
		assertEquals(ExitCodes.CONNECTION, result.exitCode(), result.err());
		assertTrue(result.out().isEmpty(),
				"stdout must be empty: " + result.out());
		assertTrue(result.err().startsWith("wotctl: errore di connessione: "
				+ "connessione a opc.tcp://127.0.0.1:1/waldot non riuscita"),
				result.err());
	}

	@Test
	void connectionFailureWithMockClient() {
		when(mock.client.connect(any(Duration.class)))
				.thenThrow(new WaldotClientException("rifiutata"));
		final Result result = run("about", "--connect-timeout", "2s");
		assertEquals(ExitCodes.CONNECTION, result.exitCode(), result.err());
		assertTrue(result.err().contains("non riuscita: rifiutata"),
				result.err());
		org.mockito.Mockito.verify(mock.client).connect(Duration.ofSeconds(2));
		org.mockito.Mockito.verify(mock.client).close();
	}

	@Test
	void exitCodesOfTheExceptions() {
		assertEquals(7, WaldotCli.exitCode(new CliException(7, "x", null)));
		assertEquals(ExitCodes.SERVER_ERROR,
				WaldotCli.exitCode(CliException.server("x")));
		assertEquals(ExitCodes.CONNECTION,
				WaldotCli.exitCode(CliException.connection("x", null)));
		assertEquals(ExitCodes.OK,
				WaldotCli.exitCode(new OutputClosedException()));
		assertEquals(ExitCodes.CONNECTION,
				WaldotCli.exitCode(new WaldotClientException("t",
						new StatusCode(StatusCodes.Bad_Timeout), null)));
		assertEquals(ExitCodes.CONNECTION,
				WaldotCli.exitCode(new WaldotClientException("t",
						new StatusCode(StatusCodes.Bad_SessionClosed), null)));
		assertEquals(ExitCodes.SERVER_ERROR,
				WaldotCli.exitCode(new WaldotClientException("t",
						new StatusCode(StatusCodes.Bad_NodeIdUnknown), null)));
		assertEquals(ExitCodes.SERVER_ERROR,
				WaldotCli.exitCode(new WaldotClientException("senza stato")));
		assertEquals(ExitCodes.INTERRUPTED,
				WaldotCli.exitCode(new InterruptedException()));
		assertEquals(ExitCodes.SERVER_ERROR,
				WaldotCli.exitCode(new IllegalStateException()));
	}

	@Test
	void exceptionsOfTheCommands() {
		when(mock.commands.about()).thenThrow(new WaldotClientException(
				"scaduta", new StatusCode(StatusCodes.Bad_Timeout), null));
		final Result timeout = run("about");
		assertEquals(ExitCodes.CONNECTION, timeout.exitCode());
		assertEquals("wotctl: errore di connessione: scaduta",
				timeout.err().trim());

		doThrow(new IllegalStateException()).when(mock.commands).about();
		final Result noMessage = run("about");
		assertEquals(ExitCodes.SERVER_ERROR, noMessage.exitCode());
		// without a message: the name of the exception
		assertEquals("wotctl: errore: IllegalStateException",
				noMessage.err().trim());

		// with -vv also the stack trace (the verbosity is set by main)
		WaldotCli.configureLogging(new String[]{"about", "-vv"});
		try {
			final Result verbose = run("about", "-vv");
			assertTrue(verbose.err().contains("at net.rossonet.waldot.cli"),
					verbose.err());
		} finally {
			WaldotCli.configureLogging(new String[0]);
		}

		// the downstream process closed the pipe: exit code 0, no message
		doThrow(new OutputClosedException()).when(mock.commands).about();
		final Result closed = run("about");
		assertEquals(ExitCodes.OK, closed.exitCode());
		assertTrue(closed.err().isEmpty(), closed.err());
	}

	@Test
	void helpIsItalianOnStdout() {
		final Result root = run("--help");
		assertEquals(0, root.exitCode());
		assertTrue(root.out().contains("Uso: wotctl [OPZIONI] COMANDO"),
				root.out());
		assertTrue(root.out().contains("Codici di uscita:"));
		assertTrue(root.out().contains("subscribe"));
		assertFalse(root.out().contains("  waldot "), root.out());
		final Result subscribe = run("help", "subscribe");
		assertEquals(0, subscribe.exitCode());
		assertTrue(subscribe.out().contains("Uso: wotctl subscribe [OPZIONI]"),
				subscribe.out());
		assertTrue(subscribe.out()
				.contains("mosquitto_pub -t waldot/sensor-1 -l"));
		assertTrue(subscribe.out().contains("WALDOT_ENDPOINT"));
		final Result nested = run("help", "snapshot", "import");
		assertEquals(0, nested.exitCode());
		assertTrue(
				nested.out().contains("Uso: wotctl snapshot import [OPZIONI]"),
				nested.out());
		assertTrue(run("help").out().contains("Uso: wotctl [OPZIONI] COMANDO"));
		assertTrue(run("help", "--help").out().contains("Uso: wotctl help"));
		final Result unknown = run("help", "snapshot", "nessuno");
		assertEquals(ExitCodes.USAGE, unknown.exitCode());
		assertTrue(unknown.err().contains("comandi di 'snapshot'"),
				unknown.err());
		assertEquals(ExitCodes.USAGE, run("help", "nessuno").exitCode());
		// every command and subcommand has an Italian help
		for (final String[] command : new String[][]{{"about"}, {"query"},
				{"describe"}, {"commands", "list"}, {"commands", "call"},
				{"vertex", "add"}, {"vertex", "delete"}, {"edge", "add"},
				{"edge", "delete"}, {"property", "get"}, {"property", "set"},
				{"snapshot", "export"}, {"snapshot", "clear"}, {"render"},
				{"browse"}, {"read"}, {"write"}, {"call"}, {"resolve"},
				{"completion"}}) {
			final String[] args = new String[command.length + 1];
			System.arraycopy(command, 0, args, 0, command.length);
			args[command.length] = "--help";
			final Result help = run(args);
			assertEquals(0, help.exitCode(), String.join(" ", command));
			assertTrue(help.out().contains(
					"Uso: wotctl " + String.join(" ", command) + " [OPZIONI]"),
					help.out());
			assertTrue(help.out().contains("Codici di uscita:"), help.out());
		}
	}

	@Test
	void loggingConfiguration() {
		WaldotCli.configureLogging(new String[]{"query", "x"});
		assertEquals(
				System.getenv("WALDOT_LOG_LEVEL") == null ? "warn" : System
						.getenv("WALDOT_LOG_LEVEL").trim().toLowerCase(),
				System.getProperty(DEFAULT_LEVEL));
		assertEquals("System.err",
				System.getProperty("org.slf4j.simpleLogger.logFile"));
		assertEquals("true", System.getProperty("io.netty.noUnsafe"));
		assertEquals("warn", System.getProperty(NETTY_LOGGER));
		clearLoggingProperties();
		WaldotCli.configureLogging(new String[]{"-v", "query"});
		assertEquals("info", System.getProperty(DEFAULT_LEVEL));
		clearLoggingProperties();
		WaldotCli.configureLogging(new String[]{"query", "-vv"});
		assertEquals("debug", System.getProperty(DEFAULT_LEVEL));
		clearLoggingProperties();
		WaldotCli.configureLogging(new String[]{"-vvv", "query"});
		assertEquals("trace", System.getProperty(DEFAULT_LEVEL));
		// with -vvv also the network stack
		assertNull(System.getProperty(NETTY_LOGGER));
		clearLoggingProperties();
		// a property given on the command line of the JVM wins
		System.setProperty(DEFAULT_LEVEL, "error");
		WaldotCli.configureLogging(new String[]{"-v"});
		assertEquals("error", System.getProperty(DEFAULT_LEVEL));
	}

	private void clearLoggingProperties() {
		System.getProperties().stringPropertyNames().stream()
				.filter(key -> key.startsWith("org.slf4j.simpleLogger.")
						|| key.equals("io.netty.noUnsafe"))
				.forEach(System::clearProperty);
	}

	@AfterEach
	void restoreProperties() {
		System.setProperties(savedProperties);
	}

	@BeforeEach
	void saveProperties() {
		savedProperties = (Properties) System.getProperties().clone();
		clearLoggingProperties();
	}

	@Test
	void topicLevels() {
		assertEquals("a_b", SubscribeCommand.topicLevel("a b"));
		assertEquals("x_y_z", SubscribeCommand.topicLevel("x+y#z"));
		assertEquals("sensor-1/value",
				SubscribeCommand.topicLevel("sensor-1/value"));
		assertEquals("_", SubscribeCommand.topicLevel(null));
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
		assertEquals(ExitCodes.USAGE,
				run("subscribe", "-p", "senza-chiave").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("subscribe", "-p", "a:b", "--count", "0").exitCode());
		assertEquals(ExitCodes.USAGE, run("subscribe", "-p", "a:b",
				"--deadband-absolute", "1", "--deadband-percent", "1")
				.exitCode());
		assertEquals(ExitCodes.USAGE,
				run("query", "g.V()", "-o", "topic").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("query", "g.V()", "-o", "xml").exitCode());
		assertEquals(ExitCodes.USAGE, run("query").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("write", "n", "x", "--type", "bool").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("call", "o", "m", "int32:abc").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("about", "--connect-timeout", "presto").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("about", "--security-policy", "Nessuna").exitCode());
		assertEquals(ExitCodes.USAGE,
				run("about", "--identity-cert", "/tmp/c.pem").exitCode());
		assertEquals(ExitCodes.USAGE, run("vertex", "add", "--id", "x")
				.exitCode());
		final Result unknown = run("about", "--sconosciuta");
		assertEquals(ExitCodes.USAGE, unknown.exitCode());
		assertTrue(unknown.err().startsWith("wotctl: uso errato:"),
				unknown.err());
		assertTrue(unknown.err().contains("Guida: wotctl about --help"),
				unknown.err());
		assertTrue(unknown.out().isEmpty());
		final Result typo = run("abuot");
		assertTrue(typo.err().contains("about"), typo.err());
		final Result root = run();
		assertTrue(root.err().contains("specificare un comando"), root.err());
		final Result group = run("vertex");
		assertTrue(group.err().contains("sottocomandi: add, delete"),
				group.err());
		org.mockito.Mockito.verifyNoInteractions(mock.client);
	}

	@Test
	void verbosityCount() {
		assertEquals(0, WaldotCli.verbosity(new String[]{"query", "x"}));
		assertEquals(1, WaldotCli.verbosity(new String[]{"-v", "query"}));
		assertEquals(3, WaldotCli
				.verbosity(new String[]{"query", "-vv", "--verbose"}));
		assertEquals(0, WaldotCli.verbosity(new String[]{"call", "--", "-v"}));
		assertEquals(0, WaldotCli.verbosity(new String[]{"-", "-x"}));
	}

	@Test
	void versionFromTheBuildResource() throws IOException {
		final Result version = run("--version");
		assertEquals(0, version.exitCode());
		assertTrue(version.out().startsWith("wotctl "), version.out());
		assertTrue(version.out().contains("\nJava "), version.out());
		assertTrue(version.out().contains("\nOS "), version.out());
		assertFalse(WaldotCli.isNativeImage());
		try (InputStream input = WaldotCli.class
				.getResourceAsStream(WaldotCli.Version.RESOURCE)) {
			if (input != null) {
				// built by Gradle (processResources)
				final Properties properties = new Properties();
				properties.load(input);
				assertEquals(properties.getProperty("version"),
						WaldotCli.Version.version());
				assertTrue(version.out().startsWith(
						"wotctl " + properties.getProperty("version") + "\n"));
			}
		}
		assertEquals(0, run("about", "--version").exitCode());
	}
}
