package net.rossonet.waldot.cli.commands;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.CommandGroup;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.graph.GraphChangeResult;
import net.rossonet.waldot.client.api.graph.GraphSnapshot;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * {@code waldot snapshot export|import|clear}.
 */
@Command(name = "snapshot", header = "Backup, ripristino e svuotamento del grafo.", mixinStandardHelpOptions = true, description = {
		"Backup e ripristino dell'intero grafo con i comandi 'export graph', 'read graph",
		"block', 'import graph' e 'clear graph' del server. Il formato è GraphSON (anche",
		"compresso gzip); i grafi grandi viaggiano automaticamente a blocchi." }, subcommands = {
				SnapshotCommand.ExportCommand.class, SnapshotCommand.ImportCommand.class,
				SnapshotCommand.ClearCommand.class }, footer = { "Esempi:",
						"  waldot snapshot export -f backup-$(date +%%F).json.gz",
						"  waldot snapshot import --clear -f backup-2026-10-28.json.gz",
						"  waldot snapshot export --endpoint opc.tcp://a:12686/waldot \\",
						"    | waldot snapshot import --clear --endpoint opc.tcp://b:12686/waldot",
						"  waldot snapshot clear --yes" })
public class SnapshotCommand extends CommandGroup {

	@Command(name = "clear", header = "Elimina l'intero grafo (richiede --yes).", mixinStandardHelpOptions = true, description = {
			"Elimina tutti i vertici e gli archi del grafo. Operazione distruttiva: richiede",
			"--yes, altrimenti termina con codice 2 senza toccare il server." }, footer = { "Output:",
					"  text : 'eliminati V vertici e E archi'; value: 'V E';",
					"  json/jsonl: {\"vertices\",\"edges\",\"warnings\"}.", "", "Esempi:",
					"  waldot snapshot clear --yes" })
	public static class ClearCommand extends ClientCommand {

		@Option(names = "--yes", description = "Conferma l'eliminazione dell'intero grafo.")
		private boolean yes;

		@Override
		protected int execute(final WaldotClient client, final Printer printer) {
			final GraphChangeResult result = client.snapshots().clearGraph();
			printResult(printer, result, "eliminati");
			return 0;
		}

		@Override
		protected void validate() {
			if (!yes) {
				throw usage("'snapshot clear' elimina l'intero grafo: confermare con --yes");
			}
		}
	}

	@Command(name = "export", header = "Esporta il grafo (backup GraphSON).", mixinStandardHelpOptions = true, description = {
			"Esporta il grafo (vertici, archi e proprietà) come GraphSON, compresso gzip se",
			"non si indica --no-compress. Senza --file (o con '-f -') i byte del backup vanno",
			"su stdout, così da poterli inviare in pipe o redirigere; in quel caso stdout",
			"contiene solo il backup e il riepilogo va nel log (visibile con -v)." }, footer = {
					"Output con --file:",
					"  text : 'esportati V vertici e E archi (N byte, B blocchi) in FILE';",
					"  value: il percorso del file;",
					"  json/jsonl: {\"file\",\"size\",\"compressed\",\"vertices\",\"edges\",\"blocks\"}.",
					"", "Esempi:", "  waldot snapshot export -f grafo.json.gz",
					"  waldot snapshot export --no-compress -f grafo.json",
					"  waldot snapshot export --no-compress | jq '.vertices | length'",
					"  waldot snapshot export --include-ephemeral --block-size 262144 -f completo.gz" })
	public static class ExportCommand extends ClientCommand {

		private static final Logger logger = LoggerFactory.getLogger(ExportCommand.class);

		@Option(names = { "-f", "--file" }, paramLabel = "FILE", description = "File di destinazione ('-' = stdout, predefinito).")
		private Path file;

		@Option(names = "--no-compress", description = "Non comprime il GraphSON (testo JSON leggibile).")
		private boolean noCompress;

		@Option(names = "--include-ephemeral", description = "Include anche gli elementi effimeri (normalmente esclusi).")
		private boolean includeEphemeral;

		@Option(names = "--block-size", paramLabel = "BYTE", description = "Dimensione dei blocchi del trasferimento (predefinita ${DEFAULT-VALUE}).")
		private int blockSize = DataExchangeProtocol.DEFAULT_BLOCK_SIZE;

