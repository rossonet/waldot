package net.rossonet.waldot.client.api.address;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;

/**
 * A reference returned by a browse.
 *
 * @param referenceTypeId  type of the reference
 * @param forward          direction of the reference
 * @param targetId         target node (null if it belongs to another server)
 * @param browseName       browse name of the target
 * @param displayName      display name of the target
 * @param nodeClass        class of the target
 * @param typeDefinitionId type definition of the target (may be null)
 */
public record ReferenceInfo(NodeId referenceTypeId, boolean forward, NodeId targetId, String browseName,
		String displayName, NodeClass nodeClass, NodeId typeDefinitionId) {
}
