package net.rossonet.waldot.api.jexel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.jexl.HysteresisPriorityQueue;

class HysteresisPriorityQueueTest {

	@Test
	void testPriorityAndOrder() {
		final HysteresisPriorityQueue<String> queue = new HysteresisPriorityQueue<>(0);
		assertTrue(queue.offer("low", 10));
		assertTrue(queue.offer("high", 1));
		assertTrue(queue.offer("high-2", 1));

		assertEquals("high", queue.poll());
		assertEquals("high-2", queue.poll());
		assertEquals("low", queue.poll());
		assertNull(queue.poll());
	}

	@Test
	void testHysteresisWindowRejectsDuplicate() throws InterruptedException {
		final HysteresisPriorityQueue<String> queue = new HysteresisPriorityQueue<>(50);
		assertTrue(queue.offer("job", 1));
		assertFalse(queue.offer("job", 1));
		Thread.sleep(60);
		assertTrue(queue.offer("job", 1));
	}

	@Test
	void testCleanUpRemovesExpiredInsertions() throws InterruptedException {
		final HysteresisPriorityQueue<String> queue = new HysteresisPriorityQueue<>(30);
		assertTrue(queue.offer("job", 1));
		Thread.sleep(40);
		queue.cleanUp();
		assertTrue(queue.offer("job", 1));
	}

	@Test
	void testHouseKeepingLifecycle() throws InterruptedException {
		final HysteresisPriorityQueue<String> queue = new HysteresisPriorityQueue<>(10);
		queue.startHouseKeeping(1, TimeUnit.MILLISECONDS);
		queue.startHouseKeeping(1, TimeUnit.MILLISECONDS);
		assertTrue(queue.offer("job", 1));
		Thread.sleep(20);
		assertTrue(queue.offer("job", 1));
		queue.stopHouseKeeping();
	}
}
