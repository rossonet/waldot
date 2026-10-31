package net.rossonet.waldot.utils;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;

/**
 * RSA key pairs for the self-signed certificates of servers and clients.
 *
 * <p>Generating a 2048 bit RSA key takes from tens to hundreds of
 * milliseconds of CPU. With the system property
 * {@value #REUSE_PROPERTY}{@code =true} (set by the test environment) every
 * request returns the same key pair, generated once per JVM; the
 * certificates are still built for each request, so their names and URIs
 * stay correct. Without the property a new key pair is generated every
 * time.</p>
 */
public final class GeneratedKeyPairs {

	/** System property that enables the reuse of the key pair. */
	public static final String REUSE_PROPERTY = "waldot.security.reuseGeneratedKeyPair";

	private static volatile KeyPair shared;

	/**
	 * Returns a 2048 bit RSA key pair: a new one, or the shared one when
	 * {@value #REUSE_PROPERTY} is {@code true}.
	 *
	 * @return the key pair
	 * @throws NoSuchAlgorithmException if RSA is not available
	 */
	public static KeyPair rsa2048() throws NoSuchAlgorithmException {
		if (!Boolean.getBoolean(REUSE_PROPERTY)) {
			return SelfSignedCertificateGenerator.generateRsaKeyPair(2048);
		}
		KeyPair keyPair = shared;
		if (keyPair == null) {
			synchronized (GeneratedKeyPairs.class) {
				keyPair = shared;
				if (keyPair == null) {
					keyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);
					shared = keyPair;
				}
			}
		}
		return keyPair;
	}

	private GeneratedKeyPairs() {
	}
}
