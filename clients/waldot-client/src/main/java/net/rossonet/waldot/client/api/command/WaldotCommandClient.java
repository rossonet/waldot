package net.rossonet.waldot.client.api.command;

import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * Discovery and invocation of the WaldOT commands (OPC UA methods). The
 * global commands are read from the commands folder of the server
 * ({@code cmd}) and cached; the element commands (e.g. {@code delete},
 * {@code property}) are read from the vertex or edge node.
 * <p>
 * The arguments of {@link #invoke} are converted to the declared data type
 * of each input (strings, booleans, numbers, NodeIds), so that also the
 * commands with typed inputs can be called with plain Java values.
 * </p>
 */
public interface WaldotCommandClient {

	/** Calls {@code about}: server information. */
	List<String> about();

	/** Calls the {@code delete} command of an element (vertex or edge). */
	CommandResult deleteElement(NodeId element);

	/** @return the commands of an element (vertex or edge) */
	List<CommandDescriptor> elementCommands(NodeId element);

	/** @return the global command with the given browse name */
	Optional<CommandDescriptor> globalCommand(String name);

	/** @return the global commands (cached, see {@link #refresh()}) */
	List<CommandDescriptor> globalCommands();

	/** Calls a command; the arguments are converted to the declared types. */
	CommandResult invoke(CommandDescriptor command, Object... arguments);

	/**
	 * Calls a global command by browse name.
	 *
	 * @throws net.rossonet.waldot.client.api.WaldotClientException if the
	 *                                                               command
	 *                                                               does not
	 *                                                               exist
	 */
	CommandResult invokeGlobal(String name, Object... arguments);

	/** Calls a command of an element by browse name. */
	CommandResult invokeOnElement(NodeId element, String name, Object... arguments);

	/** Discards the cache of the global commands. */
	void refresh();

	/** Calls the {@code property} command of an element. */
	CommandResult setProperty(NodeId element, String key, Object value);
}
