package net.rossonet.waldot.client.v1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.sdk.client.DiscoveryClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.OpcUaSession;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.identity.X509IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.client.api.WaldOTAgentClient;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.api.WaldotAgentClientObserver;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.SslHelper.KeyStoreHelper;
import net.rossonet.waldot.utils.ThreadHelper;

public class WaldOTAgentClientImplV1 implements WaldOTAgentClient {

	private static final String ABOUT_COMMAND = "about";
	private final static Logger logger = LoggerFactory.getLogger(WaldOTAgentClient.class);
	private static final String QUERY_COMMAND = "query";
	public static String SELFSIGNED_CERTIFICATE_ALIAS = "waldot-selfsigned";
	public static String SIGNED_CERTIFICATE_ALIAS = "waldot-signed";

	private transient Status _status = Status.INIT;
	private boolean activeConnectionRequest = false;
	private final KeyStoreHelper certificateHelper;
	private transient OpcUaClient client = null;
	private int clientFaultCounter;

	private final WaldOTAgentClientConfiguration configuration;
	private final Thread controlThread = Thread.ofVirtual().unstarted(() -> {
		Thread.currentThread().setName("waldot-client");
		Thread.currentThread().setPriority(CONTROL_THREAD_PRIORITY);
		logger.info("Control thread started");
		while (!_status.equals(Status.CLOSED)) {
			try {
				periodicalCheck();
				Thread.sleep(CONTROL_THREAD_SLEEP_TIME_MSEC);
			} catch (final Throwable t) {
				logger.warn("control thread error ", LogHelper.stackTraceToString(t, 5));
			}
		}
		logger.info("Control thread terminated");
	});

	private ByteString lastNonce = null;

	private WaldotAgentClientObserver waldotAgentClientObserver;

	public WaldOTAgentClientImplV1(final WaldOTAgentClientConfiguration configuration) {
		this.configuration = configuration;
		certificateHelper = new KeyStoreHelper(configuration.getKeyStorePath(), configuration.getKeyStorePassword());
		try {
			if (certificateHelper.hasCertificate(SELFSIGNED_CERTIFICATE_ALIAS)
					|| certificateHelper.hasCertificate(SIGNED_CERTIFICATE_ALIAS)) {
				logger.info("Certificate already exists in keystore, using it");
			} else {
				logger.info("Certificate not found in keystore, creating self-signed certificate");
				createSelfSignedCertificate(configuration);
			}
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			logger.error("Error while checking certificate in keystore: {}", LogHelper.stackTraceToString(e, 5));
			logger.info("Creating self-signed certificate");
			createSelfSignedCertificate(configuration);
		}
	}

	@Override
	public void changeStatus(final Status newStatus) {
		_status = newStatus;
		if (waldotAgentClientObserver != null) {
			waldotAgentClientObserver.onStatusChanged(newStatus);
		}
	}

	private boolean checkClientConnected(final OpcUaClient clientOpc) throws Exception {
		return clientOpc != null && getClientSessionWithTimeout(clientOpc).getServerNonce() != null;
	}

	private void checkFaultCounter() {
		clientFaultCounter++;
		logger.warn("Control cycle faulted, counter: {}", clientFaultCounter);
		if (!configuration.isIgnoreServiceFault() && clientFaultCounter > configuration.getMaxClientFaults()) {
			logger.error("Client faulted after {} attempts", clientFaultCounter);
			changeStatus(Status.FAULTED);
		}
	}

	private void cleanConnectionObjects() throws UaException {
		activeConnectionRequest = false;
		if (client != null) {
			client.disconnect();
			logger.info("Disconnected from server");
		}
	}

	@Override
	public void close() throws Exception {
		stopConnectionProcedure();
		changeStatus(Status.CLOSED);
	}

	public String[] createEdge(String label, String sourceNodeId, String destinationNodeId, String[] keyValues)
			throws UaException {
		final UaObjectNode cmdNode = getOpcUaClient().getAddressSpace().getObjectNode(new NodeId(2, "waldot/Edges"));
		logger.info("Creating edge with label: {}, sourceNodeId: {}, destinationNodeId: {}, keyValues: {}", label,
				sourceNodeId, destinationNodeId, String.join(",", keyValues));

		final Variant sourceVariant = Variant.of(sourceNodeId);
		final Variant labelVariant = Variant.of(label);
		final Variant destinationVariant = Variant.of(destinationNodeId);
		final Variant keyValuesVariant = Variant.of(String.join(",", keyValues));
		final Variant[] outputs = cmdNode.callMethod("add edge",
				new Variant[] { labelVariant, sourceVariant, destinationVariant, keyValuesVariant });
		final List<String> out = new ArrayList<>();
		for (final Variant output : outputs) {
			out.add((String) output.getValue());
		}
		return out.toArray(new String[0]);
	}

