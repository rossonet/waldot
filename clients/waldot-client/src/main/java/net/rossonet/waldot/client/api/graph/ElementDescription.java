package net.rossonet.waldot.client.api.graph;

import java.util.List;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * Description of a vertex or an edge returned by the
 * {@code describe element} command.
 *
 * @param id            id of the element
 * @param kind          {@code vertex} or {@code edge}
 * @param label         Gremlin label
 * @param type          WaldOT type ({@code type} property)
 * @param browseName    OPC UA browse name
 * @param displayName   OPC UA display name
 * @param description   OPC UA description
 * @param eventNotifier event notifier attribute (null if not available)
 * @param source        source vertex (edges only)
 * @param target        target vertex (edges only)
 * @param properties    Gremlin properties with the NodeId of their variables
 * @param outEdges      outgoing edges (vertices only)
 * @param inEdges       incoming edges (vertices only)
 * @param commands      commands of the element
 */
public record ElementDescription(NodeId id, String kind, String label, String type, String browseName,
		String displayName, String description, Integer eventNotifier, NodeId source, NodeId target,
		List<PropertyDescription> properties, List<EdgeSummary> outEdges, List<EdgeSummary> inEdges,
		List<CommandSummary> commands) {

	/** A command of the element. */
	public record CommandSummary(String name, NodeId nodeId, String description, List<String> inputs,
			List<String> outputs) {
	}

	/** An edge adjacent to a vertex. */
	public record EdgeSummary(NodeId id, String label, String type, NodeId source, NodeId target) {
	}

	/** A Gremlin property and its OPC UA variable. */
	public record PropertyDescription(String key, NodeId nodeId, Object value, String valueType) {
	}

	/** @return true if the element is an edge */
	public boolean isEdge() {
		return "edge".equals(kind);
	}

	/** @return the property with the given key, or null */
	public PropertyDescription property(final String key) {
		for (final PropertyDescription property : properties) {
			if (property.key().equals(key)) {
				return property;
			}
		}
		return null;
	}
}
