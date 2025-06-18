package net.rossonet.waldot.api.rules;

/**
 * Interface representing a Fact in the Waldot system. A Fact can be an event or
 * a data update, and it contains an attribute, a type, and a value.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface Fact {

	public enum FactType {
		EVENT, DATA_UPDATE
	}

	String getAttribute();

	FactType getType();

	Object getValue();

}
