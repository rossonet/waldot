package net.rossonet.waldot.api.rules;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * CachedRuleRecord is an interface that represents a record of a rule in the
 * Waldot system. It provides methods to access the creation time, fact, source
 * of the fact, validity delay, validity until time, and methods to check if the
 * record is expired or valid now.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface CachedRuleRecord {

	long getCreatedAt();

	Fact getFact();

	NodeId getFactSource();

	long getValidDelayMs();

	long getValidUntilMs();

	boolean isExpired();

	boolean isValidNow();

}
