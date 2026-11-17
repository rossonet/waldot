package net.rossonet.waldot.api.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

public class LogEventRateLimiterTest {

	@Test
	public void suppressesOverLimitAndReportsInNextWindow() {
		final AtomicLong now = new AtomicLong(10_000);
		final LogEventRateLimiter limiter = new LogEventRateLimiter(3, now::get);
		for (int i = 0; i < 3; i++) {
			assertEquals(0, limiter.acquire());
		}
		for (int i = 0; i < 5; i++) {
			assertEquals(LogEventRateLimiter.SUPPRESSED, limiter.acquire());
		}
		assertEquals(5, limiter.getPendingSuppressed());
		// stessa finestra: ancora scartato
		now.addAndGet(999);
		assertEquals(LogEventRateLimiter.SUPPRESSED, limiter.acquire());
		// finestra nuova: il primo ammesso riceve il conteggio
		now.addAndGet(1);
		assertEquals(6, limiter.acquire());
		assertEquals(0, limiter.acquire());
		assertEquals(0, limiter.getPendingSuppressed());
	}
}
