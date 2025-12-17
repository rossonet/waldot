package net.rossonet.waldot.client.auth;

import java.security.cert.X509Certificate;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.auth.WaldotX509IdentityValidator;
import net.rossonet.waldot.api.strategies.ClientManagementStrategy;
import net.rossonet.waldot.opc.WaldotOpcUaServer;

public class ClientRegisterX509IdentityValidator extends WaldotX509IdentityValidator implements ClientAuthenticator {
	private ClientManagementStrategy agentManagementStrategy;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ClientRegisterX509IdentityValidator(final WaldotOpcUaServer waldotOpcUaServer) {
		super(waldotOpcUaServer.getWaldotConfiguration());

	}

	protected Object authenticateIdentityCertificate(final Session session, final X509Certificate identityCertificate) {
		if (session.getEndpoint().getEndpointUrl().endsWith(WaldotOpcUaServer.REGISTER_PATH)) {
			final String sessionDataForLogging = ClientAuthenticator.generateSessionDataForLogging(session);
			logger.info("\n *** NEW AGENT UPDATE CERTIFICATE REQUEST\n{}", sessionDataForLogging);
			return agentManagementStrategy.updateClientCertificate(session, identityCertificate);
		} else {
			throw new IllegalArgumentException(
					"Agent validation is only allowed for sessions with endpoint URL ending with '"
							+ WaldotOpcUaServer.REGISTER_PATH + "'");
		}
	}

	@Override
	public void setAgentManagementStrategy(final ClientManagementStrategy agentManagementStrategy) {
		this.agentManagementStrategy = agentManagementStrategy;

	}

}
