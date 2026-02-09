
package net.rossonet.waldot.api.models.base;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.models.WaldotElement;

/**
 * GremlinElement is an abstract class that extends UaObjectNode and implements
 * WaldotElement. It provides a base implementation for elements in the Waldot
 * graph model, including methods for managing components, versioning, and
 * removal status.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public abstract class GremlinElement extends UaObjectNode implements WaldotElement {
	protected static IllegalStateException elementAlreadyRemoved(final Class<? extends Element> clazz,
			final NodeId id) {
		return new IllegalStateException(String.format("%s with id %s was removed.", clazz.getSimpleName(), id));
	}

	protected long currentVersion;

	private boolean removed = false;

	protected GremlinElement(final UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask) {
		super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask);
		currentVersion = -1;
	}

	protected GremlinElement(final UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			final LocalizedText displayName, final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final UByte eventNotifier, final long version) {
		super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier);
		currentVersion = version;
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
