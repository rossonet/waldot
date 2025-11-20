package net.rossonet.zenoh.client.api;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import net.rossonet.waldot.agent.digitalTwin.DtdlHandler;
import net.rossonet.waldot.dtdl.DigitalTwinModelIdentifier;
import net.rossonet.waldot.dtdl.PropertyObject;
import net.rossonet.waldot.dtdl.Schema;
import net.rossonet.waldot.dtdl.Unit;
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

}
