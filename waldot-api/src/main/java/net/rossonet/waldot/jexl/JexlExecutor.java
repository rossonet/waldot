
package net.rossonet.waldot.jexl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.utils.LogHelper;

public class JexlExecutor implements BaseExecutor {

	private static final boolean DEBUG_FLAG = true;

	protected static final Logger LOGGER = LoggerFactory.getLogger("JEXEL");

	public static JexlEngine generateEngine() {
		// XXX: provare la restrizione dei permessi o la sandbox
		// return new
		// JexlBuilder().permissions(classPermissions).debug(true).silent(false).strict(false).create();
		final JexlEngine j = new JexlBuilder().permissions(JexlPermissions.UNRESTRICTED).debug(DEBUG_FLAG).silent(false)
				.strict(true).create();
		return j;
	}

	protected final ClonableMapContext baseJexlContext = new ClonableMapContext();

	protected JexlPermissions classPermissions = new JexlPermissions.ClassPermissions();

	private final List<String> functionList = new ArrayList<>();

	protected final Set<Class<?>> functionObjects = new HashSet<>();

	private JexlEngine jexl = generateEngine();
	private final String name;

	public JexlExecutor(String name) {
		this.name = name;
	}

	@Override
	public void addOrUpdateContext(final String id, final Object context) {
		baseJexlContext.set(id, context);
	}

	@Override
	public void close() throws Exception {
		jexl.clearCache();
		jexl = null;
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

	public String getName() {
		return name;
	}

	@Override
	public void setFunctionObject(final String id, final Object function) {
		functionList.add(id);
		baseJexlContext.set(id, function);
		functionObjects.add(function.getClass());
		classPermissions = new JexlPermissions.ClassPermissions(functionObjects.toArray(new Class<?>[0]));
		// jexl = generateEngine();
	}

}
