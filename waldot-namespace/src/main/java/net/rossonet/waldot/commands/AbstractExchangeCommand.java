package net.rossonet.waldot.commands;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractOpcCommand;

/**
 * Base class of the data exchange commands (see
 * {@link net.rossonet.waldot.api.exchange.DataExchangeProtocol}): string
 * arguments only, permissions of the {@code query} command.
 */
abstract class AbstractExchangeCommand extends AbstractOpcCommand {

	static boolean booleanArgument(final String[] inputs, final int index, final boolean defaultValue) {
		final String value = stringArgument(inputs, index);
		return value.isBlank() ? defaultValue : Boolean.parseBoolean(value.trim());
	}

	static int intArgument(final String[] inputs, final int index, final int defaultValue, final String name) {
		final String value = stringArgument(inputs, index);
		if (value.isBlank()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException("argument '" + name + "' is not an integer: " + value, e);
		}
	}

	static String stringArgument(final String[] inputs, final int index) {
		return inputs == null || index >= inputs.length || inputs[index] == null ? "" : inputs[index];
	}

	private final String directory;

	AbstractExchangeCommand(final WaldotNamespace namespace, final String label, final String description,
			final String directory, final String[] inputs, final String[] outputs) {
		super(namespace.getGremlinGraph(), namespace, label, description,
				namespace.getConfiguration().getWaldotCommandWriteMask(),
				namespace.getConfiguration().getWaldotCommandUserWriteMask(),
				namespace.getConfiguration().getWaldotCommandExecutable(),
				namespace.getConfiguration().getWaldotCommandUserExecutable());
		this.directory = directory;
		for (final String output : outputs) {
			addOutputArgument(output, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
					LocalizedText.english(output));
		}
		for (final String input : inputs) {
			addInputArgument(input, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
					LocalizedText.english(input));
		}
		addReference(new Reference(getNodeId(), NodeIds.HasModellingRule, NodeIds.ModellingRule_Mandatory.expanded(),
				true));
	}

	@Override
	public String getDirectory() {
		return directory;
	}

	/** Output array filled with empty strings. */
	String[] newOutput() {
		final String[] output = new String[outputArguments.size()];
		java.util.Arrays.fill(output, "");
		return output;
	}
}
