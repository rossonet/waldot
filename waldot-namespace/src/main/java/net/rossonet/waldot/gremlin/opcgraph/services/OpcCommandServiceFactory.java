package net.rossonet.waldot.gremlin.opcgraph.services;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.util.TraverserSet;
import org.apache.tinkerpop.gremlin.structure.service.Service;
import org.apache.tinkerpop.gremlin.structure.util.CloseableIterator;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.base.GremlinElement;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;

/**
 * Gremlin service {@value #NAME}: runs the OPC UA commands (method nodes) of
 * vertices and edges, or the global commands, from any Gremlin query through
 * the TinkerPop {@code call()} step.
 *
 * <pre>{@code
 * // command of the elements (Barrier mode)
 * g.V(id).call("waldot.command", [command: "property", args: ["color", "red"]])
 * g.V().has("name", within("a", "b")).call("waldot.command", [command: "delete"])
 * // global command (Start mode)
 * g.call("waldot.command", [command: "about"])
 * }</pre>
 *
 * <h2>Execution</h2>
 * <ul>
 * <li><b>Barrier</b> (mid traversal): all the input elements are collected
 * first, then the commands run one at a time, in the order of the traversers,
 * in the thread of the query. Every element runs the command once, whatever
 * its bulk. The command is looked up among the method nodes of the element
 * ({@code UaObjectNode#getMethodNodes()}), matching the browse name or the
 * console command. The optional parameter {@link Params#CHUNK_SIZE} limits the
 * elements collected before each execution (default: all).</li>
 * <li><b>Start</b> ({@code g.call(...)}): the command is looked up among the
 * global commands of the console strategy.</li>
 * </ul>
 *
 * <h2>Checks</h2> The following conditions produce a result with
 * {@code error}, never an exception, so the other invocations go on: unknown
 * or ambiguous command, removed element, {@code Executable} or
 * {@code UserExecutable} false, fewer arguments than the input arguments of
 * the command, exception thrown by the command (logged).
 *
 * <h2>Result</h2> One {@link LinkedHashMap} for each invocation, serializable
 * in GraphSON and GraphBinary:
 * <ul>
 * <li>{@code element}: NodeId of the element
 * ({@code NodeId#toParseableString()}), {@code null} in Start mode;</li>
 * <li>{@code command}: the requested command;</li>
 * <li>{@code outputs}: output argument name ({@code out0}, {@code out1}... when
 * missing) to value (string or primitive wrapper);</li>
 * <li>{@code error}: {@code null} or the error message.</li>
 * </ul>
 *
 * <h2>Security</h2> The service is enabled only when the flag
 * {@value GremlinCommandSettings#ENABLED_KEY} is {@code true} (see
 * {@link GremlinCommandSettings}); otherwise the query fails with an
 * {@link IllegalStateException}. Commands are run with
 * {@code runCommand(null, args)}: the {@code InvocationContext} is
 * {@code null}, so there is no OPC UA session and no OPC UA user; only the
 * {@code Executable}/{@code UserExecutable} attributes ({@code isExecutable()}, {@code isUserExecutable()}) of the method node are
 * checked.
 */
public class OpcCommandServiceFactory extends OpcServiceRegistry.OpcServiceFactory<Object, Map<String, Object>> {

	/** Barrier service: runs the command on the collected elements. */
	private final class BarrierCommandService extends OpcServiceRegistry.TinkerService<Object, Map<String, Object>> {
		private final int maxBarrierSize;

		private BarrierCommandService(final int maxBarrierSize) {
			super(OpcCommandServiceFactory.this);
			this.maxBarrierSize = maxBarrierSize;
		}

		@Override
		public CloseableIterator<Map<String, Object>> execute(final ServiceCallContext ctx,
				final TraverserSet<Object> in, final Map params) {
			final String command = command(params);
			final String[] args = args(params);
			// all the elements are collected before running any command
			final Set<Object> elements = new LinkedHashSet<>();
			for (final Traverser.Admin<Object> traverser : in) {
				elements.add(traverser.get());
			}
			final List<Map<String, Object>> results = new ArrayList<>(elements.size());
			for (final Object element : elements) {
				results.add(invokeOnElement(element, command, args));
			}
			return CloseableIterator.of(results.iterator());
		}

		@Override
		public int getMaxBarrierSize() {
			return maxBarrierSize;
		}

		@Override
		public Type getType() {
			return Type.Barrier;
		}
	}

	/** Parameters of the service. */
	public interface Params {
		/** Name (browse name or console command) of the command, required. */
		String COMMAND = "command";
		/**
		 * Arguments of the command (list, array or single value), each converted
		 * with {@link String#valueOf(Object)}.
		 */
		String ARGS = "args";
		/**
		 * Optional, Barrier mode only: maximum number of elements collected before
		 * running the commands (default {@link Integer#MAX_VALUE}, all the
		 * elements).
		 */
		String CHUNK_SIZE = OpcServiceRegistry.LambdaServiceFactory.Options.CHUNK_SIZE;

