package net.rossonet.waldot.cli.support;

import java.util.Map;
import java.util.stream.Collectors;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Names of the subcommands shown to the user in the usage errors: the same
 * ones of the help and of the completion, without the hidden commands and the
 * aliases.
 */
public final class VisibleSubcommands {

	/**
	 * @return the visible subcommands of the command, in declaration order,
	 *         separated by ", "
	 */
	public static String of(final CommandSpec spec) {
		return spec.subcommands().entrySet().stream()
				.filter(VisibleSubcommands::visible)
				.map(Map.Entry::getKey)
				.collect(Collectors.joining(", "));
	}

	private static boolean visible(
			final Map.Entry<String, CommandLine> entry) {
		final CommandSpec sub = entry.getValue().getCommandSpec();
		return entry.getKey().equals(sub.name())
				&& !sub.usageMessage().hidden();
	}

	private VisibleSubcommands() {
	}
}
