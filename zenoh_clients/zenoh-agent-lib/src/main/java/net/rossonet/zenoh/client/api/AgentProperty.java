package net.rossonet.zenoh.client.api;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import net.rossonet.waldot.dtdl.DigitalTwinModelIdentifier;
import net.rossonet.waldot.dtdl.DtdlHandler;
import net.rossonet.waldot.dtdl.PropertyObject;
import net.rossonet.waldot.dtdl.Schema;
import net.rossonet.waldot.dtdl.Unit;
import net.rossonet.zenoh.annotation.AnnotatedAgentController;
import net.rossonet.zenoh.annotation.ExportedParameter;

public class AgentProperty implements Serializable {

	private static final long serialVersionUID = 6733808400358705107L;

	public static Map<String, AgentProperty> fromDtml(DtdlHandler dtmlHandler) {
		final Map<String, AgentProperty> properties = new HashMap<>();
		for (final PropertyObject prop : dtmlHandler.getProperties()) {
			final Schema schema = prop.getSchema();
			if (!schema.isJson()) {
				throw new IllegalArgumentException(
						"Only JSON schema is supported for AgentProperty, property: " + prop.getName());
			}
			final JSONObject schemaJson = schema.toSchemaJson();
			final ExportedParameter annotation = new ExportedParameter() {

				@Override
				public boolean advancedConfigurationField() {
					return schemaJson.getBoolean("advancedConfigurationField");
				}

				@Override
				public Class<? extends Annotation> annotationType() {
					return ExportedParameter.class;
				}

				@Override
				public String defaultValue() {
					return schemaJson.getString("defaultValue");
				}

				@Override
				public String description() {
					return prop.getDescription();
				}

				@Override
				public String fieldValidationRegEx() {
					return schemaJson.getString("fieldValidationRegEx");
				}

				@Override
				public boolean isArray() {
					return schemaJson.getBoolean("isArray");
				}

				@Override
				public boolean mandatary() {
					return schemaJson.getBoolean("mandatary");
				}

				@Override
				public String mimeType() {
					return schemaJson.getString("mimeType");
				}

				@Override
				public String name() {
					return prop.getName();
				}

				@Override
				public String[] parameters() {
					return schemaJson.getJSONArray("parameters").toList().stream().map(Object::toString)
							.toArray(String[]::new);
				}

				@Override
				public String permissions() {
					return schemaJson.getString("permissions");
				}

				@Override
				public boolean textArea() {
					return schemaJson.getBoolean("textArea");
				}

				@Override
				public String unit() {
					return schemaJson.getString("unit");
				}

				@Override
				public int viewOrder() {
					return schemaJson.getInt("viewOrder");
				}

				@Override
				public boolean writable() {
					return prop.isWritable();
				}

			};
			properties.put(prop.getName(), new AgentProperty(prop.getName(), null, null, annotation));
		}
		return properties;
	}

	private final AnnotatedAgentController agentController;
	private final ExportedParameter annotation;

	private final String fieldName;
	private final String propertyName;

	public AgentProperty(String propertyName, AnnotatedAgentController agentController, String fieldName,
			ExportedParameter annotation) {
		this.propertyName = propertyName;
		this.agentController = agentController;
		this.fieldName = fieldName;
		this.annotation = annotation;
	}

	public PropertyObject generateDtmlPropertyObject() {
		final PropertyObject propertyObject = new PropertyObject();
		propertyObject.setName(propertyName);
		final Map<String, Object> map = new HashMap<>();
		map.put("advancedConfigurationField", annotation.advancedConfigurationField());
		map.put("defaultValue", annotation.defaultValue());
		map.put("fieldValidationRegEx", annotation.fieldValidationRegEx());
		map.put("isArray", annotation.isArray());
		map.put("mandatary", annotation.mandatary());
		map.put("parameters", annotation.parameters());
		map.put("permissions", annotation.permissions());
		map.put("textArea", annotation.textArea());
		map.put("viewOrder", annotation.viewOrder());
		map.put("mimeType", annotation.mimeType());
		propertyObject.setDescription(annotation.description());
		propertyObject.setDisplayName(propertyName);
		propertyObject.setSchema(new Schema(new JSONObject(map)));
		propertyObject.setWritable(annotation.writable());
		propertyObject.setUnit(Unit.getUnit(annotation.unit()));
		final String idString = "property:" + propertyName.replace(" ", "_") + ";1";
		final DigitalTwinModelIdentifier id = DigitalTwinModelIdentifier.fromString(idString);
		propertyObject.setId(id);
		propertyObject.setComment("java field name: " + fieldName);
		return propertyObject;
	}

	public AnnotatedAgentController getAgentController() {
		return agentController;
	}

	public ExportedParameter getAnnotation() {
		return annotation;
	}

	public String getName() {
		return propertyName;
	}

	public String getPropertyName() {
		return propertyName;
	}

}
