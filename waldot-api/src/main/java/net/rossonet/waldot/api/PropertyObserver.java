package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;

public interface PropertyObserver {

	void propertyChanged(UaNode sourceNode, AttributeId attributeId, Object value);
}
