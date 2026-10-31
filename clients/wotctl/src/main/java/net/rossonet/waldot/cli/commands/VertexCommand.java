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
 * {@code waldot vertex add|delete}.
 */
@Command(name = "vertex", header = "Aggiunge o elimina vertici.", mixinStandardHelpOptions = true, description = {
		"Aggiunge o elimina vertici del grafo."}, subcommands = {
				VertexCommand.AddCommand.class,
				VertexCommand.DeleteCommand.class}, footer = {"Esempi:",
						"  waldot vertex add --id sensor-1 --label sensor -P zone=A -P unit=C",
						"  waldot vertex delete sensor-1"})
public class VertexCommand extends CommandGroup {

	@Command(name = "add", header = "Aggiunge un vertice.", mixinStandardHelpOptions = true, description = {
			"Aggiunge un vertice con il comando 'add vertex' del server.",
			"Senza --id l'identificatore è assegnato dal server. Le proprietà iniziali si",
			"indicano con -P chiave=valore (ripetibile) e sono create come stringhe; chiavi e",
			"valori non possono contenere virgole (in quel caso usare poi 'property set')."}, footer = {
					"Output:",
					"  text : la descrizione del vertice restituita dal server;",
					"  value: l'id del vertice (se indicato con --id) o la risposta del server;",
					"  json/jsonl: {\"kind\":\"vertex\",\"id\",\"output\"}.",
					"", "Esempi:",
					"  waldot vertex add --id sensor-1 --label sensor --type temperature",
					"  waldot vertex add --label person -P name=marko -P age=29",
					"  waldot vertex add --id pump-3 --label pump -o json"})
	public static class AddCommand extends ClientCommand {

		@Option(names = "--id", paramLabel = "ID", description = "Identificatore del nuovo vertice (stringa nel namespace di WaldOT).")
		private String id;

		@Option(names = {"-l",
				"--label"}, required = true, paramLabel = "LABEL", description = "Label del vertice (obbligatoria).")
		private String label;

		@Option(names = {"-P",
				"--property"}, paramLabel = "CHIAVE=VALORE", description = "Proprietà iniziale (ripetibile).")
		private final Map<String, String> properties = new LinkedHashMap<>();

		@Option(names = {"-t",
				"--type"}, paramLabel = "TIPO", description = "Tipo del vertice (proprietà 'type', usata dai plugin per scegliere il comportamento).")
		private String type;

		@Override
		protected int execute(final WaldotClient client, final Printer printer)
				throws Exception {
			final String output = GraphEdits.checkAnswer(
					client.createVertex(id == null ? "" : id, label,
							type == null ? "" : type,
							GraphEdits.keyValues(spec, properties)),
					"aggiunta del vertice");
			final Created created = new Created("vertex", id, output);
			printer.single(created, c -> c, Created::output,
					c -> c.id() != null ? c.id() : c.output());
			return 0;
		}
	}

	@Command(name = "delete", header = "Elimina uno o più vertici.", mixinStandardHelpOptions = true, description = {
			"Elimina uno o più vertici (comando 'delete' dell'elemento). Il comando si ferma",
			"al primo errore, che produce il codice di uscita 1."}, footer = {
					"Output:",
					"  text : 'eliminato <id>' per ogni vertice; value: gli id;",
					"  json/jsonl: {\"id\",\"deleted\",\"output\"}.", "",
					"Esempi:", "  waldot vertex delete sensor-1",
					"  waldot query -o value \"g.V().hasLabel('tmp')\" | xargs -r waldot vertex delete"})
	public static class DeleteCommand extends ClientCommand {

		@Parameters(arity = "1..*", paramLabel = "ID", description = "Id dei vertici da eliminare.")
		private final List<String> ids = new ArrayList<>();

		@Override
		protected int execute(final WaldotClient client,
				final Printer printer) {
			return GraphEdits.delete(client, printer, ids,
					id -> nodeId(client, id));
		}
	}
}
