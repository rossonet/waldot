package net.rossonet.waldot.dtdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code RelationshipObject} class represents a relationship definition in
 * the Digital Twin Definition Language (DTDL) version 2. Relationships are used
 * to describe connections between digital twins, providing context and
 * structure to the model.
 *
 * <p>
 * This class parses and stores relationship attributes defined in a DTDL model,
 * including:
 * </p>
 * <ul>
 * <li>{@code @id}: A unique identifier for the relationship.</li>
 * <li>{@code @type}: Specifies the type of the object, which must include
 * "Relationship".</li>
 * <li>{@code name}: The name of the relationship.</li>
 * <li>{@code target}: The target digital twin of the relationship.</li>
 * <li>{@code maxMultiplicity} and {@code minMultiplicity}: Define the
 * cardinality of the relationship.</li>
 * <li>{@code writable}: Indicates whether the relationship is writable.</li>
 * <li>{@code properties}: A list of properties associated with the
 * relationship.</li>
 * <li>{@code comment}, {@code description}, {@code displayName}: Optional
 * metadata fields for documentation and readability.</li>
 * </ul>
 *
 * @see <a href=
 *      "https://github.com/Azure/opendigitaltwins-dtdl/blob/master/DTDL/v2/DTDL.v2.md">DTDL
 *      v2 Specification</a>
 */
public class RelationshipObject {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(RelationshipObject.class);

	private String comment;
	private String description;

	private String displayName;
	private DigitalTwinModelIdentifier id;
	private Integer maxMultiplicity;
	private Integer minMultiplicity;
	private String name;
	private final List<PropertyObject> properties = new ArrayList<>();

	private String target;

	private boolean writable;

	/**
	 * Constructs a {@code RelationshipObject} from a map of attributes defined in a
	 * DTDL model.
	 *
	 * @param relationship a map containing the relationship attributes as key-value
	 *                     pairs. The map must conform to the DTDL v2 specification
	 *                     for relationship objects.
	 * @throws IllegalArgumentException if the {@code @type} field is missing or
	 *                                  invalid.
	 */
	@SuppressWarnings("unchecked")
	public RelationshipObject(final Map<String, Object> relationship) {
		for (final Entry<String, Object> record : relationship.entrySet()) {
			switch (record.getKey()) {
			case "@type":
				if (!"Property".equals(record.getValue())) {
					throw new IllegalArgumentException("@type must be Property but is " + record.getValue());
				}
				break;
			case "@id":
				this.id = DigitalTwinModelIdentifier.fromString(record.getValue().toString());
				break;
			case "name":
				this.name = record.getValue().toString();
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
			case "maxMultiplicity":
				this.maxMultiplicity = Integer.valueOf(record.getValue().toString());
				break;
			case "minMultiplicity":
				this.minMultiplicity = Integer.valueOf(record.getValue().toString());
				break;
			case "writable":
				this.writable = Boolean.valueOf(record.getValue().toString());
				break;
			case "target":
				this.target = record.getValue().toString();
				break;
			case "properties":
				setProperties((List<Map<String, Object>>) record.getValue());
				break;
			}
		}
	}

	/**
	 * Retrieves the optional comment associated with the relationship.
	 *
	 * @return the comment string, or {@code null} if not defined.
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Retrieves the description of the relationship.
	 *
	 * @return the description string, or {@code null} if not defined.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Retrieves the display name of the relationship.
	 *
	 * @return the display name string, or {@code null} if not defined.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Retrieves the unique identifier of the relationship.
	 *
	 * @return the {@link DigitalTwinModelIdentifier} representing the
	 *         relationship's ID.
	 */
	public DigitalTwinModelIdentifier getId() {
		return id;
	}

	/**
	 * Retrieves the maximum multiplicity of the relationship.
	 *
	 * @return the maximum multiplicity, or {@code null} if not defined.
	 */
	public Integer getMaxMultiplicity() {
		return maxMultiplicity;
	}

	/**
	 * Retrieves the minimum multiplicity of the relationship.
	 *
	 * @return the minimum multiplicity, or {@code null} if not defined.
	 */
	public Integer getMinMultiplicity() {
		return minMultiplicity;
	}

	/**
	 * Retrieves the name of the relationship.
	 *
	 * @return the name string.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the list of properties associated with the relationship.
	 *
	 * @return a list of {@link PropertyObject} instances.
	 */
	public List<PropertyObject> getProperties() {
		return properties;
	}

	/**
	 * Retrieves the target digital twin of the relationship.
	 *
	 * @return the target string.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Checks whether the relationship is writable.
	 *
	 * @return {@code true} if the relationship is writable, {@code false}
	 *         otherwise.
	 */
	public boolean isWritable() {
		return writable;
	}

	/**
	 * Sets the properties of the relationship from a list of maps.
	 *
	 * @param props a list of maps representing the properties of the relationship.
	 *              Each map must conform to the DTDL v2 specification for property
	 *              objects.
	 * @return the current {@code RelationshipObject} instance.
	 * @throws IllegalArgumentException if the {@code props} parameter is not a
	 *                                  list.
	 */
	RelationshipObject setProperties(final List<Map<String, Object>> props) {
		if (props instanceof List) {
			for (final Map<String, Object> property : props) {
				properties.add(new PropertyObject(property));
			}
		} else {
			throw new IllegalArgumentException("properties must be a List but is " + props.getClass().getName());
		}
		return this;
	}

	/**
	 * Returns a string representation of the relationship object.
	 *
	 * @return a string containing the relationship's attributes, including name,
	 *         ID, target, and metadata.
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Relationship [");
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
		if (maxMultiplicity != null) {
			builder.append("maxMultiplicity=");
			builder.append(maxMultiplicity);
			builder.append(", ");
		}
		if (minMultiplicity != null) {
			builder.append("minMultiplicity=");
			builder.append(minMultiplicity);
			builder.append(", ");
		}
		if (properties != null) {
			builder.append("properties=");
			builder.append(properties);
			builder.append(", ");
		}
		if (target != null) {
			builder.append("target=");
			builder.append(target);
			builder.append(", ");
		}
		builder.append("writable=");
		builder.append(writable);
		builder.append("]");
		return builder.toString();
	}

}