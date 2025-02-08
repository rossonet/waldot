
package net.rossonet.waldot.opc.gremlin;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.models.WaldotElement;
import net.rossonet.waldot.api.models.WaldotGraph;

public abstract class GremlinProperty<DATA_TYPE> extends UaVariableNode implements WaldotElement {

	protected static IllegalStateException elementAlreadyRemoved(final Class<? extends Element> clazz,
			final NodeId id) {
		return new IllegalStateException(String.format("%s with id %s was removed.", clazz.getSimpleName(), id));
	}

	private boolean removed = false;

	protected long currentVersion;

	protected GremlinProperty(WaldotGraph graph, final String key, final DATA_TYPE value, UaNodeContext context,
			NodeId nodeId, LocalizedText description, UInteger writeMask, UInteger userWriteMask, NodeId dataType,
			Integer valueRank, UInteger[] arrayDimensions, UByte accessLevel, UByte userAccessLevel,
			Double minimumSamplingInterval, boolean historizing) {
		super(context, nodeId, QualifiedName.parse(key), LocalizedText.english(key), description, writeMask,
				userWriteMask, null, dataType, valueRank, arrayDimensions, accessLevel, userAccessLevel,
				minimumSamplingInterval, historizing);
		currentVersion = -1;
	}

	@Override
	public void addComponent(WaldotElement waldotElement) {
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
	public String label() {
		return getBrowseName().getName();
	}

	@Override
	public void remove() {

		this.removed = true;
	}

	@Override
	public void removeComponent(WaldotElement waldotElement) {
		super.removeComponent((UaNode) waldotElement);

	}

	@Override
	public long version() {
		return this.currentVersion;
	}

}
