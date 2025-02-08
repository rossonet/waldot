package net.rossonet.waldot.api.rules;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public interface CachedRuleRecord {

	long getCreatedAt();

	Fact getFact();

	NodeId getFactSource();

	long getValidDelayMs();

	long getValidUntilMs();

	boolean isExpired();

	boolean isValidNow();

}
