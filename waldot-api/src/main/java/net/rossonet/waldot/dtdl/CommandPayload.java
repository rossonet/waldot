package net.rossonet.waldot.dtdl;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code CommandPayload} class represents the payload definition for a
 * command in the Digital Twin Definition Language (DTDL) version 2. Command
 * payloads define the structure and schema of the data exchanged when a command
 * is invoked on a digital twin.
 *
 * <p>
 * This class parses and stores command payload attributes defined in a DTDL
 * model, including:
 * </p>
 * <ul>
 * <li>{@code @id}: A unique identifier for the command payload.</li>
 * <li>{@code name}: The name of the command payload.</li>
 * <li>{@code schema}: The schema of the payload, which defines the data type or
 * structure of the command's input or output.</li>
 * <li>{@code comment}, {@code description}, {@code displayName}: Optional
 * metadata fields for documentation and readability.</li>
 * </ul>
 *
 * @see <a href=
 *      "https://github.com/Azure/opendigitaltwins-dtdl/blob/master/DTDL/v2/DTDL.v2.md">DTDL
 *      v2 Specification</a>
 * 
 */
public class CommandPayload {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(CommandPayload.class);

	private String comment;

	private String description;
	private String displayName;
	private DigitalTwinModelIdentifier id;
	private String name;
	private Schema schema;

	/**
	 * Constructs a {@code CommandPayload} from a map of attributes defined in a
	 * DTDL model.
	 *
	 * @param commandPayload a map containing the command payload attributes as
	 *                       key-value pairs. The map must conform to the DTDL v2
	 *                       specification for command payload objects.
	 */
	CommandPayload(final Map<String, Object> commandPayload) {
		for (final Entry<String, Object> record : commandPayload.entrySet()) {
			switch (record.getKey()) {
			case "@id":
				this.id = DigitalTwinModelIdentifier.fromString(record.getValue().toString());
				break;
			case "name":
				this.name = record.getValue().toString();
				break;
			case "schema":
				this.schema = new Schema(record.getValue());
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
			}
		}

	}

	/**
	 * Retrieves the optional comment associated with the command payload.
	 *
	 * @return the comment string, or {@code null} if not defined.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Retrieves the description of the command payload.
	 *
	 * @return the description string, or {@code null} if not defined.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Retrieves the display name of the command payload.
	 *
	 * @return the display name string, or {@code null} if not defined.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Retrieves the unique identifier of the command payload.
	 *
	 * @return the {@link DigitalTwinModelIdentifier} representing the payload's ID.
	 */
	public DigitalTwinModelIdentifier getId() {
		return id;
	}

	/**
	 * Retrieves the name of the command payload.
	 *
	 * @return the name string.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the schema of the command payload, which defines the data type or
	 * structure of its value.
	 *
	 * @return the {@link Schema} object representing the payload's schema.
	 */
	public Schema getSchema() {
		return schema;
	}

	/**
	 * Returns a string representation of the command payload object.
	 *
	 * @return a string containing the payload's attributes, including name, schema,
	 *         ID, and metadata.
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Command Payload [");
		if (name != null) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if (schema != null) {
			builder.append("schema=");
			builder.append(schema);
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
		}
		builder.append("]");
		return builder.toString();
	}
}