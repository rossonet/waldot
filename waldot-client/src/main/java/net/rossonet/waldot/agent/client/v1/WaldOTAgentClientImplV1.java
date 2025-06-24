package net.rossonet.waldot.agent.client.v1;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.agent.client.api.WaldOTAgentClient;
import net.rossonet.waldot.agent.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.agent.client.api.WaldotAgentClientObserver;
import net.rossonet.waldot.agent.exception.ProvisioningException;
import net.rossonet.waldot.utils.LogHelper;

public class WaldOTAgentClientImplV1 implements WaldOTAgentClient {
	private final class WaldOTAgentThread extends Thread {

		WaldOTAgentThread() {
			super();
			Thread.currentThread().setName("waldot_control");
			Thread.currentThread().setPriority(CONTROL_THREAD_PRIORITY);
		}

		@Override
		public void run() {
			while (!status.equals(Status.CLOSED)) {
				try {
					periodicalCheck();
				} catch (final Throwable t) {
					logger.warn("control thread error ", LogHelper.stackTraceToString(t, 5));
				}
			}
		}

	}

	private final static Logger logger = LoggerFactory.getLogger(WaldOTAgentClient.class);

	private final WaldOTAgentClientConfiguration configuration;
	private OpcUaClient client;
	private final ProvisioningLifeCycle provisioningLifeCycle = new ProvisioningLifeCycle(this);
	private Status status = Status.INIT;
	private final WaldOTAgentThread controlThread = new WaldOTAgentThread();

	private WaldotAgentClientObserver waldotAgentClientObserver;

	private boolean activeConnectionRequest;

	public WaldOTAgentClientImplV1(final WaldOTAgentClientConfiguration configuration) {
		this.configuration = configuration;
	}

	private void changeStatus(final Status newStatus) {
		status = newStatus;
		if (waldotAgentClientObserver != null) {
			waldotAgentClientObserver.onStatusChanged(newStatus);
		}
	}

	@Override
	public void close() throws Exception {
		stop();
		changeStatus(Status.CLOSED);
	}

	private OpcUaClient createOpcClient(final OpcUaClientConfig actualConfiguration) throws UaException {
		client = OpcUaClient.create(actualConfiguration);
		client.addFaultListener(this);
		client.connect();
		return client;
	}

	private OpcUaClientConfigBuilder getClientCertificateConfigBuider() {
		// TODO completare con i parametri
		final OpcUaClientConfigBuilder opcUaClientConfigBuilder = new OpcUaClientConfigBuilder();
		return opcUaClientConfigBuilder;
	}

	private OpcUaClientConfigBuilder getClientProvisioningManualApprovalConfigBuider() {
		// TODO Auto-generated method stub
		return null;
	}

	private OpcUaClientConfigBuilder getClientProvisioningTokenConfigBuider() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WaldOTAgentClientConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public Status getStatus() {
		return status;
	}

	@Override
	public void onServiceFault(final ServiceFault serviceFault) {
		// TODO Auto-generated method stub
		changeStatus(Status.FAULTED);
	}

	private void periodicalCheck() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStatusObserver(final WaldotAgentClientObserver waldotAgentClientObserver) {
		this.waldotAgentClientObserver = waldotAgentClientObserver;
	}

	@Override
	public void start() throws UaException {
		if (configuration.hasCertificateAuthentication()) {
			createOpcClient(getClientCertificateConfigBuider().build());
		} else if (configuration.hasProvisioningToken()) {
			createOpcClient(getClientProvisioningTokenConfigBuider().build());
			try {
				provisioningLifeCycle.tokenProvisioning();
			} catch (final ProvisioningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			createOpcClient(getClientProvisioningManualApprovalConfigBuider().build());
			try {
				provisioningLifeCycle.requestManualApprovation();
			} catch (final ProvisioningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		if (client != null) {
			client.disconnect();
		}
		activeConnectionRequest = false;
		changeStatus(Status.STOPPED);
	}

}