		@SuppressWarnings("rawtypes")
		Map DESCRIBE = Collections.unmodifiableMap(new LinkedHashMap<>(Map.of(COMMAND,
				"Name of the command (browse name or console command), required", ARGS,
				"Arguments of the command (list of values converted to strings), optional", CHUNK_SIZE,
				"Barrier only: elements collected before each execution, optional (default all)")));
	}

	/** Start service: runs a global command. */
	private final class StartCommandService extends OpcServiceRegistry.TinkerService<Object, Map<String, Object>> {

		private StartCommandService() {
			super(OpcCommandServiceFactory.this);
		}

		@Override
		public CloseableIterator<Map<String, Object>> execute(final ServiceCallContext ctx, final Map params) {
			final String command = command(params);
			final WaldotNamespace namespace = graph.getWaldotNamespace();
			final List<WaldotCommand> candidates = namespace == null || namespace.getConsoleStrategy() == null
					? List.of()
					: matching(namespace.getConsoleStrategy().getCommands(), command);
			return CloseableIterator.of(List.of(invoke(null, candidates, command, args(params))).iterator());
		}

		@Override
		public Type getType() {
			return Type.Start;
		}
	}

	/** Result key: NodeId of the element ({@code null} in Start mode). */
	public static final String RESULT_ELEMENT = "element";

	/** Result key: requested command. */
	public static final String RESULT_COMMAND = "command";

	/** Result key: outputs of the command by output argument name. */
	public static final String RESULT_OUTPUTS = "outputs";

	/** Result key: error message or {@code null}. */
	public static final String RESULT_ERROR = "error";

	private static final Logger logger = LoggerFactory.getLogger(OpcCommandServiceFactory.class);

	/** Name of the service. */
	public static final String NAME = "waldot.command";

	static String[] args(final Map<?, ?> params) {
		final Object raw = params == null ? null : params.get(Params.ARGS);
		if (raw == null) {
			return new String[0];
		}
		if (raw instanceof Collection<?>) {
			return ((Collection<?>) raw).stream().map(String::valueOf).toArray(String[]::new);
		}
		if (raw.getClass().isArray()) {
			final String[] result = new String[Array.getLength(raw)];
			for (int i = 0; i < result.length; i++) {
				result[i] = String.valueOf(Array.get(raw, i));
			}
			return result;
		}
		return new String[] { String.valueOf(raw) };
	}

	private static int chunkSize(final Map<?, ?> params) {
		final Object raw = params == null ? null : params.get(Params.CHUNK_SIZE);
		if (raw == null) {
			return Integer.MAX_VALUE;
		}
		final int value;
		try {
			value = raw instanceof Number ? ((Number) raw).intValue() : Integer.parseInt(raw.toString().trim());
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException(Params.CHUNK_SIZE + " must be a positive integer: " + raw, e);
		}
		if (value <= 0) {
			throw new IllegalArgumentException(Params.CHUNK_SIZE + " must be a positive integer: " + raw);
		}
		return value;
	}

	static String command(final Map<?, ?> params) {
		final Object raw = params == null ? null : params.get(Params.COMMAND);
		if (raw == null || raw.toString().isBlank()) {
			throw new IllegalArgumentException("parameter '" + Params.COMMAND + "' is required by " + NAME);
		}
		return raw.toString().trim();
	}

	/**
	 * Name of a command as shown by the services: the console command, or the
	 * browse name when there is none.
	 */
	public static String commandName(final WaldotCommand command) {
		final String console = command.getConsoleCommand();
		if (console != null && !console.isBlank()) {
			return console;
		}
		return command.getBrowseName() == null ? null : command.getBrowseName().getName();
	}

	/**
	 * Commands (method nodes) of a vertex or an edge.
	 *
	 * @return empty list when the object is not an OPC UA element
	 */
	public static List<WaldotCommand> commandsOf(final Object element) {
		if (!(element instanceof GremlinElement)) {
			return List.of();
		}
		final List<WaldotCommand> result = new ArrayList<>();
		for (final UaMethodNode method : ((GremlinElement) element).getMethodNodes()) {
			if (method instanceof WaldotCommand) {
				result.add((WaldotCommand) method);
			}
		}
		return result;
	}

	/** Names of the input arguments of a command. */
	public static List<String> inputNames(final WaldotCommand command) {
		final Argument[] inputs = command.getInputArguments();
		final List<String> names = new ArrayList<>();
		if (inputs != null) {
			for (final Argument input : inputs) {
				names.add(input == null ? null : input.getName());
			}
		}
		return names;
	}

