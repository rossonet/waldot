package net.rossonet.waldot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.utils.SslHelper.KeyStoreHelper;

class SslHelperTest {

	private SslHelper.KeyStoreHelper keyStoreHelper;
	private Path keyStorePath;

	@AfterEach
	void tearDown() throws Exception {
		if (keyStorePath != null) {
			Files.deleteIfExists(keyStorePath);
			if (keyStorePath.getParent() != null) {
				Files.deleteIfExists(keyStorePath.getParent());
			}
		}
	}

	@BeforeEach
	void setUp() throws Exception {
		keyStorePath = Files.createTempDirectory("waldot-api-ssl-test").resolve("test-keystore.p12");
		keyStoreHelper = new KeyStoreHelper(keyStorePath.toString(), "password");
	}

	@Test
	void testCheckSignatureWithPayload() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "IT", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final KeyPair keyPair = keyStoreHelper.getKeyPair("test-cert");
		final boolean isValid = SslHelper.checkSignatureWithPayload(keyPair.getPublic(), keyPair.getPrivate());
		assertTrue(isValid);
	}

	@Test
	void testCreateSelfSignedCertificate() {
		final boolean result = keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name",
				"Test Organization", "Test Unit", "Test Locality", "Test State", "IT", "urn:test:uri",
				"test.dns", "127.0.0.1", "test-alias.dns");
		assertTrue(result);
	}

	@Test
	void testGenerateCertificateSigningRequest() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "IT", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final PKCS10CertificationRequest csr = keyStoreHelper.generateCertificateSigningRequest("test-cert");
		assertNotNull(csr);
	}

	@Test
	void testGetCertificate() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "IT", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final X509Certificate certificate = keyStoreHelper.getCertificate("test-cert");
		assertNotNull(certificate);
	}

	@Test
	void testGetKeyPair() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "IT", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final KeyPair keyPair = keyStoreHelper.getKeyPair("test-cert");
		assertNotNull(keyPair);
		assertNotNull(keyPair.getPrivate());
		assertNotNull(keyPair.getPublic());
	}

	@Test
	void testGetSanDnsNames() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "IT", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final X509Certificate certificate = keyStoreHelper.getCertificate("test-cert");
		final List<String> dnsNames = SslHelper.getSanDnsNames(certificate);
		assertTrue(dnsNames.contains("test.dns"));
		assertTrue(dnsNames.contains("test-alias.dns"));
	}

	@Test
	void testGetSanIpAddresses() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "IT", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final X509Certificate certificate = keyStoreHelper.getCertificate("test-cert");
		final List<String> ipAddresses = SslHelper.getSanIpAddresses(certificate);
		assertTrue(ipAddresses.contains("127.0.0.1"));
	}

	@Test
	void testGetSanUri() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "IT", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final X509Certificate certificate = keyStoreHelper.getCertificate("test-cert");
		final Optional<String> uri = SslHelper.getSanUri(certificate);
		assertTrue(uri.isPresent());
		assertEquals("urn:test:uri", uri.get());
	}
}