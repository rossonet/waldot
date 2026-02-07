package net.rossonet.waldot.commands;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcCommand;

public class DeleteDirectory extends AbstractOpcCommand {

	public DeleteDirectory(WaldotNamespace waldotNamespace) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				waldotNamespace.getConfiguration().getDeleteDirectoryLabel(),
				waldotNamespace.getConfiguration().getDeleteDirectoryDescription(),
				waldotNamespace.getConfiguration().getDeleteDirectoryWriteMask(),
				waldotNamespace.getConfiguration().getDeleteDirectoryUserWriteMask(),
				waldotNamespace.getConfiguration().getDeleteDirectoryExecutable(),
				waldotNamespace.getConfiguration().getDeleteDirectoryExecutable());
		super.addOutputArgument("output", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("operation output"));
		super.addOutputArgument("error", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("operation error"));
		super.addInputArgument("directory", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("directory to delete"));
		this.addReference(new Reference(this.getNodeId(), NodeIds.HasModellingRule,
				NodeIds.ModellingRule_Mandatory.expanded(), true));
	}

	@Override
	public Object clone() {
		return new DeleteDirectory(this.waldotNamespace);
	}

	@Override
	public String getDirectory() {
		return MiloStrategy.GENERAL_CMD_DIRECTORY;
	}

	@Override
	public String[] runCommand(InvocationContext invocationContext, String[] inputValues) {
		final String[] output = new String[2];
		try {
			final Object runExpression = getNamespace().deleteDirectory(inputValues[0]);
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
