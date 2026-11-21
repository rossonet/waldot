package net.rossonet.waldot.opc;

import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validatore dei certificati dei client che registra a livello warn il motivo
 * del rifiuto (SecureChannel e CreateSession). Milo lo scrive solo a livello
 * debug e al client arriva un ServiceFault senza traccia nel log del server:
 * nel laboratorio del 21/11 ogni CreateSession del connettore OPC UA era
 * rifiutata con Bad_CertificateUriInvalid (certificato client senza URI nel
 * SAN) e il server mostrava solo i controlli soppressi.
 */
public class LoggingCertificateValidator implements CertificateValidator {

	private static final Logger logger = LoggerFactory.getLogger(LoggingCertificateValidator.class);

	/** Descrizione del rifiuto per il log. */
	static String describe(List<X509Certificate> certificateChain, String applicationUri, UaException e) {
		final X509Certificate certificate = certificateChain == null || certificateChain.isEmpty() ? null
				: certificateChain.get(0);
		final StringBuilder text = new StringBuilder("client certificate ")
				.append(certificate == null ? "(none)" : certificate.getSubjectX500Principal().getName())
				.append(" refused: ").append(StatusCodes.lookup(e.getStatusCode().value())
						.map(n -> n[0]).orElse(e.getStatusCode().toString()));
		if (e.getMessage() != null) {
			text.append(" (").append(e.getMessage()).append(")");
		}
		if (applicationUri != null) {
			text.append(", application URI ").append(applicationUri);
		}
		if (e.getStatusCode().value() == StatusCodes.Bad_CertificateUriInvalid && certificate != null) {
			// l'URI dell'ApplicationDescription deve essere tra gli URI del SAN
			text.append(", URI in the certificate ").append(
					CertificateUtil.getSubjectAltNameField(certificate, CertificateUtil.SUBJECT_ALT_NAME_URI));
		}
		return text.toString();
	}

	private final CertificateValidator delegate;

	public LoggingCertificateValidator(CertificateValidator delegate) {
		this.delegate = delegate;
	}

	@Override
	public void validateCertificateChain(List<X509Certificate> certificateChain, String applicationUri,
			String[] validHostnames) throws UaException {
		try {
			delegate.validateCertificateChain(certificateChain, applicationUri, validHostnames);
		} catch (final UaException e) {
			logger.warn(describe(certificateChain, applicationUri, e));
			throw e;
		}
	}
}
