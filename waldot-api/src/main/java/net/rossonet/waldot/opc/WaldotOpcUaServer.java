package net.rossonet.waldot.opc;

import static org.eclipse.milo.opcua.sdk.server.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;
import static org.eclipse.milo.opcua.sdk.server.OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME;
import static org.eclipse.milo.opcua.sdk.server.OpcUaServerConfig.USER_TOKEN_POLICY_X509;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.EndpointConfig;
import org.eclipse.milo.opcua.sdk.server.EndpointConfig.Builder;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.OpcUaServerConfigBuilder;
import org.eclipse.milo.opcua.sdk.server.identity.AbstractIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.security.CertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultApplicationGroup;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultServerCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.FileBasedCertificateQuarantine;
import org.eclipse.milo.opcua.stack.core.security.FileBasedTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.KeyStoreCertificateStore;
import org.eclipse.milo.opcua.stack.core.security.RsaSha256CertificateFactory;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.NonceUtil;
import org.eclipse.milo.opcua.stack.transport.server.OpcServerTransport;
import org.eclipse.milo.opcua.stack.transport.server.OpcServerTransportFactory;
import org.eclipse.milo.opcua.stack.transport.server.tcp.OpcTcpServerTransport;
import org.eclipse.milo.opcua.stack.transport.server.tcp.OpcTcpServerTransportConfig;
import org.eclipse.milo.opcua.stack.transport.server.tcp.OpcTcpServerTransportConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.ClassPath;

import io.netty.channel.nio.NioEventLoopGroup;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.auth.FactoryPasswordValidator;
import net.rossonet.waldot.api.auth.WaldotAnonymousValidator;
import net.rossonet.waldot.api.configuration.OpcConfiguration;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.client.auth.ClientRegisterAnonymousValidator;
import net.rossonet.waldot.client.auth.ClientRegisterUsernameIdentityValidator;
import net.rossonet.waldot.client.auth.ClientRegisterX509IdentityValidator;
import net.rossonet.waldot.utils.KeyStoreLoader;
import net.rossonet.waldot.utils.ThreadHelper;

