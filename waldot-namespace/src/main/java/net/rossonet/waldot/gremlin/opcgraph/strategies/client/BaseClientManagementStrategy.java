package net.rossonet.waldot.gremlin.opcgraph.strategies.client;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.Identity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.AnonymousIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.UsernameIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.X509UserIdentity;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
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
	protected ClientRegisterUsernameIdentityValidator agentIdentityValidator;
	protected ClientRegisterX509IdentityValidator agentX509IdentityValidator;
	private UaFolderNode assetRootNode;
	private UaFolderNode clientFolder;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	// accessed concurrently by the OPC UA session activation threads
	private final Map<NodeId, SessionData> sessions = new ConcurrentHashMap<>();

	private WaldotNamespace waldotNamespace;

	@Override
	public void activate(final ClientRegisterAnonymousValidator agentAnonymousValidator,
			final ClientRegisterUsernameIdentityValidator agentIdentityValidator,
			final ClientRegisterX509IdentityValidator agentX509IdentityValidator) {
		this.agentAnonymousValidator = agentAnonymousValidator;
		this.agentIdentityValidator = agentIdentityValidator;
		this.agentX509IdentityValidator = agentX509IdentityValidator;

	}

	/**
	 * Converts the session identity to the expected type. A {@code null} identity
	 * is returned as is (previous behaviour); an identity of another type is
	 * rejected instead of failing with a {@link ClassCastException}.
	 */
	static <T extends Identity> T castIdentity(final Identity identity, final Class<T> expected)
			throws UaException {
		if (identity == null) {
			return null;
		}
		if (expected.isInstance(identity)) {
			return expected.cast(identity);
		}
		throw new UaException(StatusCodes.Bad_IdentityTokenRejected, "session identity is "
				+ identity.getClass().getSimpleName() + ", expected " + expected.getSimpleName());
	}

	@Override
	public void close() throws Exception {
		// capire cosa chiudere

	}

	@Override
	public void generateAssetFolders(final UaFolderNode assetRootNode) {
		this.assetRootNode = assetRootNode;
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
		return clientFolder;
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
			UserTokenPolicy tokenPolicy, SignatureData tokenSignature) throws UaException {
		// TODO implementare l'autenticazione anonima
		registerSessionIfNeeded(session);
		return castIdentity(getSessionIdentity(session), AnonymousIdentity.class);
	}

	@Override
	public UsernameIdentity newUsernameIdentityClientSession(Session session, UserNameIdentityToken token,
			UserTokenPolicy tokenPolicy, SignatureData tokenSignature) throws UaException {
		// TODO implementare l'autenticazione con username e password
		registerSessionIfNeeded(session);
		return castIdentity(getSessionIdentity(session), UsernameIdentity.class);
	}

	@Override
	public X509UserIdentity newX509IdentityClientSession(Session session, X509Certificate identityCertificate)
			throws UaException {
		// TODO implementare l'autenticazione con certificato X509
		registerSessionIfNeeded(session);
		return castIdentity(getSessionIdentity(session), X509UserIdentity.class);
	}

	private void registerSessionIfNeeded(final Session session) {
		// putIfAbsent is atomic: two concurrent activations of the same session
		// register it only once
		if (sessions.putIfAbsent(session.getSessionId(), new SessionData(session)) == null) {
			logger.info("Registering new session: {}", session.getSessionId());
		} else {
			logger.debug("Session {} already registered", session.getSessionId());
		}
	}

}