package net.rossonet.waldot.client.api;

import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

/**
 * Unchecked error of the WaldOT client v2: OPC UA failures, command errors
 * returned by the server, invalid responses.
 */
public class WaldotClientException extends RuntimeException {

	private static final long serialVersionUID = 4467839418871322165L;

	/** Wraps an OPC UA exception keeping its status code. */
	public static WaldotClientException of(final String message, final Throwable cause) {
		if (cause instanceof WaldotClientException) {
			return (WaldotClientException) cause;
		}
		if (cause instanceof UaException) {
			return new WaldotClientException(message + ": " + cause.getMessage(),
					((UaException) cause).getStatusCode(), cause);
		}
		return new WaldotClientException(message + ": " + cause.getMessage(), StatusCode.BAD, cause);
	}

	private final StatusCode statusCode;

	public WaldotClientException(final String message) {
		this(message, StatusCode.BAD, null);
	}

	public WaldotClientException(final String message, final StatusCode statusCode, final Throwable cause) {
		super(message, cause);
		this.statusCode = statusCode == null ? StatusCode.BAD : statusCode;
	}

	/** @return the OPC UA status code of the failure ({@code Bad} if unknown) */
	public StatusCode getStatusCode() {
		return statusCode;
	}
}
