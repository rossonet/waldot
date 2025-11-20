package net.rossonet.zenoh.client.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import net.rossonet.waldot.dtdl.CommandObject;
import net.rossonet.waldot.dtdl.CommandPayload;
import net.rossonet.waldot.dtdl.DigitalTwinModelIdentifier;
import net.rossonet.waldot.dtdl.DtdlHandler;
import net.rossonet.waldot.dtdl.Schema;
import net.rossonet.zenoh.annotation.AgentController;
import net.rossonet.zenoh.annotation.ExportedCommand;

public class AgentCommand {

	public static Map<String, AgentCommand> fromDtml(DtdlHandler dtmlHandler) {
		final Map<String, AgentCommand> commands = new HashMap<>();
		for (final CommandObject commandObject : dtmlHandler.getCommands()) {
			final Set<AgentCommandParameter> parameters = new HashSet<>();
			final CommandPayload request = commandObject.getRequest();
			if (request != null) {
				final JSONArray paramsArray = request.getSchema().toSchemaJson().getJSONArray("parameters");
				for (int i = 0; i < paramsArray.length(); i++) {
					final JSONObject paramObject = paramsArray.getJSONObject(i);
					final AgentCommandParameter parameter = AgentCommandParameter.fromJsonObject(paramObject);
					parameters.add(parameter);
				}
			}
			final AgentCommand agentCommand = new AgentCommand(commandObject.getName(), null, "",
					commandObject.getResponse().getSchema().toSchemaString(), new ExportedCommand() {
						@Override
						public Class<? extends java.lang.annotation.Annotation> annotationType() {
							return ExportedCommand.class;
						}

						@Override
						public String description() {
							return commandObject.getDescription();
						}

						@Override
						public String name() {
							return commandObject.getName();
						}

						@Override
						public String returnDescription() {
							return commandObject.getResponse().getDescription();
						}

						@Override
						public String returnName() {
							return commandObject.getResponse().getName();
						}
					}, parameters);
			commands.put(commandObject.getName(), agentCommand);
		}
		return commands;
	}

	private final AgentController agentControl;
	private final ExportedCommand annotation;
	private final String commandName;
	private final Set<AgentCommandParameter> commandParameters;

	private final String methodName;
	private final String methodReturnType;

	public AgentCommand(String commandName, AgentController agentControl, String methodName, String methodReturnType,
			ExportedCommand annotation, Set<AgentCommandParameter> commandParameters) {
		this.commandName = commandName;
		this.agentControl = agentControl;
		this.methodName = methodName;
		this.annotation = annotation;
		this.commandParameters = commandParameters;
		this.methodReturnType = methodReturnType;
	}

	public void executeCommand(JSONObject message) {
		// TODO Auto-generated method stub
	}

	public CommandObject generateDtmlCommandObject() {
		final CommandObject commandObject = new CommandObject();
		commandObject.setName(commandName);
		commandObject.setDescription(annotation.description());
		commandObject.setDisplayName(annotation.name());
		commandObject.setComment(annotation.returnDescription());
		final CommandPayload response = new CommandPayload();
		response.setDescription(annotation.returnDescription());
		response.setName(annotation.returnName());
		response.setSchema(new Schema(methodReturnType.toLowerCase()));
		final String idResponse = "response:" + commandName + ";1";
		final DigitalTwinModelIdentifier idr = DigitalTwinModelIdentifier.fromString(idResponse);
		response.setId(idr);
		commandObject.setResponse(response);
		final CommandPayload request;
		if (commandParameters.isEmpty()) {
			request = null;
		} else {
			request = new CommandPayload();
			request.setDescription("Command parameters");
			request.setName("parameters");
			final JSONArray parameters = new JSONArray();
			for (final AgentCommandParameter agentCommandParameter : commandParameters) {
				parameters.put(new JSONObject(agentCommandParameter.toMap()));
			}
			final JSONObject schemaObject = new JSONObject();
			schemaObject.put("parameters", parameters);
			request.setSchema(new Schema(schemaObject));
			final String idRequest = "request:" + commandName + ";1";
			final DigitalTwinModelIdentifier idreq = DigitalTwinModelIdentifier.fromString(idRequest);
			request.setId(idreq);
			commandObject.setRequest(request);
		}
		final String idCommand = "command:" + commandName.replace(" ", "_") + ";1";
		final DigitalTwinModelIdentifier id = DigitalTwinModelIdentifier.fromString(idCommand);
		commandObject.setId(id);
		return commandObject;
	}

}
