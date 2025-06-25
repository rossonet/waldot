package net.rossonet.waldot.agent.client.v1;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.milo.opcua.sdk.client.AddressSpace;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaSession;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.X509IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.client.security.ClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.serialization.UaRequestMessage;
import org.eclipse.milo.opcua.stack.core.serialization.UaResponseMessage;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.AddNodesItem;
import org.eclipse.milo.opcua.stack.core.types.structured.AddNodesResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.AddReferencesItem;
import org.eclipse.milo.opcua.stack.core.types.structured.AddReferencesResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseNextResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowsePath;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.CreateMonitoredItemsResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.CreateSubscriptionResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteMonitoredItemsResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteNodesItem;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteNodesResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteReferencesItem;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteReferencesResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.DeleteSubscriptionsResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ModifyMonitoredItemsResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ModifySubscriptionResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemModifyRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.PublishResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterNodesResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.RepublishResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.eclipse.milo.opcua.stack.core.types.structured.SetMonitoringModeResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.SetPublishingModeResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.SetTriggeringResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.SubscriptionAcknowledgement;
import org.eclipse.milo.opcua.stack.core.types.structured.TransferSubscriptionsResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.TranslateBrowsePathsToNodeIdsResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.UnregisterNodesResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ViewDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient;
import net.rossonet.waldot.agent.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.agent.client.api.WaldotAgentClientObserver;
import net.rossonet.waldot.agent.exception.ProvisioningException;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.ThreadHelper;

public class WaldOTAgentClientImplV1 implements WaldOTAgentClient {
	private final class WaldOTAgentThread extends Thread {

		@Override
		public void run() {
			Thread.currentThread().setName("waldot-control");
			Thread.currentThread().setPriority(CONTROL_THREAD_PRIORITY);
			logger.info("Control thread started");
			while (!status.equals(Status.CLOSED)) {
				try {
					periodicalCheck();
					Thread.sleep(CONTROL_THREAD_SLEEP_TIME_MSEC);
				} catch (final Throwable t) {
					logger.warn("control thread error ", LogHelper.stackTraceToString(t, 5));
				}
			}
			logger.info("Control thread terminated");
		}

	}

	private final static Logger logger = LoggerFactory.getLogger(WaldOTAgentClient.class);

	private boolean activeConnectionRequest = false;
	private transient OpcUaClient client = null;
	private int clientFaultCounter;
	private final WaldOTAgentClientConfiguration configuration;
	private final WaldOTAgentThread controlThread = new WaldOTAgentThread();

	private ByteString lastNonce = null;

	private transient OpcUaClient provisioningClientclient;

	private final ProvisioningLifeCycle provisioningLifeCycle = new ProvisioningLifeCycle(this);

	private transient Status status = Status.INIT;

	private WaldotAgentClientObserver waldotAgentClientObserver;

