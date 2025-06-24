package net.rossonet.waldot.agent.client.v1;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClientConfiguration;

public class DefaultWaldOTAgentClientConfigurationV1 implements WaldOTAgentClientConfiguration {
	public static String DEFAULT_NEW_ORGANIZATION = null;
	public static String DEFAULT_NEW_CERTIFICATE_COMMON_NAME = null;
	public static String DEFAULT_NEW_CERTIFICATE_COUNTRY = null;
	public static String DEFAULT_NEW_CERTIFICATE_DNS = null;
	public static String DEFAULT_NEW_CERTIFICATE_DNS_ALIAS = null;
	public static String DEFAULT_NEW_CERTIFICATE_IP = null;
	public static String DEFAULT_NEW_CERTIFICATE_LOCALITY = null;
	public static String DEFAULT_NEW_CERTIFICATE_STATE = null;
	public static String DEFAULT_NEW_CERTIFICATE_UNIT = null;
	public static String DEFAULT_NEW_CERTIFICATE_APPLICATION_URI = null;

	private int acknowledgeTimeout = 5000;
	private String authMode = "none";
	private String authPrivateKey = null;
	private String authPublicCrt = null;
	private int channelLifetime = 60000;
	private String generateCertCommonName = DEFAULT_NEW_CERTIFICATE_COMMON_NAME;
	private int connectTimeout = 5000;
	private String generateCertCountry = DEFAULT_NEW_CERTIFICATE_COUNTRY;
	private String cryptoCertificateChainList = null;
	private String cryptoPrivateKey = null;
	private String cryptoPublicCrt = null;
	private String generateCertDns = DEFAULT_NEW_CERTIFICATE_DNS;
	private String generateCertDnsAlias = DEFAULT_NEW_CERTIFICATE_DNS_ALIAS;
	private String endpoint = null;
	private String generateCertIp = DEFAULT_NEW_CERTIFICATE_IP;
	private int keepAliveTimeout = 5000;
	private String generateCertLocality = DEFAULT_NEW_CERTIFICATE_LOCALITY;
	private int maxChunkCount = 0;
	private int maxChunkSize = 8196;
	private int maxMessageSize = 0;
	private String generateCertOrganization = DEFAULT_NEW_ORGANIZATION;
	private String password = null;
	private int requestTimeout = 60000;
	private MessageSecurityMode securityMode = MessageSecurityMode.None;
	private SecurityPolicy securityPolicy = SecurityPolicy.None;
	private UserTokenType authType = UserTokenType.Anonymous;
	private int sessionTimeout = 120000;
	private String generateCertState = DEFAULT_NEW_CERTIFICATE_STATE;
	private String generateCertUnit = DEFAULT_NEW_CERTIFICATE_UNIT;
	private String generateCertUri = DEFAULT_NEW_CERTIFICATE_APPLICATION_URI;
	private String userName = null;
	private String forceDiscoveryEndpointUrl = null;
	private boolean forceCertificateValidator = false;
	private boolean ignoreServiceFault = false;

	public int getAcknowledgeTimeout() {
		return acknowledgeTimeout;
	}

	public String getAuthMode() {
		return authMode;
	}

	public String getAuthPrivateKey() {
		return authPrivateKey;
	}

	public String getAuthPublicCrt() {
		return authPublicCrt;
	}

	public UserTokenType getAuthType() {
		return authType;
	}

