package net.rossonet.waldot.api.auth;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.AbstractIdentityValidator;
import org.eclipse.milo.opcua.stack.core.types.structured.AnonymousIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.SignatureData;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

public abstract class WaldotAnonymousValidator extends AbstractIdentityValidator<String> {

	protected WaldotConfiguration configuration;

	public WaldotAnonymousValidator(WaldotConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String validateAnonymousToken(Session session, AnonymousIdentityToken token, UserTokenPolicy tokenPolicy,
			SignatureData tokenSignature) {
		return String.format("anonymous_%s_%s", session.getSessionName(), session.getSessionId().toParseableString());
	}

}
