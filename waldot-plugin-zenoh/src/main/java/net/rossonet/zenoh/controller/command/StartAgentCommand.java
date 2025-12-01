package net.rossonet.zenoh.controller.command;

import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.controller.ZenohAgent;

public class StartAgentCommand extends AbstractOpcCommand {

	private final ZenohAgent zenohAgent;

	public StartAgentCommand(ZenohAgent zenohAgent) {
		super(zenohAgent.getGremlinGraph(), zenohAgent.getWaldotNamespace(), zenohAgent.getUniqueId() + ".start",
				"Start data flow", "start data flow of the agent " + zenohAgent.getUniqueId(),
				"agents/" + zenohAgent.getUniqueId(), UInteger.valueOf(WriteMask.Executable.getValue()),
				UInteger.valueOf(WriteMask.Executable.getValue()), true, true);
		this.zenohAgent = zenohAgent;
	}

	@Override
	public Object[] runCommand(InvocationContext invocationContext, String[] inputValues) {
		return zenohAgent.sendCommandToAgent(ZenohHelper.FLOW_START_COMMAND_TOPIC, inputValues);
	}

}
