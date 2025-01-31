package net.rossonet.waldot.api.rules;

import net.rossonet.waldot.api.models.WaldotNamespace;

public interface ExecutorHelper {

	public enum EvaluationType {
		ATTRIBUTE, EVENT, PROPERTY
	}

	boolean evaluateRule(WaldotNamespace waldotNamespace, Rule rule, WaldotStepLogger stepRegister);

	Object execute(String expression);

	Object executeRule(WaldotNamespace waldotNamespace, Rule rule, WaldotStepLogger stepRegister);

	void setContext(String id, Object context);

	void setFunctionObject(String id, Object function);

}
