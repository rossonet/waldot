package net.rossonet.zenoh.controller.command;

import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.controller.ZenohAgent;

public class StopAgentCommand extends AbstractOpcCommand {

	private final ZenohAgent zenohAgent;

	public StopAgentCommand(ZenohAgent zenohAgent) {
		super(zenohAgent.getGremlinGraph(), zenohAgent.getWaldotNamespace(), zenohAgent.getUniqueId() + ".stop",
				"Stop data flow", "stop data flow of the agent " + zenohAgent.getUniqueId(),
				"agents/" + zenohAgent.getUniqueId(), UInteger.valueOf(WriteMask.Executable.getValue()),
				UInteger.valueOf(WriteMask.Executable.getValue()), true, true);
		this.zenohAgent = zenohAgent;
	}

	@Override
	public Object[] runCommand(InvocationContext invocationContext, String[] inputValues) {
		try {
			return zenohAgent.elaborateRemoteCommandOnAgent(ZenohHelper.FLOW_STOP_COMMAND_TOPIC, inputValues).get()
					.getOutputValues();
		} catch (InterruptedException | ExecutionException e) {
			return new SuspendedCommand(zenohAgent.getUniqueId(), ZenohHelper.FLOW_STOP_COMMAND_TOPIC, inputValues, e)
					.getOutputValues();
		}

	}

}
