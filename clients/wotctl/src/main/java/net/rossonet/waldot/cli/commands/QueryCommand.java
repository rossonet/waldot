package net.rossonet.waldot.cli.commands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Element;

import net.rossonet.waldot.cli.support.CliException;
import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Json;
import net.rossonet.waldot.cli.support.OutputFormat;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.graph.QueryResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * {@code wotctl query}: runs a query on the graph.
 */
@Command(name = "query", header = "Interroga il grafo del server (traversal Gremlin).", mixinStandardHelpOptions = true, description = {
		"Esegue un'interrogazione sul grafo del server. L'espressione è valutata dal server",
		"(JEXL, con la stessa sintassi della console di WaldOT) e di solito è una traversal",
		"Gremlin, per esempio g.V().hasLabel('sensor').has('zone','A').", "",
		"Modalità:",
		"  predefinita: il risultato viaggia come GraphSON tipizzato ed è ricostruito in",
		"    oggetti (vertici, archi, percorsi, mappe, liste, numeri, stringhe) che vengono",
		"    scritti nel formato scelto con -o, un elemento per riga nei formati a righe;",
		"  --text: esegue il comando 'query' testuale e scrive l'output così come lo",
		"    restituisce il server (come nella console): il server non itera le traversal,",
		"    quindi si aggiunge .next() o .toList() (es. g.V().count().next());",
		"  --graphson: scrive il GraphSON v3 grezzo restituito dal server.", "",
		"Se il server segnala un errore nell'espressione il messaggio va su stderr e il",
		"codice di uscita è 1."}, footer = {"Output (modalità predefinita):",
				"  text : un elemento per riga; vertici come v[id] label {proprietà},",
				"         archi come e[id] label origine -> destinazione {proprietà};",
				"  json : array JSON con tutti gli elementi; vertici {\"type\":\"vertex\",\"id\",",
				"         \"label\",\"properties\"}, archi {\"type\":\"edge\",\"id\",\"label\",\"outV\",",
				"         \"inV\",\"properties\"} (gli id sono NodeId come ns=2;s=x);",
				"  jsonl: un oggetto JSON compatto per riga (ideale con jq);",
				"  value: un valore per riga; per vertici e archi l'id.", "",
				"Esempi:", "  wotctl query \"g.V().count()\" -o value",
				"  wotctl query -o jsonl \"g.V().hasLabel('sensor')\" | jq -r '.properties.name'",
				"  wotctl query -o value \"g.V().hasLabel('sensor').values('name')\"",
				"  wotctl query --text \"g.V().valueMap().toList()\"",
				"  wotctl query --graphson \"g.E().limit(5)\" > archi.json",
				"  echo \"g.V().limit(3)\" | wotctl query -f - -o json",
				"  wotctl query -f interrogazione.txt -o jsonl"})
public class QueryCommand extends ClientCommand {

	@Parameters(index = "0", arity = "0..1", paramLabel = "ESPRESSIONE", description = "Espressione da eseguire (racchiuderla tra virgolette doppie, con apici singoli all'interno).")
	private String expression;

	@Option(names = {"-f",
			"--file"}, paramLabel = "FILE", description = "Legge l'espressione da un file ('-' = stdin) invece che dall'argomento.")
	private Path file;

	@Option(names = "--graphson", description = "Scrive il GraphSON v3 grezzo del risultato (ignora -o).")
	private boolean graphson;

	@Option(names = "--text", description = "Usa il comando 'query' testuale e scrive l'output del server così com'è.")
	private boolean text;

	@Override
	protected int execute(final WaldotClient client, final Printer printer)
			throws IOException {
		final String query = readExpression();
		if (graphson) {
			printer.line(client.graph().queryJson(query));
			return 0;
		}
		if (text) {
			final QueryResult result = client.graph().query(query);
			if (!result.isSuccess()) {
				throw CliException
						.server("errore nella query: " + result.error());
			}
			if (printer.format() == OutputFormat.json
					|| printer.format() == OutputFormat.jsonl) {
				printer.single(result, r -> r, r -> r.output(),
						r -> r.output());
			} else {
				printer.line(result.output());
			}
			return 0;
		}
		final List<Object> objects = client.graph().queryObjects(query);
		printer.list(objects, object -> object, Json::text,
				object -> object instanceof final Element element
						? Json.id(element.id())
						: object);
		return 0;
	}

	private String readExpression() throws IOException {
		if (file != null) {
			if (expression != null) {
				throw usage(
						"indicare l'espressione come argomento oppure con --file, non entrambi");
			}
			final String content = "-".equals(file.toString())
					? new String(System.in.readAllBytes(),
							StandardCharsets.UTF_8)
					: Files.readString(file, StandardCharsets.UTF_8);
			return content.trim();
		}
		return expression;
	}

	@Override
	protected void validate() {
		if (expression == null && file == null) {
			throw usage(
					"manca l'espressione da eseguire (argomento ESPRESSIONE o --file)");
		}
		if (text && graphson) {
			throw usage("--text e --graphson sono alternativi");
		}
	}
}
