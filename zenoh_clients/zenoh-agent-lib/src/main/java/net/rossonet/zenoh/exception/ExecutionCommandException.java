package net.rossonet.zenoh.exception;

public class ExecutionCommandException extends WaldotZenohException {

	private static final long serialVersionUID = -2347588582772682544L;

	public ExecutionCommandException(String message) {
		super(message, null);
	}

	public ExecutionCommandException(String message, Exception exception) {
		super(message, exception);
	}

}
