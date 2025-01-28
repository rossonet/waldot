package net.rossonet.waldot.gremlin.opcgraph.strategies.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.ConsoleStrategy;
import net.rossonet.waldot.api.ExecutorHelper;
import net.rossonet.waldot.jexl.JexlExecutorHelper;
import net.rossonet.waldot.namespaces.HomunculusNamespace;

public class ConsoleV0Strategy implements ConsoleStrategy {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@SuppressWarnings("unused")
	private HomunculusNamespace waldotNamespace;
	private ExecutorHelper baseExecutor;

	@Override
	public void initialize(HomunculusNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		baseExecutor = new JexlExecutorHelper();
		baseExecutor.setFunctionObject("log", logger);
		baseExecutor.setFunctionObject("g", waldotNamespace.getGremlinGraph());
		logger.info("ConsoleV0Strategy initialized");
	}

	@Override
	public Object runExpression(String expression) {
		return baseExecutor.execute(expression);
	}

}
