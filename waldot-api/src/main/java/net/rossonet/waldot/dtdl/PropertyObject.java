package net.rossonet.waldot.dtdl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code PropertyObject} class represents a property definition in the
 * Digital Twin Definition Language (DTDL) version 2. Properties are used to
 * describe static or dynamic data associated with a digital twin, such as
 * configuration settings or state information.
 *
 * <p>
 * This class parses and stores property attributes defined in a DTDL model,
 * including:
 * </p>
 * <ul>
 * <li>{@code @id}: A unique identifier for the property.</li>
 * <li>{@code @type}: Specifies the type of the object, which must include
 * "Property".</li>
 * <li>{@code name}: The name of the property.</li>
 * <li>{@code schema}: The data type or structure of the property value (e.g.,
 * integer, string).</li>
 * <li>{@code unit}: The unit of measurement for the property value, if
 * applicable.</li>
 * <li>{@code writable}: Indicates whether the property is writable.</li>
 * <li>{@code comment}, {@code description}, {@code displayName}: Optional
 * metadata fields for documentation and readability.</li>
 * </ul>
 *
 * @see <a href=
 *      "https://github.com/Azure/opendigitaltwins-dtdl/blob/master/DTDL/v2/DTDL.v2.md">DTDL
 *      v2 Specification</a>
 */
public class PropertyObject {
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PropertyObject.class);
	private String comment;
	private String description;
	private String displayName;

	private DigitalTwinModelIdentifier id;

	private String name;

	private Schema schema;

	private List<String> types;

	private Unit unit;

	private boolean writable;

	public PropertyObject() {
	}

	/**
	 * Constructs a {@code PropertyObject} from a map of attributes defined in a
	 * DTDL model.
	 *
	 * @param property a map containing the property attributes as key-value pairs.
	 *                 The map must conform to the DTDL v2 specification for
	 *                 property objects.
	 * @throws IllegalArgumentException if the {@code @type} field is missing or
	 *                                  invalid.
	 */
	@SuppressWarnings("unchecked")
	public PropertyObject(final Map<String, Object> property) {
		for (final Entry<String, Object> record : property.entrySet()) {
			switch (record.getKey()) {
			case "@id":
				this.id = DigitalTwinModelIdentifier.fromString(record.getValue().toString());
				break;
			case "@type":
				if (record.getValue() instanceof String) {
					if (!"Property".equals(record.getValue())) {
						throw new IllegalArgumentException("@type must be Property but is " + record.getValue());
					}
					final List<String> typesList = new ArrayList<>();
					typesList.add(record.getValue().toString());
					this.types = typesList;
				} else if (record.getValue() instanceof List) {
					this.types = ((List<String>) record.getValue());
					if (!types.contains("Property")) {
						throw new IllegalArgumentException(
								"@type must contains Property but the values are " + types.toArray(new String[0]));
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
			case "writable":
				this.writable = Boolean.valueOf(record.getValue().toString());
				break;
			}
		}
	}

	/**
	 * Retrieves the optional comment associated with the property.
	 *
	 * @return the comment string, or {@code null} if not defined.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Retrieves the description of the property.
	 *
	 * @return the description string, or {@code null} if not defined.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Retrieves the display name of the property.
	 *
	 * @return the display name string, or {@code null} if not defined.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Retrieves the unique identifier of the property.
	 *
	 * @return the {@link DigitalTwinModelIdentifier} representing the property's
	 *         ID.
	 */
	public DigitalTwinModelIdentifier getId() {
		return id;
	}

	/**
	 * Retrieves the name of the property.
	 *
	 * @return the name string.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the schema of the property, which defines the data type or
	 * structure of its value.
	 *
	 * @return the {@link Schema} object representing the property's schema.
	 */
	public Schema getSchema() {
		return schema;
	}

	/**
	 * Retrieves the list of types associated with the property.
	 *
	 * @return a list of type strings, which must include "Property".
	 */
	public List<String> getTypes() {
		return types;
	}

	/**
	 * Retrieves the unit of measurement for the property value, if applicable.
	 *
	 * @return the {@link Unit} object representing the property's unit, or
	 *         {@code null} if not defined.
	 */
	public Unit getUnit() {
		return unit;
	}

	/**
	 * Checks whether the property is writable.
	 *
	 * @return {@code true} if the property is writable, {@code false} otherwise.
	 */
	public boolean isWritable() {
		return writable;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setId(DigitalTwinModelIdentifier id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	public void setTypes(List<String> types) {
		this.types = types;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public void setWritable(boolean writable) {
		this.writable = writable;
	}

	public Map<String, Object> toMap() {
		final Map<String, Object> map = new HashMap<>();
		map.put("@id", id.toString());
		if (types.size() == 1) {
			map.put("@type", types.get(0));
		} else {
			map.put("@type", types);
		}
		map.put("name", name);
		map.put("schema", schema.toSchemaString());
		if (comment != null) {
			map.put("comment", comment);
		}
		if (description != null) {
			map.put("description", description);
		}
		if (displayName != null) {
			map.put("displayName", displayName);
		}
		if (unit != null) {
			map.put("unit", unit.toString());
		}
		map.put("writable", writable);
		return map;

	}

	/**
	 * Returns a string representation of the property object.
	 *
	 * @return a string containing the property's attributes, including types, name,
	 *         schema, and metadata.
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Property [");
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
			builder.append(", ");
		}
		builder.append("writable=");
		builder.append(writable);
		builder.append("]");
		return builder.toString();
	}

}