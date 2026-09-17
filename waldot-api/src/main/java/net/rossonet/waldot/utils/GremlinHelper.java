package net.rossonet.waldot.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;

/**
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a r.l.
 */
public class GremlinHelper {

	public static final class EdgeDump {

		private final String[] parameters;
		private final String source;
		private final String target;

		public EdgeDump(String source, String target, String[] parameters) {
			this.source = source;
			this.target = target;
			this.parameters = parameters;
		}

		public String[] getParameters() {
			return parameters;
		}

		public String getSource() {
			return source;
		}

		public String getTarget() {
			return target;
		}

	}

	public static List<EdgeDump> dumpGraphEdges(WaldotGraph graph,
			boolean includeEphemeral) {
		final List<EdgeDump> result = new ArrayList<>();
		final List<Edge> edges = graph.traversal().E().toList();
		for (final Edge e : edges) {
			boolean positiveEphemeral = false;
			final List<String> edgeData = new ArrayList<>();
			final String source = ((NodeId) e.outVertex().id())
					.toParseableString();
			final String target = ((NodeId) e.inVertex().id())
					.toParseableString();
			for (int i = 0; i < e.keys().size(); i++) {
				final String key = (String) e.keys().toArray()[i];
				edgeData.add(key);
				edgeData.add(e.property(key).value().toString());
				if (key.equals(MiloStrategy.EPHEMERAL_PARAMETER.toLowerCase())
						&& e.property(key).value().toString().equals("true")) {
					positiveEphemeral = true;
				}
			}
			if (positiveEphemeral && !includeEphemeral) {
				continue;
			}
			result.add(new EdgeDump(source, target,
					edgeData.toArray(new String[0])));
		}
		return result;
	}

	public static List<String[]> dumpGraphVertices(WaldotGraph graph,
			boolean includeEphemeral) {
		final List<String[]> result = new ArrayList<>();
		final List<Vertex> vertices = graph.traversal().V().toList();
		for (final Vertex v : vertices) {
			final List<String> vertexData = new ArrayList<>();
			boolean positiveEphemeral = false;
			boolean foundNodeId = false;
			for (int i = 0; i < v.keys().size(); i++) {
				final String key = (String) v.keys().toArray()[i];
				vertexData.add(key);
				vertexData.add(v.property(key).value().toString());
				if (key.equals(MiloStrategy.EPHEMERAL_PARAMETER.toLowerCase())
						&& v.property(key).value().toString().equals("true")) {
					positiveEphemeral = true;
				}
				if (key.equals(MiloStrategy.ID_PARAMETER.toLowerCase())
						&& v.property(key).value().toString().equals(
								((NodeId) v.id()).toParseableString())) {
					foundNodeId = true;
				}
			}
			if (!foundNodeId) {
				vertexData.add(MiloStrategy.ID_PARAMETER.toLowerCase());
				vertexData.add(((NodeId) v.id()).toParseableString());
			}
			if (positiveEphemeral && !includeEphemeral) {
				continue;
			}
			result.add(vertexData.toArray(new String[0]));
		}
		return result;
	}

	private GremlinHelper() {
		throw new UnsupportedOperationException("Just for static usage");

	}

}
