package net.rossonet.waldot.exception;

/**
 * The {@code WaldotException} class is the base exception class for all WaldOT-related errors.
 * This exception is thrown when any error occurs within the WaldOT system, including issues
 * with digital twin operations, DTDL parsing, client operations, and plugin functionality.
 *
 * <p>
 * This class extends {@link Exception} and serves as a general-purpose exception for the
 * WaldOT project. More specific exception types should extend this class to provide
 * detailed error information for particular error conditions.
 * </p>
 *
 * @see net.rossonet.waldot.client.exception.ProvisioningException
 */
public class WaldotException extends Exception {

	private static final long serialVersionUID = 40534722559642269L;

}
