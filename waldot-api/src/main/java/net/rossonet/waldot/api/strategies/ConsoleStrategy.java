package net.rossonet.waldot.api.strategies;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * ConsoleStrategy is an interface that defines the methods required for a
 * Waldot console strategy. It provides methods to initialize the strategy, run
 * expressions, and get the Waldot namespace.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface ConsoleStrategy {
	public static String G_LABEL = "g";
	public static String LOG_LABEL = "log";
	public static String COMMANDS_LABEL = "c";
	public static String CTX_LABEL = "ctx";

	WaldotNamespace getWaldotNamespace();

	void initialize(WaldotNamespace waldotNamespace);

	void reset();

	Object runExpression(String expression);

}
