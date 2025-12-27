
package net.rossonet.waldot.jexl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.rules.ClonableMapContext;
import net.rossonet.waldot.api.rules.Rule;
import net.rossonet.waldot.api.rules.RuleExecutorHelper;
import net.rossonet.waldot.api.rules.WaldotStepLogger;
import net.rossonet.waldot.utils.LogHelper;

public class JexlExecutorHelper implements RuleExecutorHelper {

	private static final boolean DEBUG_FLAG = true;

	protected static final Logger LOGGER = LoggerFactory.getLogger("JEXEL EXECUTOR");

	protected static JexlEngine generateEngine() {
		// XXX: provare la restrizione dei permessi o la sandbox
		// return new
		// JexlBuilder().permissions(classPermissions).debug(true).silent(false).strict(false).create();
		final JexlEngine j = new JexlBuilder().permissions(JexlPermissions.UNRESTRICTED).debug(DEBUG_FLAG).silent(false)
				.strict(true).create();
		return j;
	}

	protected final ClonableMapContext baseJexlContext = new ClonableMapContext();

	protected JexlPermissions classPermissions = new JexlPermissions.ClassPermissions();

	protected transient Map<String, JexlScript> compiledActions = new HashMap<>();

	protected transient Map<String, JexlScript> compiledConditions = new HashMap<>();

	private final List<String> functionList = new ArrayList<>();

	protected final Set<Class<?>> functionObjects = new HashSet<>();

	private JexlEngine jexl = generateEngine();
	private final String name;

	public JexlExecutorHelper(String name) {
		this.name = name;
	}

	@Override
	public void close() throws Exception {
		compiledConditions.clear();
		compiledActions.clear();
		jexl.clearCache();
		jexl = null;
	}

	@Override
	public boolean evaluateRule(final WaldotNamespace waldotNamespace, final Rule rule,
			final WaldotStepLogger stepRegister) {
		try {
			final long startTime = System.currentTimeMillis();
			boolean compiledJob = false;
			if (!compiledConditions.containsKey(rule.getCondition())) {
				final JexlScript compiled = jexl.createScript(rule.getCondition());
				compiledConditions.put(rule.getCondition(), compiled);
				compiledJob = true;
			}
			final long runnableStartTime = System.currentTimeMillis();
			if (compiledJob) {
				stepRegister.onConditionCompiled(runnableStartTime - startTime);
			}
			stepRegister.onBeforeConditionExecution(rule.getJexlContext(baseJexlContext));
			final Object result = compiledConditions.get(rule.getCondition())
					.execute(rule.getJexlContext(baseJexlContext));
			stepRegister.onAfterConditionExecution(runnableStartTime - startTime, result);
			LOGGER.debug("Rule '{}' evaluated to {}", rule.getCondition(), result);
			return (boolean) result;
		} catch (final Exception e) {
			LOGGER.error(
					"Unable to evaluate rule: '" + rule.getCondition() + "\n" + LogHelper.stackTraceToString(e, 4));
			stepRegister.onConditionExecutionException(rule.getJexlContext(baseJexlContext), e);
			return false;
		}
	}

	@Override
	public Object execute(final String expression) {
		return execute(expression, baseJexlContext);
	}

	@Override
	public Object execute(final String expression, final JexlContext jexlContext) {
		try {
			final JexlScript compiled = jexl.createScript(expression);
			return compiled.execute(jexlContext);
		} catch (final Exception e) {
			LOGGER.error("Unable to execute expression: '" + expression + "' > " + e.getMessage() + "\n"
					+ LogHelper.stackTraceToString(e, 4));
			throw e;
		}
	}

	@Override
	public Object executeRule(final WaldotNamespace waldotNamespace, final Rule rule,
			final WaldotStepLogger stepRegister) {
		try {
			final long startTime = System.currentTimeMillis();

			boolean compiledJob = false;
			if (!compiledActions.containsKey(rule.getAction())) {
				final JexlScript compiled = jexl.createScript(rule.getAction());
				compiledActions.put(rule.getAction(), compiled);
				compiledJob = true;
			}
			final long runnableStartTime = System.currentTimeMillis();
			if (compiledJob) {
				stepRegister.onActionCompiled(runnableStartTime - startTime);
			}
			stepRegister.onBeforeActionExecution(rule.getJexlContext(baseJexlContext));
			final Object result = compiledActions.get(rule.getAction()).execute(rule.getJexlContext(baseJexlContext));
			stepRegister.onAfterActionExecution(runnableStartTime - startTime, result);
			LOGGER.debug("Rule '{}' executed with result: {}", rule.getAction(), result);
			return result;
		} catch (final Exception e) {
			LOGGER.error("Unable to execute rule: '" + rule.getAction() + "\n" + LogHelper.stackTraceToString(e, 4));
			stepRegister.onActionExecutionException(rule.getJexlContext(baseJexlContext), e);
			return null;
		}

	}

	public String getName() {
		return name;
	}

	@Override
	public void setContext(final String id, final Object context) {
		baseJexlContext.set(id, context);
	}

	@Override
	public void setFunctionObject(final String id, final Object function) {
		functionList.add(id);
		baseJexlContext.set(id, function);
		functionObjects.add(function.getClass());
		classPermissions = new JexlPermissions.ClassPermissions(functionObjects.toArray(new Class<?>[0]));
		jexl = generateEngine();
	}

}
