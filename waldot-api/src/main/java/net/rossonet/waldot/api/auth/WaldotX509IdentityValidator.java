package net.rossonet.waldot.api.auth;

import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;

/**
 * Identita' utente X.509 dei client OPC UA (user token X509): ammessa se il
 * certificato e' valido e sta nella trust list delle identita' (file PEM o DER
 * in {@code <security-dir>/users/trusted}, impostata dal server con
 * {@link #setTrustedDirectory(Path)}) o e' firmato da un certificato della
 * lista. Senza lista ogni identita' X.509 e' rifiutata. La lista si rilegge a
 * ogni richiesta (pochi file).
 */
public class WaldotX509IdentityValidator extends X509IdentityValidator {

	private static final Logger logger = LoggerFactory.getLogger(WaldotX509IdentityValidator.class);

	/** Directory della trust list rispetto alla directory di sicurezza del server. */
	public static final String USERS_TRUSTED_DIRECTORY = "users/trusted";

	private static volatile Path sharedDirectory;

	/** Certificati della trust list (file illeggibili ignorati). */
	static List<X509Certificate> trusted(final Path directory) {
		final List<X509Certificate> result = new ArrayList<>();
		if (directory == null || !Files.isDirectory(directory)) {
			return result;
		}
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
			final CertificateFactory factory = CertificateFactory.getInstance("X.509");
			for (final Path file : files) {
				if (!Files.isRegularFile(file)) {
					continue;
				}
				try (InputStream in = Files.newInputStream(file)) {
					for (final Certificate c : factory.generateCertificates(in)) {
						result.add((X509Certificate) c);
					}
				} catch (final Exception e) {
					logger.warn("X509 identity trust list: {} not readable ({})", file, e.getMessage());
				}
			}
		} catch (final Exception e) {
			logger.warn("X509 identity trust list {}: {}", directory, e.getMessage());
		}
		return result;
	}

	/** true se il certificato e' valido e nella lista o firmato da un suo certificato. */
	static boolean accepted(final X509Certificate certificate, final List<X509Certificate> trusted) {
		try {
			certificate.checkValidity();
		} catch (final Exception e) {
			return false;
		}
		for (final X509Certificate t : trusted) {
			if (t.equals(certificate)) {
				return true;
			}
			try {
				certificate.verify(t.getPublicKey());
				return true;
			} catch (final Exception e) {
				// non firmato da questo
			}
		}
		return false;
	}

	private static Predicate<X509Certificate> challenge() {
		return certificate -> {
			final Path directory = sharedDirectory;
			final boolean ok = accepted(certificate, trusted(directory));
			if (!ok) {
				logger.warn("X509 user identity {} refused (not in {})",
						certificate.getSubjectX500Principal(), directory);
			}
			return ok;
		};
	}

	public WaldotX509IdentityValidator(final WaldotConfiguration configuration) {
		super(challenge());
	}

	/** Directory della trust list delle identita' X.509 (dal server). */
	public void setTrustedDirectory(final Path directory) {
		sharedDirectory = directory;
	}
}
