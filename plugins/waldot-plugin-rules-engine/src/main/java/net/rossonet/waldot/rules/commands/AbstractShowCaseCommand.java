package net.rossonet.waldot.rules.commands;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.WaldotGeneratorPlugin;
import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.rules.WaldotRulesEnginePlugin;

/**
 * Base class of the commands that build a complete, running example graph
 * (show case) for the rules engine.
 * <p>
 * It provides the helpers to create vertices and edges organized in
 * directories, the argument parsing and the check that refuses to create a
 * show case that already exists.
 * </p>
 */
public abstract class AbstractShowCaseCommand extends AbstractOpcCommand {

	public static final String BASE_DIRECTORY = WaldotRulesEnginePlugin.BASE_CMD_DIRECTORY
			+ "/showcase";
	public static final UInteger DEFAULT_WRITE_MASK = UInteger
			.valueOf(WriteMask.Executable.getValue());
	/** Name of the output argument of the commands. */
	public static final String LABEL_RESULT = "result";
	private static final String LONG_LABEL_RESULT = "result of the show case creation";

	/**
	 * Returns the Gremlin/JEXL expression that resolves a vertex by its id.
	 * 
	 * @param id id of the vertex
	 * @return JEXL expression returning the vertex
	 */
	public static String ref(String id) {
		return "g.V().has('id', '" + id + "').next()";
	}

	/**
	 * Returns the JEXL expression that reads a property of a vertex.
	 * 
	 * @param id       id of the vertex
	 * @param property property label
	 * @return JEXL expression returning the value
	 */
	public static String val(String id, String property) {
		return ref(id) + ".property('" + property + "').value()";
	}

	/**
	 * Returns the JEXL statement that writes a property of a vertex.
	 * 
	 * @param id         id of the vertex
	 * @param property   property label
	 * @param expression JEXL expression of the new value
	 * @return JEXL statement
	 */
	public static String set(String id, String property, String expression) {
		return ref(id) + ".property('" + property + "', " + expression + ")";
	}

