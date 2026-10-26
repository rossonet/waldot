package net.rossonet.waldot.api.models.base;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

import net.rossonet.waldot.api.models.WaldotCommand;

/**
 * CommandInvocationHandler is a class that extends
 * AbstractMethodInvocationHandler and implements the invocation logic for
 * WaldotCommand methods. It handles the input and output arguments, invoking
 * the command with the provided input values.
 */
public class CommandInvocationHandler extends AbstractMethodInvocationHandler {

	/**
	 * Creates a new CommandInvocationHandler for the given method node.
	 *
	 * @param method the UA method node to handle invocation for
	 */
	public CommandInvocationHandler(final UaMethodNode method) {
		super(method);
	}

	@Override
	public Argument[] getInputArguments() {
		final Argument[] i = super.getNode().getInputArguments();
		return i != null ? i : new Argument[0];
	}

	@Override
	public Argument[] getOutputArguments() {
		final Argument[] o = super.getNode().getOutputArguments();
		return o != null ? o : new Argument[0];
	}

	/**
	 * Converts an input argument into the string passed to
	 * {@link WaldotCommand#runCommand}. NodeIds are rendered in their parseable
	 * form ({@code ns=2;s=pump-1}) because {@code NodeId.toString()} is a debug
	 * representation that cannot be parsed back.
	 *
	 * @param value the value of the input variant, may be null
	 * @return the string value, or null
	 */
	public static String inputToString(final Object value) {
		if (value == null) {
			return null;
		} else if (value instanceof NodeId) {
			return ((NodeId) value).toParseableString();
		} else if (value instanceof ExpandedNodeId) {
			return ((ExpandedNodeId) value).toParseableString();
		} else if (value instanceof LocalizedText) {
			return ((LocalizedText) value).text();
		} else if (value instanceof QualifiedName) {
			return ((QualifiedName) value).name();
		} else {
			return value.toString();
		}
	}

	@Override
	protected Variant[] invoke(final InvocationContext invocationContext, final Variant[] inputValues)
			throws UaException {
		final String[] inputStrings = new String[inputValues.length];
		for (int i = 0; i < inputValues.length; i++) {
			inputStrings[i] = inputToString(inputValues[i].getValue());
		}
		final Object[] result = ((WaldotCommand) super.getNode()).runCommand(invocationContext, inputStrings);
		final Variant[] output = new Variant[result.length];
		for (int i = 0; i < result.length; i++) {
			output[i] = new Variant(result[i]);
		}
		return output;
	}

}
