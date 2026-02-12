package net.rossonet.waldot.rules.events;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

public final class RunnableEvent {

	public enum TypeEvent {
		EVENT, PROPERTY_CHANGE
	}

	private final FireableAction action;
	private final BaseEventType event;
	private final String label;
	private final UaNode node;
	private final TypeEvent type;
	private final Object value;

	public RunnableEvent(UaNode node, BaseEventType event, FireableAction action) {
		this.type = TypeEvent.EVENT;
		this.node = node;
		this.action = action;
		this.event = event;
		this.label = null;
		this.value = null;
	}

	public RunnableEvent(UaNode node, String label, Object value, FireableAction action) {
		this.type = TypeEvent.PROPERTY_CHANGE;
		this.node = node;
		this.action = action;
		this.event = null;
		this.label = label;
		this.value = value;
	}

	public FireableAction getAction(long startingTimeMs) {
		action.setStartingTime(startingTimeMs);
		return action;
	}

	public BaseEventType getEvent() {
		return event;
	}

	public String getLabel() {
		return label;
	}

	public UaNode getNode() {
		return node;
	}

	public TypeEvent getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

}
