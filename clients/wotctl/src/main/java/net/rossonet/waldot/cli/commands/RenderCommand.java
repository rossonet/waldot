package net.rossonet.waldot.cli.commands;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.exchange.DataExchangeProtocol;
import net.rossonet.waldot.api.exchange.GraphRenderOptions;
import net.rossonet.waldot.cli.support.ClientCommand;
import net.rossonet.waldot.cli.support.Printer;
import net.rossonet.waldot.client.api.WaldotClient;
import net.rossonet.waldot.client.api.graph.GraphRendering;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * {@code waldot render}: draws the graph as SVG or Graphviz DOT.
 */
@Command(name = "render", header = "Disegna il grafo (SVG o Graphviz DOT).", mixinStandardHelpOptions = true, description = {
		"Disegna il grafo, o una sua parte, con il comando 'render graph' del server. Il",
		"layout dell'SVG è calcolato dal server (Eclipse ELK); il DOT è lasciato a",
		"Graphviz. Senza --file (o con '-f -') il documento va su stdout; con un file .dot",
		"o .gv il formato predefinito è DOT.",
		"",
		"Selezione dei vertici: tutti, oppure quelli raggiungibili da --root entro --depth",
		"passi (seguendo solo gli archi selezionati), filtrati per tipo, etichetta,",
		"cartella, espressione regolare o valore di proprietà. I vertici effimeri e le",
		"proprietà di runtime ('run!...') sono esclusi se non richiesti. Sono disegnati",
		"solo gli archi tra vertici disegnati: quelli omessi sono segnalati su stderr.",
		"",
		"Ogni opzione del comando server è disponibile in --options-file (JSON, vedi",
		"GraphRenderOptions); i flag della riga di comando prevalgono sul file." }, footer = {
				"Output con --file:",
				"  text : 'disegnati V vertici e E archi (N byte, FORMATO) in FILE';",
				"  value: il percorso del file;",
				"  json/jsonl: {\"file\",\"format\",\"size\",\"vertices\",\"edges\",\"blocks\",\"warnings\"}.",
				"",
				"Esempi:",
				"  waldot render -f impianto.svg --group-by directory",
				"  waldot render -f plc.svg --root plc-1 --depth 2 --include-runtime --include-ephemeral",
				"  waldot render -f regole.svg --edge-label fire --edge-label link-to --no-isolated",
				"  waldot render -f linea1.svg --directory impianto/linea1 --layout mrtree --props unit,status",
				"  waldot render -f allarmi.svg --where status=alarm --root sensore-1 --theme dark",
				"  waldot render -f grafo.dot && dot -Tpng grafo.dot -o grafo.png",
				"  waldot render --options-file vista.json --max 2000 > vista.svg" })
public class RenderCommand extends ClientCommand {

	private static final Logger logger = LoggerFactory.getLogger(RenderCommand.class);

	@Option(names = { "-f", "--file" }, paramLabel = "FILE", description = "File di destinazione ('-' = stdout, predefinito).")
	private Path file;

	@Option(names = "--format", paramLabel = "FORMATO", description = "svg o dot (predefinito svg, dot per i file .dot e .gv).")
	private GraphRenderOptions.Format format;

	@Option(names = "--options-file", paramLabel = "FILE", description = "Opzioni JSON di base del comando 'render graph'.")
	private Path optionsFile;

	@Option(names = { "-r", "--root" }, paramLabel = "ID", description = "Vertice di partenza (ripetibile).")
	private List<String> roots;

	@Option(names = { "-d", "--depth" }, paramLabel = "N", description = "Passi dai vertici di partenza (predefinito 1).")
	private Integer depth;

	@Option(names = "--direction", paramLabel = "VERSO", description = "Archi seguiti dai vertici di partenza: out, in, both (predefinito).")
	private GraphRenderOptions.Direction direction;

	@Option(names = "--type", split = ",", paramLabel = "TIPO", description = "Solo i vertici con questi valori della proprietà 'type'.")
	private List<String> types;

	@Option(names = "--exclude-type", split = ",", paramLabel = "TIPO", description = "Esclude i vertici con questi valori di 'type'.")
	private List<String> excludeTypes;

