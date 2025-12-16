package net.rossonet.waldot.api.auth;

import java.security.cert.X509Certificate;
import java.util.function.Predicate;

import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

/**
 * WaldotX509IdentityValidator is an implementation of X509IdentityValidator
 * that provides a specific validation mechanism for X.509 certificates in the
 * Waldot context.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class WaldotX509IdentityValidator extends X509IdentityValidator {
	private static final Logger logger = LoggerFactory.getLogger(WaldotX509IdentityValidator.class);

	private static Predicate<X509Certificate> getAuthChallenge(final WaldotConfiguration configuration) {
		return (X509Challenge) -> {
			logger.warn("X509Challenge: " + X509Challenge + ", certificate auhtntication not implemented yet");
			return false;
		};
	}

	public WaldotX509IdentityValidator(final WaldotConfiguration configuration) {
		// TODO completare
		super(getAuthChallenge(configuration));
	}

}
