package net.rossonet.waldot.agent.auth;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.UsernameIdentity;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.structured.SignatureData;
import org.eclipse.milo.opcua.stack.core.types.structured.UserNameIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.auth.FactoryPasswordValidator;
import net.rossonet.waldot.api.auth.WaldotIdentityValidator;
import net.rossonet.waldot.api.strategies.ClientManagementStrategy;
import net.rossonet.waldot.opc.WaldotOpcUaServer;

public class ClientRegisterUsernameIdentityValidator extends WaldotIdentityValidator implements ClientAuthenticator {
	private ClientManagementStrategy agentManagementStrategy;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ClientRegisterUsernameIdentityValidator(final WaldotOpcUaServer waldotOpcUaServer,
			final FactoryPasswordValidator authChallenge) {
		super(waldotOpcUaServer.getWaldotConfiguration(), authChallenge);
	}

	@Override
	public void setAgentManagementStrategy(final ClientManagementStrategy agentManagementStrategy) {
		this.agentManagementStrategy = agentManagementStrategy;

	}

	@Override
	protected UsernameIdentity validateUsernameToken(final Session session, final UserNameIdentityToken token,
			final UserTokenPolicy tokenPolicy, final SignatureData tokenSignature) throws UaException {
		if (session.getEndpoint().getEndpointUrl().endsWith(WaldotOpcUaServer.REGISTER_PATH)) {
			final String sessionDataForLogging = ClientAuthenticator.generateSessionDataForLogging(session);
			logger.info("\n *** NEW AGENT REGISTRATION REQUEST WITH PROVISIONING PASSWORD\n{}", sessionDataForLogging);
			return agentManagementStrategy.registerNewClientWithProvisioningPassword(session, token);
		} else {
			throw new IllegalArgumentException(
					"Agent validation is only allowed for sessions with endpoint URL ending with '"
							+ WaldotOpcUaServer.REGISTER_PATH + "'");
		}
	}

}
