package net.rossonet.waldot.client.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.client.api.graph.ElementDescription;
import net.rossonet.waldot.client.api.graph.ElementDescription.CommandSummary;
import net.rossonet.waldot.client.api.graph.ElementDescription.EdgeSummary;
import net.rossonet.waldot.client.api.graph.ElementDescription.PropertyDescription;

/** Parser of the JSON returned by the {@code describe element} command. */
public final class ElementDescriptionParser {

	private static final ObjectMapper JSON = new ObjectMapper();

	private static List<EdgeSummary> edges(final JsonNode node) {
		final List<EdgeSummary> edges = new ArrayList<>();
		if (node != null && node.isArray()) {
			for (final JsonNode edge : node) {
				edges.add(new EdgeSummary(nodeId(edge, "id"), text(edge, "label"), text(edge, "type"),
						nodeId(edge, "source"), nodeId(edge, "target")));
			}
		}
		return edges;
	}

	private static NodeId nodeId(final JsonNode node, final String field) {
		final String value = text(node, field);
		return value == null || value.isBlank() ? null : NodeId.parseOrNull(value);
	}

	/**
	 * Parses a description.
	 *
	 * @throws IOException if the JSON is not valid
	 */
	public static ElementDescription parse(final String json) throws IOException {
		final JsonNode root = JSON.readTree(json);
		if (root == null || !root.isObject()) {
			throw new IOException("element description is not a JSON object");
		}
		final List<PropertyDescription> properties = new ArrayList<>();
		final JsonNode propertiesNode = root.get("properties");
		if (propertiesNode != null && propertiesNode.isArray()) {
			for (final JsonNode property : propertiesNode) {
				properties.add(new PropertyDescription(text(property, "key"), nodeId(property, "nodeId"),
						value(property.get("value")), text(property, "valueType")));
			}
		}
		final List<CommandSummary> commands = new ArrayList<>();
		final JsonNode commandsNode = root.get("commands");
		if (commandsNode != null && commandsNode.isArray()) {
			for (final JsonNode command : commandsNode) {
				commands.add(new CommandSummary(text(command, "name"), nodeId(command, "nodeId"),
						text(command, "description"), strings(command.get("inputs")),
						strings(command.get("outputs"))));
			}
		}
		final JsonNode notifier = root.get("eventNotifier");
		return new ElementDescription(nodeId(root, "id"), text(root, "kind"), text(root, "label"),
				text(root, "type"), text(root, "browseName"), text(root, "displayName"), text(root, "description"),
				notifier == null || notifier.isNull() ? null : notifier.asInt(), nodeId(root, "source"),
				nodeId(root, "target"), List.copyOf(properties), List.copyOf(edges(root.get("outEdges"))),
				List.copyOf(edges(root.get("inEdges"))), List.copyOf(commands));
	}

	private static List<String> strings(final JsonNode node) {
		final List<String> values = new ArrayList<>();
		if (node != null && node.isArray()) {
			for (final JsonNode value : node) {
				values.add(value.isNull() ? null : value.asText());
			}
		}
		return values;
	}

	private static String text(final JsonNode node, final String field) {
		final JsonNode value = node == null ? null : node.get(field);
		return value == null || value.isNull() ? null : value.asText();
	}

	private static Object value(final JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		if (node.isIntegralNumber()) {
			return node.canConvertToInt() ? (Object) node.intValue() : (Object) node.longValue();
		}
		if (node.isNumber()) {
			return node.doubleValue();
		}
		return node.asText();
	}

	private ElementDescriptionParser() {
	}
}
