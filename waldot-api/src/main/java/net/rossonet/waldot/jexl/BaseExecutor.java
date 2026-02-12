package net.rossonet.waldot.jexl;

import org.apache.commons.jexl3.JexlContext;

public interface BaseExecutor extends AutoCloseable {

	Object execute(String expression);

	Object execute(String expression, JexlContext jexlContext);

	void addOrUpdateContext(String id, Object context);

	void setFunctionObject(String label, Object functionalObject);

}
