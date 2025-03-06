package net.rossonet.waldot.opc;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_X509;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.NonceUtil;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateGenerator;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedHttpsCertificateBuilder;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.eclipse.milo.opcua.stack.server.security.DefaultServerCertificateValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.ClassPath;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.auth.WaldotAnonymousValidator;
import net.rossonet.waldot.api.configuration.OpcConfiguration;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.utils.KeyStoreLoader;

public class WaldotOpcUaServer implements AutoCloseable {

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

	private final WaldotAnonymousValidator anonymousValidator;

	private OpcConfiguration configuration;

	private final UsernameIdentityValidator identityValidator;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private WaldotNamespace managerNamespace;
	private OpcUaServer server;
	private final X509IdentityValidator x509IdentityValidator;

	public WaldotOpcUaServer(WaldotConfiguration waldotConfiguration, OpcConfiguration serverConfiguration,
			WaldotAnonymousValidator anonymousValidator, UsernameIdentityValidator identityValidator,
			X509IdentityValidator x509IdentityValidator) {
		this.configuration = serverConfiguration;
		this.anonymousValidator = anonymousValidator;
		this.identityValidator = identityValidator;
		this.x509IdentityValidator = x509IdentityValidator;
		try {
			server = create(configuration, anonymousValidator, identityValidator, x509IdentityValidator);
		} catch (final Exception e) {
			logger.error("Error creating server", e);
		}
	}

	private EndpointConfiguration buildHttpsEndpoint(EndpointConfiguration.Builder base) {
		return base.copy().setTransportProfile(TransportProfile.HTTPS_UABINARY)
				.setBindPort(configuration.getHttpsBindPort()).build();
	}

	private EndpointConfiguration buildTcpEndpoint(EndpointConfiguration.Builder base) {
		return base.copy().setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
				.setBindPort(configuration.getTcpBindPort()).build();
	}

	@Override
	public void close() {
		if (server != null) {
			try {
				server.shutdown().get();
			} catch (final InterruptedException | ExecutionException e) {
				logger.error("Error shutting down server", e);
			}
		}
	}

