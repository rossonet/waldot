package net.rossonet.waldot.api;

public interface LoggerListener {

	void onEvent(String messagePattern, Object[] arguments, Throwable throwable);

}
