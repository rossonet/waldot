package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;

public interface EventObserver {

	void fireEvent(BaseEventTypeNode event);

}