	private OpcUaClient createOpcClient(final OpcUaClientConfig actualConfiguration) throws UaException {
		final OpcUaClient createdClient = OpcUaClient.create(actualConfiguration);
		createdClient.addFaultListener(this);
		createdClient.connect();
		return createdClient;
	}

	private void createSelfSignedCertificate(final WaldOTAgentClientConfiguration configuration) {
		certificateHelper.createSelfSignedCertificate(SELFSIGNED_CERTIFICATE_ALIAS, configuration.getAgentUniqueName(),
				configuration.getGenerateCertOrganization(), configuration.getGenerateCertUnit(),
				configuration.getGenerateCertLocality(), configuration.getGenerateCertState(),
				configuration.getGenerateCertCountry(), null, configuration.getGenerateCertDns(),
				configuration.getGenerateCertIp(), configuration.getGenerateCertDnsAlias());
	}

	public String[] createVertex(String id, String label, String type, String[] keyValues) throws UaException {
		final UaObjectNode cmdNode = getOpcUaClient().getAddressSpace().getObjectNode(new NodeId(2, "waldot/Vertices"));
		logger.info("Creating vertex with id: {}, label: {}, type: {}, keyValues: {}", id, label, type,
				String.join(",", keyValues));

		final Variant idVariant = Variant.of(id);
		final Variant labelVariant = Variant.of(label);
		final Variant typeVariant = Variant.of(type);
		final Variant keyValuesVariant = Variant.of(String.join(",", keyValues));
		final Variant[] outputs = cmdNode.callMethod("add vertex",
				new Variant[] { idVariant, labelVariant, typeVariant, keyValuesVariant });
		final List<String> out = new ArrayList<>();
		for (final Variant output : outputs) {
			out.add((String) output.getValue());
		}
		return out.toArray(new String[0]);
	}

	private void doActionClientStart() {
		try {
			if (activeConnectionRequest) {
				if (checkClientConnected(client)) {
					changeStatus(Status.RUNNING);
					logger.info("Connected to server");
				} else {
					client = createOpcClient(getGeneralConfigBuilder().build());
					logger.info("Waiting for connection to server...");
				}
			}
		} catch (final Exception e) {
			logger.error("Error during control thread check: {}", LogHelper.stackTraceToString(e, 5));
			changeStatus(Status.FAULTED);
		}
	}

	private void doConnectionCheck() {
		try {
			if (checkClientConnected(client)) {
				lastNonce = getClientSessionWithTimeout(client).getServerNonce();
				if (clientFaultCounter > 0) {
					logger.info("Client reconnected after {} control cycle in faulted _status", clientFaultCounter);
					clientFaultCounter = 0;
				}
			} else {
				logger.warn("Client is not connected, trying to reconnect...");
				checkFaultCounter();
			}
		} catch (final Exception e) {
			logger.error("Error during control thread check: {}", LogHelper.stackTraceToString(e, 5));
			changeStatus(Status.FAULTED);
		}
	}

	private X509Certificate getCertificateActualConnection() {
		try {
			return getKeyPairActualConnection().getPublic() instanceof X509Certificate
					? (X509Certificate) getKeyPairActualConnection().getPublic()
					: null;
		} catch (final Exception e) {
			logger.error("Error while getting certificate for actual connection: {}",
					LogHelper.stackTraceToString(e, 7));
			changeStatus(Status.FAULTED);
			return null;
		}
	}

	private OpcUaSession getClientSessionWithTimeout(final OpcUaClient clientOpc) throws Exception {
		final Callable<OpcUaSession> callable = new Callable<OpcUaSession>() {
			@Override
			public OpcUaSession call() throws Exception {
				final OpcUaSession session = clientOpc.getSession();
				return Optional.ofNullable(session).orElseThrow(() -> new UaException(-1, "No session"));
			}
		};
		ThreadHelper.runWithTimeout(callable, TIMEOUT_GET_SESSION_SEC, TimeUnit.SECONDS);
		return callable.call();
	}

	@Override
	public WaldOTAgentClientConfiguration getConfiguration() {
		return configuration;
	}

