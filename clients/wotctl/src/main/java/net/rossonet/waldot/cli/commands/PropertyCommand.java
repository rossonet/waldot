package net.rossonet.waldot.cli.commands;

import java.util.LinkedHashMap;
import java.util.Map;

import net.rossonet.waldot.cli.support.CliException;
import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.CommandGroup;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.cli.support.Values;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.command.CommandResult;
import net.rossonet.waldot.client.api.graph.ElementDescription.PropertyDescription;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code waldot property get|set}.
 */
@Command(name = "property", header = "Legge o imposta le proprietà di vertici e archi.", mixinStandardHelpOptions = true, description = {
		"Legge o imposta una proprietà di un vertice o di un arco." }, subcommands = {
				PropertyCommand.GetCommand.class, PropertyCommand.SetCommand.class }, footer = { "Esempi:",
						"  waldot property set sensor-1 setpoint 21.5",
						"  waldot property get sensor-1 setpoint -o value" })
public class PropertyCommand extends CommandGroup {

	@Command(name = "get", header = "Legge una proprietà.", mixinStandardHelpOptions = true, description = {
			"Legge il valore corrente di una proprietà (dalla descrizione dell'elemento).",
			"Se la proprietà non esiste il codice di uscita è 1." }, footer = { "Output:",
					"  text, value: il valore; json/jsonl: {\"element\",\"key\",\"value\",\"valueType\",\"nodeId\"}.",
					"", "Esempi:", "  waldot property get sensor-1 temperature",
					"  if [ \"$(waldot property get pump-3 state -o value)\" = on ]; then echo accesa; fi" })
	public static class GetCommand extends ClientCommand {

		@Parameters(index = "0", paramLabel = "ELEMENTO", description = "Id del vertice o dell'arco.")
		private String element;

		@Parameters(index = "1", paramLabel = "CHIAVE", description = "Nome della proprietà.")
		private String key;

		@Override
		protected int execute(final WaldotClient client, final Printer printer) {
			final PropertyDescription property = client.graph().describe(nodeId(client, element)).property(key);
			if (property == null) {
				throw CliException.server("la proprietà '" + key + "' non esiste su '" + element + "'");
			}
			final Map<String, Object> json = new LinkedHashMap<>();
			json.put("element", element);
			json.put("key", key);
			json.put("value", property.value());
			json.put("valueType", property.valueType());
			json.put("nodeId", property.nodeId());
			printer.single(json, j -> j, j -> Json.scalar(property.value()), j -> property.value());
			return 0;
		}
	}

	@Command(name = "set", header = "Imposta una proprietà.", mixinStandardHelpOptions = true, description = {
			"Imposta (o crea) una proprietà con il comando 'property' dell'elemento.",
			"Il valore è inviato come stringa; con --type si converte prima nel tipo indicato",
			"(string, bool, sbyte, byte, int16, uint16, int32, uint32, int64, uint64, float,",
			"double, nodeid, text)." }, footer = { "Output:",
					"  text : '<elemento>/<chiave> = <valore>'; value: il valore;",
					"  json/jsonl: {\"element\",\"key\",\"value\"}.", "", "Esempi:",
					"  waldot property set sensor-1 unit C",
					"  waldot property set pump-3 speed 1450 --type int32",
					"  waldot property set valve-2 open true --type bool -o json" })
	public static class SetCommand extends ClientCommand {

		@Parameters(index = "0", paramLabel = "ELEMENTO", description = "Id del vertice o dell'arco.")
		private String element;

		@Parameters(index = "1", paramLabel = "CHIAVE", description = "Nome della proprietà.")
		private String key;

		@Parameters(index = "2", paramLabel = "VALORE", description = "Nuovo valore.")
		private String value;

		@Option(names = { "-t", "--type" }, paramLabel = "TIPO", description = "Tipo in cui convertire il valore prima dell'invio.")
		private String type;

		private Object converted;

		@Override
		protected int execute(final WaldotClient client, final Printer printer) {
			final CommandResult result = client.commands().setProperty(nodeId(client, element), key, converted);
			if (!result.isSuccess()) {
				throw CliException.server("impostazione di '" + key + "' su '" + element + "' non riuscita: "
						+ result.error().orElse("errore sconosciuto"));
			}
			final Map<String, Object> json = new LinkedHashMap<>();
			json.put("element", element);
			json.put("key", key);
			json.put("value", converted);
			printer.single(json, j -> j, j -> element + "/" + key + " = " + Json.scalar(converted), j -> converted);
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
}
