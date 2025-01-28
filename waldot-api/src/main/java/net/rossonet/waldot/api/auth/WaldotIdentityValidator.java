package net.rossonet.waldot.api.auth;

import java.util.function.Predicate;

import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public abstract class WaldotIdentityValidator extends UsernameIdentityValidator {
	private static final Logger logger = LoggerFactory.getLogger(WaldotIdentityValidator.class);

	private static Predicate<AuthenticationChallenge> getAuthChallenge(WaldotConfiguration opcUaConfiguration) {
		return (authChallenge) -> {
			final String username = authChallenge.getUsername();
			final String password = authChallenge.getPassword();

			final boolean userOk = opcUaConfiguration.getFactoryUsername().equals(username)
					&& opcUaConfiguration.getFactoryPassword().equals(password);
			if (!userOk) {
				logger.warn("Authentication failed for user: {}", username);
			}
			return userOk;
		};
	}

	protected WaldotConfiguration configuration;

	public WaldotIdentityValidator(WaldotConfiguration configuration) {
		super(configuration.getAnonymousAccessAllowed(), getAuthChallenge(configuration));
		this.configuration = configuration;
	}

}
