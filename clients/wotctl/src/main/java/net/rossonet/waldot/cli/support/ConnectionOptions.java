package net.rossonet.waldot.cli.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Locale;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;

import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.api.WaldotClientConfigurationBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/**
 * Connection options, common to every command that talks to the server. Each
 * option has an environment variable {@code WALDOT_*} used when the option is
 * not given (useful with {@code docker run -e}).
 */
public class ConnectionOptions {

	/** Default timeout of the connection. */
	public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration
			.ofSeconds(10);

	/** Default endpoint. */
	public static final String DEFAULT_ENDPOINT = "opc.tcp://localhost:12686/waldot";

	private static Duration duration(final CommandLine commandLine,
			final String option, final String value) {
		try {
			final Duration duration = Durations.parse(value);
			if (duration.isZero()) {
				throw new IllegalArgumentException(
						"la durata deve essere positiva");
			}
			return duration;
		} catch (final IllegalArgumentException e) {
			throw new ParameterException(commandLine,
					option + ": " + e.getMessage(), e, null, value);
		}
	}

	private static String env(final String name) {
		final String value = System.getenv(name);
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static String env(final String name, final String defaultValue) {
		final String value = env(name);
		return value == null ? defaultValue : value;
	}

	private static MessageSecurityMode mode(final CommandLine commandLine,
			final String value) {
		for (final MessageSecurityMode mode : MessageSecurityMode.values()) {
			if (mode != MessageSecurityMode.Invalid
					&& mode.name().equalsIgnoreCase(value.trim())) {
				return mode;
			}
		}
		throw new ParameterException(commandLine, "--security-mode: valore '"
				+ value + "' non valido (None, Sign, SignAndEncrypt)");
	}

	private static SecurityPolicy policy(final CommandLine commandLine,
			final String value) {
		for (final SecurityPolicy policy : SecurityPolicy.values()) {
			if (policy.name().equalsIgnoreCase(value.trim())) {
				return policy;
			}
		}
		throw new ParameterException(commandLine, "--security-policy: valore '"
				+ value
				+ "' non valido (None, Basic128Rsa15, Basic256, Basic256Sha256, Aes128_Sha256_RsaOaep, Aes256_Sha256_RsaPss)");
	}

	@Option(names = "--agent-name", paramLabel = "NOME", order = 13, description = {
			"Nome univoco del client (common name del certificato applicativo).",
			"Variabile: WALDOT_AGENT_NAME."})
	String agentName = env("WALDOT_AGENT_NAME");

	@Option(names = "--application-uri", paramLabel = "URI", order = 12, description = {
			"Application URI del client. Variabile: WALDOT_APPLICATION_URI."})
	String applicationUri = env("WALDOT_APPLICATION_URI");

	@Option(names = "--commands-root", paramLabel = "ID", order = 22, description = {
			"Identificatore della cartella dei comandi globali (predefinito cmd).",
			"Variabile: WALDOT_COMMANDS_ROOT."})
	String commandsRoot = env("WALDOT_COMMANDS_ROOT");

	@Option(names = "--connect-timeout", paramLabel = "DURATA", order = 14, description = {
			"Tempo massimo per la connessione (predefinito 10s). Le durate si scrivono",
			"come 500ms, 10s, 2m, 1h, 1m30s, PT10S o come numero di secondi.",
			"Variabile: WALDOT_CONNECT_TIMEOUT."})
	String connectTimeout = env("WALDOT_CONNECT_TIMEOUT");

	@Option(names = "--endpoint", paramLabel = "URL", order = 1, description = {
			"Endpoint OPC UA del server WaldOT (predefinito: ${DEFAULT-VALUE}).",
			"Variabile: WALDOT_ENDPOINT."})
	String endpoint = env("WALDOT_ENDPOINT", DEFAULT_ENDPOINT);

	@Option(names = "--force-endpoint-url", paramLabel = "URL", order = 11, description = {
			"Sostituisce l'URL dell'endpoint scoperto (server dietro NAT o in un container).",
			"Variabile: WALDOT_FORCE_ENDPOINT_URL."})
	String forceEndpointUrl = env("WALDOT_FORCE_ENDPOINT_URL");

	@Option(names = "--identity-cert", paramLabel = "PEM", order = 6, description = {
			"Certificato X509 dell'utente (PEM o DER, anche con la catena) per l'identità",
			"a certificato; richiede --identity-key. Variabile: WALDOT_IDENTITY_CERT."})
	Path identityCert = env("WALDOT_IDENTITY_CERT") == null
			? null
			: Path.of(env("WALDOT_IDENTITY_CERT"));

	@Option(names = "--identity-key", paramLabel = "PEM", order = 7, description = {
			"Chiave privata non cifrata dell'identità X509 (PEM PKCS#8 'PRIVATE KEY' o",
			"PKCS#1 'RSA PRIVATE KEY'). Variabile: WALDOT_IDENTITY_KEY."})
	Path identityKey = env("WALDOT_IDENTITY_KEY") == null
			? null
			: Path.of(env("WALDOT_IDENTITY_KEY"));

	@Option(names = "--keepalive-timeout", paramLabel = "DURATA", order = 17, description = {
			"Timeout del keep alive della sessione. Variabile: WALDOT_KEEPALIVE_TIMEOUT."})
	String keepaliveTimeout = env("WALDOT_KEEPALIVE_TIMEOUT");

	@Option(names = "--keystore", paramLabel = "FILE", order = 8, description = {
			"Keystore PKCS12 con il certificato applicativo del client (creato se non esiste;",
			"predefinito /tmp/waldot-client.ks). Variabile: WALDOT_KEYSTORE."})
	Path keystore = env("WALDOT_KEYSTORE") == null
			? null
			: Path.of(env("WALDOT_KEYSTORE"));

	@Option(names = "--keystore-password", paramLabel = "PASSWORD", order = 9, description = {
			"Password del keystore applicativo. Variabile: WALDOT_KEYSTORE_PASSWORD."})
	String keystorePassword = env("WALDOT_KEYSTORE_PASSWORD");

	@Option(names = "--namespace-uri", paramLabel = "URI", order = 21, description = {
			"URI del namespace WaldOT (predefinito urn:rossonet:waldot:engine).",
			"Variabile: WALDOT_NAMESPACE_URI."})
	String namespaceUri = env("WALDOT_NAMESPACE_URI");

	@Option(names = "--password-file", paramLabel = "FILE", order = 5, description = {
			"File con la password (si usa la prima riga; '-' = stdin).",
			"Variabile: WALDOT_PASSWORD_FILE (in alternativa WALDOT_PASSWORD con la password)."})
	Path passwordFile = env("WALDOT_PASSWORD_FILE") == null
			? null
			: Path.of(env("WALDOT_PASSWORD_FILE"));

	@Option(names = "--reconnect-attempts", paramLabel = "N", order = 20, description = {
			"Tentativi consecutivi falliti dopo i quali il client rinuncia (0 = nessun",
			"limite). Con subscribe la rinuncia termina il comando con codice 3.",
			"Variabile: WALDOT_RECONNECT_ATTEMPTS."})
	String reconnectAttempts = env("WALDOT_RECONNECT_ATTEMPTS");

	@Option(names = "--reconnect-initial", paramLabel = "DURATA", order = 18, description = {
			"Primo ritardo della riconnessione (backoff esponenziale).",
			"Variabile: WALDOT_RECONNECT_INITIAL."})
	String reconnectInitial = env("WALDOT_RECONNECT_INITIAL");

	@Option(names = "--reconnect-max", paramLabel = "DURATA", order = 19, description = {
			"Ritardo massimo tra due tentativi di riconnessione.",
			"Variabile: WALDOT_RECONNECT_MAX."})
	String reconnectMax = env("WALDOT_RECONNECT_MAX");

	@Option(names = "--request-timeout", paramLabel = "DURATA", order = 15, description = {
			"Timeout di ogni richiesta OPC UA. Variabile: WALDOT_REQUEST_TIMEOUT."})
	String requestTimeout = env("WALDOT_REQUEST_TIMEOUT");

	@Option(names = "--security-mode", paramLabel = "MODO", order = 3, completionCandidates = Completions.SecurityModes.class, description = {
			"Modalità dei messaggi: None, Sign, SignAndEncrypt (predefinita: None, oppure",
			"SignAndEncrypt se la policy non è None). Variabile: WALDOT_SECURITY_MODE."})
	String securityMode = env("WALDOT_SECURITY_MODE");

	@Option(names = "--security-policy", paramLabel = "POLICY", order = 2, completionCandidates = Completions.SecurityPolicies.class, description = {
			"Security policy: None, Basic128Rsa15, Basic256, Basic256Sha256,",
			"Aes128_Sha256_RsaOaep, Aes256_Sha256_RsaPss (predefinita: None, oppure",
			"Basic256Sha256 se è indicato solo --security-mode). Variabile: WALDOT_SECURITY_POLICY."})
	String securityPolicy = env("WALDOT_SECURITY_POLICY");

	@Option(names = "--session-timeout", paramLabel = "DURATA", order = 16, description = {
			"Timeout della sessione OPC UA. Variabile: WALDOT_SESSION_TIMEOUT."})
	String sessionTimeout = env("WALDOT_SESSION_TIMEOUT");

	@Option(names = "--trust-all", order = 10, description = {
			"Accetta qualunque certificato del server (solo test e sviluppo).",
			"Variabile: WALDOT_TRUST_ALL=true."})
	boolean trustAll = Boolean.parseBoolean(env("WALDOT_TRUST_ALL", "false"));

	@Option(names = "--username", paramLabel = "UTENTE", order = 4, description = {
			"Identità utente/password; la password si legge da --password-file o dalla",
			"variabile WALDOT_PASSWORD (non esiste un'opzione con la password in chiaro).",
			"Variabile: WALDOT_USERNAME."})
	String username = env("WALDOT_USERNAME");

	/** @return the timeout of the connection */
	public Duration connectTimeout(final CommandLine commandLine) {
		return connectTimeout == null
				? DEFAULT_CONNECT_TIMEOUT
				: duration(commandLine, "--connect-timeout", connectTimeout);
	}

	/** @return the endpoint */
	public String endpoint() {
		return endpoint;
	}

	private String password(final CommandLine commandLine) {
		if (passwordFile != null) {
			try {
				final String content = "-".equals(passwordFile.toString())
						? new String(System.in.readAllBytes(),
								StandardCharsets.UTF_8)
						: Files.readString(passwordFile,
								StandardCharsets.UTF_8);
				final int end = content.indexOf('\n');
				return (end >= 0 ? content.substring(0, end) : content)
						.replace("\r", "");
			} catch (final IOException e) {
				throw new ParameterException(commandLine,
						"--password-file: impossibile leggere " + passwordFile
								+ ": " + e.getMessage());
			}
		}
		final String password = System.getenv("WALDOT_PASSWORD");
		if (password == null) {
			throw new ParameterException(commandLine,
					"--username richiede la password: usare --password-file o la variabile WALDOT_PASSWORD");
		}
		return password;
	}

	/**
	 * Builds the configuration of the client.
	 *
	 * @throws ParameterException if an option is not valid
	 */
	public WaldOTAgentClientConfiguration toConfiguration(
			final CommandLine commandLine) {
		final WaldOTAgentClientConfiguration configuration = WaldOTAgentClientConfiguration
				.getDefaultConfiguration();
		final WaldotClientConfigurationBuilder builder = WaldotClientConfigurationBuilder
				.from(configuration);
		try {
			builder.endpoint(endpoint == null ? DEFAULT_ENDPOINT : endpoint);
			if (securityPolicy != null || securityMode != null) {
				SecurityPolicy policy = securityPolicy == null
						? null
						: policy(commandLine, securityPolicy);
				MessageSecurityMode mode = securityMode == null
						? null
						: mode(commandLine, securityMode);
				if (policy == null) {
					policy = mode == MessageSecurityMode.None
							? SecurityPolicy.None
							: SecurityPolicy.Basic256Sha256;
				}
				if (mode == null) {
					mode = policy == SecurityPolicy.None
							? MessageSecurityMode.None
							: MessageSecurityMode.SignAndEncrypt;
				}
				if (policy == SecurityPolicy.None != (mode == MessageSecurityMode.None)) {
					throw new ParameterException(commandLine,
							"combinazione non valida: --security-policy "
									+ policy.name() + " con --security-mode "
									+ mode.name());
				}
				builder.security(policy, mode);
			}
			if (username != null
					&& (identityCert != null || identityKey != null)) {
				throw new ParameterException(commandLine,
						"--username e --identity-cert/--identity-key sono alternativi");
			}
			if (username != null) {
				builder.username(username, password(commandLine));
			}
			if (identityCert != null || identityKey != null) {
				if (identityCert == null || identityKey == null) {
					throw new ParameterException(commandLine,
							"l'identità X509 richiede sia --identity-cert sia --identity-key");
				}
				try {
					builder.certificate(Pem.certificates(identityCert),
							Pem.privateKey(identityKey));
				} catch (final IOException | GeneralSecurityException e) {
					throw new ParameterException(commandLine,
							"identità X509 non valida: " + e.getMessage());
				}
			}
			if (keystore != null || keystorePassword != null) {
				builder.keyStore(
						keystore == null
								? configuration.getKeyStorePath()
								: keystore.toString(),
						keystorePassword == null
								? configuration.getKeyStorePassword()
								: keystorePassword);
			}
			if (trustAll) {
				builder.trustAllServerCertificates(true);
			}
			if (forceEndpointUrl != null) {
				builder.forceDiscoveredEndpointUrl(forceEndpointUrl);
			}
			if (applicationUri != null) {
				builder.applicationUri(applicationUri);
			}
			if (agentName != null) {
				builder.agentName(agentName);
			}
			final Duration connect = connectTimeout(commandLine);
			configuration.setConnectTimeout(
					(int) Math.min(Integer.MAX_VALUE, connect.toMillis()));
			if (requestTimeout != null) {
				builder.requestTimeout(duration(commandLine,
						"--request-timeout", requestTimeout));
			}
			if (sessionTimeout != null) {
				builder.sessionTimeout(duration(commandLine,
						"--session-timeout", sessionTimeout));
			}
			if (keepaliveTimeout != null) {
				builder.keepAliveTimeout(duration(commandLine,
						"--keepalive-timeout", keepaliveTimeout));
			}
			if (reconnectInitial != null || reconnectMax != null
					|| reconnectAttempts != null) {
				final Duration initial = reconnectInitial == null
						? Duration.ofMillis(
								configuration.getReconnectInitialDelay())
						: duration(commandLine, "--reconnect-initial",
								reconnectInitial);
				Duration max = reconnectMax == null
						? Duration
								.ofMillis(configuration.getReconnectMaxDelay())
						: duration(commandLine, "--reconnect-max",
								reconnectMax);
				if (reconnectMax == null && max.compareTo(initial) < 0) {
					max = initial;
				}
				int attempts = configuration.getReconnectMaxAttempts();
				if (reconnectAttempts != null) {
					try {
						attempts = Integer.parseInt(reconnectAttempts.trim());
					} catch (final NumberFormatException e) {
						throw new ParameterException(commandLine,
								"--reconnect-attempts: numero non valido '"
										+ reconnectAttempts + "'");
					}
				}
				builder.reconnect(initial, max, attempts);
			}
			if (namespaceUri != null) {
				builder.waldotNamespaceUri(namespaceUri);
			}
			if (commandsRoot != null) {
				builder.commandsRootNodeId(commandsRoot);
			}
			return builder.build();
		} catch (final IllegalArgumentException | IllegalStateException e) {
			throw new ParameterException(commandLine,
					"configurazione della connessione non valida: "
							+ e.getMessage().toLowerCase(Locale.ROOT));
		}
	}
}