	public WaldOTAgentClientImplV1(final WaldOTAgentClientConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public CompletableFuture<AddNodesResponse> addNodes(List<AddNodesItem> nodesToAdd) {
		return client.addNodes(nodesToAdd);
	}

	@Override
	public CompletableFuture<AddReferencesResponse> addReferences(List<AddReferencesItem> referencesToAdd) {
		return client.addReferences(referencesToAdd);
	}

	@Override
	public CompletableFuture<BrowseResponse> browse(ViewDescription viewDescription, UInteger maxReferencesPerNode,
			List<BrowseDescription> nodesToBrowse) {
		return client.browse(viewDescription, maxReferencesPerNode, nodesToBrowse);
	}

	@Override
	public CompletableFuture<BrowseNextResponse> browseNext(boolean releaseContinuationPoints,
			List<ByteString> continuationPoints) {
		return client.browseNext(releaseContinuationPoints, continuationPoints);
	}

	@Override
	public CompletableFuture<CallResponse> call(List<CallMethodRequest> methodsToCall) {
		return client.call(methodsToCall);
	}

	@Override
	public void changeStatus(final Status newStatus) {
		status = newStatus;
		if (waldotAgentClientObserver != null) {
			waldotAgentClientObserver.onStatusChanged(newStatus);
		}
	}

	private boolean checkClientConnected(OpcUaClient clientOpc)
			throws InterruptedException, ExecutionException, TimeoutException {
		return clientOpc != null && clientOpc.getSession().isDone()
				&& getClientSessionWithTimeout(clientOpc).getServerNonce() != null;
	}

	private void checkFaultCounter() {
		clientFaultCounter++;
		logger.warn("Control cycle faulted, counter: {}", clientFaultCounter);
		if (!configuration.isIgnoreServiceFault() && clientFaultCounter > configuration.getMaxClientFaults()) {
			logger.error("Client faulted after {} attempts", clientFaultCounter);
			changeStatus(Status.FAULTED);
		}
	}

	private void cleanConnectionObjects() {
		activeConnectionRequest = false;
		if (client != null) {
			client.disconnect();
			logger.info("Disconnected from server");
		}
		if (provisioningClientclient != null) {
			provisioningClientclient.disconnect();
			logger.info("Disconnected from provisioning server");
		}
	}

	@Override
	public void close() throws Exception {
		disconnect();
		changeStatus(Status.CLOSED);
	}

	@Override
	public CompletableFuture<? extends UaClient> connect() {
		controlThread.start();
		activeConnectionRequest = true;
		nextConnectionAction();
		return CompletableFuture.completedFuture(this);
	}

	@Override
	public CompletableFuture<CreateMonitoredItemsResponse> createMonitoredItems(UInteger subscriptionId,
			TimestampsToReturn timestampsToReturn, List<MonitoredItemCreateRequest> itemsToCreate) {
		return client.createMonitoredItems(subscriptionId, timestampsToReturn, itemsToCreate);
	}

	private OpcUaClient createOpcClient(final OpcUaClientConfig actualConfiguration) throws UaException {
		final OpcUaClient createdClient = OpcUaClient.create(actualConfiguration);
		createdClient.addFaultListener(this);
		createdClient.connect();
		return createdClient;
	}

	@Override
	public CompletableFuture<CreateSubscriptionResponse> createSubscription(double requestedPublishingInterval,
			UInteger requestedLifetimeCount, UInteger requestedMaxKeepAliveCount, UInteger maxNotificationsPerPublish,
			boolean publishingEnabled, UByte priority) {
		return client.createSubscription(requestedPublishingInterval, requestedLifetimeCount,
				requestedMaxKeepAliveCount, maxNotificationsPerPublish, publishingEnabled, priority);
	}

	@Override
	public CompletableFuture<DeleteMonitoredItemsResponse> deleteMonitoredItems(UInteger subscriptionId,
			List<UInteger> monitoredItemIds) {
		return client.deleteMonitoredItems(subscriptionId, monitoredItemIds);
	}

	@Override
	public CompletableFuture<DeleteNodesResponse> deleteNodes(List<DeleteNodesItem> nodesToDelete) {
		return client.deleteNodes(nodesToDelete);
	}

	@Override
	public CompletableFuture<DeleteReferencesResponse> deleteReferences(List<DeleteReferencesItem> referencesToDelete) {
		return client.deleteReferences(referencesToDelete);
	}

	@Override
	public CompletableFuture<DeleteSubscriptionsResponse> deleteSubscriptions(List<UInteger> subscriptionIds) {
		return client.deleteSubscriptions(subscriptionIds);
	}

	@Override
	public CompletableFuture<? extends UaClient> disconnect() {
		changeStatus(Status.STOPPED);
		cleanConnectionObjects();
		return CompletableFuture.completedFuture(this);
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
				if (checkClientConnected(provisioningClientclient)) {
					changeStatus(Status.CONNECTED_PROVISIONING_MANUAL_APPROVAL);
					logger.info("Connected to server for manual approval provisioning");
				} else {
					provisioningClientclient = createOpcClient(getGeneralConfigBuilder().build());
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
				if (checkClientConnected(provisioningClientclient)) {
					changeStatus(Status.CONNECTED_PROVISIONING_TOKEN);
					logger.info("Connected to server for token provisioning");
				} else {
					provisioningClientclient = createOpcClient(getGeneralConfigBuilder().build());
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
					logger.info("Client reconnected after {} control cycle in faulted status", clientFaultCounter);
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

	private void generateCertificate() {
		final OpcKeyUtils loader = new OpcKeyUtils();
		final String commonName = configuration.getCommonName();
		final String organization = configuration.getOrganization();
		final String unit = configuration.getUnit();
		final String locality = configuration.getLocality();
		final String state = configuration.getState();
		final String country = configuration.getCountry();
		final String uri = configuration.getUri();
		final String dns = configuration.getDns();
		final String dnsAlias = configuration.getDnsAlias();
		final String ip = configuration.getIp();
		if (!dns.isEmpty() && !ip.isEmpty() && !commonName.isEmpty() && !organization.isEmpty() && !state.isEmpty()
				&& !country.isEmpty()) {

			logger.debug("try to generate certificate with data: " + commonName + "," + organization + "," + unit + ","
					+ locality + "," + state + "," + country + "," + uri + "," + dns + "," + ip);
			loader.create(commonName, organization, unit, locality, state, country, uri, dns, ip, dnsAlias);
		} else {
			logger.debug("try to generate standard certificate");
			loader.create();
		}
		final String privateKey = loader.getPrivateKeyBase64().toString();
		String crt = null;
		try (final FileWriter certFileWriter = new FileWriter(GENERATED_CERT_PATH)) {
			final PrintWriter printCertWriter = new PrintWriter(certFileWriter);
			crt = loader.getClientCertificateBase64().toString();
			printCertWriter.print(crt);
			printCertWriter.close();

			logger.debug("written " + GENERATED_CERT_PATH + " with new certificate");
		} catch (final IOException | CertificateEncodingException e) {
			logger.error("saving cert generated", e);
		}
		try (final FileWriter keyFileWriter = new FileWriter(GENERATED_KEY_PATH)) {
			final PrintWriter printKeyWriter = new PrintWriter(keyFileWriter);
			printKeyWriter.print(privateKey);
			printKeyWriter.close();

			logger.debug("written " + GENERATED_KEY_PATH + " with new certificate");
		} catch (final IOException e) {
			logger.error("saving key generated", e);
		}

	}

	@Override
	public AddressSpace getAddressSpace() {
		return client.getAddressSpace();
	}

	private X509Certificate getCertificate() {
		String cryptoPrivateKey = configuration.getCryptoPrivateKey();
		cryptoPrivateKey = convertFileToString(cryptoPrivateKey);
		String cryptoPublicCrt = configuration.getCryptoPublicCrt();
		cryptoPublicCrt = convertFileToString(cryptoPublicCrt);
		if (cryptoPrivateKey != null && cryptoPublicCrt != null) {
			final OpcKeyUtils opcKeyUtils = new OpcKeyUtils();
			opcKeyUtils.setClientKeyPair(cryptoPrivateKey, cryptoPublicCrt);
			final X509Certificate clientCertificate = opcKeyUtils.getClientCertificate();
			clientCertificateChain = opcKeyUtils.getClientCertificateChain();
			return clientCertificate;
		} else {
			return null;
		}
	}

	private X509Certificate getCertificateActualConnection() {
		// TODO Auto-generated method stub
		return null;
	}

	private X509Certificate[] getCertificateChain() {

		String certificateChainList = configuration.getCryptoCertificateChainList();
		certificateChainList = convertFileToString(certificateChainList);
		logger.trace("certificateChainList " + certificateChainList);
		if (certificateChainList != null) {
			final List<X509Certificate> certList = new ArrayList<>();
			if (certificateChainList != null) {
				if (certificateChainList.contains(CERTIFICATE_CHAIN_SEPARATOR)) {
					for (final String singleCert : certificateChainList.split(CERTIFICATE_CHAIN_SEPARATOR)) {
						try {
							certList.add((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
									new ByteArrayInputStream(Base64.getDecoder().decode(singleCert.getBytes()))));
						} catch (final CertificateException exception) {
							logger.error("encoding one of crypto chains cert", exception);
						}
					}
				} else {
					try {
						certList.add((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
								new ByteArrayInputStream(Base64.getDecoder().decode(certificateChainList.getBytes()))));
					} catch (final CertificateException exception) {
						logger.error("encoding unique crypto chain cert", exception);
					}
				}
			}
			return certList.toArray(new X509Certificate[0]);
		} else {
			return clientCertificateChain;
		}
	}

	private X509Certificate[] getCertificateChainActualConnection() {
		// TODO Auto-generated method stub
		return null;
	}

	private X509Certificate getClientAuthCertificate() {
		final OpcKeyUtils opcKeyUtils = new OpcKeyUtils();
		String authPrivateKey = configuration.getAuthPrivateKey();
		authPrivateKey = convertFileToString(authPrivateKey);
		String authPublicCrt = configuration.getAuthPublicCrt();
		authPublicCrt = convertFileToString(authPublicCrt);
		opcKeyUtils.setClientKeyPair(authPrivateKey, authPublicCrt);
		return opcKeyUtils.getClientCertificate();
	}

	private PrivateKey getClientAuthPrivateKey() {
		final OpcKeyUtils opcKeyUtils = new OpcKeyUtils();
		String authPrivateKey = configuration.getAuthPrivateKey();
		authPrivateKey = convertFileToString(authPrivateKey);
		String authPublicCrt = configuration.getAuthPublicCrt();
		authPublicCrt = convertFileToString(authPublicCrt);
		opcKeyUtils.setClientKeyPair(authPrivateKey, authPublicCrt);
		return opcKeyUtils.getPrivateKey();
	}

	private OpcUaSession getClientSessionWithTimeout(OpcUaClient clientOpc)
			throws InterruptedException, ExecutionException, TimeoutException {
		return clientOpc.getSession().get(TIMEOUT_GET_SESSION_SEC, TimeUnit.SECONDS);
	}

	@Override
	public OpcUaClientConfig getConfig() {
		return client.getConfig();
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

	private OpcUaClientConfigBuilder getGeneralConfigBuilder() {
		final OpcUaClientConfigBuilder config = OpcUaClientConfig.builder().setEndpoint(getEndpoint())
				.setApplicationUri(configuration.getApplicationUri())
				.setApplicationName(LocalizedText.english(configuration.getApplicationName()));
		config.setSessionTimeout(UInteger.valueOf(configuration.getSessionTimeout()));
		config.setConnectTimeout(UInteger.valueOf(configuration.getConnectTimeout()));
		config.setKeepAliveTimeout(UInteger.valueOf(configuration.getKeepAliveTimeout()));
		config.setRequestTimeout(UInteger.valueOf(configuration.getRequestTimeout()));
		config.setChannelLifetime(UInteger.valueOf(configuration.getChannelLifetime()));
		config.setAcknowledgeTimeout(UInteger.valueOf(configuration.getAcknowledgeTimeout()));
		config.setIdentityProvider(getIdentityProviderActualConnection());
		config.setKeyPair(getKeyPairActualConnection());
		config.setCertificateChain(getCertificateChainActualConnection());
		config.setCertificate(getCertificateActualConnection());
		if (configuration.isForceCertificateValidator()) {
			final ClientCertificateValidator alwaysValidCert = new ClientCertificateValidator() {
				@Override
				public void validateCertificateChain(final List<X509Certificate> certificateChain) throws UaException {
					final StringBuilder sb = new StringBuilder();
					for (final X509Certificate c : certificateChain) {
						sb.append("\n" + c + "\n");
					}
					logger.warn(
							"because forceCertificateValidator is true, the function validateCertificateChain authorizes the follow certificates"
									+ sb.toString() + "\nApplicationUri is " + configuration.getApplicationUri()
									+ " and endpoint is " + getEndpoint().getEndpointUrl());
				}

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

	private KeyPair getKeyPair() {
		String cryptoPrivateKey = configuration.getCryptoPrivateKey();
		cryptoPrivateKey = convertFileToString(cryptoPrivateKey);
		String cryptoPublicCrt = configuration.getCryptoPublicCrt();
		cryptoPublicCrt = convertFileToString(cryptoPublicCrt);
		if (cryptoPrivateKey != null && cryptoPublicCrt != null) {
			final OpcKeyUtils opcKeyUtils = new OpcKeyUtils();
			opcKeyUtils.setClientKeyPair(cryptoPrivateKey, cryptoPublicCrt);
			return opcKeyUtils.getClientKeyPair();
		} else {
			return null;
		}
	}

	private KeyPair getKeyPairActualConnection() {
		// TODO Auto-generated method stub
		return null;
	}

	public ByteString getLastNonce() {
		return lastNonce;
	}

	@Override
	public CompletableFuture<? extends UaSession> getSession() {
		return client.getSession();
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public UaSubscriptionManager getSubscriptionManager() {
		return client.getSubscriptionManager();
	}

	@Override
	public CompletableFuture<HistoryReadResponse> historyRead(HistoryReadDetails historyReadDetails,
			TimestampsToReturn timestampsToReturn, boolean releaseContinuationPoints,
			List<HistoryReadValueId> nodesToRead) {
		return client.historyRead(historyReadDetails, timestampsToReturn, releaseContinuationPoints, nodesToRead);
	}

	@Override
	public CompletableFuture<HistoryUpdateResponse> historyUpdate(List<HistoryUpdateDetails> historyUpdateDetails) {
		return client.historyUpdate(historyUpdateDetails);
	}

	private void logServerCertificate(EndpointDescription endpoint) throws CertificateException {
		if (!endpoint.getServerCertificate().isNullOrEmpty()) {
			logger.info("Server certificate for endpoint {}: {}", endpoint, CertificateFactory.getInstance("X.509")
					.generateCertificate(new ByteArrayInputStream(endpoint.getServerCertificate().bytes())));
		}
	}

	@Override
	public CompletableFuture<ModifyMonitoredItemsResponse> modifyMonitoredItems(UInteger subscriptionId,
			TimestampsToReturn timestampsToReturn, List<MonitoredItemModifyRequest> itemsToModify) {
		return client.modifyMonitoredItems(subscriptionId, timestampsToReturn, itemsToModify);
	}

	@Override
	public CompletableFuture<ModifySubscriptionResponse> modifySubscription(UInteger subscriptionId,
			double requestedPublishingInterval, UInteger requestedLifetimeCount, UInteger requestedMaxKeepAliveCount,
			UInteger maxNotificationsPerPublish, UByte priority) {
		return client.modifySubscription(subscriptionId, requestedPublishingInterval, requestedLifetimeCount,
				requestedMaxKeepAliveCount, maxNotificationsPerPublish, priority);
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
		switch (status) {
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
			logger.error("Client is in faulted status, resetting connection");
			cleanConnectionObjects();
			activeConnectionRequest = true;
			nextConnectionAction();
			break;
		case INIT:
			logger.trace("Client is in INIT status, waiting for start");
			break;
		case STOPPED:
			logger.trace("Client is stopped");
			break;
		default:
			break;
		}
	}

	@Override
	public CompletableFuture<PublishResponse> publish(List<SubscriptionAcknowledgement> subscriptionAcknowledgements) {
		return client.publish(subscriptionAcknowledgements);
	}

	@Override
	public CompletableFuture<ReadResponse> read(double maxAge, TimestampsToReturn timestampsToReturn,
			List<ReadValueId> readValueIds) {
		return client.read(maxAge, timestampsToReturn, readValueIds);
	}

	private void refreshCertificateIfNeeded() {
		// TODO rigenerare il certificato se necessario
	}

	@Override
	public CompletableFuture<RegisterNodesResponse> registerNodes(List<NodeId> nodesToRegister) {
		return client.registerNodes(nodesToRegister);
	}

	@Override
	public CompletableFuture<RepublishResponse> republish(UInteger subscriptionId, UInteger retransmitSequenceNumber) {
		return client.republish(subscriptionId, retransmitSequenceNumber);
	}

	@Override
	public <T extends UaResponseMessage> CompletableFuture<T> sendRequest(UaRequestMessage request) {
		return client.sendRequest(request);
	}

	@Override
	public CompletableFuture<SetMonitoringModeResponse> setMonitoringMode(UInteger subscriptionId,
			MonitoringMode monitoringMode, List<UInteger> monitoredItemIds) {
		return client.setMonitoringMode(subscriptionId, monitoringMode, monitoredItemIds);
	}

	@Override
	public CompletableFuture<SetPublishingModeResponse> setPublishingMode(boolean publishingEnabled,
			List<UInteger> subscriptionIds) {
		return client.setPublishingMode(publishingEnabled, subscriptionIds);
	}

	@Override
	public void setStatusObserver(final WaldotAgentClientObserver waldotAgentClientObserver) {
		this.waldotAgentClientObserver = waldotAgentClientObserver;
	}

	@Override
	public CompletableFuture<SetTriggeringResponse> setTriggering(UInteger subscriptionId, UInteger triggeringItemId,
			List<UInteger> linksToAdd, List<UInteger> linksToRemove) {
		return client.setTriggering(subscriptionId, triggeringItemId, linksToAdd, linksToRemove);
	}

	@Override
	public CompletableFuture<TransferSubscriptionsResponse> transferSubscriptions(List<UInteger> subscriptionIds,
			boolean sendInitialValues) {
		return client.transferSubscriptions(subscriptionIds, sendInitialValues);
	}

	@Override
	public CompletableFuture<TranslateBrowsePathsToNodeIdsResponse> translateBrowsePaths(List<BrowsePath> browsePaths) {
		return client.translateBrowsePaths(browsePaths);
	}

	@Override
	public CompletableFuture<UnregisterNodesResponse> unregisterNodes(List<NodeId> nodesToUnregister) {
		return client.unregisterNodes(nodesToUnregister);
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

	@Override
	public CompletableFuture<WriteResponse> write(List<WriteValue> writeValues) {
		return client.write(writeValues);
	}

}
