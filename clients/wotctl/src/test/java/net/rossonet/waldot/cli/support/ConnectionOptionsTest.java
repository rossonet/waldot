package net.rossonet.waldot.cli.support;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.cli.CliTestSupport.Result;
import net.rossonet.waldot.cli.CommandTestBase;
import net.rossonet.waldot.client.api.AuthenticationMode;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;

/**
 * Connection options mapped on the configuration of the client (the command
 * runs on the mock client, the configuration is the one given to the
 * factory).
 */
class ConnectionOptionsTest extends CommandTestBase {

	private static String pem(final String type, final byte[] data) {
		return "-----BEGIN " + type + "-----\n"
				+ Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(data)
				+ "\n-----END " + type + "-----\n";
	}

	private WaldOTAgentClientConfiguration configuration(
			final String... options) {
		final String[] args = new String[options.length + 1];
		args[0] = "about";
		System.arraycopy(options, 0, args, 1, options.length);
		final Result result = run(args);
		assertEquals(0, result.exitCode(), result.err());
		return mock.configuration;
	}

	@Test
	void defaults() {
		final WaldOTAgentClientConfiguration configuration = configuration();
		if (System.getenv("WALDOT_ENDPOINT") == null) {
			assertEquals(ConnectionOptions.DEFAULT_ENDPOINT,
					configuration.getBaseWaldOTOpcEndpoint());
		}
		if (System.getenv("WALDOT_CONNECT_TIMEOUT") == null) {
			assertEquals(ConnectionOptions.DEFAULT_CONNECT_TIMEOUT.toMillis(),
					configuration.getConnectTimeout());
		}
	}

	@Test
	void identityCertificate() throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		final KeyPair pair = generator.generateKeyPair();
		final X509Certificate certificate = new SelfSignedCertificateBuilder(
				pair).setCommonName("operatore")
				.setApplicationUri("urn:test:operatore").build();
		final Path cert = tmp.resolve("cert.pem");
		final Path key = tmp.resolve("key.pem");
		Files.writeString(cert, pem("CERTIFICATE", certificate.getEncoded()));
		Files.writeString(key,
				pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
		final WaldOTAgentClientConfiguration configuration = configuration(
				"--identity-cert", cert.toString(), "--identity-key",
				key.toString());
		assertEquals(AuthenticationMode.CERTIFICATE,
				configuration.getAuthenticationMode());
		assertArrayEquals(pair.getPrivate().getEncoded(),
				configuration.getCryptoPrivateKey().getEncoded());

		final Path broken = tmp.resolve("broken.pem");
		Files.writeString(broken, pem("PRIVATE KEY", new byte[]{1, 2, 3}));
		final Result invalid = run("about", "--identity-cert", cert.toString(),
				"--identity-key", broken.toString());
		assertEquals(ExitCodes.USAGE, invalid.exitCode());
		assertTrue(invalid.err().contains("identità X509 non valida"),
				invalid.err());
		final Result onlyKey = run("about", "--identity-key", key.toString());
		assertEquals(ExitCodes.USAGE, onlyKey.exitCode());
		final Result both = run("about", "--username", "u", "--identity-cert",
				cert.toString(), "--identity-key", key.toString());
		assertEquals(ExitCodes.USAGE, both.exitCode());
		assertTrue(both.err().contains("alternativi"), both.err());
	}

	@Test
	void invalidValuesAreUsageErrors() {
		for (final String[] args : new String[][]{
				{"--security-mode", "Cifrato"},
				{"--security-policy", "Basic256Sha256", "--security-mode",
						"None"},
				{"--security-policy", "None", "--security-mode", "Sign"},
				{"--connect-timeout", "0s"}, {"--request-timeout", "x"},
				{"--session-timeout", "-1s"}, {"--keepalive-timeout", "1y"},
				{"--reconnect-initial", "mai"},
				{"--reconnect-attempts", "tanti"},
				{"--reconnect-attempts", "-1"},
				{"--reconnect-initial", "10s", "--reconnect-max", "1s"},
				{"--username", "u", "--password-file", "/nessuna/password"},
				{"--endpoint", " "}}) {
			final String[] full = new String[args.length + 1];
			full[0] = "about";
			System.arraycopy(args, 0, full, 1, args.length);
			final Result result = run(full);
			assertEquals(ExitCodes.USAGE, result.exitCode(),
					String.join(" ", args) + ": " + result.err());
			assertTrue(result.err().startsWith("wotctl: uso errato:"),
					result.err());
		}
		if (System.getenv("WALDOT_PASSWORD") == null) {
			final Result noPassword = run("about", "--username", "operatore");
			assertEquals(ExitCodes.USAGE, noPassword.exitCode());
			assertTrue(noPassword.err().contains("WALDOT_PASSWORD"),
					noPassword.err());
		}
		verifyNoInteractions(mock.client);
	}

