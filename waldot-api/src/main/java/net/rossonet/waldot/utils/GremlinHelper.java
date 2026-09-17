package net.rossonet.waldot.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.models.WaldotGraph;

/**
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a r.l.
 */
public class GremlinHelper {

	public static List<String[]> dumpGraphEdges(WaldotGraph graph) {
		final List<String[]> result = new ArrayList<>();
		final List<Edge> edges = graph.traversal().E().toList();
		for (final Edge e : edges) {
			final String[] edgeData = new String[(e.keys().size() + 2) * 2];
			edgeData[0] = "source";
			edgeData[1] = ((NodeId) e.outVertex().id()).toParseableString();
			edgeData[2] = "destination";
			edgeData[3] = ((NodeId) e.inVertex().id()).toParseableString();
			int counter = 4;
			for (int i = 0; i < e.keys().size(); i++) {
				final String key = (String) e.keys().toArray()[i];
				edgeData[counter] = key;
				counter++;
				edgeData[counter] = e.property(key).value().toString();
				counter++;
			}
			result.add(edgeData);
		}
		return result;
	}

	public static List<String[]> dumpGraphVertices(WaldotGraph graph) {
		final List<String[]> result = new ArrayList<>();
		final List<Vertex> vertices = graph.traversal().V().toList();
		for (final Vertex v : vertices) {
			final String[] vertexData = new String[v.keys().size() * 2];
			int counter = 0;
			for (int i = 0; i < v.keys().size(); i++) {
				final String key = (String) v.keys().toArray()[i];
				vertexData[counter] = key;
				counter++;
				vertexData[counter] = v.property(key).value().toString();
				counter++;
			}
			result.add(vertexData);
		}
		return result;
	}

	private GremlinHelper() {
		throw new UnsupportedOperationException("Just for static usage");

	}

}
