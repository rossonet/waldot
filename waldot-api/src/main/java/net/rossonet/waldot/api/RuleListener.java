package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;

import net.rossonet.waldot.jexl.CachedRuleRecord;
import net.rossonet.waldot.jexl.WaldotStepLogger;

/**
 * RuleListener interface for handling events related to rule evaluation and
 * execution. Implementations of this interface should define how to handle rule
 * lifecycle events, including evaluation, execution, success, failure, and
 * attribute changes.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface RuleListener {

	default void afterEvaluate(final WaldotStepLogger stepRegister, final boolean condition) {
	}

	default void afterExecute(final WaldotStepLogger stepRegister, final Object executionResult) {
	}

	default boolean beforeEvaluate(final WaldotStepLogger stepRegister) {
		return true;
	}

	default void beforeExecute(final WaldotStepLogger stepRegister) {
	}

	default void onActionError(final WaldotStepLogger stepRegister, final Throwable exception) {
	}

	default void onAttributeChange(final UaNode node, final AttributeId attributeId, final Object value) {

	}

	default void onEvaluationError(final WaldotStepLogger stepRegister, final Throwable exception) {
	}

	default void onEventFired(final UaNode node, final BaseEventType event) {

	}

	default void onFactExpired(final CachedRuleRecord expiredFact) {

	}

	default void onFailure(final WaldotStepLogger stepRegister, final Throwable exception) {
	}

	default void onSuccess(final WaldotStepLogger stepRegister, final boolean condition, final Object executionResult) {
	}
}
