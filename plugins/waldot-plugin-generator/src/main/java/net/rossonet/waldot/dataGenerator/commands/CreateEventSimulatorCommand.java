package net.rossonet.waldot.dataGenerator.commands;

import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_MAX_SEVERITY;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_MIN_SEVERITY;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_ALGORITHM_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_ALGORITHM_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_BURST_SIZE_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_BURST_SIZE_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_CATEGORY_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_CATEGORY_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_DELAY_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_DELAY_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_GENERATOR_OBJECT_TYPE_LABEL;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_JITTER_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_JITTER_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MAX_EVENTS_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MAX_EVENTS_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MAX_SEVERITY_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MAX_SEVERITY_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MESSAGES_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MESSAGES_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MIN_SEVERITY_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MIN_SEVERITY_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_RANDOM_MAX_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_RANDOM_MAX_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_RANDOM_MIN_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_RANDOM_MIN_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEED_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEED_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEPARATOR_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEPARATOR_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEVERITIES_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEVERITIES_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_TEMPLATE_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_TEMPLATE_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_TYPE_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_TYPE_FIELD;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.EnumUtils;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.dataGenerator.EventGeneratorVertex;
import net.rossonet.waldot.dataGenerator.EventGeneratorVertex.EventAlgorithm;
import net.rossonet.waldot.dataGenerator.GeneratorConstants;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;

/**
 * OPC UA command that creates an event generator vertex (
 * {@value GeneratorConstants#EVENT_GENERATOR_OBJECT_TYPE_LABEL}).
 */
public class CreateEventSimulatorCommand extends AbstractCreateVertexCommand {

	private static final String DESCRIPTION = "Create an event generator vertex that periodically publishes "
			+ "simulated OPC UA events (messages built from a list and a template)";
	private static final String NAME = "create event generator";

	private static String checkLong(final List<Object> keyValues,
			final String key, final long min, final long max) {
		final String text = findValue(keyValues, key);
		if (text == null) {
			return null;
		}
		try {
			final long value = Long.parseLong(text.trim());
			if (value < min || value > max) {
				return "'" + key + "' must be between " + min + " and " + max;
			}
			return null;
		} catch (final NumberFormatException e) {
			return "'" + key + "' is not a number: " + text;
		}
	}

