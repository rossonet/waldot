package net.rossonet.waldot.cli.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.cli.CliTestSupport;
import net.rossonet.waldot.cli.CliTestSupport.Result;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * The usage errors list the subcommands of the help and of the completion:
 * hidden commands and aliases are left out.
 */
class VisibleSubcommandsTest {

	@Command(name = "group", subcommands = {Shown.class, Hidden.class})
	static class Group extends CommandGroup {
	}

	@Command(name = "hidden", hidden = true)
	static class Hidden implements Runnable {
		@Override
		public void run() {
		}
	}

	@Command(name = "shown", aliases = "alias")
	static class Shown implements Runnable {
		@Override
		public void run() {
		}
	}

	@Test
	void hiddenAndAliasesLeftOut() {
		final CommandLine group = new CommandLine(new Group());
		assertEquals("shown", VisibleSubcommands.of(group.getCommandSpec()));
		final java.io.StringWriter err = new java.io.StringWriter();
		group.setErr(new java.io.PrintWriter(err));
		group.execute();
		assertTrue(err.toString().contains("(sottocomandi: shown)"), err.toString());
	}

	@Test
	void rootUsageErrorListsTheCommandsOfTheHelp() {
		final Result root = CliTestSupport.run();
		assertTrue(root.err().contains("specificare un comando (comandi: help, about"),
				root.err());
		assertFalse(root.err().contains(", ,"), root.err());
	}
}
