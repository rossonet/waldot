package net.rossonet.waldot.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;

/**
 * Need bouncycastle libs. This libs are not in the jar.
 * 
 * implementation group: 'org.bouncycastle', name: 'bcprov-jdk18on', version:
 * '1.76' implementation group: 'org.bouncycastle', name: 'bcpkix-jdk18on',
 * version: '1.76' implementation group: 'org.bouncycastle', name:
 * 'bcutil-jdk18on', version: '1.76'
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 *
 */
public class SslHelper {

	public static String DEFAULT_CONTEXT_TLS_PROTOCOL = "TLSv1.2";

	public static String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";
	public static final int SUBJECT_ALT_NAME_DNS_NAME = GeneralName.dNSName;

	public static final int SUBJECT_ALT_NAME_IP_ADDRESS = GeneralName.iPAddress;

	public static final int SUBJECT_ALT_NAME_URI = GeneralName.uniformResourceIdentifier;

	public static String certificateStringFromOneLine(final String certificateInOneLine) {
		final StringBuilder result = new StringBuilder();
		String header = null;
		if (certificateInOneLine.contains("-----BEGIN CERTIFICATE-----")) {
			header = "-----BEGIN CERTIFICATE-----";
		} else if (certificateInOneLine.contains("-----BEGIN RSA PRIVATE KEY-----")) {
			header = "-----BEGIN RSA PRIVATE KEY-----";
		} else if (certificateInOneLine.contains("-----BEGIN PRIVATE KEY-----")) {
			header = "-----BEGIN PRIVATE KEY-----";
		}
		String footer = null;
		if (certificateInOneLine.contains("-----END CERTIFICATE-----")) {
			footer = "-----END CERTIFICATE-----";
		} else if (certificateInOneLine.contains("-----END RSA PRIVATE KEY-----")) {
			footer = "-----END RSA PRIVATE KEY-----";
		} else if (certificateInOneLine.contains("-----END PRIVATE KEY-----")) {
			footer = "-----END PRIVATE KEY-----";
		}
		if (header != null && footer != null) {
			final String payload = certificateInOneLine.replace(header, "").replace(footer, "");
			for (final String line : TextHelper.splitFixSize(payload, 64)) {
				result.append(line);
				result.append("\n");
			}
			final String converted = header + "\n" + result.toString() + footer;
			return converted;
		}
		return result.toString();
	}

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

	public static PKCS10CertificationRequest createCertificationRequest(final KeyPair keyPair,
			final X509Certificate certificate)
			throws CertificateEncodingException, OperatorCreationException, CertificateParsingException, IOException {
		return createCertificationRequest(keyPair, certificate, DEFAULT_SIGNATURE_ALGORITHM);
	}

	public static PKCS10CertificationRequest createCertificationRequest(final KeyPair keyPair,
			final X509Certificate certificate, final String signatureAlgorithm)
			throws OperatorCreationException, CertificateEncodingException, CertificateParsingException, IOException {
		final SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo
				.getInstance(ASN1Sequence.getInstance(keyPair.getPublic().getEncoded()));
		final PKCS10CertificationRequestBuilder p10Builder = new PKCS10CertificationRequestBuilder(
				new JcaX509CertificateHolder(certificate).getSubject(), subjectPublicKeyInfo);
		final JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(signatureAlgorithm);
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

	public static KeyStore createKeystore(final String certificateAlias, final X509Certificate certificate,
			final String privateKeyAlias, final PrivateKey privateKey, final String keystorePassword)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		clientKeyStore.load(null, keystorePassword.toCharArray());
		clientKeyStore.setCertificateEntry(certificateAlias, certificate);
		clientKeyStore.setKeyEntry(privateKeyAlias, privateKey, keystorePassword.toCharArray(),
				new Certificate[] { certificate });
		return clientKeyStore;
	}

