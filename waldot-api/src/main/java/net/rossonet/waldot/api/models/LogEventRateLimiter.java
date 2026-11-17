package net.rossonet.waldot.api.models;

import java.util.function.LongSupplier;

/**
 * Limite di frequenza per eventi di log che possono partire tutti insieme da
 * molti vertici (es. la qualita' di migliaia di punti quando cade il campo).
 * Si usa un'istanza condivisa per plugin e per tipo di evento, chiamata solo
 * sulle transizioni (mai per campione).
 * <p>
 * In ogni finestra di un secondo passano al massimo {@code maxPerSecond}
 * eventi; gli altri sono contati. Il primo evento ammesso dopo una finestra
 * con eventi scartati riceve il numero degli scartati, da mandare in un
 * evento di riepilogo prima del proprio.
 */
public final class LogEventRateLimiter {

	/** Esito di {@link #acquire()}. */
	public static final long SUPPRESSED = -1L;

	private static final long WINDOW_MS = 1000L;

	private final LongSupplier clock;
	private long count;
	private final int maxPerSecond;
	private long suppressed;
	private long windowStart = Long.MIN_VALUE;

	public LogEventRateLimiter(int maxPerSecond) {
		this(maxPerSecond, System::currentTimeMillis);
	}

	/** Per i test: orologio in ms. */
	public LogEventRateLimiter(int maxPerSecond, LongSupplier clock) {
		this.maxPerSecond = Math.max(1, maxPerSecond);
		this.clock = clock;
	}

	/**
	 * @return {@link #SUPPRESSED} se l'evento va scartato; altrimenti il
	 *         numero di eventi scartati da riassumere prima di questo (0 se
	 *         nessuno)
	 */
	public synchronized long acquire() {
		final long now = clock.getAsLong();
		if (windowStart == Long.MIN_VALUE || now - windowStart >= WINDOW_MS) {
			windowStart = now;
			count = 0;
		}
		if (count >= maxPerSecond) {
			suppressed++;
			return SUPPRESSED;
		}
		count++;
		final long toReport = suppressed;
		suppressed = 0;
		return toReport;
	}

	/** Eventi scartati non ancora riassunti. */
	public synchronized long getPendingSuppressed() {
		return suppressed;
	}
}
