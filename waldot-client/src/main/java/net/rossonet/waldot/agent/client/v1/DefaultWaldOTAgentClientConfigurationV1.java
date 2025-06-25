package net.rossonet.waldot.agent.client.v1;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient;
import net.rossonet.waldot.agent.client.api.WaldOTAgentClientConfiguration;

public class DefaultWaldOTAgentClientConfigurationV1 implements WaldOTAgentClientConfiguration {
	public static int _DEFAULT_TCP_BIND_PORT = 12686;
	public static String DEFAULT_ENDPOINT = "opc.tcp://localhost:" + _DEFAULT_TCP_BIND_PORT + "/waldot";
	public static String DEFAULT_NEW_CERTIFICATE_APPLICATION_URI = "urn:rossonet:waldot:agent";
	// public static String DEFAULT_NEW_CERTIFICATE_COMMON_NAME = "";
	public static String DEFAULT_NEW_CERTIFICATE_COUNTRY = "IT";
	public static String DEFAULT_NEW_CERTIFICATE_DNS = "localhost";
	public static String DEFAULT_NEW_CERTIFICATE_DNS_ALIAS = "localhost.localdomain";
	public static String DEFAULT_NEW_CERTIFICATE_IP = "127.0.0.1";
	public static String DEFAULT_NEW_CERTIFICATE_LOCALITY = "Imola";
	public static String DEFAULT_NEW_CERTIFICATE_STATE = "Bologna";
	public static String DEFAULT_NEW_CERTIFICATE_UNIT = "WaldOT Agent Client";
	public static String DEFAULT_NEW_ORGANIZATION = "Rossonet s.c.a r.l.";
	public static String DISCOVERY_PATH = "/discovery";
	private final static Logger logger = LoggerFactory.getLogger(WaldOTAgentClient.class);
	public static final long serialVersionUID = 977371251684189681L;

	private int acknowledgeTimeout = 5000;
	private String agentUniqueName = null;
	private String applicationUri = DEFAULT_NEW_CERTIFICATE_APPLICATION_URI;
	private List<X509Certificate> authCertificateChain = null;
	// private UserTokenType authMode = UserTokenType.Anonymous;
	private PrivateKey authPrivateKey = null;
	private String baseWaldOTOpcEndpoint = DEFAULT_ENDPOINT;
	private int channelLifetime = 60000;
	private int connectTimeout = 5000;
	private List<X509Certificate> cryptoCertificateChain = null;
	private PrivateKey cryptoPrivateKey = null;

	private boolean forceCertificateValidator = false;

	private String forceDiscoveredEndpointUrl = null;

	private String generateCertCountry = DEFAULT_NEW_CERTIFICATE_COUNTRY;

	private String generateCertDns = DEFAULT_NEW_CERTIFICATE_DNS;

	private String generateCertDnsAlias = DEFAULT_NEW_CERTIFICATE_DNS_ALIAS;

	private String generateCertIp = DEFAULT_NEW_CERTIFICATE_IP;

	private String generateCertLocality = DEFAULT_NEW_CERTIFICATE_LOCALITY;

	private String generateCertOrganization = DEFAULT_NEW_ORGANIZATION;

	private String generateCertState = DEFAULT_NEW_CERTIFICATE_STATE;

	private String generateCertUnit = DEFAULT_NEW_CERTIFICATE_UNIT;

	private boolean ignoreServiceFault = false;

	private int keepAliveTimeout = 5000;

	private int maxChunkCount = 0;

	private int maxChunkSize = 8196;

	private int maxClientFaults;

	private int maxMessageSize = 0;

	private MessageSecurityMode messageSecurityMode = MessageSecurityMode.None;

	private String password;

	protected PrivateKey provisioningPrivateKey = null;

	private X509Certificate provisioningPublicCrt = null;

	private String provisioningPublicCsr = null;

	private String provisioningUniqueId = null;

	private int requestTimeout = 60000;

	private SecurityPolicy securityPolicy = SecurityPolicy.None;

	private int sessionTimeout = 120000;
	private String username;

