package net.rossonet.waldot.opc.gremlin;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.opc.OpcCommandInvocationHandler;

public abstract class GremlinCommandVertex extends UaMethodNode implements WaldotCommand {

	protected static IllegalStateException elementAlreadyRemoved(final Class<? extends Element> clazz,
			final NodeId id) {
		return new IllegalStateException(String.format("%s with id %s was removed.", clazz.getSimpleName(), id));
	}

	protected long currentVersion;

	private boolean removed = false;

	protected GremlinCommandVertex(UaNodeContext context, final NodeId nodeId, final QualifiedName browseName,
			LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			Boolean executable, Boolean userExecutable) {
		super(context, nodeId, browseName, displayName, description, writeMask, userWriteMask, executable,
				userExecutable);
		currentVersion = 0;
		setInvocationHandler(new OpcCommandInvocationHandler(this));
	}

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
	public String label() {
		return getBrowseName().getName();
	}

	@Override
	public void remove() {
		this.removed = true;
	}

	@Override
	public long version() {
		return this.currentVersion;
	}
}