	protected AbstractShowCaseCommand(final WaldotNamespace waldotNamespace,
			final String name, final String description) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace, name,
				description, DEFAULT_WRITE_MASK, DEFAULT_WRITE_MASK, true,
				true);
		addOutputArgument(LABEL_RESULT, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null,
				LocalizedText.english(LONG_LABEL_RESULT));
		this.addReference(
				new Reference(this.getNodeId(), NodeIds.HasModellingRule,
						NodeIds.ModellingRule_Mandatory.expanded(), true));
	}

	protected void addStringArgument(String name, String description) {
		addInputArgument(name, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(description));
	}

	/**
	 * Creates a compute vertex.
	 */
	protected Vertex compute(String id, String label, String directory,
			int threads) {
		return vertex(id, WaldotRulesEnginePlugin.COMPUTE_NODE_PARAMETER, label,
				directory, WaldotRulesEnginePlugin.THREAD_POOL_SIZE_FIELD,
				String.valueOf(threads));
	}

	/**
	 * Creates an edge placed in a directory.
	 */
	protected Edge edge(Vertex from, Vertex to, String label, String type,
			String directory, Object... extra) {
		final Object[] result = new Object[4 + extra.length];
		result[0] = MiloStrategy.TYPE_FIELD.toLowerCase();
		result[1] = type;
		result[2] = MiloStrategy.DIRECTORY_PARAMETER.toLowerCase();
		result[3] = directory;
		System.arraycopy(extra, 0, result, 4, extra.length);
		return from.addEdge(label, to, result);
	}

	/**
	 * Creates the <code>rules:execute</code> edge from a compute vertex to a
	 * rule.
	 */
	protected Edge execute(Vertex compute, Vertex rule, String name,
			int priority, String directory) {
		return edge(compute, rule, name,
				WaldotRulesEnginePlugin.EXECUTE_EDGE_LABEL, directory,
				WaldotRulesEnginePlugin.PRIORITY_FIELD,
				String.valueOf(priority));
	}

	/**
	 * Checks if a vertex with the id exists in the graph.
	 */
	protected boolean exists(String id) {
		return getGraph().traversal().V().has("id", id).hasNext();
	}

	/**
	 * Creates the <code>fire</code> edge that triggers a rule when a property
	 * of the source changes.
	 */
	protected Edge fire(Vertex source, Vertex rule, String name,
			String property, String directory) {
		return edge(source, rule, name,
				WaldotRulesEnginePlugin.FIRE_EDGE_PARAMETER, directory,
				MonitoredEdge.MONITORED_PROPERTIES_LABEL, property);
	}

	/**
	 * Creates a generator vertex of waldot-plugin-generator.
	 */
	protected Vertex generator(String id, String label, String directory,
			String algorithm, long min, long max, long delayMs) {
		return vertex(id, WaldotGeneratorPlugin.DATA_GENERATOR_OBJECT_TYPE_LABEL,
				label, directory,
				WaldotGeneratorPlugin.ALGORITHM_FIELD.toLowerCase(), algorithm,
				WaldotGeneratorPlugin.MIN_VALUE_FIELD.toLowerCase(),
				String.valueOf(min),
				WaldotGeneratorPlugin.MAX_VALUE_FIELD.toLowerCase(),
				String.valueOf(max),
				WaldotGeneratorPlugin.DELAY_FIELD.toLowerCase(),
				String.valueOf(delayMs));
	}

	/**
	 * Parses an optional numeric argument.
	 * 
	 * @return the value, or the default when the argument is empty
	 * @throws IllegalArgumentException when the argument is not a number
	 */
	protected static long longArgument(String[] inputValues, int index,
			long defaultValue, String name) {
		if (inputValues == null || inputValues.length <= index
				|| inputValues[index] == null
				|| inputValues[index].trim().isEmpty()) {
			return defaultValue;
		}
		try {
			return Long.parseLong(inputValues[index].trim());
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException(
					"argument '" + name + "' is not a number: "
							+ inputValues[index]);
		}
	}

	/**
	 * Creates the link-to edge (the value of the source is copied to the
	 * destination).
	 */
	protected Edge linkTo(Vertex source, Vertex destination, String name,
			String property, String directory) {
		return edge(source, destination, name,
				MiloStrategy.LINK_TO_EDGE_TYPE, directory,
				MonitoredEdge.MONITORED_PROPERTIES_LABEL, property);
	}

	/**
	 * Creates a rule vertex.
	 */
	protected Vertex rule(String id, String label, String directory,
			String condition, String action, long hysteresisMs, int debug) {
		return vertex(id, WaldotRulesEnginePlugin.RULE_NODE_PARAMETER, label,
				directory, WaldotRulesEnginePlugin.CONDITION_FIELD, condition,
				WaldotRulesEnginePlugin.ACTION_FIELD, action,
				WaldotRulesEnginePlugin.HYSTERESIS_LABEL,
				String.valueOf(hysteresisMs),
				WaldotRulesEnginePlugin.DEBUG_LEVEL_LABEL,
				String.valueOf(debug));
	}

	/**
	 * Creates a vertex placed in a directory. A null type creates a plain
	 * vertex.
	 */
	protected Vertex vertex(String id, String type, String label,
			String directory, Object... extra) {
		final java.util.List<Object> all = new java.util.ArrayList<>();
		all.add(MiloStrategy.ID_PARAMETER.toLowerCase());
		all.add(id);
		if (type != null) {
			all.add(MiloStrategy.TYPE_FIELD.toLowerCase());
			all.add(type);
		}
		all.add(MiloStrategy.LABEL_FIELD.toLowerCase());
		all.add(label);
		all.add(MiloStrategy.DIRECTORY_PARAMETER.toLowerCase());
		all.add(directory);
		all.addAll(java.util.Arrays.asList(extra));
		return getGraph().addVertex(all.toArray());
	}

	@Override
	public String getDirectory() {
		return BASE_DIRECTORY;
	}
}
