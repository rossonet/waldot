package net.rossonet.waldot.cli.commands;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.DataChangeTrigger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.DeadbandType;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.cli.support.CliException;
import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Durations;
import net.rossonet.waldot.cli.support.ExitCodes;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.OutputClosedException;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldOTAgentClient.Status;
import net.rossonet.waldot.client.api.WaldOTAgentClientConfiguration;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.WaldotClientListener;
import net.rossonet.waldot.client.api.subscription.DataItemOptions;
import net.rossonet.waldot.client.api.subscription.EventItemOptions;
import net.rossonet.waldot.client.api.subscription.MonitoredItemHandle;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroup;
import net.rossonet.waldot.client.api.subscription.SubscriptionGroupOptions;
import net.rossonet.waldot.client.api.subscription.WaldotEvent;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * {@code waldot subscribe}: streams values and events on stdout.
 */
@Command(name = "subscribe", header = "Sottoscrive valori ed eventi e li scrive su stdout.", mixinStandardHelpOptions = true, description = {
		"Sottoscrive proprietà dei vertici, variabili OPC UA ed eventi e scrive su stdout",
		"ogni notifica appena arriva, una riga per notifica con flush immediato. Il comando",
		"resta in esecuzione finché non si raggiunge --count o --duration, finché non si",
		"preme Ctrl-C (codice 130) o finché il processo a valle non chiude la pipe (codice 0).",
		"",
		"Cosa sottoscrivere (almeno uno, tutte le opzioni sono ripetibili):",
		"  -p VERTICE:CHIAVE  una proprietà di un vertice o di un arco (la variabile OPC UA",
		"                     <vertice>/<chiave>); il separatore è l'ultimo ':';",
		"  -n NODO            una variabile OPC UA qualunque, indicata con il NodeId;",
		"  -e VERTICE         gli eventi emessi da un vertice (o da un nodo qualunque).",
		"",
		"Subito dopo la sottoscrizione il server invia il valore corrente di ogni variabile,",
		"poi solo le variazioni secondo --trigger e l'eventuale deadband. Se la connessione",
		"cade il client si riconnette da solo (vedi --reconnect-*) e ricrea le",
		"sottoscrizioni; se rinuncia il comando termina con codice 3." }, footer = {
				"Formati di output:",
				"  jsonl (o json): un oggetto JSON per notifica, per esempio",
				"    {\"type\":\"data\",\"vertex\":\"s1\",\"key\":\"value\",\"nodeId\":\"ns=2;s=s1/value\",",
				"     \"value\":21.5,\"status\":\"Good\",\"sourceTime\":\"2026-10-28T10:00:00Z\",...}",
				"    {\"type\":\"event\",\"vertex\":\"s1\",\"nodeId\":\"ns=2;s=s1\",\"fields\":{\"Message\":...}}",
				"  value: solo il valore (per gli eventi il messaggio), adatto a mosquitto_pub -l;",
				"  topic: '<prefisso>/<vertice>/<chiave> <valore>' per le proprietà,",
				"         '<prefisso>/<identificatore> <valore>' per i nodi (--node),",
				"         '<prefisso>/<vertice>/events <evento JSON>' per gli eventi;",
				"         il prefisso si sceglie con --topic-prefix (vuoto = nessun prefisso);",
				"         spazi, '+' e '#' nei nomi diventano '_' (topic MQTT validi);",
				"  text : '<ora> <vertice>/<chiave> = <valore>' leggibile.",
				"  I valori con a capo sono scritti come stringhe JSON, così ogni notifica resta",
				"  su una sola riga.",
				"",
				"Pubblicazione su MQTT:",
				"  mosquitto_pub -l legge stdin e pubblica una riga per messaggio sullo stesso",
				"  topic; per un topic diverso per ogni riga si usa il formato topic con un ciclo",
				"  'while read'. (mosquitto_sub serve invece a ricevere, non a pubblicare.)",
				"",
				"Esempi:",
				"  waldot subscribe -p sensor-1:value",
				"  waldot subscribe -p sensor-1:value -o value | mosquitto_pub -t waldot/sensor-1 -l",
				"  waldot subscribe -p s1:value -p s2:value -e s1 -o topic --topic-prefix waldot \\",
				"    | while read -r t p; do mosquitto_pub -t \"$t\" -m \"$p\"; done",
				"  waldot subscribe -p pump-3:speed --sampling 100 --publishing 500 --queue 20 -o jsonl",
				"  waldot subscribe -n 'ns=0;i=2258' --count 3 -o value        # 3 letture dell'ora",
				"  waldot subscribe -e plant-1 --event-fields Message,Severity -o jsonl | jq .fields",
				"  waldot subscribe -p s1:value --duration 1h -o jsonl >> registro.jsonl",
				"  waldot subscribe -p s1:value -o value | head -n 1        # primo valore e uscita",
				"  docker run --rm -e WALDOT_ENDPOINT=opc.tcp://server:12686/waldot \\",
				"    rossonet/waldot-client subscribe -p s1:value" })
