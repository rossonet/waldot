package net.rossonet.waldot.client.api.address;

import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;

/**
 * Filter of a browse request.
 *
 * @param direction       direction of the references
 * @param referenceTypeId type of the references
 * @param includeSubtypes include the subtypes of the reference type
 * @param nodeClasses     node classes to return, empty for all
 */
public record BrowseFilter(BrowseDirection direction, NodeId referenceTypeId, boolean includeSubtypes,
		NodeClass... nodeClasses) {

	/** Forward hierarchical references, every node class. */
	public static BrowseFilter children() {
		return new BrowseFilter(BrowseDirection.Forward, NodeIds.HierarchicalReferences, true);
	}

	/** Forward references of every type. */
	public static BrowseFilter forward() {
		return new BrowseFilter(BrowseDirection.Forward, NodeIds.References, true);
	}

	/** References of every type in both directions. */
	public static BrowseFilter all() {
		return new BrowseFilter(BrowseDirection.Both, NodeIds.References, true);
	}

	/** @return the node class mask of the request */
	public int nodeClassMask() {
		int mask = 0;
		if (nodeClasses != null) {
			for (final NodeClass nodeClass : nodeClasses) {
				mask |= nodeClass.getValue();
			}
		}
		return mask;
	}

	/** Same filter limited to the given node classes. */
	public BrowseFilter withNodeClasses(final NodeClass... classes) {
		return new BrowseFilter(direction, referenceTypeId, includeSubtypes, classes);
	}
}
