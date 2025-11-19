package net.rossonet.zenoh.client.api;

import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;

import net.rossonet.waldot.agent.digitalTwin.DtdlHandler;
import net.rossonet.waldot.dtdl.CommandPayload;
import net.rossonet.zenoh.annotation.ExportedMethodParameter;

public class AgentCommandParameter {

	public static Map<String, AgentCommandParameter> fromDtml(DtdlHandler dtmlHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	public static CommandPayload generateDtmlCommandPayload(Collection<AgentCommandParameter> commandParameters) {
		// TODO Auto-generated method stub
		return null;
	}

	private final ExportedMethodParameter methodParamAnnotation;
	private final Parameter methodParameter;

	private final String parameterName;

	public AgentCommandParameter(String parameterName, Parameter methodParameter,
			ExportedMethodParameter methodParamAnnotation) {
		this.parameterName = parameterName;
		this.methodParameter = methodParameter;
		this.methodParamAnnotation = methodParamAnnotation;
	}

}
