package net.rossonet.waldot.api.rules;

public interface ExecutorHelper {

	public enum EvaluationType {
		ATTRIBUTE, PROPERTY
	}

	Object execute(String expression);

	void setContext(String id, Object context);

	void setFunctionObject(String id, Object function);

}
