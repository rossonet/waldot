package net.rossonet.waldot.client.api.address;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;

/**
 * Main attributes of a node.
 *
 * @param nodeId      id of the node
 * @param nodeClass   class of the node
 * @param browseName  browse name (without namespace index)
 * @param displayName display name text
 * @param description description text (may be null)
 */
public record NodeInfo(NodeId nodeId, NodeClass nodeClass, String browseName, String displayName,
		String description) {
}
