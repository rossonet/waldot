package net.rossonet.waldot.auth;

import java.security.cert.X509Certificate;
import java.util.function.Predicate;

import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.WaldotOpcUaServer;
import net.rossonet.waldot.configuration.OpcUaConfiguration;

public class WaldotX509IdentityValidator extends X509IdentityValidator {
	private static final Logger logger = LoggerFactory.getLogger(WaldotX509IdentityValidator.class);

	private static Predicate<X509Certificate> getAuthChallenge(OpcUaConfiguration opcUaConfiguration) {
		return (X509Challenge) -> {
			logger.info("X509Challenge: " + X509Challenge);

			return true;
		};
	}

	private final WaldotOpcUaServer waldotOpcUaServer;

	public WaldotX509IdentityValidator(WaldotOpcUaServer waldotOpcUaServer) {
		super(getAuthChallenge(waldotOpcUaServer.getConfiguration()));
		this.waldotOpcUaServer = waldotOpcUaServer;
	}
}