	private EndpointDescription getEndpoint() {
		EndpointDescription targetEndpoint = null;
		List<EndpointDescription> endpoints = null;
		try {
			final String discoveryEndpoint = configuration.getDiscoveryEndpoint();
			logger.info("Discovering OPCUA endpoints on: {}", discoveryEndpoint);
			endpoints = DiscoveryClient.getEndpoints(discoveryEndpoint).get();
		} catch (final Exception exception) {
			logger.error("Error while discovering OPCUA endpoints: {}", LogHelper.stackTraceToString(exception, 5));
		}
		final MessageSecurityMode searchSecurityMode = configuration.getMessageSecurityMode();
		final String securityPolicyUri = configuration.getSecurityPolicy().getUri();
		if (endpoints != null && !endpoints.isEmpty()) {
			for (final EndpointDescription e : endpoints) {
				if (e.getSecurityPolicyUri().equals(securityPolicyUri)
						&& e.getSecurityMode().equals(searchSecurityMode)) {
					logger.debug("found endpoint: {}", e);
					targetEndpoint = e;
					break;
				}
			}
		}
		if (targetEndpoint == null) {
			logger.error("no endpoint found with security policy: {} and security mode: {}", securityPolicyUri,
					searchSecurityMode);
		} else if (configuration.getForceDiscoveredEndpointUrl() != null
				&& !configuration.getForceDiscoveredEndpointUrl().isEmpty()) {
			logger.warn("force discovery endpoint URL: {}", configuration.getForceDiscoveredEndpointUrl());
			targetEndpoint = EndpointUtil.updateUrl(targetEndpoint, configuration.getForceDiscoveredEndpointUrl());
		}
		try {
			logServerCertificate(targetEndpoint);
		} catch (final CertificateException e) {
			logger.error("Error while logging server certificate: {}", LogHelper.stackTraceToString(e, 5));
		}
		return targetEndpoint;
	}

	private OpcUaClientConfigBuilder getGeneralConfigBuilder() throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException, UnrecoverableKeyException {
		final OpcUaClientConfigBuilder config = OpcUaClientConfig.builder().setEndpoint(getEndpoint())
				.setApplicationUri(configuration.getApplicationUri())
				.setApplicationName(LocalizedText.english(configuration.getApplicationName()));
		config.setSessionTimeout(UInteger.valueOf(configuration.getSessionTimeout()));
		// config.setConnectTimeout(UInteger.valueOf(configuration.getConnectTimeout()));
		config.setKeepAliveTimeout(UInteger.valueOf(configuration.getKeepAliveTimeout()));
		config.setRequestTimeout(UInteger.valueOf(configuration.getRequestTimeout()));
		// config.setChannelLifetime(UInteger.valueOf(configuration.getChannelLifetime()));
		// config.setAcknowledgeTimeout(UInteger.valueOf(configuration.getAcknowledgeTimeout()));
		config.setIdentityProvider(getIdentityProviderActualConnection());
		config.setKeyPair(getKeyPairActualConnection());
		// config.setCertificateChain(getCertificateChainActualConnection());
		config.setCertificate(getCertificateActualConnection());
		if (configuration.isForceCertificateValidator()) {
			final CertificateValidator alwaysValidCert = new CertificateValidator() {

				@Override
				public void validateCertificateChain(final List<X509Certificate> certificateChain,
						final String applicationUri, final String... validHostNames) throws UaException {
					final StringBuilder sb = new StringBuilder();
					final StringBuilder hosts = new StringBuilder();
					for (final X509Certificate c : certificateChain) {
						sb.append("\n" + c + "\n");
					}
					for (final String host : validHostNames) {
						sb.append(host + ", ");
					}
					logger.warn(
							"because forceCertificateValidator is true, the function validateCertificateChain authorizes the follow certificates"
									+ sb.toString() + "\nApplicationUri is " + applicationUri + " and endpoint is "
									+ getEndpoint().getEndpointUrl() + "\nValid hostnames are: " + hosts.toString());

				}

			};
			config.setCertificateValidator(alwaysValidCert);
		}
		return config;
	}

