package net.rossonet.waldot.api.rules;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * ExecutorHelper is an interface that provides methods for evaluating rules,
 * executing expressions, and managing contexts and function objects within a
 * WaldotNamespace. It defines the contract for rule evaluation and execution in
 * the Waldot framework.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface ExecutorHelper extends AutoCloseable {

	public enum EvaluationType {
		ATTRIBUTE, EVENT, PROPERTY
	}

	boolean evaluateRule(WaldotNamespace waldotNamespace, Rule rule, WaldotStepLogger stepRegister);

	Object execute(String expression);

	Object executeRule(WaldotNamespace waldotNamespace, Rule rule, WaldotStepLogger stepRegister);

	void setContext(String id, Object context);

	void setFunctionObject(String id, Object function);

}
