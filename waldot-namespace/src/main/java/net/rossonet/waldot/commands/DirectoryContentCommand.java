package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_JSON;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.DIRECTORY_CONTENT_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.INFORMATION_DIRECTORY;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.changes.GraphChangeRecorder;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;

/**
 * Lists, without removing anything, the vertices and edges that
 * {@code delete directory} would remove: the dry-run of the deletion.
 */
public class DirectoryContentCommand extends AbstractExchangeCommand {

	private static final ObjectMapper JSON = new ObjectMapper();

	public DirectoryContentCommand(final WaldotNamespace namespace) {
		super(namespace, DIRECTORY_CONTENT_COMMAND,
				"list the vertices and edges that 'delete directory' would remove (dry-run) as JSON",
				INFORMATION_DIRECTORY, new String[] { ARG_DIRECTORY }, new String[] { ARG_JSON, ARG_ERROR });
	}

	@Override
	public Object clone() {
		return new DirectoryContentCommand(waldotNamespace);
	}

	/**
	 * JSON tree of the content: the vertices and edges of the folder and the
	 * edges that go away with its vertices.
	 */
	public static Map<String, Object> content(final WaldotNamespace namespace, final String directory) {
		final List<Map<String, Object>> vertices = new ArrayList<>();
		final Map<String, Map<String, Object>> edges = new LinkedHashMap<>();
		final List<WaldotVertex> found = new ArrayList<>();
		for (final NodeId id : namespace.directoryContent(directory)) {
			final WaldotVertex vertex = namespace.getVertexNode(id);
			if (vertex != null) {
				final Map<String, Object> m = new LinkedHashMap<>();
				m.put("id", GraphChangeRecorder.id(vertex));
				m.put("label", vertex.label());
				final var type = vertex.property("type");
				m.put("type", type.isPresent() ? String.valueOf(type.value()) : null);
				vertices.add(m);
				found.add(vertex);
				continue;
			}
			final WaldotEdge edge = namespace.getEdgeNode(id);
			if (edge != null) {
				edges.put(GraphChangeRecorder.id(edge), edge(edge, false));
			}
		}
		for (final WaldotVertex vertex : found) {
			final java.util.Iterator<org.apache.tinkerpop.gremlin.structure.Edge> it = vertex
					.edges(org.apache.tinkerpop.gremlin.structure.Direction.BOTH);
			while (it.hasNext()) {
				final org.apache.tinkerpop.gremlin.structure.Edge e = it.next();
				edges.putIfAbsent(GraphChangeRecorder.id(e), edge(e, true));
			}
		}
		final Map<String, Object> out = new LinkedHashMap<>();
		out.put("directory", directory);
		out.put("count", vertices.size() + edges.size());
		out.put("vertices", vertices);
		out.put("edges", List.copyOf(edges.values()));
		return out;
	}

	private static Map<String, Object> edge(final org.apache.tinkerpop.gremlin.structure.Edge edge,
			final boolean withVertex) {
		final Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", GraphChangeRecorder.id(edge));
		m.put("label", edge.label());
		m.put("from", GraphChangeRecorder.id(edge.outVertex()));
		m.put("to", GraphChangeRecorder.id(edge.inVertex()));
		if (withVertex) {
			m.put("with_vertex", true);
		}
		return m;
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] output = newOutput();
		try {
			output[0] = JSON.writeValueAsString(
					content(getNamespace(), CommandArguments.requireFirst(inputValues, ARG_DIRECTORY)));
		} catch (final Throwable e) {
			output[1] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
