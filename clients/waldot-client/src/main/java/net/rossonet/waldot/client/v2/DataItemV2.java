package net.rossonet.waldot.client.v2;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.Optional;

import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.structured.DataChangeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.client.api.subscription.DataChangeListener;
import net.rossonet.waldot.client.api.subscription.DataItemOptions;
import net.rossonet.waldot.client.api.subscription.MonitoredDataItem;

/** A monitored value of the client v2. */
final class DataItemV2 extends AbstractMonitoredItemV2 implements MonitoredDataItem {

	private static final Logger logger = LoggerFactory.getLogger(DataItemV2.class);

	/** @return the data change filter of the options, null if not needed */
	static DataChangeFilter filter(final DataItemOptions options) {
		return options.needsFilter() ? new DataChangeFilter(options.trigger(),
				uint(options.deadbandType().getValue()), options.deadbandValue()) : null;
	}

	private static void apply(final OpcUaMonitoredItem item, final DataItemOptions options) {
		item.setSamplingInterval(options.samplingInterval());
		item.setQueueSize(uint(options.queueSize()));
		item.setDiscardOldest(options.discardOldest());
		item.setFilter(filter(options));
	}

	private volatile DataValue lastValue;
	private final DataChangeListener listener;
	private volatile DataItemOptions options;

	DataItemV2(final SubscriptionGroupV2 group, final NodeId nodeId, final DataItemOptions options,
			final DataChangeListener listener) {
		super(group, nodeId, options.monitoringMode());
		this.options = options;
		this.listener = listener;
	}

	@Override
	protected OpcUaMonitoredItem buildMonitoredItem() {
		final OpcUaMonitoredItem item = OpcUaMonitoredItem.newDataItem(nodeId(), requestedMode());
		apply(item, options);
		item.setDataValueListener((monitored, value) -> {
			lastValue = value;
			if (listener != null) {
				try {
					listener.onDataChange(this, value);
				} catch (final RuntimeException e) {
					logger.warn("data change listener error on {}: {}", nodeId(), e.getMessage());
				}
			}
		});
		return item;
	}

	@Override
	public Optional<DataValue> lastValue() {
		return Optional.ofNullable(lastValue);
	}

	@Override
	public void modify(final DataItemOptions newOptions) {
		synchronized (groupV2()) {
			final DataItemOptions previousOptions = options;
			final MonitoringMode previousMode = requestedMode();
			options = newOptions;
			requestedMode(newOptions.monitoringMode());
			final OpcUaMonitoredItem current = monitoredItem();
			if (current != null) {
				apply(current, newOptions);
				try {
					groupV2().modifyItem(this, previousMode);
				} catch (final RuntimeException e) {
					// the server refused the parameters: the previous ones stay active
					options = previousOptions;
					requestedMode(previousMode);
					apply(current, previousOptions);
					try {
						groupV2().modifyItem(this, previousMode);
					} catch (final RuntimeException rollback) {
						logger.warn("rollback of the parameters of {} failed: {}", nodeId(), rollback.getMessage());
					}
					throw e;
				}
			}
		}
	}

	@Override
	public DataItemOptions options() {
		return options;
	}
}
