package net.rossonet.waldot.cli.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code waldot read}: values and attributes of nodes.
 */
@Command(name = "read", header = "Legge valori e attributi di nodi OPC UA.", mixinStandardHelpOptions = true, description = {
		"Legge il valore (attributo Value) di uno o più nodi OPC UA in un'unica richiesta,",
		"oppure, con --attribute, altri attributi (DisplayName, BrowseName, DataType, ...).",
		"Il NodeId della variabile di una proprietà di WaldOT si ottiene con 'describe'",
		"(di solito <elemento>/<chiave>, per esempio sensor-1/temperature).",
		"Se uno stato OPC UA è cattivo (es. Bad_NodeIdUnknown) la riga è scritta comunque",
		"e il codice di uscita è 1." }, footer = { "Output:",
				"  text : '<nodeId> = <valore>' (con '[<attributo>]' e lo stato se non Good);",
				"  value: solo i valori, uno per riga;",
				"  json/jsonl: {\"nodeId\",\"attribute\",\"value\",\"status\",\"sourceTime\",\"serverTime\"}.",
				"", "Esempi:", "  waldot read sensor-1/temperature -o value",
				"  waldot read 'ns=2;s=sensor-1/temperature' 'ns=2;s=sensor-2/temperature' -o jsonl",
				"  waldot read i=2258 -o value                 # ora corrente del server",
				"  waldot read sensor-1 --attribute DisplayName --attribute Description" })
public class ReadCommand extends ClientCommand {

	/** A value read. */
	public record Reading(NodeId nodeId, AttributeId attribute, DataValue dataValue) {

		Map<String, Object> json() {
			final Map<String, Object> json = new LinkedHashMap<>();
			json.put("nodeId", nodeId);
			json.put("attribute", attribute.name());
			json.put("value", dataValue.getValue());
			json.put("status", dataValue.getStatusCode());
			json.put("sourceTime", dataValue.getSourceTime());
			json.put("serverTime", dataValue.getServerTime());
			return json;
		}

		boolean isGood() {
			return dataValue.getStatusCode() == null || dataValue.getStatusCode().isGood();
		}

		String text() {
			return Json.id(nodeId) + (attribute == AttributeId.Value ? "" : " [" + attribute.name() + "]") + " = "
					+ Json.scalarLine(dataValue.getValue())
					+ (isGood() ? "" : "   (" + Json.statusName(dataValue.getStatusCode()) + ")");
		}
	}

	@Parameters(arity = "1..*", paramLabel = "NODO", description = "Nodi da leggere.")
	private List<String> nodes = new ArrayList<>();

	@Option(names = { "-a", "--attribute" }, paramLabel = "ATTRIBUTO", description = "Attributo da leggere (ripetibile, predefinito Value): ${COMPLETION-CANDIDATES}.")
	private List<AttributeId> attributes = new ArrayList<>();

	@Override
	protected int execute(final WaldotClient client, final Printer printer) {
		final List<NodeId> ids = nodes.stream().map(node -> nodeId(client, node)).toList();
		final List<Reading> readings = new ArrayList<>();
		if (attributes.isEmpty()) {
			final List<DataValue> values = client.addressSpace().readValues(ids);
			for (int i = 0; i < ids.size(); i++) {
				readings.add(new Reading(ids.get(i), AttributeId.Value, values.get(i)));
			}
		} else {
			for (final NodeId id : ids) {
				final Map<AttributeId, DataValue> values = client.addressSpace().readAttributes(id,
						attributes.toArray(new AttributeId[0]));
				for (final AttributeId attribute : attributes) {
					readings.add(new Reading(id, attribute, values.get(attribute)));
				}
			}
		}
		printer.list(readings, Reading::json, Reading::text, reading -> reading.dataValue().getValue());
		for (final Reading reading : readings) {
			if (!reading.isGood()) {
				throw net.rossonet.waldot.cli.support.CliException.server("lettura di " + Json.id(reading.nodeId())
						+ " non riuscita: " + Json.statusName(reading.dataValue().getStatusCode()));
			}
		}
		return 0;
	}
}
