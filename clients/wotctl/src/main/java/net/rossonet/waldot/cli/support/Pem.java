package net.rossonet.waldot.cli.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the X509 identity of the user from PEM (or DER) files: the certificate
 * chain and the unencrypted private key (PKCS#8 {@code PRIVATE KEY} or PKCS#1
 * {@code RSA PRIVATE KEY}).
 */
public final class Pem {

	private static final Pattern BLOCK = Pattern
			.compile("-----BEGIN ([A-Z0-9 ]+)-----([^-]+)-----END \\1-----");

	/** rsaEncryption AlgorithmIdentifier (OID 1.2.840.113549.1.1.1, NULL). */
	private static final byte[] RSA_ALGORITHM = {0x30, 0x0d, 0x06, 0x09, 0x2a,
			(byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
			0x05, 0x00};

	/** Reads one or more certificates (PEM or DER). */
	public static List<X509Certificate> certificates(final Path file)
			throws IOException, GeneralSecurityException {
		final byte[] content = Files.readAllBytes(file);
		final List<X509Certificate> chain = new ArrayList<>();
		for (final Certificate certificate : CertificateFactory
				.getInstance("X.509")
				.generateCertificates(new ByteArrayInputStream(content))) {
			chain.add((X509Certificate) certificate);
		}
		if (chain.isEmpty()) {
			throw new GeneralSecurityException("nessun certificato in " + file);
		}
		return chain;
	}

	private static byte[] der(final int tag, final byte[]... parts) {
		final ByteArrayOutputStream body = new ByteArrayOutputStream();
		for (final byte[] part : parts) {
			body.writeBytes(part);
		}
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(tag);
		final int length = body.size();
		if (length < 0x80) {
			out.write(length);
		} else {
			final byte[] bytes = java.math.BigInteger.valueOf(length)
					.toByteArray();
			final int offset = bytes[0] == 0 ? 1 : 0;
			out.write(0x80 | bytes.length - offset);
			out.write(bytes, offset, bytes.length - offset);
		}
		out.writeBytes(body.toByteArray());
		return out.toByteArray();
	}

	private static PrivateKey pkcs8(final byte[] encoded)
			throws GeneralSecurityException {
		final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoded);
		GeneralSecurityException last = null;
		for (final String algorithm : new String[]{"RSA", "EC", "Ed25519",
				"Ed448"}) {
			try {
				return KeyFactory.getInstance(algorithm).generatePrivate(spec);
			} catch (final GeneralSecurityException e) {
				last = e;
			}
		}
		throw new GeneralSecurityException(
				"chiave privata PKCS#8 non riconosciuta", last);
	}

	/** Reads an unencrypted private key (PEM PKCS#8, PEM PKCS#1 RSA or DER PKCS#8). */
	public static PrivateKey privateKey(final Path file)
			throws IOException, GeneralSecurityException {
		final byte[] content = Files.readAllBytes(file);
		final Matcher matcher = BLOCK
				.matcher(new String(content, StandardCharsets.US_ASCII));
		while (matcher.find()) {
			final String type = matcher.group(1);
			if (!type.endsWith("PRIVATE KEY")) {
				continue;
			}
			final byte[] data = Base64.getMimeDecoder()
					.decode(matcher.group(2).trim());
			switch (type) {
				case "PRIVATE KEY" :
					return pkcs8(data);
				case "RSA PRIVATE KEY" :
					return KeyFactory.getInstance("RSA").generatePrivate(
							new PKCS8EncodedKeySpec(wrapRsa(data)));
				case "ENCRYPTED PRIVATE KEY" :
					throw new GeneralSecurityException(
							"chiave privata cifrata non supportata: decifrarla con 'openssl pkcs8 -topk8 -nocrypt'");
				default :
					throw new GeneralSecurityException("formato di chiave '"
							+ type
							+ "' non supportato: convertirla in PKCS#8 con 'openssl pkcs8 -topk8 -nocrypt'");
			}
		}
		return pkcs8(content);
	}

	/** Wraps a PKCS#1 RSAPrivateKey in a PKCS#8 PrivateKeyInfo. */
	static byte[] wrapRsa(final byte[] pkcs1) {
		final byte[] version = {0x02, 0x01, 0x00};
		return der(0x30, version, RSA_ALGORITHM, der(0x04, pkcs1));
	}

	private Pem() {
	}
}
