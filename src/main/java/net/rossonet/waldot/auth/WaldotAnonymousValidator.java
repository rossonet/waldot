package net.rossonet.waldot.auth;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.AbstractIdentityValidator;
import org.eclipse.milo.opcua.stack.core.types.structured.AnonymousIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.SignatureData;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;

import net.rossonet.waldot.WaldotOpcUaServer;

public class WaldotAnonymousValidator extends AbstractIdentityValidator<String> {

	public WaldotAnonymousValidator(WaldotOpcUaServer waldotOpcUaServer) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String validateAnonymousToken(Session session, AnonymousIdentityToken token, UserTokenPolicy tokenPolicy,
			SignatureData tokenSignature) {
		return String.format("anonymous_%s_%s", session.getSessionName(), session.getSessionId().toParseableString());
	}

}