public class SubscribeCommand extends ClientCommand {

	/** A subscribed source. */
	record Source(String vertex, String key, NodeId nodeId, boolean event, String topic) {

		String label() {
			if (vertex != null && key != null) {
				return vertex + "/" + key;
			}
			return vertex != null ? vertex : Json.id(nodeId);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(SubscribeCommand.class);

	/** Makes a valid MQTT topic level (no spaces, wildcards). */
	public static String topicLevel(final String text) {
		return text == null ? "_" : text.replaceAll("[\\s+#]", "_");
	}

	@Option(names = { "-p", "--property" }, paramLabel = "VERTICE:CHIAVE", description = "Proprietà di un vertice o di un arco da sottoscrivere (ripetibile).")
	private List<String> properties = new ArrayList<>();

	@Option(names = { "-n", "--node" }, paramLabel = "NODO", description = "Variabile OPC UA da sottoscrivere, per NodeId (ripetibile).")
	private List<String> nodes = new ArrayList<>();

	@Option(names = { "-e", "--events" }, paramLabel = "VERTICE", description = "Vertice o nodo di cui ricevere gli eventi (ripetibile).")
	private List<String> eventSources = new ArrayList<>();

	@Option(names = "--sampling", paramLabel = "MS", description = {
			"Intervallo di campionamento richiesto in millisecondi (-1 = uguale al",
			"publishing; predefinito: quello della configurazione del client)." })
	private Double sampling;

	@Option(names = "--publishing", paramLabel = "MS", description = {
			"Intervallo di pubblicazione della sottoscrizione OPC UA in millisecondi",
			"(predefinito: quello della configurazione del client)." })
	private Double publishing;

	@Option(names = "--queue", paramLabel = "N", description = "Dimensione della coda sul server (predefinita 10 per i valori, 100 per gli eventi).")
	private Integer queue;

	@Option(names = "--discard-oldest", negatable = true, defaultValue = "true", fallbackValue = "true", description = {
			"Con la coda piena scarta la notifica più vecchia (predefinito);",
			"--no-discard-oldest scarta invece la più recente." })
	private boolean discardOldest = true;

	@Option(names = "--trigger", paramLabel = "TRIGGER", description = {
			"Cosa genera una notifica: Status (solo lo stato), StatusValue (stato o valore,",
			"predefinito), StatusValueTimestamp (anche il solo timestamp)." })
	private DataChangeTrigger trigger = DataChangeTrigger.StatusValue;

	@Option(names = "--deadband-absolute", paramLabel = "DELTA", description = {
			"Notifica solo le variazioni maggiori di DELTA (variabili numeriche). Le proprietà",
			"di WaldOT sono variabili BaseDataType e il server può rifiutare il filtro",
			"(Bad_FilterNotAllowed, codice di uscita 1)." })
	private Double deadbandAbsolute;

	@Option(names = "--deadband-percent", paramLabel = "PERCENTO", description = "Notifica solo le variazioni maggiori della percentuale dell'intervallo EURange (solo AnalogItem).")
	private Double deadbandPercent;

	@Option(names = "--event-fields", split = ",", paramLabel = "CAMPO", description = {
			"Campi degli eventi da ricevere, separati da virgole (browse name di",
			"BaseEventType, es. Message,Severity,Time). Predefiniti: EventId, EventType,",
			"SourceNode, SourceName, Time, ReceiveTime, Message, Severity." })
	private List<String> eventFields = new ArrayList<>();

	@Option(names = "--event-type", paramLabel = "NODO", description = "Consegna solo gli eventi di questo tipo (NodeId, ripetibile).")
	private List<String> eventTypes = new ArrayList<>();

	@Option(names = { "-c", "--count" }, paramLabel = "N", description = "Termina dopo N notifiche (valori ed eventi insieme).")
	private Integer count;

	@Option(names = { "-d", "--duration" }, paramLabel = "DURATA", converter = Durations.class, description = "Termina dopo questo tempo (es. 30s, 5m, 1h).")
	private Duration duration;

	@Option(names = "--topic-prefix", paramLabel = "PREFISSO", description = "Prefisso dei topic del formato topic (predefinito ${DEFAULT-VALUE}).")
	private String topicPrefix = "waldot";

	private double defaultSampling;
	private final CountDownLatch done = new CountDownLatch(1);
	private final AtomicInteger exitCode = new AtomicInteger(ExitCodes.OK);
	private volatile String failure;
	private final Object lock = new Object();
	private int received;
	private boolean stopped;

	@Override
	protected void configure(final WaldOTAgentClientConfiguration configuration) {
		defaultSampling = configuration.getDefaultSamplingInterval();
	}

	private void deliver(final Printer printer, final String line) {
		synchronized (lock) {
			if (stopped) {
				return;
			}
			try {
				printer.line(line);
			} catch (final OutputClosedException e) {
				logger.info("standard output closed, stopping");
				stop(ExitCodes.OK, null);
				return;
			}
			received++;
			if (count != null && received >= count) {
				stop(ExitCodes.OK, null);
			}
		}
	}

	private String dataLine(final Printer printer, final Source source, final DataValue value) {
		return switch (printer.format()) {
		case json, jsonl -> {
			final Map<String, Object> json = new LinkedHashMap<>();
			json.put("type", "data");
			json.put("vertex", source.vertex());
			json.put("key", source.key());
			json.put("nodeId", source.nodeId());
			json.put("value", value.getValue());
			json.put("status", value.getStatusCode());
			json.put("sourceTime", value.getSourceTime());
			json.put("serverTime", value.getServerTime());
			yield Json.compact(Json.toNode(json));
		}
		case value -> Json.scalarLine(value.getValue());
		case topic -> source.topic() + " " + Json.scalarLine(value.getValue());
		default -> {
			final StatusCode status = value.getStatusCode();
			yield time(value.getSourceTime()) + " " + source.label() + " = " + Json.scalarLine(value.getValue())
					+ (status == null || status.isGood() ? "" : "   (" + Json.statusName(status) + ")");
		}
		};
	}

	private String eventLine(final Printer printer, final Source source, final WaldotEvent event) {
		return switch (printer.format()) {
		case json, jsonl -> {
			final Map<String, Object> json = new LinkedHashMap<>();
			json.put("type", "event");
			json.put("vertex", source.vertex());
			json.put("nodeId", source.nodeId());
			json.put("fields", event.fields());
			yield Json.compact(Json.toNode(json));
		}
		case value -> event.message() != null ? Json.scalarLine(event.message())
				: Json.compact(Json.toNode(event.fields()));
		case topic -> source.topic() + " " + Json.compact(Json.toNode(event.fields()));
		default -> time(event.time()) + " " + source.label() + " [evento] "
				+ (event.message() == null ? "" : Json.scalarLine(event.message()))
				+ (event.severity() >= 0 ? "   (severity " + event.severity() + ")" : "");
		};
	}

	@Override
	protected int execute(final WaldotClient client, final Printer printer) throws Exception {
		client.addListener(new WaldotClientListener() {
			@Override
			public void onReconnectScheduled(final int attempt, final Duration delay) {
				logger.warn("connection lost, reconnection attempt {} in {} ms", attempt, delay.toMillis());
			}

			@Override
			public void onStatusChanged(final Status previous, final Status current) {
				logger.info("client status {} -> {}", previous, current);
				if (current == Status.UNRECOVERABLE_FAULTED) {
					stop(ExitCodes.CONNECTION, "connessione persa: il client ha rinunciato a riconnettersi");
				}
			}

			@Override
			public void onSubscriptionGroupRecreated(final String groupName) {
				logger.info("subscription group {} recreated after the reconnection", groupName);
			}
		});
		final SubscriptionGroup group = publishing == null ? client.subscriptions().defaultGroup()
				: client.subscriptions().createGroup("waldot-cli",
						SubscriptionGroupOptions.withPublishingInterval(publishing));
		final DataItemOptions dataOptions = dataOptions();
		final EventItemOptions eventOptions = new EventItemOptions(eventFields,
				eventTypes.stream().map(type -> nodeId(client, type)).collect(java.util.stream.Collectors.toSet()),
				queue == null ? 100 : queue, discardOldest, MonitoringMode.Reporting);
		for (final Source source : sources(client)) {
			final MonitoredItemHandle item;
			if (source.event()) {
				item = group.subscribeEvents(source.nodeId(), eventOptions,
						(handle, event) -> deliver(printer, eventLine(printer, source, event)));
			} else {
				item = group.subscribeData(source.nodeId(), dataOptions,
						(handle, value) -> deliver(printer, dataLine(printer, source, value)));
			}
			item.status().filter(StatusCode::isBad).ifPresent(status -> {
				throw CliException.server("sottoscrizione di " + source.label() + " rifiutata dal server: "
						+ Json.statusName(status));
			});
			logger.info("subscribed {} ({})", source.label(), Json.id(source.nodeId()));
		}
		if (duration != null) {
			if (!done.await(duration.toNanos(), TimeUnit.NANOSECONDS)) {
				logger.info("duration {} elapsed", duration);
			}
		} else {
			done.await();
		}
		synchronized (lock) {
			stopped = true;
		}
		if (failure != null) {
			throw new CliException(exitCode.get(), failure, null);
		}
		return exitCode.get();
	}

	private DataItemOptions dataOptions() {
		DeadbandType deadbandType = DeadbandType.None;
		double deadbandValue = 0;
		if (deadbandAbsolute != null) {
			deadbandType = DeadbandType.Absolute;
			deadbandValue = deadbandAbsolute;
		} else if (deadbandPercent != null) {
			deadbandType = DeadbandType.Percent;
			deadbandValue = deadbandPercent;
		}
		return new DataItemOptions(sampling == null ? defaultSampling : sampling, queue == null ? 10 : queue,
				discardOldest, trigger, deadbandType, deadbandValue, MonitoringMode.Reporting);
	}

	private List<Source> sources(final WaldotClient client) {
		final String prefix = topicPrefix == null ? "" : topicPrefix.replaceAll("/+$", "");
		final String base = prefix.isEmpty() ? "" : prefix + "/";
		final Set<Source> sources = new LinkedHashSet<>();
		for (final String property : properties) {
			final int separator = property.lastIndexOf(':');
			final String vertex = property.substring(0, separator);
			final String key = property.substring(separator + 1);
			final NodeId variable = client.graph().propertyNodeId(nodeId(client, vertex), key);
			sources.add(new Source(vertex, key, variable, false,
					base + topicLevel(vertex) + "/" + topicLevel(key)));
		}
		for (final String node : nodes) {
			final NodeId nodeId = nodeId(client, node);
			sources.add(new Source(null, null, nodeId, false,
					base + topicLevel(String.valueOf(nodeId.getIdentifier()))));
		}
		for (final String vertex : eventSources) {
			sources.add(new Source(vertex, null, nodeId(client, vertex), true, base + topicLevel(vertex) + "/events"));
		}
		return new ArrayList<>(sources);
	}

	private void stop(final int code, final String message) {
		synchronized (lock) {
			if (stopped) {
				return;
			}
			stopped = true;
			exitCode.set(code);
			failure = message;
		}
		done.countDown();
	}

	@Override
	protected boolean supportsTopicFormat() {
		return true;
	}

	private String time(final DateTime time) {
		return time == null || time.isNull() ? Instant.now().toString() : time.getJavaInstant().toString();
	}

	@Override
	protected void validate() {
		if (properties.isEmpty() && nodes.isEmpty() && eventSources.isEmpty()) {
			throw usage("indicare almeno una sorgente: -p VERTICE:CHIAVE, -n NODO oppure -e VERTICE");
		}
		for (final String property : properties) {
			final int separator = property.lastIndexOf(':');
			if (separator <= 0 || separator == property.length() - 1) {
				throw usage("-p richiede la forma VERTICE:CHIAVE, ricevuto '" + property + "'");
			}
		}
		if (deadbandAbsolute != null && deadbandPercent != null) {
			throw usage("--deadband-absolute e --deadband-percent sono alternativi");
		}
		if (deadbandAbsolute != null && deadbandAbsolute < 0 || deadbandPercent != null && deadbandPercent < 0) {
			throw usage("il deadband non può essere negativo");
		}
		if (count != null && count < 1) {
			throw usage("--count deve essere almeno 1");
		}
		if (duration != null && duration.isZero()) {
			throw usage("--duration deve essere positiva");
		}
		if (queue != null && queue < 1) {
			throw usage("--queue deve essere almeno 1");
		}
		if (publishing != null && publishing < 0) {
			throw usage("--publishing non può essere negativo");
		}
		if (sampling != null && sampling < -1) {
			throw usage("--sampling deve essere -1 o maggiore");
		}
	}
}
