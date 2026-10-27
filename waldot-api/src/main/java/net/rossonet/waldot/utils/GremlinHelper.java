package net.rossonet.waldot.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.json.JSONObject;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.strategies.MiloStrategy;

/**
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public class GremlinHelper {

	public static final class EdgeDump {

		private final String label;
		private final String[] parameters;
		private final String source;
		private final String target;

		public EdgeDump(String source, String target, String[] parameters) {
			this(source, target, null, parameters);
		}

		/**
		 * @param source     parseable NodeId of the source vertex
		 * @param target     parseable NodeId of the target vertex
		 * @param label      label of the edge ({@link Edge#label()})
		 * @param parameters properties of the edge as key/value pairs
		 */
		public EdgeDump(String source, String target, String label,
				String[] parameters) {
			this.source = source;
			this.target = target;
			this.label = label;
			this.parameters = parameters;
		}

		/**
		 * @return the label of the edge, null if not known
		 */
		public String getLabel() {
			return label;
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

	private static final String EPHEMERAL_KEY = MiloStrategy.EPHEMERAL_PARAMETER
			.toLowerCase();

	private static final String ID_KEY = MiloStrategy.ID_PARAMETER
			.toLowerCase();

	/**
	 * Dumps the edges of the graph. The label of each edge is in
	 * {@link EdgeDump#getLabel()}, the properties in
	 * {@link EdgeDump#getParameters()}.
	 *
	 * @param includeEphemeral if false, the ephemeral edges and the edges that
	 *                         touch an ephemeral vertex are skipped (the
	 *                         vertex is not in the dump, so the edge could not
	 *                         be recreated), as the runtime properties
	 *                         ({@value MiloStrategy#RUNTIME_DATA_PREFIX}...)
	 */
	public static List<EdgeDump> dumpGraphEdges(WaldotGraph graph,
			boolean includeEphemeral) {
		final List<EdgeDump> result = new ArrayList<>();
		final List<Edge> edges = graph.traversal().E().toList();
		for (final Edge e : edges) {
			if (!includeEphemeral && (isEphemeral(e)
					|| isEphemeral(e.outVertex())
					|| isEphemeral(e.inVertex()))) {
				continue;
			}
			final List<String> edgeData = new ArrayList<>();
			for (final String key : e.keys()) {
				if (!includeEphemeral && isRuntimeKey(key)) {
					continue;
				}
				edgeData.add(key);
				edgeData.add(e.property(key).value().toString());
			}
			result.add(new EdgeDump(
					((NodeId) e.outVertex().id()).toParseableString(),
					((NodeId) e.inVertex().id()).toParseableString(),
					e.label(), edgeData.toArray(new String[0])));
		}
		return result;
	}

	/**
	 * Dumps the vertices of the graph as key/value pairs. The first pair is
	 * always the id, as parseable NodeId ({@code ns=2;s=pump-1}); an
	 * {@code id} property of the vertex is not repeated.
	 *
	 * @param includeEphemeral if false, the ephemeral vertices and the runtime
	 *                         properties
	 *                         ({@value MiloStrategy#RUNTIME_DATA_PREFIX}...)
	 *                         are skipped
	 */
	public static List<String[]> dumpGraphVertices(WaldotGraph graph,
			boolean includeEphemeral) {
		final List<String[]> result = new ArrayList<>();
		final List<Vertex> vertices = graph.traversal().V().toList();
		for (final Vertex v : vertices) {
			if (!includeEphemeral && isEphemeral(v)) {
				continue;
			}
			final List<String> vertexData = new ArrayList<>();
			vertexData.add(ID_KEY);
			vertexData.add(((NodeId) v.id()).toParseableString());
			for (final String key : v.keys()) {
				if (key.equals(ID_KEY)
						|| (!includeEphemeral && isRuntimeKey(key))) {
					continue;
				}
				vertexData.add(key);
				vertexData.add(v.property(key).value().toString());
			}
			result.add(vertexData.toArray(new String[0]));
		}
		return result;
	}

	/**
	 * DTML JSON of a dumped edge, with the label of the edge.
	 */
	public static JSONObject edgeDumpToDtmlJson(EdgeDump edge) {
		final JSONObject json = propertyArrayToDtmlJson(edge.getSource(),
				edge.getTarget(), edge.getParameters());
		if (edge.getLabel() != null) {
			json.put("@" + MiloStrategy.LABEL_FIELD.toLowerCase(),
					edge.getLabel());
		}
		return json;
	}

	private static boolean isEphemeral(Element element) {
		final Property<?> property = element.property(EPHEMERAL_KEY);
		return property.isPresent()
				&& "true".equals(String.valueOf(property.value()));
	}

	/**
	 * Runtime data (e.g. {@code run!connected}): produced by the vertex while
	 * running, not part of the configuration.
	 */
	private static boolean isRuntimeKey(String key) {
		return key != null && key.toLowerCase(Locale.ROOT)
				.startsWith(MiloStrategy.RUNTIME_DATA_PREFIX);
	}

	public static JSONObject propertyArrayToDtmlJson(String sourceVertex,
			String targetVertex, String[] edgeParameters) {
		String label = null;
		String type = null;
		final List<String> otherParameters = new ArrayList<>();
		for (int i = 0; i < edgeParameters.length; i += 2) {
			final String key = edgeParameters[i];
			final String value = edgeParameters[i + 1];
			if (key.equals(MiloStrategy.LABEL_FIELD.toLowerCase())) {
				label = value;
			} else if (key.equals(MiloStrategy.TYPE_FIELD.toLowerCase())) {
				type = value;
			} else {
				otherParameters.add(key);
				otherParameters.add(value);
			}
		}
		final JSONObject json = new JSONObject();
		json.put("@" + MiloStrategy.LABEL_FIELD.toLowerCase(), label);
		json.put("@" + MiloStrategy.SOURCE_NODE.toLowerCase(), sourceVertex);
		json.put("@" + MiloStrategy.TARGET_NODE.toLowerCase(), targetVertex);
		json.put("@" + MiloStrategy.TYPE_FIELD.toLowerCase(), type);
		for (int i = 0; i < otherParameters.size(); i += 2) {
			final String key = otherParameters.get(i);
			final String value = otherParameters.get(i + 1);
			json.put(key, value);
		}
		return json;
	}

	public static JSONObject propertyArrayToDtmlJson(
			String[] vertexParameters) {
		String nodeId = null;
		String label = null;
		String type = null;
		final List<String> otherParameters = new ArrayList<>();
		for (int i = 0; i < vertexParameters.length; i += 2) {
			final String key = vertexParameters[i];
			final String value = vertexParameters[i + 1];
			if (key.equals(MiloStrategy.ID_PARAMETER.toLowerCase())) {
				nodeId = value;
			} else if (key.equals(MiloStrategy.LABEL_FIELD.toLowerCase())) {
				label = value;
			} else if (key.equals(MiloStrategy.TYPE_FIELD.toLowerCase())) {
				type = value;
			} else {
				otherParameters.add(key);
				otherParameters.add(value);
			}
		}
		final JSONObject json = new JSONObject();
		json.put("@" + MiloStrategy.ID_PARAMETER.toLowerCase(), nodeId);
		json.put("@" + MiloStrategy.LABEL_FIELD.toLowerCase(), label);
		json.put("@" + MiloStrategy.TYPE_FIELD.toLowerCase(), type);
		for (int i = 0; i < otherParameters.size(); i += 2) {
			final String key = otherParameters.get(i);
			final String value = otherParameters.get(i + 1);
			json.put(key, value);
		}
		return json;
	}

	private GremlinHelper() {
		throw new UnsupportedOperationException("Just for static usage");

	}

}
