package net.rossonet.waldot.api.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Identita' utente X.509: ammessa se nella trust list o firmata da una CA
 * della lista; rifiutata se estranea o senza lista.
 */
public class WaldotX509IdentityValidatorTest {

	@TempDir
	Path dir;

	private static X509Certificate certificate(String name) throws Exception {
		try (InputStream in = WaldotX509IdentityValidatorTest.class.getResourceAsStream("/x509-identity/" + name)) {
			return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
		}
	}

	private void copy(String name) throws Exception {
		try (InputStream in = WaldotX509IdentityValidatorTest.class.getResourceAsStream("/x509-identity/" + name)) {
			Files.copy(in, dir.resolve(name));
		}
	}

	@Test
	public void identitiesOfTheTrustList() throws Exception {
		final X509Certificate user = certificate("sender.pem");
		final X509Certificate foreign = certificate("foreign.pem");
		assertFalse(WaldotX509IdentityValidator.accepted(user, List.of()), "no trust list");
		assertFalse(WaldotX509IdentityValidator.accepted(user, WaldotX509IdentityValidator.trusted(dir)), "empty list");
		copy("ca.pem");
		final List<X509Certificate> byCa = WaldotX509IdentityValidator.trusted(dir);
		assertTrue(WaldotX509IdentityValidator.accepted(user, byCa), "signed by a CA of the list");
		assertFalse(WaldotX509IdentityValidator.accepted(foreign, byCa), "certificate of another CA");
		Files.delete(dir.resolve("ca.pem"));
		copy("foreign.pem");
		assertTrue(WaldotX509IdentityValidator.accepted(foreign, WaldotX509IdentityValidator.trusted(dir)),
				"certificate in the list");
	}
}
