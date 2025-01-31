package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.model.types.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;

import net.rossonet.waldot.api.rules.CachedRuleRecord;
import net.rossonet.waldot.api.rules.WaldotStepLogger;

public interface RuleListener {

	default void afterEvaluate(WaldotStepLogger stepRegister, boolean condition) {
	}

	default void afterExecute(WaldotStepLogger stepRegister, Object executionResult) {
	}

	default boolean beforeEvaluate(WaldotStepLogger stepRegister) {
		return true;
	}

	default void beforeExecute(WaldotStepLogger stepRegister) {
	}

	default void onActionError(WaldotStepLogger stepRegister, Throwable exception) {
	}

	default void onAttributeChange(UaNode node, AttributeId attributeId, Object value) {

	}

	default void onEvaluationError(WaldotStepLogger stepRegister, Throwable exception) {
	}

	default void onEventFired(UaNode node, BaseEventType event) {

	}

	default void onFactExpired(CachedRuleRecord expiredFact) {

	}

	default void onFailure(WaldotStepLogger stepRegister, Throwable exception) {
	}

	default void onSuccess(WaldotStepLogger stepRegister, boolean condition, Object executionResult) {
	}
}
