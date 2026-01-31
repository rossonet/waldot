package net.rossonet.zenoh.exception;

public class WaldotZenohException extends Exception {

	private static final long serialVersionUID = -2953355191902233057L;

	public WaldotZenohException(String message, Throwable exception) {
		super(message, exception);
	}

	public WaldotZenohException(String message, Throwable exception, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, exception, enableSuppression, writableStackTrace);
	}

}