/**
 * WaldotOpcUaServer is the main class for the Waldot OPC UA server. It handles
 * the server configuration, endpoint creation, and plugin registration.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class WaldotOpcUaServer implements AutoCloseable {

	public static final String[] PLUGINS_BASE_SEARCH_PACKAGE = new String[] { "net.rossonet.waldot", "plugin",
			"plugins" };

	public static final String REGISTER_PATH = "/register";

	private static final boolean USE_VIRTUAL_THREADS = true;

	static {
		// Required for SecurityPolicy.Aes256_Sha256_RsaPss
		Security.addProvider(new BouncyCastleProvider());

		try {
			NonceUtil.blockUntilSecureRandomSeeded(10, TimeUnit.SECONDS);
		} catch (ExecutionException | InterruptedException | TimeoutException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private final ClientRegisterAnonymousValidator agentAnonymousValidator;

	private final ClientRegisterUsernameIdentityValidator agentIdentityValidator;

	private final ClientRegisterX509IdentityValidator agentX509IdentityValidator;

	private final AbstractIdentityValidator anonymousValidator;

	private OpcConfiguration configuration;

	private NioEventLoopGroup eventLoop;

	private ExecutorService executor;
	private final UsernameIdentityValidator identityValidator;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private WaldotNamespace managerNamespace;

	private ScheduledExecutorService scheduledExecutor;

	private OpcUaServer server;

	private final WaldotConfiguration waldotConfiguration;

	private final X509IdentityValidator x509IdentityValidator;

	public WaldotOpcUaServer(final WaldotConfiguration waldotConfiguration, final OpcConfiguration serverConfiguration,
			final WaldotAnonymousValidator anonymousValidator, final UsernameIdentityValidator identityValidator,
			final X509IdentityValidator x509IdentityValidator) {
		this.configuration = serverConfiguration;
		this.anonymousValidator = anonymousValidator;
		this.identityValidator = identityValidator;
		this.x509IdentityValidator = x509IdentityValidator;
		this.waldotConfiguration = waldotConfiguration;
		agentAnonymousValidator = new ClientRegisterAnonymousValidator(this);

		agentIdentityValidator = new ClientRegisterUsernameIdentityValidator(this, new FactoryPasswordValidator());

		agentX509IdentityValidator = new ClientRegisterX509IdentityValidator(this);
		try {
			server = create(configuration, anonymousValidator, identityValidator, x509IdentityValidator);
		} catch (final Exception e) {
			logger.error("Error creating server", e);
		}
	}

	/*
	 * private EndpointConfig buildHttpsEndpoint(EndpointConfig.Builder base) {
	 * return base.copy().setTransportProfile(TransportProfile.HTTPS_UABINARY)
	 * .setBindPort(configuration.getHttpsBindPort()).build(); }
	 */
	private EndpointConfig buildTcpEndpoint(EndpointConfig.Builder base) {
		return base.copy().setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
				.setBindPort(configuration.getTcpBindPort()).build();
	}

	@Override
	public void close() {
		logger.info("Shutting down OPCUA Server");
		if (server != null) {
			try {
				server.shutdown().get(10, TimeUnit.SECONDS);
				logger.info("OPCUA Server shutdown completed");
			} catch (final InterruptedException | ExecutionException | TimeoutException e) {
				logger.error("Timeout during OPCUA Server shutdown", e);
			}
		}
		if (eventLoop != null) {
			eventLoop.shutdownGracefully();
		}
		if (scheduledExecutor != null) {
			scheduledExecutor.shutdown();
		}
		if (executor != null) {
			executor.shutdown();
		}
		if (scheduledExecutor != null) {
			try {
				scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				logger.error("Error shutting down scheduled executor", e);
			}
		}
		if (executor != null) {
			try {
				executor.awaitTermination(10, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				logger.error("Error shutting down executor", e);
			}
		}
	}

	private OpcUaServer create(final OpcConfiguration configuration, final WaldotAnonymousValidator anonymousValidator,
			final UsernameIdentityValidator identityValidator, final X509IdentityValidator x509IdentityValidator)
			throws Exception {
		this.configuration = configuration;
		final Path securityTempDir = Paths.get(configuration.getSecurityTempDir());
		Files.createDirectories(securityTempDir);
		if (!Files.exists(securityTempDir)) {
			throw new Exception("unable to create security temp dir: " + securityTempDir);
		}
		final File pkiDir = securityTempDir.resolve("pki").toFile();
		final File issuerDir = securityTempDir.resolve("store").toFile();
		Files.createDirectories(issuerDir.toPath());
		logger.info("security dir: {}", securityTempDir.toAbsolutePath());
		logger.info("security pki dir: {}", pkiDir.getAbsolutePath());
		logger.info("security issuer dir: {}", issuerDir.getAbsolutePath());
		final KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

		final RsaSha256CertificateFactory certificateFactory = new RsaSha256CertificateFactory() {
			@Override
			protected X509Certificate[] createRsaSha256CertificateChain(KeyPair keyPair) {
				return loader.getServerCertificateChain();
			}

			@Override
			protected KeyPair createRsaSha256KeyPair() {
				return loader.getServerKeyPair();
			}
		};

		final X509Certificate certificate = loader.getServerCertificate();

		final KeyStoreCertificateStore certificateStore = KeyStoreCertificateStore
				.createAndInitialize(new KeyStoreCertificateStore.Settings(securityTempDir.resolve("pki"),
						"password"::toCharArray, alias -> "password".toCharArray()));

		final FileBasedTrustListManager trustListManager = FileBasedTrustListManager
				.createAndInitialize(issuerDir.toPath());

		final FileBasedCertificateQuarantine certificateQuarantine = FileBasedCertificateQuarantine
				.create(issuerDir.toPath().resolve("rejected").resolve("certs"));

		final DefaultServerCertificateValidator certificateValidator = new DefaultServerCertificateValidator(
				trustListManager, certificateQuarantine);

		final DefaultApplicationGroup defaultGroup = DefaultApplicationGroup.createAndInitialize(trustListManager,
				certificateStore, certificateFactory, certificateValidator);

		final DefaultCertificateManager certificateManager = new DefaultCertificateManager(certificateQuarantine,
				defaultGroup);

		// The configured application URI must match the one in the certificate(s)
		final String applicationUri = CertificateUtil.getSanUri(certificate)
				.orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError,
						"certificate is missing the application URI"));

		final Set<EndpointConfig> endpointConfigurations = createEndpointConfigurations(certificate);

		return generateServerInstance(configuration, certificateManager, applicationUri, endpointConfigurations,
				anonymousValidator, identityValidator, x509IdentityValidator);

	}

	private Set<EndpointConfig> createEndpointConfigurations(final X509Certificate certificate) {
		final Set<EndpointConfig> endpointConfigurations = new LinkedHashSet<>();
		final List<String> bindAddresses = new ArrayList<>();
		bindAddresses.add(configuration.getBindAddresses());
		final Set<String> hostnames = new LinkedHashSet<>();
		hostnames.add(HostnameUtil.getHostname());
		hostnames.addAll(HostnameUtil.getHostnames(configuration.getBindHostname()));
		for (final String bindAddress : bindAddresses) {
			for (final String hostname : hostnames) {
				final Builder builder = EndpointConfig.newBuilder().setBindAddress(bindAddress).setHostname(hostname)
						.setPath(configuration.getPath()).setCertificate(certificate).addTokenPolicies(
								USER_TOKEN_POLICY_ANONYMOUS, USER_TOKEN_POLICY_USERNAME, USER_TOKEN_POLICY_X509);
				// TODO gestire la configurazione delle policy da configurazione opc
				final Builder noSecurityBuilder = builder.copy().setSecurityPolicy(SecurityPolicy.None)
						.setSecurityMode(MessageSecurityMode.None);
				endpointConfigurations.add(buildTcpEndpoint(noSecurityBuilder));
				// endpointConfigurations.add(buildHttpsEndpoint(noSecurityBuilder));
				// TCP Basic256Sha256 / SignAndEncrypt
				endpointConfigurations
						.add(buildTcpEndpoint(builder.copy().setSecurityPolicy(SecurityPolicy.Basic256Sha256)
								.setSecurityMode(MessageSecurityMode.SignAndEncrypt)));
				// TODO gestire endpoint http e https da configurazione
				// HTTPS Basic256Sha256 / Sign (SignAndEncrypt not allowed for HTTPS)
				// endpointConfigurations.add(buildHttpsEndpoint(builder.copy()
				// .setSecurityPolicy(SecurityPolicy.Basic256Sha256).setSecurityMode(MessageSecurityMode.Sign)));
				/*
				 * It's good practice to provide a discovery-specific endpoint with no security.
				 * It's required practice if all regular endpoints have security configured.
				 *
				 * Usage of the "/discovery" suffix is defined by OPC UA Part 6:
				 *
				 * Each OPC UA Server Application implements the Discovery Service Set. If the
				 * OPC UA Server requires a different address for this Endpoint it shall create
				 * the address by appending the path "/discovery" to its base address.
				 */
				final Builder discoveryBuilder = builder.copy().setPath(configuration.getPath() + "/discovery")
						.setSecurityPolicy(SecurityPolicy.None).setSecurityMode(MessageSecurityMode.None);
				endpointConfigurations.add(buildTcpEndpoint(discoveryBuilder));
				// endpointConfigurations.add(buildHttpsEndpoint(discoveryBuilder));
				final Builder registerBuilder = builder.copy().setPath(configuration.getPath() + REGISTER_PATH)
						.setSecurityPolicy(SecurityPolicy.None).setSecurityMode(MessageSecurityMode.None);
				endpointConfigurations.add(buildTcpEndpoint(registerBuilder));
				// endpointConfigurations.add(buildHttpsEndpoint(registerBuilder));
			}
		}

		return endpointConfigurations;
	}

	private OpcUaServer generateServerInstance(final OpcConfiguration configuration,
			final CertificateManager certificateManager, final String applicationUri,
			final Set<EndpointConfig> endpointConfigurations, final IdentityValidator anonymousValidator,
			final UsernameIdentityValidator identityValidator, final X509IdentityValidator x509IdentityValidator) {
		final OpcUaServerConfigBuilder serverConfigBuilder = OpcUaServerConfig.builder()
				.setApplicationUri(applicationUri)
				.setApplicationName(LocalizedText.english(configuration.getApplicationName()))
				.setEndpoints(endpointConfigurations)
				.setBuildInfo(new BuildInfo(configuration.getProductUri(), configuration.getManufacturerName(),
						configuration.getProductName(), OpcUaServer.SDK_VERSION, configuration.getBuildNumber(),
						configuration.getBuildDate()))
				.setCertificateManager(certificateManager)

				.setIdentityValidator(new CompositeValidator(agentAnonymousValidator, agentIdentityValidator,
						agentX509IdentityValidator, anonymousValidator, identityValidator, x509IdentityValidator))
				.setProductUri(configuration.getProductUri());
		if (USE_VIRTUAL_THREADS) {
			if (executor == null) {
				executor = ThreadHelper.newVirtualThreadExecutor();
			}
			serverConfigBuilder.setExecutor(executor);
			if (scheduledExecutor == null) {
				scheduledExecutor = ThreadHelper.newVirtualSchedulerExecutor("vsched-opcua-");
			}
			serverConfigBuilder.setScheduledExecutor(scheduledExecutor);
		}
		final OpcServerTransportFactory transportFactory = new OpcServerTransportFactory() {

			@Override
			public OpcServerTransport create(TransportProfile transportProfile) {
				assert transportProfile == TransportProfile.TCP_UASC_UABINARY;
				final OpcTcpServerTransportConfigBuilder transportConfigBuilder = OpcTcpServerTransportConfig
						.newBuilder();
				if (USE_VIRTUAL_THREADS) {
					if (executor == null) {
						executor = ThreadHelper.newVirtualThreadExecutor();
					}
					transportConfigBuilder.setExecutor(executor);
					if (eventLoop == null) {
						eventLoop = ThreadHelper.newVirtualEventLoopGroup("netty-evtloop-");
					}
					transportConfigBuilder.setEventLoop(eventLoop);
				}
				return new OpcTcpServerTransport(transportConfigBuilder.build());
			}

		};
		final OpcUaServer opcUaServer = new OpcUaServer(serverConfigBuilder.build(), transportFactory);
		return opcUaServer;
	}

	public IdentityValidator getAnonymousValidator() {
		return anonymousValidator;
	}

	public OpcConfiguration getConfiguration() {
		return configuration;
	}

	public WaldotGraph getGremlinGraph() {
		return getManagerNamespace().getGremlinGraph();

	}

	public UsernameIdentityValidator getIdentityValidator() {
		return identityValidator;
	}

	public WaldotNamespace getManagerNamespace() {
		if (managerNamespace == null) {
			throw new RuntimeException("WaldotNamespace not initialized");
		}
		return managerNamespace;
	}

	public OpcUaServer getServer() {
		return server;
	}

	public WaldotConfiguration getWaldotConfiguration() {
		return waldotConfiguration;
	}

	public X509IdentityValidator getX509IdentityValidator() {
		return x509IdentityValidator;
	}

	private void registerPluginsInNamespace() throws IOException {
		final ClassPath cp = ClassPath.from(Thread.currentThread().getContextClassLoader());
		for (final String basePackage : PLUGINS_BASE_SEARCH_PACKAGE) {
			logger.info("Searching plugins in base package: {}", basePackage);
			for (final ClassPath.ClassInfo classInfo : cp.getTopLevelClassesRecursive(basePackage)) {
				final Class<?> clazz = classInfo.load();
				if (clazz.isAnnotationPresent(WaldotPlugin.class)) {
					logger.info("Found plugin: {}", clazz.getName());
					try {
						final PluginListener candidate = (PluginListener) clazz.getConstructor().newInstance();
						getManagerNamespace().registerPlugin(candidate);
					} catch (final Exception e) {
						logger.error("Error creating plugin", e);
					}

				}
			}
		}
	}

	/**
	 * Comandi per l'interazione con il server OPC UA
	 * 
	 * @param query
	 * @return
	 */
	public Object runExpression(final String query) {
		return getManagerNamespace().runExpression(query);
	}

	public CompletableFuture<OpcUaServer> startup(final WaldotNamespace waldotNamespace) {
		try {
			managerNamespace = waldotNamespace;
			registerPluginsInNamespace();
			managerNamespace.registerAgentValidators(agentAnonymousValidator, agentIdentityValidator,
					agentX509IdentityValidator);
			managerNamespace.startup();
			final CompletableFuture<OpcUaServer> future = server.startup();
			Thread.sleep(2_000);
			return future;
		} catch (final Exception e) {
			logger.error("Error creating server", e);
			return CompletableFuture.failedFuture(e);
		}
	}

	public void updateReferenceTypeTree() {
		server.updateReferenceTypeTree();

	}

	public void waitCompletion() throws InterruptedException, ExecutionException {
		final CompletableFuture<Void> future = new CompletableFuture<>();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));
		future.get();
	}

}
