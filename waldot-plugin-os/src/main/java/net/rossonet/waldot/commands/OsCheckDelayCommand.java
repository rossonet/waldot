package net.rossonet.waldot.commands;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.WaldotOsPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractOpcCommand;

public class OsCheckDelayCommand extends AbstractOpcCommand {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final WaldotOsPlugin waldotOsPlugin;

	public OsCheckDelayCommand(final WaldotNamespace waldotNamespace, WaldotOsPlugin waldotOsPlugin) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				waldotNamespace.getConfiguration().getOsCheckDelayCommandLabel(),
				waldotNamespace.getConfiguration().getOsCheckDelayCommandDescription(),
				waldotNamespace.getConfiguration().getOsCheckDelayCommandWriteMask(),
				waldotNamespace.getConfiguration().getOsCheckDelayCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getOsCheckDelayCommandExecutable(),
				waldotNamespace.getConfiguration().getOsCheckDelayCommandUserExecutable());
		this.waldotOsPlugin = waldotOsPlugin;
		super.addOutputArgument("delay", VariableNodeTypes.UInt32.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("delay between os data updates in milliseconds"));
		super.addInputArgument("new_delay", VariableNodeTypes.UInt32.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("new delay in milliseconds"));
		this.addReference(new Reference(this.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
	}

	@Override
	public Object clone() {
		return new OsCheckDelayCommand(this.waldotNamespace, waldotOsPlugin);
	}

	@Override
	public Long[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		try {
			long timeout = WaldotOsPlugin.DEFAULT_UPDATE_DELAY;
			for (int i = 0; i < inputValues.length; i++) {
				final String label = getInputArguments()[i].getName();
				if (inputValues[i] == null || inputValues[i].isEmpty()) {
					continue;
				}
				if (label.equals("new_delay") && inputValues[i] != null && !inputValues[i].isEmpty()) {
					timeout = Long.parseLong(inputValues[i]);
					waldotOsPlugin.setUpdateDelay(timeout);
				}
			}

			return new Long[] { waldotOsPlugin.getUpdateDelay() };
		} catch (final Exception e) {
			logger.error("Error executing command", e);
			return new Long[] { null, null };
		}
	}

}
