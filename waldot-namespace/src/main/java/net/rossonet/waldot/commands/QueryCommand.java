package net.rossonet.waldot.commands;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractOpcCommand;

public class QueryCommand extends AbstractOpcCommand {

	public QueryCommand(WaldotNamespace waldotNamespace) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				waldotNamespace.getConfiguration().getWaldotCommandLabel(),
				waldotNamespace.getConfiguration().getWaldotCommandDescription(),
				waldotNamespace.getConfiguration().getWaldotCommandWriteMask(),
				waldotNamespace.getConfiguration().getWaldotCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getWaldotCommandExecutable(),
				waldotNamespace.getConfiguration().getWaldotCommandUserExecutable());
		super.addOutputArgument("output", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command output"));
		super.addOutputArgument("error", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command error"));
		super.addInputArgument("command", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command to execute"));
		this.addReference(new Reference(this.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
	}

	@Override
	public Object clone() {
		return new QueryCommand(this.waldotNamespace);
	}

	@Override
	public String[] runCommand(InvocationContext invocationContext, String[] inputValues) {
		final String[] output = new String[2];
		try {
			final Object runExpression = getNamespace().runExpression(inputValues[0]);
			if (runExpression != null) {
				output[0] = runExpression.toString();
				output[1] = "";
			} else {
				output[0] = "";
				output[1] = "empty result";
			}
		} catch (final Exception e) {
			output[0] = "";
			output[1] = e.getMessage();
		}
		return output;
	}

}
