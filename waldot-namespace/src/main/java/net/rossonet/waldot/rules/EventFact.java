package net.rossonet.waldot.rules;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;

import net.rossonet.waldot.api.rules.Fact;

public class EventFact implements Fact {

	private final String attribute;
	private final String value;

	public EventFact(BaseEventType event) {
		this.attribute = event.getBrowseName().getName();
		this.value = event.getMessage().getText();
	}

	@Override
	public String getAttribute() {
		return attribute;
	}

	@Override
	public FactType getType() {
		return FactType.EVENT;
	}

	@Override
	public Object getValue() {
		return value;
	}

}
