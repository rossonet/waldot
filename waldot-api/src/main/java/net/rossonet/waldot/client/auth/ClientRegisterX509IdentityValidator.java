package net.rossonet.waldot.client.auth;

import java.security.cert.X509Certificate;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.X509UserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.auth.WaldotX509IdentityValidator;
import net.rossonet.waldot.api.strategies.ClientManagementStrategy;
import net.rossonet.waldot.opc.WaldotOpcUaServer;

public class ClientRegisterX509IdentityValidator extends WaldotX509IdentityValidator implements ClientAuthenticator {
	private ClientManagementStrategy clientManagementStrategy;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ClientRegisterX509IdentityValidator(final WaldotOpcUaServer waldotOpcUaServer) {
		super(waldotOpcUaServer.getWaldotConfiguration());

	}

	protected X509UserIdentity authenticateIdentityCertificate(final Session session,
			final X509Certificate identityCertificate) {
		final String sessionDataForLogging = ClientAuthenticator.generateSessionDataForLogging(session);
		logger.info("\n *** NEW AGENT UPDATE CERTIFICATE REQUEST\n{}", sessionDataForLogging);
		return clientManagementStrategy.newX509IdentityClientSession(session, identityCertificate);

	}

	@Override
	public void setAgentManagementStrategy(final ClientManagementStrategy clientManagementStrategy) {
		this.clientManagementStrategy = clientManagementStrategy;

	}

}
