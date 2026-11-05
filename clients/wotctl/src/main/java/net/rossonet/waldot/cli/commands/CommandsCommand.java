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
						"  ${ROOT-COMMAND-NAME} commands list",
						"  ${ROOT-COMMAND-NAME} commands list --element sensor-1 -o json",
						"  ${ROOT-COMMAND-NAME} commands call about",
						"  ${ROOT-COMMAND-NAME} commands call 'export graph' -o json",
						"  ${ROOT-COMMAND-NAME} commands call property --element sensor-1 setpoint 21.5"})
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
					"", "Esempi:", "  ${ROOT-COMMAND-NAME} commands call about",
					"  ${ROOT-COMMAND-NAME} commands call query \"g.V().count()\"",
					"  ${ROOT-COMMAND-NAME} commands call delete --element sensor-old",
					"  ${ROOT-COMMAND-NAME} commands call property --element sensor-1 unit '°C' -o json"})
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
					"  json/jsonl: forma breve {name, path, inputs:[\"nome:Tipo\"], outputs:[nome]};",
					"              con --full (o se il filtro trova un solo comando) oggetti con",
					"              objectId, methodId, name, path, description, inputs, outputs.",
					"", "Esempi:", "  ${ROOT-COMMAND-NAME} commands list",
					"  ${ROOT-COMMAND-NAME} commands list -o value | sort",
					"  ${ROOT-COMMAND-NAME} commands list --element sensor-1",
					"  ${ROOT-COMMAND-NAME} commands list --path iec60870-5-104 -o json",
					"  ${ROOT-COMMAND-NAME} commands list --name 'create*rule*' -o json",
					"  ${ROOT-COMMAND-NAME} commands list -o json | jq '.[] | select(.name==\"about\")'"})
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

		/** Short name of an OPC UA data type (String, Boolean, Int32...). */
		static String typeName(final org.eclipse.milo.opcua.stack.core.types.builtin.NodeId dataType) {
			if (dataType == null) {
				return null;
			}
			try {
				final org.eclipse.milo.opcua.stack.core.OpcUaDataType type = org.eclipse.milo.opcua.stack.core.OpcUaDataType
						.fromNodeId(dataType);
				if (type != null) {
					return type.name();
				}
			} catch (final RuntimeException e) {
				// not a builtin type
			}
			return dataType.toParseableString();
		}

		/** Brief form for JSON: name, folder, arguments with name and type. */
		static Map<String, Object> brief(final CommandDescriptor command) {
			final Map<String, Object> m = new LinkedHashMap<>();
			m.put("name", command.name());
			m.put("path", command.path());
			m.put("inputs", command.inputs().stream().map(a -> a.name() + ":" + typeName(a.dataType())).toList());
			m.put("outputs", command.outputs().stream().map(ArgumentDescriptor::name).toList());
			return m;
		}

		/** Glob with * and ? (case insensitive). */
		static boolean glob(final String pattern, final String text) {
			if (pattern == null) {
				return true;
			}
			if (text == null) {
				return false;
			}
			final StringBuilder regex = new StringBuilder();
			for (final char c : pattern.toCharArray()) {
				switch (c) {
					case '*' -> regex.append(".*");
					case '?' -> regex.append('.');
					default -> regex.append(java.util.regex.Pattern.quote(String.valueOf(c)));
				}
			}
			return java.util.regex.Pattern.compile(regex.toString(),
					java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text).matches();
		}

		@Option(names = {"-e",
				"--element"}, paramLabel = "ELEMENTO", description = "Elenca i comandi di questo vertice o arco invece di quelli globali.")
		private String element;

		@Option(names = "--full", description = "In JSON l'oggetto completo (NodeId, tipi, descrizioni) invece della forma breve.")
		private boolean full;

		@Option(names = "--name", paramLabel = "GLOB", description = "Solo i comandi con questo nome (* e ?, senza distinzione di maiuscole).")
		private String name;

		@Option(names = "--path", paramLabel = "TESTO", description = "Solo i comandi la cui cartella contiene questo testo (per esempio iec60870-5-104).")
		private String path;

		@Override
		protected int execute(final WaldotClient client,
				final Printer printer) {
			final List<CommandDescriptor> all = element == null
					? client.commands().globalCommands()
					: client.commands()
							.elementCommands(nodeId(client, element));
			final List<CommandDescriptor> commands = all.stream()
					.filter(c -> glob(name, c.name()))
					.filter(c -> path == null || c.path() != null && c.path()
							.toLowerCase(java.util.Locale.ROOT)
							.contains(path.toLowerCase(java.util.Locale.ROOT)))
					.toList();
			// a filter that finds a single command shows it in full
			final boolean complete = full
					|| (name != null || path != null) && commands.size() == 1;
			printer.list(commands,
					command -> complete ? command : brief(command),
					ListCommand::text, CommandDescriptor::name);
			return 0;
		}
	}
}
