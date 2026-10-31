package net.rossonet.waldot.cli.support;

import java.util.concurrent.Callable;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/**
 * Command that only groups subcommands (e.g. {@code snapshot}): called alone
 * it is a usage error.
 */
public abstract class CommandGroup implements Callable<Integer> {

	@Spec
	protected CommandSpec spec;

	@Override
	public Integer call() {
		throw new ParameterException(spec.commandLine(),
				"specificare un sottocomando di '" + spec.qualifiedName()
						+ "' (sottocomandi: "
						+ String.join(", ", spec.subcommands().keySet()) + ")");
	}
}
