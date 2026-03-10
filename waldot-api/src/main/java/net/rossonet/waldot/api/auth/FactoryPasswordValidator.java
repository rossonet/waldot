package net.rossonet.waldot.api.auth;

import java.util.function.Predicate;

import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator.AuthenticationChallenge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

/**
 * FactoryPasswordValidator provides authentication validation for factory-level
 * username/password credentials. This validator is used to authenticate special
 * factory users with elevated privileges for administrative operations.
 */
public class FactoryPasswordValidator {

	private static final Logger logger = LoggerFactory.getLogger(FactoryPasswordValidator.class);

	/**
	 * Creates a predicate for validating factory authentication challenges.
	 * The predicate checks if the provided username and password match the
	 * factory credentials configured in the Waldot configuration.
	 *
	 * @param  configuration the Waldot configuration containing factory credentials
	 * @return a Predicate that validates AuthenticationChallenge objects
	 */
	public Predicate<AuthenticationChallenge> getAuthChallenge(final WaldotConfiguration configuration) {
		return (authChallenge) -> {
			final String username = authChallenge.getUsername();
			final String password = authChallenge.getPassword();

			final boolean userOk = configuration.getFactoryUsername().equals(username)
					&& configuration.getFactoryPassword().equals(password);
			if (!userOk) {
				logger.warn("Authentication failed for user: {}", username);
			}
			return userOk;
		};
	}

}
