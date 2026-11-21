package net.rossonet.waldot.opc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.eclipse.milo.opcua.stack.core.util.validation.CertificateValidationUtil;
import org.junit.jupiter.api.Test;

/** Rifiuto dei certificati client con il motivo (ServiceFault della CreateSession). */
class LoggingCertificateValidatorTest {

	private static X509Certificate certificate(String uri) throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		final KeyPair keyPair = generator.generateKeyPair();
		return new SelfSignedCertificateBuilder(keyPair).setCommonName("mu-field").setApplicationUri(uri).build();
	}

	@Test
	void applicationUriMismatchIsDescribedAndRethrown() throws Exception {
		final X509Certificate certificate = certificate("urn:test:mu-field");
		final LoggingCertificateValidator validator = new LoggingCertificateValidator(
				(chain, uri, hosts) -> CertificateValidationUtil.checkApplicationUri(chain.get(0), uri));
		final UaException e = assertThrows(UaException.class,
				() -> validator.validateCertificateChain(List.of(certificate), "urn:other", null));
		assertEquals(StatusCodes.Bad_CertificateUriInvalid, e.getStatusCode().value());
		final String text = LoggingCertificateValidator.describe(List.of(certificate), "urn:other", e);
		assertTrue(text.contains("CN=mu-field"), text);
		assertTrue(text.contains("Bad_CertificateUriInvalid"), text);
		assertTrue(text.contains("application URI urn:other"), text);
		assertTrue(text.contains("[urn:test:mu-field]"), text);
		// certificato valido: nessuna eccezione
		validator.validateCertificateChain(List.of(certificate), "urn:test:mu-field", null);
	}

	@Test
	void sameExceptionIsPropagated() throws Exception {
		final UaException refused = new UaException(StatusCodes.Bad_SecurityChecksFailed);
		final LoggingCertificateValidator validator = new LoggingCertificateValidator((chain, uri, hosts) -> {
			throw refused;
		});
		final X509Certificate certificate = certificate("urn:test:x");
		assertSame(refused, assertThrows(UaException.class,
				() -> validator.validateCertificateChain(List.of(certificate), null, null)));
		assertTrue(LoggingCertificateValidator.describe(List.of(certificate), null, refused)
				.contains("Bad_SecurityChecksFailed"));
	}
}
