package net.rossonet.waldot.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.rossonet.waldot.api.auth.WaldotX509IdentityValidator;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.WaldotOpcUaServer;

/**
 * {@code opcua trust}: trust lists and certificate of the OPC UA server of
 * WaldOT, in its security directory:
 * <ul>
 * <li>{@code client}: certificates of the clients accepted on the
 * SecureChannel ({@code store/trusted/certs}, reloaded by Milo when the
 * directory changes);</li>
 * <li>{@code user}: certificates of the X.509 user identities
 * ({@code users/trusted}, read at every activation of a session);</li>
 * <li>{@code issuer}: CA certificates that issued the client certificates
 * ({@code store/issuer/certs}, layout of Milo FileBasedTrustListManager).
 * Milo builds the chain of a client certificate up to a self-signed CA: a client certificate signed by a CA
 * is accepted only with the CA among the issuers and the client
 * certificate (or the CA itself) among the trusted ones. Without the CA a
 * CA-signed client certificate is always refused with
 * Bad_SecurityChecksFailed, even when trusted (lab of 20/11/2026).</li>
 * </ul>
 * Actions: {@code list}, {@code add} (certificate in PEM or Base64 of DER),
 * {@code remove} (SHA-1 or SHA-256 thumbprint, hex with or without colons),
 * {@code server-certificate} (PEM and thumbprints of the server). The output
 * is JSON.
 */
public final class OpcUaTrustCommand extends AbstractExchangeCommand {

	/** Name of the command. */
	public static final String NAME = "opcua trust";

	private static final ObjectMapper JSON = new ObjectMapper();

	/** Directory of a kind of certificates in the security directory. */
	static Path directory(final Path securityDir, final String kind) {
		return switch (kind) {
			case "client" -> securityDir.resolve("store").resolve("trusted").resolve("certs");
			case "user" -> securityDir.resolve(WaldotX509IdentityValidator.USERS_TRUSTED_DIRECTORY);
			case "issuer" -> securityDir.resolve("store").resolve("issuer").resolve("certs");
			default -> throw new IllegalArgumentException(
					"kind must be client, user or issuer: " + kind);
		};
	}

	/** Certificate from PEM or Base64 of DER. */
	static X509Certificate parse(final String text) throws CertificateException {
		final String trimmed = text.strip();
		final byte[] data = trimmed.contains("-----BEGIN")
				? trimmed.getBytes(java.nio.charset.StandardCharsets.US_ASCII)
				: Base64.getMimeDecoder().decode(trimmed);
		return (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(data));
	}

	static String thumbprint(final X509Certificate certificate, final String algorithm)
			throws Exception {
		return HexFormat.of().formatHex(
				MessageDigest.getInstance(algorithm).digest(certificate.getEncoded()));
	}

	static Map<String, Object> describe(final X509Certificate certificate)
			throws Exception {
		final Map<String, Object> m = new LinkedHashMap<>();
		m.put("subject", certificate.getSubjectX500Principal().getName());
		m.put("issuer", certificate.getIssuerX500Principal().getName());
		m.put("notBefore", certificate.getNotBefore().toInstant().toString());
		m.put("notAfter", certificate.getNotAfter().toInstant().toString());
		m.put("sha1", thumbprint(certificate, "SHA-1"));
		m.put("sha256", thumbprint(certificate, "SHA-256"));
		return m;
	}

	/** Kinds of certificates, in listing order. */
	static final List<String> KINDS = List.of("client", "user", "issuer");

	/** Trust lists of the directory (every kind). */
	static Map<String, Object> list(final Path securityDir) throws Exception {
		final List<Map<String, Object>> list = new ArrayList<>();
		for (final String kind : KINDS) {
			final Path dir = directory(securityDir, kind);
			if (!Files.isDirectory(dir)) {
				continue;
			}
			try (Stream<Path> files = Files.list(dir)) {
				for (final Path file : files.filter(Files::isRegularFile).sorted().toList()) {
					final Map<String, Object> m = new LinkedHashMap<>();
					m.put("kind", kind);
					m.put("file", file.getFileName().toString());
					try (var in = Files.newInputStream(file)) {
						for (final var c : CertificateFactory.getInstance("X.509")
								.generateCertificates(in)) {
							m.putAll(describe((X509Certificate) c));
							break;
						}
					} catch (final CertificateException e) {
						m.put("error", "not a certificate: " + e.getMessage());
					}
					list.add(m);
				}
			}
		}
		final Map<String, Object> out = new LinkedHashMap<>();
		out.put("securityDir", securityDir.toString());
		out.put("certificates", list);
		return out;
	}

