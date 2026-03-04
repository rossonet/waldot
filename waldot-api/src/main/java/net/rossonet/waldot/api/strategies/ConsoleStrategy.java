package net.rossonet.waldot.api.strategies;

import java.util.Collection;
import java.util.List;

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
public interface ConsoleStrategy extends AutoCloseable {
	public static String ALIAS_LABEL = "alias";
	public static String COMMANDS_LABEL = "cmd";
	public static String GRAPH_LABEL = "graph";
	public static String LOG_LABEL = "log";
	public static final String MATH = "math";
	public static final String RANDOM = "rand";
	public static String SELF_LABEL = "self";
	public static String TRAVERSE_LABEL = "g";

	List<WaldotCommand> getCommands();

	WaldotNamespace getWaldotNamespace();

	void initialize(WaldotNamespace waldotNamespace);

	Collection<String> listConfiguredCommands();

	void registerCommand(WaldotCommand command);

	void removeCommand(WaldotCommand command);

	Object runExpression(String expression);

	Object runExpression(String expression, JexlContext jexlContext);

}
