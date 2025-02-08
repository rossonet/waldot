package net.rossonet.waldot.rules;

import org.eclipse.milo.opcua.stack.core.AttributeId;

import net.rossonet.waldot.api.rules.Fact;

public class DataUpdateFact implements Fact {

	private final String attribute;

	private final Object value;

	public DataUpdateFact(AttributeId attributeId, Object value) {
		this.attribute = attributeId.name();
		this.value = value;
	}

	@Override
	public String getAttribute() {
		return attribute;
	}

	@Override
	public FactType getType() {
		return FactType.DATA_UPDATE;
	}

	@Override
	public Object getValue() {
		return value;
	}

}
