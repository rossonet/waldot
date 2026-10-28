package net.rossonet.waldot.cli.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.cli.support.CliException;
import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code waldot resolve}: follows a path of browse names.
 */
@Command(name = "resolve", header = "Risolve un percorso di browse name in un NodeId.", mixinStandardHelpOptions = true, description = {
		"Risolve un percorso di browse name a partire da un nodo (predefinito: la cartella",
		"Objects, i=85) seguendo i riferimenti gerarchici in avanti, e scrive il NodeId",
		"del nodo finale. Ogni argomento è un passo del percorso; un browse name con",
		"namespace si scrive come 'indice:nome' (es. 2:WaldOT).",
		"Se il percorso non esiste il codice di uscita è 1." }, footer = { "Output:",
				"  text, value: il NodeId; json/jsonl: {\"start\",\"path\",\"nodeId\"}.", "", "Esempi:",
				"  waldot resolve Server ServerStatus CurrentTime",
				"  waldot read \"$(waldot resolve Server ServerStatus State)\" -o value",
				"  waldot resolve --start sensor-1 temperature" })
public class ResolveCommand extends ClientCommand {

	/** Result of the resolution. */
	public record Resolved(NodeId start, List<String> path, NodeId nodeId) {
	}

	@Parameters(arity = "1..*", paramLabel = "NOME", description = "Passi del percorso (browse name).")
	private List<String> names = new ArrayList<>();

	@Option(names = { "-s", "--start" }, paramLabel = "NODO", description = "Nodo di partenza (predefinito i=85).")
	private String start;

	@Override
	protected int execute(final WaldotClient client, final Printer printer) {
		final NodeId startNode = start == null ? NodeIds.ObjectsFolder : nodeId(client, start);
		final Optional<NodeId> resolved = client.addressSpace().resolvePath(startNode, names.toArray(new String[0]));
		if (resolved.isEmpty()) {
			throw CliException.server("percorso non trovato: " + String.join("/", names) + " da "
					+ Json.id(startNode));
		}
		printer.single(new Resolved(startNode, names, resolved.get()), r -> r, r -> Json.id(r.nodeId()),
				Resolved::nodeId);
		return 0;
	}
}
