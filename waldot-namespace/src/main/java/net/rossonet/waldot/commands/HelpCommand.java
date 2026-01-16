package net.rossonet.waldot.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcCommand;

public class HelpCommand extends AbstractOpcCommand {
	private static final String _TXT = ".txt";
	private static final String INDEX_TXT = "index" + _TXT;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public HelpCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				waldotNamespace.getConfiguration().getHelpCommandLabel(),
				waldotNamespace.getConfiguration().getHelpCommandDescription(),
				waldotNamespace.getConfiguration().getHelpCommandWriteMask(),
				waldotNamespace.getConfiguration().getHelpCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getHelpCommandExecutable(),
				waldotNamespace.getConfiguration().getHelpCommandUserExecutable());
		super.addOutputArgument("output", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("help message"));
		super.addInputArgument("context", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("help context to search"));
		this.addReference(new Reference(this.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
	}

	@Override
	public Object clone() {
		return new HelpCommand(this.waldotNamespace);
	}

	@Override
	public String getDirectory() {
		return MiloStrategy.GENERAL_CMD_DIRECTORY;
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		Path target;
		if (inputValues != null && inputValues.length > 0 && inputValues[0] != null && !inputValues[0].isEmpty()) {
			target = Path.of(getNamespace().getConfiguration().getHelpDirectory(), inputValues[0] + _TXT);
		} else {
			target = Path.of(getNamespace().getConfiguration().getHelpDirectory(), INDEX_TXT);
		}
		if (Files.exists(target)) {
			try {
				final List<String> lines = Files.readAllLines(target);
				if (lines.isEmpty()) {
					return new String[] { "Help file is empty: " + target.toString(), "" };
				} else {
					final StringBuilder output = new StringBuilder();
					for (final String line : lines) {
						output.append(line).append(System.lineSeparator());
					}
					return new String[] { output.toString(), "" };
				}
			} catch (final IOException e) {
				logger.error("Error reading help file: {}", target, e);
				return new String[] { "Error reading help file: " + target.toString(), e.getMessage() };
			}
		} else {
			return new String[] { "Help file not found: " + target.toString(), "" };
		}
	}

}
