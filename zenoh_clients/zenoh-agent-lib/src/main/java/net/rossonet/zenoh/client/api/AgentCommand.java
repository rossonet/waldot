package net.rossonet.zenoh.client.api;

import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import net.rossonet.waldot.agent.digitalTwin.DtdlHandler;
import net.rossonet.waldot.dtdl.CommandObject;
import net.rossonet.zenoh.annotation.AgentController;
import net.rossonet.zenoh.annotation.ExportedCommand;

public class AgentCommand {

	public static Map<String, AgentCommand> fromDtml(DtdlHandler dtmlHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	private final AgentController agentControl;
	private final ExportedCommand annotation;
	private final String commandName;
	private final Set<AgentCommandParameter> commandParameters;

	private final String methodName;

	public AgentCommand(String commandName, AgentController agentControl, String methodName, ExportedCommand annotation,
			Set<AgentCommandParameter> commandParameters) {
		this.commandName = commandName;
		this.agentControl = agentControl;
		this.methodName = methodName;
		this.annotation = annotation;
		this.commandParameters = commandParameters;
	}

	public void executeCommand(JSONObject message) {
		// TODO Auto-generated method stub
	}

	public CommandObject generateDtmlCommandObject() {
		// TODO Auto-generated method stub
		return null;
	}

}
