package net.rossonet.waldot.commands;

import java.io.File;
import java.util.function.Consumer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.utils.SystemCommandHelper;
import net.rossonet.waldot.utils.SystemCommandHelper.QuotedStringTokenizer;

/**
 * ExecCommand is an implementation of AbstractOpcCommand that allows the
 * execution of system commands. It provides input arguments for the command,
 * directory, and timeout, and outputs the command's output and error messages.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class ExecCommand extends AbstractOpcCommand {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ExecCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				waldotNamespace.getConfiguration().getExecCommandLabel(),
				waldotNamespace.getConfiguration().getExecCommandDescription(),
				waldotNamespace.getConfiguration().getExecCommandWriteMask(),
				waldotNamespace.getConfiguration().getExecCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getExecCommandExecutable(),
				waldotNamespace.getConfiguration().getExecCommandUserExecutable());
		super.addOutputArgument("output", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command output"));
		super.addOutputArgument("error", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command error"));
		super.addInputArgument("command", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command to execute"));
		super.addInputArgument("directory", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("directory to execute command"));
		super.addInputArgument("timeout", VariableNodeTypes.UInt16.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("timeout in seconds"));
		this.addReference(new Reference(this.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));
	}

	@Override
	public Object clone() {
		return new ExecCommand(this.waldotNamespace);
	}

	@Override
	public String getDirectory() {
		return "system";
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		try {
			String command = "echo no command set";
			String directory = "/tmp";
			int timeout = 60;
			for (int i = 0; i < inputValues.length; i++) {
				final String label = getInputArguments()[i].getName();
				if (inputValues[i] == null || inputValues[i].isEmpty()) {
					continue;
				}
				switch (label) {
				case "command":
					command = inputValues[i];
					break;
				case "directory":
					directory = inputValues[i];
					break;
				case "timeout":
					timeout = Integer.parseInt(inputValues[i]);
				}
			}
			final String[] commandArray = new QuotedStringTokenizer(command).getTokens().toArray(new String[0]);
			final StringBuilder sb = new StringBuilder();
			final Consumer<String> consumer = new Consumer<String>() {
				@Override
				public void accept(final String line) {
					sb.append(line + System.lineSeparator());
				}
			};
			SystemCommandHelper.executeSystemCommandAndWait(new File(directory), commandArray, consumer,
					timeout * 1000);
			return new String[] { sb.toString(), null };
		} catch (final Exception e) {
			logger.error("Error executing command", e);
			return new String[] { null, "error executing command " + ExceptionUtils.getRootCauseMessage(e) };
		}
	}

}
