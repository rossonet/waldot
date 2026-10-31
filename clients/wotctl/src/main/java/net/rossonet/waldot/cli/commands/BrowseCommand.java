package net.rossonet.waldot.cli.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;

import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.address.BrowseFilter;
import net.rossonet.waldot.client.api.address.ReferenceInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code waldot browse}: references of a node of the address space.
 */
@Command(name = "browse", header = "Esplora lo spazio degli indirizzi OPC UA.", mixinStandardHelpOptions = true, description = {
		"Esplora lo spazio degli indirizzi OPC UA a partire da un nodo (predefinito: la",
		"cartella Objects, i=85) e scrive i nodi collegati. Normalmente segue solo i",
		"riferimenti gerarchici in avanti (figli); --references all li segue tutti.",
		"Con --depth N scende ricorsivamente fino a N livelli (solo in avanti, ogni nodo",
		"è visitato una sola volta)."}, footer = {"Output:",
				"  text : una riga per nodo, rientrata secondo la profondità:",
				"         '<classe> <browseName> <nodeId>  (<displayName>)';",
				"  value: i NodeId, uno per riga;",
				"  json/jsonl: {\"depth\",\"parent\",\"referenceTypeId\",\"forward\",\"targetId\",",
				"              \"browseName\",\"displayName\",\"nodeClass\",\"typeDefinitionId\"}.",
				"", "Esempi:", "  waldot browse", "  waldot browse --depth 3",
				"  waldot browse sensor-1 --node-class Variable -o value",
				"  waldot browse i=2253 --references all --direction both -o json"})
public class BrowseCommand extends ClientCommand {

	/** A reference found at a depth. */
	public record Entry(int depth, NodeId parent, NodeId referenceTypeId,
			boolean forward, NodeId targetId, String browseName,
			String displayName, NodeClass nodeClass, NodeId typeDefinitionId) {

		static Entry of(final int depth, final NodeId parent,
				final ReferenceInfo reference) {
			return new Entry(depth, parent, reference.referenceTypeId(),
					reference.forward(), reference.targetId(),
					reference.browseName(), reference.displayName(),
					reference.nodeClass(), reference.typeDefinitionId());
		}

		String text() {
			return "  ".repeat(Math.max(0, depth - 1))
					+ (nodeClass == null ? "?" : nodeClass.name()) + " "
					+ browseName + " " + Json.id(targetId)
					+ (displayName != null && !displayName.equals(browseName)
							&& !displayName.isBlank()
									? "  (" + displayName + ")"
									: "")
					+ (forward ? "" : "  [inverso]");
		}
	}

	/** References to follow. */
	public enum References {
		all, hierarchical
	}

	@Option(names = "--depth", paramLabel = "N", description = "Profondità della visita (predefinita 1, solo i figli diretti).")
	private final int depth = 1;

	@Option(names = "--direction", paramLabel = "DIREZIONE", description = "Direzione dei riferimenti: ${COMPLETION-CANDIDATES} (predefinita Forward).")
	private final BrowseDirection direction = BrowseDirection.Forward;

	@Parameters(index = "0", arity = "0..1", paramLabel = "NODO", description = "Nodo di partenza (predefinito i=85, la cartella Objects).")
	private String node;

	@Option(names = "--node-class", split = ",", paramLabel = "CLASSE", description = "Solo i nodi di queste classi (Object, Variable, Method, ObjectType, VariableType, ReferenceType, DataType, View), separate da virgole.")
	private final List<NodeClass> nodeClasses = new ArrayList<>();

	@Option(names = "--references", paramLabel = "TIPO", description = "Riferimenti da seguire: ${COMPLETION-CANDIDATES} (predefinito hierarchical).")
	private final References references = References.hierarchical;

	private void browse(final WaldotClient client, final BrowseFilter filter,
			final NodeId start, final int level, final Set<NodeId> visited,
			final List<Entry> entries) {
		for (final ReferenceInfo reference : client.addressSpace().browse(start,
				filter)) {
			final Entry entry = Entry.of(level, start, reference);
			entries.add(entry);
			if (level < depth && reference.forward()
					&& reference.targetId() != null
					&& visited.add(reference.targetId())) {
				browse(client, filter, reference.targetId(), level + 1, visited,
						entries);
			}
		}
	}

	@Override
	protected int execute(final WaldotClient client, final Printer printer) {
		final NodeId start = node == null
				? NodeIds.ObjectsFolder
				: nodeId(client, node);
		final BrowseFilter filter = new BrowseFilter(direction,
				references == References.all
						? NodeIds.References
						: NodeIds.HierarchicalReferences,
				true, nodeClasses.toArray(new NodeClass[0]));
		final List<Entry> entries = new ArrayList<>();
		final Set<NodeId> visited = new HashSet<>();
		visited.add(start);
		browse(client, filter, start, 1, visited, entries);
		printer.list(entries, entry -> entry, Entry::text, Entry::targetId);
		return 0;
	}

	@Override
	protected void validate() {
		if (depth < 1) {
			throw usage("--depth deve essere almeno 1");
		}
		if (direction == BrowseDirection.Invalid) {
			throw usage("--direction non valida");
		}
	}
}
