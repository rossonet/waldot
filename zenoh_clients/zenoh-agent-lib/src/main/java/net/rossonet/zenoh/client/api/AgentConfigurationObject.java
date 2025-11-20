package net.rossonet.zenoh.client.api;

import java.util.Map;

import net.rossonet.waldot.agent.digitalTwin.DtdlHandler;
import net.rossonet.waldot.dtdl.DigitalTwinModelIdentifier;
import net.rossonet.waldot.dtdl.RelationshipObject;

public class AgentConfigurationObject {

	public static Map<String, AgentConfigurationObject> fromDtml(DtdlHandler dtmlHandler) {
		// TODO Auto-generated method stub
		return null;
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
		relationshipObject.setComment("configuration class name: " + configurationClassName + ", unique: " + unique);
		for (final AgentProperty agentProperty : properties.values()) {
			relationshipObject.getProperties().add(agentProperty.generateDtmlPropertyObject());
		}
		final String idString = "configuration:" + configurationName.replace(" ", "_") + ";1";
		final DigitalTwinModelIdentifier id = DigitalTwinModelIdentifier.fromString(idString);
		relationshipObject.setId(id);
		return relationshipObject;
	}
}
