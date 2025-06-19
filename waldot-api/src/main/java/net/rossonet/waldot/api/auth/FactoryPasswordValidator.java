package net.rossonet.waldot.api.auth;

import java.util.function.Predicate;

import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator.AuthenticationChallenge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public class FactoryPasswordValidator {

	private static final Logger logger = LoggerFactory.getLogger(FactoryPasswordValidator.class);

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
