package net.rossonet.waldot.api.rules;

import org.eclipse.milo.opcua.sdk.server.nodes.AttributeObserver;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.PropertyObserver;

public interface Rule extends AttributeObserver, PropertyObserver, EventObserver {

	String getAction();

	String getCondition();

	String getLabel();

	NodeId getNodeId();

	int getPriority();

}
