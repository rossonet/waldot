package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;

import net.rossonet.waldot.api.ExecutorHelper.EvaluationType;

public interface RuleListener {

	default void afterEvaluate(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			boolean condition) {
	}

	default void afterExecute(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			Object executionResult) {
	}

	default boolean beforeEvaluate(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value) {
		return true;
	}

	default void beforeExecute(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value) {
	}

	default void onEvaluationError(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			Throwable exception) {
	}

	default void onFailure(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			Throwable exception) {
	}

	default void onSuccess(EvaluationType evaluationType, UaNode node, AttributeId attributeId, Object value,
			boolean condition) {
	}
}
