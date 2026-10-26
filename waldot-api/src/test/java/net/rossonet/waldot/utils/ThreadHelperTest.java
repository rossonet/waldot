package net.rossonet.waldot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class ThreadHelperTest {

	@Test
	void testRunWithTimeoutCompletesInTime() throws Exception {
		final Callable<String> task = () -> {
			Thread.sleep(1); // Simulate a task that takes 500ms
			return "Completed";
		};

		final String result = ThreadHelper.runWithTimeout(task, 1, TimeUnit.SECONDS);
		assertEquals("Completed", result);
	}

	@Test
	void testRunWithTimeoutExceedsTime() {
		final Callable<String> task = () -> {
			Thread.sleep(2_000);
			return "Completed";
		};

		assertThrows(TimeoutException.class, () -> ThreadHelper.runWithTimeout(task, 1, TimeUnit.SECONDS));
	}

	@Test
	void testRunWithTimeoutPropagatesTaskException() {
		final Callable<String> task = () -> {
			throw new IllegalStateException("boom");
		};

		final Exception exception = assertThrows(IllegalStateException.class,
				() -> ThreadHelper.runWithTimeout(task, 1, TimeUnit.SECONDS));
		assertEquals("boom", exception.getMessage());
	}
}