	@Override
	public int getAcknowledgeTimeout() {
		return acknowledgeTimeout;
	}

	@Override
	public String getAgentUniqueName() {
		return agentUniqueName;
	}

	@Override
	public String getApplicationName() {
		return agentUniqueName;
	}

	@Override
	public String getApplicationUri() {
		return applicationUri;
	}

	@Override
	public List<X509Certificate> getAuthCertificateChain() {
		return authCertificateChain;
	}

	@Override
	public PrivateKey getAuthPrivateKey() {
		return authPrivateKey;
	}

	@Override
	public String getBaseWaldOTOpcEndpoint() {
		return baseWaldOTOpcEndpoint;
	}

	@Override
	public int getChannelLifetime() {
		return channelLifetime;
	}

	@Override
	public int getConnectTimeout() {
		return connectTimeout;
	}

	@Override
	public List<X509Certificate> getCryptoCertificateChain() {
		return cryptoCertificateChain;
	}

	@Override
	public PrivateKey getCryptoPrivateKey() {
		return cryptoPrivateKey;
	}

	@Override
	public String getDiscoveryEndpoint() {
		return getBaseWaldOTOpcEndpoint() + DISCOVERY_PATH;
	}

	@Override
	public String getForceDiscoveredEndpointUrl() {
		return forceDiscoveredEndpointUrl;
	}

	@Override
	public String getGenerateCertCountry() {
		return generateCertCountry;
	}

	@Override
	public String getGenerateCertDns() {
		return generateCertDns;
	}

	@Override
	public String getGenerateCertDnsAlias() {
		return generateCertDnsAlias;
	}

	@Override
	public String getGenerateCertIp() {
		return generateCertIp;
	}

	@Override
	public String getGenerateCertLocality() {
		return generateCertLocality;
	}

	@Override
	public String getGenerateCertOrganization() {
		return generateCertOrganization;
	}

	@Override
	public String getGenerateCertState() {
		return generateCertState;
	}

	@Override
	public String getGenerateCertUnit() {
		return generateCertUnit;
	}

	@Override
	public int getKeepAliveTimeout() {
		return keepAliveTimeout;
	}

	@Override
	public int getMaxChunkCount() {
		return maxChunkCount;
	}

	@Override
	public int getMaxChunkSize() {
		return maxChunkSize;
	}

	@Override
	public int getMaxClientFaults() {
		return maxClientFaults;
	}

	@Override
	public int getMaxMessageSize() {
		return maxMessageSize;
	}

	@Override
	public MessageSecurityMode getMessageSecurityMode() {
		return messageSecurityMode;
	}

	@Override
	public PrivateKey getProvisioningPrivateKey() {
		return provisioningPrivateKey;
	}

	@Override
	public X509Certificate getProvisioningPublicCrt() {
		return provisioningPublicCrt;
	}

	@Override
	public String getProvisioningPublicCsr() {
		return provisioningPublicCsr;
	}

	@Override
	public String getProvisioningUniqueId() {
		return provisioningUniqueId;
	}

	@Override
	public int getRequestTimeout() {
		return requestTimeout;
	}

	@Override
	public SecurityPolicy getSecurityPolicy() {
		return securityPolicy;
	}

