package net.rossonet.waldot.cli.support;

import picocli.CommandLine.Option;

/**
 * Output format and verbosity, common to every command.
 */
public class OutputOptions {

	private static OutputFormat parseFormat(final String value) {
		if (value == null || value.isBlank()) {
			return OutputFormat.text;
		}
		try {
			return OutputFormat
					.valueOf(value.trim().toLowerCase(java.util.Locale.ROOT));
		} catch (final IllegalArgumentException e) {
			return OutputFormat.text;
		}
	}

	@Option(names = {"-o", "--output"}, paramLabel = "FORMATO", description = {
			"Formato dei dati su stdout: ${COMPLETION-CANDIDATES}.",
			"  text  = leggibile (predefinito);",
			"  json  = un unico documento JSON (array per gli elenchi);",
			"  jsonl = un oggetto JSON compatto per riga (JSON Lines);",
			"  value = solo i valori, uno per riga;",
			"  topic = righe '<prefisso>/<vertice>/<chiave> <payload>' (solo subscribe).",
			"Variabile: WALDOT_OUTPUT."})
	OutputFormat format = parseFormat(System.getenv("WALDOT_OUTPUT"));

	@Option(names = {"-v", "--verbose"}, description = {
			"Aumenta i messaggi di log su stderr (ripetibile): nessuno = solo avvisi ed errori,",
			"-v = informazioni, -vv = debug (e stack trace degli errori), -vvv = trace.",
			"Variabile: WALDOT_LOG_LEVEL (error, warn, info, debug, trace)."})
	boolean[] verbose = new boolean[0];

	public OutputFormat format() {
		return format == null ? OutputFormat.text : format;
	}
}
