package net.rossonet.waldot.dtdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code TelemetryObject} class represents a telemetry definition in the
 * Digital Twin Definition Language (DTDL) version 2. Telemetry is used to
 * describe dynamic data emitted by a digital twin, such as sensor readings or
 * status updates.
 *
 * <p>
 * This class parses and stores telemetry attributes defined in a DTDL model,
 * including:
 * <ul>
 * <li>{@code @id}: A unique identifier for the telemetry.</li>
 * <li>{@code @type}: Specifies the type of the object, which must include
 * "Telemetry".</li>
 * <li>{@code name}: The name of the telemetry.</li>
 * <li>{@code schema}: The data type of the telemetry value (e.g., integer,
 * double).</li>
 * <li>{@code unit}: The unit of measurement for the telemetry value, if
 * applicable.</li>
 * <li>{@code comment}, {@code description}, {@code displayName}: Optional
 * metadata fields for documentation and readability.</li>
 * </ul>
 *
 * @see <a href=
 *      "https://github.com/Azure/opendigitaltwins-dtdl/blob/master/DTDL/v2/DTDL.v2.md">DTDL
 *      v2 Specification</a>
 */
public class TelemetryObject {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(TelemetryObject.class);

	private String comment;
	private String description;
	private String displayName;
	private DigitalTwinModelIdentifier id;

	private String name;
	private Schema schema;
	private List<String> types;

	private Unit unit;

	/**
	 * Constructs a {@code TelemetryObject} from a map of attributes defined in a
	 * DTDL model.
	 *
	 * @param telemetry a map containing the telemetry attributes as key-value
	 *                  pairs. The map must conform to the DTDL v2 specification for
	 *                  telemetry objects.
	 * @throws IllegalArgumentException if the {@code @type} field is missing or
	 *                                  invalid.
	 */
	@SuppressWarnings("unchecked")
	public TelemetryObject(final Map<String, Object> telemetry) {
		for (final Entry<String, Object> record : telemetry.entrySet()) {
			switch (record.getKey()) {
			case "@id":
				this.id = DigitalTwinModelIdentifier.fromString(record.getValue().toString());
				break;
			case "@type":
				if (record.getValue() instanceof String) {
					if (!"Telemetry".equals(record.getValue())) {
						throw new IllegalArgumentException("@type must be Telemetry but is " + record.getValue());
					}
					final List<String> typesList = new ArrayList<>();
					typesList.add(record.getValue().toString());
					this.types = typesList;
				} else if (record.getValue() instanceof List) {
					this.types = ((List<String>) record.getValue());
					if (!types.contains("Telemetry")) {
						throw new IllegalArgumentException(
								"@type must contains Telemetry but the values are " + types.toArray(new String[0]));
					}
				} else {
					throw new IllegalArgumentException(
							"@type must be a List or a String. It is a " + record.getValue().getClass());
				}
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
			case "unit":
				this.unit = Unit.getUnit(record.getValue().toString());
				break;

			}
		}
	}

	/**
	 * Retrieves the optional comment associated with the telemetry.
	 *
	 * @return the comment string, or {@code null} if not defined.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Retrieves the description of the telemetry.
	 *
	 * @return the description string, or {@code null} if not defined.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Retrieves the display name of the telemetry.
	 *
	 * @return the display name string, or {@code null} if not defined.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Retrieves the unique identifier of the telemetry.
	 *
	 * @return the {@link DigitalTwinModelIdentifier} representing the telemetry's
	 *         ID.
	 */
	public DigitalTwinModelIdentifier getId() {
		return id;
	}

	/**
	 * Retrieves the name of the telemetry.
	 *
	 * @return the name string.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the schema of the telemetry, which defines the data type of its
	 * value.
	 *
	 * @return the {@link Schema} object representing the telemetry's schema.
	 */
	public Schema getSchema() {
		return schema;
	}

	/**
	 * Retrieves the list of types associated with the telemetry.
	 *
	 * @return a list of type strings, which must include "Telemetry".
	 */
	public List<String> getTypes() {
		return types;
	}

	/**
	 * Retrieves the unit of measurement for the telemetry value, if applicable.
	 *
	 * @return the {@link Unit} object representing the telemetry's unit, or
	 *         {@code null} if not defined.
	 */
	public Unit getUnit() {
		return unit;
	}

	/**
	 * Returns a string representation of the telemetry object.
	 *
	 * @return a string containing the telemetry's attributes, including types,
	 *         name, schema, and metadata.
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Telemetry [");
		if (types != null) {
			builder.append("types=");
			builder.append(types.stream().collect(Collectors.joining(",", "[", "]")));
			builder.append(", ");
		}
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
			builder.append(", ");
		}
		if (unit != null) {
			builder.append("unit=");
			builder.append(unit);
		}
		builder.append("]");
		return builder.toString();
	}

}