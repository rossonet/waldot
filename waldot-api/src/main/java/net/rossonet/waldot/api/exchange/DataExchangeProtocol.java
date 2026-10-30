package net.rossonet.waldot.api.exchange;

/**
 * Names and limits of the OPC UA base commands used to exchange structured
 * data between a WaldOT server and its clients (structured query results,
 * element descriptions, graph snapshots, graph drawings).
 * <p>
 * The constants are shared by the server commands (waldot-namespace) and by
 * the client library (waldot-client), so that both sides always agree on
 * browse names and argument order.
 * </p>
 *
 * <h2>Commands</h2>
 * <ul>
 * <li>{@value #QUERY_JSON_COMMAND} (folder {@value #INFORMATION_DIRECTORY}):
 * input {@code command} (JEXL expression), outputs {@code json} (result
 * serialized as typed GraphSON v3), {@code error};</li>
 * <li>{@value #DESCRIBE_ELEMENT_COMMAND} (folder
 * {@value #INFORMATION_DIRECTORY}): input {@code element} (NodeId or plain
 * id), outputs {@code json} (plain JSON description), {@code error};</li>
 * <li>{@value #EXPORT_GRAPH_COMMAND} (folder {@value #MANAGE_DIRECTORY}):
 * inputs {@code compress}, {@code includeEphemeral}, {@code blockSize};
 * outputs {@code data} (base64, single block), {@code transferId} (chunked
 * transfer), {@code blocks}, {@code size}, {@code vertices}, {@code edges},
 * {@code error};</li>
 * <li>{@value #READ_GRAPH_BLOCK_COMMAND} (folder {@value #MANAGE_DIRECTORY}):
 * inputs {@code transferId}, {@code index}; outputs {@code data},
 * {@code last}, {@code error};</li>
 * <li>{@value #IMPORT_GRAPH_COMMAND} (folder {@value #MANAGE_DIRECTORY}):
 * inputs {@code transferId}, {@code data}, {@code last},
 * {@code clearBefore}; outputs {@code transferId}, {@code vertices},
 * {@code edges}, {@code error};</li>
 * <li>{@value #CLEAR_GRAPH_COMMAND} (folder {@value #MANAGE_DIRECTORY}): no
 * inputs; outputs {@code vertices}, {@code edges} (removed elements),
 * {@code error};</li>
 * <li>{@value #RENDER_GRAPH_COMMAND} (folder {@value #INFORMATION_DIRECTORY}):
 * inputs {@code options} (JSON, see {@link GraphRenderOptions}),
 * {@code compress}, {@code blockSize}; outputs {@code data} (base64, single
 * block), {@code transferId} (chunked transfer read with
 * {@value #READ_GRAPH_BLOCK_COMMAND}), {@code blocks}, {@code size},
 * {@code format} ({@code svg} or {@code dot}), {@code vertices},
 * {@code edges} (drawn elements), {@code warnings} (one per line),
 * {@code error}.</li>
 * </ul>
 * All the arguments are strings; an empty {@code error} means success.
 */
public final class DataExchangeProtocol {

	/** Folder of the read only commands. */
	public static final String INFORMATION_DIRECTORY = "information";
	/** Folder of the management commands. */
	public static final String MANAGE_DIRECTORY = "manage";

	public static final String QUERY_JSON_COMMAND = "query json";
	public static final String DESCRIBE_ELEMENT_COMMAND = "describe element";
	public static final String EXPORT_GRAPH_COMMAND = "export graph";
	public static final String READ_GRAPH_BLOCK_COMMAND = "read graph block";
	public static final String IMPORT_GRAPH_COMMAND = "import graph";
	public static final String CLEAR_GRAPH_COMMAND = "clear graph";
	public static final String RENDER_GRAPH_COMMAND = "render graph";

	/** Input/output argument names. */
	public static final String ARG_COMMAND = "command";
	public static final String ARG_ELEMENT = "element";
	public static final String ARG_JSON = "json";
	public static final String ARG_ERROR = "error";
	public static final String ARG_COMPRESS = "compress";
	public static final String ARG_INCLUDE_EPHEMERAL = "includeEphemeral";
	public static final String ARG_BLOCK_SIZE = "blockSize";
	public static final String ARG_DATA = "data";
	public static final String ARG_TRANSFER_ID = "transferId";
	public static final String ARG_BLOCKS = "blocks";
	public static final String ARG_SIZE = "size";
	public static final String ARG_VERTICES = "vertices";
	public static final String ARG_EDGES = "edges";
	public static final String ARG_INDEX = "index";
	public static final String ARG_LAST = "last";
	public static final String ARG_CLEAR_BEFORE = "clearBefore";
	public static final String ARG_OPTIONS = "options";
	public static final String ARG_FORMAT = "format";
	public static final String ARG_WARNINGS = "warnings";

	/** Serialization format of the snapshots (one vertex per line). */
	public static final String SNAPSHOT_FORMAT = "graphson-v3-adjacency";

	/**
	 * Default size (bytes of the possibly compressed payload) of a block: a
	 * snapshot not bigger than this is returned in a single call.
	 */
	public static final int DEFAULT_BLOCK_SIZE = 256 * 1024;
	/** Smallest accepted block size. */
	public static final int MIN_BLOCK_SIZE = 1024;
	/** Biggest accepted block size (OPC UA messages must stay small). */
	public static final int MAX_BLOCK_SIZE = 4 * 1024 * 1024;

	/**
	 * System property that disables the data exchange commands
	 * ({@code -Dwaldot.commands.exchange.enabled=false}).
	 */
	public static final String ENABLED_PROPERTY = "waldot.commands.exchange.enabled";

	/**
	 * System property: maximum size in bytes of a snapshot uploaded with
	 * {@value #IMPORT_GRAPH_COMMAND} (default 256 MiB).
	 */
	public static final String MAX_IMPORT_SIZE_PROPERTY = "waldot.commands.exchange.max-import-size";

	/**
	 * System property: time to live in milliseconds of a pending transfer
	 * (default 10 minutes).
	 */
	public static final String TRANSFER_TTL_PROPERTY = "waldot.commands.exchange.transfer-ttl-msec";

	/**
	 * System property: most vertices drawn by {@value #RENDER_GRAPH_COMMAND},
	 * whatever the requested {@code vertices.max} (default
	 * {@value #DEFAULT_MAX_RENDER_VERTICES}).
	 */
	public static final String MAX_RENDER_VERTICES_PROPERTY = "waldot.commands.exchange.max-render-vertices";

	/** Default of {@value #MAX_RENDER_VERTICES_PROPERTY}. */
	public static final int DEFAULT_MAX_RENDER_VERTICES = 5000;

	/**
	 * @return true unless the commands were disabled with
	 *         {@value #ENABLED_PROPERTY}
	 */
	public static boolean isEnabled() {
		return !"false".equalsIgnoreCase(System.getProperty(ENABLED_PROPERTY, "true").trim());
	}

	private DataExchangeProtocol() {
	}
}