	private OpcUaServer create(OpcConfiguration configuration, WaldotAnonymousValidator anonymousValidator,
			UsernameIdentityValidator identityValidator, X509IdentityValidator x509IdentityValidator) throws Exception {
		this.configuration = configuration;
		final Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "server", "security");
		Files.createDirectories(securityTempDir);
		if (!Files.exists(securityTempDir)) {
			throw new Exception("unable to create security temp dir: " + securityTempDir);
		}
		final File pkiDir = securityTempDir.resolve("pki").toFile();
		logger.info("security dir: {}", securityTempDir.toAbsolutePath());
		logger.info("security pki dir: {}", pkiDir.getAbsolutePath());
		final KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);
		final DefaultCertificateManager certificateManager = new DefaultCertificateManager(loader.getServerKeyPair(),
				loader.getServerCertificateChain());
		final DefaultTrustListManager trustListManager = new DefaultTrustListManager(pkiDir);
		final DefaultServerCertificateValidator certificateValidator = new DefaultServerCertificateValidator(
				trustListManager);
		final KeyPair httpsKeyPair = SelfSignedCertificateGenerator.generateRsaKeyPair(2048);
		final SelfSignedHttpsCertificateBuilder httpsCertificateBuilder = new SelfSignedHttpsCertificateBuilder(
				httpsKeyPair);
		httpsCertificateBuilder.setCommonName(HostnameUtil.getHostname());
		HostnameUtil.getHostnames(configuration.getDnsAddressCertificateGenerator())
				.forEach(httpsCertificateBuilder::addDnsName);
		httpsCertificateBuilder.addIpAddress("127.0.0.1");
		final X509Certificate httpsCertificate = httpsCertificateBuilder.build();
		// If you need to use multiple certificates you'll have to be smarter than this.
		final X509Certificate certificate = certificateManager.getCertificates().stream().findFirst()
				.orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError, "no certificate found"));

		// The configured application URI must match the one in the certificate(s)
		final String applicationUri = CertificateUtil.getSanUri(certificate)
				.orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError,
						"certificate is missing the application URI"));

		final Set<EndpointConfiguration> endpointConfigurations = createEndpointConfigurations(certificate);

		return generateServerInstance(configuration, certificateManager, trustListManager, certificateValidator,
				httpsKeyPair, httpsCertificate, applicationUri, endpointConfigurations, anonymousValidator,
				identityValidator, x509IdentityValidator);

	}

	private Set<EndpointConfiguration> createEndpointConfigurations(X509Certificate certificate) {
		final Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();
		final List<String> bindAddresses = newArrayList();
		bindAddresses.add(configuration.getBindAddresses());
		final Set<String> hostnames = new LinkedHashSet<>();
		hostnames.add(HostnameUtil.getHostname());
		hostnames.addAll(HostnameUtil.getHostnames(configuration.getBindHostname()));
		for (final String bindAddress : bindAddresses) {
			for (final String hostname : hostnames) {
				final EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder()
						.setBindAddress(bindAddress).setHostname(hostname).setPath(configuration.getPath())
						.setCertificate(certificate).addTokenPolicies(USER_TOKEN_POLICY_ANONYMOUS,
								USER_TOKEN_POLICY_USERNAME, USER_TOKEN_POLICY_X509);
				// TODO gestire la configurazione delle policy da configurazione opc
				final EndpointConfiguration.Builder noSecurityBuilder = builder.copy()
						.setSecurityPolicy(SecurityPolicy.None).setSecurityMode(MessageSecurityMode.None);
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
				final EndpointConfiguration.Builder discoveryBuilder = builder.copy()
						.setPath(configuration.getPath() + "/discovery").setSecurityPolicy(SecurityPolicy.None)
						.setSecurityMode(MessageSecurityMode.None);
				endpointConfigurations.add(buildTcpEndpoint(discoveryBuilder));
				endpointConfigurations.add(buildHttpsEndpoint(discoveryBuilder));
			}
		}

		return endpointConfigurations;
	}

	private OpcUaServer generateServerInstance(OpcConfiguration configuration,
			final DefaultCertificateManager certificateManager, final DefaultTrustListManager trustListManager,
			final DefaultServerCertificateValidator certificateValidator, final KeyPair httpsKeyPair,
			final X509Certificate httpsCertificate, final String applicationUri,
			final Set<EndpointConfiguration> endpointConfigurations, final IdentityValidator<String> anonymousValidator,
			final UsernameIdentityValidator identityValidator, final X509IdentityValidator x509IdentityValidator) {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final OpcUaServerConfig serverConfig = OpcUaServerConfig.builder().setApplicationUri(applicationUri)
				.setApplicationName(LocalizedText.english(configuration.getApplicationName()))
				.setEndpoints(endpointConfigurations)
				.setBuildInfo(new BuildInfo(configuration.getProductUri(), configuration.getManufacturerName(),
						configuration.getProductName(), OpcUaServer.SDK_VERSION, "", DateTime.now()))
				.setCertificateManager(certificateManager).setTrustListManager(trustListManager)
				.setCertificateValidator(certificateValidator).setHttpsKeyPair(httpsKeyPair)
				.setHttpsCertificateChain(new X509Certificate[] { httpsCertificate })
				.setIdentityValidator(
						new CompositeValidator(anonymousValidator, identityValidator, x509IdentityValidator))
				.setProductUri(configuration.getProductUri()).build();
		return new OpcUaServer(serverConfig);
	}

	public WaldotAnonymousValidator getAnonymousValidator() {
		return anonymousValidator;
	}

	private String[] getBootFromUrl(URL url) {
		// TODO completare con la capacità di leggere un file da URL tipo git:// http://
		// file://
		// etc...
		return new String[] {};
	}

	public OpcConfiguration getConfiguration() {
		return configuration;
	}

	public WaldotGraph getGremlinGraph() {
		return managerNamespace.getGremlinGraph();

	}

	public UsernameIdentityValidator getIdentityValidator() {
		return identityValidator;
	}

	public OpcUaServer getServer() {
		return server;
	}

	public X509IdentityValidator getX509IdentityValidator() {
		return x509IdentityValidator;
	}

	private void registerPluginsInNamespace() throws IOException {
		final ClassPath cp = ClassPath.from(Thread.currentThread().getContextClassLoader());
		for (final ClassPath.ClassInfo classInfo : cp.getTopLevelClassesRecursive("net.rossonet.waldot")) {
			final Class<?> clazz = classInfo.load();
			if (clazz.isAnnotationPresent(WaldotPlugin.class)) {
				logger.info("Found plugin: {}", clazz.getName());
				try {
					final PluginListener candidate = (PluginListener) clazz.getConstructor().newInstance();
					managerNamespace.registerPlugin(candidate);
				} catch (final Exception e) {
					logger.error("Error creating plugin", e);
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
	public Object runExpression(String query) {
		return managerNamespace.runExpression(query);
	}

	public CompletableFuture<OpcUaServer> startup(WaldotNamespace waldotNamespace) {
		try {
			managerNamespace = waldotNamespace;
			registerPluginsInNamespace();
			managerNamespace.startup();
			return server.startup();
		} catch (final Exception e) {
			logger.error("Error creating server", e);
			return CompletableFuture.failedFuture(e);
		}
	}

	public void waitCompletion() throws InterruptedException, ExecutionException {
		final CompletableFuture<Void> future = new CompletableFuture<>();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));
		future.get();
	}

}
