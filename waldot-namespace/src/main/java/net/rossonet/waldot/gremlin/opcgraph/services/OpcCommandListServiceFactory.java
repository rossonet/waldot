package net.rossonet.waldot.gremlin.opcgraph.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.service.Service;
import org.apache.tinkerpop.gremlin.structure.util.CloseableIterator;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;

/**
 * Gremlin service {@value #NAME}: lists the OPC UA commands that
 * {@link OpcCommandServiceFactory} can run.
 *
 * <pre>{@code
 * g.V(id).call("waldot.commands")   // commands of the element (Streaming)
 * g.call("waldot.commands")         // global commands (Start)
 * }</pre>
 *
 * Every command is a map with {@code command} (name), {@code description} and
 * {@code inputs} (names of the input arguments). Nothing is executed, so the
 * service is available also when {@value GremlinCommandSettings#ENABLED_KEY}
 * is off.
 */
public class OpcCommandListServiceFactory extends OpcServiceRegistry.OpcServiceFactory<Object, Map<String, Object>>
		implements Service<Object, Map<String, Object>> {

	/** Name of the service. */
	public static final String NAME = "waldot.commands";

	/** Result key: name of the command. */
	public static final String RESULT_COMMAND = "command";

	/** Result key: description of the command. */
	public static final String RESULT_DESCRIPTION = "description";

	/** Result key: names of the input arguments. */
	public static final String RESULT_INPUTS = "inputs";

	private static CloseableIterator<Map<String, Object>> describe(final Collection<? extends WaldotCommand> commands) {
		final List<Map<String, Object>> result = new ArrayList<>();
		if (commands != null) {
			for (final WaldotCommand command : commands) {
				final Map<String, Object> entry = new LinkedHashMap<>();
				entry.put(RESULT_COMMAND, OpcCommandServiceFactory.commandName(command));
				entry.put(RESULT_DESCRIPTION,
						command.getDescription() == null ? null : command.getDescription().getText());
				entry.put(RESULT_INPUTS, OpcCommandServiceFactory.inputNames(command));
				result.add(entry);
			}
		}
		return CloseableIterator.of(result.iterator());
	}

	private final Service<Object, Map<String, Object>> startService = new OpcServiceRegistry.TinkerService<>(this) {
		@Override
		public CloseableIterator<Map<String, Object>> execute(final ServiceCallContext ctx, final Map params) {
			final WaldotNamespace namespace = graph.getWaldotNamespace();
			return describe(namespace == null || namespace.getConsoleStrategy() == null ? List.of()
					: namespace.getConsoleStrategy().getCommands());
		}

		@Override
		public Type getType() {
			return Type.Start;
		}
	};

	public OpcCommandListServiceFactory(final AbstractOpcGraph graph) {
		super(graph, NAME);
	}

	@Override
	public void close() {
		// nothing to release
	}

	@Override
	public Service<Object, Map<String, Object>> createService(final boolean isStart, final Map params) {
		return isStart ? startService : this;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Map describeParams() {
		return Collections.emptyMap();
	}

	@Override
	public CloseableIterator<Map<String, Object>> execute(final ServiceCallContext ctx,
			final Traverser.Admin<Object> in, final Map params) {
		return describe(OpcCommandServiceFactory.commandsOf(in.get()));
	}

	@Override
	public Set<Type> getSupportedTypes() {
		return Set.of(Type.Streaming, Type.Start);
	}

	@Override
	public Type getType() {
		return Type.Streaming;
	}
}
