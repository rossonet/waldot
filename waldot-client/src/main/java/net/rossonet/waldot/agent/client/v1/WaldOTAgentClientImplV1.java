package net.rossonet.waldot.agent.client.v1;

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
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient;
import net.rossonet.waldot.agent.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.agent.client.api.WaldotAgentClientObserver;
import net.rossonet.waldot.client.exception.ProvisioningException;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.SslHelper.KeyStoreHelper;
import net.rossonet.waldot.utils.ThreadHelper;

public class WaldOTAgentClientImplV1 implements WaldOTAgentClient {

	private final static Logger logger = LoggerFactory.getLogger(WaldOTAgentClient.class);
	public static String SELFSIGNED_CERTIFICATE_ALIAS = "waldot-selfsigned";
	public static String SIGNED_CERTIFICATE_ALIAS = "waldot-signed";

	private transient Status _status = Status.INIT;
	private boolean activeConnectionRequest = false;
	private final KeyStoreHelper certificateHelper;
	private transient OpcUaClient client = null;
	private int clientFaultCounter;

	private final WaldOTAgentClientConfiguration configuration;
	private final Thread controlThread = Thread.ofVirtual().start(() -> {
		Thread.currentThread().setName("waldot-control");
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

	private transient OpcUaClient provisioningClient = null;

	private final ProvisioningLifeCycleProcedure provisioningLifeCycle = new ProvisioningLifeCycleProcedure(this);

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
		if (provisioningClient != null) {
			provisioningClient.disconnect();
			logger.info("Disconnected from provisioning server");
		}
	}

	@Override
	public void close() throws Exception {
		stopConnectionProcedure();
		changeStatus(Status.CLOSED);
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

	private void doActionClientStart() {
		try {
			if (activeConnectionRequest) {
				if (checkClientConnected(client)) {
					changeStatus(Status.CONNECTED);
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

	private void doActionProvisioningClientManualStart() {
		try {
			if (activeConnectionRequest) {
				if (checkClientConnected(provisioningClient)) {
					changeStatus(Status.CONNECTED_PROVISIONING_MANUAL_APPROVAL);
					logger.info("Connected to server for manual approval provisioning");
				} else {
					provisioningClient = createOpcClient(getGeneralConfigBuilder().build());
					logger.info("Waiting for connection to server for manual approval provisioning...");
				}
			}
		} catch (final Exception e) {
			logger.error("Error during control thread check: {}", LogHelper.stackTraceToString(e, 5));
			changeStatus(Status.FAULTED);
		}
	}

	private void doActionProvisioningClientTokenStart() {
		try {
			if (activeConnectionRequest) {
				if (checkClientConnected(provisioningClient)) {
					changeStatus(Status.CONNECTED_PROVISIONING_TOKEN);
					logger.info("Connected to server for token provisioning");
				} else {
					provisioningClient = createOpcClient(getGeneralConfigBuilder().build());
					logger.info("Waiting for connection to server for token provisioning...");
				}
			}
		} catch (final Exception e) {
			logger.error("Error during control thread check: {}", LogHelper.stackTraceToString(e, 5));
			changeStatus(Status.FAULTED);
		}
	}

	private void doActionProvisioningManualRegistration() {
		final Runnable approvalRequest = new Runnable() {
			@Override
			public void run() {
				try {
					provisioningLifeCycle.requestManualApprovation();
					changeStatus(WaldOTAgentClient.Status.COMPLETED_PROVISIONING_MANUAL_REQUEST);
				} catch (final ProvisioningException e) {
					logger.error("Error during manual approval provisioning: {}", LogHelper.stackTraceToString(e, 5));
					checkFaultCounter();
				}

			}
		};
		try {
			ThreadHelper.runWithTimeout(approvalRequest, TIMEOUT_PROVISIONING_ACTION_SEC, TimeUnit.SECONDS);
		} catch (final Exception e) {
			logger.error("Error during manual approval provisioning: {}", LogHelper.stackTraceToString(e, 5));
			checkFaultCounter();
		}
	}

	private void doActionProvisioningTokenRegistration() {
		final Runnable tokenRequest = new Runnable() {
			@Override
			public void run() {
				try {
					provisioningLifeCycle.tokenProvisioning();
					changeStatus(WaldOTAgentClient.Status.COMPLETED_PROVISIONING_TOKEN);
				} catch (final ProvisioningException e) {
					logger.error("Error during token provisioning: {}", LogHelper.stackTraceToString(e, 5));
					checkFaultCounter();
				}

			}
		};
		try {
			ThreadHelper.runWithTimeout(tokenRequest, TIMEOUT_PROVISIONING_ACTION_SEC, TimeUnit.SECONDS);
		} catch (final Exception e) {
			logger.error("Error during token provisioning: {}", LogHelper.stackTraceToString(e, 5));
			checkFaultCounter();
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

	private X509Certificate getCertificateActualConnection()
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		return certificateHelper.getCertificate(SIGNED_CERTIFICATE_ALIAS);
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
		return certificateHelper.getKeyPair(SIGNED_CERTIFICATE_ALIAS);
	}

	public ByteString getLastNonce() {
		return lastNonce;
	}

	@Override
	public OpcUaClient getOpcUaClient() {
		if (client == null) {
			return provisioningClient;
		}
		return client;

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

	private void nextConnectionAction() {
		if (configuration.hasCertificateAuthentication()) {
			changeStatus(Status.STARTING);
		} else {
			if (configuration.hasProvisioningToken()) {
				changeStatus(Status.STARTING_PROVISIONING_TOKEN);
			} else {
				changeStatus(Status.STARTING_PROVISIONING_MANUAL_APPROVAL);
			}
		}
	}

	@Override
	public void onServiceFault(final ServiceFault serviceFault) {
		logger.error("Service fault received: {}", serviceFault);
		changeStatus(Status.FAULTED);
	}

	private void periodicalCheck() {
		switch (_status) {
		case STARTING:
			doActionClientStart();
			break;
		case STARTING_PROVISIONING_MANUAL_APPROVAL:
			doActionProvisioningClientManualStart();
			break;
		case STARTING_PROVISIONING_TOKEN:
			doActionProvisioningClientTokenStart();
			break;
		case CONNECTED:
			doConnectionCheck();
			refreshCertificateIfNeeded();
			break;
		case CONNECTED_PROVISIONING_MANUAL_APPROVAL:
			doActionProvisioningManualRegistration();
			break;
		case CONNECTED_PROVISIONING_TOKEN:
			doActionProvisioningTokenRegistration();
			break;
		case COMPLETED_PROVISIONING_MANUAL_REQUEST:
			changeStatus(Status.WAITING_PROVISIONING_MANUAL_APPROVAL);
			break;
		case COMPLETED_PROVISIONING_TOKEN:
			changeStatus(Status.STARTING);
			break;
		case WAITING_PROVISIONING_MANUAL_APPROVAL:
			waitingManualApprovation();
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
			nextConnectionAction();
			break;
		case INIT:
			logger.trace("Client is in INIT _status, waiting for start");
			break;
		case STOPPED:
			logger.trace("Client is stopped");
			break;
		default:
			break;
		}
	}

	private void refreshCertificateIfNeeded() {
		// TODO rigenerare il certificato se necessario
	}

	@Override
	public void setStatusObserver(final WaldotAgentClientObserver waldotAgentClientObserver) {
		this.waldotAgentClientObserver = waldotAgentClientObserver;
	}

	@Override
	public CompletableFuture<WaldOTAgentClient> startConnectionProcedure() {
		controlThread.start();
		activeConnectionRequest = true;
		nextConnectionAction();
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

	private void waitingManualApprovation() {
		logger.info("Waiting for manual approval provisioning, please check the server for the approval request");
		logger.info("REQUEST UNIQUE CODE IS: {}", provisioningLifeCycle.getRequestUniqueCode());
		try {
			Thread.sleep(MANUAL_APPROVAL_WAITING_TIME_SEC * 1000L);
			if (provisioningLifeCycle.isManualRequestCompleted()) {
				changeStatus(Status.CONNECTED_PROVISIONING_TOKEN);
			}
		} catch (final InterruptedException e) {
			logger.warn("Thread interrupted during waiting for manual approval provisioning");
		}
	}

}
