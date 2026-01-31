package net.rossonet.zenoh.exception;

public class ZenohSerializationException extends WaldotZenohException {

	private static final long serialVersionUID = 9185296592462088233L;

	public ZenohSerializationException(String message) {
		super(message, null);
	}

	public ZenohSerializationException(String message, Exception exception) {
		super(message, exception);
	}

}
