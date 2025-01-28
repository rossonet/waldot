package net.rossonet.waldot.auth;

import java.util.function.Predicate;

import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.WaldotOpcUaServer;
import net.rossonet.waldot.configuration.OpcUaConfiguration;

public class WaldotIdentityValidator extends UsernameIdentityValidator {
	private static final Logger logger = LoggerFactory.getLogger(WaldotIdentityValidator.class);

	private static Predicate<AuthenticationChallenge> getAuthChallenge(OpcUaConfiguration opcUaConfiguration) {
		return (authChallenge) -> {
			final String username = authChallenge.getUsername();
			final String password = authChallenge.getPassword();

			final boolean userOk = opcUaConfiguration.getFactoryUsername().equals(username)
					&& opcUaConfiguration.getFactoryPassword().equals(password);

			return userOk;
		};
	}

	private final WaldotOpcUaServer waldotOpcUaServer;

	public WaldotIdentityValidator(WaldotOpcUaServer waldotOpcUaServer) {
		super(waldotOpcUaServer.getConfiguration().getAnonymousAccessAllowed(),
				getAuthChallenge(waldotOpcUaServer.getConfiguration()));
		this.waldotOpcUaServer = waldotOpcUaServer;
	}

}
