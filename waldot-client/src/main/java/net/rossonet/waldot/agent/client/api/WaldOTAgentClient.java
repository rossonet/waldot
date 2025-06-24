package net.rossonet.waldot.agent.client.api;

import org.eclipse.milo.opcua.sdk.client.api.ServiceFaultListener;
import org.eclipse.milo.opcua.stack.core.UaException;

import net.rossonet.waldot.agent.client.v1.WaldOTAgentClientImplV1;

public interface WaldOTAgentClient extends ServiceFaultListener, AutoCloseable {

	public enum Status {
		INIT, CLOSED, FAULTED, STOPPED
	}

	public static int CONTROL_THREAD_PRIORITY = Thread.MAX_PRIORITY;

	public static WaldOTAgentClient withConfiguration(final WaldOTAgentClientConfiguration configuration) {
		return new WaldOTAgentClientImplV1(configuration);
	}

	WaldOTAgentClientConfiguration getConfiguration();

	Status getStatus();

	void setStatusObserver(WaldotAgentClientObserver waldotAgentClientObserver);

	void start() throws UaException;

	void stop();
}
