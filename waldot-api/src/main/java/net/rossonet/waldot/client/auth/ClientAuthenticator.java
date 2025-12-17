package net.rossonet.waldot.client.auth;

import org.eclipse.milo.opcua.sdk.server.Session;

import net.rossonet.waldot.api.strategies.ClientManagementStrategy;

public interface ClientAuthenticator {

	public static String generateSessionDataForLogging(final Session session) {
		final StringBuilder sb = new StringBuilder();
		final String tab = "\t";
		sb.append("Session Name:").append(tab).append(session.getSessionName()).append("\n");
		sb.append("Session ID:").append(tab).append(session.getSessionId().toParseableString()).append("\n");
		sb.append("User Identity:").append(tab).append(session.getClientUserId()).append("\n");
		sb.append("Identity Token:").append(tab).append(session.getIdentityToken()).append("\n");
		sb.append("Client Address:").append(tab).append(session.getClientAddress()).append("\n");
		sb.append("Client Description:").append(tab).append(session.getClientDescription().toString()).append("\n");
		sb.append("Session Timeout:").append(tab).append(session.getSessionTimeout()).append(" ms").append("\n");
		sb.append("Session Endpoint:").append(tab).append(session.getEndpoint().getEndpointUrl()).append("\n\n");
		return sb.toString();
	}

	public void setAgentManagementStrategy(ClientManagementStrategy agentManagementStrategy);

}
