package net.rossonet.waldot.zenoh;

import io.zenoh.handlers.Callback;
import io.zenoh.sample.Sample;

public interface ZenohSubscriberVertex extends Callback<Sample> {

	String getSubscriptionTopic();

}
