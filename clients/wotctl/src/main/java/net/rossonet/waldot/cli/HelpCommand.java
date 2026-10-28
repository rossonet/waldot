package net.rossonet.waldot.cli;

import java.io.PrintWriter;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.IHelpCommandInitializable2;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

/**
 * {@code waldot help [COMANDO [SOTTOCOMANDO]]}: guida di un comando.
 */
@Command(name = "help", header = "Mostra la guida di un comando.", helpCommand = true, mixinStandardHelpOptions = true, description = {
		"Mostra la guida completa di un comando o di un sottocomando.",
		"Senza argomenti mostra la guida generale di waldot.",
		"Equivale all'opzione -h/--help del comando." }, footer = { "%nEsempi:",
				"  waldot help", "  waldot help subscribe", "  waldot help snapshot import",
				"  waldot subscribe --help" })
public class HelpCommand implements IHelpCommandInitializable2, Runnable {

	@Parameters(paramLabel = "COMANDO", arity = "0..*", description = "Comando (ed eventuale sottocomando) di cui mostrare la guida.")
	private String[] commands = {};

	private Help.ColorScheme colorScheme;
	private PrintWriter out;
	private CommandLine self;

	@Override
	public void init(final CommandLine helpCommandLine, final Help.ColorScheme colorScheme, final PrintWriter out,
			final PrintWriter err) {
		this.self = helpCommandLine;
		this.colorScheme = colorScheme;
		this.out = out;
	}

	@Override
	public void run() {
		CommandLine target = self.getParent();
		for (final String name : commands) {
			final CommandLine next = target.getSubcommands().get(name);
			if (next == null) {
				throw new ParameterException(self, "comando sconosciuto: '" + name + "' (comandi di '"
						+ target.getCommandName() + "': " + String.join(", ", target.getSubcommands().keySet()) + ")");
			}
			target = next;
		}
		target.usage(out, colorScheme);
	}
}
