package net.rossonet.waldot.client.api;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;

import net.rossonet.waldot.client.v1.DefaultWaldOTAgentClientConfigurationV1;

public interface WaldOTAgentClientConfiguration extends Serializable {

	static WaldOTAgentClientConfiguration getDefaultConfiguration() {

		return new DefaultWaldOTAgentClientConfigurationV1();
	}

	int getAcknowledgeTimeout();

	String getAgentUniqueName();

	String getApplicationName();

	String getApplicationUri();

	/** @return identity used to open the session (default anonymous) */
	AuthenticationMode getAuthenticationMode();

	String getBaseWaldOTOpcEndpoint();

	int getChannelLifetime();

	/** @return NodeId identifier (string) of the commands root folder */
	String getCommandsRootNodeId();

	int getConnectTimeout();

	List<X509Certificate> getCryptoCertificateChain();

	PrivateKey getCryptoPrivateKey();

	/** @return default publishing interval (ms) of the subscription groups */
	double getDefaultPublishingInterval();

	/** @return default sampling interval (ms) of the monitored items */
	double getDefaultSamplingInterval();

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

	String getPassword();

	/** @return first delay (ms) before a new connection attempt */
	long getReconnectInitialDelay();

	/**
	 * @return maximum number of consecutive failed connection attempts, 0 for
	 *         no limit
	 */
	int getReconnectMaxAttempts();

	/** @return maximum delay (ms) between two connection attempts */
	long getReconnectMaxDelay();

	int getRequestTimeout();

	SecurityPolicy getSecurityPolicy();

	int getSessionTimeout();

	String getUsername();

	/** @return URI of the WaldOT namespace (to resolve its index) */
	String getWaldotNamespaceUri();

	boolean hasCertificateAuthentication();

	boolean hasProvisioningToken();

	boolean isForceCertificateValidator();

	boolean isIgnoreServiceFault();

	void setAcknowledgeTimeout(int acknowledgeTimeout);

	void setAgentUniqueName(String agentUniqueName);

	void setApplicationUri(String applicationUri);

	void setAuthenticationMode(AuthenticationMode authenticationMode);

	void setBaseWaldOTOpcEndpoint(String baseWaldOTOpcEndpoint);

	void setChannelLifetime(int channelLifetime);

	void setCommandsRootNodeId(String commandsRootNodeId);

	void setConnectTimeout(int connectTimeout);

	void setCryptoCertificateChain(List<X509Certificate> cryptoCertificateChain);

	void setCryptoPrivateKey(PrivateKey cryptoPrivateKey);

	void setDefaultPublishingInterval(double defaultPublishingInterval);

	void setDefaultSamplingInterval(double defaultSamplingInterval);

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

	void setReconnectInitialDelay(long reconnectInitialDelay);

	void setReconnectMaxAttempts(int reconnectMaxAttempts);

	void setReconnectMaxDelay(long reconnectMaxDelay);

	void setRequestTimeout(int requestTimeout);

	void setSecurityPolicy(SecurityPolicy securityPolicy);

	void setSessionTimeout(int sessionTimeout);

	void setUsername(String username);

	void setWaldotNamespaceUri(String waldotNamespaceUri);

}
