package net.rossonet.waldot.jexl;

import org.apache.commons.jexl3.JexlContext;

/**
 * BaseExecutor is an interface that defines the operations for executing
 * JEXL (Java Expression Language) expressions with context management.
 * 
 * <p>BaseExecutor provides a unified interface for expression evaluation
 * in WaldOT. It supports basic expression execution, context variables,
 * and function registration.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create executor
 * BaseExecutor executor = new JexlExecutor();
 * 
 * // Execute simple expression
 * Object result = executor.execute("1 + 2");  // returns 3
 * 
 * // Execute with context
 * JexlContext context = new JexlContext();
 * context.set("x", 10);
 * context.set("y", 20);
 * Object result2 = executor.execute("x + y", context);  // returns 30
 * 
 * // Add context variable
 * executor.addOrUpdateContext("message", "Hello");
 * Object result3 = executor.execute("message.toUpperCase()");
 * 
 * // Register function object
 * executor.setFunctionObject("math", new MathHelper());
 * Object result4 = executor.execute("math.sqrt(16)");  // if MathHelper has sqrt method
 * 
 * // Clean up
 * executor.close();
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see JexlContext
 */
public interface BaseExecutor extends AutoCloseable {

	/**
	 * Executes a JEXL expression with default context.
	 * 
	 * <p>Uses any previously added context variables and function objects.</p>
	 * 
	 * @param expression the JEXL expression to execute
	 * @return the result of the expression evaluation
	 * @throws Exception if execution fails
	 */
	Object execute(String expression);

	/**
	 * Executes a JEXL expression with a custom context.
	 * 
	 * <p>The provided context is merged with the default context for this
	 * execution, allowing temporary variables.</p>
	 * 
	 * @param expression the JEXL expression to execute
	 * @param jexlContext the context variables to use
	 * @return the result of the expression evaluation
	 * @throws Exception if execution fails
	 * @see JexlContext
	 */
	Object execute(String expression, JexlContext jexlContext);

	/**
	 * Adds or updates a context variable.
	 * 
	 * <p>The variable becomes available in subsequent expression executions.
	 * If the variable already exists, its value is updated.</p>
	 * 
	 * @param id the variable identifier
	 * @param context the variable value
	 */
	void addOrUpdateContext(String id, Object context);

	/**
	 * Sets a function object for use in expressions.
	 * 
	 * <p>The function object is registered under the given label and its
	 * methods can be called in expressions using the label.</p>
	 * 
	 * <p>Example: if setFunctionObject("math", new MathHelper()) is called,
	 * expressions can use "math.sqrt(16)" to call MathHelper.sqrt(16).</p>
	 * 
	 * @param label the namespace label for the function object
	 * @param functionalObject the object containing methods to expose
	 */
	void setFunctionObject(String label, Object functionalObject);

}
