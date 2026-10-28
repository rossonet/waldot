package net.rossonet.waldot.client.v2;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.SimpleAttributeOperand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.client.api.subscription.EventItemOptions;
import net.rossonet.waldot.client.api.subscription.MonitoredEventItem;
import net.rossonet.waldot.client.api.subscription.WaldotEvent;
import net.rossonet.waldot.client.api.subscription.WaldotEventListener;

/** A monitored event source of the client v2. */
final class EventItemV2 extends AbstractMonitoredItemV2 implements MonitoredEventItem {

	private static final Logger logger = LoggerFactory.getLogger(EventItemV2.class);

	/** @return the event filter selecting the fields of the options */
	static EventFilter filter(final EventItemOptions options) {
		final List<String> fields = options.fields();
		final SimpleAttributeOperand[] select = new SimpleAttributeOperand[fields.size()];
		for (int i = 0; i < fields.size(); i++) {
			select[i] = new SimpleAttributeOperand(NodeIds.BaseEventType,
					new QualifiedName[] { new QualifiedName(0, fields.get(i)) }, AttributeId.Value.uid(), null);
		}
		return new EventFilter(select, new ContentFilter(null));
	}

	/**
	 * @return the event built from the received values, null if its type is
	 *         filtered out
	 */
	static WaldotEvent toEvent(final NodeId emitter, final EventItemOptions options, final Variant[] values) {
		final Map<String, Object> fields = new LinkedHashMap<>();
		final List<String> names = options.fields();
		for (int i = 0; i < names.size(); i++) {
			fields.put(names.get(i), values != null && i < values.length && values[i] != null ? values[i].getValue()
					: null);
		}
		final WaldotEvent event = new WaldotEvent(emitter, fields);
		if (!options.eventTypes().isEmpty()
				&& (event.eventType() == null || !options.eventTypes().contains(event.eventType()))) {
			return null;
		}
		return event;
	}

	private final WaldotEventListener listener;
	private volatile EventItemOptions options;

	EventItemV2(final SubscriptionGroupV2 group, final NodeId nodeId, final EventItemOptions options,
			final WaldotEventListener listener) {
		super(group, nodeId, options.monitoringMode());
		this.options = options;
		this.listener = listener;
	}

	@Override
	protected OpcUaMonitoredItem buildMonitoredItem() {
		final OpcUaMonitoredItem item = OpcUaMonitoredItem.newEventItem(nodeId(), requestedMode(), filter(options));
		item.setQueueSize(uint(options.queueSize()));
		item.setDiscardOldest(options.discardOldest());
		item.setEventValueListener((monitored, values) -> {
			final WaldotEvent event = toEvent(nodeId(), options, values);
			if (event != null && listener != null) {
				try {
					listener.onEvent(this, event);
				} catch (final RuntimeException e) {
					logger.warn("event listener error on {}: {}", nodeId(), e.getMessage());
				}
			}
		});
		return item;
	}

	private static void apply(final OpcUaMonitoredItem item, final EventItemOptions options) {
		item.setQueueSize(uint(options.queueSize()));
		item.setDiscardOldest(options.discardOldest());
		item.setFilter(filter(options));
	}

	@Override
	public void modify(final EventItemOptions newOptions) {
		synchronized (groupV2()) {
			final EventItemOptions previousOptions = options;
			final MonitoringMode previousMode = requestedMode();
			options = newOptions;
			requestedMode(newOptions.monitoringMode());
			final OpcUaMonitoredItem current = monitoredItem();
			if (current != null) {
				apply(current, newOptions);
				try {
					groupV2().modifyItem(this, previousMode);
				} catch (final RuntimeException e) {
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
	public EventItemOptions options() {
		return options;
	}
}