	@Option(names = "--label", split = ",", paramLabel = "ETICHETTA", description = "Solo i vertici con queste etichette.")
	private List<String> labels;

	@Option(names = "--directory", split = ",", paramLabel = "CARTELLA", description = "Solo i vertici in queste cartelle (e sottocartelle).")
	private List<String> directories;

	@Option(names = "--match", paramLabel = "REGEX", description = "Solo i vertici con id, nome o display name che contengono la regex.")
	private String match;

	@Option(names = "--where", paramLabel = "CHIAVE=REGEX", description = "Solo i vertici con la proprietà che contiene la regex (ripetibile, in AND).")
	private Map<String, String> where;

	@Option(names = "--expression", paramLabel = "ESPRESSIONE", description = "Espressione (sintassi della console) che restituisce i vertici candidati.")
	private String expression;

	@Option(names = "--include-ephemeral", description = "Disegna anche i vertici e gli archi effimeri (bordo tratteggiato).")
	private Boolean includeEphemeral;

	@Option(names = "--no-isolated", description = "Non disegna i vertici senza archi disegnati.")
	private boolean noIsolated;

	@Option(names = "--max", paramLabel = "N", description = "Numero massimo di vertici (predefinito " + GraphRenderOptions.DEFAULT_MAX_VERTICES + ").")
	private Integer max;

	@Option(names = "--edge-label", split = ",", paramLabel = "ETICHETTA", description = "Solo gli archi con queste etichette.")
	private List<String> edgeLabels;

	@Option(names = "--exclude-edge-label", split = ",", paramLabel = "ETICHETTA", description = "Esclude gli archi con queste etichette.")
	private List<String> excludeEdgeLabels;

	@Option(names = "--props", split = ",", paramLabel = "GLOB", description = "Proprietà mostrate nei vertici (glob, predefinito '*'; '' = nessuna).")
	private List<String> props;

	@Option(names = "--edge-props", split = ",", paramLabel = "GLOB", description = "Proprietà mostrate sugli archi (glob, predefinito nessuna).")
	private List<String> edgeProps;

	@Option(names = "--include-runtime", description = "Mostra anche le proprietà di runtime ('run!...').")
	private Boolean includeRuntime;

	@Option(names = "--layout", paramLabel = "ALGORITMO", description = "layered (predefinito), force, stress, mrtree, radial.")
	private GraphRenderOptions.Algorithm algorithm;

	@Option(names = "--layout-direction", paramLabel = "VERSO", description = "right (predefinito), down, left, up.")
	private GraphRenderOptions.LayoutDirection layoutDirection;

	@Option(names = "--group-by", paramLabel = "CRITERIO", description = "Raggruppa i vertici: none (predefinito), directory, type.")
	private GraphRenderOptions.GroupBy groupBy;

	@Option(names = "--color-by", paramLabel = "CRITERIO", description = "Colore dei vertici: type (predefinito), label, directory, none.")
	private GraphRenderOptions.ColorBy colorBy;

	@Option(names = "--theme", paramLabel = "TEMA", description = "auto (predefinito, segue il visualizzatore), light, dark.")
	private GraphRenderOptions.Theme theme;

	@Option(names = "--title", paramLabel = "TITOLO", description = "Titolo del disegno.")
	private String title;

	@Option(names = "--highlight", split = ",", paramLabel = "ID", description = "Elementi da evidenziare (i vertici di partenza lo sono sempre).")
	private List<String> highlight;

	@Option(names = "--block-size", paramLabel = "BYTE", description = "Dimensione dei blocchi del trasferimento (predefinita ${DEFAULT-VALUE}).")
	private int blockSize = DataExchangeProtocol.DEFAULT_BLOCK_SIZE;

	private GraphRenderOptions options;