	public static KeyStore createKeystore(final String caAlias, final X509Certificate ca, final String certificateAlias,
			final X509Certificate certificate, final String privateKeyAlias, final PrivateKey privateKey,
			final String keystorePassword)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		clientKeyStore.load(null, keystorePassword.toCharArray());
		clientKeyStore.setCertificateEntry(certificateAlias, certificate);
		clientKeyStore.setKeyEntry(privateKeyAlias, privateKey, keystorePassword.toCharArray(),
				new Certificate[] { certificate });
		return clientKeyStore;
	}

	public static KeyStore createKeyStore(final String caAlias, final Path caCrtFile, final String certificateAlias,
			final Path crtFile, final String privateKeyAlias, final Path keyFile, final String keystorePassword)
			throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
		Security.addProvider(new BouncyCastleProvider());
		final JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter().setProvider("BC");
		PEMParser reader = new PEMParser(new FileReader(caCrtFile.toFile().getAbsolutePath()));
		final X509CertificateHolder caCertHolder = (X509CertificateHolder) reader.readObject();
		reader.close();
		final X509Certificate caCert = certificateConverter.getCertificate(caCertHolder);
		reader = new PEMParser(new FileReader(crtFile.toFile().getAbsolutePath()));
		final X509CertificateHolder certHolder = (X509CertificateHolder) reader.readObject();
		reader.close();
		final X509Certificate cert = certificateConverter.getCertificate(certHolder);
		reader = new PEMParser(new FileReader(keyFile.toFile().getAbsolutePath()));
		final Object readObject = reader.readObject();
		PrivateKeyInfo privateKeyInfo = null;
		if (readObject instanceof PrivateKeyInfo) {
			privateKeyInfo = (PrivateKeyInfo) readObject;
		} else if (readObject instanceof PEMKeyPair) {
			final PEMKeyPair pemKeyPair = (PEMKeyPair) readObject;
			privateKeyInfo = pemKeyPair.getPrivateKeyInfo();
		} else {
			throw new CertificateException("private key not valid");
		}
		reader.close();
		final KeyStore keyStore = createKeyStore(caAlias, caCert, certificateAlias, cert, privateKeyAlias,
				privateKeyInfo, keystorePassword);
		return keyStore;
	}

	public static KeyStore createKeyStore(final String caAlias, final String caCrtString, final String certificateAlias,
			final String certificateString, final String privateKeyAlias, final String privateKeyString,
			final String keystorePassword)
			throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
		final Path caCrtFile = Files.createTempFile("caCrtFile", ".pem");
		final Path crtFile = Files.createTempFile("crtFile", ".pem");
		final Path keyFile = Files.createTempFile("keyFile", ".pem");
		Files.write(caCrtFile, caCrtString.getBytes());
		Files.write(crtFile, certificateString.getBytes());
		Files.write(keyFile, privateKeyString.getBytes());
		final KeyStore keyStore = createKeyStore(caAlias, caCrtFile, certificateAlias, crtFile, privateKeyAlias,
				keyFile, keystorePassword);
		caCrtFile.toFile().delete();
		crtFile.toFile().delete();
		keyFile.toFile().delete();
		return keyStore;
	}

	public static TrustManagerFactory createKeyStore(final String caAlias, final X509Certificate caCert)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		final KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		caKeyStore.load(null, null);
		caKeyStore.setCertificateEntry(caAlias, caCert);
		final TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(caKeyStore);
		return trustManagerFactory;
	}

	public static KeyStore createKeyStore(final String certificateAlias, final X509Certificate certificate,
			final String privateKeyAlias, final PrivateKeyInfo privateKeyInfo, final String keystorePassword)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");
		final PrivateKey key = keyConverter.getPrivateKey(privateKeyInfo);
		final KeyStore clientKeyStore = createKeystore(certificateAlias, certificate, privateKeyAlias, key,
				keystorePassword);
		return clientKeyStore;
	}

	public static KeyStore createKeyStore(final String caAlias, final X509Certificate ca, final String certificateAlias,
			final X509Certificate certificate, final String privateKeyAlias, final PrivateKeyInfo privateKeyInfo,
			final String keystorePassword)
			throws PEMException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		final JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");
		final PrivateKey key = keyConverter.getPrivateKey(privateKeyInfo);
		final KeyStore clientKeyStore = createKeystore(caAlias, ca, certificateAlias, certificate, privateKeyAlias, key,
				keystorePassword);
		return clientKeyStore;
	}

	public static SSLContext createSSLContext(final String caAlias, final Path caCrtFile, final String certificateAlias,
			final Path crtFile, final String privateKeyAlias, final Path keyFile, final String keystorePassword)
			throws KeyManagementException, UnrecoverableKeyException, CertificateException, KeyStoreException,
			NoSuchAlgorithmException, IOException {
		return createSSLContext(keystorePassword, keyFile, keystorePassword, keyFile, keystorePassword, keyFile,
				keystorePassword, DEFAULT_CONTEXT_TLS_PROTOCOL);
	}

	public static SSLContext createSSLContext(final String caAlias, final Path caCrtFile, final String certificateAlias,
			final Path crtFile, final String privateKeyAlias, final Path keyFile, final String keystorePassword,
			final String sslContextProtocol) throws CertificateException, IOException, KeyStoreException,
			NoSuchAlgorithmException, KeyManagementException, UnrecoverableKeyException {
		Security.addProvider(new BouncyCastleProvider());
		final JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter().setProvider("BC");
		PEMParser reader = new PEMParser(new FileReader(caCrtFile.toFile().getAbsolutePath()));
		final X509CertificateHolder caCertHolder = (X509CertificateHolder) reader.readObject();
		reader.close();
		final X509Certificate caCert = certificateConverter.getCertificate(caCertHolder);
		reader = new PEMParser(new FileReader(crtFile.toFile().getAbsolutePath()));
		final X509CertificateHolder certHolder = (X509CertificateHolder) reader.readObject();
		reader.close();
		final X509Certificate cert = certificateConverter.getCertificate(certHolder);
		reader = new PEMParser(new FileReader(keyFile.toFile().getAbsolutePath()));
		final PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo) reader.readObject();
		reader.close();
		final KeyStore clientKeyStore = createKeyStore(certificateAlias, cert, privateKeyAlias, privateKeyInfo,
				keystorePassword);
		final TrustManagerFactory trustManagerFactory = createKeyStore(caAlias, caCert);
		final KeyManagerFactory keyManagerFactory = KeyManagerFactory
				.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(clientKeyStore, keystorePassword.toCharArray());
		final SSLContext context = SSLContext.getInstance(sslContextProtocol);
		context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
		return context;
	}

	public static SSLContext createSSLContext(final String caAlias, final String caCrtString,
			final String certificateAlias, final String certificateString, final String privateKeyAlias,
			final String privateKeyString, final String keystorePassword) throws IOException, KeyManagementException,
			UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
		final Path caCrtFile = Files.createTempFile("caCrtFile", ".pem");
		final Path crtFile = Files.createTempFile("crtFile", ".pem");
		final Path keyFile = Files.createTempFile("keyFile", ".pem");
		Files.write(caCrtFile, caCrtString.getBytes());
		Files.write(crtFile, certificateString.getBytes());
		Files.write(keyFile, privateKeyString.getBytes());
		final SSLContext sslContext = createSSLContext(caAlias, caCrtFile, certificateAlias, crtFile, privateKeyAlias,
				keyFile, keystorePassword);
		caCrtFile.toFile().delete();
		crtFile.toFile().delete();
		keyFile.toFile().delete();
		return sslContext;
	}

	public static <OBJECT_TYPE extends Object> String encodeInPemFormat(final OBJECT_TYPE data) throws IOException {
		final StringWriter writer = new StringWriter();
		final JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
		pemWriter.writeObject(data);
		pemWriter.flush();
		pemWriter.close();
		return writer.toString();
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
