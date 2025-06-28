package net.rossonet.waldot.agent.client.v1;

import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient;
import net.rossonet.waldot.agent.exception.ProvisioningException;

public class ProvisioningLifeCycleProcedure {
	private final static Logger logger = LoggerFactory.getLogger(ProvisioningLifeCycleProcedure.class);
	private final WaldOTAgentClient waldOTAgentClient;
	private final String requestUniqueCode = UUID.randomUUID().toString();
	private boolean manualRequestCompleted = false;

	public ProvisioningLifeCycleProcedure(final WaldOTAgentClient waldOTAgentClient) {
		this.waldOTAgentClient = waldOTAgentClient;
	}

	private void checkManualRequestCompleted() {
		// TODO Auto-generated method stub
		manualRequestCompleted = false;
	}

	public String getRequestUniqueCode() {
		return requestUniqueCode;
	}

	public boolean isManualRequestCompleted() {
		if (!manualRequestCompleted) {
			checkManualRequestCompleted();
		}
		return manualRequestCompleted;
	}

	public void requestManualApprovation() throws ProvisioningException {
		// TODO Auto-generated method stub
	}

	private CompletableFuture<Double> sqrt(final Double input) {
		final NodeId objectId = NodeId.parse("ns=2;s=HelloWorld");
		final NodeId methodId = NodeId.parse("ns=2;s=HelloWorld/sqrt(x)");

		final CallMethodRequest request = new CallMethodRequest(objectId, methodId,
				new Variant[] { new Variant(input) });

		return waldOTAgentClient.call(request).thenCompose(result -> {
			final StatusCode statusCode = result.getStatusCode();

			if (statusCode.isGood()) {
				final Double value = (Double) l(result.getOutputArguments()).get(0).getValue();
				return CompletableFuture.completedFuture(value);
			} else {
				final StatusCode[] inputArgumentResults = result.getInputArgumentResults();
				for (int i = 0; i < inputArgumentResults.length; i++) {
					logger.error("inputArgumentResults[{}]={}", i, inputArgumentResults[i]);
				}

				final CompletableFuture<Double> f = new CompletableFuture<>();
				f.completeExceptionally(new UaException(statusCode));
				return f;
			}
		});
	}

	public void tokenProvisioning() throws ProvisioningException {
		// TODO Auto-generated method stub

	}

}
