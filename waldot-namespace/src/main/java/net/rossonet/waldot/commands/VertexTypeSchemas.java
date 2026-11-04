package net.rossonet.waldot.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand.VertexParameter;
import net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes;

/**
 * Schemas of the vertex types of the loaded plugins, built from the create
 * commands (key, type, default, range, allowed values, description) and, for
 * the types without a create command, from the parameters published on the
 * OPC UA type node (key, type, description).
 */
public final class VertexTypeSchemas {

	private static final String RUNTIME_PREFIX = "run!";

	/** Create command of a type, if any. */
	public static Optional<AbstractCreateVertexCommand> createCommand(final WaldotNamespace namespace,
			final String type) {
		for (final PluginListener plugin : namespace.getPlugins()) {
			for (final WaldotCommand command : plugin.getCommands()) {
				if (command instanceof final AbstractCreateVertexCommand create
						&& type.equals(create.getVertexType())) {
					return Optional.of(create);
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Schema of a vertex type.
	 *
	 * @throws IllegalArgumentException if no plugin manages the type
	 */
	public static Map<String, Object> describe(final WaldotNamespace namespace, final String type) {
		final PluginListener plugin = owner(namespace, type).orElseThrow(() -> new IllegalArgumentException(
				"tipo '" + type + "' sconosciuto; tipi: " + String.join(", ", types(namespace).keySet())));
		final Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", type);
		schema.put("plugin", plugin.getClass().getSimpleName());
		final List<Map<String, Object>> properties = new ArrayList<>();
		final Optional<AbstractCreateVertexCommand> create = createCommand(namespace, type);
		if (create.isPresent()) {
			final AbstractCreateVertexCommand command = create.get();
			final Map<String, Object> c = new LinkedHashMap<>();
			c.put("name", command.getBrowseName() == null ? null : command.getBrowseName().getName());
			c.put("directory", command.getDirectory());
			c.put("description", command.getDescription() == null ? null : command.getDescription().getText());
			schema.put("create_command", c);
			for (final VertexParameter parameter : command.getParameters()) {
				properties.add(parameter.toSchema());
			}
			schema.put("source", "create-command");
		} else {
			schema.put("source", "type-node");
		}
		final List<Map<String, Object>> runtime = new ArrayList<>();
		final List<Map<String, Object>> fromTypeNode = typeNodeParameters(namespace, plugin, type);
		for (final Map<String, Object> p : fromTypeNode) {
			final String key = String.valueOf(p.get("key"));
			if (key.startsWith(RUNTIME_PREFIX)) {
				runtime.add(p);
			} else if (create.isEmpty() || properties.stream().noneMatch(e -> key.equalsIgnoreCase(
					String.valueOf(e.get("key"))))) {
				p.put("from_type_node", true);
				properties.add(p);
			}
		}
		schema.put("properties", properties);
		schema.put("runtime", runtime);
		return schema;
	}

	private static Optional<PluginListener> owner(final WaldotNamespace namespace, final String type) {
		for (final PluginListener plugin : namespace.getPlugins()) {
			if (plugin.getManagedVertexTypes().contains(type) || plugin.containsVertexType(type)) {
				return Optional.of(plugin);
			}
		}
		return Optional.empty();
	}

	private static String typeName(final NodeId dataType) {
		if (dataType == null) {
			return "string";
		}
		for (final VariableNodeTypes t : VariableNodeTypes.values()) {
			if (t.getNodeId().equals(dataType)) {
				return VertexParameter.schemaType(t);
			}
		}
		return NodeIds.Boolean.equals(dataType) ? "bool" : "string";
	}

	private static List<Map<String, Object>> typeNodeParameters(final WaldotNamespace namespace,
			final PluginListener plugin, final String type) {
		final List<Map<String, Object>> out = new ArrayList<>();
		final NodeId typeNodeId = plugin.getVertexTypeNode(type);
		if (typeNodeId == null) {
			return out;
		}
		final Optional<UaNode> typeNode = namespace.getStorageManager().getNode(typeNodeId);
		if (typeNode.isEmpty()) {
			return out;
		}
		for (final Reference r : typeNode.get().getReferences()) {
			if (!r.isForward() || !(r.getReferenceTypeId().equals(NodeIds.HasComponent)
					|| r.getReferenceTypeId().equals(NodeIds.HasProperty))) {
				continue;
			}
			final Optional<NodeId> target = r.getTargetNodeId().toNodeId(namespace.getNamespaceTable());
			if (target.isEmpty()) {
				continue;
			}
			final Optional<UaNode> node = namespace.getStorageManager().getNode(target.get());
			if (node.isPresent() && node.get() instanceof final UaVariableNode v) {
				final Map<String, Object> m = new LinkedHashMap<>();
				m.put("key", v.getBrowseName().getName());
				m.put("type", typeName(v.getDataType()));
				m.put("description", v.getDescription() == null ? "" : v.getDescription().getText());
				out.add(m);
			}
		}
		return out;
	}

	/** Vertex types of the loaded plugins: type → plugin, create command. */
	public static Map<String, Map<String, Object>> types(final WaldotNamespace namespace) {
		final Map<String, Map<String, Object>> types = new TreeMap<>();
		for (final PluginListener plugin : namespace.getPlugins()) {
			for (final String type : plugin.getManagedVertexTypes()) {
				final Map<String, Object> m = new LinkedHashMap<>();
				m.put("type", type);
				m.put("plugin", plugin.getClass().getSimpleName());
				final Optional<AbstractCreateVertexCommand> create = createCommand(namespace, type);
				m.put("create_command", create.map(c -> c.getBrowseName() == null ? null : c.getBrowseName().getName())
						.orElse(null));
				m.put("parameters", create.map(c -> c.getParameters().size()).orElse(0));
				types.put(type, m);
			}
		}
		return types;
	}

	private VertexTypeSchemas() {
	}
}
