package net.rossonet.waldot.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.cli.CliTestSupport;
import net.rossonet.waldot.cli.CliTestSupport.Result;
import net.rossonet.waldot.cli.support.Completions;

/**
 * {@code wotctl completion} and the candidates of the options.
 */
class CompletionCommandTest {

	private static List<String> candidates(final Iterable<String> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false).toList();
	}

	@Test
	void candidates() {
		assertEquals(List.of("None", "Sign", "SignAndEncrypt"),
				candidates(new Completions.SecurityModes()));
		assertTrue(candidates(new Completions.SecurityPolicies())
				.containsAll(List.of("None", "Basic256Sha256",
						"Aes256_Sha256_RsaPss")));
		assertTrue(candidates(new Completions.ValueTypes())
				.containsAll(List.of("string", "bool", "int32", "double",
						"nodeid", "text")));
		assertEquals(List.of("force", "layered", "mrtree", "radial", "stress"),
				candidates(new Completions.RenderAlgorithms()));
		assertEquals(List.of("directory", "label", "none", "type"),
				candidates(new Completions.RenderColors()));
		assertEquals(List.of("both", "in", "out"),
				candidates(new Completions.RenderDirections()));
		assertEquals(List.of("dot", "svg"),
				candidates(new Completions.RenderFormats()));
		assertEquals(List.of("directory", "none", "type"),
				candidates(new Completions.RenderGroups()));
		assertEquals(List.of("down", "left", "right", "up"),
				candidates(new Completions.RenderLayoutDirections()));
		assertEquals(List.of("auto", "dark", "light"),
				candidates(new Completions.RenderThemes()));
	}

	@Test
	void helpIsItalian() {
		final Result help = CliTestSupport.run("completion", "--help");
		assertEquals(0, help.exitCode());
		assertTrue(help.out().contains("Uso: wotctl completion [OPZIONI]"),
				help.out());
		assertTrue(help.out().contains("source <(wotctl completion)"));
		assertTrue(CliTestSupport.run("--help").out().contains("completion"));
	}

	@Test
	void scriptCompletesCommandsOptionsAndValues() {
		final Result result = CliTestSupport.run("completion");
		assertEquals(0, result.exitCode(), result.err());
		final String script = result.out();
		assertTrue(script.startsWith("#!/usr/bin/env bash"), script);
		assertTrue(script.contains("complete -F _complete_wotctl"), script);
		assertTrue(script.contains("-o default wotctl"), script);
		// commands and nested subcommands
		for (final String command : List.of("about", "query", "snapshot",
				"subscribe", "render", "completion", "help")) {
			assertTrue(script.contains(command), command);
		}
		assertTrue(script.contains("_picocli_wotctl_snapshot_import"), script);
		assertTrue(script.contains("_picocli_wotctl_commands_call"), script);
		// values of the options
		assertTrue(script.contains("SignAndEncrypt"), script);
		assertTrue(script.contains("Aes256_Sha256_RsaPss"), script);
		assertTrue(script.contains("StatusValueTimestamp"), script);
		assertTrue(script.contains("layered"), script);
		assertTrue(script.contains("jsonl"), script);
		assertTrue(script.contains("--endpoint"), script);
		assertTrue(!script.contains("waldot"), "no reference to the old name");
	}
}
