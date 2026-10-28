package net.rossonet.waldot.client.api.subscription;

/**
 * Parameters of a subscription group (an OPC UA subscription).
 *
 * @param publishingInterval         requested publishing interval (ms)
 * @param maxKeepAliveCount          keep alive count (0 = computed by Milo)
 * @param lifetimeCount              lifetime count (0 = computed by Milo)
 * @param maxNotificationsPerPublish notifications per publish (0 = no limit)
 * @param priority                   relative priority (0-255)
 * @param publishingEnabled          publishing enabled at creation
 */
public record SubscriptionGroupOptions(double publishingInterval, int maxKeepAliveCount, int lifetimeCount,
		int maxNotificationsPerPublish, int priority, boolean publishingEnabled) {

	/** Default options with the given publishing interval. */
	public static SubscriptionGroupOptions withPublishingInterval(final double publishingInterval) {
		return new SubscriptionGroupOptions(publishingInterval, 0, 0, 0, 0, true);
	}

	public SubscriptionGroupOptions {
		if (publishingInterval < 0) {
			throw new IllegalArgumentException("publishingInterval must be >= 0");
		}
		if (priority < 0 || priority > 255) {
			throw new IllegalArgumentException("priority must be between 0 and 255");
		}
	}

	/** Same options with another priority. */
	public SubscriptionGroupOptions priority(final int newPriority) {
		return new SubscriptionGroupOptions(publishingInterval, maxKeepAliveCount, lifetimeCount,
				maxNotificationsPerPublish, newPriority, publishingEnabled);
	}

	/** Same options with another publishing interval. */
	public SubscriptionGroupOptions publishingInterval(final double newPublishingInterval) {
		return new SubscriptionGroupOptions(newPublishingInterval, maxKeepAliveCount, lifetimeCount,
				maxNotificationsPerPublish, priority, publishingEnabled);
	}

	/** Same options with another limit of notifications per publish. */
	public SubscriptionGroupOptions maxNotificationsPerPublish(final int newMaxNotificationsPerPublish) {
		return new SubscriptionGroupOptions(publishingInterval, maxKeepAliveCount, lifetimeCount,
				newMaxNotificationsPerPublish, priority, publishingEnabled);
	}
}
