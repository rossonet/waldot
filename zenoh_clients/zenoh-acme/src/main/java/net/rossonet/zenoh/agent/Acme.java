package net.rossonet.zenoh.agent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.zenoh.Config;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.zenoh.acme.AcmeController;
import net.rossonet.zenoh.annotation.AbstractAgentAnnotationControlHandler;
import net.rossonet.zenoh.api.InternalLogMessage;
import net.rossonet.zenoh.api.TelemetryData;
import net.rossonet.zenoh.api.WaldotZenohClient.Status;
import net.rossonet.zenoh.api.message.TelemetryMessage;
import net.rossonet.zenoh.client.WaldotZenohClientImpl;
import net.rossonet.zenoh.exception.WaldotZenohException;

public class Acme {

	private static final String NET_ROSSONET_ZENOH_ACME = "net.rossonet.zenoh.acme";

	public static void main(final String[] args) throws WaldotZenohException {
		final String runtimeUniqueId = UUID.randomUUID().toString();
		final Acme acme = new Acme(runtimeUniqueId);
		acme.startAgent();
	}

	private WaldotZenohClientImpl client;

	private final AbstractAgentAnnotationControlHandler controlHandler = new AbstractAgentAnnotationControlHandler(
			NET_ROSSONET_ZENOH_ACME) {

		@Override
		protected void doShutdown() {
			System.out.println("Shutting down Acme agent");
			System.exit(0);

		}

		@Override
		protected void elaborateErrorMessage(final InternalLogMessage errorMessage) {
			System.err.println("ERROR: " + errorMessage.getMessage() + " stacktrace: "
					+ (errorMessage.getException() != null ? (LogHelper.stackTraceToString(errorMessage.getException()))
							: ""));

		}

		@Override
		protected void elaborateInfoMessage(final InternalLogMessage errorMessage) {
			System.out.println("INFO: " + errorMessage.getMessage());

		}

		@Override
		protected void elaborateTelemetryUpdate(final TelemetryData node, final TelemetryMessage<?> telemetry) {
			System.out.println("Telemetry update for node " + node + " with value " + telemetry.getValue());

		}

		@Override
		protected String getAgentDescription() {
			return "Acme Agent for testing purposes";
		}

		@Override
		protected String getAgentDisplayName() {
			return "Acme Agent";
		}

		@Override
		protected String getDigitalTwinModelPath() {
			return "/acme/" + runtimeUniqueId;
		}

		@Override
		protected String getDigitalTwinModelVersion() {
			return "1";
		}

	};

	private final String runtimeUniqueId;

	public Acme(final String runtimeUniqueId) {
		this.runtimeUniqueId = runtimeUniqueId;
	}

	public String getTestData() {
		return ((AcmeController) controlHandler.getFlowController()).getData();
	}

	public boolean isRegistered() {
		return client.getStatus().equals(Status.RUNNING);
	}

	public void startAgent() {
		try {
			client = new WaldotZenohClientImpl(runtimeUniqueId, controlHandler);
			final Config config = Config.loadDefault();
			client.setZenohConfig(config);
			client.start();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() throws WaldotZenohException {
		if (client != null) {
			client.stop();
		}
	}

	public void waitForRegistration(final int delay, final TimeUnit seconds) throws Exception {
		client.waitRegistration(delay, seconds);

	}

}
