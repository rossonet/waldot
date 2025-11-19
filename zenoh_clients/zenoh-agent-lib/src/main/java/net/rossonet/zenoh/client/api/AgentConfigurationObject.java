package net.rossonet.zenoh.client.api;

import java.util.Map;

import net.rossonet.waldot.agent.digitalTwin.DtdlHandler;
import net.rossonet.waldot.dtdl.RelationshipObject;

public class AgentConfigurationObject {

	public static Map<String, AgentConfigurationObject> fromDtml(DtdlHandler dtmlHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	private final String configurationClassName;
	private final String configuredName;
	private final String description;
	private final RelationshipObject dtmlComponentObject;
	private final Map<String, AgentProperty> properties;
	private final boolean unique;

	public AgentConfigurationObject(String configuredName, String configurationClassName, String description,
			Map<String, AgentProperty> properties, boolean unique) {
		this.configuredName = configuredName;
		this.configurationClassName = configurationClassName;
		this.description = description;
		this.properties = properties;
		this.unique = unique;
		dtmlComponentObject = generateDtmlComponentObject();
	}

	private RelationshipObject generateDtmlComponentObject() {
		// TODO Auto-generated method stub
		return null;
	}
}
