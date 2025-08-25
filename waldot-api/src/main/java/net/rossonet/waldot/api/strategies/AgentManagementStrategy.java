package net.rossonet.waldot.api.strategies;

import java.security.cert.X509Certificate;

import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.AnonymousIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.UsernameIdentity;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.structured.UserNameIdentityToken;

import net.rossonet.waldot.agent.auth.AgentRegisterAnonymousValidator;
import net.rossonet.waldot.agent.auth.AgentRegisterUsernameIdentityValidator;
import net.rossonet.waldot.agent.auth.AgentRegisterX509IdentityValidator;
import net.rossonet.waldot.api.models.WaldotNamespace;

public interface AgentManagementStrategy {

	void activate(AgentRegisterAnonymousValidator agentAnonymousValidator,
			AgentRegisterUsernameIdentityValidator agentIdentityValidator,
			AgentRegisterX509IdentityValidator agentX509IdentityValidator);

	void generateAssetFolders(UaFolderNode assetRootNode);

	void initialize(WaldotNamespace waldotNamespace);

	AnonymousIdentity registerNewAgentForApproval(Session session);

	UsernameIdentity registerNewAgentWithProvisioningPassword(Session session, UserNameIdentityToken token);

	Object updateAgentCertificate(Session session, X509Certificate identityCertificate);

}
