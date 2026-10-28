package net.rossonet.waldot.cli.support;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picocli.CommandLine.ITypeConverter;

/**
 * Parser of durations: {@code 500ms}, {@code 10s}, {@code 2m}, {@code 1h},
 * {@code 1d}, combinations like {@code 1m30s}, ISO-8601 ({@code PT10S}) or a
 * plain number of seconds ({@code 10}, {@code 2.5}).
 */
public final class Durations implements ITypeConverter<Duration> {

	private static final Pattern PART = Pattern.compile("(\\d+(?:\\.\\d+)?)(ms|s|m|h|d)");
	private static final Pattern PLAIN = Pattern.compile("\\d+(?:\\.\\d+)?");

	/**
	 * @throws IllegalArgumentException if the text is not a valid positive or
	 *                                  zero duration
	 */
	public static Duration parse(final String text) {
		if (text == null || text.isBlank()) {
			throw new IllegalArgumentException("durata vuota");
		}
		final String value = text.trim().toLowerCase(Locale.ROOT);
		if (value.startsWith("p")) {
			try {
				return nonNegative(Duration.parse(value.toUpperCase(Locale.ROOT)), text);
			} catch (final RuntimeException e) {
				throw new IllegalArgumentException("durata non valida: '" + text + "'", e);
			}
		}
		if (PLAIN.matcher(value).matches()) {
			return nanos(Double.parseDouble(value) * 1e9);
		}
		final Matcher matcher = PART.matcher(value);
		int position = 0;
		double totalNanos = 0;
		while (position < value.length()) {
			if (!matcher.find(position) || matcher.start() != position) {
				throw new IllegalArgumentException("durata non valida: '" + text
						+ "' (esempi: 500ms, 10s, 2m, 1h, 1m30s, PT10S, 10)");
			}
			final double amount = Double.parseDouble(matcher.group(1));
			totalNanos += amount * switch (matcher.group(2)) {
			case "ms" -> 1e6;
			case "s" -> 1e9;
			case "m" -> 60e9;
			case "h" -> 3600e9;
			default -> 86400e9;
			};
			position = matcher.end();
		}
		return nanos(totalNanos);
	}

	private static Duration nanos(final double value) {
		return Duration.ofNanos((long) Math.min(Long.MAX_VALUE, value));
	}

	private static Duration nonNegative(final Duration duration, final String text) {
		if (duration.isNegative()) {
			throw new IllegalArgumentException("durata negativa: '" + text + "'");
		}
		return duration;
	}

	@Override
	public Duration convert(final String value) {
		return parse(value);
	}
}
