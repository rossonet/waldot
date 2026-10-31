
package net.rossonet.waldot.api.models.base;

import java.util.Map;
import java.util.Optional;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.core.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.structured.AccessRestrictionType;
import org.eclipse.milo.opcua.stack.core.types.structured.RolePermissionType;

import net.rossonet.waldot.api.models.WaldotElement;

/**
 * GremlinElement is an abstract class that extends UaObjectNode and implements
 * WaldotElement. It provides a base implementation for elements in the Waldot
 * graph model, including methods for managing components, versioning, and
 * removal status.
 * 
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public abstract class GremlinElement extends UaObjectNode implements WaldotElement {
	protected static IllegalStateException elementAlreadyRemoved(final Class<? extends Element> clazz,
			final NodeId id) {
		return new IllegalStateException(String.format("%s with id %s was removed.", clazz.getSimpleName(), id));
	}

	protected long currentVersion;

	private boolean removed = false;

	/**
	 * OPC UA property nodes (HasProperty) by browse name. Milo finds them by
	 * scanning every reference of the node, which made each read of a managed
	 * property linear in the number of references.
	 */
	private final Map<QualifiedName, VariableNode> propertyNodes = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * The NodeId never changes after creation: it is kept here so that
	 * {@link #getNodeId()} does not take the node monitor (Milo synchronizes
	 * {@code getAttribute} on the node, which made every {@code id()} and
	 * {@code hashCode()} contend with attribute writes).
	 */
	private final NodeId nodeId;

	protected GremlinElement(final UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask) {
		super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask);
		currentVersion = -1;
		this.nodeId = nodeId;
	}

	protected GremlinElement(final UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final UByte eventNotifier, final long version) {
		super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask,
				new RolePermissionType[0], new RolePermissionType[0], new AccessRestrictionType(UShort.valueOf(0)),
				eventNotifier);
		currentVersion = version;
		this.nodeId = nodeId;
	}

	@Override
	public NodeId getNodeId() {
		// null only while the Milo constructor is still running
		return nodeId != null ? nodeId : super.getNodeId();
	}

	@Override
	public Optional<VariableNode> getPropertyNode(
			final QualifiedName browseName) {
		if (propertyNodes == null) {
			// still inside the Milo constructor
			return super.getPropertyNode(browseName);
		}
		final VariableNode cached = propertyNodes.get(browseName);
		// the node can have been removed or replaced in the address space
		if (cached != null && getManagedNode(cached.getNodeId())
				.orElse(null) == cached) {
			return Optional.of(cached);
		}
		final Optional<VariableNode> found = super.getPropertyNode(browseName);
		if (found.isPresent()) {
			propertyNodes.put(browseName, found.get());
		} else {
			propertyNodes.remove(browseName);
		}
		return found;
	}

	@Override
	public void addComponent(final WaldotElement waldotElement) {
		super.addComponent((UaNode) waldotElement);

	}

	@Override
	public abstract Object clone();

	@Override
	public boolean equals(final Object object) {
		return ElementHelper.areEqual(this, object);
	}

	@Override
	public int hashCode() {
		return ElementHelper.hashCode(this);
	}

	@Override
	public NodeId id() {
		return getNodeId();
	}

	@Override
	public boolean isRemoved() {
		return removed;
	}

	@Override
	public void remove() {
		this.removed = true;
	}

	@Override
	public void removeComponent(final WaldotElement waldotElement) {
		super.removeComponent((UaNode) waldotElement);

	}

	@Override
	public long version() {
		return this.currentVersion;
	}

}
