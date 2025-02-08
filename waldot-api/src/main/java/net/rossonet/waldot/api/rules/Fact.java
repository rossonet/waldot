package net.rossonet.waldot.api.rules;

public interface Fact {

	public enum FactType {
		EVENT, DATA_UPDATE
	}

	String getAttribute();

	FactType getType();

	Object getValue();

}
