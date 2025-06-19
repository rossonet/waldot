package net.rossonet.waldot.api.auth;

import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

/**
 * Abstract class for Waldot identity validation. This class extends the
 * UsernameIdentityValidator to provide a specific implementation for validating
 * user identities in the Waldot context.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public abstract class WaldotIdentityValidator extends UsernameIdentityValidator {

	public WaldotIdentityValidator(final WaldotConfiguration configuration,
			final FactoryPasswordValidator authChallenge) {
		super(configuration.getAnonymousAccessAllowed(), authChallenge.getAuthChallenge(configuration));
	}

}
