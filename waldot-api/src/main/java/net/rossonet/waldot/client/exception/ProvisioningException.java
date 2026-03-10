package net.rossonet.waldot.client.exception;

import net.rossonet.waldot.exception.WaldotException;

/**
 * The {@code ProvisioningException} class represents an exception that occurs during
 * the provisioning process of WaldOT client or agent resources.
 *
 * <p>
 * This exception is thrown when there are issues with provisioning operations, such as:
 * </p>
 * <ul>
 * <li>Failure to register a digital twin model</li>
 * <li>Invalid configuration during provisioning</li>
 * <li>Connection issues with the backend services</li>
 * <li>Authentication or authorization failures</li>
 * <li>Resource allocation failures</li>
 * </ul>
 *
 * @see net.rossonet.waldot.exception.WaldotException
 */
public class ProvisioningException extends WaldotException {

	private static final long serialVersionUID = -4035397403688831965L;

}
