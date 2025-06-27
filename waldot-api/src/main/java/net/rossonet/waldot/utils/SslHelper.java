package net.rossonet.waldot.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SslHelper provides utility methods for handling SSL/TLS operations, including
 * creating SSL contexts, generating certificate signing requests, and managing
 * keystores.
 * 
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class SslHelper {

	public static final class KeyStoreHelper {

		private final Logger logger = LoggerFactory.getLogger(getClass());
		private final String keyStoreFilePath;
		private final String keyStorePassword;

		public KeyStoreHelper(final String keyStoreFilePath, final String keyStorePassword) {
			this.keyStoreFilePath = keyStoreFilePath;
			this.keyStorePassword = keyStorePassword;
		}

		public synchronized boolean createSelfSignedCertificate(final String certificateLabel, final String commonName,
				final String organization, final String unit, final String locality, final String state,
				final String country, final String uri, final String dns, final String ip, final String dnsAlias) {
			boolean result = false;
			try {
				final KeyStore keyStore = loadOrCreateKeyStore();
				final KeyPair keyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);
				final SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(keyPair)
						.setCommonName(commonName).setOrganization(organization).setOrganizationalUnit(unit)
						.setLocalityName(locality).setStateName(state).setCountryCode(country).setApplicationUri(uri)
						.addDnsName(dns).addDnsName(dnsAlias).addIpAddress(ip);
				final X509Certificate certificate = builder.build();
				keyStore.setKeyEntry(certificateLabel, keyPair.getPrivate(), keyStorePassword.toCharArray(),
						new X509Certificate[] { certificate });
				keyStore.store(new FileOutputStream(keyStoreFilePath), keyStorePassword.toCharArray());
				result = true;
			} catch (final Exception df) {
				logger.error("error in key generation", df);
				result = false;
			}
			return result;
		}

		public PKCS10CertificationRequest generateCertificateSigningRequest(final String certificateLabel)
				throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
				IOException, OperatorCreationException {
			final KeyPair keyPair = getKeyPair(certificateLabel);
			final X509Certificate certificate = getCertificate(certificateLabel);
			final SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo
					.getInstance(ASN1Sequence.getInstance(keyPair.getPublic().getEncoded()));
			final PKCS10CertificationRequestBuilder p10Builder = new PKCS10CertificationRequestBuilder(
					new JcaX509CertificateHolder(certificate).getSubject(), subjectPublicKeyInfo);
			final JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(DEFAULT_SIGNATURE_ALGORITHM);
			final ContentSigner signer = csBuilder.build(keyPair.getPrivate());
			final List<GeneralName> generalNames = new ArrayList<>();
			if (getSanUri(certificate).isPresent()) {
				generalNames.add(new GeneralName(SUBJECT_ALT_NAME_URI, getSanUri(certificate).get()));
			}
			generalNames.addAll(getSubjectAltNames(certificate));
			final GeneralNames subjectAltNames = new GeneralNames(generalNames.toArray(new GeneralName[0]));
			final ExtensionsGenerator extGen = new ExtensionsGenerator();
			extGen.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
			p10Builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
			final PKCS10CertificationRequest certificateRequest = p10Builder.build(signer);
			return certificateRequest;
		}

		public X509Certificate getCertificate(final String certificateLabel)
				throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
			final KeyStore keyStore = loadOrCreateKeyStore();
			final X509Certificate clientCertificate = (X509Certificate) keyStore.getCertificate(certificateLabel);
			return clientCertificate;
		}

		public String getCertificateBase64(final String certificateLabel)
				throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
			return Base64.getEncoder().encodeToString(getCertificate(certificateLabel).getEncoded());
		}

		public KeyPair getKeyPair(final String certificateLabel) throws UnrecoverableKeyException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, IOException {
			final PrivateKey privateKey = getPrivateKey(certificateLabel);
			final PublicKey publicKey = getPublicKey(certificateLabel);
			return new KeyPair(publicKey, privateKey);
		}

		public PrivateKey getPrivateKey(final String certificateLabel) throws UnrecoverableKeyException,
				KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
			final KeyStore keyStore = loadOrCreateKeyStore();
			final Key privateKey = keyStore.getKey(certificateLabel, keyStorePassword.toCharArray());
			if (!(privateKey instanceof PrivateKey)) {
				logger.error("The key for label {} is not a PrivateKey", certificateLabel);
				throw new IllegalArgumentException("The key for label " + certificateLabel + " is not a PrivateKey");
			} else {
				return (PrivateKey) privateKey;
			}
		}

		public String getPrivateKeyBase64(final String certificateLabel) throws UnrecoverableKeyException,
				KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
			return Base64.getEncoder().encodeToString(getPrivateKey(certificateLabel).getEncoded());
		}

		public PublicKey getPublicKey(final String certificateLabel)
				throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
			final X509Certificate certificate = getCertificate(certificateLabel);
			return certificate.getPublicKey();
		}

		public boolean hasCertificate(final String certificateLabel)
				throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
			final KeyStore keyStore = loadOrCreateKeyStore();
			return keyStore.containsAlias(certificateLabel);
		}

		private KeyStore loadOrCreateKeyStore()
				throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
			final KeyStore keyStore = KeyStore.getInstance("PKCS12");
			if (!Files.exists(Path.of(keyStoreFilePath))) {
				logger.info("Creating new keystore at {}", keyStoreFilePath);
				keyStore.load(null, keyStorePassword.toCharArray());
			} else {
				logger.info("Loading existing keystore from {}", keyStoreFilePath);
				try (FileInputStream fis = new FileInputStream(keyStoreFilePath)) {
					keyStore.load(fis, keyStorePassword.toCharArray());
				} catch (final IOException e) {
					logger.error("Failed to load keystore", e);
					throw new KeyStoreException("Could not load keystore", e);
				}
			}
			return keyStore;
		}

		public synchronized boolean setCertificate(final String certificateLabel, final byte[] certificate,
				final byte[] privateKey) {
			boolean result = false;
			try {
				logger.info("importing keypair from string data");
				final KeyStore keyStore = loadOrCreateKeyStore();
				final KeyFactory kf = KeyFactory.getInstance("RSA");
				final X509Certificate clientCertificate = (X509Certificate) CertificateFactory.getInstance("X.509")
						.generateCertificate(new ByteArrayInputStream(certificate));
				final PublicKey pubKey = clientCertificate.getPublicKey();
				final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);
				final PrivateKey privKey = kf.generatePrivate(keySpec);
				final KeyPair clientKeyPair = new KeyPair(pubKey, privKey);
				final X509Certificate[] clientCertificateChain = new X509Certificate[] { clientCertificate };
				keyStore.setKeyEntry(certificateLabel, clientKeyPair.getPrivate(), keyStorePassword.toCharArray(),
						clientCertificateChain);
				keyStore.store(new FileOutputStream(keyStoreFilePath), keyStorePassword.toCharArray());
				result = true;
				logger.info("keypair imported successfully");
			} catch (final Exception df) {
				result = false;
				logger.error("error in select keypair", df);
			}
			return result;
		}

		public boolean setCertificate(final String certificateLabel, final PemObject certificate,
				final PemObject privateKey) {
			return setCertificate(certificateLabel, certificate.getContent(), privateKey.getContent());

		}

		public boolean setCertificate(final String certificateLabel, final String base64Certificate,
				final String base64PrivateKey) {
			return setCertificate(certificateLabel, Base64.getDecoder().decode(base64Certificate),
					Base64.getDecoder().decode(base64PrivateKey));

		}
	}

	public static String DEFAULT_CONTEXT_TLS_PROTOCOL = "TLSv1.2";
	public static String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";
	public static final int SUBJECT_ALT_NAME_DNS_NAME = GeneralName.dNSName;
	public static final int SUBJECT_ALT_NAME_IP_ADDRESS = GeneralName.iPAddress;
	public static final int SUBJECT_ALT_NAME_URI = GeneralName.uniformResourceIdentifier;

	public static boolean checkSignatureWithPayload(final PublicKey pubKey, final PrivateKey privKey)
			throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
		return checkSignatureWithPayload(pubKey, privKey, DEFAULT_SIGNATURE_ALGORITHM);

	}

	public static boolean checkSignatureWithPayload(final PublicKey pubKey, final PrivateKey privKey,
			final String signatureAlgorithm) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
		final Signature sig = Signature.getInstance(signatureAlgorithm);
		sig.initSign(privKey);
		final byte[] bytesCheck = "1234567890".getBytes();
		sig.update(bytesCheck);
		final byte[] signature = sig.sign();
		sig.initVerify(pubKey);
		sig.update(bytesCheck);
		return sig.verify(signature);
	}

	public static String getDefaultCharSet() {
		final OutputStreamWriter writer = new OutputStreamWriter(new ByteArrayOutputStream());
		final String enc = writer.getEncoding();
		return enc;
	}

	/**
	 * Get the DNS names from the {@code certificate}'s Subject Alternative Name
	 * extension, if it's present.
	 *
	 * @param certificate the certificate to get the DNS names from.
	 * @return the values of the SAN DNS names, or empty list if none are present.
	 */
	public static List<String> getSanDnsNames(final X509Certificate certificate) {
		final List<Object> values = getSubjectAltNameField(certificate, SUBJECT_ALT_NAME_DNS_NAME);
		return values.stream().filter(v -> v instanceof String).map(String.class::cast).collect(Collectors.toList());
	}

	/**
	 * Get the IP addresses from the {@code certificate}'s Subject Alternative Name
	 * extension, if it's present.
	 *
	 * @param certificate the certificate to get the IP addresses from.
	 * @return the values of the SAN IP addresses, or empty list if none are
	 *         present.
	 */
	public static List<String> getSanIpAddresses(final X509Certificate certificate) {
		final List<Object> values = getSubjectAltNameField(certificate, SUBJECT_ALT_NAME_IP_ADDRESS);

		return values.stream().filter(v -> v instanceof String).map(String.class::cast).collect(Collectors.toList());
	}

	/**
	 * Get the URI from the {@code certificate}'s Subject Alternative Name
	 * extension, if it's present.
	 *
	 * @param certificate the certificate to get the URI from.
	 * @return the value of the SAN URI, if present.
	 */
	public static Optional<String> getSanUri(final X509Certificate certificate) {
		final List<Object> values = getSubjectAltNameField(certificate, SUBJECT_ALT_NAME_URI);

		return values.stream().filter(v -> v instanceof String).map(String.class::cast).findFirst();
	}

	/**
	 * Extract the value of a given SubjectAltName field from a
	 * {@link X509Certificate}.
	 *
	 * @param certificate the certificate.
	 * @param field       the field number.
	 * @return an {@link Optional} containing the value in the field.
	 * @see #SUBJECT_ALT_NAME_IP_ADDRESS
	 * @see #SUBJECT_ALT_NAME_DNS_NAME
	 * @see #SUBJECT_ALT_NAME_URI
	 */
	public static List<Object> getSubjectAltNameField(final X509Certificate certificate, final int field) {
		try {
			final List<Object> values = new ArrayList<>();

			Collection<List<?>> subjectAltNames = certificate.getSubjectAlternativeNames();
			if (subjectAltNames == null) {
				subjectAltNames = Collections.emptyList();
			}

			for (final List<?> idAndValue : subjectAltNames) {
				if (idAndValue != null && idAndValue.size() == 2) {
					if (idAndValue.get(0).equals(field)) {
						final Object value = idAndValue.get(1);
						if (value != null) {
							values.add(value);
						}
					}
				}
			}

			return values;
		} catch (final CertificateParsingException e) {
			return Collections.emptyList();
		}
	}

	public static List<GeneralName> getSubjectAltNames(final X509Certificate certificate) {
		try {
			final List<GeneralName> generalNames = new ArrayList<>();

			Collection<List<?>> subjectAltNames = certificate.getSubjectAlternativeNames();
			if (subjectAltNames == null) {
				subjectAltNames = Collections.emptyList();
			}

			for (final List<?> idAndValue : subjectAltNames) {
				if (idAndValue != null && idAndValue.size() == 2) {
					final Object id = idAndValue.get(0);
					final String value = Objects.toString(idAndValue.get(1));
					if (Objects.equals(id, SUBJECT_ALT_NAME_DNS_NAME)) {
						generalNames.add(new GeneralName(SUBJECT_ALT_NAME_DNS_NAME, value));
					} else if (Objects.equals(id, SUBJECT_ALT_NAME_IP_ADDRESS)) {
						generalNames.add(new GeneralName(SUBJECT_ALT_NAME_IP_ADDRESS, value));
					} else if (Objects.equals(id, SUBJECT_ALT_NAME_URI)) {
						generalNames.add(new GeneralName(SUBJECT_ALT_NAME_URI, value));
					}
				}
			}
			return generalNames;
		} catch (final CertificateParsingException e) {
			return Collections.emptyList();
		}
	}

	public static X509Certificate signCertificate(final PKCS10CertificationRequest certificationRequest,
			final X509Certificate caCertificate, final PrivateKey caPrivateKey, final int validity)
			throws IOException, OperatorCreationException, CertificateException {
		return signCertificate(certificationRequest, caCertificate, caPrivateKey, validity,
				DEFAULT_SIGNATURE_ALGORITHM);
	}

	public static X509Certificate signCertificate(final PKCS10CertificationRequest certificationRequest,
			final X509Certificate caCertificate, final PrivateKey caPrivateKey, final int validity,
			final String signatureAlgorithm) throws IOException, OperatorCreationException, CertificateException {
		final StringBuilder attributes = new StringBuilder();
		for (final Attribute attribute : certificationRequest.getAttributes()) {
			attributes.append(attribute.getAttrValues().toString());
		}
		final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
		final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

		final X509v3CertificateBuilder certificateGenerator = new X509v3CertificateBuilder(
				new X509CertificateHolder(caCertificate.getEncoded()).getSubject(),
				new BigInteger(64, new SecureRandom()),
				Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC).minus(1, ChronoUnit.DAYS)),
				Date.from(LocalDateTime.now().plusDays(validity).toInstant(ZoneOffset.UTC)),
				certificationRequest.getSubject(),
				SubjectPublicKeyInfo.getInstance(certificationRequest.getSubjectPublicKeyInfo()));

		final ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
				.build(PrivateKeyFactory.createKey(caPrivateKey.getEncoded()));
		final X509CertificateHolder holder = certificateGenerator.build(sigGen);
		final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		final X509Certificate certificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(holder.toASN1Structure().getEncoded()));

		return certificate;
	}

	private SslHelper() {
		throw new UnsupportedOperationException("Just for static usage");
	}
}
