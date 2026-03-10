package net.rossonet.waldot.jexl;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coda concorrente con: 1. Thread–safety (basata su strutture
 * java.util.concurrent). 2. Histeresis: evita l’inserimento di un job se ne
 * esiste già uno uguale inserito meno di hysteresisMs fa. 3. Priorità: i job
 * con valore di priorità minore vengono estratti per primi.
 *
 * @param <J> tipo dell’oggetto messaggio (deve implementare
 *            equals()/hashCode()).
 */
public class HysteresisPriorityQueue<J> {

	/** Classe interna che incapsula job + priorità + timestamp. */
	private static final class PrioritizedMessage<MESSAGE_TYPE>
			implements Comparable<PrioritizedMessage<MESSAGE_TYPE>> {
		private static final AtomicLong SEQ = new AtomicLong();

		final MESSAGE_TYPE payload;
		final int priority;
		final long seq; // utilito per mantenere l’ordine di inserimento tra messaggi con stessa
						// priorità

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

	private final long hysteresisMs;
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
		lastInsertion.forEach((message, ts) -> {
			if (now - ts >= hysteresisMs) {
				lastInsertion.remove(message, ts);
			}
		});
	}

	/**
	 * Rimuove il job estratto dalla mappa lastInsertion, restituendo il payload.
	 */
	private J finalizeDequeue(PrioritizedMessage<J> prioritizedMessage) {
		if (prioritizedMessage == null) {
			return null;
		}
		final J message = prioritizedMessage.payload;
		return message;
	}

	/**
	 * Offre un job con priorità; se respinto per hysteresis restituisce false.
	 * 
	 * @param message  oggetto job
	 * @param priority valore di priorità (più basso = più urgente)
	 */
	/**
	 * Offers a job with priority to the queue.
	 * If a duplicate job exists that was inserted less than hysteresisMs ago, returns false.
	 * 
	 * @param message  the job object to add
	 * @param priority the priority value (lower value = higher urgency)
	 * @return true if the job was added, false if rejected due to hysteresis
	 */
	public boolean offer(J message, int priority) {
		final long now = System.currentTimeMillis();
		if (!lastInsertion.containsKey(message)) {
			lastInsertion.put(message, 0L);
		}
		final long last = lastInsertion.get(message);
		if (now - last >= hysteresisMs) {
			pq.offer(new PrioritizedMessage<>(message, priority));
			lastInsertion.put(message, now);
			return true;
		} else {
			return false;
		}
	}

	/** Estrae (non bloccante) o restituisce null se vuota. */
	/**
	 * Retrieves and removes the highest priority job from the queue, or returns null if empty.
	 * 
	 * @return the job with highest priority, or null if the queue is empty
	 */
	public J poll() {
		final PrioritizedMessage<J> pj = pq.poll();
		return pj == null ? null : finalizeDequeue(pj);
	}

	/** Restituisce il numero di job attualmente in coda. */
	/**
	 * Returns the number of jobs currently in the queue.
	 * 
	 * @return the number of jobs in the queue
	 */
	public int size() {
		return pq.size();
	}

	/**
	 * Rimuove periodicamente (ogni ‘period’) i riferimenti nella mappa
	 * lastInsertion più vecchi di hysteresisMs, per evitare memory leak.
	 */
	/**
	 * Starts a background housekeeping task that periodically removes stale entries
	 * from the internal lastInsertion map to prevent memory leaks.
	 * 
	 * @param period the period between cleanups
	 * @param unit   the time unit for the period
	 */
	public void startHouseKeeping(long period, TimeUnit unit) {
		final ScheduledExecutorService ses = Executors
				.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("HysteresisQueue-HouseKeeper", 0).factory());
		ses.scheduleAtFixedRate(this::cleanUp, period, period, unit);
	}

	/** Estrae (bloccante) il job con priorità più alta (valore minore). */
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