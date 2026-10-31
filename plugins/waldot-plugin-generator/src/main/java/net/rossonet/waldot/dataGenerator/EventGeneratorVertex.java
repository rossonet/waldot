package net.rossonet.waldot.dataGenerator;

import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_ALGORITHM;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_BURST_SIZE;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_CATEGORY;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_MAX_SEVERITY;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_MESSAGES;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_MIN_SEVERITY;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_RANDOM_MAX;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_SEPARATOR;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_TEMPLATE;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.DEFAULT_EVENT_TYPE;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_ALGORITHM_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_ALGORITHM_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_BURST_SIZE_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_BURST_SIZE_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_CATEGORY_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_CATEGORY_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_COUNT_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_COUNT_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_DATA_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_DELAY_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_DELAY_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_JITTER_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_JITTER_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_LAST_SEVERITY_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_LAST_SEVERITY_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MAX_EVENTS_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MAX_EVENTS_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MAX_SEVERITY_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MAX_SEVERITY_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MESSAGES_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MESSAGES_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MIN_SEVERITY_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_MIN_SEVERITY_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_RANDOM_MAX_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_RANDOM_MAX_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_RANDOM_MIN_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_RANDOM_MIN_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEED_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEED_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEPARATOR_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEPARATOR_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEVERITIES_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_SEVERITIES_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_TEMPLATE_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_TEMPLATE_FIELD;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_TYPE_DESCRIPTION;
import static net.rossonet.waldot.dataGenerator.GeneratorConstants.EVENT_TYPE_FIELD;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractOpcVertex;

/**
 * Vertex that simulates a source of OPC UA events (alarms, log lines, device
 * messages).
 * <p>
 * At every cycle the vertex builds one or more texts from a list of messages
 * and a template, and publishes each of them as an OPC UA event of the
 * configured type and severity. The events reach the vertices connected with
 * a <code>fire</code> edge (for example <code>syslog:sender</code> or
 * <code>snmp:trap-sender</code>). The text of the last event is also written
 * in the <code>data</code> property, so it can be copied to other vertices
 * with a <code>link-to</code> edge.
 * </p>
 * <p>
 * All the parameters are read again at every cycle: they can be changed at
 * runtime, for example by a rule or by an OPC UA client.
 * </p>
 *
 * @see GeneratorConstants
 */
