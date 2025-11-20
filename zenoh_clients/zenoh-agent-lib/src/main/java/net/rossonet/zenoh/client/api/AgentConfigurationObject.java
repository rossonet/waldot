package net.rossonet.zenoh.client.api;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import net.rossonet.waldot.dtdl.DigitalTwinModelIdentifier;
import net.rossonet.waldot.dtdl.DtdlHandler;
import net.rossonet.waldot.dtdl.RelationshipObject;
import net.rossonet.waldot.dtdl.Schema;
import net.rossonet.zenoh.annotation.ExportedParameter;

public class AgentConfigurationObject {

	public static Map<String, AgentConfigurationObject> fromDtml(DtdlHandler dtmlHandler) {
		final Map<String, AgentConfigurationObject> configurations = new HashMap<>();
		for (final RelationshipObject configuration : dtmlHandler.getRelationships()) {
			final Map<String, AgentProperty> props = new HashMap<>();
			for (final var propertyObject : configuration.getProperties()) {
				final Schema schema = propertyObject.getSchema();
				if (!schema.isJson()) {
					throw new IllegalArgumentException("Only JSON schema is supported for configuration properties");
				}
				final JSONObject jsonSchema = schema.toSchemaJson();
				final ExportedParameter annotation = new ExportedParameter() {

					@Override
					public boolean advancedConfigurationField() {
						return jsonSchema.getBoolean("advancedConfigurationField");
					}

					@Override
					public Class<? extends Annotation> annotationType() {
						return ExportedParameter.class;
					}

					@Override
					public String defaultValue() {
						return jsonSchema.getString("defaultValue");
					}

					@Override
					public String description() {
						return propertyObject.getDescription();
					}

					@Override
					public String fieldValidationRegEx() {
						return jsonSchema.getString("fieldValidationRegEx");
					}

					@Override
					public boolean isArray() {
						return jsonSchema.getBoolean("isArray");
					}

					@Override
					public boolean mandatary() {
						return jsonSchema.getBoolean("mandatary");
					}

					@Override
					public String mimeType() {
						return jsonSchema.getString("mimeType");
					}

					@Override
					public String name() {
						return propertyObject.getName();
					}

					@Override
					public String[] parameters() {
						return jsonSchema.getJSONArray("parameters").toList().stream().map(Object::toString)
								.toArray(String[]::new);
					}

					@Override
					public String permissions() {
						return jsonSchema.getString("permissions");
					}

					@Override
					public boolean textArea() {
						return jsonSchema.getBoolean("textArea");
					}

					@Override
					public String unit() {
						return propertyObject.getUnit().toString();
					}

					@Override
					public int viewOrder() {
						return jsonSchema.getInt("viewOrder");
					}

					@Override
					public boolean writable() {
						return propertyObject.isWritable();
					}
				};
				final AgentProperty agentProperty = new AgentProperty(propertyObject.getName(), null, null, annotation);

				props.put(agentProperty.getName(), agentProperty);
			}
			final AgentConfigurationObject configurationObject = new AgentConfigurationObject(configuration.getName(),
					configuration.getTarget(), configuration.getDescription(), props,
					Boolean.valueOf(configuration.getComment()));
			configurations.put(configurationObject.configurationName, configurationObject);
		}
		return configurations;
	}

	private final String configurationClassName;
	private final String configurationName;
	private final String description;
	private final Map<String, AgentProperty> properties;
	private final boolean unique;

	public AgentConfigurationObject(String configurationName, String configurationClassName, String description,
			Map<String, AgentProperty> properties, boolean unique) {
		this.configurationName = configurationName;
		this.configurationClassName = configurationClassName;
		this.description = description;
		this.properties = properties;
		this.unique = unique;
	}

	public RelationshipObject generateDtmlRelationshipObject() {
		final RelationshipObject relationshipObject = new RelationshipObject();
		relationshipObject.setName(configurationName);
		relationshipObject.setDescription(description);
		relationshipObject.setDisplayName(configurationName);
		relationshipObject.setTarget(configurationClassName);
		relationshipObject.setWritable(true);
		relationshipObject.setComment(Boolean.toString(unique));
		for (final AgentProperty agentProperty : properties.values()) {
			relationshipObject.getProperties().add(agentProperty.generateDtmlPropertyObject());
		}
		final String idString = "configuration:" + configurationName.replace(" ", "_") + ";1";
		final DigitalTwinModelIdentifier id = DigitalTwinModelIdentifier.fromString(idString);
		relationshipObject.setId(id);
		return relationshipObject;
	}

	public String getConfigurationClassName() {
		return configurationClassName;
	}

	public String getConfigurationName() {
		return configurationName;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, AgentProperty> getProperties() {
		return properties;
	}

	public boolean isUnique() {
		return unique;
	}
}
