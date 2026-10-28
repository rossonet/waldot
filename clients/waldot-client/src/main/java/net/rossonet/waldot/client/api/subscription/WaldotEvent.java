package net.rossonet.waldot.client.api.subscription;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * An event received from the server: the selected fields by browse name.
 *
 * @param emitter node monitored by the item
 * @param fields  field values by browse name
 */
public record WaldotEvent(NodeId emitter, Map<String, Object> fields) {

	public static final String EVENT_ID = "EventId";
	public static final String EVENT_TYPE = "EventType";
	public static final String MESSAGE = "Message";
	public static final String RECEIVE_TIME = "ReceiveTime";
	public static final String SEVERITY = "Severity";
	public static final String SOURCE_NAME = "SourceName";
	public static final String SOURCE_NODE = "SourceNode";
	public static final String TIME = "Time";

	public WaldotEvent {
		fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
	}

	public ByteString eventId() {
		return field(EVENT_ID) instanceof ByteString ? (ByteString) field(EVENT_ID) : null;
	}

	public NodeId eventType() {
		return field(EVENT_TYPE) instanceof NodeId ? (NodeId) field(EVENT_TYPE) : null;
	}

	/** @return a field value, null if not selected or null */
	public Object field(final String name) {
		return fields.get(name);
	}

	/** @return the message text */
	public String message() {
		final Object message = field(MESSAGE);
		return message instanceof LocalizedText ? ((LocalizedText) message).getText()
				: message == null ? null : message.toString();
	}

	/** @return the severity, -1 if not available */
	public int severity() {
		return field(SEVERITY) instanceof Number ? ((Number) field(SEVERITY)).intValue() : -1;
	}

	public String sourceName() {
		return field(SOURCE_NAME) == null ? null : field(SOURCE_NAME).toString();
	}

	public NodeId sourceNode() {
		return field(SOURCE_NODE) instanceof NodeId ? (NodeId) field(SOURCE_NODE) : null;
	}

	public DateTime time() {
		return field(TIME) instanceof DateTime ? (DateTime) field(TIME) : null;
	}
}
