package net.rossonet.waldot.api.models;

import org.eclipse.milo.opcua.sdk.core.nodes.MethodNode;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaServerNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

/**
 * WaldotCommand is an interface that extends WaldotVertex, MethodNode, and
 * UaServerNode. It defines the structure and behavior of a command in the
 * Waldot system, including methods for adding input and output arguments,
 * running the command, and managing its properties.
 * 
 * <p>WaldotCommand represents an executable command in the WaldOT system that
 * can be invoked via OPC UA method calls or through the console. Commands
 * have input and output arguments and can perform various operations on the graph.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a command
 * WaldotCommand command = new MyCommand();
 * command.setInputArguments(new Argument[]{
 *     new Argument("temperature", NodeIds.Double, ValueRanks.Scalar, null, LocalizedText.english("Temperature value"))
 * });
 * command.setOutputArguments(new Argument[]{
 *     new Argument("result", NodeIds.Boolean, ValueRanks.Scalar, null, LocalizedText.english("Operation result"))
 * });
 * 
 * // Register command
 * namespace.registerCommand(command);
 * 
 * // Execute command
 * Object[] result = command.runCommand("25.5");
 * 
 * // Execute via invocation context
 * Object[] contextResult = command.runCommand(invocationContext, inputValues);
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see WaldotVertex
 * @see MethodNode
 */
public interface WaldotCommand extends WaldotVertex, MethodNode, UaServerNode {

	/**
	 * Adds an input argument to the command.
	 * 
	 * <p>Input arguments define the parameters that must be provided when
	 * invoking the command.</p>
	 * 
	 * @param name the argument name
	 * @param dataType the NodeId of the data type
	 * @param valueRank the value rank (scalar, array, etc.)
	 * @param arrayDimensions the array dimensions, if applicable
	 * @param description the argument description
	 * @see Argument
	 * @see NodeId
	 * @see LocalizedText
	 */
	void addInputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description);

	/**
	 * Adds an output argument to the command.
	 * 
	 * <p>Output arguments define the values returned by the command after execution.</p>
	 * 
	 * @param name the argument name
	 * @param dataType the NodeId of the data type
	 * @param valueRank the value rank (scalar, array, etc.)
	 * @param arrayDimensions the array dimensions, if applicable
	 * @param description the argument description
	 * @see Argument
	 */
	void addOutputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description);

	/**
	 * Executes the command with string inputs.
	 * 
	 * <p>Convenience method for simple command execution.</p>
	 * 
	 * @param methodInputs the input values as strings
	 * @return the results as object array
	 * @see #runCommand(String[])
	 */
	default Object[] exec(String... methodInputs) {
		return runCommand(methodInputs);
	}

	/**
	 * Returns the console command string.
	 * 
	 * <p>This is the string used to invoke the command through the console.</p>
	 * 
	 * @return the console command string
	 */
	String getConsoleCommand();

	/**
	 * Returns the directory containing this command.
	 * 
	 * <p>Commands are organized in directories in the OPC UA address space.</p>
	 * 
	 * @return the directory path
	 */
	String getDirectory();

	/**
	 * Returns the icon for this command.
	 * 
	 * @return the icon ByteString
	 * @see ByteString
	 */
	@Override
	public ByteString getIcon();

	/**
	 * Returns the input arguments for this command.
	 * 
	 * @return array of Argument objects
	 * @see Argument
	 */
	Argument[] getInputArguments();

	/**
	 * Returns the invocation handler for this command.
	 * 
	 * @return the MethodInvocationHandler
	 * @see MethodInvocationHandler
	 */
	MethodInvocationHandler getInvocationHandler();

	/**
	 * Returns the namespace for this command.
	 * 
	 * @return the WaldotNamespace
	 * @see WaldotNamespace
	 */
	@Override
	WaldotNamespace getNamespace();

	/**
	 * Returns the node version.
	 * 
	 * @return the version string
	 */
	@Override
	public String getNodeVersion();

	/**
	 * Returns the output arguments for this command.
	 * 
	 * @return array of Argument objects
	 * @see Argument
	 */
	Argument[] getOutputArguments();

	/**
	 * Checks if the command has been removed.
	 * 
	 * @return true if removed
	 */
	@Override
	boolean isRemoved();

	/**
	 * Runs the command with the given invocation context and input values.
	 * 
	 * <p>This is the main method for executing the command through OPC UA.
	 * The implementation should perform the actual operation and return
	 * the results.</p>
	 * 
	 * @param invocationContext the invocation context
	 * @param inputValues the input values
	 * @return the output values
	 * @see InvocationContext
	 */
	Object[] runCommand(InvocationContext invocationContext, String[] inputValues);

	/**
	 * Runs the command with string inputs.
	 * 
	 * <p>Convenience method for executing commands with string parameters.
	 * Input values are parsed and processed by the command implementation.</p>
	 * 
	 * @param methodInputs the input values as strings
	 * @return the results as object array
	 */
	Object[] runCommand(String[] methodInputs);

	/**
	 * Sets the icon for this command.
	 * 
	 * @param icon the icon ByteString
	 * @see ByteString
	 */
	@Override
	public void setIcon(ByteString icon);

	/**
	 * Sets the input arguments for this command.
	 * 
	 * @param array the array of input arguments
	 * @see Argument
	 */
	void setInputArguments(Argument[] array);

	/**
	 * Sets the node version.
	 * 
	 * @param nodeVersion the version string
	 */
	@Override
	public void setNodeVersion(String nodeVersion);

	/**
	 * Sets the output arguments for this command.
	 * 
	 * @param array the array of output arguments
	 * @see Argument
	 */
	void setOutputArguments(Argument[] array);

	/**
	 * Returns the version of this command.
	 * 
	 * @return the version number
	 */
	@Override
	long version();

}
