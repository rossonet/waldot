package net.rossonet.waldot.cli.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.cli.CliTestSupport;
import net.rossonet.waldot.cli.CliTestSupport.Result;
import net.rossonet.waldot.cli.CommandTestBase;
import net.rossonet.waldot.cli.WaldotCli;
import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.CommandGroup;
import net.rossonet.waldot.cli.support.ExitCodes;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Commands added by the plugins: at the root and in the groups, with the help,
 * the exit codes, the version and the completion of the core commands.
 */
class PluginLoaderTest extends CommandTestBase {

	/** Plugin with a fixed name, version and registration. */
	record TestPlugin(String name, String version,
			Consumer<WotctlCommands> commands) implements WotctlPlugin {

		@Override
		public void register(final WotctlCommands registration) {
			commands.accept(registration);
		}
	}

	/** Candidates of {@code alpha hello --mode}. */
	public static final class ModesTest implements Iterable<String> {
		@Override
		public java.util.Iterator<String> iterator() {
			return List.of("lento", "veloce").iterator();
		}
	}

	@Command(name = "alpha", header = "Comandi del plugin alpha.", mixinStandardHelpOptions = true, subcommands = {
			HelloTest.class})
	public static class AlphaTest extends CommandGroup {
	}

	@Command(name = "hello", header = "Saluta il server.", mixinStandardHelpOptions = true)
	public static class HelloTest extends ClientCommand {

		@Option(names = "--mode", completionCandidates = ModesTest.class, description = "Modo: ${COMPLETION-CANDIDATES}.")
		private String mode = "lento";

		@Override
		protected int execute(final WaldotClient client,
				final Printer printer) {
			for (final String line : client.commands().about()) {
				printer.line(mode + " " + line);
			}
			return 0;
		}
	}

	@Command(name = "about", header = "Nome già usato dal core.")
	public static class AboutTest extends CommandGroup {
	}

	@Command(name = "verify", header = "Verifica un backup.", mixinStandardHelpOptions = true)
	public static class VerifyTest extends CommandGroup {
	}

	private static TestPlugin alpha() {
		return new TestPlugin("alpha", "1.2.3",
				commands -> commands.add(AlphaTest.class).add("snapshot",
						VerifyTest.class));
	}

	private static Result run(final List<WotctlPlugin> plugins,
			final String... args) {
		return CliTestSupport.run(plugins, args);
	}

	@Test
	void commandAtTheRootRunsWithTheConnectionOptions() {
		when(mock.commands.about()).thenReturn(List.of("WaldOT 1"));
		final Result result = run(List.of(alpha()), "alpha", "hello", "--mode",
				"veloce", "--endpoint", "opc.tcp://server:1/waldot");
		assertEquals(0, result.exitCode(), result.err());
		assertEquals(List.of("veloce WaldOT 1"), result.lines());
		assertEquals("opc.tcp://server:1/waldot",
				mock.configuration.getBaseWaldOTOpcEndpoint());
	}

	@Test
	void commandInAnExistingGroup() {
		final Result result = run(List.of(alpha()), "snapshot", "--help");
		assertEquals(0, result.exitCode(), result.err());
		assertTrue(result.out().contains("verify"), result.out());
		assertTrue(result.out().contains("Verifica un backup."), result.out());
	}

	@Test
	void groupWithoutSubcommandIsUsageError() {
		final Result result = run(List.of(alpha()), "alpha");
		assertEquals(ExitCodes.USAGE, result.exitCode());
		assertTrue(result.err().contains("sottocomandi: hello"),
				result.err());
	}

	@Test
	void helpOfThePluginIsLikeTheCore() {
		final Result result = run(List.of(alpha()), "alpha", "hello",
				"--help");
		assertEquals(0, result.exitCode(), result.err());
		assertTrue(result.out().contains("Uso: wotctl alpha hello [OPZIONI]"),
				result.out());
		assertTrue(result.out().contains("Codici di uscita:"), result.out());
		assertTrue(result.out().contains("--endpoint"), result.out());
		assertTrue(result.out().contains("Modo: lento, veloce."),
				result.out());
		final Result root = run(List.of(alpha()), "--help");
		assertTrue(root.out().contains("alpha"), root.out());
	}

	@Test
	void missingGroupIsAnError() {
		final TestPlugin plugin = new TestPlugin("beta", null,
				commands -> commands.add("nessuno", VerifyTest.class));
		final IllegalStateException error = assertThrows(
				IllegalStateException.class,
				() -> WaldotCli.newCommandLine(new PrintWriter(new StringWriter()),
						new PrintWriter(new StringWriter()), List.of(plugin)));
		assertEquals("plugin 'beta': il gruppo 'nessuno' non esiste",
				error.getMessage());
	}

	@Test
	void nameOfTheCoreIsAnError() {
		final TestPlugin plugin = new TestPlugin("beta", null,
				commands -> commands.add(AboutTest.class));
		final IllegalStateException error = assertThrows(
				IllegalStateException.class,
				() -> WaldotCli.newCommandLine(new PrintWriter(new StringWriter()),
						new PrintWriter(new StringWriter()), List.of(plugin)));
		assertEquals("plugin 'beta': il comando 'wotctl about' esiste già",
				error.getMessage());
	}

	@Test
	void nameOfAnotherPluginIsAnError() {
		final TestPlugin other = new TestPlugin("gamma", null,
				commands -> commands.add(new AlphaTest()));
		assertThrows(IllegalStateException.class,
				() -> WaldotCli.newCommandLine(new PrintWriter(new StringWriter()),
						new PrintWriter(new StringWriter()),
						List.of(alpha(), other)));
	}

	@Test
	void noPluginsOnTheClasspathOfWotctl() {
		assertEquals(List.of(), PluginLoader.plugins());
	}

	@Test
	void scriptCompletesTheCommandsOfThePlugins() {
		final Result result = run(List.of(alpha()), "completion");
		assertEquals(0, result.exitCode(), result.err());
		assertTrue(result.out().contains("_picocli_wotctl_alpha_hello"),
				"no function for alpha hello");
		assertTrue(result.out().contains("_picocli_wotctl_snapshot_verify"),
				"no function for snapshot verify");
		assertTrue(result.out().contains("lento"), "no candidates of --mode");
		assertTrue(result.out().contains("veloce"), "no candidates of --mode");
	}

	@Test
	void versionListsThePlugins() {
		final TestPlugin unknown = new TestPlugin("beta", null,
				commands -> {
				});
		final Result result = run(List.of(alpha(), unknown), "--version");
		assertEquals(0, result.exitCode(), result.err());
		assertTrue(result.lines().contains("plugin alpha 1.2.3"),
				result.out());
		assertTrue(result.lines().contains("plugin beta (sviluppo)"),
				result.out());
	}

	@Test
	void versionOfAResource() {
		assertEquals(null, PluginVersion.fromResource(PluginLoaderTest.class,
				"/assente.properties"));
	}
}
