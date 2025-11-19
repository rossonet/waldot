package net.rossonet.zenoh.client.api;

public final class TelemetryUpdate<VALUE_TYPE> {

	private final TelemetryQuality quality;
	private final TelemetryData telemetryData;
	private final long timestamp;
	private final long ttlMs;
	private final VALUE_TYPE value;

	public TelemetryUpdate(TelemetryData telemetryData, VALUE_TYPE value, long timestamp, TelemetryQuality quality,
			long ttlMs) {
		this.telemetryData = telemetryData;
		this.value = value;
		this.timestamp = timestamp;
		this.quality = quality;
		this.ttlMs = ttlMs;
		if (this.ttlMs < 0) {
			throw new IllegalArgumentException("TTL must be non-negative");
		}
		if (this.timestamp < 0) {
			throw new IllegalArgumentException("Timestamp must be non-negative");
		}
		if (this.telemetryData == null) {
			throw new IllegalArgumentException("TelemetryData must be non-null");
		}
		if (this.quality == null) {
			throw new IllegalArgumentException("TelemetryQuality must be non-null");
		}
		if (System.currentTimeMillis() + this.ttlMs < this.timestamp) {
			throw new IllegalArgumentException(
					"TTL and Timestamp are inconsistent because current time + TTL < timestamp ("
							+ System.currentTimeMillis() + " + " + this.ttlMs + " < " + this.timestamp + ")");
		}
	}

	public TelemetryQuality getQuality() {
		return quality;
	}

	public TelemetryData getTelemetryData() {
		return telemetryData;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getTtl() {
		return ttlMs;
	}

	public VALUE_TYPE getValue() {
		return value;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Telemetry [telemetryNode=");
		builder.append(telemetryData);
		builder.append(", value=");
		builder.append(value);
		builder.append(", quality=");
		builder.append(quality);
		builder.append(", timestamp (ms since 1/1/1970)=");
		builder.append(timestamp);
		builder.append(", ttl (ms)=");
		builder.append(ttlMs);
		builder.append("]");
		return builder.toString();
	}

}
