package net.rossonet.waldot.api;

/**
 * LoggerListener is an interface for listening to logging events. It provides a
 * method to handle log messages, including a message pattern, arguments for the
 * message, and an optional throwable for error handling.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface LoggerListener {

	void onEvent(String messagePattern, Object[] arguments, Throwable throwable);

}