public class EventGeneratorVertex extends AbstractOpcVertex
		implements
			AutoCloseable {

	/**
	 * Lock of the state of this vertex. Not the vertex monitor: Milo
	 * synchronizes the attribute access of the node on it, so blocking work or
	 * property reads under {@code synchronized (this)} stall the OPC UA reads
	 * and can make lock cycles with the namespace.
	 */
	private final Object vertexLock = new Object();

	/** Generation algorithms of the events. */
	public enum EventAlgorithm {
		burst, random, sequential, stopped
	}

	/** Values of the {@value GeneratorConstants#EVENT_TYPE_FIELD} parameter. */
	public static final Map<String, NodeId> EVENT_TYPES = Map.of("base",
			NodeIds.BaseEventType, "system", NodeIds.SystemEventType,
			"device-failure", NodeIds.DeviceFailureEventType, "audit",
			NodeIds.AuditEventType, "progress", NodeIds.ProgressEventType,
			"system-status", NodeIds.SystemStatusChangeEventType);

	private static final long MIN_DELAY = 10L;

	private static final Pattern PLACEHOLDER = Pattern
			.compile("\\$\\{([^}]+)\\}");

	/** Property key of the text of the last event. */
	public static final String VALUE_KEY = "data";

	/**
	 * Registers the parameters of the vertex type node.
	 */
	public static void generateParameters(WaldotNamespace waldotNamespace,
			UaObjectTypeNode typeNode) {
		param(waldotNamespace, typeNode, EVENT_ALGORITHM_FIELD, NodeIds.String,
				EVENT_ALGORITHM_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_DELAY_FIELD, NodeIds.Int64,
				EVENT_DELAY_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_JITTER_FIELD, NodeIds.Int64,
				EVENT_JITTER_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_MESSAGES_FIELD, NodeIds.String,
				EVENT_MESSAGES_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_SEPARATOR_FIELD, NodeIds.String,
				EVENT_SEPARATOR_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_TEMPLATE_FIELD, NodeIds.String,
				EVENT_TEMPLATE_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_CATEGORY_FIELD, NodeIds.String,
				EVENT_CATEGORY_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_TYPE_FIELD, NodeIds.String,
				EVENT_TYPE_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_MIN_SEVERITY_FIELD,
				NodeIds.Int64, EVENT_MIN_SEVERITY_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_MAX_SEVERITY_FIELD,
				NodeIds.Int64, EVENT_MAX_SEVERITY_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_SEVERITIES_FIELD, NodeIds.String,
				EVENT_SEVERITIES_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_BURST_SIZE_FIELD, NodeIds.Int64,
				EVENT_BURST_SIZE_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_MAX_EVENTS_FIELD, NodeIds.Int64,
				EVENT_MAX_EVENTS_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_RANDOM_MIN_FIELD, NodeIds.Int64,
				EVENT_RANDOM_MIN_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_RANDOM_MAX_FIELD, NodeIds.Int64,
				EVENT_RANDOM_MAX_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_SEED_FIELD, NodeIds.String,
				EVENT_SEED_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_COUNT_FIELD, NodeIds.Int64,
				EVENT_COUNT_DESCRIPTION);
		param(waldotNamespace, typeNode, EVENT_LAST_SEVERITY_FIELD,
				NodeIds.Int64, EVENT_LAST_SEVERITY_DESCRIPTION);
		param(waldotNamespace, typeNode, VALUE_KEY, NodeIds.String,
				EVENT_DATA_DESCRIPTION);
	}

	private static void param(WaldotNamespace namespace,
			UaObjectTypeNode typeNode, String label, NodeId type,
			String description) {
		PluginListener.addParameterToTypeNode(namespace, typeNode, label, type,
				description);
	}

	/**
	 * Splits a list with a literal separator, dropping the empty items.
	 */
	public static List<String> split(String text, String separator) {
		final List<String> result = new ArrayList<>();
		if (text == null || text.isEmpty()) {
			return result;
		}
		final String sep = separator == null || separator.isEmpty()
				? DEFAULT_EVENT_SEPARATOR
				: separator;
		int start = 0;
		while (true) {
			final int end = text.indexOf(sep, start);
			final String item = (end < 0
					? text.substring(start)
					: text.substring(start, end)).trim();
			if (!item.isEmpty()) {
				result.add(item);
			}
			if (end < 0) {
				break;
			}
			start = end + sep.length();
		}
		return result;
	}

	private volatile boolean active = true;

	private volatile boolean paused = false;

	private long counter;

	private long cursor;

	private String lastPublishError;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Random random = new Random();

	private final Thread runnerThread;

	private String seedSignature;

	public EventGeneratorVertex(WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName,
			LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version,
			Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description,
				writeMask, userWriteMask, eventNotifier, version);
		addString(EVENT_ALGORITHM_FIELD, DEFAULT_EVENT_ALGORITHM,
				propertyKeyValues);
		addLong(EVENT_DELAY_FIELD, GeneratorConstants.DEFAULT_DELAY,
				propertyKeyValues);
		addLong(EVENT_JITTER_FIELD, 0L, propertyKeyValues);
		addString(EVENT_MESSAGES_FIELD, DEFAULT_EVENT_MESSAGES,
				propertyKeyValues);
		addString(EVENT_SEPARATOR_FIELD, DEFAULT_EVENT_SEPARATOR,
				propertyKeyValues);
		addString(EVENT_TEMPLATE_FIELD, DEFAULT_EVENT_TEMPLATE,
				propertyKeyValues);
		addString(EVENT_CATEGORY_FIELD, DEFAULT_EVENT_CATEGORY,
				propertyKeyValues);
		addString(EVENT_TYPE_FIELD, DEFAULT_EVENT_TYPE, propertyKeyValues);
		addLong(EVENT_MIN_SEVERITY_FIELD, DEFAULT_EVENT_MIN_SEVERITY,
				propertyKeyValues);
		addLong(EVENT_MAX_SEVERITY_FIELD, DEFAULT_EVENT_MAX_SEVERITY,
				propertyKeyValues);
		addString(EVENT_SEVERITIES_FIELD, null, propertyKeyValues);
		addLong(EVENT_BURST_SIZE_FIELD, DEFAULT_EVENT_BURST_SIZE,
				propertyKeyValues);
		addLong(EVENT_MAX_EVENTS_FIELD, 0L, propertyKeyValues);
		addLong(EVENT_RANDOM_MIN_FIELD, 0L, propertyKeyValues);
		addLong(EVENT_RANDOM_MAX_FIELD, DEFAULT_EVENT_RANDOM_MAX,
				propertyKeyValues);
		addString(EVENT_SEED_FIELD, null, propertyKeyValues);
		addManagedProperty(EVENT_COUNT_FIELD, NodeIds.Int64, Long.class, false,
				0L, null, false, propertyKeyValues);
		addManagedProperty(EVENT_LAST_SEVERITY_FIELD, NodeIds.Int64, Long.class,
				false, 0L, null, false, propertyKeyValues);
		addManagedProperty(VALUE_KEY, NodeIds.String, String.class, false, "",
				null, false, propertyKeyValues);
		runnerThread = Thread.ofVirtual()
				.name("event-generator-" + getNodeId().toParseableString())
				.unstarted(this::cycle);
		runnerThread.start();
	}

	private void addLong(String label, long defaultValue, Object[] keyValues) {
		addManagedProperty(label, NodeIds.Int64, Long.class, false,
				defaultValue, null, false, keyValues);
	}

	private void addString(String label, String defaultValue,
			Object[] keyValues) {
		addManagedProperty(label, NodeIds.String, String.class, false,
				defaultValue, null, false, keyValues);
	}

	private EventAlgorithm algorithm() {
		final String text = stringValue(EVENT_ALGORITHM_FIELD,
				DEFAULT_EVENT_ALGORITHM).toLowerCase(Locale.ROOT);
		try {
			return EventAlgorithm.valueOf(text);
		} catch (final IllegalArgumentException e) {
			logger.warn("unknown algorithm '{}' in {}, no event generated",
					text, this);
			return EventAlgorithm.stopped;
		}
	}

	@Override
	public Object clone() {
		return new EventGeneratorVertex(graph, getNodeContext(), getNodeId(),
				getBrowseName(), getDisplayName(), getDescription(),
				getWriteMask(), getUserWriteMask(), getEventNotifier(),
				version(), getPropertiesAsStringArray());
	}

	@Override
	public void close() {
		active = false;
		runnerThread.interrupt();
	}

	/**
	 * Suspends the generation (the plugin was stopped): the cycle keeps
	 * running but publishes nothing until {@link #resumeGeneration()}.
	 */
	public void pauseGeneration() {
		paused = true;
	}

	/** Resumes the generation suspended by {@link #pauseGeneration()}. */
	public void resumeGeneration() {
		paused = false;
	}

	/**
	 * @return {@code true} while the generation thread is alive
	 */
	public boolean isGenerating() {
		return runnerThread.isAlive() && !paused;
	}

	/**
	 * @return {@code true} after {@link #close()}
	 */
	public boolean isClosed() {
		return !active;
	}

	private void cycle() {
		// the first cycle waits a delay: the edges that receive the events are
		// usually created just after the vertex
		while (active && !Thread.currentThread().isInterrupted()) {
			long wait = GeneratorConstants.DEFAULT_DELAY;
			try {
				wait = Math.max(MIN_DELAY, longValue(EVENT_DELAY_FIELD, wait));
				refreshSeed();
				final long jitter = Math.max(0,
						longValue(EVENT_JITTER_FIELD, 0));
				if (jitter > 0) {
					wait += nextLong(0, jitter);
				}
			} catch (final RuntimeException e) {
				logger.warn("Error reading the parameters of {}", this, e);
			}
			try {
				Thread.sleep(wait);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			try {
				if (active && !paused) {
					generateCycle();
				}
			} catch (final RuntimeException e) {
				logger.warn("Error generating the events of {}", this, e);
			}
		}
	}

	/**
	 * Runs one generation cycle with the current parameters.
	 *
	 * @return number of events published in the cycle
	 */
	public int generateCycle() {
		synchronized (vertexLock) {
			final EventAlgorithm algorithm = algorithm();
			final int events;
			switch (algorithm) {
				case burst :
					events = (int) Math.max(1,
							Math.min(10000, longValue(EVENT_BURST_SIZE_FIELD,
									DEFAULT_EVENT_BURST_SIZE)));
					break;
				case random :
				case sequential :
					events = 1;
					break;
				case stopped :
				default :
					return 0;
			}
			int published = 0;
			for (int i = 0; i < events; i++) {
				final long maxEvents = longValue(EVENT_MAX_EVENTS_FIELD, 0);
				if (maxEvents > 0 && counter >= maxEvents) {
					property(EVENT_ALGORITHM_FIELD, EventAlgorithm.stopped.name());
					break;
				}
				generateEvent(algorithm == EventAlgorithm.random);
				published++;
			}
			final long maxEvents = longValue(EVENT_MAX_EVENTS_FIELD, 0);
			if (maxEvents > 0 && counter >= maxEvents
					&& algorithm() != EventAlgorithm.stopped) {
				property(EVENT_ALGORITHM_FIELD, EventAlgorithm.stopped.name());
			}
			return published;
		}
	}

	private void generateEvent(boolean randomChoice) {
		final String separator = stringValue(EVENT_SEPARATOR_FIELD,
				DEFAULT_EVENT_SEPARATOR);
		List<String> messages = split(
				stringValue(EVENT_MESSAGES_FIELD, DEFAULT_EVENT_MESSAGES),
				separator);
		if (messages.isEmpty()) {
			messages = List.of(DEFAULT_EVENT_MESSAGES);
		}
		final int index = randomChoice
				? random.nextInt(messages.size())
				: (int) (cursor++ % messages.size());
		counter++;
		final int severity = severity(index, separator);
		final long now = System.currentTimeMillis();
		final String category = render(
				stringValue(EVENT_CATEGORY_FIELD, DEFAULT_EVENT_CATEGORY), "",
				index, severity, now, "");
		final String message = render(messages.get(index), "", index, severity,
				now, category);
		final String text = render(
				stringValue(EVENT_TEMPLATE_FIELD, DEFAULT_EVENT_TEMPLATE),
				message, index, severity, now, category);
		publish(category.isEmpty() ? DEFAULT_EVENT_CATEGORY : category,
				text.isEmpty() ? " " : text, severity, now);
		property(EVENT_COUNT_FIELD, counter);
		property(EVENT_LAST_SEVERITY_FIELD, (long) severity);
		property(VALUE_KEY, text);
	}

	/**
	 * @return number of events generated since the creation of the vertex
	 */
	public long getCounter() {
		return counter;
	}

	private long longValue(String key, long defaultValue) {
		final String text = stringValue(key, null);
		if (text == null) {
			return defaultValue;
		}
		try {
			return (long) Double.parseDouble(text);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	private long nextLong(long min, long max) {
		if (max <= min) {
			return min;
		}
		return min + (long) (random.nextDouble() * (max - min + 1));
	}

	@Override
	public void notifyRemoveVertex() {
		close();
	}

	private void publish(String category, String text, int severity,
			long time) {
		final UUID id = UUID.randomUUID();
		try {
			final BaseEventTypeNode event = getNamespace().getEventFactory()
					.createEvent(getNamespace().generateNodeId(id),
							NodeIds.BaseEventType);
			event.setBrowseName(getNamespace().generateQualifiedName(category));
			event.setDisplayName(LocalizedText.english(category));
			event.setEventId(ByteString.of(id.toString().getBytes()));
			event.setEventType(EVENT_TYPES.getOrDefault(
					stringValue(EVENT_TYPE_FIELD, DEFAULT_EVENT_TYPE)
							.toLowerCase(Locale.ROOT),
					NodeIds.SystemEventType));
			event.setSourceNode(getNodeId());
			event.setSourceName(getBrowseName().getName());
			event.setTime(new DateTime(Instant.ofEpochMilli(time)));
			event.setReceiveTime(DateTime.NULL_VALUE);
			event.setMessage(LocalizedText.english(text));
			event.setSeverity(ushort(severity));
			postEvent(event);
		} catch (final Exception e) {
			// the same error is logged once (for example when the server stops)
			final String signature = e.getClass().getName() + e.getMessage();
			if (!signature.equals(lastPublishError)) {
				lastPublishError = signature;
				logger.warn("Error publishing the event of {}: {}", this,
						e.getMessage());
			} else {
				logger.debug("Error publishing the event of {}", this, e);
			}
		}
	}

	private void refreshSeed() {
		final String seed = stringValue(EVENT_SEED_FIELD, "");
		if (!seed.equals(seedSignature)) {
			seedSignature = seed;
			if (seed.isEmpty()) {
				random = new Random();
			} else {
				long value;
				try {
					value = Long.parseLong(seed);
				} catch (final NumberFormatException e) {
					value = seed.hashCode();
				}
				random = new Random(value);
			}
		}
	}

	private String render(String template, String message, int index,
			int severity, long time, String category) {
		final Matcher matcher = PLACEHOLDER.matcher(template);
		final StringBuilder out = new StringBuilder();
		while (matcher.find()) {
			final String key = matcher.group(1);
			final String value;
			switch (key) {
				case "message" :
					value = message;
					break;
				case "counter" :
					value = String.valueOf(counter);
					break;
				case "index" :
					value = String.valueOf(index);
					break;
				case "time" :
					value = Instant.ofEpochMilli(time).toString();
					break;
				case "epoch" :
					value = String.valueOf(time);
					break;
				case "severity" :
					value = String.valueOf(severity);
					break;
				case "random" :
					value = String.valueOf(
							nextLong(longValue(EVENT_RANDOM_MIN_FIELD, 0),
									longValue(EVENT_RANDOM_MAX_FIELD,
											DEFAULT_EVENT_RANDOM_MAX)));
					break;
				case "source" :
					value = getBrowseName().getName();
					break;
				case "category" :
					value = category;
					break;
				default :
					if (key.startsWith("p:")) {
						value = stringValue(key.substring(2), "");
					} else {
						value = matcher.group(0);
					}
			}
			matcher.appendReplacement(out, Matcher.quoteReplacement(value));
		}
		matcher.appendTail(out);
		return out.toString();
	}

	/**
	 * Resets the counter and the position in the list of the messages.
	 */
	public void reset() {
		synchronized (vertexLock) {
			counter = 0;
			cursor = 0;
			property(EVENT_COUNT_FIELD, 0L);
		}
	}

	private int severity(int index, String separator) {
		final List<String> list = split(
				stringValue(EVENT_SEVERITIES_FIELD, null), separator);
		long value;
		if (!list.isEmpty()) {
			final String item = list.get(Math.min(index, list.size() - 1));
			try {
				value = (long) Double.parseDouble(item);
			} catch (final NumberFormatException e) {
				value = DEFAULT_EVENT_MIN_SEVERITY;
			}
		} else {
			final long min = longValue(EVENT_MIN_SEVERITY_FIELD,
					DEFAULT_EVENT_MIN_SEVERITY);
			final long max = longValue(EVENT_MAX_SEVERITY_FIELD,
					DEFAULT_EVENT_MAX_SEVERITY);
			value = nextLong(Math.min(min, max), Math.max(min, max));
		}
		return (int) Math.max(1, Math.min(1000, value));
	}

	private String stringValue(String key, String defaultValue) {
		final VertexProperty<Object> property = property(key);
		if (property == null || !property.isPresent()
				|| property.value() == null) {
			return defaultValue;
		}
		final String text = property.value().toString();
		return text.isEmpty() ? defaultValue : text;
	}
}
