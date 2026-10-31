package net.rossonet.waldot.cli.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.rossonet.waldot.cli.support.CliException;
import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.CommandGroup;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.command.ArgumentDescriptor;
import net.rossonet.waldot.client.api.command.CommandDescriptor;
import net.rossonet.waldot.client.api.command.CommandResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code wotctl commands list|call}: the WaldOT commands (OPC UA methods).
 */
@Command(name = "commands", header = "Elenca e invoca i comandi del server e degli elementi.", mixinStandardHelpOptions = true, description = {
		"Elenca e invoca i comandi di WaldOT, cioè i metodi OPC UA esposti dal server:",
		"  * comandi globali: si trovano nella cartella dei comandi del server (predefinita",
		"    'cmd', vedi --commands-root) e comprendono i comandi di base e quelli dei plugin;",
		"  * comandi di elemento: definiti su un vertice o su un arco (es. delete, property);",
		"    si indicano con --element."}, subcommands = {
				CommandsCommand.ListCommand.class,
				CommandsCommand.CallCommand.class}, footer = {"Esempi:",
						"  wotctl commands list",
						"  wotctl commands list --element sensor-1 -o json",
						"  wotctl commands call about",
						"  wotctl commands call 'export graph' -o json",
						"  wotctl commands call property --element sensor-1 setpoint 21.5"})
public class CommandsCommand extends CommandGroup {

	@Command(name = "call", header = "Invoca un comando globale o di un elemento.", mixinStandardHelpOptions = true, description = {
			"Invoca un comando per nome (browse name, per esempio 'about' o 'export graph';",
			"i nomi con spazi vanno tra virgolette). Gli argomenti sono passati nell'ordine",
			"degli ingressi dichiarati dal comando (vedi 'commands list') e convertiti",
			"automaticamente nel tipo dichiarato (stringhe, booleani, numeri, NodeId).",
			"",
			"Se il comando risponde con un errore (stato OPC UA non buono o uscita 'error'",
			"valorizzata) il messaggio va su stderr e il codice di uscita è 1."}, footer = {
					"Output:",
					"  text : se il comando ha una sola uscita (oltre a 'error') il suo valore,",
					"         altrimenti una riga 'nome: valore' per ogni uscita;",
					"  value: il valore di ogni uscita (esclusa 'error'), uno per riga;",
					"  json/jsonl: {\"command\",\"status\",\"outputs\":{nome: valore}}.",
					"", "Esempi:", "  wotctl commands call about",
					"  wotctl commands call query \"g.V().count()\"",
					"  wotctl commands call delete --element sensor-old",
					"  wotctl commands call property --element sensor-1 unit '°C' -o json"})
	public static class CallCommand extends ClientCommand {

		@Parameters(index = "1..*", arity = "0..*", paramLabel = "ARGOMENTO", description = "Argomenti del comando, nell'ordine degli ingressi.")
		private final List<String> arguments = new ArrayList<>();

		@Option(names = {"-e",
				"--element"}, paramLabel = "ELEMENTO", description = "Invoca il comando definito su questo vertice o arco invece di un comando globale.")
		private String element;

		@Parameters(index = "0", paramLabel = "NOME", description = "Nome del comando.")
		private String name;

		@Override
		protected int execute(final WaldotClient client,
				final Printer printer) {
			final Object[] values = arguments.toArray();
			final CommandResult result = element == null
					? client.commands().invokeGlobal(name, values)
					: client.commands().invokeOnElement(nodeId(client, element),
							name, values);
			if (!result.isSuccess()) {
				throw CliException.server(
						"il comando '" + name + "' ha restituito un errore: "
								+ result.error().orElse("errore sconosciuto"));
			}
			final Map<String, Object> outputs = new LinkedHashMap<>(
					result.asMap());
			outputs.remove(CommandResult.ERROR_OUTPUT);
			switch (printer.format()) {
				case json, jsonl -> {
					final Map<String, Object> json = new LinkedHashMap<>();
					json.put("command", name);
					json.put("status", result.status());
					json.put("outputs", outputs);
					printer.single(json, j -> j, j -> "", j -> "");
				}
				case value -> outputs.values()
						.forEach(value -> printer.line(Json.scalar(value)));
				default -> {
					if (outputs.size() == 1) {
						printer.line(Json
								.scalar(outputs.values().iterator().next()));
					} else {
						outputs.forEach((key, value) -> printer
								.line(key + ": " + Json.scalar(value)));
					}
				}
			}
			return 0;
		}
	}

	@Command(name = "list", header = "Elenca i comandi globali o di un elemento.", mixinStandardHelpOptions = true, description = {
			"Elenca i comandi globali del server oppure, con --element, quelli di un vertice o",
			"di un arco, con gli argomenti di ingresso e di uscita."}, footer = {
					"Output:",
					"  text : 'nome(ingressi) -> uscite   descrizione', un comando per riga;",
					"  value: solo i nomi, uno per riga;",
					"  json/jsonl: oggetti con objectId, methodId, name, path, description, inputs,",
					"              outputs (ogni argomento con name, dataType, valueRank, description).",
					"", "Esempi:", "  wotctl commands list",
					"  wotctl commands list -o value | sort",
					"  wotctl commands list --element sensor-1",
					"  wotctl commands list -o json | jq '.[] | select(.name==\"about\")'"})
	public static class ListCommand extends ClientCommand {

		private static String arguments(
				final List<ArgumentDescriptor> arguments) {
			return arguments.stream().map(ArgumentDescriptor::name)
					.collect(Collectors.joining(", "));
		}

		private static String text(final CommandDescriptor command) {
			final StringBuilder text = new StringBuilder(command.name())
					.append('(').append(arguments(command.inputs()))
					.append(')');
			if (!command.outputs().isEmpty()) {
				text.append(" -> ").append(arguments(command.outputs()));
			}
			if (command.description() != null
					&& !command.description().isBlank()) {
				text.append("   ").append(command.description());
			}
			return text.toString();
		}

		@Option(names = {"-e",
				"--element"}, paramLabel = "ELEMENTO", description = "Elenca i comandi di questo vertice o arco invece di quelli globali.")
		private String element;

		@Override
		protected int execute(final WaldotClient client,
				final Printer printer) {
			final List<CommandDescriptor> commands = element == null
					? client.commands().globalCommands()
					: client.commands()
							.elementCommands(nodeId(client, element));
			printer.list(commands, command -> command, ListCommand::text,
					CommandDescriptor::name);
			return 0;
		}
	}
}
