package net.rossonet.waldot.cli.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.rossonet.waldot.cli.commands.GraphEdits.Created;
import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.CommandGroup;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code wotctl edge add|delete}.
 */
@Command(name = "edge", header = "Aggiunge o elimina archi.", mixinStandardHelpOptions = true, description = {
		"Aggiunge o elimina archi del grafo."}, subcommands = {
				EdgeCommand.AddCommand.class,
				EdgeCommand.DeleteCommand.class}, footer = {"Esempi:",
						"  wotctl edge add --label feeds --from pump-3 --to tank-1 -P weight=0.5",
						"  wotctl edge delete e-17"})
public class EdgeCommand extends CommandGroup {

	@Command(name = "add", header = "Aggiunge un arco tra due vertici.", mixinStandardHelpOptions = true, description = {
			"Aggiunge un arco tra due vertici esistenti con il comando 'add edge' del server.",
			"Le proprietà iniziali si indicano con -P chiave=valore (ripetibile, niente",
			"virgole in chiavi e valori)."}, footer = {"Output:",
					"  text, value: la risposta del server (descrizione dell'arco creato);",
					"  json/jsonl: {\"kind\":\"edge\",\"id\":null,\"output\"}.",
					"", "Esempi:",
					"  wotctl edge add --label knows --from marko --to vadas -P weight=0.5",
					"  wotctl edge add -l feeds -s pump-3 -d tank-1 -o json"})
	public static class AddCommand extends ClientCommand {

		@Option(names = {"-s",
				"--from"}, required = true, paramLabel = "VERTICE", description = "Vertice di origine.")
		private String from;

		@Option(names = {"-l",
				"--label"}, required = true, paramLabel = "LABEL", description = "Label (tipo di relazione) dell'arco, obbligatoria.")
		private String label;

		@Option(names = {"-P",
				"--property"}, paramLabel = "CHIAVE=VALORE", description = "Proprietà iniziale (ripetibile).")
		private final Map<String, String> properties = new LinkedHashMap<>();

		private String[] keyValues;

		@Option(names = {"-d",
				"--to"}, required = true, paramLabel = "VERTICE", description = "Vertice di destinazione.")
		private String to;

		@Override
		protected int execute(final WaldotClient client, final Printer printer)
				throws Exception {
			final String output = GraphEdits.checkAnswer(
					client.createEdge(label,
							nodeId(client, from).toParseableString(),
							nodeId(client, to).toParseableString(),
							keyValues),
					"aggiunta dell'arco");
			final Created created = new Created("edge", null, output);
			printer.single(created, c -> c, Created::output, Created::output);
			return 0;
		}

		@Override
		protected void validate() {
			keyValues = GraphEdits.keyValues(spec, properties);
		}
	}

	@Command(name = "delete", header = "Elimina uno o più archi.", mixinStandardHelpOptions = true, description = {
			"Elimina uno o più archi (comando 'delete' dell'elemento). Gli id degli archi si",
			"ottengono con 'describe' sui vertici o con una query su g.E()."}, footer = {
					"Output:",
					"  text : 'eliminato <id>' per ogni arco; value: gli id;",
					"  json/jsonl: {\"id\",\"deleted\",\"output\"}.", "",
					"Esempi:",
					"  wotctl query -o value \"g.V('marko').outE('knows')\" | xargs -r wotctl edge delete"})
	public static class DeleteCommand extends ClientCommand {

		@Parameters(arity = "1..*", paramLabel = "ID", description = "Id degli archi da eliminare.")
		private final List<String> ids = new ArrayList<>();

		@Override
		protected int execute(final WaldotClient client,
				final Printer printer) {
			return GraphEdits.delete(client, printer, ids,
					id -> nodeId(client, id));
		}
	}
}
