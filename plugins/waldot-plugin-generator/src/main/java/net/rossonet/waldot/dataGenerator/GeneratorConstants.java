package net.rossonet.waldot.dataGenerator;

import net.rossonet.waldot.dataGenerator.DataGeneratorVertex.Algorithm;

/**
 * Constants of the data generator plugin: vertex type label, property keys,
 * default values and parameter descriptions.
 *
 * <p>
 * The <code>*_DESCRIPTION</code> constants are shared by the OPC UA command
 * that creates the vertices and by
 * {@link DataGeneratorVertex#generateParameters} that registers the parameters
 * on the vertex type node.
 * </p>
 *
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public final class GeneratorConstants {

	/** OPC UA folder of the plugin commands. */
	public static final String BASE_CMD_DIRECTORY = "simulator";

	/** Property name for generation algorithm */
	public static final String ALGORITHM_FIELD = "Algorithm";

	public static final String ALGORITHM_FIELD_DESCRIPTION = "Algorithm used to generate the values: "
			+ "incremental (+1 at every step from Min to Max, then restarts from Min), "
			+ "decremental (-1 at every step from Max to Min, then restarts from Max), "
			+ "random (uniformly distributed value between Min and Max), "
			+ "sinusoidal (sine wave oscillating between Min and Max), "
			+ "triangular (triangle wave ramping between Min and Max) or "
			+ "stopped (the last value is kept). Default: incremental";

	/** Vertex type label for data generator vertices */
	public static final String DATA_GENERATOR_OBJECT_TYPE_LABEL = "sim:generator";

	/** Default algorithm: incremental */
	public static final String DEFAULT_ALGORITHM = Algorithm.incremental
			.toString();

	/** Default update delay: 1 second */
	public static final Long DEFAULT_DELAY = 1000L;

	/** Default maximum value: 20000 */
	public static final Long DEFAULT_MAX_VALUE = 20000L;

	/** Default minimum value: 0 */
	public static final Long DEFAULT_MIN_VALUE = 0L;

	public static final String DEFAULT_POST_NODE_ID_DATA = null;

	public static final String DELAY_DESCRIPTION = "Interval in milliseconds between two generated values, "
			+ "written in the 'data' property of the vertex; values lower than 10 ms are rejected. Default: 1000 ms";

	/** Property name for update delay */
	public static final String DELAY_FIELD = "Delay";

	public static final String MAX_VALUE_DESCRIPTION = "Upper bound (integer) of the generated values. Default: 20000";

	/** Property name for maximum value */
	public static final String MAX_VALUE_FIELD = "Max";

	public static final String MIN_VALUE_DESCRIPTION = "Lower bound (integer) of the generated values. Default: 0";

	/** Property name for minimum value */
	public static final String MIN_VALUE_FIELD = "Min";

	public static final String POST_NODE_ID_DATA_DESCRIPTION = "Optional parseable NodeId of the node that should "
			+ "receive the generated values instead of the 'data' property of the generator. "
			+ "The forwarding is not implemented yet: when this parameter is set the 'data' property is not updated. "
			+ "It cannot be changed at runtime";

	public static final String POST_NODE_ID_DATA_FIELD = "postNodeIdData";

	// ---------------------------------------------------------------------
	// event generator (sim:event-generator)
	// ---------------------------------------------------------------------

	/** Vertex type label for the event generator vertices */
	public static final String EVENT_GENERATOR_OBJECT_TYPE_LABEL = "sim:event-generator";

	public static final String EVENT_ALGORITHM_FIELD = "algorithm";
	public static final String EVENT_ALGORITHM_DESCRIPTION = "How the events are generated: "
			+ "sequential (one event per cycle, the messages are taken in order), "
			+ "random (one event per cycle, a random message), "
			+ "burst (burst-size events per cycle, the messages are taken in order) or "
			+ "stopped (no event, the vertex waits). It can be changed at runtime. Default: sequential";
	public static final String DEFAULT_EVENT_ALGORITHM = "sequential";

	public static final String EVENT_DELAY_FIELD = "delay";
	public static final String EVENT_DELAY_DESCRIPTION = "Interval in milliseconds between two generation cycles "
			+ "(minimum 10, it can be changed at runtime). Default: 1000 ms";

	public static final String EVENT_JITTER_FIELD = "jitter";
	public static final String EVENT_JITTER_DESCRIPTION = "Random extra wait in milliseconds added to every cycle "
			+ "(0 - jitter), to make the timing irregular like a real device. Default: 0";

	public static final String EVENT_MESSAGES_FIELD = "messages";
	public static final String EVENT_MESSAGES_DESCRIPTION = "List of the message texts, divided by the separator "
			+ "(default '|'). Every text can contain the placeholders of the template. "
			+ "Default: 'event ${counter}'";
	public static final String DEFAULT_EVENT_MESSAGES = "event ${counter}";

	public static final String EVENT_SEPARATOR_FIELD = "separator";
	public static final String EVENT_SEPARATOR_DESCRIPTION = "Separator of the lists in 'messages' and 'severities'. "
			+ "Default: '|'";
	public static final String DEFAULT_EVENT_SEPARATOR = "|";

	public static final String EVENT_TEMPLATE_FIELD = "template";
	public static final String EVENT_TEMPLATE_DESCRIPTION = "Template of the text of the event. Placeholders: "
			+ "${message} (the selected message), ${counter} (number of the event, from 1), ${index} (index of the "
			+ "message in the list), ${time} (ISO 8601), ${epoch} (milliseconds), ${severity}, ${random} (integer "
			+ "between random-min and random-max), ${source} (name of the vertex), ${category}, ${p:name} (value of the "
			+ "property 'name' of this vertex, useful with a link-to edge that feeds it). Default: '${message}'";
	public static final String DEFAULT_EVENT_TEMPLATE = "${message}";

	public static final String EVENT_CATEGORY_FIELD = "category";
	public static final String EVENT_CATEGORY_DESCRIPTION = "Name of the events (browse name, used as event name by "
			+ "the receivers). It can contain the placeholders of the template. Default: 'simulated'";
	public static final String DEFAULT_EVENT_CATEGORY = "simulated";

	public static final String EVENT_TYPE_FIELD = "event-type";
	public static final String EVENT_TYPE_DESCRIPTION = "OPC UA type of the events: base (BaseEventType), system "
			+ "(SystemEventType), device-failure (DeviceFailureEventType), audit (AuditEventType), progress "
			+ "(ProgressEventType) or system-status (SystemStatusChangeEventType). Default: system";
	public static final String DEFAULT_EVENT_TYPE = "system";

	public static final String EVENT_MIN_SEVERITY_FIELD = "min-severity";
	public static final String EVENT_MIN_SEVERITY_DESCRIPTION = "Minimum OPC UA severity (1-1000) of the events, "
			+ "used when 'severities' is empty. Default: 100";
	public static final long DEFAULT_EVENT_MIN_SEVERITY = 100;

	public static final String EVENT_MAX_SEVERITY_FIELD = "max-severity";
	public static final String EVENT_MAX_SEVERITY_DESCRIPTION = "Maximum OPC UA severity (1-1000) of the events, "
			+ "used when 'severities' is empty; the severity is random between min and max. Default: 500";
	public static final long DEFAULT_EVENT_MAX_SEVERITY = 500;

	public static final String EVENT_SEVERITIES_FIELD = "severities";
	public static final String EVENT_SEVERITIES_DESCRIPTION = "Optional list of severities (1-1000) divided by the "
			+ "separator, one for each message: the message at position N gets the severity at position N (the "
			+ "last one is repeated). When set it replaces min-severity and max-severity";

	public static final String EVENT_BURST_SIZE_FIELD = "burst-size";
	public static final String EVENT_BURST_SIZE_DESCRIPTION = "Number of events of every cycle with the 'burst' "
			+ "algorithm (1-10000). Default: 5";
	public static final long DEFAULT_EVENT_BURST_SIZE = 5;

	public static final String EVENT_MAX_EVENTS_FIELD = "max-events";
	public static final String EVENT_MAX_EVENTS_DESCRIPTION = "Number of events after which the generator stops "
			+ "(the algorithm becomes 'stopped'); 0 means no limit. Default: 0";

	public static final String EVENT_RANDOM_MIN_FIELD = "random-min";
	public static final String EVENT_RANDOM_MIN_DESCRIPTION = "Lower bound of the ${random} placeholder. Default: 0";

	public static final String EVENT_RANDOM_MAX_FIELD = "random-max";
	public static final String EVENT_RANDOM_MAX_DESCRIPTION = "Upper bound of the ${random} placeholder. "
			+ "Default: 100";
	public static final long DEFAULT_EVENT_RANDOM_MAX = 100;

	public static final String EVENT_SEED_FIELD = "seed";
	public static final String EVENT_SEED_DESCRIPTION = "Seed of the random choices (messages, severities, jitter, "
			+ "${random}), to repeat the same sequence; empty for a random seed";

	public static final String EVENT_COUNT_FIELD = "count";
	public static final String EVENT_COUNT_DESCRIPTION = "Runtime value: number of events generated";

	public static final String EVENT_LAST_SEVERITY_FIELD = "severity";
	public static final String EVENT_LAST_SEVERITY_DESCRIPTION = "Runtime value: severity of the last event";

	public static final String EVENT_DATA_DESCRIPTION = "Runtime value: text of the last event; it can be copied "
			+ "to other vertices with a link-to edge";

	private GeneratorConstants() {
	}

}
