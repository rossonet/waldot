package net.rossonet.zenoh.client.api;

import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import net.rossonet.waldot.agent.digitalTwin.DtdlHandler;
import net.rossonet.zenoh.annotation.ExportedMethodParameter;

public class AgentCommandParameter {

	public static Map<String, AgentCommandParameter> fromDtml(DtdlHandler dtmlHandler) {
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

	public Map<String, Object> toMap() {
		final Map<String, Object> map = new HashMap<>();
		final String idString = "command.parameter:" + parameterName + ";1";
		map.put("@id", idString);
		map.put("@type", "String");
		map.put("name", parameterName);
		map.put("advancedConfigurationField", methodParamAnnotation.advancedConfigurationField());
		map.put("defaultValue", methodParamAnnotation.defaultValue());
		map.put("description", methodParamAnnotation.description());
		map.put("fieldValidationRegEx", methodParamAnnotation.fieldValidationRegEx());
		map.put("isArray", methodParamAnnotation.isArray());
		map.put("mandatary", methodParamAnnotation.mandatary());
		map.put("mimeType", methodParamAnnotation.mimeType());
		map.put("unit", methodParamAnnotation.unit());
		map.put("parameters", methodParamAnnotation.parameters());
		map.put("permissions", methodParamAnnotation.permissions());
		map.put("textArea", methodParamAnnotation.textArea());
		map.put("viewOrder", methodParamAnnotation.viewOrder());
		map.put("writable", methodParamAnnotation.writable());
		return map;
	}

}
