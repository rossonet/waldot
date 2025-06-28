package net.rossonet.waldot.agent.digitalTwin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.utils.JsonUtils;

import net.rossonet.waldot.dtdl.CommandObject;
import net.rossonet.waldot.dtdl.ComponentObject;
import net.rossonet.waldot.dtdl.DigitalTwinModelIdentifier;
import net.rossonet.waldot.dtdl.PropertyObject;
import net.rossonet.waldot.dtdl.RelationshipObject;
import net.rossonet.waldot.dtdl.TelemetryObject;

/**
 * The {@code DtdlHandler} class is responsible for parsing and handling Digital
 * Twin Definition Language (DTDL) version 2 models. DTDL is a JSON-LD-based
 * language used to describe digital twins, including their telemetry,
 * properties, commands, relationships, and components. This class provides
 * methods to parse DTDL v2 models and manage their contents.
 *
 * @see <a href=
 *      "https://github.com/Azure/opendigitaltwins-dtdl/blob/master/DTDL/v2/DTDL.v2.md">DTDL
 *      v2 Specification</a>
 */
public class DtdlHandler {

	private static final Logger logger = LoggerFactory.getLogger(DtdlHandler.class);

	/**
	 * Processes the {@code contents} field of a DTDL model, which defines the
	 * telemetry, properties, commands, relationships, and components of the digital
	 * twin.
	 *
	 * @param templateObject the {@code DtdlHandler} instance to populate with the
	 *                       parsed contents.
	 * @param contents       a list of maps representing the contents of the DTDL
	 *                       model.
	 */
	private static void elaborateContents(final DtdlHandler templateObject, final List<Map<String, Object>> contents) {
		for (final Map<String, Object> singleContent : contents) {

			if (singleContent.get("@type") instanceof String) {
				switch (singleContent.get("@type").toString()) {
				case "Telemetry":
					templateObject.addTelemetry(singleContent);
					break;
				case "Property":
					templateObject.addProperty(singleContent);
					break;
				case "Command":
					templateObject.addCommand(singleContent);
					break;
				case "Relationship":
					templateObject.addRelationship(singleContent);
					break;
				case "Component":
					templateObject.addComponent(singleContent);
					break;
				default:
					logger.error("type " + singleContent.get("@type").toString() + " is not know");
					break;
				}
			} else if (singleContent.get("@type") instanceof List) {
				@SuppressWarnings("unchecked")
				final List<String> typesList = (List<String>) singleContent.get("@type");
				if (typesList.contains("Telemetry")) {
					templateObject.addTelemetry(singleContent);
				} else if (typesList.contains("Property")) {
					templateObject.addProperty(singleContent);
				} else {
					logger.error("types " + typesList.toArray(new String[0]) + " are not know");
				}
			}
		}
	}

	/**
	 * Parses a DTDL v2 model from a JSON string and creates a {@code DtdlHandler}
	 * instance.
	 *
	 * @param dtdlV2String the JSON string representing the DTDL v2 model. The
	 *                     string must conform to the DTDL v2 specification.
	 * @return a {@code DtdlHandler} instance populated with the parsed model data.
	 * @throws JsonParseException       if the JSON string is malformed.
	 * @throws IOException              if an I/O error occurs during parsing.
	 * @throws IllegalArgumentException if the {@code @context} or {@code @type}
	 *                                  fields are invalid.
	 * @throws NullPointerException     if the {@code @id} field is missing.
	 */
	@SuppressWarnings("unchecked")
	public static DtdlHandler newFromDtdlV2(final String dtdlV2String) throws JsonParseException, IOException {
		final DtdlHandler templateObject = new DtdlHandler();
		final Object data = JsonUtils.fromString(dtdlV2String);
		if (data instanceof Map) {
			for (final Entry<String, Object> v : ((Map<String, Object>) data).entrySet()) {
				switch (v.getKey()) {
				case "@context":
					if (!"dtmi:dtdl:context;2".equals(v.getValue())) {
						throw new IllegalArgumentException(
								"@context must be dtmi:dtdl:context;2 but is " + v.getValue());
					}
					break;
				case "@type":
					if (!"Interface".equals(v.getValue())) {
						throw new IllegalArgumentException("@type must be Interface but is " + v.getValue());
					}
					templateObject.setType(v.getValue());
					break;
				case "@id":
					templateObject.setId(DigitalTwinModelIdentifier.fromString(v.getValue().toString()));
					break;
				case "displayName":
					templateObject.setDisplayName(v.getValue().toString());
					break;
				case "comment":
					templateObject.setComment(v.getValue().toString());
					break;
				case "description":
					templateObject.setDescription(v.getValue().toString());
					break;
				case "schemas":
					templateObject.setSchemas(v.getValue());
					break;
				case "extends":
					templateObject.setExtends(v.getValue());
					break;
				case "contents":
					templateObject.setContents(v.getValue());
					break;
				}
			}
		} else {
			throw new IllegalArgumentException("dtdl must resolve to Map, but is " + data.getClass());
		}
		if (templateObject.getId() == null) {
			throw new NullPointerException("@id is mandatory in DTDL v2");
		}
		return templateObject;
	}

