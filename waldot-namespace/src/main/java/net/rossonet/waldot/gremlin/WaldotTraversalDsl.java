package net.rossonet.waldot.gremlin;

import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

import net.rossonet.waldot.gremlin.opcgraph.services.OpcCommandListServiceFactory;
import net.rossonet.waldot.gremlin.opcgraph.services.OpcCommandServiceFactory;

/**
 * Java shortcuts (no annotation processor) for the WaldOT Gremlin services
 * that run the OPC UA commands of vertices and edges. They are equivalent to
 * the {@code call()} step:
 *
 * <pre>{@code
 * traversal.exec("property", "color", "red")
 * // same as
 * traversal.call("waldot.command", Map.of("command", "property", "args", List.of("color", "red")))
 * }</pre>
 *
 * @see OpcCommandServiceFactory
 * @see OpcCommandListServiceFactory
 */
public interface WaldotTraversalDsl<S, E> extends GraphTraversal.Admin<S, E> {

	/**
	 * Lists the commands of the current elements (service
	 * {@value OpcCommandListServiceFactory#NAME}).
	 */
	default <M> GraphTraversal<S, M> commands() {
		return call(OpcCommandListServiceFactory.NAME);
	}

	/**
	 * Runs a command on the current elements (service
	 * {@value OpcCommandServiceFactory#NAME}, Barrier mode).
	 *
	 * @param command name of the command
	 * @param args    arguments of the command
	 */
	default <M> GraphTraversal<S, M> exec(final String command, final String... args) {
		return call(OpcCommandServiceFactory.NAME, Map.of(OpcCommandServiceFactory.Params.COMMAND, command,
				OpcCommandServiceFactory.Params.ARGS, List.of(args)));
	}
}
