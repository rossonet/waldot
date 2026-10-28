package net.rossonet.waldot.client.support;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.milo.opcua.sdk.client.DiscoveryClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.identity.X509IdentityProvider;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.client.api.AuthenticationMode;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.SslHelper.KeyStoreHelper;

/**
 * Builds the Milo client configuration from a
 * {@link WaldOTAgentClientConfiguration}: application certificate (kept in a
 * PKCS12 keystore, self signed on first use), endpoint discovery, identity
 * provider and certificate validator. Shared by every client version.
 */
public final class OpcClientConfigurator {

	public static final String SELFSIGNED_CERTIFICATE_ALIAS = "waldot-selfsigned";
	public static final String SIGNED_CERTIFICATE_ALIAS = "waldot-signed";

	private static final Logger logger = LoggerFactory.getLogger(OpcClientConfigurator.class);

	private final KeyStoreHelper certificateHelper;

	private final WaldOTAgentClientConfiguration configuration;

	public OpcClientConfigurator(final WaldOTAgentClientConfiguration configuration) {
		this.configuration = configuration;
		certificateHelper = new KeyStoreHelper(configuration.getKeyStorePath(), configuration.getKeyStorePassword());
		ensureCertificate();
	}

	/**
	 * Builds the client configuration, discovering the endpoint that matches
	 * the security policy and mode of the configuration.
	 *
	 * @throws UaException if no matching endpoint is found
	 */
	public OpcUaClientConfig build() throws UaException {
		return configBuilder().build();
	}

	/** Same as {@link #build()} but returns the builder to customize it. */
	public OpcUaClientConfigBuilder configBuilder() throws UaException {
		final EndpointDescription endpoint = discoverEndpoint();
		final OpcUaClientConfigBuilder config = OpcUaClientConfig.builder().setEndpoint(endpoint)
				.setApplicationUri(configuration.getApplicationUri())
				.setApplicationName(LocalizedText.english(configuration.getApplicationName()));
		config.setSessionTimeout(UInteger.valueOf(configuration.getSessionTimeout()));
		config.setKeepAliveTimeout(UInteger.valueOf(configuration.getKeepAliveTimeout()));
		config.setRequestTimeout(UInteger.valueOf(configuration.getRequestTimeout()));
		config.setIdentityProvider(identityProvider());
		final KeyPair keyPair = keyPair();
		if (keyPair != null) {
			config.setKeyPair(keyPair);
		}
		final X509Certificate certificate = certificate();
		// with SecurityPolicy None the certificate is not needed (and it would
		// be sent to the server in CreateSession)
		if (certificate != null && configuration.getSecurityPolicy() != SecurityPolicy.None) {
			config.setCertificate(certificate);
		}
		if (configuration.isForceCertificateValidator()) {
			config.setCertificateValidator(trustAllValidator());
		}
		return config;
	}

	/** @return the application certificate (signed, otherwise self signed) */
	public X509Certificate certificate() {
		try {
			if (certificateHelper.hasCertificate(SIGNED_CERTIFICATE_ALIAS)) {
				return certificateHelper.getCertificate(SIGNED_CERTIFICATE_ALIAS);
			}
			return certificateHelper.getCertificate(SELFSIGNED_CERTIFICATE_ALIAS);
		} catch (final Exception e) {
			logger.error("Error reading the client certificate: {}", LogHelper.stackTraceToString(e, 5));
			return null;
		}
	}

	/** @return the helper of the client keystore */
	public KeyStoreHelper certificateHelper() {
		return certificateHelper;
	}

	/**
	 * Discovers the endpoints of the server and returns the one matching the
	 * configured security policy and mode.
	 */
	public EndpointDescription discoverEndpoint() throws UaException {
		final String discoveryEndpoint = configuration.getDiscoveryEndpoint();
		final List<EndpointDescription> endpoints;
		try {
			logger.debug("Discovering OPC UA endpoints on: {}", discoveryEndpoint);
			endpoints = DiscoveryClient.getEndpoints(discoveryEndpoint).get();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UaException(e);
		} catch (final Exception e) {
			throw new UaException(e);
		}
		final EndpointDescription endpoint = selectEndpoint(endpoints, configuration);
		if (endpoint == null) {
			throw new UaException(org.eclipse.milo.opcua.stack.core.StatusCodes.Bad_ConfigurationError,
					"no endpoint found on " + discoveryEndpoint + " with security policy "
							+ configuration.getSecurityPolicy().getUri() + " and mode "
							+ configuration.getMessageSecurityMode());
		}
		logServerCertificate(endpoint);
		return endpoint;
	}

