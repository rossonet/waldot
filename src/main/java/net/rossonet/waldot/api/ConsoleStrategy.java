package net.rossonet.waldot.api;

import net.rossonet.waldot.namespaces.HomunculusNamespace;

public interface ConsoleStrategy {

	void initialize(HomunculusNamespace waldotNamespace);

	Object runExpression(String expression);

}
