package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.model.types.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;

import net.rossonet.waldot.api.rules.ExecutorHelper.EvaluationType;

public interface RuleListener {

	default void afterEvaluate(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			BaseEventType event, boolean condition) {
	}

	default void afterExecute(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			BaseEventType event, Object executionResult) {
	}

	default boolean beforeEvaluate(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			BaseEventType event) {
		return true;
	}

	default void beforeExecute(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			BaseEventType event) {
	}

	default void onEvaluationError(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			BaseEventType event, Throwable exception) {
	}

	default void onFailure(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			BaseEventType event, Throwable exception) {
	}

	default void onSuccess(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			BaseEventType event, boolean condition) {
	}
}
