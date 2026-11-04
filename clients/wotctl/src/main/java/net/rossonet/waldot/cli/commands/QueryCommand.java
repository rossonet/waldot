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
				"  wotctl query -f interrogazione.txt -o jsonl",
				"  wotctl query --table --round 2 \"g.V().has('type','zona').valueMap('t-ambiente','setpoint')\" -o json"})
@SuppressWarnings("unchecked")
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

	@Option(names = "--table", description = "Tabella {columns, rows} dai risultati (valueMap, elementMap, project, vertici): i valori singoli escono dalle liste.")
	private boolean table;

	@Option(names = "--round", paramLabel = "N", description = "Arrotonda i numeri decimali a N cifre.")
	private Integer round;

	/** Rounds the decimal numbers of a result (maps and lists included). */
	static Object rounded(final Object value, final Integer digits) {
		if (digits == null) {
			return value;
		}
		if (value instanceof Double || value instanceof Float || value instanceof java.math.BigDecimal) {
			final double d = ((Number) value).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				return value;
			}
			return java.math.BigDecimal.valueOf(d).setScale(digits, java.math.RoundingMode.HALF_UP).doubleValue();
		}
		if (value instanceof final java.util.Map<?, ?> map) {
			final java.util.Map<Object, Object> out = new java.util.LinkedHashMap<>();
			map.forEach((k, v) -> out.put(k, rounded(v, digits)));
			return out;
		}
		if (value instanceof final java.util.List<?> list) {
			return list.stream().map(v -> rounded(v, digits)).toList();
		}
		return value;
	}

	/** A single value outside its list (valueMap gives [23.5] for every key). */
	static Object unwrap(final Object value) {
		if (value instanceof final java.util.List<?> list && list.size() == 1) {
			return list.get(0);
		}
		return value;
	}

	/**
	 * Table of the results: columns from the keys of the maps (or id, label and
	 * properties of the elements, or a single column 'value').
	 */
	static java.util.Map<String, Object> table(final List<Object> objects, final Integer digits) {
		final List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
		final java.util.LinkedHashSet<String> columns = new java.util.LinkedHashSet<>();
		for (final Object object : objects) {
			final java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
			if (object instanceof final Element element) {
				row.put("id", Json.id(element.id()));
				row.put("label", element.label());
				element.properties().forEachRemaining(p -> row.put(p.key(), p.value()));
			} else if (object instanceof final java.util.Map<?, ?> map) {
				map.forEach((k, v) -> row.put(k instanceof org.apache.tinkerpop.gremlin.structure.T t ? t.getAccessor()
						: String.valueOf(k), v instanceof final Element e ? Json.id(e.id()) : v));
			} else {
				row.put("value", object);
			}
			columns.addAll(row.keySet());
			rows.add(row);
		}
		final List<String> ordered = new java.util.ArrayList<>();
		for (final String first : new String[] { "id", "label" }) {
			if (columns.remove(first)) {
				ordered.add(first);
			}
		}
		ordered.addAll(columns);
		final List<List<Object>> values = new java.util.ArrayList<>();
		for (final java.util.Map<String, Object> row : rows) {
			final List<Object> line = new java.util.ArrayList<>();
			for (final String column : ordered) {
				line.add(rounded(unwrap(row.get(column)), digits));
			}
			values.add(line);
		}
		final java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
		out.put("columns", ordered);
		out.put("rows", values);
		return out;
	}

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
		if (table) {
			final java.util.Map<String, Object> t = table(objects, round);
			printer.single(t, x -> x, x -> {
				final StringBuilder sb = new StringBuilder(String.join("\t", (List<String>) x.get("columns")));
				for (final Object row : (List<?>) x.get("rows")) {
					sb.append('\n').append(String.join("\t",
							((List<?>) row).stream().map(v -> v == null ? "" : Json.scalar(v)).toList()));
				}
				return sb.toString();
			}, x -> ((List<?>) x.get("rows")).size());
			return 0;
		}
		if (round != null) {
			final List<Object> r = objects.stream().map(o -> rounded(o, round)).toList();
			printer.list(r, object -> object, Json::text,
					object -> object instanceof final Element element ? Json.id(element.id()) : object);
			return 0;
		}
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
		if (table && (text || graphson)) {
			throw usage("--table vale solo nella modalità predefinita (senza --text o --graphson)");
		}
		if (round != null && (round < 0 || round > 15)) {
			throw usage("--round vuole un numero di cifre tra 0 e 15");
		}
	}
}