	public CreateEventSimulatorCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace, NAME, DESCRIPTION,
				GeneratorConstants.BASE_CMD_DIRECTORY,
				EVENT_GENERATOR_OBJECT_TYPE_LABEL, Language.ENGLISH,
				List.of(VertexParameter.optional(EVENT_ALGORITHM_FIELD,
						VariableNodeTypes.String, EVENT_ALGORITHM_DESCRIPTION),
						VertexParameter.optional(EVENT_DELAY_FIELD,
								VariableNodeTypes.Int64,
								EVENT_DELAY_DESCRIPTION),
						VertexParameter.optional(EVENT_JITTER_FIELD,
								VariableNodeTypes.Int64,
								EVENT_JITTER_DESCRIPTION),
						VertexParameter.optional(EVENT_MESSAGES_FIELD,
								VariableNodeTypes.String,
								EVENT_MESSAGES_DESCRIPTION),
						VertexParameter.optional(EVENT_SEPARATOR_FIELD,
								VariableNodeTypes.String,
								EVENT_SEPARATOR_DESCRIPTION),
						VertexParameter.optional(EVENT_TEMPLATE_FIELD,
								VariableNodeTypes.String,
								EVENT_TEMPLATE_DESCRIPTION),
						VertexParameter.optional(EVENT_CATEGORY_FIELD,
								VariableNodeTypes.String,
								EVENT_CATEGORY_DESCRIPTION),
						VertexParameter.optional(EVENT_TYPE_FIELD,
								VariableNodeTypes.String,
								EVENT_TYPE_DESCRIPTION),
						VertexParameter.optional(EVENT_MIN_SEVERITY_FIELD,
								VariableNodeTypes.Int64,
								EVENT_MIN_SEVERITY_DESCRIPTION),
						VertexParameter.optional(EVENT_MAX_SEVERITY_FIELD,
								VariableNodeTypes.Int64,
								EVENT_MAX_SEVERITY_DESCRIPTION),
						VertexParameter.optional(EVENT_SEVERITIES_FIELD,
								VariableNodeTypes.String,
								EVENT_SEVERITIES_DESCRIPTION),
						VertexParameter.optional(EVENT_BURST_SIZE_FIELD,
								VariableNodeTypes.Int64,
								EVENT_BURST_SIZE_DESCRIPTION),
						VertexParameter.optional(EVENT_MAX_EVENTS_FIELD,
								VariableNodeTypes.Int64,
								EVENT_MAX_EVENTS_DESCRIPTION),
						VertexParameter.optional(EVENT_RANDOM_MIN_FIELD,
								VariableNodeTypes.Int64,
								EVENT_RANDOM_MIN_DESCRIPTION),
						VertexParameter.optional(EVENT_RANDOM_MAX_FIELD,
								VariableNodeTypes.Int64,
								EVENT_RANDOM_MAX_DESCRIPTION),
						VertexParameter.optional(EVENT_SEED_FIELD,
								VariableNodeTypes.String,
								EVENT_SEED_DESCRIPTION)));
	}

	@Override
	protected String validate(final List<Object> keyValues) {
		final String algorithm = findValue(keyValues, EVENT_ALGORITHM_FIELD);
		if (algorithm != null && !EnumUtils.isValidEnum(EventAlgorithm.class,
				algorithm.trim().toLowerCase(Locale.ROOT))) {
			return "unknown algorithm '" + algorithm + "', valid values are "
					+ Arrays.toString(EventAlgorithm.values());
		}
		final String type = findValue(keyValues, EVENT_TYPE_FIELD);
		if (type != null && !EventGeneratorVertex.EVENT_TYPES
				.containsKey(type.trim().toLowerCase(Locale.ROOT))) {
			return "unknown event type '" + type + "', valid values are "
					+ EventGeneratorVertex.EVENT_TYPES.keySet();
		}
		for (final String error : new String[]{
				checkLong(keyValues, EVENT_DELAY_FIELD, 10, Long.MAX_VALUE),
				checkLong(keyValues, EVENT_JITTER_FIELD, 0, 3_600_000),
				checkLong(keyValues, EVENT_MIN_SEVERITY_FIELD, 1, 1000),
				checkLong(keyValues, EVENT_MAX_SEVERITY_FIELD, 1, 1000),
				checkLong(keyValues, EVENT_BURST_SIZE_FIELD, 1, 10000),
				checkLong(keyValues, EVENT_MAX_EVENTS_FIELD, 0, Long.MAX_VALUE),
				checkLong(keyValues, EVENT_RANDOM_MIN_FIELD, Long.MIN_VALUE,
						Long.MAX_VALUE),
				checkLong(keyValues, EVENT_RANDOM_MAX_FIELD, Long.MIN_VALUE,
						Long.MAX_VALUE)}) {
			if (error != null) {
				return error;
			}
		}
		final String severities = findValue(keyValues, EVENT_SEVERITIES_FIELD);
		if (severities != null) {
			final String separator = findValue(keyValues,
					EVENT_SEPARATOR_FIELD);
			for (final String item : EventGeneratorVertex.split(severities,
					separator)) {
				try {
					final long value = Long.parseLong(item);
					if (value < 1 || value > 1000) {
						return "severity " + item
								+ " must be between 1 and 1000";
					}
				} catch (final NumberFormatException e) {
					return "severity '" + item + "' is not a number";
				}
			}
		}
		final String min = findValue(keyValues, EVENT_MIN_SEVERITY_FIELD);
		final String max = findValue(keyValues, EVENT_MAX_SEVERITY_FIELD);
		final long minValue = min != null
				? Long.parseLong(min.trim())
				: DEFAULT_EVENT_MIN_SEVERITY;
		final long maxValue = max != null
				? Long.parseLong(max.trim())
				: DEFAULT_EVENT_MAX_SEVERITY;
		if (minValue > maxValue) {
			return "min-severity (" + minValue
					+ ") must not be greater than max-severity (" + maxValue
					+ ")";
		}
		return null;
	}

}
