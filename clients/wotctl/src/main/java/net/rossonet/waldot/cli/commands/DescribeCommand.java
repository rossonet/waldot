package net.rossonet.waldot.cli.commands;

import java.util.List;

import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.graph.ElementDescription;
import net.rossonet.waldot.client.api.graph.ElementDescription.CommandSummary;
import net.rossonet.waldot.client.api.graph.ElementDescription.EdgeSummary;
import net.rossonet.waldot.client.api.graph.ElementDescription.PropertyDescription;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * {@code waldot describe}: description of a vertex or an edge.
 */
@Command(name = "describe", header = "Descrive un vertice o un arco (proprietà, archi, comandi).", mixinStandardHelpOptions = true, description = {
		"Descrive un vertice o un arco: tipo, label, nomi OPC UA, proprietà (con il NodeId",
		"della variabile OPC UA di ciascuna, utile per 'read', 'write' e 'subscribe --node'),",
		"archi uscenti ed entranti e comandi disponibili sull'elemento." }, footer = {
				"Output:",
				"  text : scheda leggibile su più righe;",
				"  json : oggetto con id, kind (vertex/edge), label, type, browseName, displayName,",
				"         description, eventNotifier, source, target, properties, outEdges,",
				"         inEdges, commands; jsonl: lo stesso oggetto su una riga;",
				"  value: una riga 'chiave=valore' per ogni proprietà.",
				"",
				"Esempi:",
				"  waldot describe sensor-1",
				"  waldot describe 'ns=2;s=sensor-1' -o json   (il ';' va protetto dalla shell)",
				"  waldot describe sensor-1 -o json | jq -r '.properties[].key'",
				"  waldot describe sensor-1 -o value | grep '^temperature='" })
public class DescribeCommand extends ClientCommand {

	private static String text(final ElementDescription description) {
		final StringBuilder text = new StringBuilder();
		row(text, "id", Json.id(description.id()));
		row(text, "tipo", description.kind());
		row(text, "label", description.label());
		row(text, "type", description.type());
		row(text, "browseName", description.browseName());
		row(text, "displayName", description.displayName());
		row(text, "descrizione", description.description());
		if (description.isEdge()) {
			row(text, "origine", Json.id(description.source()));
			row(text, "destinazione", Json.id(description.target()));
		}
		text.append("proprietà:");
		if (description.properties().isEmpty()) {
			text.append(" nessuna");
		}
		for (final PropertyDescription property : description.properties()) {
			text.append("\n  ").append(property.key()).append(" = ").append(Json.scalarLine(property.value()))
					.append("   (").append(property.valueType() == null ? "?" : property.valueType()).append(", ")
					.append(Json.id(property.nodeId())).append(')');
		}
		edges(text, "archi uscenti", description.outEdges());
		edges(text, "archi entranti", description.inEdges());
		text.append("\ncomandi:");
		if (description.commands().isEmpty()) {
			text.append(" nessuno");
		}
		for (final CommandSummary command : description.commands()) {
			text.append("\n  ").append(command.name()).append('(').append(String.join(", ", command.inputs()))
					.append(')');
			if (!command.outputs().isEmpty()) {
				text.append(" -> ").append(String.join(", ", command.outputs()));
			}
			if (command.description() != null && !command.description().isBlank()) {
				text.append("   ").append(command.description());
			}
		}
		return text.toString();
	}

	private static void edges(final StringBuilder text, final String title, final List<EdgeSummary> edges) {
		text.append('\n').append(title).append(':');
		if (edges.isEmpty()) {
			text.append(" nessuno");
		}
		for (final EdgeSummary edge : edges) {
			text.append("\n  ").append(Json.id(edge.id())).append(' ').append(edge.label()).append(' ')
					.append(Json.id(edge.source())).append(" -> ").append(Json.id(edge.target()));
		}
	}

	private static void row(final StringBuilder text, final String name, final String value) {
		if (value != null && !value.isBlank()) {
			text.append(String.format("%-13s %s%n", name + ":", value));
		}
	}

	@Parameters(index = "0", paramLabel = "ELEMENTO", description = "Id del vertice o dell'arco (sensor-1 oppure ns=2;s=sensor-1).")
	private String element;

	@Override
	protected int execute(final WaldotClient client, final Printer printer) {
		final ElementDescription description = client.graph().describe(nodeId(client, element));
		switch (printer.format()) {
		case value -> description.properties()
				.forEach(property -> printer.line(property.key() + "=" + Json.scalarLine(property.value())));
		default -> printer.single(description, d -> d, DescribeCommand::text, d -> d);
		}
		return 0;
	}
}
