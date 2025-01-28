package net.rossonet.waldot.commands;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.opc.AbstractWaldotCommand;

public class AboutCommand extends AbstractWaldotCommand {

	public AboutCommand(HomunculusNamespace waldotNamespace) {
		super(waldotNamespace, waldotNamespace.getConfiguration().getAboutCommandLabel(),
				waldotNamespace.getConfiguration().getAboutCommandDescription(),
				waldotNamespace.getConfiguration().getAboutCommandWriteMask(),
				waldotNamespace.getConfiguration().getAboutCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getAboutCommandExecutable(),
				waldotNamespace.getConfiguration().getAboutCommandUserExecutable());
		super.addOutputArgument("creator info", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("info about the creator of WaldOT"));
		this.addReference(new Reference(this.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));

	}

	@Override
	public String[] runCommand(InvocationContext invocationContext, String[] inputValues) {
		// TODO completare about command
		return new String[] { "result of about command" };
	}

}
