package net.rossonet.zenoh.api;

import net.rossonet.zenoh.api.message.TelemetryMessage;

public final class InternalLogMessage {
	public enum MessageType {
		ERROR, INFO, TELEMETRY
	}

	public final Throwable exception;
	public final String message;
	private TelemetryMessage<?> telemetry;
	private final MessageType type;

	public InternalLogMessage(MessageType type, String message, Throwable exception) {
		this.type = type;
		this.message = message;
		this.exception = exception;
	}

	public InternalLogMessage(MessageType type, TelemetryMessage<?> telemetry) {
		this.exception = null;
		this.type = type;
		this.telemetry = telemetry;
		this.message = null;
	}

	public Throwable getException() {
		return exception;
	}

	public String getMessage() {
		return message;
	}

	public TelemetryMessage<?> getTelemetry() {
		return telemetry;
	}

	public MessageType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "InternalLogMessage [message=" + message + ", exception=" + exception + "]";
	}
}