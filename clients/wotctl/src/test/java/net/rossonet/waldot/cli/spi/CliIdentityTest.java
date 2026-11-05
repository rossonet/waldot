package net.rossonet.waldot.cli.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.cli.CliTestSupport;
import net.rossonet.waldot.cli.CliTestSupport.Result;

/**
 * Name of the executable: wotctl without the resource of a distribution,
 * otherwise the name of the distribution in the texts, the completion, the
 * version and the error messages.
 */
class CliIdentityTest {

	@AfterEach
	void restore() {
		CliIdentity.set(null);
	}

	@Test
	void coreIsWotctl() {
		final CliIdentity identity = CliIdentity.get();
		assertEquals("wotctl", identity.name());
		assertEquals("wotctl", identity.mcpName());
		assertEquals("", identity.description());
		assertEquals("WOTCTL_MCP_ARGS", identity.envName("MCP_ARGS"));
		final Result help = CliTestSupport.run("query", "--help");
		assertTrue(help.out().contains("wotctl query \"g.V().count()\""), help.out());
	}

	@Test
	void distributionNameInTextsVersionCompletionAndErrors() {
		CliIdentity.set(CliIdentity.of("muuh", "muuh - console della Macchina Unica", null));
		final Result help = CliTestSupport.run("query", "--help");
		assertTrue(help.out().contains("muuh query \"g.V().count()\""), help.out());
		assertFalse(help.out().contains("wotctl"), help.out());
		final Result root = CliTestSupport.run("--help");
		assertTrue(root.out().startsWith("muuh - console della Macchina Unica"), root.out());
		assertTrue(root.out().contains("muuh help COMANDO"), root.out());
		final Result version = CliTestSupport.run("--version");
		assertTrue(version.out().startsWith("muuh "), version.out());
		assertTrue(version.out().contains("basato su wotctl "), version.out());
		final Result completion = CliTestSupport.run("completion");
		assertTrue(completion.out().contains("_picocli_muuh_query"), completion.out());
		assertTrue(completion.out().contains("complete -F _complete_muuh"), completion.out());
		final Result usage = CliTestSupport.run("comando-che-non-esiste");
		assertTrue(usage.err().startsWith("muuh: uso errato: "), usage.err());
	}

	@Test
	void environmentAndCacheOfTheExecutable() {
		final CliIdentity identity = CliIdentity.of("muuh", null, null);
		assertEquals("muuh", identity.mcpName());
		assertEquals("MUUH", identity.envPrefix());
		assertEquals("MUUH_MCP_WORK_DIR", identity.envName("MCP_WORK_DIR"));
		assertEquals(Path.of(System.getProperty("user.home"), ".cache", "muuh"),
				identity.cacheDir());
		assertEquals("MY_CLI_2", CliIdentity.of("my-cli.2", null, "x").envPrefix());
		assertEquals("x", CliIdentity.of("my-cli.2", null, "x").mcpName());
	}
}
