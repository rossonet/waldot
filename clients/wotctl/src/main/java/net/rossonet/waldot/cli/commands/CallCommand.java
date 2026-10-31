package net.rossonet.waldot.cli.commands;

import java.util.ArrayList;
import java.util.List;

import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.cli.support.Values;
import net.rossonet.waldot.client.api.WaldotClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * {@code wotctl call}: calls an OPC UA method.
 */
@Command(name = "call", header = "Chiama un metodo OPC UA generico.", mixinStandardHelpOptions = true, description = {
		"Chiama un metodo OPC UA generico indicando l'oggetto e il metodo. Per i comandi",
		"di WaldOT è più comodo 'commands call', che trova il metodo per nome e converte",
		"gli argomenti da solo.", "",
		"Gli argomenti sono stringhe; per un altro tipo si usa il prefisso 'tipo:valore'",
		"(string, bool, sbyte, byte, int16, uint16, int32, uint32, int64, uint64, float,",
		"double, nodeid, text), per esempio int32:5, double:0.5, bool:true. Una stringa che",
		"inizia per uno di questi prefissi si scrive come string:int32:5.",
		"Uno stato di risposta non buono produce il codice di uscita 1."}, footer = {
				"Output:", "  text, value: i valori di uscita, uno per riga;",
				"  json: array dei valori; jsonl: un valore JSON per riga.", "",
				"Esempi:",
				"  wotctl browse sensor-1 --node-class Method   # trova i metodi di un nodo",
				"  wotctl call i=2253 i=11492 uint32:1          # GetMonitoredItems del server",
				"  wotctl call plant-1 plant-1/reset bool:true -o json"})
public class CallCommand extends ClientCommand {

	@Parameters(index = "2..*", arity = "0..*", paramLabel = "ARGOMENTO", description = "Argomenti di ingresso ([tipo:]valore).")
	private final List<String> arguments = new ArrayList<>();

	private Object[] inputs;

	@Parameters(index = "1", paramLabel = "METODO", description = "Metodo da chiamare.")
	private String method;

	@Parameters(index = "0", paramLabel = "OGGETTO", description = "Oggetto su cui è definito il metodo.")
	private String object;

	@Override
	protected int execute(final WaldotClient client, final Printer printer) {
		final List<Object> outputs = client.addressSpace()
				.call(nodeId(client, object), nodeId(client, method), inputs);
		printer.list(outputs, value -> value, Json::scalar, value -> value);
		return 0;
	}

	@Override
	protected void validate() {
		try {
			inputs = arguments.stream().map(Values::literal).toArray();
		} catch (final IllegalArgumentException e) {
			throw usage(e.getMessage());
		}
	}
}