	@Override
	protected int execute(final WaldotClient client, final Printer printer) throws IOException {
		final GraphRendering rendering = client.graph().render(options, blockSize);
		if (rendering.hasWarnings()) {
			rendering.warnings().forEach(w -> logger.warn("server: {}", w));
		}
		final byte[] data = rendering.data();
		if (toStdout()) {
			final OutputStream stdout = new FileOutputStream(FileDescriptor.out);
			stdout.write(data);
			stdout.flush();
			logger.info("drawn {} vertices and {} edges ({} bytes, {})", rendering.vertices(), rendering.edges(),
					data.length, rendering.format());
			return 0;
		}
		Files.write(file, data);
		printer.single(rendering, r -> {
			final Map<String, Object> json = new LinkedHashMap<>();
			json.put("file", file.toString());
			json.put("format", r.format());
			json.put("size", r.size());
			json.put("vertices", r.vertices());
			json.put("edges", r.edges());
			json.put("blocks", r.blocks());
			json.put("warnings", r.warnings());
			return json;
		}, r -> "disegnati " + r.vertices() + " vertici e " + r.edges() + " archi (" + r.size() + " byte, "
				+ r.format() + ") in " + file, r -> file.toString());
		return 0;
	}

	private boolean toStdout() {
		return file == null || "-".equals(file.toString());
	}

	@Override
	protected void validate() {
		if (blockSize < DataExchangeProtocol.MIN_BLOCK_SIZE || blockSize > DataExchangeProtocol.MAX_BLOCK_SIZE) {
			throw usage("--block-size deve essere compreso tra " + DataExchangeProtocol.MIN_BLOCK_SIZE + " e "
					+ DataExchangeProtocol.MAX_BLOCK_SIZE);
		}
		try {
			options = optionsFile == null ? new GraphRenderOptions()
					: GraphRenderOptions.fromJson(Files.readString(optionsFile, StandardCharsets.UTF_8));
			applyFlags();
		} catch (final IOException e) {
			throw usage("impossibile leggere " + optionsFile + ": " + e.getMessage());
		} catch (final IllegalArgumentException e) {
			throw usage("opzioni non valide: " + e.getMessage());
		}
	}

	private void applyFlags() {
		if (format != null) {
			options.format(format);
		} else if (optionsFile == null && !toStdout()) {
			final String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
			if (name.endsWith(".dot") || name.endsWith(".gv")) {
				options.format(GraphRenderOptions.Format.DOT);
			}
		}
		final GraphRenderOptions.Vertices vertices = options.vertices();
		if (roots != null) {
			vertices.roots(roots);
		}
		if (depth != null) {
			vertices.depth(depth);
		}
		if (direction != null) {
			vertices.direction(direction);
		}
		if (types != null) {
			vertices.types(types);
		}
		if (excludeTypes != null) {
			vertices.excludeTypes(excludeTypes);
		}
		if (labels != null) {
			vertices.labels(labels);
		}
		if (directories != null) {
			vertices.directories(directories);
		}
		if (match != null) {
			vertices.match(match);
		}
		if (where != null) {
			final Map<String, String> merged = new LinkedHashMap<>(vertices.where());
			merged.putAll(where);
			vertices.where(merged);
		}
		if (expression != null) {
			vertices.expression(expression);
		}
		if (includeEphemeral != null) {
			vertices.includeEphemeral(includeEphemeral);
		}
		if (noIsolated) {
			vertices.includeIsolated(false);
		}
		if (max != null) {
			vertices.max(max);
		}
		if (edgeLabels != null) {
			options.edges().labels(edgeLabels);
		}
		if (excludeEdgeLabels != null) {
			options.edges().excludeLabels(excludeEdgeLabels);
		}
		if (props != null) {
			options.properties().vertex(props);
		}
		if (edgeProps != null) {
			options.properties().edge(edgeProps);
		}
		if (includeRuntime != null) {
			options.properties().includeRuntime(includeRuntime);
		}
		if (algorithm != null) {
			options.layout().algorithm(algorithm);
		}
		if (layoutDirection != null) {
			options.layout().direction(layoutDirection);
		}
		if (groupBy != null) {
			options.layout().groupBy(groupBy);
		}
		if (colorBy != null) {
			options.style().colorBy(colorBy);
		}
		if (theme != null) {
			options.style().theme(theme);
		}
		if (title != null) {
			options.style().title(title);
		}
		if (highlight != null) {
			options.style().highlight(highlight);
		}
	}
}
