package net.rossonet.waldot.logger;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;

import net.rossonet.waldot.api.LoggerListener;

public class TraceLogger extends AbstractLogger {

	public enum ContexLogger {
		CONSOLE, RULES
	}

	private static final long serialVersionUID = 1831476351399997851L;

	private boolean debug = false;
	private final List<LoggerListener> debugObservers = new ArrayList<>();

	private final List<LoggerListener> errorObservers = new ArrayList<>();

	private final List<LoggerListener> infoObservers = new ArrayList<>();
	private final Logger logger;
	private final List<LoggerListener> traceObservers = new ArrayList<>();
	private final ContexLogger type;
	private final List<LoggerListener> warnObservers = new ArrayList<>();

	public TraceLogger(final ContexLogger type) {
		this.type = type;
		this.logger = LoggerFactory.getLogger(type.name());
	}

	public List<LoggerListener> getDebugObservers() {
		return debugObservers;
	}

	public List<LoggerListener> getErrorObservers() {
		return errorObservers;
	}

	@Override
	protected String getFullyQualifiedCallerName() {
		return null;
	}

	public List<LoggerListener> getInfoObservers() {
		return infoObservers;
	}

	public List<LoggerListener> getTraceObservers() {
		return traceObservers;
	}

	public ContexLogger getType() {
		return type;
	}

	public List<LoggerListener> getWarnObservers() {
		return warnObservers;
	}

	@Override
	protected void handleNormalizedLoggingCall(final Level level, final Marker marker, final String messagePattern,
			final Object[] arguments, final Throwable throwable) {
		switch (level) {
		case ERROR:
			logger.error(messagePattern, arguments, throwable);
			errorObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			warnObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			infoObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			debugObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			traceObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			break;
		case WARN:
			logger.warn(messagePattern, arguments, throwable);
			warnObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			infoObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			debugObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			traceObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			break;
		case INFO:
			// logger.info(messagePattern);
			logger.info(messagePattern, arguments, throwable);
			infoObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			debugObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			traceObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			break;
		case DEBUG:
			logger.debug(messagePattern, arguments, throwable);
			debugObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			traceObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			break;
		case TRACE:
			logger.trace(messagePattern, arguments, throwable);
			traceObservers.forEach(e -> e.onEvent(messagePattern, arguments, throwable));
			break;
		default:
			throw new IllegalStateException("Level " + level + " is not supported.");
		}
	}

	public boolean isDebug() {
		return debug;
	}

	@Override
	public boolean isDebugEnabled() {
		return debug;
	}

	@Override
	public boolean isDebugEnabled(final Marker marker) {
		return debug;
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isErrorEnabled(final Marker marker) {
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled(final Marker marker) {
		return true;
	}

	@Override
	public boolean isTraceEnabled() {
		return debug;
	}

	@Override
	public boolean isTraceEnabled(final Marker marker) {
		return debug;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled(final Marker marker) {
		return true;
	}

	public void setDebug(final boolean debug) {
		this.debug = debug;
	}

}
