package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ELEMENT;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_JSON;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.DESCRIBE_ELEMENT_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.INFORMATION_DIRECTORY;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotElement;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcCommandServiceFactory;

/**
 * Describes a vertex or an edge as plain JSON: identity, OPC UA attributes,
 * Gremlin properties with the NodeId of their variables (ready to be
 * subscribed), adjacent edges and the commands (method nodes) of the
 * element. NodeIds are rendered in their parseable form.
 */
public class DescribeElementCommand extends AbstractExchangeCommand {

	private static final ObjectMapper JSON = new ObjectMapper();

	private static List<String> argumentNames(final Argument[] arguments) {
		final List<String> names = new ArrayList<>();
		if (arguments != null) {
			for (final Argument argument : arguments) {
				names.add(argument == null ? null : argument.getName());
			}
		}
		return names;
	}

	private static Object jsonValue(final Object value) {
		if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
			return value;
		}
		if (value instanceof NodeId) {
			return ((NodeId) value).toParseableString();
		}
		return String.valueOf(value);
	}

	private static String nodeId(final Object id) {
		return id instanceof NodeId ? ((NodeId) id).toParseableString() : String.valueOf(id);
	}

	private static String stringProperty(final Element element, final String key, final String defaultValue) {
		try {
			final Property<Object> property = element.property(key);
			return property.isPresent() && property.value() != null ? String.valueOf(property.value())
					: defaultValue;
		} catch (final RuntimeException e) {
			return defaultValue;
		}
	}

	public DescribeElementCommand(final WaldotNamespace namespace) {
		super(namespace, DESCRIBE_ELEMENT_COMMAND,
				"describe a vertex or an edge (properties, edges, commands) as JSON", INFORMATION_DIRECTORY,
				new String[] { ARG_ELEMENT }, new String[] { ARG_JSON, ARG_ERROR });
	}

	@Override
	public Object clone() {
		return new DescribeElementCommand(waldotNamespace);
	}

	private List<Map<String, Object>> commands(final Element element) {
		final List<Map<String, Object>> result = new ArrayList<>();
		for (final WaldotCommand command : OpcCommandServiceFactory.commandsOf(element)) {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("name", command.getBrowseName() == null ? null : command.getBrowseName().getName());
			entry.put("nodeId", ((UaNode) command).getNodeId().toParseableString());
			entry.put("description", command.getDescription() == null ? null : command.getDescription().getText());
			entry.put("inputs", argumentNames(command.getInputArguments()));
			entry.put("outputs", argumentNames(command.getOutputArguments()));
			result.add(entry);
		}
		return result;
	}

	Map<String, Object> describe(final String element) {
		final NodeId nodeId = MiloStrategy.getNodeIdManager().convert(getNamespace().getGremlinGraph(),
				element.trim());
		if (nodeId == null) {
			throw new IllegalArgumentException("invalid element id '" + element + "'");
		}
		final WaldotVertex vertex = getNamespace().getVertexNode(nodeId);
		if (vertex != null) {
			return describeVertex(vertex);
		}
		final WaldotEdge edge = getNamespace().getEdgeNode(nodeId);
		if (edge != null) {
			return describeEdge(edge);
		}
		throw new IllegalArgumentException("element '" + element + "' not found");
	}

	private Map<String, Object> describeCommon(final WaldotElement element, final String kind) {
		final Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", element.getNodeId().toParseableString());
		result.put("kind", kind);
		result.put("label", element.label());
		result.put("browseName", element.getBrowseName() == null ? null : element.getBrowseName().getName());
		result.put("displayName", element.getDisplayName() == null ? null : element.getDisplayName().getText());
		result.put("description", element.getDescription() == null ? null : element.getDescription().getText());
		if (element instanceof UaObjectNode && ((UaObjectNode) element).getEventNotifier() != null) {
			result.put("eventNotifier", ((UaObjectNode) element).getEventNotifier().intValue());
		}
		return result;
	}

	private Map<String, Object> describeEdge(final WaldotEdge edge) {
		final Map<String, Object> result = describeCommon(edge, "edge");
		result.put("type", stringProperty(edge, MiloStrategy.TYPE_FIELD.toLowerCase(), edge.label()));
		result.put("source", nodeId(edge.outVertex().id()));
		result.put("target", nodeId(edge.inVertex().id()));
		result.put("properties", properties(edge));
		result.put("commands", commands(edge));
		return result;
	}

	private Map<String, Object> describeVertex(final WaldotVertex vertex) {
		final Map<String, Object> result = describeCommon(vertex, "vertex");
		result.put("type", stringProperty(vertex, MiloStrategy.TYPE_FIELD.toLowerCase(), "vertex"));
		result.put("properties", properties(vertex));
		result.put("outEdges", edges(vertex, Direction.OUT));
		result.put("inEdges", edges(vertex, Direction.IN));
		result.put("commands", commands(vertex));
		return result;
	}

	private List<Map<String, Object>> edges(final WaldotVertex vertex, final Direction direction) {
		final List<Map<String, Object>> result = new ArrayList<>();
		vertex.edges(direction).forEachRemaining(edge -> {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("id", nodeId(edge.id()));
			entry.put("label", edge.label());
			entry.put("type", stringProperty(edge, MiloStrategy.TYPE_FIELD.toLowerCase(), edge.label()));
			entry.put("source", nodeId(edge.outVertex().id()));
			entry.put("target", nodeId(edge.inVertex().id()));
			result.add(entry);
		});
		return result;
	}

	private List<Map<String, Object>> properties(final Element element) {
		final List<Map<String, Object>> result = new ArrayList<>();
		element.properties().forEachRemaining(p -> {
			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("key", p.key());
			entry.put("nodeId", p instanceof UaNode ? ((UaNode) p).getNodeId().toParseableString() : null);
			final Object value = p.isPresent() ? p.value() : null;
			entry.put("value", jsonValue(value));
			entry.put("valueType", value == null ? null : value.getClass().getSimpleName());
			result.add(entry);
		});
		return result;
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] output = newOutput();
		try {
			output[0] = JSON.writeValueAsString(describe(CommandArguments.requireFirst(inputValues, ARG_ELEMENT)));
		} catch (final Throwable e) {
			output[1] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
