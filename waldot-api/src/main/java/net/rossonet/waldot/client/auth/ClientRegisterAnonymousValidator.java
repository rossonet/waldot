package net.rossonet.waldot.client.auth;

import java.util.Set;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.AnonymousIdentity;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.AnonymousIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.SignatureData;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.auth.WaldotAnonymousValidator;
import net.rossonet.waldot.api.strategies.ClientManagementStrategy;
import net.rossonet.waldot.opc.WaldotOpcUaServer;

public class ClientRegisterAnonymousValidator extends WaldotAnonymousValidator implements ClientAuthenticator {
	private ClientManagementStrategy clientManagementStrategy;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ClientRegisterAnonymousValidator(final WaldotOpcUaServer waldotOpcUaServer) {
		super(waldotOpcUaServer.getWaldotConfiguration());
	}

	@Override
	public Set<UserTokenType> getSupportedTokenTypes() {
		return Set.of(UserTokenType.Anonymous);
	}

	@Override
	public void setAgentManagementStrategy(final ClientManagementStrategy clientManagementStrategy) {
		this.clientManagementStrategy = clientManagementStrategy;

	}

	@Override
	public AnonymousIdentity validateAnonymousToken(final Session session, final AnonymousIdentityToken token,
			final UserTokenPolicy tokenPolicy, final SignatureData tokenSignature) {
		final String sessionDataForLogging = ClientAuthenticator.generateSessionDataForLogging(session);
		logger.info("\n *** NEW ANONYMOUS REGISTRATION CLIENT REQUEST\n{}", sessionDataForLogging);
		return clientManagementStrategy.newAnonymousClientSession(session, token, tokenPolicy, tokenSignature);
	}

}
