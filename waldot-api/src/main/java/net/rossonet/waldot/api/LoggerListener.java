package net.rossonet.waldot.api;

/**
 * LoggerListener is an interface for listening to logging events. It provides a
 * method to handle log messages, including a message pattern, arguments for the
 * message, and an optional throwable for error handling.
 * 
 * <p>LoggerListener provides a callback mechanism for logging events in the
 * WaldOT system. Implementations can capture log messages for external
 * logging, analysis, or filtering.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public class CustomLoggerListener implements LoggerListener {
 *     @Override
 *     public void onEvent(String messagePattern, Object[] arguments, Throwable throwable) {
 *         String message = MessageFormat.format(messagePattern, arguments);
 *         
 *         if (throwable != null) {
 *             System.err.println("ERROR: " + message);
 *             throwable.printStackTrace();
 *         } else {
 *             System.out.println("INFO: " + message);
 *         }
 *     }
 * }
 * 
 * // Register with logger
 * logger.addListener(new CustomLoggerListener());
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface LoggerListener {

	/**
	 * Called when a log event occurs.
	 * 
	 * <p>This callback receives log messages with optional format arguments
	 * and optional throwable for error conditions.</p>
	 * 
	 * @param messagePattern the message pattern (can use MessageFormat syntax)
	 * @param arguments the arguments to substitute into the pattern
	 * @param throwable optional Throwable if this is an error log
	 */
	void onEvent(String messagePattern, Object[] arguments, Throwable throwable);

}
