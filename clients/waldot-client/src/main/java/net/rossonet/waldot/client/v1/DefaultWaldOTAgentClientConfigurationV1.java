package net.rossonet.waldot.client.v1;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;

import net.rossonet.waldot.client.api.AuthenticationMode;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;

public class DefaultWaldOTAgentClientConfigurationV1
		implements
			WaldOTAgentClientConfiguration {
	/** System property overriding the default keystore path (used by tests). */
	public static final String KEYSTORE_PATH_PROPERTY = "waldot.client.keystore";
	/** System property overriding the default server TCP port (used by tests). */
	public static final String TCP_PORT_PROPERTY = "waldot.opc.tcp.port";

	public static int _DEFAULT_TCP_BIND_PORT = Integer.getInteger(TCP_PORT_PROPERTY, 12686);
	public static String DEFAULT_ENDPOINT = "opc.tcp://localhost:"
			+ _DEFAULT_TCP_BIND_PORT + "/waldot";
	public static String DEFAULT_KEYSTORE_PASSWORD = "_pa_ss_wo_rd_";
	public static String DEFAULT_KEYSTORE_PATH = System.getProperty(KEYSTORE_PATH_PROPERTY, "/tmp/waldot-client.ks");
	public static String DEFAULT_NEW_CERTIFICATE_APPLICATION_URI = "urn:rossonet:waldot:agent";
	// public static String DEFAULT_NEW_CERTIFICATE_COMMON_NAME = "";
	public static String DEFAULT_NEW_CERTIFICATE_COUNTRY = "IT";
	public static String DEFAULT_NEW_CERTIFICATE_DNS = "localhost";
	public static String DEFAULT_NEW_CERTIFICATE_DNS_ALIAS = "localhost.localdomain";
	public static String DEFAULT_NEW_CERTIFICATE_IP = "127.0.0.1";
	public static String DEFAULT_NEW_CERTIFICATE_LOCALITY = "Ferrara";
	public static String DEFAULT_NEW_CERTIFICATE_STATE = "Ferrara";
	public static String DEFAULT_NEW_CERTIFICATE_UNIT = "WaldOT Agent Client";
	public static String DEFAULT_NEW_ORGANIZATION = "Zona Autonoma srl";
	public static String DISCOVERY_PATH = "/discovery";
	public static final String DEFAULT_COMMANDS_ROOT_NODE_ID = "cmd";
	public static final String DEFAULT_WALDOT_NAMESPACE_URI = "urn:rossonet:waldot:engine";
	public static final long serialVersionUID = 977371251684189681L;

	private int acknowledgeTimeout = 5000;
	private String agentUniqueName = UUID.randomUUID().toString();
	private String applicationUri = DEFAULT_NEW_CERTIFICATE_APPLICATION_URI;
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

	private String keyStorePassword = DEFAULT_KEYSTORE_PASSWORD;

	private String keyStorePath = DEFAULT_KEYSTORE_PATH;

	private int maxChunkCount = 0;

	private int maxChunkSize = 8196;

	private int maxClientFaults;

	private int maxMessageSize = 0;

	private MessageSecurityMode messageSecurityMode = MessageSecurityMode.None;

	private String password;

	private int requestTimeout = 60000;
	private SecurityPolicy securityPolicy = SecurityPolicy.None;
	private int sessionTimeout = 120000;

	private String username;

	private AuthenticationMode authenticationMode = AuthenticationMode.ANONYMOUS;

	private String commandsRootNodeId = DEFAULT_COMMANDS_ROOT_NODE_ID;

	private double defaultPublishingInterval = 500.0;

	private double defaultSamplingInterval = 250.0;

	private long reconnectInitialDelay = 500L;

	private int reconnectMaxAttempts = 0;

	private long reconnectMaxDelay = 30_000L;

	private String waldotNamespaceUri = DEFAULT_WALDOT_NAMESPACE_URI;

	@Override
	public AuthenticationMode getAuthenticationMode() {
		return authenticationMode;
	}

	@Override
	public String getCommandsRootNodeId() {
		return commandsRootNodeId;
	}

	@Override
	public double getDefaultPublishingInterval() {
		return defaultPublishingInterval;
	}

	@Override
	public double getDefaultSamplingInterval() {
		return defaultSamplingInterval;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public long getReconnectInitialDelay() {
		return reconnectInitialDelay;
	}

	@Override
	public int getReconnectMaxAttempts() {
		return reconnectMaxAttempts;
	}

	@Override
	public long getReconnectMaxDelay() {
		return reconnectMaxDelay;
	}

	@Override
	public String getWaldotNamespaceUri() {
		return waldotNamespaceUri;
	}

	@Override
	public void setAuthenticationMode(final AuthenticationMode authenticationMode) {
		this.authenticationMode = authenticationMode == null ? AuthenticationMode.ANONYMOUS : authenticationMode;
	}

	@Override
	public void setCommandsRootNodeId(final String commandsRootNodeId) {
		this.commandsRootNodeId = commandsRootNodeId;
	}

	@Override
	public void setDefaultPublishingInterval(final double defaultPublishingInterval) {
		this.defaultPublishingInterval = defaultPublishingInterval;
	}

	@Override
	public void setDefaultSamplingInterval(final double defaultSamplingInterval) {
		this.defaultSamplingInterval = defaultSamplingInterval;
	}

	@Override
	public void setReconnectInitialDelay(final long reconnectInitialDelay) {
		this.reconnectInitialDelay = reconnectInitialDelay;
	}

	@Override
	public void setReconnectMaxAttempts(final int reconnectMaxAttempts) {
		this.reconnectMaxAttempts = reconnectMaxAttempts;
	}

	@Override
	public void setReconnectMaxDelay(final long reconnectMaxDelay) {
		this.reconnectMaxDelay = reconnectMaxDelay;
	}

	@Override
	public void setWaldotNamespaceUri(final String waldotNamespaceUri) {
		this.waldotNamespaceUri = waldotNamespaceUri;
	}
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
	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	@Override
	public String getKeyStorePath() {
		return keyStorePath;
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
		return authenticationMode == AuthenticationMode.CERTIFICATE && cryptoCertificateChain != null
				&& !cryptoCertificateChain.isEmpty() && cryptoPrivateKey != null;
	}

	@Override
	public boolean hasProvisioningToken() {
		// TODO completare metodo hasProvisioningToken
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
	public void setAcknowledgeTimeout(final int acknowledgeTimeout) {
		this.acknowledgeTimeout = acknowledgeTimeout;
	}

	@Override
	public void setAgentUniqueName(final String agentUniqueName) {
		this.agentUniqueName = agentUniqueName;
	}

	@Override
	public void setApplicationUri(final String applicationUri) {
		this.applicationUri = applicationUri;

	}

	@Override
	public void setBaseWaldOTOpcEndpoint(final String baseWaldOTOpcEndpoint) {
		this.baseWaldOTOpcEndpoint = baseWaldOTOpcEndpoint;
	}

	@Override
	public void setChannelLifetime(final int channelLifetime) {
		this.channelLifetime = channelLifetime;
	}

	@Override
	public void setConnectTimeout(final int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	@Override
	public void setCryptoCertificateChain(
			final List<X509Certificate> cryptoCertificateChain) {
		this.cryptoCertificateChain = cryptoCertificateChain;
	}

	@Override
	public void setCryptoPrivateKey(final PrivateKey cryptoPrivateKey) {
		this.cryptoPrivateKey = cryptoPrivateKey;
	}

	@Override
	public void setForceCertificateValidator(
			final boolean forceCertificateValidator) {
		this.forceCertificateValidator = forceCertificateValidator;
	}

	@Override
	public void setForceDiscoveredEndpointUrl(
			final String forceDiscoveredEndpointUrl) {
		this.forceDiscoveredEndpointUrl = forceDiscoveredEndpointUrl;
	}

	@Override
	public void setGenerateCertCountry(final String generateCertCountry) {
		this.generateCertCountry = generateCertCountry;
	}

	@Override
	public void setGenerateCertDns(final String generateCertDns) {
		this.generateCertDns = generateCertDns;
	}

	@Override
	public void setGenerateCertDnsAlias(final String generateCertDnsAlias) {
		this.generateCertDnsAlias = generateCertDnsAlias;
	}

	@Override
	public void setGenerateCertIp(final String generateCertIp) {
		this.generateCertIp = generateCertIp;
	}

	@Override
	public void setGenerateCertLocality(final String generateCertLocality) {
		this.generateCertLocality = generateCertLocality;
	}

	@Override
	public void setGenerateCertOrganization(
			final String generateCertOrganization) {
		this.generateCertOrganization = generateCertOrganization;
	}

	@Override
	public void setGenerateCertState(final String generateCertState) {
		this.generateCertState = generateCertState;
	}

	@Override
	public void setGenerateCertUnit(final String generateCertUnit) {
		this.generateCertUnit = generateCertUnit;
	}

	@Override
	public void setIgnoreServiceFault(final boolean ignoreServiceFault) {
		this.ignoreServiceFault = ignoreServiceFault;
	}

	@Override
	public void setKeepAliveTimeout(final int keepAliveTimeout) {
		this.keepAliveTimeout = keepAliveTimeout;
	}

	@Override
	public void setKeyStorePassword(final String password) {
		this.keyStorePassword = password;

	}

	@Override
	public void setKeyStorePath(final String path) {
		this.keyStorePath = path;

	}

	@Override
	public void setMaxChunkCount(final int maxChunkCount) {
		this.maxChunkCount = maxChunkCount;
	}

	@Override
	public void setMaxChunkSize(final int maxChunkSize) {
		this.maxChunkSize = maxChunkSize;
	}

	@Override
	public void setMaxClientFaults(final int maxClientFaults) {
		this.maxClientFaults = maxClientFaults;
	}

	@Override
	public void setMaxMessageSize(final int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	@Override
	public void setMessageSecurityMode(final MessageSecurityMode securityMode) {
		this.messageSecurityMode = securityMode;
	}

	@Override
	public void setPassword(final String password) {
		this.password = password;
	}

	@Override
	public void setRequestTimeout(final int requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	@Override
	public void setSecurityPolicy(final SecurityPolicy securityPolicy) {
		this.securityPolicy = securityPolicy;
	}

	@Override
	public void setSessionTimeout(final int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	@Override
	public void setUsername(final String username) {
		this.username = username;
	}

}
