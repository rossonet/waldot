package net.rossonet.waldot.client.v2;

import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;

import net.rossonet.waldot.client.api.subscription.MonitoredItemHandle;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroup;

/**
 * Handle of a monitored item of the client v2: it keeps the parameters and
 * the callback, the Milo item is created again at every bind of the group.
 */
abstract class AbstractMonitoredItemV2 implements MonitoredItemHandle {

	private volatile boolean active = true;
	private final SubscriptionGroupV2 group;
	private volatile OpcUaMonitoredItem monitoredItem;
	private final NodeId nodeId;
	private volatile MonitoringMode requestedMode;

	AbstractMonitoredItemV2(final SubscriptionGroupV2 group, final NodeId nodeId, final MonitoringMode mode) {
		this.group = group;
		this.nodeId = nodeId;
		this.requestedMode = mode == null ? MonitoringMode.Reporting : mode;
	}

	/** Builds the Milo item with the current parameters. */
	protected abstract OpcUaMonitoredItem buildMonitoredItem();

	@Override
	public void close() {
		if (active) {
			group.remove(this);
		}
		active = false;
	}

	/** @return true if the server refused the creation of the current Milo item */
	boolean creationFailed() {
		final OpcUaMonitoredItem current = monitoredItem;
		return current != null && current.getCreateResult().map(StatusCode::isBad).orElse(false);
	}

	/** Forgets the Milo item (the subscription was closed or reset). */
	void detach() {
		monitoredItem = null;
	}

	@Override
	public SubscriptionGroup group() {
		return group;
	}

	SubscriptionGroupV2 groupV2() {
		return group;
	}

	@Override
	public boolean isActive() {
		return active && group.isActive();
	}

	OpcUaMonitoredItem monitoredItem() {
		return monitoredItem;
	}

	@Override
	public MonitoringMode monitoringMode() {
		final OpcUaMonitoredItem current = monitoredItem;
		return current == null ? requestedMode : current.getMonitoringMode();
	}

	/** Creates a new Milo item (bind of the group). */
	OpcUaMonitoredItem newMonitoredItem() {
		final OpcUaMonitoredItem created = buildMonitoredItem();
		monitoredItem = created;
		return created;
	}

	@Override
	public NodeId nodeId() {
		return nodeId;
	}

	MonitoringMode requestedMode() {
		return requestedMode;
	}

	void requestedMode(final MonitoringMode mode) {
		requestedMode = mode;
	}

	@Override
	public Optional<UInteger> revisedQueueSize() {
		final OpcUaMonitoredItem current = monitoredItem;
		return current == null ? Optional.empty() : current.getRevisedQueueSize();
	}

	@Override
	public Optional<Double> revisedSamplingInterval() {
		final OpcUaMonitoredItem current = monitoredItem;
		return current == null ? Optional.empty() : current.getRevisedSamplingInterval();
	}

	@Override
	public void setMonitoringMode(final MonitoringMode mode) {
		group.setMonitoringMode(mode, List.of(this));
	}

	@Override
	public Optional<StatusCode> status() {
		final OpcUaMonitoredItem current = monitoredItem;
		if (current == null) {
			return Optional.empty();
		}
		final Optional<StatusCode> modified = current.getModifyResult();
		return modified.isPresent() ? modified : current.getCreateResult();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + nodeId.toParseableString() + ", " + monitoringMode() + "]";
	}
}
