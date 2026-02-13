package net.rossonet.zenoh.rpc.controller.command;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.rpc.controller.CommandLifecycleRegister;
import net.rossonet.zenoh.rpc.controller.ZenohAgent;

public class StartAgentCommand extends AbstractOpcCommand {

	private final ZenohAgent zenohAgent;

	public StartAgentCommand(ZenohAgent zenohAgent) {
		super(zenohAgent.getGremlinGraph(), zenohAgent.getWaldotNamespace(), zenohAgent.getUniqueId() + ".start",
				"Start data flow", "start data flow of the agent " + zenohAgent.getUniqueId(),
				"agents/" + zenohAgent.getUniqueId(), UInteger.valueOf(WriteMask.Executable.getValue()),
				UInteger.valueOf(WriteMask.Executable.getValue()), true, true);
		this.zenohAgent = zenohAgent;
		CommandLifecycleRegister.addStandardAgentCommandOutputArguments(this);
	}

	@Override
	public Object[] runCommand(InvocationContext invocationContext, String[] inputValues) {
		try {
			return zenohAgent
					.elaborateRemoteCommandOnAgent(ZenohHelper.FLOW_START_COMMAND_TOPIC, List.of(), inputValues).get()
					.getOutputValues();
		} catch (InterruptedException | ExecutionException e) {
			return new CommandLifecycleRegister(zenohAgent.getUniqueId(), ZenohHelper.FLOW_START_COMMAND_TOPIC,
					inputValues, e).getOutputValues();
		}
	}

}