	private final List<CommandObject> commands = new ArrayList<>();

	private String comment;

	private final List<ComponentObject> components = new ArrayList<>();

	private String description;

	private String displayName;
	private DigitalTwinModelIdentifier id;

	private final List<PropertyObject> properties = new ArrayList<>();

	private final List<RelationshipObject> relationships = new ArrayList<>();

	private final List<TelemetryObject> telemetries = new ArrayList<>();

	private String type;

	private DtdlHandler() {

	}

	/**
	 * Adds a command definition to the digital twin model.
	 *
	 * @param command a map representing the command definition. The map must
	 *                include the {@code @type}, {@code name}, and optionally
	 *                {@code request} and {@code response} fields.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler addCommand(final Map<String, Object> command) {
		commands.add(new CommandObject(command));
		return this;
	}

	/**
	 * Adds a component definition to the digital twin model.
	 *
	 * @param component a map representing the component definition. The map must
	 *                  include the {@code @type}, {@code name}, and {@code schema}
	 *                  fields.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler addComponent(final Map<String, Object> component) {
		components.add(new ComponentObject(component));
		return this;
	}

	/**
	 * Adds a property definition to the digital twin model.
	 *
	 * @param property a map representing the property definition. The map must
	 *                 include the {@code @type}, {@code name}, and {@code schema}
	 *                 fields.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler addProperty(final Map<String, Object> property) {
		properties.add(new PropertyObject(property));
		return this;
	}

	/**
	 * Adds a relationship definition to the digital twin model.
	 *
	 * @param relationship a map representing the relationship definition. The map
	 *                     must include the {@code @type}, {@code name}, and
	 *                     {@code target} fields.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler addRelationship(final Map<String, Object> relationship) {
		relationships.add(new RelationshipObject(relationship));
		return this;
	}

	/**
	 * Adds a telemetry definition to the digital twin model.
	 *
	 * @param telemetry a map representing the telemetry definition. The map must
	 *                  include the {@code @type}, {@code name}, and {@code schema}
	 *                  fields.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler addTelemetry(final Map<String, Object> telemetry) {
		telemetries.add(new TelemetryObject(telemetry));
		return this;
	}

	/**
	 * Returns the list of command objects defined in the digital twin model.
	 *
	 * @return the list of command objects.
	 */
	public List<CommandObject> getCommands() {
		return commands;
	}

	/**
	 * Returns the comment associated with the digital twin model.
	 *
	 * @return the comment string.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Returns the list of component objects defined in the digital twin model.
	 *
	 * @return the list of component objects.
	 */
	public List<ComponentObject> getComponents() {
		return components;
	}

	/**
	 * Returns the description of the digital twin model.
	 *
	 * @return the description string.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the display name of the digital twin model.
	 *
	 * @return the display name string.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Returns the unique identifier of the digital twin model.
	 *
	 * @return the {@code DigitalTwinModelIdentifier} representing the model's ID.
	 */
	public DigitalTwinModelIdentifier getId() {
		return id;
	}

	/**
	 * Returns the list of property objects defined in the digital twin model.
	 *
	 * @return the list of property objects.
	 */
	public List<PropertyObject> getProperties() {
		return properties;
	}

