package net.rossonet.waldot.client.api.subscription;

import java.util.List;
import java.util.Set;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;

/**
 * Parameters of a monitored event source.
 *
 * @param fields         fields of BaseEventType to receive (browse names,
 *                       e.g. {@code Message}); empty for the default set
 * @param eventTypes     event types to deliver (filtered by the client),
 *                       empty for all
 * @param queueSize      size of the server queue
 * @param discardOldest  discard the oldest event when the queue is full
 * @param monitoringMode reporting, sampling or disabled
 */
public record EventItemOptions(List<String> fields, Set<NodeId> eventTypes, int queueSize, boolean discardOldest,
		MonitoringMode monitoringMode) {

	/** Default fields of an event. */
	public static final List<String> DEFAULT_FIELDS = List.of(WaldotEvent.EVENT_ID, WaldotEvent.EVENT_TYPE,
			WaldotEvent.SOURCE_NODE, WaldotEvent.SOURCE_NAME, WaldotEvent.TIME, WaldotEvent.RECEIVE_TIME,
			WaldotEvent.MESSAGE, WaldotEvent.SEVERITY);

	/** Default options: default fields, every type, queue 100. */
	public static EventItemOptions defaults() {
		return new EventItemOptions(DEFAULT_FIELDS, Set.of(), 100, true, MonitoringMode.Reporting);
	}

	public EventItemOptions {
		fields = fields == null || fields.isEmpty() ? DEFAULT_FIELDS : List.copyOf(fields);
		eventTypes = eventTypes == null ? Set.of() : Set.copyOf(eventTypes);
		if (queueSize < 1) {
			throw new IllegalArgumentException("queueSize must be >= 1");
		}
		monitoringMode = monitoringMode == null ? MonitoringMode.Reporting : monitoringMode;
	}

	/** Same options delivering only the given event types. */
	public EventItemOptions onlyTypes(final NodeId... types) {
		return new EventItemOptions(fields, Set.of(types), queueSize, discardOldest, monitoringMode);
	}

	/** Same options with other fields. */
	public EventItemOptions withFields(final String... names) {
		return new EventItemOptions(List.of(names), eventTypes, queueSize, discardOldest, monitoringMode);
	}
}
