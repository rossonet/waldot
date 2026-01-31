package net.rossonet.zenoh.api.message;

public enum TelemetryQuality {

	BAD(1), ENGINEERING_UNITS_EXCEEDED(11), GOOD(0), OUT_OF_RANGE(12), OVERFLOW(13), SENSOR_FAILURE(14), SUB_NORMAL(15),
	UNCERTAIN(3);

	static TelemetryQuality fromCode(int code) {
		for (final TelemetryQuality quality : TelemetryQuality.values()) {
			if (quality.getCode() == code) {
				return quality;
			}
		}
		throw new IllegalArgumentException("Unknown TelemetryQuality code: " + code);
	}

	private final int code;

	TelemetryQuality(int code) {
		this.code = code;
	}

	int getCode() {
		return code;
	}
}
