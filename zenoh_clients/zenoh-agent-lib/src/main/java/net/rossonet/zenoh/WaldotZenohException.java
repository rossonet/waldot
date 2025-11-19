package net.rossonet.zenoh;

public class WaldotZenohException extends Exception {

	private static final long serialVersionUID = -2953355191902233057L;

	public WaldotZenohException(String message, Exception exception) {
		super(message, exception);
	}

}
