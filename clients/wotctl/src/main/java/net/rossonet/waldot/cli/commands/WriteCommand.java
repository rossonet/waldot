package net.rossonet.waldot.cli.commands;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

import net.rossonet.waldot.cli.support.CliException;
import net.rossonet.waldot.cli.support.Completions;
import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.cli.support.Values;
import net.rossonet.waldot.client.api.WaldotClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code wotctl write}: writes the value of a variable.
 */
@Command(name = "write", header = "Scrive il valore di una variabile OPC UA.", mixinStandardHelpOptions = true, description = {
		"Scrive l'attributo Value di una variabile OPC UA. Senza --type il valore è",
		"convertito nel tipo del valore corrente della variabile (letto dal server: le",
		"proprietà di WaldOT sono BaseDataType, quindi un intero resta intero) oppure, se",
		"non è possibile, nel tipo di dato dichiarato; altrimenti è inviato come stringa;",
		"con --type si sceglie esplicitamente il tipo: string, bool, sbyte, byte, int16,",
		"uint16, int32, uint32, int64, uint64, float, double, nodeid, text.",
		"Uno stato di risposta non buono (es. Bad_NotWritable, Bad_TypeMismatch) produce",
		"il codice di uscita 1."}, footer = {"Output:",
				"  text : '<nodeId> <- <valore> (<stato>)';",
				"  value: lo stato (Good); json/jsonl: {\"nodeId\",\"value\",\"status\"}.",
				"", "Esempi:", "  wotctl write sensor-1/setpoint 21.5",
				"  wotctl write 'ns=2;s=pump-3/enabled' true --type bool",
				"  wotctl write valve-2/position 75 -o json"})
public class WriteCommand extends ClientCommand {

	private static final Map<Class<?>, String> TYPE_OF = Map.ofEntries(
			Map.entry(Boolean.class, "bool"), Map.entry(Byte.class, "sbyte"),
			Map.entry(Short.class, "int16"), Map.entry(Integer.class, "int32"),
			Map.entry(Long.class, "int64"), Map.entry(Float.class, "float"),
			Map.entry(Double.class, "double"),
			Map.entry(
					org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte.class,
					"byte"),
			Map.entry(
					org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort.class,
					"uint16"),
			Map.entry(
					org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger.class,
					"uint32"),
			Map.entry(
					org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong.class,
					"uint64"),
			Map.entry(NodeId.class, "nodeid"));

	/**
	 * Without --type: the type of the current value of the variable (the
	 * properties of WaldOT are BaseDataType variables, so the declared data
	 * type is not enough); the text if the conversion is not possible.
	 */
	private static Object inferType(final WaldotClient client,
			final NodeId nodeId, final String text) {
		try {
			final Object current = client.addressSpace().readValue(nodeId)
					.getValue().getValue();
			final String name = current == null
					? null
					: TYPE_OF.get(current.getClass());
			return name == null ? text : Values.convert(text, name);
		} catch (final RuntimeException e) {
			return text;
		}
	}

	private Object converted;

	@Parameters(index = "0", paramLabel = "NODO", description = "Variabile da scrivere.")
	private String node;

	@Option(names = {"-t",
			"--type"}, paramLabel = "TIPO", completionCandidates = Completions.ValueTypes.class, description = "Tipo del valore (predefinito: quello della variabile).")
	private String type;

	@Parameters(index = "1", paramLabel = "VALORE", description = "Valore da scrivere.")
	private String value;

	@Override
	protected int execute(final WaldotClient client, final Printer printer) {
		final NodeId nodeId = nodeId(client, node);
		if (type == null) {
			converted = inferType(client, nodeId, value);
		}
		final StatusCode status = client.addressSpace().writeValue(nodeId,
				converted);
		if (status != null && !status.isGood()) {
			throw CliException.server("scrittura di " + Json.id(nodeId)
					+ " non riuscita: " + Json.statusName(status));
		}
		final Map<String, Object> json = new LinkedHashMap<>();
		json.put("nodeId", nodeId);
		json.put("value", converted);
		json.put("status", status == null ? StatusCode.GOOD : status);
		printer.single(json, j -> j,
				j -> Json.id(nodeId) + " <- " + Json.scalarLine(converted)
						+ " ("
						+ Json.statusName(
								status == null ? StatusCode.GOOD : status)
						+ ")",
				j -> Json
						.statusName(status == null ? StatusCode.GOOD : status));
		return 0;
	}

	@Override
	protected void validate() {
		try {
			converted = type == null ? value : Values.convert(value, type);
		} catch (final IllegalArgumentException e) {
			throw usage(e.getMessage());
		}
	}
}