	public int getChannelLifetime() {
		return channelLifetime;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public String getCryptoCertificateChainList() {
		return cryptoCertificateChainList;
	}

	public String getCryptoPrivateKey() {
		return cryptoPrivateKey;
	}

	public String getCryptoPublicCrt() {
		return cryptoPublicCrt;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getForceDiscoveryEndpointUrl() {
		return forceDiscoveryEndpointUrl;
	}

	public String getGenerateCertCommonName() {
		return generateCertCommonName;
	}

	public String getGenerateCertCountry() {
		return generateCertCountry;
	}

	public String getGenerateCertDns() {
		return generateCertDns;
	}

	public String getGenerateCertDnsAlias() {
		return generateCertDnsAlias;
	}

	public String getGenerateCertIp() {
		return generateCertIp;
	}

	public String getGenerateCertLocality() {
		return generateCertLocality;
	}

	public String getGenerateCertOrganization() {
		return generateCertOrganization;
	}

	public String getGenerateCertState() {
		return generateCertState;
	}

	public String getGenerateCertUnit() {
		return generateCertUnit;
	}

	public String getGenerateCertUri() {
		return generateCertUri;
	}

	public int getKeepAliveTimeout() {
		return keepAliveTimeout;
	}

	public int getMaxChunkCount() {
		return maxChunkCount;
	}

	public int getMaxChunkSize() {
		return maxChunkSize;
	}

	public int getMaxMessageSize() {
		return maxMessageSize;
	}

	public String getPassword() {
		return password;
	}

	public int getRequestTimeout() {
		return requestTimeout;
	}

	public MessageSecurityMode getSecurityMode() {
		return securityMode;
	}

	public SecurityPolicy getSecurityPolicy() {
		return securityPolicy;
	}

	public int getSessionTimeout() {
		return sessionTimeout;
	}

	public String getUserName() {
		return userName;
	}

	public boolean isForceCertificateValidator() {
		return forceCertificateValidator;
	}

	public boolean isIgnoreServiceFault() {
		return ignoreServiceFault;
	}

	public void setAcknowledgeTimeout(final int acknowledgeTimeout) {
		this.acknowledgeTimeout = acknowledgeTimeout;
	}

	public void setAuthMode(final String authMode) {
		this.authMode = authMode;
	}

	public void setAuthPrivateKey(final String authPrivateKey) {
		this.authPrivateKey = authPrivateKey;
	}

	public void setAuthPublicCrt(final String authPublicCrt) {
		this.authPublicCrt = authPublicCrt;
	}

	public void setAuthType(final UserTokenType authType) {
		this.authType = authType;
	}

	public void setChannelLifetime(final int channelLifetime) {
		this.channelLifetime = channelLifetime;
	}

	public void setConnectTimeout(final int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setCryptoCertificateChainList(final String cryptoCertificateChainList) {
		this.cryptoCertificateChainList = cryptoCertificateChainList;
	}

	public void setCryptoPrivateKey(final String cryptoPrivateKey) {
		this.cryptoPrivateKey = cryptoPrivateKey;
	}

	public void setCryptoPublicCrt(final String cryptoPublicCrt) {
		this.cryptoPublicCrt = cryptoPublicCrt;
	}

	public void setEndpoint(final String endpoint) {
		this.endpoint = endpoint;
	}

	public void setForceCertificateValidator(final boolean forceCertificateValidator) {
		this.forceCertificateValidator = forceCertificateValidator;
	}

	public void setForceDiscoveryEndpointUrl(final String forceDiscoveryEndpointUrl) {
		this.forceDiscoveryEndpointUrl = forceDiscoveryEndpointUrl;
	}

	public void setGenerateCertCommonName(final String generateCertCommonName) {
		this.generateCertCommonName = generateCertCommonName;
	}

	public void setGenerateCertCountry(final String generateCertCountry) {
		this.generateCertCountry = generateCertCountry;
	}

	public void setGenerateCertDns(final String generateCertDns) {
		this.generateCertDns = generateCertDns;
	}

	public void setGenerateCertDnsAlias(final String generateCertDnsAlias) {
		this.generateCertDnsAlias = generateCertDnsAlias;
	}

	public void setGenerateCertIp(final String generateCertIp) {
		this.generateCertIp = generateCertIp;
	}

	public void setGenerateCertLocality(final String generateCertLocality) {
		this.generateCertLocality = generateCertLocality;
	}

	public void setGenerateCertOrganization(final String generateCertOrganization) {
		this.generateCertOrganization = generateCertOrganization;
	}

	public void setGenerateCertState(final String generateCertState) {
		this.generateCertState = generateCertState;
	}

	public void setGenerateCertUnit(final String generateCertUnit) {
		this.generateCertUnit = generateCertUnit;
	}

	public void setGenerateCertUri(final String generateCertUri) {
		this.generateCertUri = generateCertUri;
	}

	public void setIgnoreServiceFault(final boolean ignoreServiceFault) {
		this.ignoreServiceFault = ignoreServiceFault;
	}

	public void setKeepAliveTimeout(final int keepAliveTimeout) {
		this.keepAliveTimeout = keepAliveTimeout;
	}

	public void setMaxChunkCount(final int maxChunkCount) {
		this.maxChunkCount = maxChunkCount;
	}

	public void setMaxChunkSize(final int maxChunkSize) {
		this.maxChunkSize = maxChunkSize;
	}

	public void setMaxMessageSize(final int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public void setRequestTimeout(final int requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public void setSecurityMode(final MessageSecurityMode securityMode) {
		this.securityMode = securityMode;
	}

	public void setSecurityPolicy(final SecurityPolicy securityPolicy) {
		this.securityPolicy = securityPolicy;
	}

	public void setSessionTimeout(final int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public void setUserName(final String userName) {
		this.userName = userName;
	}

}
