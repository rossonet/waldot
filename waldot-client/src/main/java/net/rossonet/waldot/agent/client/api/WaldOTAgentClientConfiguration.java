package net.rossonet.waldot.agent.client.api;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;

import net.rossonet.waldot.agent.client.v1.DefaultWaldOTAgentClientConfigurationV1;

public interface WaldOTAgentClientConfiguration extends Serializable {

	static WaldOTAgentClientConfiguration getDefaultConfiguration() {

		return new DefaultWaldOTAgentClientConfigurationV1();
	}

	int getAcknowledgeTimeout();

	String getAgentUniqueName();

	String getApplicationName();

	String getApplicationUri();

	String getBaseWaldOTOpcEndpoint();

	int getChannelLifetime();

	int getConnectTimeout();

	List<X509Certificate> getCryptoCertificateChain();

	PrivateKey getCryptoPrivateKey();

	String getDiscoveryEndpoint();

	String getForceDiscoveredEndpointUrl();

	String getGenerateCertCountry();

	String getGenerateCertDns();

	String getGenerateCertDnsAlias();

	String getGenerateCertIp();

	String getGenerateCertLocality();

	String getGenerateCertOrganization();

	String getGenerateCertState();

	String getGenerateCertUnit();

	int getKeepAliveTimeout();

	String getKeyStorePassword();

	String getKeyStorePath();

	int getMaxChunkCount();

	int getMaxChunkSize();

	int getMaxClientFaults();

	int getMaxMessageSize();

	MessageSecurityMode getMessageSecurityMode();

	int getRequestTimeout();

	SecurityPolicy getSecurityPolicy();

	int getSessionTimeout();

	String getUsername();

	boolean hasCertificateAuthentication();

	boolean hasProvisioningToken();

	boolean isForceCertificateValidator();

	boolean isIgnoreServiceFault();

	boolean isTestAnonymousConnection();

	void setAcknowledgeTimeout(int acknowledgeTimeout);

	void setAgentUniqueName(String agentUniqueName);

	void setApplicationUri(String applicationUri);

	void setBaseWaldOTOpcEndpoint(String baseWaldOTOpcEndpoint);

	void setChannelLifetime(int channelLifetime);

	void setConnectTimeout(int connectTimeout);

	void setCryptoCertificateChain(List<X509Certificate> cryptoCertificateChain);

	void setCryptoPrivateKey(PrivateKey cryptoPrivateKey);

	void setForceCertificateValidator(boolean forceCertificateValidator);

	void setForceDiscoveredEndpointUrl(String forceDiscoveredEndpointUrl);

	void setGenerateCertCountry(String generateCertCountry);

	void setGenerateCertDns(String generateCertDns);

	void setGenerateCertDnsAlias(String generateCertDnsAlias);

	void setGenerateCertIp(String generateCertIp);

	void setGenerateCertLocality(String generateCertLocality);

	void setGenerateCertOrganization(String generateCertOrganization);

	void setGenerateCertState(String generateCertState);

	void setGenerateCertUnit(String generateCertUnit);

	void setIgnoreServiceFault(boolean ignoreServiceFault);

	void setKeepAliveTimeout(int keepAliveTimeout);

	void setKeyStorePassword(String password);

	void setKeyStorePath(String path);

	void setMaxChunkCount(int maxChunkCount);

	void setMaxChunkSize(int maxChunkSize);

	void setMaxClientFaults(int maxClientFaults);

	void setMaxMessageSize(int maxMessageSize);

	void setMessageSecurityMode(MessageSecurityMode securityMode);

	void setPassword(String password);

	void setRequestTimeout(int requestTimeout);

	void setSecurityPolicy(SecurityPolicy securityPolicy);

	void setSessionTimeout(int sessionTimeout);

	void setTestAnonymousConnection(boolean active);

	void setUsername(String username);

}