	private void ensureCertificate() {
		try {
			if (certificateHelper.hasCertificate(SELFSIGNED_CERTIFICATE_ALIAS)
					|| certificateHelper.hasCertificate(SIGNED_CERTIFICATE_ALIAS)) {
				logger.debug("Certificate already exists in keystore, using it");
				return;
			}
		} catch (final Exception e) {
			logger.error("Error while checking certificate in keystore: {}", LogHelper.stackTraceToString(e, 5));
		}
		logger.info("Certificate not found in keystore {}, creating a self-signed certificate",
				configuration.getKeyStorePath());
		certificateHelper.createSelfSignedCertificate(SELFSIGNED_CERTIFICATE_ALIAS, configuration.getAgentUniqueName(),
				configuration.getGenerateCertOrganization(), configuration.getGenerateCertUnit(),
				configuration.getGenerateCertLocality(), configuration.getGenerateCertState(),
				configuration.getGenerateCertCountry(), null, configuration.getGenerateCertDns(),
				configuration.getGenerateCertIp(), configuration.getGenerateCertDnsAlias());
	}

	/** @return the identity provider of the configured authentication mode */
	public IdentityProvider identityProvider() {
		final AuthenticationMode mode = configuration.getAuthenticationMode() == null ? AuthenticationMode.ANONYMOUS
				: configuration.getAuthenticationMode();
		switch (mode) {
		case USERNAME:
			if (configuration.getUsername() != null && !configuration.getUsername().isEmpty()
					&& configuration.getPassword() != null) {
				return new UsernameProvider(configuration.getUsername(), configuration.getPassword());
			}
			logger.warn("Username and/or password not set for USERNAME authentication, using anonymous identity");
			return new AnonymousProvider();
		case CERTIFICATE:
			if (configuration.getCryptoCertificateChain() != null
					&& !configuration.getCryptoCertificateChain().isEmpty()
					&& configuration.getCryptoPrivateKey() != null) {
				return new X509IdentityProvider(configuration.getCryptoCertificateChain().get(0),
						configuration.getCryptoPrivateKey());
			}
			logger.warn("Certificate and/or private key not set for CERTIFICATE authentication, using anonymous");
			return new AnonymousProvider();
		case ANONYMOUS:
		default:
			return new AnonymousProvider();
		}
	}

	/** @return the key pair of the application certificate */
	public KeyPair keyPair() {
		try {
			if (certificateHelper.hasCertificate(SIGNED_CERTIFICATE_ALIAS)) {
				return certificateHelper.getKeyPair(SIGNED_CERTIFICATE_ALIAS);
			}
			return certificateHelper.getKeyPair(SELFSIGNED_CERTIFICATE_ALIAS);
		} catch (final Exception e) {
			logger.error("Error reading the client key pair: {}", LogHelper.stackTraceToString(e, 5));
			return null;
		}
	}

	private static void logServerCertificate(final EndpointDescription endpoint) {
		try {
			if (endpoint.getServerCertificate() != null && !endpoint.getServerCertificate().isNullOrEmpty()) {
				logger.debug("Server certificate for endpoint {}: {}", endpoint.getEndpointUrl(),
						CertificateFactory.getInstance("X.509").generateCertificate(
								new ByteArrayInputStream(endpoint.getServerCertificate().bytes())));
			}
		} catch (final Exception e) {
			logger.warn("Error while reading the server certificate: {}", e.getMessage());
		}
	}

	/**
	 * Selects the endpoint with the configured security policy and mode,
	 * replacing its URL when {@code forceDiscoveredEndpointUrl} is set.
	 *
	 * @return the endpoint or null
	 */
	public static EndpointDescription selectEndpoint(final List<EndpointDescription> endpoints,
			final WaldOTAgentClientConfiguration configuration) {
		if (endpoints == null) {
			return null;
		}
		final MessageSecurityMode mode = configuration.getMessageSecurityMode();
		final String policy = configuration.getSecurityPolicy().getUri();
		for (final EndpointDescription e : endpoints) {
			if (policy.equals(e.getSecurityPolicyUri()) && mode.equals(e.getSecurityMode())) {
				final String forced = configuration.getForceDiscoveredEndpointUrl();
				if (forced != null && !forced.isEmpty()) {
					logger.warn("force discovery endpoint URL: {}", forced);
					return EndpointUtil.updateUrl(e, forced);
				}
				return e;
			}
		}
		return null;
	}

	private CertificateValidator trustAllValidator() {
		return new CertificateValidator() {
			@Override
			public void validateCertificateChain(final List<X509Certificate> certificateChain,
					final String applicationUri, final String... validHostNames) throws UaException {
				logger.warn("forceCertificateValidator is true: server certificate chain of {} accepted "
						+ "without validation ({} certificates)", applicationUri,
						certificateChain == null ? 0 : certificateChain.size());
			}
		};
	}
}
