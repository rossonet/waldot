package net.rossonet.waldot.regression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;

import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.auth.DefaultAnonymousValidator;
import net.rossonet.waldot.configuration.DefaultHomunculusConfiguration;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.opc.WaldotOpcUaServer;
import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Regression: {@code --anonymous-access false} was parsed but anonymous
 * sessions were still accepted (the endpoints always advertised the anonymous
 * token policy and the anonymous validators ignored the configuration).
 */
class AnonymousAccessRegressionTest {

	private static WaldotConfiguration configuration(final boolean anonymous) {
		final WaldotConfiguration configuration = DefaultHomunculusConfiguration.getDefault();
		((DefaultHomunculusConfiguration) configuration).setAnonymousAccessAllowed(anonymous);
		return configuration;
	}

	private static boolean hasAnonymousPolicy(final Iterable<UserTokenPolicy> policies) {
		for (final UserTokenPolicy policy : policies) {
			if (policy.getTokenType() == UserTokenType.Anonymous) {
				return true;
			}
		}
		return false;
	}

	private WaldotGraph graph;

	@BeforeEach
	void setUp() throws Exception {
		cleanup();
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
	}

	@AfterEach
	void tearDown() throws Exception {
		cleanup();
	}

	private void cleanup() throws Exception {
		if (graph != null) {
			graph.getWaldotNamespace().close();
			graph = null;
		}
		WaldotTestEnvironment.awaitOpcPortReleased();
	}

	@Test
	void endpointsAdvertiseAnonymousPolicyOnlyWhenAllowed() {
		assertTrue(hasAnonymousPolicy(WaldotOpcUaServer.userTokenPolicies(true)));
		assertFalse(hasAnonymousPolicy(WaldotOpcUaServer.userTokenPolicies(false)));
		// username and certificate stay available in both cases
		assertEquals(2, WaldotOpcUaServer.userTokenPolicies(false).size());
	}

	@Test
	void validatorAcceptsAnonymousTokenWhenAllowed() throws UaException {
		final DefaultAnonymousValidator validator = new DefaultAnonymousValidator(configuration(true));
		assertNotNull(validator.validateAnonymousToken(null, null, null, null));
	}

	@Test
	void validatorRejectsAnonymousTokenWhenDisabled() {
		final DefaultAnonymousValidator validator = new DefaultAnonymousValidator(configuration(false));
		final UaException e = assertThrows(UaException.class,
				() -> validator.validateAnonymousToken(null, null, null, null));
		assertEquals(StatusCodes.Bad_IdentityTokenRejected, e.getStatusCode().value());
	}

	@Test
	void serverRejectsAnonymousSessionWhenDisabled() throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(),
				OpcFactory.createMiloStrategy(), configuration(false));
		assertThrows(Exception.class, () -> {
			try (RawOpcUaClient client = RawOpcUaClient.connect(new AnonymousProvider())) {
				// connected: the regression is back
			}
		});
	}

	@Test
	void serverAcceptsAnonymousSessionByDefault() throws Exception {
		graph = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy(),
				OpcFactory.createMiloStrategy(), configuration(true));
		try (RawOpcUaClient client = RawOpcUaClient.connect(new AnonymousProvider())) {
			assertNotNull(client);
		}
	}
}
