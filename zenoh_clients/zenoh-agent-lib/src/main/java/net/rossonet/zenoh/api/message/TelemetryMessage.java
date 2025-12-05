package net.rossonet.zenoh.api.message;

import org.json.JSONObject;

public final class TelemetryMessage<VALUE_TYPE> {

	private static final String TELEMETRY_ID_FIELD = "i";
	private static final String TELEMETRY_QUALITY_FIELD = "q";
	private static final String TELEMETRY_TIMESTAMP_FIELD = "d";
	private static final String TELEMETRY_TTL_FIELD = "l";
	private static final String TELEMETRY_TYPE_FIELD = "t";
	private static final String TELEMETRY_VALUE_FIELD = "v";

	public static final TelemetryMessage<?> fromJson(JSONObject jsonObject) {
		final long id = jsonObject.getLong(TELEMETRY_ID_FIELD);
		final String type = jsonObject.getString(TELEMETRY_TYPE_FIELD);
		final Object v = jsonObject.get(TELEMETRY_VALUE_FIELD);
		final int q = jsonObject.getInt(TELEMETRY_QUALITY_FIELD);
		final long time = jsonObject.getLong(TELEMETRY_TIMESTAMP_FIELD);
		final long ttl = jsonObject.getLong(TELEMETRY_TTL_FIELD);
		switch (type) {
		case "java.lang.Long":
			return new TelemetryMessage<Long>(id, (Long) v, time, TelemetryQuality.fromCode(q), ttl);
		case "java.lang.Integer":
			return new TelemetryMessage<Integer>(id, (Integer) v, time, TelemetryQuality.fromCode(q), ttl);
		case "java.lang.String":
			return new TelemetryMessage<String>(id, (String) v, time, TelemetryQuality.fromCode(q), ttl);
//TODO: completare tipo dati
		}
		return null;

	}

	private final TelemetryQuality quality;
	private final long telemetryDataId;
	private final long timestamp;
	private final long ttlMs;

	private final VALUE_TYPE value;

	public TelemetryMessage(long telemetryDataId, VALUE_TYPE value, long timestamp, TelemetryQuality quality,
			long ttlMs) {
		this.telemetryDataId = telemetryDataId;
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
		if (this.telemetryDataId < 1) {
			throw new IllegalArgumentException("TelemetryDataId must be major of 1");
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

	public long getTelemetryDataId() {
		return telemetryDataId;
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

	public JSONObject toJson() {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put(TELEMETRY_ID_FIELD, telemetryDataId);
		jsonObject.put(TELEMETRY_TYPE_FIELD, value.getClass().getCanonicalName());
		jsonObject.put(TELEMETRY_VALUE_FIELD, valueToJson());
		jsonObject.put(TELEMETRY_QUALITY_FIELD, quality.getCode());
		jsonObject.put(TELEMETRY_TIMESTAMP_FIELD, timestamp);
		jsonObject.put(TELEMETRY_TTL_FIELD, ttlMs);
		return jsonObject;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Telemetry [telemetryNode=");
		builder.append(telemetryDataId);
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

	private Object valueToJson() {
		final String canonical = value.getClass().getCanonicalName();
		if ("java.lang.Long".equals(canonical) && "java.lang.Integer".equals(canonical)
				&& "java.lang.String".equals(canonical)) {
			return value;
		} else {
			// TODO: completa i tipi dato
			return null;
		}
	}
}
