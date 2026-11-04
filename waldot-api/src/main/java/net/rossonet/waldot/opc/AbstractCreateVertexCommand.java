package net.rossonet.waldot.opc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.strategies.MiloStrategy;

/**
 * Base class of the OPC UA commands that create a vertex of a type managed by
 * a plugin.
 * <p>
 * The command exposes:
 * <ul>
 * <li>the input argument {@value #LABEL_ARGUMENT} (label of the new
 * vertex);</li>
 * <li>the input argument {@value #DIRECTORY_ARGUMENT} (folder of the new vertex
 * in the address space);</li>
 * <li>one input argument for every {@link VertexParameter} of the vertex
 * type;</li>
 * <li>a single output argument {@value #RESULT_ARGUMENT}, containing the
 * parseable NodeId of the created vertex or, on failure, a message starting
 * with {@value #ERROR_PREFIX}.</li>
 * </ul>
 * Empty input values are not passed to the vertex, which then uses its
 * defaults. Required parameters and the syntax of numeric and boolean values are
 * validated before the creation.
 * </p>
 *
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public abstract class AbstractCreateVertexCommand extends AbstractOpcCommand {

	/** Language of the standard descriptions (label, directory, result). */
	public enum Language {
		ENGLISH, ITALIAN
	}

	/**
	 * Input parameter of the vertex created by the command.
	 *
	 * @param key         key passed to {@code graph.addVertex} (exactly the key
	 *                    read by the vertex)
	 * @param type        OPC UA data type of the input argument
	 * @param description accurate description of the parameter
	 * @param required    true if the parameter must be provided
	 */
	public record VertexParameter(String key, VariableNodeTypes type, String description, boolean required,
			String defaultValue, Double min, Double max, List<String> allowedValues) {

		private static final java.util.regex.Pattern DEFAULT_IN_DESCRIPTION = java.util.regex.Pattern
				.compile("(?i)\\bdefault:?\\s+'?([^\\s';,()]+?)'?(?=[\\s.;,()]|$)");

		public VertexParameter {
			allowedValues = allowedValues == null ? List.of() : List.copyOf(allowedValues);
		}

		/**
		 * Parameter without default, range and allowed values (the default can be
		 * read from the description, see {@link #effectiveDefault()}).
		 */
		public VertexParameter(final String key, final VariableNodeTypes type, final String description,
				final boolean required) {
			this(key, type, description, required, null, null, null, List.of());
		}

		/**
		 * Optional parameter.
		 *
		 * @param key         key of the vertex property
		 * @param type        OPC UA data type
		 * @param description description of the parameter
		 * @return the parameter
		 */
		public static VertexParameter optional(final String key, final VariableNodeTypes type,
				final String description) {
			return new VertexParameter(key, type, description, false);
		}

		/**
		 * Required parameter.
		 *
		 * @param key         key of the vertex property
		 * @param type        OPC UA data type
		 * @param description description of the parameter
		 * @return the parameter
		 */
		public static VertexParameter required(final String key, final VariableNodeTypes type,
				final String description) {
			return new VertexParameter(key, type, description, true);
		}

		/**
		 * The default value: the declared one or, when missing, the one written in
		 * the description as {@code Default: X}.
		 *
		 * @return the default, null if unknown
		 */
		public String effectiveDefault() {
			if (defaultValue != null) {
				return defaultValue;
			}
			if (description == null) {
				return null;
			}
			final java.util.regex.Matcher m = DEFAULT_IN_DESCRIPTION.matcher(description);
			return m.find() ? m.group(1) : null;
		}

		/** Same parameter with the given allowed values. */
		public VertexParameter withAllowed(final java.util.Collection<String> values) {
			return new VertexParameter(key, type, description, required, defaultValue, min, max,
					values == null ? List.of() : List.copyOf(values));
		}

		/** Same parameter with the given allowed values. */
		public VertexParameter withAllowed(final String... values) {
			return withAllowed(List.of(values));
		}

		/** Same parameter with the given default. */
		public VertexParameter withDefault(final Object value) {
			return new VertexParameter(key, type, description, required, value == null ? null : value.toString(), min,
					max, allowedValues);
		}

		/** Same parameter with the given range (null for no bound). */
		public VertexParameter withRange(final Number minimum, final Number maximum) {
			return new VertexParameter(key, type, description, required, defaultValue,
					minimum == null ? null : minimum.doubleValue(), maximum == null ? null : maximum.doubleValue(),
					allowedValues);
		}

		/** JSON-ready description of the parameter (used by {@code type describe}). */
		public java.util.Map<String, Object> toSchema() {
			final java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
			m.put("key", key);
			m.put("type", schemaType(type));
			m.put("opcua_type", type.name());
			m.put("required", required);
			final String def = effectiveDefault();
			if (def != null) {
				m.put("default", def);
			}
			if (min != null) {
				m.put("min", min);
			}
			if (max != null) {
				m.put("max", max);
			}
			if (!allowedValues.isEmpty()) {
				m.put("enum", allowedValues);
			}
			m.put("description", description);
			return m;
		}

		/** Short type name for the schema: int, long, double, bool, string. */
		public static String schemaType(final VariableNodeTypes type) {
			return switch (type) {
			case Boolean -> "bool";
			case Byte, Int16, Int32, Integer, SByte, UInt16, UInt32, UInteger -> "int";
			case Int64, UInt64 -> "long";
			case Double, Float, Duration -> "double";
			default -> "string";
			};
		}
	}

	public static final UInteger DEFAULT_WRITE_MASK = UInteger.valueOf(WriteMask.Executable.getValue());

	public static final String DIRECTORY_ARGUMENT = MiloStrategy.DIRECTORY_PARAMETER;

	public static final String ERROR_PREFIX = "error: ";

	public static final String LABEL_ARGUMENT = MiloStrategy.LABEL_FIELD.toLowerCase();

	public static final String RESULT_ARGUMENT = "result";

	private static String directoryDescription(final Language language) {
		return language == Language.ITALIAN
				? "Cartella (opzionale) in cui creare il vertice nello spazio degli indirizzi OPC UA; i livelli sono separati da '"
						+ MiloStrategy.DIRECTORY_SPLIT_SIMBOL + "'. Se vuoto il vertice viene creato nella cartella radice"
				: "Optional folder of the new vertex in the OPC UA address space; levels are separated by '"
						+ MiloStrategy.DIRECTORY_SPLIT_SIMBOL + "'. When empty the vertex is created in the root folder";
	}

	private static String labelDescription(final Language language) {
		return language == Language.ITALIAN
				? "Etichetta del nuovo vertice, usata anche come nome di visualizzazione e browse name OPC UA"
				: "Label of the new vertex, also used as OPC UA display name and browse name";
	}

	private static String resultDescription(final Language language) {
		return language == Language.ITALIAN
				? "NodeId del vertice creato oppure, in caso di errore, il messaggio che inizia con '" + ERROR_PREFIX
						+ "'"
				: "NodeId of the created vertex or, on failure, the message starting with '" + ERROR_PREFIX + "'";
	}

	private final Language language;

	private volatile List<VertexParameter> parameters;

	private final String vertexType;

	/**
	 * @param waldotNamespace    namespace
	 * @param commandName        unique name of the command
	 * @param commandDescription description of the command
	 * @param directory          directory of the command
	 * @param vertexType         type label of the created vertex
	 * @param language           language of the standard descriptions
	 * @param parameters         parameters of the vertex
	 */
	protected AbstractCreateVertexCommand(final WaldotNamespace waldotNamespace, final String commandName,
			final String commandDescription, final String directory, final String vertexType,
			final Language language, final List<VertexParameter> parameters) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace, commandName, commandName, commandDescription,
				directory, DEFAULT_WRITE_MASK, DEFAULT_WRITE_MASK, true, true);
		this.vertexType = vertexType;
		this.language = language;
		this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
		addInputArgument(LABEL_ARGUMENT, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(labelDescription(language)));
		addInputArgument(DIRECTORY_ARGUMENT, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(directoryDescription(language)));
		for (final VertexParameter parameter : this.parameters) {
			addInputArgument(parameter.key(), parameter.type().getNodeId(), ValueRanks.Scalar, null,
					LocalizedText.english(parameter.description()));
		}
		addOutputArgument(RESULT_ARGUMENT, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(resultDescription(language)));
		addReference(new Reference(getNodeId(), NodeIds.HasModellingRule, NodeIds.ModellingRule_Mandatory.expanded(),
				true));
	}

	private String checkValue(final VertexParameter parameter, final String value) {
		try {
			switch (parameter.type()) {
			case Boolean:
				if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
					throw new IllegalArgumentException();
				}
				break;
			case Byte, Int16, Int32, Int64, Integer, SByte, UInt16, UInt32, UInt64, UInteger:
				Long.parseLong(value.trim());
				break;
			case Double, Float, Duration:
				Double.parseDouble(value.trim());
				break;
			default:
				break;
			}
		} catch (final RuntimeException e) {
			return language == Language.ITALIAN
					? "il valore '" + value + "' del parametro " + parameter.key() + " non è di tipo "
							+ parameter.type()
					: "the value '" + value + "' of the parameter " + parameter.key() + " is not of type "
							+ parameter.type();
		}
		if (!parameter.allowedValues().isEmpty()
				&& parameter.allowedValues().stream().noneMatch(a -> a.equalsIgnoreCase(value.trim()))) {
			return language == Language.ITALIAN
					? "il valore '" + value + "' del parametro " + parameter.key() + " non è ammesso; valori: "
							+ parameter.allowedValues()
					: "the value '" + value + "' of the parameter " + parameter.key() + " is not allowed; values: "
							+ parameter.allowedValues();
		}
		if (parameter.min() != null || parameter.max() != null) {
			final double number;
			try {
				number = Double.parseDouble(value.trim());
			} catch (final RuntimeException e) {
				return null;
			}
			if (parameter.min() != null && number < parameter.min()
					|| parameter.max() != null && number > parameter.max()) {
				return language == Language.ITALIAN
						? "il valore " + value + " del parametro " + parameter.key() + " è fuori dall'intervallo ["
								+ bound(parameter.min()) + ", " + bound(parameter.max()) + "]"
						: "the value " + value + " of the parameter " + parameter.key() + " is out of the range ["
								+ bound(parameter.min()) + ", " + bound(parameter.max()) + "]";
			}
		}
		return null;
	}

	private static String bound(final Double value) {
		if (value == null) {
			return "-";
		}
		return value == Math.rint(value) ? String.valueOf(value.longValue()) : String.valueOf(value);
	}

	/**
	 * Clones the command, using the constructor with the namespace as only
	 * argument. Subclasses with different constructors must override it.
	 */
	@Override
	public Object clone() {
		try {
			return getClass().getConstructor(WaldotNamespace.class).newInstance(waldotNamespace);
		} catch (final ReflectiveOperationException e) {
			throw new IllegalStateException("cannot clone " + getClass().getName(), e);
		}
	}

	private String error(final String message) {
		return ERROR_PREFIX + message;
	}

	/**
	 * Adds schema information (default, range, allowed values) to a parameter;
	 * for the constructors of the subclasses, after {@code super(...)}. The OPC UA
	 * input arguments do not change.
	 *
	 * @param key    key of the parameter
	 * @param refine function that returns the refined parameter
	 * @throws IllegalArgumentException if the parameter does not exist
	 */
	protected final void refineParameter(final String key,
			final java.util.function.UnaryOperator<VertexParameter> refine) {
		final List<VertexParameter> refined = new ArrayList<>(parameters);
		for (int i = 0; i < refined.size(); i++) {
			if (refined.get(i).key().equals(key)) {
				refined.set(i, refine.apply(refined.get(i)));
				parameters = Collections.unmodifiableList(refined);
				return;
			}
		}
		throw new IllegalArgumentException("no parameter " + key + " in " + getClass().getSimpleName());
	}

	/**
	 * @return the parameters of the created vertex (label and directory excluded)
	 */
	public List<VertexParameter> getParameters() {
		return parameters;
	}

	/**
	 * @return the type label of the created vertex
	 */
	public String getVertexType() {
		return vertexType;
	}

	private boolean isTypeManaged() {
		for (final PluginListener plugin : waldotNamespace.getPlugins()) {
			if (plugin.containsVertexType(vertexType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] inputs = inputValues == null ? new String[0] : inputValues;
		try {
			if (!isTypeManaged()) {
				return new String[] { error(language == Language.ITALIAN
						? "il tipo di vertice " + vertexType + " non è gestito da nessun plugin"
						: "the vertex type " + vertexType + " is not managed by any plugin") };
			}
			final List<Object> keyValues = new ArrayList<>();
			keyValues.add(MiloStrategy.TYPE_FIELD.toLowerCase());
			keyValues.add(vertexType);
			addIfPresent(keyValues, LABEL_ARGUMENT, value(inputs, 0));
			addIfPresent(keyValues, DIRECTORY_ARGUMENT, value(inputs, 1));
			for (int i = 0; i < parameters.size(); i++) {
				final VertexParameter parameter = parameters.get(i);
				final String value = value(inputs, i + 2);
				if (value == null) {
					if (parameter.required()) {
						return new String[] { error(language == Language.ITALIAN
								? "il parametro " + parameter.key() + " è obbligatorio"
								: "the parameter " + parameter.key() + " is required") };
					}
					continue;
				}
				final String check = checkValue(parameter, value);
				if (check != null) {
					return new String[] { error(check) };
				}
				keyValues.add(parameter.key());
				keyValues.add(value);
			}
			final String validation = validate(keyValues);
			if (validation != null) {
				return new String[] { error(validation) };
			}
			LOGGER.info("create vertex with parameters: {}", keyValues);
			final Vertex vertex = getGraph().addVertex(keyValues.toArray());
			if (!(vertex instanceof WaldotVertex)) {
				return new String[] { error(language == Language.ITALIAN ? "il vertice non è stato creato"
						: "the vertex has not been created") };
			}
			return new String[] { ((WaldotVertex) vertex).getNodeId().toParseableString() };
		} catch (final Throwable e) {
			LOGGER.warn("error creating vertex of type {} with parameters {}", vertexType, Arrays.toString(inputs),
					e);
			final String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
			return new String[] { error(message) };
		}
	}

	private static void addIfPresent(final List<Object> keyValues, final String key, final String value) {
		if (value != null) {
			keyValues.add(key);
			keyValues.add(value);
		}
	}

	/**
	 * Finds a value in the key/value list passed to {@link #validate(List)}.
	 *
	 * @param keyValues key/value pairs
	 * @param key       key to find
	 * @return the value or null if not present
	 */
	protected static String findValue(final List<Object> keyValues, final String key) {
		for (int i = 0; i + 1 < keyValues.size(); i += 2) {
			if (key.equals(keyValues.get(i))) {
				final Object value = keyValues.get(i + 1);
				return value == null ? null : value.toString().trim();
			}
		}
		return null;
	}

	/**
	 * Additional validation of the parameters before the creation of the vertex.
	 *
	 * @param keyValues key/value pairs that will be passed to
	 *                  {@code graph.addVertex}
	 * @return null if valid, the error message otherwise
	 */
	protected String validate(final List<Object> keyValues) {
		return null;
	}

	private static String value(final String[] inputs, final int index) {
		if (index >= inputs.length || inputs[index] == null || inputs[index].trim().isEmpty()) {
			return null;
		}
		return inputs[index];
	}
}
