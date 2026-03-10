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
 * <p>The ConsoleStrategy provides a command-line interface for interacting with
 * the WaldOT graph. It uses JEXL (Java Expression Language) for expression evaluation
 * and provides access to graph traversal, math functions, random number generation,
 * and custom commands.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * ConsoleStrategy console = new MyConsoleStrategy();
 * console.initialize(waldotNamespace);
 * 
 * // Execute Gremlin traversal
 * Object result = console.runExpression("g.V().count()");
 * 
 * // Execute with custom context
 * JexlContext context = new JexlContext();
 * context.set("myVar", "value");
 * Object result2 = console.runExpression("myVar.toUpperCase()", context);
 * 
 * // List available commands
 * for (String cmd : console.listConfiguredCommands()) {
 *     System.out.println(cmd);
 * }
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface ConsoleStrategy extends AutoCloseable {
	/**
	 * Label used for alias configuration in the console context.
	 */
	public static String ALIAS_LABEL = "alias";
	/**
	 * Label used for commands in the console context.
	 */
	public static String COMMANDS_LABEL = "cmd";
	/**
	 * Label used for graph traversal reference (Gremlin) in the console context.
	 */
	public static String GRAPH_LABEL = "graph";
	/**
	 * Label used for logging in the console context.
	 */
	public static String LOG_LABEL = "log";
	/**
	 * Namespace for math functions in JEXL expressions.
	 */
	public static final String MATH = "math";
	/**
	 * Namespace for random number generation in JEXL expressions.
	 */
	public static final String RANDOM = "rand";
	/**
	 * Label used for self-reference in the console context.
	 */
	public static String SELF_LABEL = "self";
	/**
	 * Label used for Gremlin traversal variable 'g' in the console context.
	 */
	public static String TRAVERSE_LABEL = "g";

	/**
	 * Returns the list of commands available in this console strategy.
	 * 
	 * <p>Commands are executable operations that can be invoked through the console.
	 * Each command is associated with a WaldotCommand that defines input/output
	 * arguments and execution logic.</p>
	 * 
	 * @return list of available WaldotCommand instances
	 * @see WaldotCommand
	 */
	List<WaldotCommand> getCommands();

	/**
	 * Returns the WaldotNamespace associated with this console strategy.
	 * 
	 * <p>The namespace provides access to the underlying graph, OPC UA server,
	 * and other system components needed for command execution.</p>
	 * 
	 * @return the WaldotNamespace instance
	 * @see WaldotNamespace
	 */
	WaldotNamespace getWaldotNamespace();

	/**
	 * Initializes the console strategy with the given WaldotNamespace.
	 * 
	 * <p>This method should be called before any other console operations.
	 * It sets up the JEXL context, registers built-in functions, and prepares
	 * the console for command execution.</p>
	 * 
	 * @param waldotNamespace the WaldotNamespace to use
	 * @throws Exception if initialization fails
	 */
	void initialize(WaldotNamespace waldotNamespace);

	/**
	 * Lists all configured command names available in the console.
	 * 
	 * <p>This returns the command names (labels) that can be executed via
	 * the console. These correspond to registered WaldotCommand instances.</p>
	 * 
	 * @return collection of command name strings
	 * @see #registerCommand(WaldotCommand)
	 */
	Collection<String> listConfiguredCommands();

	/**
	 * Registers a command with the console strategy.
	 * 
	 * <p>Registered commands become available for execution through the console
	 * using their console command string. The command is added to the JEXL
	 * context under the COMMANDS_LABEL namespace.</p>
	 * 
	 * @param command the WaldotCommand to register
	 * @see #removeCommand(WaldotCommand)
	 * @see WaldotCommand
	 */
	void registerCommand(WaldotCommand command);

	/**
	 * Removes a command from the console strategy.
	 * 
	 * <p>The command is unregistered and no longer available for execution
	 * through the console.</p>
	 * 
	 * @param command the WaldotCommand to remove
	 * @see #registerCommand(WaldotCommand)
	 */
	void removeCommand(WaldotCommand command);

	/**
	 * Executes a JEXL expression with the default console context.
	 * 
	 * <p>The default context includes:</p>
	 * <ul>
	 *   <li>{@code g} - Graph traversal source (Gremlin)</li>
	 *   <li>{@code math} - Math functions</li>
	 *   <li>{@code rand} - Random number generation</li>
	 *   <li>{@code cmd} - Registered commands</li>
	 *   <li>{@code log} - Logging functions</li>
	 *   <li>{@code self} - Reference to this console strategy</li>
	 * </ul>
	 * 
	 * <p>Example expressions:</p>
	 * <pre>{@code
	 * // Count vertices
	 * g.V().count()
	 * 
	 * // Math expression
	 * math.sqrt(16) + math.pow(2, 3)
	 * 
	 * // Random number
	 * rand.nextInt(100)
	 * 
	 * // Call a registered command
	 * cmd.help()
	 * }</pre>
	 * 
	 * @param expression the JEXL expression to execute
	 * @return the result of the expression evaluation
	 * @see #runExpression(String, JexlContext)
	 */
	Object runExpression(String expression);

	/**
	 * Executes a JEXL expression with a custom JEXL context.
	 * 
	 * <p>This method allows executing expressions with additional context variables
	 * beyond the default console context. The custom context is merged with the
	 * default context, with custom variables taking precedence.</p>
	 * 
	 * <p>Example:</p>
	 * <pre>{@code
	 * JexlContext context = new JexlContext();
	 * context.set("myVertex", someVertex);
	 * context.set("threshold", 100);
	 * 
	 * Object result = console.runExpression(
	 *     "g.V(myVertex).out().values('temperature').filter{it > threshold}",
	 *     context
	 * );
	 * }</pre>
	 * 
	 * @param expression the JEXL expression to execute
	 * @param jexlContext custom context variables to make available
	 * @return the result of the expression evaluation
	 * @see #runExpression(String)
	 */
	Object runExpression(String expression, JexlContext jexlContext);

}
