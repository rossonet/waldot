package net.rossonet.waldot.commands;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

import net.rossonet.waldot.namespaces.HomunculusNamespace;
import net.rossonet.waldot.opc.AbstractOpcCommand;

public class VersionCommand extends AbstractOpcCommand {

	public VersionCommand(HomunculusNamespace waldotNamespace) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				waldotNamespace.getConfiguration().getVersionCommandLabel(),
				waldotNamespace.getConfiguration().getVersionCommandDescription(),
				waldotNamespace.getConfiguration().getVersionCommandWriteMask(),
				waldotNamespace.getConfiguration().getVersionCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getVersionCommandExecutable(),
				waldotNamespace.getConfiguration().getVersionCommandUserExecutable());
		super.addOutputArgument("version", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("official version of the software"));
		super.addOutputArgument("git version", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("build version of the software"));
		this.addReference(new Reference(this.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
	}

	@Override
	public String[] runCommand(InvocationContext invocationContext, String[] inputValues) {
		// TODO ritornare il file di versione
		return new String[] { "result of version command" };
	}

}
