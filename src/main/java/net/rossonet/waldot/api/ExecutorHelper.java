package net.rossonet.waldot.api;

public interface ExecutorHelper {

	public enum EvaluationType {
		ATTRIBUTE, PROPERTY
	}

	Object execute(String expression);

	void setContext(String id, Object context);

	void setFunctionObject(String id, Object function);

}
