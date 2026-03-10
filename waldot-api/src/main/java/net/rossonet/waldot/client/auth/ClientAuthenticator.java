package net.rossonet.waldot.client.auth;

import org.eclipse.milo.opcua.sdk.server.Session;

import net.rossonet.waldot.api.strategies.ClientManagementStrategy;

/**
 * ClientAuthenticator is an interface for handling OPC UA client authentication.
 * 
 * <p>ClientAuthenticator provides a base interface for authentication strategies.
 * It includes a utility method for generating session log data.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create authenticator
 * ClientAuthenticator authenticator = new MyClientAuthenticator();
 * 
 * // Set the management strategy
 * authenticator.setAgentManagementStrategy(clientManagementStrategy);
 * 
 * // Use utility to generate session log
 * Session session = ...;
 * String sessionLog = ClientAuthenticator.generateSessionDataForLogging(session);
 * System.out.println(sessionLog);
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see ClientManagementStrategy
 * @see Session
 */
public interface ClientAuthenticator {

	/**
	 * Generates a formatted string containing session information for logging.
	 * 
	 * <p>This utility method creates a human-readable representation of
	 * an OPC UA session for debugging and logging purposes.</p>
	 * 
	 * <p>Output includes:</p>
	 * <ul>
	 *   <li>Session name</li>
	 *   <li>Session ID</li>
	 *   <li>User identity</li>
	 *   <li>Identity token type</li>
	 *   <li>Client address</li>
	 *   <li>Client description</li>
	 *   <li>Session timeout</li>
	 *   <li>Endpoint URL</li>
	 * </ul>
	 * 
	 * @param session the Session to generate log data for
	 * @return formatted string with session information
	 * @see Session
	 */
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

	/**
	 * Sets the client management strategy for this authenticator.
	 * 
	 * <p>This should be called after creating the authenticator to establish
	 * the connection to the client management system.</p>
	 * 
	 * @param agentManagementStrategy the ClientManagementStrategy to use
	 * @see ClientManagementStrategy
	 */
	public void setAgentManagementStrategy(ClientManagementStrategy agentManagementStrategy);

}
