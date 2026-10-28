package net.rossonet.waldot.client.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import net.rossonet.waldot.client.api.WaldotClientException;
import net.rossonet.waldot.client.api.address.BrowseFilter;
import net.rossonet.waldot.client.api.address.ReferenceInfo;
import net.rossonet.waldot.client.api.command.ArgumentDescriptor;
import net.rossonet.waldot.client.api.command.CommandDescriptor;
import net.rossonet.waldot.client.api.command.CommandResult;
import net.rossonet.waldot.client.api.command.WaldotCommandClient;
import net.rossonet.waldot.client.support.ArgumentConverter;
import net.rossonet.waldot.client.support.BaseOperationCalls;

/**
 * Commands of the client v2: the global commands are discovered browsing the
 * commands root folder (folders organize the methods), the element commands
 * browsing the components of the element.
 */
final class CommandClientV2 implements WaldotCommandClient {

	static final String INPUT_ARGUMENTS = "InputArguments";
	static final String OUTPUT_ARGUMENTS = "OutputArguments";
	static final String PROPERTY_COMMAND = "property";

	private static final int MAX_FOLDER_DEPTH = 16;

	private volatile List<CommandDescriptor> globalCommands;
	private final WaldotClientImplV2 owner;

	CommandClientV2(final WaldotClientImplV2 owner) {
		this.owner = owner;
	}

	@Override
	public List<String> about() {
		return invokeGlobal(BaseOperationCalls.ABOUT_COMMAND).orThrow().asStrings();
	}

	private List<ArgumentDescriptor> arguments(final OpcUaClient client, final DataValue value) {
		final List<ArgumentDescriptor> arguments = new ArrayList<>();
		if (value == null || value.getValue() == null || value.getValue().getValue() == null) {
			return arguments;
		}
		final Object raw = value.getValue().getValue();
		final Object[] items = raw instanceof Object[] array ? array : new Object[] { raw };
		for (final Object item : items) {
			Object decoded = item;
			if (item instanceof ExtensionObject extension) {
				decoded = extension.decode(client.getStaticEncodingContext());
			}
			if (decoded instanceof Argument argument) {
				arguments.add(new ArgumentDescriptor(argument.getName(), argument.getDataType(),
						argument.getValueRank() == null ? -1 : argument.getValueRank(),
						argument.getDescription() == null ? null : argument.getDescription().getText()));
			}
		}
		return arguments;
	}

	private CommandDescriptor describe(final NodeId objectId, final ReferenceInfo method, final String path) {
		final OpcUaClient client = owner.requireClient();
		NodeId inputsNode = null;
		NodeId outputsNode = null;
		for (final ReferenceInfo property : owner.browser()
				.browse(method.targetId(), new BrowseFilter(BrowseDirection.Forward, NodeIds.HasProperty, true))) {
			if (INPUT_ARGUMENTS.equals(property.browseName())) {
				inputsNode = property.targetId();
			} else if (OUTPUT_ARGUMENTS.equals(property.browseName())) {
				outputsNode = property.targetId();
			}
		}
		final List<ReadValueId> reads = new ArrayList<>();
		reads.add(new ReadValueId(method.targetId(), AttributeId.Description.uid(), null, QualifiedName.NULL_VALUE));
		if (inputsNode != null) {
			reads.add(new ReadValueId(inputsNode, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE));
		}
		if (outputsNode != null) {
			reads.add(new ReadValueId(outputsNode, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE));
		}
		try {
			final DataValue[] values = client.read(0.0, TimestampsToReturn.Neither, reads).getResults();
			final Object description = values[0].getValue() == null ? null : values[0].getValue().getValue();
			int index = 1;
			final List<ArgumentDescriptor> inputs = inputsNode == null ? List.of() : arguments(client, values[index++]);
			final List<ArgumentDescriptor> outputs = outputsNode == null ? List.of() : arguments(client, values[index]);
			return new CommandDescriptor(objectId, method.targetId(), method.browseName(), path,
					description instanceof LocalizedText text ? text.getText() : null, inputs, outputs);
		} catch (final UaException e) {
			throw WaldotClientException.of("reading the arguments of " + method.browseName() + " failed", e);
		}
	}

