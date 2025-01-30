package net.rossonet.waldot.api.rules;

import org.eclipse.milo.opcua.sdk.server.model.types.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;

import net.rossonet.waldot.api.models.WaldotNamespace;

public interface ExecutorHelper {

	public enum EvaluationType {
		ATTRIBUTE, PROPERTY, EVENT
	}

	boolean evaluateRule(WaldotNamespace waldotNamespace, Rule rule, EvaluationType evaluationType, UaNode node,
			AttributeId attributeId, Object value, BaseEventType event);

	Object execute(String expression);

	Object executeRule(WaldotNamespace waldotNamespace, Rule rule, EvaluationType evalutationType, UaNode node,
			AttributeId attributeId, Object value, BaseEventType event);

	void setContext(String id, Object context);

	void setFunctionObject(String id, Object function);

}
