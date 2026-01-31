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

public interface ClientManagementStrategy extends AutoCloseable {

	void activate(ClientRegisterAnonymousValidator agentAnonymousValidator,
			ClientRegisterUsernameIdentityValidator agentIdentityValidator,
			ClientRegisterX509IdentityValidator agentX509IdentityValidator);

	void generateAssetFolders(UaFolderNode assetRootNode);

	UaFolderNode getAssetClientsFolderNode();

	void initialize(WaldotNamespace waldotNamespace);

	AnonymousIdentity newAnonymousClientSession(Session session, AnonymousIdentityToken token,
			UserTokenPolicy tokenPolicy, SignatureData tokenSignature);

	UsernameIdentity newUsernameIdentityClientSession(Session session, UserNameIdentityToken token,
			UserTokenPolicy tokenPolicy, SignatureData tokenSignature);

	X509UserIdentity newX509IdentityClientSession(Session session, X509Certificate identityCertificate);

}
