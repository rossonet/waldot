package net.rossonet.waldot.client.support;

import java.time.Duration;

/**
 * Exponential backoff of the connection attempts: the delay doubles at every
 * failed attempt, from the initial delay to the maximum delay.
 *
 * @param initialDelay delay before the first new attempt (ms, > 0)
 * @param maxDelay     maximum delay (ms, >= initialDelay)
 * @param maxAttempts  consecutive failed attempts before giving up, 0 for no
 *                     limit
 */
public record ReconnectBackoff(long initialDelay, long maxDelay, int maxAttempts) {

	public ReconnectBackoff {
		if (initialDelay <= 0) {
			throw new IllegalArgumentException("initialDelay must be > 0");
		}
		if (maxDelay < initialDelay) {
			throw new IllegalArgumentException("maxDelay must be >= initialDelay");
		}
		if (maxAttempts < 0) {
			throw new IllegalArgumentException("maxAttempts must be >= 0");
		}
	}

	/**
	 * @param failedAttempts number of consecutive failed attempts (>= 1)
	 * @return the delay before the next attempt
	 */
	public Duration delay(final int failedAttempts) {
		final int exponent = Math.max(0, Math.min(failedAttempts - 1, 30));
		final long delay = initialDelay << exponent;
		return Duration.ofMillis(delay <= 0 || delay > maxDelay ? maxDelay : delay);
	}

	/**
	 * @param failedAttempts number of consecutive failed attempts
	 * @return true if the client must give up
	 */
	public boolean exhausted(final int failedAttempts) {
		return maxAttempts > 0 && failedAttempts >= maxAttempts;
	}
}
