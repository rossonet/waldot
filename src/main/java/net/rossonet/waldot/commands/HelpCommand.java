package net.rossonet.waldot.commands;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.opc.AbstractWaldotCommand;

public class HelpCommand extends AbstractWaldotCommand {

	public HelpCommand(HomunculusNamespace waldotNamespace) {
		super(waldotNamespace, waldotNamespace.getConfiguration().getHelpCommandLabel(),
				waldotNamespace.getConfiguration().getHelpCommandDescription(),
				waldotNamespace.getConfiguration().getHelpCommandWriteMask(),
				waldotNamespace.getConfiguration().getHelpCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getHelpCommandExecutable(),
				waldotNamespace.getConfiguration().getHelpCommandUserExecutable());
		super.addOutputArgument("output", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("help message"));
		super.addInputArgument("context", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("help context to search"));
		this.addReference(new Reference(this.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
	}

	@Override
	public String[] runCommand(InvocationContext invocationContext, String[] inputValues) {
		// TODO completare help
		return new String[] { "result of help command" };
	}

}
