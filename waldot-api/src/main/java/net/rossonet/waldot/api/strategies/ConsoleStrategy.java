package net.rossonet.waldot.api.strategies;

import java.util.Collection;

import org.apache.commons.jexl3.JexlContext;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * ConsoleStrategy is an interface that defines the methods required for a
 * Waldot console strategy. It provides methods to initialize the strategy, run
 * expressions, and get the Waldot namespace.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface ConsoleStrategy {
	public static String COMMANDS_LABEL = "cmd";
	public static String G_LABEL = "g";
	public static String LOG_LABEL = "log";
	public static String SELF_LABEL = "self";

	WaldotNamespace getWaldotNamespace();

	void initialize(WaldotNamespace waldotNamespace);

	Collection<String> listConfiguredCommands();

	void registerCommand(WaldotCommand command);

	void removeCommand(WaldotCommand command);

	Object runExpression(String expression);

	Object runExpression(String expression, JexlContext jexlContext);

}
