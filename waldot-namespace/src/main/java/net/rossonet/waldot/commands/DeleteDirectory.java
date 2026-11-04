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
				waldotNamespace.getConfiguration()
						.getDeleteDirectoryDescription(),
				waldotNamespace.getConfiguration()
						.getDeleteDirectoryWriteMask(),
				waldotNamespace.getConfiguration()
						.getDeleteDirectoryUserWriteMask(),
				waldotNamespace.getConfiguration()
						.getDeleteDirectoryExecutable(),
				waldotNamespace.getConfiguration()
						.getDeleteDirectoryExecutable());
		super.addOutputArgument("output", VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null,
				LocalizedText.english("operation output"));
		super.addOutputArgument("error", VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null,
				LocalizedText.english("operation error"));
		super.addOutputArgument("removed", VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(
						"number of vertices and edges removed (see 'directory content' for the list before deleting)"));
		super.addInputArgument("directory",
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("directory to delete"));
		this.addReference(
				new Reference(this.getNodeId(), NodeIds.HasModellingRule,
						NodeIds.ModellingRule_Mandatory.expanded(), true));
	}

	@Override
	public Object clone() {
		return new DeleteDirectory(this.waldotNamespace);
	}

	@Override
	public String getDirectory() {
		return MiloStrategy.MANAGE_CMD_DIRECTORY;
	}

	@Override
	public String[] runCommand(InvocationContext invocationContext,
			String[] inputValues) {
		final String[] output = new String[3];
		output[2] = "";
		try {
			final String directory = CommandArguments.requireFirst(inputValues, "directory");
			output[2] = String.valueOf(DirectoryContentCommand.content(getNamespace(), directory).get("count"));
			final Object runExpression = getNamespace().deleteDirectory(directory);
			if (runExpression != null) {
				output[0] = runExpression.toString();
				output[1] = "";
			} else {
				output[0] = "";
				output[1] = "empty result";
			}
		} catch (final Exception e) {
			output[0] = "";
			output[1] = CommandArguments.errorMessage(e);
		}
		return output;
	}

}
