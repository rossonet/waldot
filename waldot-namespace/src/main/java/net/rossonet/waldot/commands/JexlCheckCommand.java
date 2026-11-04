package net.rossonet.waldot.commands;

import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_COMMAND;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_CONTEXT;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_ERROR;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.ARG_JSON;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.INFORMATION_DIRECTORY;
import static net.rossonet.waldot.api.exchange.DataExchangeProtocol.JEXL_CHECK_COMMAND;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.jexl3.JexlEngine;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.jexl.JexlChecker;
import net.rossonet.waldot.jexl.JexlContextRegistry;
import net.rossonet.waldot.jexl.JexlExecutor;

/**
 * Checks a JEXL script for one of the contexts of the server (console, rule,
 * iec104-command, modbus-point, ...) without running it: compilation errors
 * with line, column and excerpt, variables not provided by the context, traps
 * of the language. Without a script it lists the contexts with their
 * variables.
 */
public class JexlCheckCommand extends AbstractExchangeCommand {

	private static final JexlEngine ENGINE = JexlExecutor.generateEngine();

	private static final ObjectMapper JSON = new ObjectMapper();

	/** Description of a context for the JSON output. */
	public static Map<String, Object> describe(final JexlContextRegistry.Context context) {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("name", context.name());
		map.put("description", context.description());
		final List<Map<String, Object>> variables = new ArrayList<>();
		for (final JexlContextRegistry.Variable v : context.variables()) {
			final Map<String, Object> var = new LinkedHashMap<>();
			var.put("name", v.name());
			var.put("type", v.type());
			var.put("description", v.description());
			variables.add(var);
		}
		map.put("variables", variables);
		map.put("console_commands", context.consoleCommands());
		return map;
	}

	public JexlCheckCommand(final WaldotNamespace namespace) {
		super(namespace, JEXL_CHECK_COMMAND,
				"check a JEXL script for a context (console, rule, iec104-command, ...) without running it; "
						+ "without a script list the contexts",
				INFORMATION_DIRECTORY, new String[] { ARG_COMMAND, ARG_CONTEXT }, new String[] { ARG_JSON, ARG_ERROR });
	}

	/**
	 * Runs the check (also used by the tests).
	 *
	 * @param script  the script, blank to list the contexts
	 * @param context name of the context, blank for the console
	 * @return the JSON tree as maps
	 */
	public Map<String, Object> check(final String script, final String context) {
		if (script == null || script.isBlank()) {
			final Map<String, Object> map = new LinkedHashMap<>();
			map.put("contexts", JexlContextRegistry.all().stream().map(JexlCheckCommand::describe).toList());
			return map;
		}
		final String name = context == null || context.isBlank() ? JexlContextRegistry.CONSOLE : context.strip();
		final JexlContextRegistry.Context descriptor = JexlContextRegistry.get(name)
				.orElseThrow(() -> new IllegalArgumentException("contesto sconosciuto '" + name + "'; contesti: "
						+ String.join(", ",
								JexlContextRegistry.all().stream().map(JexlContextRegistry.Context::name).toList())));
		final List<String> commands = new ArrayList<>();
		if (getNamespace().getConsoleStrategy() != null) {
			for (final WaldotCommand command : getNamespace().getConsoleStrategy().getCommands()) {
				commands.add(command.getConsoleCommand());
			}
		}
		return JexlChecker.check(ENGINE, descriptor, commands, script).toMap();
	}

	@Override
	public Object clone() {
		return new JexlCheckCommand(waldotNamespace);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		final String[] output = newOutput();
		try {
			output[0] = JSON.writeValueAsString(check(stringArgument(inputValues, 0), stringArgument(inputValues, 1)));
		} catch (final Throwable e) {
			output[1] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
