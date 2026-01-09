package net.rossonet.waldot.gremlin.opcgraph.strategies.console;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.jexl3.JexlContext;
import org.slf4j.Logger;

import net.rossonet.waldot.api.annotation.WaldotConsoleStrategy;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.rules.RuleExecutor;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.jexl.JexlExecutor;

@WaldotConsoleStrategy
public class BaseConsoleStrategy implements ConsoleStrategy {
	private RuleExecutor baseExecutor;
	private final List<WaldotCommand> commands = new ArrayList<>();
	private boolean dirty = false;
	private Logger logger;
	private WaldotNamespace waldotNamespace;

	@Override
	public void close() throws Exception {
		// nothing to do

	}

	@Override
	public WaldotNamespace getWaldotNamespace() {
		return waldotNamespace;
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
	}

	@Override
	public Collection<String> listConfiguredCommands() {
		final Collection<String> result = new ArrayList<>();
		for (final WaldotCommand command : commands) {
			result.add(command.getConsoleCommand());
		}
		result.add(COMMANDS_LABEL);
		result.add(LOG_LABEL);
		result.add(G_LABEL);
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
		baseExecutor.setFunctionObject(ConsoleStrategy.G_LABEL, waldotNamespace.getGremlinGraph());
		baseExecutor.setFunctionObject(ConsoleStrategy.COMMANDS_LABEL, waldotNamespace.getCommandsAsFunction());
		for (final WaldotCommand command : commands) {
			logger.info("Registering console command: {}", command.getConsoleCommand());
			baseExecutor.setFunctionObject(command.getConsoleCommand(), command);
		}
		logger.info("Console Strategy V0 initialized");
		dirty = false;
	}

	@Override
	public Object runExpression(final String expression) {
		if (dirty || baseExecutor == null) {
			reset();
		}
		return baseExecutor.execute(expression);
	}

	@Override
	public Object runExpression(final String expression, final JexlContext jexlContext) {
		if (dirty || baseExecutor == null) {
			reset();
		}
		return baseExecutor.execute(expression, jexlContext);
	}

}
