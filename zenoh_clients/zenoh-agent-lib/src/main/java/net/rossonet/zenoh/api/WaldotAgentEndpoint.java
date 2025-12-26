package net.rossonet.zenoh.api;

import java.util.Map;

import io.zenoh.Session;
import io.zenoh.pubsub.CallbackSubscriber;
import io.zenoh.pubsub.Publisher;
import io.zenoh.sample.Sample;

public interface WaldotAgentEndpoint {

	void elaborateCommandReplyMessage(Sample sample);

	void elaborateConfigurationMessage(Sample sample);

	void elaborateInternalTelemetryMessage(Sample sample);

	void elaborateParameterMessage(Sample sample);

	void elaboratePongMessage(Sample sample);

	void elaborateTelemetryMessage(Sample sample);

	void elaborateUpdateDiscoveryMessage(Sample sample);

	Map<String, Publisher> getPublishers();

	Map<String, CallbackSubscriber> getSubcribers();

	String getUniqueId();

	Session getZenohClient();

}
