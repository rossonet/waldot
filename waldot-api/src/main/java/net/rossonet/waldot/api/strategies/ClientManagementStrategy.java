package net.rossonet.waldot.api.strategies;

import java.security.cert.X509Certificate;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.AnonymousIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.UsernameIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.X509UserIdentity;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.structured.AnonymousIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.SignatureData;
import org.eclipse.milo.opcua.stack.core.types.structured.UserNameIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.client.auth.ClientRegisterAnonymousValidator;
import net.rossonet.waldot.client.auth.ClientRegisterUsernameIdentityValidator;
import net.rossonet.waldot.client.auth.ClientRegisterX509IdentityValidator;

/**
 * ClientManagementStrategy is an interface that defines the strategy for
 * managing OPC UA client connections, sessions, and authentication.
 * 
 * <p>This strategy handles:</p>
 * <ul>
 *   <li>Client session creation and management</li>
 *   <li>Authentication (anonymous, username/password, X.509)</li>
 *   <li>Client asset folder organization</li>
 *   <li>Identity validation</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Initialize strategy
 * clientStrategy.initialize(waldotNamespace);
 * 
 * // Activate with validators
 * clientStrategy.activate(
 *     anonymousValidator,
 *     usernameValidator,
 *     x509Validator
 * );
 * 
 * // Generate asset folders
 * clientStrategy.generateAssetFolders(assetRootNode);
 * 
 * // Get clients folder
 * UaFolderNode clientsFolder = clientStrategy.getAssetClientsFolderNode();
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface ClientManagementStrategy extends AutoCloseable {

	/**
	 * Activates the client management strategy with validators.
	 * 
	 * <p>This should be called after initialize() to enable client connections
	 * with the specified authentication methods.</p>
	 * 
	 * @param agentAnonymousValidator validator for anonymous access
	 * @param agentIdentityValidator validator for username/password
	 * @param agentX509IdentityValidator validator for X.509 certificates
	 * @see ClientRegisterAnonymousValidator
	 * @see ClientRegisterUsernameIdentityValidator
	 * @see ClientRegisterX509IdentityValidator
	 */
	void activate(ClientRegisterAnonymousValidator agentAnonymousValidator,
			ClientRegisterUsernameIdentityValidator agentIdentityValidator,
			ClientRegisterX509IdentityValidator agentX509IdentityValidator);

	/**
	 * Generates asset folders for client organization.
	 * 
	 * <p>Creates the folder structure under the asset root for organizing
	 * client-related nodes in the OPC UA address space.</p>
	 * 
	 * @param assetRootNode the root folder for assets
	 * @see UaFolderNode
	 */
	void generateAssetFolders(UaFolderNode assetRootNode);

	/**
	 * Returns the folder for client assets.
	 * 
	 * <p>This folder contains nodes representing connected clients
	 * in the OPC UA address space.</p>
	 * 
	 * @return the UaFolderNode for client assets
	 * @see UaFolderNode
	 */
	UaFolderNode getAssetClientsFolderNode();

	/**
	 * Initializes the client management strategy.
	 * 
	 * @param waldotNamespace the namespace to use
	 * @see WaldotNamespace
	 */
	void initialize(WaldotNamespace waldotNamespace);

	/**
	 * Creates a new anonymous client session.
	 * 
	 * @param session the OPC UA session
	 * @param token the anonymous identity token
	 * @param tokenPolicy the token policy
	 * @param tokenSignature the token signature
	 * @return the AnonymousIdentity for the session
	 * @see Session
	 * @see AnonymousIdentityToken
	 * @see UserTokenPolicy
	 * @see SignatureData
	 * @see AnonymousIdentity
	 */
	AnonymousIdentity newAnonymousClientSession(Session session, AnonymousIdentityToken token,
			UserTokenPolicy tokenPolicy, SignatureData tokenSignature);

	/**
	 * Creates a new username/password client identity.
	 * 
	 * @param session the OPC UA session
	 * @param token the username identity token
	 * @param tokenPolicy the token policy
	 * @param tokenSignature the token signature
	 * @return the UsernameIdentity for the session
	 * @see Session
	 * @see UserNameIdentityToken
	 * @see UserTokenPolicy
	 * @see SignatureData
	 * @see UsernameIdentity
	 */
	UsernameIdentity newUsernameIdentityClientSession(Session session, UserNameIdentityToken token,
			UserTokenPolicy tokenPolicy, SignatureData tokenSignature);

	/**
	 * Creates a new X.509 certificate client identity.
	 * 
	 * @param session the OPC UA session
	 * @param identityCertificate the client certificate
	 * @return the X509UserIdentity for the session
	 * @see Session
	 * @see X509Certificate
	 * @see X509UserIdentity
	 */
	X509UserIdentity newX509IdentityClientSession(Session session, X509Certificate identityCertificate);

}
