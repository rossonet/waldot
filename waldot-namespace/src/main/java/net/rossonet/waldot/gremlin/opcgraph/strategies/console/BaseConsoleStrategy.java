package net.rossonet.waldot.gremlin.opcgraph.strategies.console;

import org.apache.commons.jexl3.JexlContext;
import org.slf4j.Logger;

import net.rossonet.waldot.api.annotation.WaldotConsoleStrategy;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.rules.ExecutorHelper;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.jexl.JexlExecutorHelper;

@WaldotConsoleStrategy
public class BaseConsoleStrategy implements ConsoleStrategy {
	private WaldotNamespace waldotNamespace;
	private ExecutorHelper baseExecutor;
	private Logger logger;

	@Override
	public WaldotNamespace getWaldotNamespace() {
		return waldotNamespace;
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		reset();
	}

	@Override
	public void reset() {
		logger = waldotNamespace.getConsoleLogger();
		baseExecutor = new JexlExecutorHelper();
		baseExecutor.setFunctionObject(ConsoleStrategy.LOG_LABEL, waldotNamespace.getConsoleLogger());
		baseExecutor.setFunctionObject(ConsoleStrategy.G_LABEL, waldotNamespace.getGremlinGraph());
		baseExecutor.setFunctionObject(ConsoleStrategy.COMMANDS_LABEL, waldotNamespace.getCommandsAsFunction());
		logger.info("Console Strategy V0 initialized");

	}

	@Override
	public Object runExpression(final String expression) {
		return baseExecutor.execute(expression);
	}

	@Override
	public Object runExpression(final String expression, final JexlContext jexlContext) {
		return baseExecutor.execute(expression, jexlContext);
	}

}
