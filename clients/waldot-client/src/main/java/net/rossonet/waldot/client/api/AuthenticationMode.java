package net.rossonet.waldot.client.api;

/** Identity used by the client to open the OPC UA session. */
public enum AuthenticationMode {
	/** anonymous session */
	ANONYMOUS,
	/** X509 certificate ({@code cryptoCertificateChain} and {@code cryptoPrivateKey}) */
	CERTIFICATE,
	/** username and password */
	USERNAME
}
