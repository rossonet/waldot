package net.rossonet.waldot.gremlin.opcgraph.strategies.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.annotation.WaldotConsoleStrategy;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.rules.ExecutorHelper;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.jexl.JexlExecutorHelper;

@WaldotConsoleStrategy
public class ConsoleV0Strategy implements ConsoleStrategy {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private WaldotNamespace waldotNamespace;
	private ExecutorHelper baseExecutor;

	@Override
	public void initialize(WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		baseExecutor = new JexlExecutorHelper();
		baseExecutor.setFunctionObject(ConsoleStrategy.LOG_LABEL, waldotNamespace.getConsoleLogger());
		baseExecutor.setFunctionObject(ConsoleStrategy.G_LABEL, waldotNamespace.getGremlinGraph());
		baseExecutor.setFunctionObject(ConsoleStrategy.COMMANDS_LABEL, waldotNamespace.getCommandsAsFunction());
		logger.info("ConsoleV0Strategy initialized");
	}

	@Override
	public Object runExpression(String expression) {
		return baseExecutor.execute(expression);
	}

}
