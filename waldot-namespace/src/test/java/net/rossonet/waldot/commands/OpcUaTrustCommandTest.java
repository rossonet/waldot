package net.rossonet.waldot.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.DefaultServerCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.FileBasedCertificateQuarantine;
import org.eclipse.milo.opcua.stack.core.security.FileBasedTrustListManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** opcua trust: trust list dei client e degli utenti X.509 nella cartella di sicurezza. */
class OpcUaTrustCommandTest {

	@TempDir
	Path dir;

	private static String pem(final String name) throws Exception {
		try (InputStream in = OpcUaTrustCommandTest.class.getResourceAsStream("/opcua-trust/" + name)) {
			return new String(in.readAllBytes(), StandardCharsets.US_ASCII);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void addListRemove() throws Exception {
		final Map<String, Object> client = OpcUaTrustCommand.add(dir, "client", pem("sender.pem"));
		assertTrue(Files.exists(dir.resolve("store/trusted/certs").resolve((String) client.get("file"))));
		// Base64 di DER, come gli attributi dell'XML
		final String der = Base64.getEncoder().encodeToString(
				OpcUaTrustCommand.parse(pem("foreign.pem")).getEncoded());
		final Map<String, Object> user = OpcUaTrustCommand.add(dir, "user", der);
		assertTrue(Files.exists(dir.resolve("users/trusted").resolve((String) user.get("file"))));

		final List<Map<String, Object>> all = (List<Map<String, Object>>) OpcUaTrustCommand.list(dir)
				.get("certificates");
		assertEquals(2, all.size());
		assertEquals("client", all.get(0).get("kind"));
		assertEquals(client.get("sha256"), all.get(0).get("sha256"));

		// impronta SHA-256 con i due punti, solo tra gli utenti: il client resta
		final String sha256 = ((String) user.get("sha256")).replaceAll("(..)(?!$)", "$1:");
		final List<String> removed = (List<String>) OpcUaTrustCommand.remove(dir, "", sha256).get("removed");
		assertEquals(List.of("user/" + user.get("file")), removed);
		assertEquals(1, ((List<?>) OpcUaTrustCommand.list(dir).get("certificates")).size());
		assertEquals(List.of(), OpcUaTrustCommand.remove(dir, "user", (String) client.get("sha1")).get("removed"));
		assertThrows(IllegalArgumentException.class, () -> OpcUaTrustCommand.add(dir, "admin", der));
	}

	/** Validazione del SecureChannel con la trust list della cartella, come il server. */
	private void validate(final String clientPem) throws Exception {
		final Path store = dir.resolve("store");
		Files.createDirectories(store);
		try (FileBasedTrustListManager trust = FileBasedTrustListManager.createAndInitialize(store)) {
			new DefaultServerCertificateValidator(trust,
					FileBasedCertificateQuarantine.create(store.resolve("rejected").resolve("certs")))
					.validateCertificateChain(List.of(OpcUaTrustCommand.parse(clientPem)), null, null);
		}
	}

	/**
	 * Certificato client firmato da una CA (mu-field del laboratorio): senza la
	 * CA tra gli emittenti il server lo rifiuta anche se fidato
	 * (Bad_SecurityChecksFailed); con la CA (issuer) e il client fidato passa;
	 * tolto il client dai fidati e' di nuovo rifiutato.
	 */
	@Test
	void caSignedClientNeedsTheIssuer() throws Exception {
		final String client = pem("client-of-ca.pem");
		final Map<String, Object> trusted = OpcUaTrustCommand.add(dir, "client", client);
		assertThrows(UaException.class, () -> validate(client), "without the CA");
		final Map<String, Object> issuer = OpcUaTrustCommand.add(dir, "issuer", pem("ca.pem"));
		assertTrue(Files.exists(dir.resolve("store/issuer/certs").resolve((String) issuer.get("file"))));
		assertEquals("issuer", issuer.get("kind"));
		validate(client);
		assertEquals(2, ((List<?>) OpcUaTrustCommand.list(dir).get("certificates")).size(),
				"client and issuer listed");
		// rimozione senza tipo: il client dai fidati, la CA resta
		assertEquals(List.of("client/" + trusted.get("file")),
				OpcUaTrustCommand.remove(dir, "", (String) trusted.get("sha1")).get("removed"));
		assertThrows(UaException.class, () -> validate(client), "client no longer trusted");
	}
}
