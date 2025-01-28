
package net.rossonet.waldot.jexl;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.rules.ExecutorHelper;

public class JexlExecutorHelper implements ExecutorHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(JexlExecutorHelper.class);

	private static JexlEngine jexl = generateEngine();

	private static JexlEngine generateEngine() {
		// TODO: provare la restrizione dei permessi o la sandbox
		// return new
		// JexlBuilder().permissions(classPermissions).debug(true).silent(false).strict(false).create();
		final JexlEngine j = new JexlBuilder().permissions(JexlPermissions.UNRESTRICTED).debug(false).silent(false)
				.strict(true).create();
		return j;
	}

	public static JexlExecutorHelper getInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unused")
	private JexlPermissions classPermissions = new JexlPermissions.ClassPermissions();

	private final JexlContext jexlContext = new MapContext();

	private final Set<Class<?>> functionObjects = new HashSet<>();

	public boolean evaluateRule(WaldotNamespace waldotNamespace, EvaluationType evaluationType, UaNode node,
			AttributeId attributeId, Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object execute(String expression) {
		try {
			final JexlScript compiled = jexl.createScript(expression);
			return compiled.execute(jexlContext);
		} catch (final Exception e) {
			LOGGER.error("Unable to execute expression: '" + expression + "'", e);
			throw e;
		}
	}

	public Object executeRule(WaldotNamespace waldotNamespace, EvaluationType attribute, UaNode node,
			AttributeId attributeId, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setContext(String id, Object context) {
		jexlContext.set(id, context);
	}

	@Override
	public void setFunctionObject(String id, Object function) {
		jexlContext.set(id, function);
		functionObjects.add(function.getClass());
		// TODO trovare un modo per ottimizzare gli engine in funzione delle classi
		// abilitate
		classPermissions = new JexlPermissions.ClassPermissions(functionObjects.toArray(new Class<?>[0]));
		jexl = generateEngine();
	}

}
