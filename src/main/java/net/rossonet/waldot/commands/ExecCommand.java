package net.rossonet.waldot.commands;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.opc.AbstractWaldotCommand;

public class ExecCommand extends AbstractWaldotCommand {

	public ExecCommand(HomunculusNamespace waldotNamespace) {
		super(waldotNamespace, waldotNamespace.getConfiguration().getExecCommandLabel(),
				waldotNamespace.getConfiguration().getExecCommandDescription(),
				waldotNamespace.getConfiguration().getExecCommandWriteMask(),
				waldotNamespace.getConfiguration().getExecCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getExecCommandExecutable(),
				waldotNamespace.getConfiguration().getExecCommandUserExecutable());
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
	public String[] runCommand(InvocationContext invocationContext, String[] inputValues) {
		// TODO eseguire un comando su sistema operativo
		return new String[] { "result of exec command" };
	}

}
