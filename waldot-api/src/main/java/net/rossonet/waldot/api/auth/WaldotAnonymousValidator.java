package net.rossonet.waldot.api.auth;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.AbstractIdentityValidator;
import org.eclipse.milo.opcua.stack.core.types.structured.AnonymousIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.SignatureData;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

/**
 * Abstract class for Waldot anonymous token validation. This class extends the
 * AbstractIdentityValidator to provide a specific implementation for validating
 * anonymous tokens in the Waldot context.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public abstract class WaldotAnonymousValidator extends AbstractIdentityValidator<String> {

	protected WaldotConfiguration configuration;

	public WaldotAnonymousValidator(final WaldotConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String validateAnonymousToken(final Session session, final AnonymousIdentityToken token,
			final UserTokenPolicy tokenPolicy, final SignatureData tokenSignature) {
		return String.format("anonymous_%s_%s", session.getSessionName(), session.getSessionId().toParseableString());
	}

}