	@Override
	public int getSessionTimeout() {
		return sessionTimeout;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean hasCertificateAuthentication() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasProvisioningToken() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isForceCertificateValidator() {
		return forceCertificateValidator;
	}

	@Override
	public boolean isIgnoreServiceFault() {
		return ignoreServiceFault;
	}

	@Override
	public void setAcknowledgeTimeout(int acknowledgeTimeout) {
		this.acknowledgeTimeout = acknowledgeTimeout;
	}

	@Override
	public void setAgentUniqueName(String agentUniqueName) {
		this.agentUniqueName = agentUniqueName;
	}

	@Override
	public void setApplicationUri(String applicationUri) {
		this.applicationUri = applicationUri;

	}

	@Override
	public void setAuthCertificateChain(List<X509Certificate> authCertificateChain) {
		this.authCertificateChain = authCertificateChain;
	}

	@Override
	public void setAuthPrivateKey(PrivateKey authPrivateKey) {
		this.authPrivateKey = authPrivateKey;
	}

	@Override
	public void setBaseWaldOTOpcEndpoint(String baseWaldOTOpcEndpoint) {
		this.baseWaldOTOpcEndpoint = baseWaldOTOpcEndpoint;
	}

	@Override
	public void setChannelLifetime(int channelLifetime) {
		this.channelLifetime = channelLifetime;
	}

	@Override
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	@Override
	public void setCryptoCertificateChain(List<X509Certificate> cryptoCertificateChain) {
		this.cryptoCertificateChain = cryptoCertificateChain;
	}

	@Override
	public void setCryptoPrivateKey(PrivateKey cryptoPrivateKey) {
		this.cryptoPrivateKey = cryptoPrivateKey;
	}

	@Override
	public void setForceCertificateValidator(boolean forceCertificateValidator) {
		this.forceCertificateValidator = forceCertificateValidator;
	}

	@Override
	public void setForceDiscoveredEndpointUrl(String forceDiscoveredEndpointUrl) {
		this.forceDiscoveredEndpointUrl = forceDiscoveredEndpointUrl;
	}

	@Override
	public void setGenerateCertCountry(String generateCertCountry) {
		this.generateCertCountry = generateCertCountry;
	}

	@Override
	public void setGenerateCertDns(String generateCertDns) {
		this.generateCertDns = generateCertDns;
	}

	@Override
	public void setGenerateCertDnsAlias(String generateCertDnsAlias) {
		this.generateCertDnsAlias = generateCertDnsAlias;
	}

	@Override
	public void setGenerateCertIp(String generateCertIp) {
		this.generateCertIp = generateCertIp;
	}

	@Override
	public void setGenerateCertLocality(String generateCertLocality) {
		this.generateCertLocality = generateCertLocality;
	}

	@Override
	public void setGenerateCertOrganization(String generateCertOrganization) {
		this.generateCertOrganization = generateCertOrganization;
	}

	@Override
	public void setGenerateCertState(String generateCertState) {
		this.generateCertState = generateCertState;
	}

	@Override
	public void setGenerateCertUnit(String generateCertUnit) {
		this.generateCertUnit = generateCertUnit;
	}

	@Override
	public void setIgnoreServiceFault(boolean ignoreServiceFault) {
		this.ignoreServiceFault = ignoreServiceFault;
	}

	@Override
	public void setKeepAliveTimeout(int keepAliveTimeout) {
		this.keepAliveTimeout = keepAliveTimeout;
	}

	@Override
	public void setMaxChunkCount(int maxChunkCount) {
		this.maxChunkCount = maxChunkCount;
	}

	@Override
	public void setMaxChunkSize(int maxChunkSize) {
		this.maxChunkSize = maxChunkSize;
	}

	@Override
	public void setMaxClientFaults(int maxClientFaults) {
		this.maxClientFaults = maxClientFaults;
	}

	@Override
	public void setMaxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	@Override
	public void setMessageSecurityMode(MessageSecurityMode securityMode) {
		this.messageSecurityMode = securityMode;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void setProvisioningPrivateKey(PrivateKey provisioningPrivateKey) {
		this.provisioningPrivateKey = provisioningPrivateKey;
	}

	@Override
	public void setProvisioningPublicCrt(X509Certificate provisioningPublicCrt) {
		this.provisioningPublicCrt = provisioningPublicCrt;
	}

	@Override
	public void setProvisioningPublicCsr(String provisioningPublicCsr) {
		this.provisioningPublicCsr = provisioningPublicCsr;
	}

	@Override
	public void setProvisioningUniqueId(String provisioningUniqueId) {
		this.provisioningUniqueId = provisioningUniqueId;
	}

	@Override
	public void setRequestTimeout(int requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	@Override
	public void setSecurityPolicy(SecurityPolicy securityPolicy) {
		this.securityPolicy = securityPolicy;
	}

	@Override
	public void setSessionTimeout(int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

}
