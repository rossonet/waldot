package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public interface Rule extends AttributeObserver {

	String getAction();

	String getCondition();

	String getLabel();

	NodeId getNodeId();

	int getPriority();

	void propertyChanged(UaNode node, AttributeId attributeId, Object value);

}
