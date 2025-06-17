
package net.rossonet.waldot.jexl;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.rules.ExecutorHelper;
import net.rossonet.waldot.api.rules.Rule;
import net.rossonet.waldot.api.rules.WaldotStepLogger;

public class JexlExecutorHelper implements ExecutorHelper {

	protected static final Logger LOGGER = LoggerFactory.getLogger(JexlExecutorHelper.class);

	private static final boolean DEBUG_FLAG = true;

	protected static JexlEngine generateEngine() {
		// TODO: provare la restrizione dei permessi o la sandbox
		// return new
		// JexlBuilder().permissions(classPermissions).debug(true).silent(false).strict(false).create();
		final JexlEngine j = new JexlBuilder().permissions(JexlPermissions.UNRESTRICTED).debug(DEBUG_FLAG).silent(false)
				.strict(true).create();
		return j;
	}

	protected JexlPermissions classPermissions = new JexlPermissions.ClassPermissions();

	protected final Set<Class<?>> functionObjects = new HashSet<>();

	private JexlEngine jexl = generateEngine();

	protected final JexlContext jexlContext = new MapContext();

	@Override
	public boolean evaluateRule(final WaldotNamespace waldotNamespace, final Rule rule,
			final WaldotStepLogger stepRegister) {
		try {
			// TODO: meccanismo per cache del compilato
			final long startTime = System.currentTimeMillis();
			final JexlScript compiled = jexl.createScript(rule.getCondition());
			final long runnableStartTime = System.currentTimeMillis();
			stepRegister.onConditionCompiled(runnableStartTime - startTime);
			stepRegister.onBeforeConditionExecution(jexlContext);
			final Object result = compiled.execute(jexlContext);
			stepRegister.onAfterConditionExecution(runnableStartTime - startTime, result);
			LOGGER.debug("Rule '{}' evaluated to {}", rule.getCondition(), result);
			return (boolean) result;
		} catch (final Exception e) {
			LOGGER.error("Unable to evaluate rule: '" + rule.getCondition() + "'", e);
			stepRegister.onConditionExecutionException(jexlContext, e);
			return false;
		}
	}

	@Override
	public Object execute(final String expression) {
		try {
			// TODO: meccanismo per cache del compilato
			final JexlScript compiled = jexl.createScript(expression);
			return compiled.execute(jexlContext);
		} catch (final Exception e) {
			LOGGER.error("Unable to execute expression: '" + expression + "'", e);
			throw e;
		}
	}

	@Override
	public Object executeRule(final WaldotNamespace waldotNamespace, final Rule rule,
			final WaldotStepLogger stepRegister) {
		try {
			final long startTime = System.currentTimeMillis();
			final JexlScript compiled = jexl.createScript(rule.getAction());
			final long runnableStartTime = System.currentTimeMillis();
			stepRegister.onActionCompiled(runnableStartTime - startTime);
			stepRegister.onBeforeActionExecution(jexlContext);
			final Object result = compiled.execute(jexlContext);
			stepRegister.onAfterActionExecution(runnableStartTime - startTime, result);
			LOGGER.debug("Rule '{}' executed with result: {}", rule.getAction(), result);
			return result;
		} catch (final Exception e) {
			LOGGER.error("Unable to execute rule: '" + rule.getAction() + "'", e);
			stepRegister.onActionExecutionException(jexlContext, e);
			return null;
		}

	}

	@Override
	public void setContext(final String id, final Object context) {
		jexlContext.set(id, context);
	}

	@Override
	public void setFunctionObject(final String id, final Object function) {
		jexlContext.set(id, function);
		functionObjects.add(function.getClass());
		classPermissions = new JexlPermissions.ClassPermissions(functionObjects.toArray(new Class<?>[0]));
		jexl = generateEngine();
	}

}
