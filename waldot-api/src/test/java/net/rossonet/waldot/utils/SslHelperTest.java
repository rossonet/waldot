package net.rossonet.waldot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.utils.SslHelper.KeyStoreHelper;

class SslHelperTest {

	private SslHelper.KeyStoreHelper keyStoreHelper;

	@BeforeEach
	void setUp() {
		keyStoreHelper = new KeyStoreHelper("test-keystore.p12", "password");
	}

	@Test
	void testCheckSignatureWithPayload() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "Test Country", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final KeyPair keyPair = keyStoreHelper.getKeyPair("test-cert");
		final boolean isValid = SslHelper.checkSignatureWithPayload(keyPair.getPublic(), keyPair.getPrivate());
		assertTrue(isValid);
	}

	@Test
	void testCreateSelfSignedCertificate() {
		final boolean result = keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name",
				"Test Organization", "Test Unit", "Test Locality", "Test State", "Test Country", "urn:test:uri",
				"test.dns", "127.0.0.1", "test-alias.dns");
		assertTrue(result);
	}

	@Test
	void testGenerateCertificateSigningRequest() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "Test Country", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final PKCS10CertificationRequest csr = keyStoreHelper.generateCertificateSigningRequest("test-cert");
		System.out.println("CSR: " + csr);
		assertNotNull(csr);
	}

	@Test
	void testGetCertificate() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "Test Country", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final X509Certificate certificate = keyStoreHelper.getCertificate("test-cert");
		System.out.println("Certificate: " + certificate);
		assertNotNull(certificate);
	}

	@Test
	void testGetKeyPair() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "Test Country", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final KeyPair keyPair = keyStoreHelper.getKeyPair("test-cert");
		assertNotNull(keyPair);
		System.out.println("KeyPair: " + keyPair);
		assertNotNull(keyPair.getPrivate());
		assertNotNull(keyPair.getPublic());
	}

	@Test
	void testGetSanDnsNames() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "Test Country", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final X509Certificate certificate = keyStoreHelper.getCertificate("test-cert");
		final List<String> dnsNames = SslHelper.getSanDnsNames(certificate);
		assertTrue(dnsNames.contains("test.dns"));
		assertTrue(dnsNames.contains("test-alias.dns"));
	}

	@Test
	void testGetSanIpAddresses() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "Test Country", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final X509Certificate certificate = keyStoreHelper.getCertificate("test-cert");
		final List<String> ipAddresses = SslHelper.getSanIpAddresses(certificate);
		assertTrue(ipAddresses.contains("127.0.0.1"));
	}

	@Test
	void testGetSanUri() throws Exception {
		keyStoreHelper.createSelfSignedCertificate("test-cert", "Test Common Name", "Test Organization", "Test Unit",
				"Test Locality", "Test State", "Test Country", "urn:test:uri", "test.dns", "127.0.0.1",
				"test-alias.dns");

		final X509Certificate certificate = keyStoreHelper.getCertificate("test-cert");
		final Optional<String> uri = SslHelper.getSanUri(certificate);
		assertTrue(uri.isPresent());
		assertEquals("urn:test:uri", uri.get());
	}
}