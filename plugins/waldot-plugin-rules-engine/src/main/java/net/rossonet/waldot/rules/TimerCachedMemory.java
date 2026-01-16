package net.rossonet.waldot.rules;

import java.time.Instant;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.jexl.CachedRuleRecord;
import net.rossonet.waldot.jexl.Fact;

public class TimerCachedMemory implements CachedRuleRecord {
	private final long createdAt;
	private final Fact fact;

	private final NodeId factSource;

	private final long validDelayMs;
	private final long validUntilMs;

	public TimerCachedMemory(final NodeId factSource, final long validDelayMs, final long validUntilMs,
			final Fact fact) {
		this.validDelayMs = validDelayMs;
		this.validUntilMs = validUntilMs;
		this.fact = fact;
		this.factSource = factSource;
		this.createdAt = Instant.now().toEpochMilli();
	}

	@Override
	public long getCreatedAt() {
		return createdAt;
	}

	@Override
	public Fact getFact() {
		return fact;
	}

	@Override
	public NodeId getFactSource() {
		return factSource;
	}

	@Override
	public long getValidDelayMs() {
		return validDelayMs;
	}

	@Override
	public long getValidUntilMs() {
		return validUntilMs;
	}

	@Override
	public boolean isExpired() {
		if (validUntilMs != -1 && createdAt + validUntilMs < Instant.now().toEpochMilli()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isValidNow() {
		if (validDelayMs != -1 && createdAt + validDelayMs > Instant.now().toEpochMilli()) {
			return false;
		}
		if (isExpired()) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Fact [");
		if (factSource != null) {
			builder.append("factSource=");
			builder.append(factSource);
			builder.append(", ");
		}
		builder.append("createdAt=");
		builder.append(createdAt);
		builder.append(", validDelayMs=");
		builder.append(validDelayMs);
		builder.append(", validUntilMs=");
		builder.append(validUntilMs);
		builder.append(", ");
		if (fact != null) {
			builder.append("fact=");
			builder.append(fact);
		}
		builder.append("]");
		return builder.toString();
	}

}
