package net.rossonet.waldot.client.support;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

import net.rossonet.waldot.api.strategies.MiloStrategy;

/**
 * Implementation of the basic operations
 * ({@link net.rossonet.waldot.client.api.WaldotBaseOperations}) shared by the
 * client versions: calls of the console commands by browse name.
 */
public final class BaseOperationCalls {

	public static final String ABOUT_COMMAND = "about";
	public static final String ADD_EDGE_COMMAND = "add edge";
	public static final String ADD_VERTEX_COMMAND = "add vertex";
	public static final String DELETE_COMMAND = "delete";
	public static final String EDGES_FOLDER = "waldot/Edges";
	public static final String QUERY_COMMAND = "query";
	public static final String VERTICES_FOLDER = "waldot/Vertices";
	/** Namespace index of the WaldOT nodes used by the first client version. */
	public static final int WALDOT_NAMESPACE_INDEX = 2;

	public static String[] call(final OpcUaClient client, final NodeId objectId, final String method,
			final Object... inputs) throws UaException {
		if (client == null) {
			throw new UaException(org.eclipse.milo.opcua.stack.core.StatusCodes.Bad_NotConnected,
					"client not connected");
		}
		final UaObjectNode node = client.getAddressSpace().getObjectNode(objectId);
		final Variant[] variants = new Variant[inputs.length];
		for (int i = 0; i < inputs.length; i++) {
			variants[i] = new Variant(inputs[i]);
		}
		return toStrings(node.callMethod(method, variants)).toArray(new String[0]);
	}

	public static String[] createEdge(final OpcUaClient client, final String label, final String sourceNodeId,
			final String destinationNodeId, final String[] keyValues) throws UaException {
		return call(client, new NodeId(WALDOT_NAMESPACE_INDEX, EDGES_FOLDER), ADD_EDGE_COMMAND, label, sourceNodeId,
				destinationNodeId, String.join(",", keyValues));
	}

	public static String[] createVertex(final OpcUaClient client, final String id, final String label,
			final String type, final String[] keyValues) throws UaException {
		return call(client, new NodeId(WALDOT_NAMESPACE_INDEX, VERTICES_FOLDER), ADD_VERTEX_COMMAND, id, label, type,
				String.join(",", keyValues));
	}

	public static String[] deleteVertex(final OpcUaClient client, final int id) throws UaException {
		return call(client, new NodeId(WALDOT_NAMESPACE_INDEX, id), DELETE_COMMAND);
	}

	public static String[] deleteVertex(final OpcUaClient client, final String id) throws UaException {
		return call(client, new NodeId(WALDOT_NAMESPACE_INDEX, id), DELETE_COMMAND);
	}

	public static List<String> getServerInfo(final OpcUaClient client) throws UaException {
		return List.of(call(client, informationFolder(), ABOUT_COMMAND));
	}

	private static NodeId informationFolder() {
		return new NodeId(WALDOT_NAMESPACE_INDEX, MiloStrategy.INFOMATION_CMD_DIRECTORY);
	}

	public static List<String> runExpression(final OpcUaClient client, final String expression)
			throws UaException {
		return List.of(call(client, informationFolder(), QUERY_COMMAND, expression));
	}

	/** Renders the output variants as strings (null stays null). */
	public static List<String> toStrings(final Variant[] outputs) {
		final List<String> out = new ArrayList<>();
		if (outputs != null) {
			for (final Variant output : outputs) {
				final Object value = output == null ? null : output.getValue();
				out.add(value == null ? null : value.toString());
			}
		}
		return out;
	}

	private BaseOperationCalls() {
	}
}
