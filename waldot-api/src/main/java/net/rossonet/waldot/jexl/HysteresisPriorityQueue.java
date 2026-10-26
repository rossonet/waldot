package net.rossonet.waldot.jexl;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Concurrent queue with: 1. thread safety (based on java.util.concurrent data
 * structures), 2. hysteresis (rejects duplicates inserted within the
 * configured window), 3. priority ordering (lower priority values are served
 * first).
 *
 * @param <J> message type (must implement equals()/hashCode()).
 */
public class HysteresisPriorityQueue<J> {

	/** Internal wrapper that stores payload, priority, and insertion sequence. */
	private static final class PrioritizedMessage<MESSAGE_TYPE>
			implements Comparable<PrioritizedMessage<MESSAGE_TYPE>> {
		private static final AtomicLong SEQ = new AtomicLong();

		final MESSAGE_TYPE payload;
		final int priority;
		final long seq; // Keeps insertion order among items with equal priority.

		private PrioritizedMessage(MESSAGE_TYPE payload, int priority) {
			this.payload = Objects.requireNonNull(payload);
			this.priority = priority;
			this.seq = SEQ.getAndIncrement();
		}

		@Override
		public int compareTo(PrioritizedMessage<MESSAGE_TYPE> other) {
			final int byPrio = Integer.compare(this.priority, other.priority);
			return byPrio != 0 ? byPrio : Long.compare(this.seq, other.seq);
		}
	}

	private volatile long hysteresisMs;
	private volatile ScheduledExecutorService houseKeepingService;
	private final ConcurrentHashMap<J, Long> lastInsertion = new ConcurrentHashMap<>();
	private final PriorityBlockingQueue<PrioritizedMessage<J>> pq = new PriorityBlockingQueue<>();

	/**
	 * Creates a HysteresisPriorityQueue with the specified hysteresis time.
	 * 
	 * @param hysteresisMs the hysteresis time in milliseconds; prevents insertion of duplicate jobs within this time window
	 * @throws IllegalArgumentException if hysteresisMs is negative
	 */
	public HysteresisPriorityQueue(long hysteresisMs) {
		if (hysteresisMs < 0) {
			throw new IllegalArgumentException("hysteresisMs < 0");
		}
		this.hysteresisMs = hysteresisMs;
	}

	/**
	 * Cleans up old entries from the internal lastInsertion map.
	 * Removes entries that are older than hysteresisMs milliseconds.
	 */
	public void cleanUp() {
		final long now = System.currentTimeMillis();
		final long window = hysteresisMs;
		lastInsertion.entrySet().removeIf(entry -> now - entry.getValue() >= window);
	}

	/**
	 * Removes all the queued jobs and the hysteresis history.
	 */
	public void clear() {
		pq.clear();
		lastInsertion.clear();
	}

	/**
	 * Returns the payload of the dequeued wrapper.
	 */
	private J finalizeDequeue(PrioritizedMessage<J> prioritizedMessage) {
		if (prioritizedMessage == null) {
			return null;
		}
		final J message = prioritizedMessage.payload;
		return message;
	}

	/**
	 * Returns the hysteresis time in milliseconds.
	 * 
	 * @return the hysteresis time in milliseconds
	 */
	public long getHysteresisMs() {
		return hysteresisMs;
	}

	/**
	 * Offers a job with priority to the queue.
	 * If a duplicate job exists that was inserted less than hysteresisMs ago, returns false.
	 * The check and the registration of the insertion time are atomic.
	 * 
	 * @param message  the job object to add
	 * @param priority the priority value (lower value = higher urgency)
	 * @return true if the job was added, false if rejected due to hysteresis
	 */
	public boolean offer(J message, int priority) {
		final long now = System.currentTimeMillis();
		final long window = hysteresisMs;
		final boolean[] accepted = new boolean[1];
		lastInsertion.compute(message, (key, last) -> {
			if (last == null || now - last >= window) {
				accepted[0] = true;
				return now;
			}
			return last;
		});
		if (accepted[0]) {
			pq.offer(new PrioritizedMessage<>(message, priority));
		}
		return accepted[0];
	}

	/**
	 * Changes the hysteresis time; the new value is used by the next offers.
	 * 
	 * @param hysteresisMs the hysteresis time in milliseconds
	 * @throws IllegalArgumentException if hysteresisMs is negative
	 */
	public void setHysteresisMs(long hysteresisMs) {
		if (hysteresisMs < 0) {
			throw new IllegalArgumentException("hysteresisMs < 0");
		}
		this.hysteresisMs = hysteresisMs;
	}

	/**
	 * Retrieves and removes the highest priority job from the queue, or returns null if empty.
	 * 
	 * @return the job with highest priority, or null if the queue is empty
	 */
	public J poll() {
		final PrioritizedMessage<J> pj = pq.poll();
		return pj == null ? null : finalizeDequeue(pj);
	}

	/**
	 * Returns the number of jobs currently in the queue.
	 * 
	 * @return the number of jobs in the queue
	 */
	public int size() {
		return pq.size();
	}

	/**
	 * Starts a background housekeeping task that periodically removes stale entries
	 * from the internal lastInsertion map to prevent memory leaks.
	 * 
	 * @param period the period between cleanups
	 * @param unit   the time unit for the period
	 */
	public void startHouseKeeping(long period, TimeUnit unit) {
		if (houseKeepingService != null && !houseKeepingService.isShutdown()) {
			return;
		}
		houseKeepingService = Executors
				.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("HysteresisQueue-HouseKeeper", 0).factory());
		houseKeepingService.scheduleAtFixedRate(this::cleanUp, period, period, unit);
	}

	/**
	 * Stops the running housekeeping task, if any.
	 */
	public void stopHouseKeeping() {
		if (houseKeepingService != null) {
			houseKeepingService.shutdownNow();
			houseKeepingService = null;
		}
	}

	/**
	 * Retrieves and removes the highest priority job from the queue, blocking if necessary.
	 * 
	 * @return the job with highest priority
	 * @throws InterruptedException if interrupted while waiting
	 */
	public J take() throws InterruptedException {
		final PrioritizedMessage<J> pj = pq.take();
		return finalizeDequeue(pj);
	}
}