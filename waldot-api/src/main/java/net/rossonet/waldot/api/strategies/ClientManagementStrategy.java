package net.rossonet.waldot.api.strategies;

import java.security.cert.X509Certificate;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.AnonymousIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.UsernameIdentity;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.structured.UserNameIdentityToken;

import net.rossonet.waldot.agent.auth.ClientRegisterAnonymousValidator;
import net.rossonet.waldot.agent.auth.ClientRegisterUsernameIdentityValidator;
import net.rossonet.waldot.agent.auth.ClientRegisterX509IdentityValidator;
import net.rossonet.waldot.api.models.WaldotNamespace;

public interface ClientManagementStrategy {

	void activate(ClientRegisterAnonymousValidator agentAnonymousValidator,
			ClientRegisterUsernameIdentityValidator agentIdentityValidator,
			ClientRegisterX509IdentityValidator agentX509IdentityValidator);

	void generateAssetFolders(UaFolderNode assetRootNode);

	UaFolderNode getAssetClientsFolderNode();

	void initialize(WaldotNamespace waldotNamespace);

	AnonymousIdentity registerNewClientForApproval(Session session);

	UsernameIdentity registerNewClientWithProvisioningPassword(Session session, UserNameIdentityToken token);

	Object updateClientCertificate(Session session, X509Certificate identityCertificate);

}
