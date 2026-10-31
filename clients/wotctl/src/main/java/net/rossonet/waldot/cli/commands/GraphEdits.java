package net.rossonet.waldot.cli.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.cli.support.CliException;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.command.CommandResult;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

/**
 * Helpers shared by the commands that change the graph.
 */
final class GraphEdits {

	/** Result of the deletion of an element. */
	record Deleted(String id, boolean deleted, String output) {
	}

	/** Result of the creation of an element. */
	record Created(String kind, String id, String output) {
	}

	/**
	 * Checks the {@code [output, error]} answer of the add commands.
	 *
	 * @return the output
	 */
	static String checkAnswer(final String[] answer, final String what) {
		if (answer == null || answer.length == 0) {
			throw CliException.server(what + ": risposta vuota dal server");
		}
		if (answer.length > 1 && answer[1] != null && !answer[1].isBlank()) {
			throw CliException.server(what + ": " + answer[1]);
		}
		return answer[0] == null ? "" : answer[0];
	}

	/** Deletes the elements and prints a line for each. */
	static int delete(final WaldotClient client, final Printer printer,
			final List<String> ids, final Function<String, NodeId> parser) {
		final List<Deleted> results = new ArrayList<>();
		for (final String id : ids) {
			final CommandResult result = client.commands()
					.deleteElement(parser.apply(id));
			if (!result.isSuccess()) {
				throw CliException
						.server("eliminazione di '" + id + "' non riuscita: "
								+ result.error().orElse("errore sconosciuto"));
			}
			results.add(
					new Deleted(id, true, Json.scalar(result.get("output"))));
		}
		printer.list(results, deleted -> deleted,
				deleted -> "eliminato " + deleted.id(), Deleted::id);
		return 0;
	}

	/**
	 * Joins the properties {@code chiave=valore} in the comma separated list of
	 * the server.
	 */
	static String[] keyValues(final CommandSpec spec,
			final Map<String, String> properties) {
		final List<String> keyValues = new ArrayList<>();
		for (final Map.Entry<String, String> property : properties.entrySet()) {
			for (final String part : new String[]{property.getKey(),
					property.getValue()}) {
				if (part.indexOf(',') >= 0) {
					throw new ParameterException(spec.commandLine(),
							"le chiavi e i valori delle proprietà non possono "
									+ "contenere virgole ('" + part
									+ "'): usare poi 'wotctl property set'");
				}
			}
			keyValues.add(property.getKey().trim());
			keyValues.add(property.getValue().trim());
		}
		return keyValues.toArray(new String[0]);
	}

	private GraphEdits() {
	}
}
