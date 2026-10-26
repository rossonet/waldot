package net.rossonet.waldot.regression;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.List;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.identity.IdentityProvider;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;

import net.rossonet.waldot.test.WaldotTestEnvironment;

/**
 * Minimal Milo client used by the regression tests: it connects to the test
 * server on the unsecured endpoint with a chosen identity, so that the tests
 * exercise exactly the path of an external OPC UA client.
 */
final class RawOpcUaClient implements AutoCloseable {

	static String endpointUrl() {
		return "opc.tcp://localhost:" + WaldotTestEnvironment.opcTcpPort() + "/waldot";
	}

	static RawOpcUaClient connect(final IdentityProvider identityProvider) throws UaException {
		final OpcUaClient client = OpcUaClient.create(endpointUrl(),
				endpoints -> endpoints.stream()
						.filter(e -> SecurityPolicy.None.getUri().equals(e.getSecurityPolicyUri()))
						.filter(e -> e.getEndpointUrl().endsWith("/waldot"))
						.map(e -> EndpointUtil.updateUrl(e, "localhost")).findFirst(),
				transport -> {
				}, config -> config.setApplicationName(LocalizedText.english("waldot regression test"))
						.setApplicationUri("urn:waldot:regression:test").setRequestTimeout(uint(10_000))
						.setIdentityProvider(identityProvider));
		try {
			client.connect();
		} catch (final UaException e) {
			try {
				client.disconnect();
			} catch (final Exception ignored) {
				// nothing to do
			}
			throw e;
		}
		return new RawOpcUaClient(client);
	}

	private final OpcUaClient client;

	private RawOpcUaClient(final OpcUaClient client) {
		this.client = client;
	}

	CallMethodResult call(final NodeId objectId, final NodeId methodId, final Variant... inputs)
			throws UaException {
		return client.call(List.of(new CallMethodRequest(objectId, methodId, inputs))).getResults()[0];
	}

	@Override
	public void close() {
		try {
			client.disconnect();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
