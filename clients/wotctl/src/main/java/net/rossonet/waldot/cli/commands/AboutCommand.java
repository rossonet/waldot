package net.rossonet.waldot.cli.commands;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import picocli.CommandLine.Command;

/**
 * {@code wotctl about}: server information.
 */
@Command(name = "about", header = "Informazioni sul server WaldOT (anche verifica della connessione).", mixinStandardHelpOptions = true, description = {
		"Mostra le informazioni sul server WaldOT (comando 'about': versione, namespace,",
		"plugin e configurazione principale). Utile anche come verifica della connessione:",
		"il codice di uscita è 0 se il server risponde, 3 se non è raggiungibile."}, footer = {
				"Output:",
				"  text, value: una riga per ogni riga della risposta;",
				"  json: array di stringhe; jsonl: una stringa JSON per riga.",
				"  --structured: {product, description, version, commit, branch, build_date, license,",
				"              license_url, repository_url, vertices, edges, start_time, uptime_s,",
				"              raw:{uscita: valore}} (i valori assenti sono null).",
				"", "Esempi:", "  ${ROOT-COMMAND-NAME} about", "  ${ROOT-COMMAND-NAME} about --structured -o json",
				"  ${ROOT-COMMAND-NAME} about --endpoint opc.tcp://192.168.1.10:12686/waldot",
				"  ${ROOT-COMMAND-NAME} about --connect-timeout 3s >/dev/null && echo raggiungibile"})
public class AboutCommand extends ClientCommand {

	/**
	 * Structured information: product, version, commit, build date, license,
	 * repository, size of the graph, start time and uptime of the server, and
	 * the raw outputs by name.
	 */
	public static Map<String, Object> structured(final WaldotClient client) {
		Map<String, Object> raw;
		try {
			raw = client.commands().aboutInfo();
		} catch (final RuntimeException e) {
			raw = Map.of();
		}
		if (raw.isEmpty()) {
			// a server (or a client) without output names: the positions
			raw = new LinkedHashMap<>();
			final List<String> lines = client.commands().about();
			final String[] names = { "name", "description", "license", "licence URL", "repository URL" };
			for (int i = 0; i < lines.size(); i++) {
				raw.put(i < names.length ? names[i] : "output-" + i, lines.get(i));
			}
		}
		final Map<String, Object> out = new LinkedHashMap<>();
		out.put("product", text(raw.get("name")));
		out.put("description", text(raw.get("description")));
		out.put("version", first(raw, "git.build.version", "git.closest.tag.name"));
		out.put("commit", first(raw, "git.commit.id.abbrev", "git.commit.id"));
		out.put("branch", first(raw, "git.branch"));
		out.put("build_date", first(raw, "git.build.time", "git.commit.time"));
		out.put("license", text(raw.get("license")));
		out.put("license_url", text(raw.get("licence URL")));
		out.put("repository_url", text(raw.get("repository URL")));
		out.put("vertices", count(client, "g.V().count()"));
		out.put("edges", count(client, "g.E().count()"));
		try {
			final Object start = client.addressSpace()
					.readValue(org.eclipse.milo.opcua.stack.core.NodeIds.Server_ServerStatus_StartTime).getValue()
					.getValue();
			final Object now = client.addressSpace()
					.readValue(org.eclipse.milo.opcua.stack.core.NodeIds.Server_ServerStatus_CurrentTime).getValue()
					.getValue();
			if (start instanceof final org.eclipse.milo.opcua.stack.core.types.builtin.DateTime s
					&& now instanceof final org.eclipse.milo.opcua.stack.core.types.builtin.DateTime n) {
				out.put("start_time", s.getJavaInstant().toString());
				out.put("uptime_s", java.time.Duration.between(s.getJavaInstant(), n.getJavaInstant()).toSeconds());
			}
		} catch (final RuntimeException e) {
			// server status not readable
		}
		out.put("raw", raw);
		return out;
	}

	private static Long count(final WaldotClient client, final String expression) {
		try {
			final List<Object> result = client.graph().queryObjects(expression);
			return result.isEmpty() || !(result.get(0) instanceof final Number n) ? null : n.longValue();
		} catch (final RuntimeException e) {
			return null;
		}
	}

	private static String first(final Map<String, Object> raw, final String... keys) {
		for (final String key : keys) {
			final String value = text(raw.get(key));
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	/** Text of an output, null when empty or the "NaN" placeholder. */
	private static String text(final Object value) {
		if (value == null) {
			return null;
		}
		final String text = value.toString().strip();
		return text.isEmpty() || "NaN".equals(text) ? null : text;
	}

	@picocli.CommandLine.Option(names = "--structured", description = "Oggetto con campi nominati (product, version, commit, vertices...) invece delle righe.")
	boolean structured;

	@Override
	protected int execute(final WaldotClient client, final Printer printer) {
		if (structured) {
			final Map<String, Object> info = structured(client);
			printer.single(info, i -> i, i -> {
				final StringBuilder sb = new StringBuilder();
				i.forEach((k, v) -> {
					if (!"raw".equals(k) && v != null) {
						sb.append(k).append(": ").append(v).append('\n');
					}
				});
				return sb.toString().stripTrailing();
			}, i -> i.get("version"));
			return 0;
		}
		final List<String> lines = client.commands().about();
		printer.list(lines, line -> line, line -> line, line -> line);
		return 0;
	}
}
