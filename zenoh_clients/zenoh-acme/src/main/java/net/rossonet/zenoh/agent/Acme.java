package net.rossonet.zenoh.agent;

import java.util.UUID;

import io.zenoh.Config;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.zenoh.WaldotZenohException;
import net.rossonet.zenoh.annotation.AbstractAgentAnnotationControlHandler;
import net.rossonet.zenoh.client.WaldotZenohClientImpl;
import net.rossonet.zenoh.client.api.TelemetryUpdate;

public class Acme {

	private static final String NET_ROSSONET_ZENOH_ACME = "net.rossonet.zenoh.acme";

	public static void main(String[] args) throws WaldotZenohException {
		final String runtimeUniqueId = UUID.randomUUID().toString();
		final Acme acme = new Acme(runtimeUniqueId);
		acme.startAgent();
	}

	private final AbstractAgentAnnotationControlHandler controlHandler = new AbstractAgentAnnotationControlHandler(
			NET_ROSSONET_ZENOH_ACME) {

		@Override
		protected void elaborateErrorMessage(InternalLogMessage errorMessage) {
			System.err.println("ERROR: " + errorMessage.getMessage() + " stacktrace: "
					+ LogHelper.stackTraceToString(errorMessage.getException()));

		}

		@Override
		protected void elaborateInfoMessage(InternalLogMessage errorMessage) {
			System.out.println("INFO: " + errorMessage.getMessage());

		}

		@Override
		protected void elaborateTelemetryUpdate(TelemetryUpdate<?> telemetry) {
			System.out.println("Telemetry update: " + telemetry.toString());

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

		@Override
		protected void shutdown() {
			System.out.println("Shutting down Acme agent");
			System.exit(0);

		}

	};

	private final String runtimeUniqueId;

	public Acme(String runtimeUniqueId) {
		this.runtimeUniqueId = runtimeUniqueId;
	}

	public void startAgent() {
		try (WaldotZenohClientImpl client = new WaldotZenohClientImpl(runtimeUniqueId, controlHandler)) {
			final Config config = Config.loadDefault();
			client.setZenohConfig(config);
			client.start();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