	/** Adds a certificate as {@code <sha1>.der}. */
	static Map<String, Object> add(final Path securityDir, final String kind,
			final String value) throws Exception {
		final X509Certificate certificate = parse(value);
		final Path dir = directory(securityDir, kind);
		Files.createDirectories(dir);
		final Path file = dir.resolve(thumbprint(certificate, "SHA-1") + ".der");
		Files.write(file, certificate.getEncoded());
		final Map<String, Object> out = describe(certificate);
		out.put("kind", kind);
		out.put("file", file.getFileName().toString());
		out.put("added", true);
		return out;
	}

	/** Removes the certificates with the thumbprint (one kind or every kind). */
	static Map<String, Object> remove(final Path securityDir, final String kind,
			final String value) throws Exception {
		final String wanted = value.replace(":", "").strip().toLowerCase(Locale.ROOT);
		if (wanted.isEmpty()) {
			throw new IllegalArgumentException("thumbprint missing");
		}
		final List<String> removed = new ArrayList<>();
		for (final String k : kind.isBlank() ? KINDS : List.of(kind)) {
			final Path dir = directory(securityDir, k);
			if (!Files.isDirectory(dir)) {
				continue;
			}
			try (Stream<Path> files = Files.list(dir)) {
				for (final Path file : files.filter(Files::isRegularFile).toList()) {
					try (var in = Files.newInputStream(file)) {
						final X509Certificate c = (X509Certificate) CertificateFactory
								.getInstance("X.509").generateCertificate(in);
						if (wanted.equals(thumbprint(c, "SHA-1"))
								|| wanted.equals(thumbprint(c, "SHA-256"))) {
							Files.delete(file);
							removed.add(k + "/" + file.getFileName());
						}
					} catch (final CertificateException | IOException e) {
						// not a certificate: left alone
					}
				}
			}
		}
		final Map<String, Object> out = new LinkedHashMap<>();
		out.put("removed", removed);
		return out;
	}

	public OpcUaTrustCommand(final WaldotNamespace namespace) {
		super(namespace, NAME,
				"trust lists of the OPC UA server: action list | add | remove | server-certificate, kind client"
						+ " (SecureChannel), user (X.509 identity) or issuer (CA of the clients), value = certificate (PEM or Base64 of DER)"
						+ " or thumbprint (SHA-1/SHA-256); JSON output",
				MiloStrategy.MANAGE_CMD_DIRECTORY, new String[]{"action", "kind", "value"},
				new String[]{"json", "error"});
	}

	@Override
	public Object clone() {
		return new OpcUaTrustCommand(waldotNamespace);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext,
			final String[] inputValues) {
		final String[] output = newOutput();
		try {
			final WaldotOpcUaServer server = waldotNamespace.getOpcuaServer();
			final Path securityDir = server == null ? null : server.getSecurityDir();
			if (securityDir == null) {
				throw new IllegalStateException("OPC UA server without security directory");
			}
			final String action = CommandArguments.requireFirst(inputValues, "action").strip();
			final String kind = stringArgument(inputValues, 1).strip();
			final String value = stringArgument(inputValues, 2);
			final Object result = switch (action) {
				case "list" -> list(securityDir);
				case "add" -> add(securityDir, kind.isEmpty() ? "client" : kind, value);
				case "remove" -> remove(securityDir, kind, value);
				case "server-certificate" -> {
					final X509Certificate certificate = server.getServerCertificate();
					if (certificate == null) {
						throw new IllegalStateException("server certificate not loaded");
					}
					final Map<String, Object> m = describe(certificate);
					m.put("pem", "-----BEGIN CERTIFICATE-----\n"
							+ Base64.getMimeEncoder(64, "\n".getBytes())
									.encodeToString(certificate.getEncoded())
							+ "\n-----END CERTIFICATE-----\n");
					yield m;
				}
				default -> throw new IllegalArgumentException(
						"action must be list, add, remove or server-certificate: " + action);
			};
			output[0] = JSON.writeValueAsString(result);
		} catch (final Throwable e) {
			output[1] = CommandArguments.errorMessage(e);
		}
		return output;
	}
}
