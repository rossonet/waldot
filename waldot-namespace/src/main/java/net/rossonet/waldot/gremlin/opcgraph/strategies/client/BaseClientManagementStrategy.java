package net.rossonet.waldot.gremlin.opcgraph.strategies.client;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.Identity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.AnonymousIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.UsernameIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.X509UserIdentity;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.AnonymousIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.SignatureData;
import org.eclipse.milo.opcua.stack.core.types.structured.UserNameIdentityToken;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.ClientManagementStrategy;
import net.rossonet.waldot.client.auth.ClientRegisterAnonymousValidator;
import net.rossonet.waldot.client.auth.ClientRegisterUsernameIdentityValidator;
import net.rossonet.waldot.client.auth.ClientRegisterX509IdentityValidator;

public class BaseClientManagementStrategy implements ClientManagementStrategy {

	private final class SessionData {
		private final Session session;

		public SessionData(final Session session) {
			this.session = session;
		}

	}

	protected ClientRegisterAnonymousValidator agentAnonymousValidator;
	private UaFolderNode agentFolder;
	protected ClientRegisterUsernameIdentityValidator agentIdentityValidator;
	protected ClientRegisterX509IdentityValidator agentX509IdentityValidator;
	private UaFolderNode assetRootNode;
	private UaFolderNode clientFolder;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<NodeId, SessionData> sessions = new HashMap<>();

	private WaldotNamespace waldotNamespace;

	@Override
	public void activate(final ClientRegisterAnonymousValidator agentAnonymousValidator,
			final ClientRegisterUsernameIdentityValidator agentIdentityValidator,
			final ClientRegisterX509IdentityValidator agentX509IdentityValidator) {
		this.agentAnonymousValidator = agentAnonymousValidator;
		this.agentIdentityValidator = agentIdentityValidator;
		this.agentX509IdentityValidator = agentX509IdentityValidator;

	}

	@Override
	public void close() throws Exception {
		// capire cosa chiudere

	}

	@Override
	public void generateAssetFolders(final UaFolderNode assetRootNode) {
		this.assetRootNode = assetRootNode;
		generateClientFolder();
		generateClientFolder();

	}

	private void generateClientFolder() {
		clientFolder = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId("asset.clients"),
				waldotNamespace.generateQualifiedName("Client Management"),
				LocalizedText.english("Clients Management"));
		waldotNamespace.getStorageManager().addNode(clientFolder);
		assetRootNode.addOrganizes(clientFolder);
	}

	@Override
	public UaFolderNode getAssetClientsFolderNode() {
		return agentFolder;
	}

	private Identity getSessionIdentity(final Session session) {
		if (session == null) {
			logger.warn("Session is null, cannot get session ID");
			return null;
		}
		if (sessions.containsKey(session.getSessionId())) {
			return session.getIdentity();
		} else {
			logger.warn("Session {} not found in registered sessions", session.getSessionId());
			throw new IllegalStateException("Session " + session.getSessionId() + " not found in registered sessions");
		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		logger.info("Agent Management Strategy initialized");
	}

	@Override
	public AnonymousIdentity newAnonymousClientSession(Session session, AnonymousIdentityToken token,
			UserTokenPolicy tokenPolicy, SignatureData tokenSignature) {
		// TODO implementare l'autenticazione anonima
		registerSessionIfNeeded(session);
		return (AnonymousIdentity) getSessionIdentity(session);
	}

	@Override
	public UsernameIdentity newUsernameIdentityClientSession(Session session, UserNameIdentityToken token,
			UserTokenPolicy tokenPolicy, SignatureData tokenSignature) {
		// TODO implementare l'autenticazione con username e password
		registerSessionIfNeeded(session);
		return (UsernameIdentity) getSessionIdentity(session);
	}

	@Override
	public X509UserIdentity newX509IdentityClientSession(Session session, X509Certificate identityCertificate) {
		// TODO implementare l'autenticazione con certificato X509
		registerSessionIfNeeded(session);
		return (X509UserIdentity) getSessionIdentity(session);
	}

	private void registerSessionIfNeeded(final Session session) {
		if (sessions.containsKey(session.getSessionId())) {
			logger.debug("Session {} already registered", session.getSessionId());
			return;
		} else {
			logger.info("Registering new session: {}", session.getSessionId());
			sessions.put(session.getSessionId(), new SessionData(session));
		}

	}

}
