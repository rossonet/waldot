package net.rossonet.waldot.client.api;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;

/**
 * Fluent builder of a {@link WaldOTAgentClientConfiguration}: endpoint,
 * security, identity, timeouts, reconnection policy and subscription
 * defaults. Every value not set keeps the default of
 * {@link WaldOTAgentClientConfiguration#getDefaultConfiguration()}.
 *
 * <pre>
 * WaldOTAgentClientConfiguration configuration = WaldotClientConfigurationBuilder.create()
 * 		.endpoint("opc.tcp://server:12686/waldot").username("user", "secret")
 * 		.reconnect(Duration.ofMillis(500), Duration.ofSeconds(30), 0).build();
 * </pre>
 */
public final class WaldotClientConfigurationBuilder {

	/** Starts from the default configuration. */
	public static WaldotClientConfigurationBuilder create() {
		return new WaldotClientConfigurationBuilder(WaldOTAgentClientConfiguration.getDefaultConfiguration());
	}

	/** Customizes an existing configuration (it is modified in place). */
	public static WaldotClientConfigurationBuilder from(final WaldOTAgentClientConfiguration configuration) {
		return new WaldotClientConfigurationBuilder(Objects.requireNonNull(configuration, "configuration"));
	}

	private static int millis(final Duration duration, final String name) {
		if (duration == null || duration.isNegative() || duration.isZero()) {
			throw new IllegalArgumentException(name + " must be a positive duration");
		}
		return (int) Math.min(Integer.MAX_VALUE, duration.toMillis());
	}

	private final WaldOTAgentClientConfiguration configuration;

	private WaldotClientConfigurationBuilder(final WaldOTAgentClientConfiguration configuration) {
		this.configuration = configuration;
	}

	/** Unique name of the client (also common name of its certificate). */
	public WaldotClientConfigurationBuilder agentName(final String agentUniqueName) {
		configuration.setAgentUniqueName(agentUniqueName);
		return this;
	}

	/** Anonymous identity (default). */
	public WaldotClientConfigurationBuilder anonymous() {
		configuration.setAuthenticationMode(AuthenticationMode.ANONYMOUS);
		return this;
	}

	/** Application URI of the client. */
	public WaldotClientConfigurationBuilder applicationUri(final String applicationUri) {
		configuration.setApplicationUri(applicationUri);
		return this;
	}

	/**
	 * Validates and returns the configuration.
	 *
	 * @throws IllegalStateException if the values are inconsistent
	 */
	public WaldOTAgentClientConfiguration build() {
		if (configuration.getBaseWaldOTOpcEndpoint() == null || configuration.getBaseWaldOTOpcEndpoint().isBlank()) {
			throw new IllegalStateException("endpoint not set");
		}
		if (configuration.getReconnectInitialDelay() <= 0) {
			throw new IllegalStateException("reconnect initial delay must be > 0");
		}
		if (configuration.getReconnectMaxDelay() < configuration.getReconnectInitialDelay()) {
			throw new IllegalStateException("reconnect max delay must be >= initial delay");
		}
		if (configuration.getReconnectMaxAttempts() < 0) {
			throw new IllegalStateException("reconnect max attempts must be >= 0");
		}
		if (configuration.getAuthenticationMode() == AuthenticationMode.USERNAME
				&& (configuration.getUsername() == null || configuration.getUsername().isBlank())) {
			throw new IllegalStateException("username not set");
		}
		if (configuration.getAuthenticationMode() == AuthenticationMode.CERTIFICATE
				&& (configuration.getCryptoCertificateChain() == null
						|| configuration.getCryptoCertificateChain().isEmpty()
						|| configuration.getCryptoPrivateKey() == null)) {
			throw new IllegalStateException("certificate chain and private key are required");
		}
		return configuration;
	}

	/** X509 identity. */
	public WaldotClientConfigurationBuilder certificate(final List<X509Certificate> chain,
			final PrivateKey privateKey) {
		configuration.setAuthenticationMode(AuthenticationMode.CERTIFICATE);
		configuration.setCryptoCertificateChain(chain);
		configuration.setCryptoPrivateKey(privateKey);
		return this;
	}

