package net.rossonet.waldot.agent.auth;

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
import net.rossonet.waldot.api.strategies.AgentManagementStrategy;
import net.rossonet.waldot.opc.WaldotOpcUaServer;

public class AgentRegisterAnonymousValidator extends WaldotAnonymousValidator implements AgentAuthenticator {
	private AgentManagementStrategy agentManagementStrategy;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public AgentRegisterAnonymousValidator(final WaldotOpcUaServer waldotOpcUaServer) {
		super(waldotOpcUaServer.getWaldotConfiguration());
	}

	@Override
	public Set<UserTokenType> getSupportedTokenTypes() {
		// TODO Auto-generated method stub
		return Collections.emptySet();
	}

	@Override
	public void setAgentManagementStrategy(final AgentManagementStrategy agentManagementStrategy) {
		this.agentManagementStrategy = agentManagementStrategy;

	}

	@Override
	public AnonymousIdentity validateAnonymousToken(final Session session, final AnonymousIdentityToken token,
			final UserTokenPolicy tokenPolicy, final SignatureData tokenSignature) {
		if (session.getEndpoint().getEndpointUrl().endsWith(WaldotOpcUaServer.REGISTER_PATH)) {
			final String sessionDataForLogging = AgentAuthenticator.generateSessionDataForLogging(session);
			logger.info("\n *** NEW ANONYMOUS REGISTRATION AGENT REQUEST\n{}", sessionDataForLogging);
			return agentManagementStrategy.registerNewAgentForApproval(session);
		} else {
			throw new IllegalArgumentException(
					"Agent validation is only allowed for sessions with endpoint URL ending with '"
							+ WaldotOpcUaServer.REGISTER_PATH + "'");
		}
	}

}
