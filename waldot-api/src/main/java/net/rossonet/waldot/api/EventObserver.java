package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

/**
 * EventObserver interface for handling events from OPC UA nodes.
 * Implementations of this interface should define how to handle events fired by
 * nodes in the OPC UA server.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface EventObserver {

	void fireEvent(UaNode node, BaseEventType event);

}
