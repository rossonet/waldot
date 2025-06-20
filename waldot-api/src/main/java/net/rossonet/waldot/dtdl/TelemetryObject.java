package net.rossonet.waldot.dtdl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public String getComment() {
		return comment;
	}

	public String getDescription() {
		return description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public DigitalTwinModelIdentifier getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Schema getSchema() {
		return schema;
	}

	public List<String> getTypes() {
		return types;
	}

	public Unit getUnit() {
		return unit;
	}

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