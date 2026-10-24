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

	private GeneratorConstants() {
	}

}
