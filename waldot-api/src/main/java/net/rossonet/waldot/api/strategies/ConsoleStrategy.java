package net.rossonet.waldot.api.strategies;

import net.rossonet.waldot.api.models.WaldotNamespace;

public interface ConsoleStrategy {

	void initialize(WaldotNamespace waldotNamespace);

	Object runExpression(String expression);

}