	@Override
	public CommandResult deleteElement(final NodeId element) {
		return invokeOnElement(element, BaseOperationCalls.DELETE_COMMAND);
	}

	private void discover(final NodeId folder, final String path, final int depth,
			final List<CommandDescriptor> commands) {
		if (depth > MAX_FOLDER_DEPTH) {
			return;
		}
		for (final ReferenceInfo reference : owner.browser().browse(folder,
				BrowseFilter.children().withNodeClasses(NodeClass.Object, NodeClass.Method))) {
			if (reference.targetId() == null) {
				continue;
			}
			if (reference.nodeClass() == NodeClass.Method) {
				commands.add(describe(folder, reference, path));
			} else if (reference.nodeClass() == NodeClass.Object) {
				discover(reference.targetId(),
						path.isEmpty() ? reference.browseName() : path + "/" + reference.browseName(), depth + 1,
						commands);
			}
		}
	}

	@Override
	public List<CommandDescriptor> elementCommands(final NodeId element) {
		final List<CommandDescriptor> commands = new ArrayList<>();
		final String path = element.getIdentifier() == null ? "" : element.getIdentifier().toString();
		for (final ReferenceInfo reference : owner.browser().browse(element,
				new BrowseFilter(BrowseDirection.Forward, NodeIds.HasComponent, true, NodeClass.Method))) {
			if (reference.targetId() != null) {
				commands.add(describe(element, reference, path));
			}
		}
		return commands;
	}

	@Override
	public Optional<CommandDescriptor> globalCommand(final String name) {
		for (final CommandDescriptor command : globalCommands()) {
			if (command.name() != null && command.name().equals(name)) {
				return Optional.of(command);
			}
		}
		return Optional.empty();
	}

	@Override
	public List<CommandDescriptor> globalCommands() {
		List<CommandDescriptor> commands = globalCommands;
		if (commands == null) {
			synchronized (this) {
				commands = globalCommands;
				if (commands == null) {
					final List<CommandDescriptor> discovered = new ArrayList<>();
					discover(owner.browser().waldotNodeId(owner.getConfiguration().getCommandsRootNodeId()), "", 0,
							discovered);
					commands = List.copyOf(discovered);
					globalCommands = commands;
				}
			}
		}
		return commands;
	}

	@Override
	public CommandResult invoke(final CommandDescriptor command, final Object... arguments) {
		final Variant[] inputs;
		try {
			inputs = ArgumentConverter.toVariants(arguments, command.inputs());
		} catch (final IllegalArgumentException e) {
			throw new WaldotClientException("invalid arguments for '" + command.name() + "': " + e.getMessage());
		}
		final CallMethodResult result = owner.browser().callMethod(command.objectId(), command.methodId(), inputs);
		return new CommandResult(command, result.getStatusCode(), owner.browser().outputs(result));
	}

	@Override
	public CommandResult invokeGlobal(final String name, final Object... arguments) {
		final CommandDescriptor command = globalCommand(name)
				.orElseThrow(() -> new WaldotClientException("global command '" + name + "' not found"));
		return invoke(command, arguments);
	}

	@Override
	public CommandResult invokeOnElement(final NodeId element, final String name, final Object... arguments) {
		for (final CommandDescriptor command : elementCommands(element)) {
			if (name.equals(command.name())) {
				return invoke(command, arguments);
			}
		}
		throw new WaldotClientException("command '" + name + "' not found on element " + element);
	}

	@Override
	public void refresh() {
		globalCommands = null;
	}

	@Override
	public CommandResult setProperty(final NodeId element, final String key, final Object value) {
		return invokeOnElement(element, PROPERTY_COMMAND, key, value == null ? "" : value.toString());
	}
}
