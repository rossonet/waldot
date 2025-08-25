package net.rossonet.waldot.agent.client.api;

import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;

import net.rossonet.waldot.agent.client.v1.WaldOTAgentClientImplV1;

public interface WaldOTAgentClient extends ServiceFaultListener, AutoCloseable {

	public enum Status {
		CLOSED, COMPLETED_PROVISIONING_MANUAL_REQUEST, COMPLETED_PROVISIONING_TOKEN, CONNECTED,
		CONNECTED_PROVISIONING_MANUAL_APPROVAL, CONNECTED_PROVISIONING_TOKEN, FAULTED, INIT, STARTING,
		STARTING_PROVISIONING_MANUAL_APPROVAL, STARTING_PROVISIONING_TOKEN, STOPPED,
		WAITING_PROVISIONING_MANUAL_APPROVAL
	}

	public static int CONTROL_THREAD_PRIORITY = Thread.MAX_PRIORITY;
	public static long CONTROL_THREAD_SLEEP_TIME_MSEC = 5000L;
	public static long MANUAL_APPROVAL_WAITING_TIME_SEC = 20;

	public static int TIMEOUT_GET_SESSION_SEC = 2;

	public static long TIMEOUT_PROVISIONING_ACTION_SEC = 120;

	public static WaldOTAgentClient withConfiguration(final WaldOTAgentClientConfiguration configuration) {
		return new WaldOTAgentClientImplV1(configuration);
	}

	void changeStatus(Status newStatus);

	WaldOTAgentClientConfiguration getConfiguration();

	OpcUaClient getOpcUaClient();

	Status getStatus();

	void setStatusObserver(WaldotAgentClientObserver waldotAgentClientObserver);

	CompletableFuture<WaldOTAgentClient> startConnectionProcedure();

	CompletableFuture<WaldOTAgentClient> stopConnectionProcedure();

}
