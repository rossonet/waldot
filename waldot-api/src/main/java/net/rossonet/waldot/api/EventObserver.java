package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.model.types.objects.BaseEventType;

public interface EventObserver {

	void fireEvent(BaseEventType event);

}
