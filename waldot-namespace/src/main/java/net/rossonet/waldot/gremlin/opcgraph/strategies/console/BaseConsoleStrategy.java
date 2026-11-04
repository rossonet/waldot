package net.rossonet.waldot.gremlin.opcgraph.strategies.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.commons.jexl3.JexlContext;
import org.slf4j.Logger;

import net.rossonet.waldot.api.annotation.WaldotConsoleStrategy;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.jexl.BaseExecutor;
import net.rossonet.waldot.jexl.JexlContextRegistry;
import net.rossonet.waldot.jexl.JexlExecutor;

@WaldotConsoleStrategy
public class BaseConsoleStrategy implements ConsoleStrategy {

	/** Context of the console scripts, as seen by {@code jexl-check}. */
	public static final JexlContextRegistry.Context CONSOLE_CONTEXT = new JexlContextRegistry.Context(
			JexlContextRegistry.CONSOLE,
			"console del server: query, script, file di bootstrap, selettore di render",
			List.of(JexlContextRegistry.g(), JexlContextRegistry.graph(), JexlContextRegistry.cmd(),
					JexlContextRegistry.log("logger della console del server"), JexlContextRegistry.math(),
					JexlContextRegistry.rand()),
			true);

	private BaseExecutor baseExecutor;
	// thread safe: plugins register commands from their own threads
	private final List<WaldotCommand> commands = new java.util.concurrent.CopyOnWriteArrayList<>();
	private volatile boolean dirty = false;

	private Logger logger;
	private WaldotNamespace waldotNamespace;

	@Override
	public void close() throws Exception {
		// nothing to do

	}

	@Override
	public List<WaldotCommand> getCommands() {
		return commands;
	}

	@Override
	public WaldotNamespace getWaldotNamespace() {
		return waldotNamespace;
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		JexlContextRegistry.register(CONSOLE_CONTEXT);
	}

	@Override
	public Collection<String> listConfiguredCommands() {
		final Collection<String> result = new ArrayList<>();
		for (final WaldotCommand command : commands) {
			result.add(command.getConsoleCommand());
		}
		result.add(COMMANDS_LABEL);
		result.add(LOG_LABEL);
		result.add(TRAVERSE_LABEL);
		result.add(GRAPH_LABEL);
		result.add(MATH);
		result.add(RANDOM);
		return result;
	}

	@Override
	public void registerCommand(WaldotCommand command) {
		commands.add(command);
		dirty = true;
	}

	@Override
	public void removeCommand(WaldotCommand command) {
		commands.remove(command);
		dirty = true;
	}

	private void reset() {
		logger = waldotNamespace.getConsoleLogger();
		baseExecutor = new JexlExecutor("console");
		baseExecutor.setFunctionObject(ConsoleStrategy.LOG_LABEL, waldotNamespace.getConsoleLogger());
		baseExecutor.setFunctionObject(ConsoleStrategy.TRAVERSE_LABEL, waldotNamespace.getGremlinGraph().traversal());
		baseExecutor.setFunctionObject(ConsoleStrategy.GRAPH_LABEL, waldotNamespace.getGremlinGraph());
		baseExecutor.setFunctionObject(ConsoleStrategy.COMMANDS_LABEL, waldotNamespace.getCommandsAsFunction());
		// same names as the rules: a script tested in the console behaves the same in a rule
		baseExecutor.addOrUpdateContext(ConsoleStrategy.MATH, Math.class);
		baseExecutor.addOrUpdateContext(ConsoleStrategy.RANDOM, new Random());
		for (final WaldotCommand command : commands) {
			logger.info("Registering console command: {}", command.getConsoleCommand());
			baseExecutor.setFunctionObject(command.getConsoleCommand(), command);
		}
		logger.info("Console Strategy V0 initialized");
		dirty = false;
	}

	// synchronized: the dirty check and the reset must be atomic, otherwise two
	// threads can both rebuild the executor losing concurrently registered commands
	private synchronized BaseExecutor executor() {
		if (dirty || baseExecutor == null) {
			reset();
		}
		return baseExecutor;
	}

	@Override
	public Object runIsolated(final String expression, final java.util.Map<String, Object> variables) {
		final BaseExecutor executor = executor();
		final net.rossonet.waldot.jexl.ClonableMapContext context = executor.copyBaseContext();
		if (variables != null) {
			variables.forEach(context::set);
		}
		return executor.execute(expression, context);
	}

	@Override
	public Object runExpression(final String expression) {
		return executor().execute(expression);
	}

	@Override
	public Object runExpression(final String expression, final JexlContext jexlContext) {
		return executor().execute(expression, jexlContext);
	}

}