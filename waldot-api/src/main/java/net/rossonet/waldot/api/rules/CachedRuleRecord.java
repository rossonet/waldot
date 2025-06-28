package net.rossonet.waldot.api.rules;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * The {@code CachedRuleRecord} interface represents a record of a rule in the
 * Waldot system. It provides methods to access metadata and state information
 * about a rule, including:
 * <ul>
 * <li>Creation time of the rule record.</li>
 * <li>The associated {@link Fact} that triggered the rule.</li>
 * <li>The source of the fact as a {@link NodeId}.</li>
 * <li>Validity periods, including delay and expiration times.</li>
 * <li>Status checks to determine if the rule is expired or currently
 * valid.</li>
 * </ul>
 *
 * <p>
 * This interface is a core component of the rule engine, enabling efficient
 * tracking and management of rule execution states.
 * </p>
 *
 * @see Fact
 * @see NodeId
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface CachedRuleRecord {

	/**
	 * Retrieves the creation timestamp of the rule record.
	 *
	 * @return the creation time in milliseconds since the epoch.
	 */
	long getCreatedAt();

	/**
	 * Retrieves the {@link Fact} associated with this rule record.
	 *
	 * @return the fact that triggered the rule.
	 */
	Fact getFact();

	/**
	 * Retrieves the source of the fact as a {@link NodeId}.
	 *
	 * @return the source of the fact.
	 */
	NodeId getFactSource();

	/**
	 * Retrieves the validity delay of the rule in milliseconds.
	 *
	 * @return the validity delay in milliseconds.
	 */
	long getValidDelayMs();

	/**
	 * Retrieves the expiration time of the rule in milliseconds.
	 *
	 * @return the expiration time in milliseconds.
	 */
	long getValidUntilMs();

	/**
	 * Checks whether the rule record is expired.
	 *
	 * @return {@code true} if the rule is expired, {@code false} otherwise.
	 */
	boolean isExpired();

	/**
	 * Checks whether the rule record is currently valid.
	 *
	 * @return {@code true} if the rule is valid now, {@code false} otherwise.
	 */
	boolean isValidNow();

}