package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.model.types.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

public interface EventObserver {

	void fireEvent(UaNode node, BaseEventType event);

}
