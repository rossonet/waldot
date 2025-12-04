package net.rossonet.zenoh.client.api;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import net.rossonet.zenoh.annotation.ExportedMethodParameter;

public class AgentCommandParameter implements Serializable {

	private static final long serialVersionUID = 6972239985896868568L;

	public static AgentCommandParameter fromJsonObject(JSONObject paramObject) {
		final String parameterNameCandidate = paramObject.getString("name");
		final ExportedMethodParameter methodParamAnnotationCandidate = new ExportedMethodParameter() {

			@Override
			public boolean advancedConfigurationField() {
				return paramObject.getBoolean("advancedConfigurationField");
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return ExportedMethodParameter.class;
			}

			@Override
			public String defaultValue() {
				return paramObject.getString("defaultValue");
			}

			@Override
			public String description() {
				return paramObject.getString("description");
			}

			@Override
			public String fieldValidationRegEx() {
				return paramObject.getString("fieldValidationRegEx");
			}

			@Override
			public boolean isArray() {
				return paramObject.getBoolean("isArray");
			}

			@Override
			public boolean mandatary() {
				return paramObject.getBoolean("mandatary");
			}

			@Override
			public String mimeType() {
				return paramObject.getString("mimeType");
			}

			@Override
			public String name() {
				return parameterNameCandidate;
			}

			@Override
			public String[] parameters() {
				return paramObject.getJSONArray("parameters").toList().stream().map(Object::toString)
						.toArray(String[]::new);
			}

			@Override
			public String permissions() {
				return paramObject.getString("permissions");
			}

			@Override
			public boolean textArea() {
				return paramObject.getBoolean("textArea");
			}

			@Override
			public String unit() {
				return paramObject.getString("unit");
			}

			@Override
			public int viewOrder() {
				return paramObject.getInt("viewOrder");
			}

			@Override
			public boolean writable() {
				return paramObject.getBoolean("writable");
			}

		};
		return new AgentCommandParameter(parameterNameCandidate, null, methodParamAnnotationCandidate);
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

	public Parameter getMethodParameter() {
		return methodParameter;
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
