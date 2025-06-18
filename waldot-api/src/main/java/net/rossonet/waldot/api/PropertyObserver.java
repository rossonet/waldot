package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;

/**
 * PropertyObserver interface for handling changes in properties of OPC UA
 * nodes. Implementations of this interface should define how to handle property
 * changes for nodes in the OPC UA server.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface PropertyObserver {

	void propertyChanged(UaNode sourceNode, AttributeId attributeId, Object value);
}
