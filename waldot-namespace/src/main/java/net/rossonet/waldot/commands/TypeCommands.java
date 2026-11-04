package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_JSON;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_TYPE;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.INFORMATION_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.TYPE_DESCRIBE_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.TYPE_LIST_COMMAND;

import java.util.Map;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * {@code type list} and {@code type describe}: the vertex types of the plugins
 * and the schema of their properties (see {@link VertexTypeSchemas}).
 */
public final class TypeCommands {

	/** {@code type describe}: schema of the properties of a vertex type. */
	public static final class Describe extends AbstractExchangeCommand {

		public Describe(final WaldotNamespace namespace) {
			super(namespace, TYPE_DESCRIBE_COMMAND,
					"describe the properties of a vertex type as JSON (key, type, default, min/max, allowed values)",
					INFORMATION_DIRECTORY, new String[] { ARG_TYPE }, new String[] { ARG_JSON, ARG_ERROR });
		}

		@Override
		public Object clone() {
			return new Describe(waldotNamespace);
		}

		@Override
		public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
			final String[] output = newOutput();
			try {
				final String type = CommandArguments.requireFirst(inputValues, ARG_TYPE).strip();
				output[0] = JSON.writeValueAsString(VertexTypeSchemas.describe(getNamespace(), type));
			} catch (final Throwable e) {
				output[1] = CommandArguments.errorMessage(e);
			}
			return output;
		}
	}

	/** {@code type list}: the vertex types of the loaded plugins. */
	public static final class List extends AbstractExchangeCommand {

		public List(final WaldotNamespace namespace) {
			super(namespace, TYPE_LIST_COMMAND,
					"list the vertex types of the loaded plugins with their create command as JSON",
					INFORMATION_DIRECTORY, new String[0], new String[] { ARG_JSON, ARG_ERROR });
		}

		@Override
		public Object clone() {
			return new List(waldotNamespace);
		}

		@Override
		public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
			final String[] output = newOutput();
			try {
				output[0] = JSON.writeValueAsString(Map.of("types",
						java.util.List.copyOf(VertexTypeSchemas.types(getNamespace()).values())));
			} catch (final Throwable e) {
				output[1] = CommandArguments.errorMessage(e);
			}
			return output;
		}
	}

	private static final ObjectMapper JSON = new ObjectMapper();

	private TypeCommands() {
	}
}