	private static List<WaldotCommand> matching(final Collection<? extends WaldotCommand> commands,
			final String command) {
		final List<WaldotCommand> result = new ArrayList<>();
		if (commands == null) {
			return result;
		}
		for (final WaldotCommand candidate : commands) {
			final boolean byBrowseName = candidate.getBrowseName() != null
					&& command.equals(candidate.getBrowseName().getName());
			if ((byBrowseName || command.equals(candidate.getConsoleCommand())) && !result.contains(candidate)) {
				result.add(candidate);
			}
		}
		return result;
	}

	private static Object outputValue(final Object value) {
		if (value == null || value instanceof String || value instanceof Boolean || value instanceof Integer
				|| value instanceof Long || value instanceof Double || value instanceof Float
				|| value instanceof Short || value instanceof Byte) {
			return value;
		}
		if (value instanceof Object[]) {
			return Arrays.deepToString((Object[]) value);
		}
		return String.valueOf(value);
	}

	private static Map<String, Object> result(final String element, final String command,
			final Map<String, Object> outputs, final String error) {
		final Map<String, Object> result = new LinkedHashMap<>();
		result.put(RESULT_ELEMENT, element);
		result.put(RESULT_COMMAND, command);
		result.put(RESULT_OUTPUTS, outputs);
		result.put(RESULT_ERROR, error);
		return result;
	}

	public OpcCommandServiceFactory(final AbstractOpcGraph graph) {
		super(graph, NAME);
	}

	@Override
	public void close() {
		// nothing to release
	}

	@Override
	public Service<Object, Map<String, Object>> createService(final boolean isStart, final Map params) {
		if (!GremlinCommandSettings.isEnabled(graph.getWaldotNamespace())) {
			throw new IllegalStateException("Gremlin commands are disabled: set "
					+ GremlinCommandSettings.ENABLED_KEY + "=true to use the service " + NAME);
		}
		command(params);
		if (isStart) {
			return new StartCommandService();
		}
		return new BarrierCommandService(chunkSize(params));
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map describeParams() {
		return Params.DESCRIBE;
	}

	@Override
	public Set<Service.Type> getSupportedTypes() {
		return Set.of(Service.Type.Barrier, Service.Type.Start);
	}

	/**
	 * Checks and runs one invocation.
	 *
	 * @param element    NodeId of the element, {@code null} for global commands
	 * @param candidates commands matching the requested name
	 */
	private Map<String, Object> invoke(final String element, final List<WaldotCommand> candidates,
			final String command, final String[] args) {
		if (candidates.isEmpty()) {
			return result(element, command, null, "command '" + command + "' not found");
		}
		if (candidates.size() > 1) {
			return result(element, command, null,
					"command '" + command + "' is ambiguous: " + candidates.size() + " commands match");
		}
		final WaldotCommand target = candidates.get(0);
		if (!Boolean.TRUE.equals(target.isExecutable()) || !Boolean.TRUE.equals(target.isUserExecutable())) {
			return result(element, command, null, "command '" + command + "' is not executable");
		}
		final Argument[] inputs = target.getInputArguments();
		final int required = inputs == null ? 0 : inputs.length;
		if (args.length < required) {
			return result(element, command, null, "command '" + command + "' requires " + required
					+ " arguments " + inputNames(target) + ", got " + args.length);
		}
		// read before running: a command such as delete removes its own node
		// (and the OutputArguments property) from the address space
		final Argument[] outputArguments = target.getOutputArguments();
		final Object[] values;
		try {
			// no InvocationContext: no OPC UA session and no OPC UA user
			values = target.runCommand(null, args);
		} catch (final Exception e) {
			logger.error("Gremlin command '{}' on {} failed", command, element == null ? "namespace" : element, e);
			return result(element, command, null,
					e.getMessage() == null ? e.getClass().getName() : e.getMessage());
		}
		final Map<String, Object> outputs = new LinkedHashMap<>();
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				final String name = outputArguments != null && i < outputArguments.length
						&& outputArguments[i] != null && outputArguments[i].getName() != null
								? outputArguments[i].getName()
								: "out" + i;
				outputs.put(name, outputValue(values[i]));
			}
		}
		return result(element, command, outputs, null);
	}

	private Map<String, Object> invokeOnElement(final Object element, final String command, final String[] args) {
		if (!(element instanceof GremlinElement)) {
			return result(null, command, null, "not an OPC UA vertex or edge: " + element);
		}
		final GremlinElement gremlinElement = (GremlinElement) element;
		final String elementId = gremlinElement.getNodeId().toParseableString();
		if (gremlinElement.isRemoved()) {
			return result(elementId, command, null, "element " + elementId + " has been removed");
		}
		return invoke(elementId, matching(commandsOf(gremlinElement), command), command, args);
	}
}
