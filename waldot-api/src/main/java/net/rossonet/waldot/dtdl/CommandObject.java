package net.rossonet.waldot.dtdl;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code CommandObject} class represents a command definition in the
 * Digital Twin Definition Language (DTDL) version 2. Commands are used to
 * define operations that can be invoked on a digital twin, including their
 * input and output payloads.
 *
 * <p>
 * This class parses and stores command attributes defined in a DTDL model,
 * including:
 * </p>
 * <ul>
 * <li>{@code @id}: A unique identifier for the command.</li>
 * <li>{@code @type}: Specifies the type of the object, which must include
 * "Command".</li>
 * <li>{@code name}: The name of the command.</li>
 * <li>{@code request}: The input payload schema for the command.</li>
 * <li>{@code response}: The output payload schema for the command.</li>
 * <li>{@code comment}, {@code description}, {@code displayName}: Optional
 * metadata fields for documentation and readability.</li>
 * <li>{@code commandType}: A deprecated field indicating the type of the
 * command.</li>
 * </ul>
 *
 * @see <a href=
 *      "https://github.com/Azure/opendigitaltwins-dtdl/blob/master/DTDL/v2/DTDL.v2.md">DTDL
 *      v2 Specification</a>
 */
public class CommandObject {

	private static final Logger logger = LoggerFactory.getLogger(CommandObject.class);

	private String commandType;

	private String comment;
	private String description;
	private String displayName;
	private DigitalTwinModelIdentifier id;
	private String name;
	private CommandPayload request;
	private CommandPayload response;

	/**
	 * Constructs a {@code CommandObject} from a map of attributes defined in a DTDL
	 * model.
	 *
	 * @param command a map containing the command attributes as key-value pairs.
	 *                The map must conform to the DTDL v2 specification for command
	 *                objects.
	 * @throws IllegalArgumentException if the {@code @type} field is missing or
	 *                                  invalid.
	 */
	@SuppressWarnings("unchecked")
	public CommandObject(final Map<String, Object> command) {
		for (final Entry<String, Object> record : command.entrySet()) {
			switch (record.getKey()) {
			case "@id":
				this.id = DigitalTwinModelIdentifier.fromString(record.getValue().toString());
				break;
			case "name":
				this.name = record.getValue().toString();
				break;
			case "@type":
				if (!"Command".equals(record.getValue())) {
					throw new IllegalArgumentException("@type must be Command but is " + record.getValue());
				}
				break;
			case "comment":
				this.comment = record.getValue().toString();
				break;
			case "description":
				this.description = record.getValue().toString();
				break;
			case "displayName":
				this.displayName = record.getValue().toString();
				break;
			case "commandType":
				this.commandType = record.getValue().toString();
				logger.warn("in Command the field commandType is deprecated");
				break;
			case "request":
				this.request = new CommandPayload((Map<String, Object>) record.getValue());
				break;
			case "response":
				this.response = new CommandPayload((Map<String, Object>) record.getValue());
				break;
			}
		}

	}

	/**
	 * Retrieves the deprecated {@code commandType} field of the command.
	 *
	 * @return the command type as a string, or {@code null} if not defined.
	 */
	public String getCommandType() {
		return commandType;
	}

	/**
	 * Retrieves the optional comment associated with the command.
	 *
	 * @return the comment string, or {@code null} if not defined.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Retrieves the description of the command.
	 *
	 * @return the description string, or {@code null} if not defined.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Retrieves the display name of the command.
	 *
	 * @return the display name string, or {@code null} if not defined.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Retrieves the unique identifier of the command.
	 *
	 * @return the {@link DigitalTwinModelIdentifier} representing the command's ID.
	 */
	public DigitalTwinModelIdentifier getId() {
		return id;
	}

	/**
	 * Retrieves the name of the command.
	 *
	 * @return the name string.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the input payload schema for the command.
	 *
	 * @return the {@link CommandPayload} object representing the request payload,
	 *         or {@code null} if not defined.
	 */
	public CommandPayload getRequest() {
		return request;
	}

	/**
	 * Retrieves the output payload schema for the command.
	 *
	 * @return the {@link CommandPayload} object representing the response payload,
	 *         or {@code null} if not defined.
	 */
	public CommandPayload getResponse() {
		return response;
	}

	/**
	 * Returns a string representation of the command object.
	 *
	 * @return a string containing the command's attributes, including name, ID,
	 *         payloads, and metadata.
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Command [");
		if (name != null) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if (id != null) {
			builder.append("id=");
			builder.append(id);
			builder.append(", ");
		}
		if (comment != null) {
			builder.append("comment=");
			builder.append(comment);
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
			builder.append(", ");
		}
		if (commandType != null) {
			builder.append("commandType=");
			builder.append(commandType);
			builder.append(", ");
		}
		if (request != null) {
			builder.append("request=");
			builder.append(request);
			builder.append(", ");
		}
		if (response != null) {
			builder.append("response=");
			builder.append(response);
		}
		builder.append("]");
		return builder.toString();
	}
}