	/**
	 * Returns the list of relationship objects defined in the digital twin model.
	 *
	 * @return the list of relationship objects.
	 */
	public List<RelationshipObject> getRelationships() {
		return relationships;
	}

	/**
	 * Returns the list of telemetry objects defined in the digital twin model.
	 *
	 * @return the list of telemetry objects.
	 */
	public List<TelemetryObject> getTelemetries() {
		return telemetries;
	}

	/**
	 * Returns the type of the digital twin model.
	 *
	 * @return the type string, typically {@code Interface}.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the {@code comment} field of the digital twin model.
	 *
	 * @param comment an optional comment for the model.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler setComment(final String comment) {
		this.comment = comment;
		return this;
	}

	/**
	 * Sets the {@code contents} field of the digital twin model.
	 *
	 * @param contents an object representing the contents of the model. This must
	 *                 be a list of maps conforming to the DTDL v2 specification.
	 * @return the current {@code DtdlHandler} instance.
	 * @throws IllegalArgumentException if the contents are not a list.
	 */
	@SuppressWarnings("unchecked")
	DtdlHandler setContents(final Object contents) {
		if (contents instanceof List) {
			elaborateContents(this, (List<Map<String, Object>>) contents);
		} else {
			throw new IllegalArgumentException("contents must be a List but is " + contents.getClass().getName());
		}
		return this;
	}

	/**
	 * Sets the {@code description} field of the digital twin model.
	 *
	 * @param description the description of the model.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler setDescription(final String description) {
		this.description = description;
		return this;
	}

	/**
	 * Sets the {@code displayName} field of the digital twin model.
	 *
	 * @param displayName the display name of the model.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler setDisplayName(final String displayName) {
		this.displayName = displayName;
		return this;
	}

	/**
	 * Sets the {@code extends} field of the digital twin model.
	 *
	 * @param extendsData an object representing the models extended by this model.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler setExtends(final Object extendsData) {
		logger.info("extends ->" + extendsData.getClass().getName());
		// TODO completare interprete Extends
		return this;
	}

	/**
	 * Sets the {@code @id} field of the digital twin model.
	 *
	 * @param id the {@code DigitalTwinModelIdentifier} representing the unique
	 *           identifier of the model.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler setId(final DigitalTwinModelIdentifier id) {
		this.id = id;
		return this;
	}

	/**
	 * Sets the {@code schemas} field of the digital twin model.
	 *
	 * @param schemas an object representing the schemas used in the model.
	 * @return the current {@code DtdlHandler} instance.
	 */
	DtdlHandler setSchemas(final Object schemas) {
		logger.info("schemas ->" + schemas.getClass().getName());
		// TODO completare interprete Schemas
		return this;
	}

	/**
	 * Sets the {@code @type} field of the digital twin model.
	 *
	 * @param type the type of the model, typically {@code Interface}.
	 */
	void setType(final Object type) {
		this.type = type.toString();

	}

	/**
	 * Returns a string representation of the digital twin model.
	 *
	 * @return a string representation of the model, including its ID, type, and
	 *         contents.
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Interface [");
		if (id != null) {
			builder.append("id=");
			builder.append(id);
			builder.append(", ");
		}
		if (type != null) {
			builder.append("type=");
			builder.append(type);
			builder.append(", ");
		}
		if (comment != null) {
			builder.append("comment=");
			builder.append(comment);
			builder.append(", ");
		}
		if (telemetries != null) {
			builder.append("telemetries=");
			builder.append(telemetries);
			builder.append(", ");
		}
		if (properties != null) {
			builder.append("properties=");
			builder.append(properties);
			builder.append(", ");
		}
		if (commands != null) {
			builder.append("commands=");
			builder.append(commands);
			builder.append(", ");
		}
		if (relationships != null) {
			builder.append("relationships=");
			builder.append(relationships);
			builder.append(", ");
		}
		if (components != null) {
			builder.append("components=");
			builder.append(components);
			builder.append(", ");
		}
		if (description != null) {
			builder.append("description=");
			builder.append(description);
			builder.append(", ");
		}
		if (displayName != null) {
			builder.append("displayName=");
			builder.append(displayName);
		}
		builder.append("]");
		return builder.toString();
	}

}