package net.rossonet.waldot.api.models.base;

import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

import net.rossonet.waldot.api.models.WaldotCommand;

/**
 * CommandInvocationHandler is a class that extends
 * AbstractMethodInvocationHandler and implements the invocation logic for
 * WaldotCommand methods. It handles the input and output arguments, invoking
 * the command with the provided input values.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class CommandInvocationHandler extends AbstractMethodInvocationHandler {

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

	@Override
	protected Variant[] invoke(final InvocationContext invocationContext, final Variant[] inputValues)
			throws UaException {
		final String[] inputStrings = new String[inputValues.length];
		for (int i = 0; i < inputValues.length; i++) {
			if (inputValues[i].getValue() != null) {
				inputStrings[i] = inputValues[i].getValue().toString();
			}
		}
		final Object[] result = ((WaldotCommand) super.getNode()).runCommand(invocationContext, inputStrings);
		final Variant[] output = new Variant[result.length];
		for (int i = 0; i < result.length; i++) {
			output[i] = new Variant(result[i]);
		}
		return output;
	}

}