	@Test
	void reconnectAndTimeouts() {
		final WaldOTAgentClientConfiguration configuration = configuration(
				"--connect-timeout", "3s", "--request-timeout", "1m30s",
				"--session-timeout", "PT2M", "--keepalive-timeout", "2.5",
				"--reconnect-initial", "500ms", "--reconnect-max", "1m",
				"--reconnect-attempts", "4");
		assertEquals(3000, configuration.getConnectTimeout());
		assertEquals(90_000, configuration.getRequestTimeout());
		assertEquals(120_000, configuration.getSessionTimeout());
		assertEquals(2500, configuration.getKeepAliveTimeout());
		assertEquals(500, configuration.getReconnectInitialDelay());
		assertEquals(60_000, configuration.getReconnectMaxDelay());
		assertEquals(4, configuration.getReconnectMaxAttempts());
		org.mockito.Mockito.verify(mock.client)
				.connect(Duration.ofSeconds(3));
		// only the initial delay, larger than the default maximum
		final WaldOTAgentClientConfiguration initial = configuration(
				"--reconnect-initial", "1d");
		assertEquals(Duration.ofDays(1).toMillis(),
				initial.getReconnectInitialDelay());
		assertEquals(Duration.ofDays(1).toMillis(),
				initial.getReconnectMaxDelay());
		// only the attempts
		assertEquals(0,
				configuration("--reconnect-attempts", "0")
						.getReconnectMaxAttempts());
	}

	@Test
	void securityAndEndpoint() {
		final WaldOTAgentClientConfiguration configuration = configuration(
				"--endpoint", "opc.tcp://plc:4840/waldot", "--security-policy",
				"basic256sha256", "--force-endpoint-url",
				"opc.tcp://nat:4840/waldot", "--application-uri", "urn:prova",
				"--agent-name", "agente-1", "--namespace-uri", "urn:ns",
				"--commands-root", "comandi", "--trust-all", "--keystore",
				tmp.resolve("client.ks").toString(), "--keystore-password",
				"segreta");
		assertEquals("opc.tcp://plc:4840/waldot",
				configuration.getBaseWaldOTOpcEndpoint());
		assertEquals(SecurityPolicy.Basic256Sha256,
				configuration.getSecurityPolicy());
		// mode derived from the policy
		assertEquals(MessageSecurityMode.SignAndEncrypt,
				configuration.getMessageSecurityMode());
		assertEquals("opc.tcp://nat:4840/waldot",
				configuration.getForceDiscoveredEndpointUrl());
		assertEquals("urn:prova", configuration.getApplicationUri());
		assertEquals("agente-1", configuration.getAgentUniqueName());
		assertEquals("urn:ns", configuration.getWaldotNamespaceUri());
		assertEquals("comandi", configuration.getCommandsRootNodeId());
		assertTrue(configuration.isForceCertificateValidator());
		assertEquals(tmp.resolve("client.ks").toString(),
				configuration.getKeyStorePath());
		assertEquals("segreta", configuration.getKeyStorePassword());

		// policy derived from the mode
		final WaldOTAgentClientConfiguration sign = configuration(
				"--security-mode", "sign");
		assertEquals(SecurityPolicy.Basic256Sha256, sign.getSecurityPolicy());
		assertEquals(MessageSecurityMode.Sign, sign.getMessageSecurityMode());
		final WaldOTAgentClientConfiguration none = configuration(
				"--security-mode", "None");
		assertEquals(SecurityPolicy.None, none.getSecurityPolicy());
		// only the password of the keystore: the default path
		final WaldOTAgentClientConfiguration keystorePassword = configuration(
				"--keystore-password", "altra");
		assertEquals("altra", keystorePassword.getKeyStorePassword());
		assertEquals(
				WaldOTAgentClientConfiguration.getDefaultConfiguration()
						.getKeyStorePath(),
				keystorePassword.getKeyStorePath());
	}

	@Test
	void usernameWithPasswordFromFileAndStdin() throws Exception {
		final Path file = tmp.resolve("password.txt");
		Files.writeString(file, "segreta\r\nseconda riga\n");
		final WaldOTAgentClientConfiguration configuration = configuration(
				"--username", "operatore", "--password-file", file.toString());
		assertEquals(AuthenticationMode.USERNAME,
				configuration.getAuthenticationMode());
		assertEquals("operatore", configuration.getUsername());
		assertEquals("segreta", configuration.getPassword());
		final Result stdin = runWithStdin("dallo-stdin", "about", "--username",
				"operatore", "--password-file", "-");
		assertEquals(0, stdin.exitCode(), stdin.err());
		assertEquals("dallo-stdin", mock.configuration.getPassword());
	}
}