		@Override
		protected int execute(final WaldotClient client, final Printer printer) throws IOException {
			final GraphSnapshot snapshot = client.snapshots().exportGraph(!noCompress, includeEphemeral, blockSize);
			final byte[] data = snapshot.data();
			if (file == null || "-".equals(file.toString())) {
				final OutputStream stdout = new FileOutputStream(FileDescriptor.out);
				stdout.write(data);
				stdout.flush();
				logger.info("exported {} vertices and {} edges ({} bytes, {} blocks)", snapshot.vertices(),
						snapshot.edges(), data.length, snapshot.blocks());
				return 0;
			}
			Files.write(file, data);
			printer.single(snapshot, s -> {
				final java.util.Map<String, Object> json = new java.util.LinkedHashMap<>();
				json.put("file", file.toString());
				json.put("size", s.size());
				json.put("compressed", s.compressed());
				json.put("vertices", s.vertices());
				json.put("edges", s.edges());
				json.put("blocks", s.blocks());
				return json;
			}, s -> "esportati " + s.vertices() + " vertici e " + s.edges() + " archi (" + s.size() + " byte, "
					+ s.blocks() + " blocchi) in " + file, s -> file.toString());
			return 0;
		}

		@Override
		protected void validate() {
			final String error = blockSizeError(blockSize);
			if (error != null) {
				throw usage(error);
			}
		}
	}

	@Command(name = "import", header = "Importa un backup del grafo.", mixinStandardHelpOptions = true, description = {
			"Importa un backup GraphSON (semplice o gzip, riconosciuto automaticamente).",
			"Senza --file (o con '-f -') il backup si legge da stdin. Con --clear il grafo",
			"corrente è eliminato prima dell'importazione (ripristino completo); senza, gli",
			"elementi del backup si aggiungono a quelli esistenti. Gli avvisi del server",
			"(elementi saltati, conflitti) vanno su stderr." }, footer = { "Output:",
					"  text : 'importati V vertici e E archi'; value: 'V E';",
					"  json/jsonl: {\"vertices\",\"edges\",\"warnings\"}.", "", "Esempi:",
					"  waldot snapshot import --clear -f grafo.json.gz",
					"  waldot snapshot import < aggiunte.json",
					"  curl -s https://example.org/grafo.json.gz | waldot snapshot import --clear" })
	public static class ImportCommand extends ClientCommand {

		@Option(names = { "-f", "--file" }, paramLabel = "FILE", description = "File da importare ('-' = stdin, predefinito).")
		private Path file;

		@Option(names = "--clear", description = "Elimina il grafo corrente prima di importare.")
		private boolean clear;

		@Option(names = "--block-size", paramLabel = "BYTE", description = "Dimensione dei blocchi del caricamento (predefinita ${DEFAULT-VALUE}).")
		private int blockSize = DataExchangeProtocol.DEFAULT_BLOCK_SIZE;

		private byte[] data;

		@Override
		protected int execute(final WaldotClient client, final Printer printer) {
			final GraphChangeResult result = client.snapshots().importGraph(GraphSnapshot.of(data), clear,
					blockSize);
			printResult(printer, result, "importati");
			return 0;
		}

		@Override
		protected void validate() {
			final String error = blockSizeError(blockSize);
			if (error != null) {
				throw usage(error);
			}
			try {
				data = file == null || "-".equals(file.toString()) ? System.in.readAllBytes()
						: Files.readAllBytes(file);
			} catch (final IOException e) {
				throw usage("impossibile leggere il backup " + (file == null ? "da stdin" : file) + ": "
						+ e.getMessage());
			}
			if (data.length == 0) {
				throw usage("il backup da importare è vuoto");
			}
		}
	}

	/** @return the error of an invalid block size, null if valid */
	private static String blockSizeError(final int blockSize) {
		if (blockSize < DataExchangeProtocol.MIN_BLOCK_SIZE || blockSize > DataExchangeProtocol.MAX_BLOCK_SIZE) {
			return "--block-size deve essere compreso tra " + DataExchangeProtocol.MIN_BLOCK_SIZE + " e "
					+ DataExchangeProtocol.MAX_BLOCK_SIZE;
		}
		return null;
	}

	private static void printResult(final Printer printer, final GraphChangeResult result, final String verb) {
		if (result.hasWarnings()) {
			LoggerFactory.getLogger(SnapshotCommand.class).warn("server warnings: {}", result.warnings());
		}
		printer.single(result, r -> r, r -> verb + " " + r.vertices() + " vertici e " + r.edges() + " archi",
				r -> r.vertices() + " " + r.edges());
	}
}
