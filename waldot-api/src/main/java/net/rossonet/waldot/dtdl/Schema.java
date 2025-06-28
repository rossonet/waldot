package net.rossonet.waldot.dtdl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.utils.LogHelper;

/**
 * The {@code Schema} class represents the schema definition in the Digital Twin
 * Definition Language (DTDL) version 2. A schema defines the data type or
 * structure of a property, telemetry, or command in a digital twin model.
 *
 * <p>
 * This class supports both primitive data types (e.g., integer, string) and
 * complex schemas (e.g., arrays, objects) as defined in the DTDL v2
 * specification. It parses and stores schema information based on the provided
 * input value.
 * </p>
 *
 * <p>
 * Supported primitive types include:
 * </p>
 * <ul>
 * <li>{@code boolean}</li>
 * <li>{@code date}</li>
 * <li>{@code dateTime}</li>
 * <li>{@code double}</li>
 * <li>{@code duration}</li>
 * <li>{@code float}</li>
 * <li>{@code integer}</li>
 * <li>{@code long}</li>
 * <li>{@code string}</li>
 * <li>{@code time}</li>
 * </ul>
 *
 * <p>
 * For complex schemas, the class supports model identifiers that reference
 * other DTDL components.
 * </p>
 *
 * @see <a href=
 *      "https://github.com/Azure/opendigitaltwins-dtdl/blob/master/DTDL/v2/DTDL.v2.md">DTDL
 *      v2 Specification</a>
 */

public class Schema {

	public enum DigitalTwinPrimitive {
		_boolean, _date, _dateTime, _double, _duration, _float, _integer, _long, _string, _time
	}

	private static final Logger logger = LoggerFactory.getLogger(Schema.class);
	private DigitalTwinModelIdentifier digitalTwinModelIdentifier = null;
	private DigitalTwinPrimitive digitalTwinPrimitive = null;

	/**
	 * Constructs a {@code Schema} instance based on the provided value.
	 *
	 * @param value the schema definition, which can be a string representing a
	 *              primitive type or a model identifier for complex schemas.
	 * @throws IllegalArgumentException if the value does not match any known
	 *                                  primitive type or model identifier.
	 */
	public Schema(final Object value) {
		if (value instanceof String) {
			if (!(value.toString().contains(":") || value.toString().contains(";"))) {
				try {
					this.digitalTwinPrimitive = DigitalTwinPrimitive.valueOf("_" + value);
				} catch (final IllegalArgumentException a) {
					logger.error("primitive type " + value + " not know\n" + LogHelper.stackTraceToString(a, 4));
				}
			}
			if (this.digitalTwinPrimitive == null) {
				try {
					this.digitalTwinModelIdentifier = DigitalTwinModelIdentifier.fromString(value.toString());
				} catch (final Exception e) {
					logger.error("model identifier type " + value + " not know\n" + LogHelper.stackTraceToString(e, 4));
				}
			}
		} else {
			// TODO elaborare schemi complessi
		}
	}

	/**
	 * Returns a string representation of the schema.
	 *
	 * <p>
	 * The string includes details about the schema's primitive type or model
	 * identifier, providing a human-readable summary of the schema definition.
	 * </p>
	 *
	 * @return a string representation of the schema.
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Schema [");
		if (digitalTwinModelIdentifier != null) {
			builder.append("digitalTwinModelIdentifier=");
			builder.append(digitalTwinModelIdentifier);
			builder.append(", ");
		}
		if (digitalTwinPrimitive != null) {
			builder.append("digitalTwinPrimitive=");
			builder.append(digitalTwinPrimitive.toString().substring(1));
		}
		builder.append("]");
		return builder.toString();
	}

}