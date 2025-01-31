package net.rossonet.waldot.api.strategies;

import net.rossonet.waldot.api.models.WaldotNamespace;

public interface ConsoleStrategy {
	public static final String G_LABEL = "g";
	public static final String LOG_LABEL = "log";
	public static final String COMMANDS_LABEL = "c";
	public static final String CTX_LABEL = "ctx";

	void initialize(WaldotNamespace waldotNamespace);

	Object runExpression(String expression);

}
