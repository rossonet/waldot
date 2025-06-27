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
			Thread.sleep(500); // Simulate a task that takes 500ms
			return "Completed";
		};

		final String result = ThreadHelper.runWithTimeout(task, 1, TimeUnit.SECONDS);
		assertEquals("Completed", result);
	}

	@Test
	void testRunWithTimeoutExceedsTime() {
		final Callable<String> task = () -> {
			Thread.sleep(2000); // Simulate a task that takes 2 seconds
			return "Completed";
		};

		assertThrows(TimeoutException.class, () -> ThreadHelper.runWithTimeout(task, 1, TimeUnit.SECONDS));
	}
}