	private IdentityProvider getIdentityProviderActualConnection() {
		IdentityProvider idp = null;
		final UserTokenType authMode = UserTokenType.Anonymous;
		switch (authMode) {
		case UserTokenType.UserName:
			final String username = null;
			final String password = null;
			if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
				idp = new UsernameProvider(username, password);
			} else {
				logger.warn(
						"Username and/or password not set for UserName authentication, using AnonymousProvider instead.");
			}

			break;
		case UserTokenType.Anonymous:
			idp = new AnonymousProvider();
			break;
		case UserTokenType.Certificate:
			final X509Certificate authCertificateChain = null;
			final PrivateKey authPrivateKey = null;
			idp = new X509IdentityProvider(authCertificateChain, authPrivateKey);
			break;
		default:
			idp = new AnonymousProvider();
			break;
		}
		return idp;
	}

	private KeyPair getKeyPairActualConnection() throws UnrecoverableKeyException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {
		try {
			final KeyPair signed = certificateHelper.getKeyPair(SIGNED_CERTIFICATE_ALIAS);
			if (signed != null) {
				return signed;
			} else {
				return certificateHelper.getKeyPair(SELFSIGNED_CERTIFICATE_ALIAS);
			}
		} catch (final Exception e) {
			logger.error("Error while getting signed certificate, falling back to self-signed: {}",
					LogHelper.stackTraceToString(e, 2));
			return certificateHelper.getKeyPair(SELFSIGNED_CERTIFICATE_ALIAS);
		}
	}

	public ByteString getLastNonce() {
		return lastNonce;
	}

	@Override
	public OpcUaClient getOpcUaClient() {
		return client;

	}

	public List<String> getServerInfo() throws UaException {
		final UaObjectNode cmdNode = getOpcUaClient().getAddressSpace()
				.getObjectNode(new NodeId(2, MiloStrategy.GENERAL_CMD_DIRECTORY));
		final Variant[] outputs = cmdNode.callMethod(ABOUT_COMMAND, new Variant[] {});
		final List<String> out = new ArrayList<>();
		for (final Variant output : outputs) {
			out.add((String) output.getValue());
		}
		return out;
	}

	@Override
	public Status getStatus() {
		return _status;
	}

	private void logServerCertificate(final EndpointDescription endpoint) throws CertificateException {
		if (!endpoint.getServerCertificate().isNullOrEmpty()) {
			logger.info("Server certificate for endpoint {}: {}", endpoint, CertificateFactory.getInstance("X.509")
					.generateCertificate(new ByteArrayInputStream(endpoint.getServerCertificate().bytes())));
		}
	}

	@Override
	public void onServiceFault(final ServiceFault serviceFault) {
		logger.error("Service fault received: {}", serviceFault);
		changeStatus(Status.FAULTED);
	}

	private void periodicalCheck() {
		switch (_status) {
		case RUNNING:
			doConnectionCheck();
			break;
		case CLOSED:
			logger.info("Client is closed, the control thread will terminate");
			break;
		case FAULTED:
			logger.error("Client is in faulted _status, resetting connection");
			try {
				cleanConnectionObjects();
			} catch (final UaException e) {
				logger.error("Error during cleaning connection objects: {}", LogHelper.stackTraceToString(e, 5));
			}
			activeConnectionRequest = true;
			changeStatus(Status.INIT);
			break;
		case INIT:
			doActionClientStart();
			logger.info("Client is initializing");
			break;
		case STOPPED:
			logger.trace("Client is stopped");
			break;
		default:
			break;
		}
	}

	public List<String> runExpression(String expression) throws UaException {
		final UaObjectNode cmdNode = getOpcUaClient().getAddressSpace()
				.getObjectNode(new NodeId(2, MiloStrategy.GENERAL_CMD_DIRECTORY));
		final Variant[] outputs = cmdNode.callMethod(QUERY_COMMAND, new Variant[] { new Variant(expression) });
		final List<String> out = new ArrayList<>();
		for (final Variant output : outputs) {
			out.add((String) output.getValue());
		}
		return out;
	}

	@Override
	public void setStatusObserver(final WaldotAgentClientObserver waldotAgentClientObserver) {
		this.waldotAgentClientObserver = waldotAgentClientObserver;
	}

	@Override
	public CompletableFuture<WaldOTAgentClient> startConnectionProcedure() {
		controlThread.start();
		activeConnectionRequest = true;
		return CompletableFuture.completedFuture(this);

	}

	@Override
	public CompletableFuture<WaldOTAgentClient> stopConnectionProcedure() {
		changeStatus(Status.STOPPED);
		try {
			cleanConnectionObjects();
		} catch (final UaException e) {
			logger.error("Error during cleaning connection objects: {}", LogHelper.stackTraceToString(e, 5));
		}
		return CompletableFuture.completedFuture(this);
	}

}
