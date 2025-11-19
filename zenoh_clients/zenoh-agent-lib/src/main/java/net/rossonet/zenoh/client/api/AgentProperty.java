package net.rossonet.zenoh.client.api;

import java.util.Map;

import net.rossonet.waldot.agent.digitalTwin.DtdlHandler;
import net.rossonet.waldot.dtdl.PropertyObject;
import net.rossonet.zenoh.annotation.AgentController;
import net.rossonet.zenoh.annotation.ExportedParameter;

public class AgentProperty {

	public static Map<String, AgentProperty> fromDtml(DtdlHandler dtmlHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	private final AgentController agentController;
	private final ExportedParameter annotation;
	private final String fieldName;
	private final String propertyName;

	public AgentProperty(String propertyName, AgentController agentController, String fieldName,
			ExportedParameter annotation) {
		this.propertyName = propertyName;
		this.agentController = agentController;
		this.fieldName = fieldName;
		this.annotation = annotation;
	}

	public PropertyObject generateDtmlPropertyObject() {
		// TODO Auto-generated method stub
		return null;
	}

}
