package net.rossonet.waldot.agent.auth;

import java.security.cert.X509Certificate;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.auth.WaldotX509IdentityValidator;
import net.rossonet.waldot.api.strategies.WaldotAgentManagementStrategy;
import net.rossonet.waldot.opc.WaldotOpcUaServer;

public class AgentRegisterX509IdentityValidator extends WaldotX509IdentityValidator implements AgentAuthenticator {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private WaldotAgentManagementStrategy agentManagementStrategy;

	public AgentRegisterX509IdentityValidator(final WaldotOpcUaServer waldotOpcUaServer) {
		super(waldotOpcUaServer.getWaldotConfiguration());

	}

	@Override
	protected Object authenticateIdentityCertificate(final Session session, final X509Certificate identityCertificate) {
		if (session.getEndpoint().getEndpointUrl().endsWith(WaldotOpcUaServer.REGISTER_PATH)) {
			final String sessionDataForLogging = AgentAuthenticator.generateSessionDataForLogging(session);
			logger.info("\n *** NEW AGENT UPDATE CERTIFICATE REQUEST\n{}", sessionDataForLogging);
			return agentManagementStrategy.updateAgentCertificate(session, identityCertificate);
		} else {
			throw new IllegalArgumentException(
					"Agent validation is only allowed for sessions with endpoint URL ending with '"
							+ WaldotOpcUaServer.REGISTER_PATH + "'");
		}
	}

	@Override
	public void setAgentManagementStrategy(final WaldotAgentManagementStrategy agentManagementStrategy) {
		this.agentManagementStrategy = agentManagementStrategy;

	}

}
