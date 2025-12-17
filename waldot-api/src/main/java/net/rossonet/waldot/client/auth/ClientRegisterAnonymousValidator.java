package net.rossonet.waldot.client.auth;

import java.util.Collections;
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
	private ClientManagementStrategy agentManagementStrategy;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ClientRegisterAnonymousValidator(final WaldotOpcUaServer waldotOpcUaServer) {
		super(waldotOpcUaServer.getWaldotConfiguration());
	}

	@Override
	public Set<UserTokenType> getSupportedTokenTypes() {
		// TODO completare
		return Collections.emptySet();
	}

	@Override
	public void setAgentManagementStrategy(final ClientManagementStrategy agentManagementStrategy) {
		this.agentManagementStrategy = agentManagementStrategy;

	}

	@Override
	public AnonymousIdentity validateAnonymousToken(final Session session, final AnonymousIdentityToken token,
			final UserTokenPolicy tokenPolicy, final SignatureData tokenSignature) {
		if (session.getEndpoint().getEndpointUrl().endsWith(WaldotOpcUaServer.REGISTER_PATH)) {
			final String sessionDataForLogging = ClientAuthenticator.generateSessionDataForLogging(session);
			logger.info("\n *** NEW ANONYMOUS REGISTRATION AGENT REQUEST\n{}", sessionDataForLogging);
			return agentManagementStrategy.registerNewClientForApproval(session);
		} else {
			throw new IllegalArgumentException(
					"Agent validation is only allowed for sessions with endpoint URL ending with '"
							+ WaldotOpcUaServer.REGISTER_PATH + "'");
		}
	}

}