	/** NodeId identifier of the commands root folder (default {@code cmd}). */
	public WaldotClientConfigurationBuilder commandsRootNodeId(final String nodeId) {
		configuration.setCommandsRootNodeId(nodeId);
		return this;
	}

	/** Default publishing interval (ms) of the subscription groups. */
	public WaldotClientConfigurationBuilder defaultPublishingInterval(final double milliseconds) {
		if (milliseconds < 0) {
			throw new IllegalArgumentException("publishing interval must be >= 0");
		}
		configuration.setDefaultPublishingInterval(milliseconds);
		return this;
	}

	/** Default sampling interval (ms) of the monitored items. */
	public WaldotClientConfigurationBuilder defaultSamplingInterval(final double milliseconds) {
		configuration.setDefaultSamplingInterval(milliseconds);
		return this;
	}

	/** Base endpoint of the server, e.g. {@code opc.tcp://host:12686/waldot}. */
	public WaldotClientConfigurationBuilder endpoint(final String endpoint) {
		configuration.setBaseWaldOTOpcEndpoint(endpoint);
		return this;
	}

	/** Replaces the URL of the discovered endpoint (NAT, docker). */
	public WaldotClientConfigurationBuilder forceDiscoveredEndpointUrl(final String url) {
		configuration.setForceDiscoveredEndpointUrl(url);
		return this;
	}

	/** Session keep alive timeout. */
	public WaldotClientConfigurationBuilder keepAliveTimeout(final Duration timeout) {
		configuration.setKeepAliveTimeout(millis(timeout, "keepAliveTimeout"));
		return this;
	}

	/** Keystore of the application certificate. */
	public WaldotClientConfigurationBuilder keyStore(final String path, final String password) {
		configuration.setKeyStorePath(path);
		configuration.setKeyStorePassword(password);
		return this;
	}

	/**
	 * Reconnection policy: exponential backoff from {@code initialDelay} to
	 * {@code maxDelay}; {@code maxAttempts} consecutive failures (0 = no
	 * limit) lead to {@link WaldOTAgentClient.Status#UNRECOVERABLE_FAULTED}.
	 */
	public WaldotClientConfigurationBuilder reconnect(final Duration initialDelay, final Duration maxDelay,
			final int maxAttempts) {
		configuration.setReconnectInitialDelay(millis(initialDelay, "initialDelay"));
		configuration.setReconnectMaxDelay(millis(maxDelay, "maxDelay"));
		configuration.setReconnectMaxAttempts(maxAttempts);
		return this;
	}

	/** Timeout of every request. */
	public WaldotClientConfigurationBuilder requestTimeout(final Duration timeout) {
		configuration.setRequestTimeout(millis(timeout, "requestTimeout"));
		return this;
	}

	/** Security policy and mode of the endpoint to use. */
	public WaldotClientConfigurationBuilder security(final SecurityPolicy policy, final MessageSecurityMode mode) {
		configuration.setSecurityPolicy(Objects.requireNonNull(policy, "policy"));
		configuration.setMessageSecurityMode(Objects.requireNonNull(mode, "mode"));
		return this;
	}

	/** Session timeout. */
	public WaldotClientConfigurationBuilder sessionTimeout(final Duration timeout) {
		configuration.setSessionTimeout(millis(timeout, "sessionTimeout"));
		return this;
	}

	/** Accepts every server certificate (tests and development only). */
	public WaldotClientConfigurationBuilder trustAllServerCertificates(final boolean trustAll) {
		configuration.setForceCertificateValidator(trustAll);
		return this;
	}

	/** Username and password identity. */
	public WaldotClientConfigurationBuilder username(final String username, final String password) {
		configuration.setAuthenticationMode(AuthenticationMode.USERNAME);
		configuration.setUsername(username);
		configuration.setPassword(password);
		return this;
	}

	/** URI of the WaldOT namespace (default {@code urn:rossonet:waldot:engine}). */
	public WaldotClientConfigurationBuilder waldotNamespaceUri(final String uri) {
		configuration.setWaldotNamespaceUri(uri);
		return this;
	}
}
