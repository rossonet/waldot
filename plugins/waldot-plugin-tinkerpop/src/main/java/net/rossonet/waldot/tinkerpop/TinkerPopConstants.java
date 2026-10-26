package net.rossonet.waldot.tinkerpop;

/**
 * Constants of the TinkerPop plugin: vertex type label, property keys, status
 * values, default values and parameter descriptions.
 *
 * <p>
 * The <code>*_DESCRIPTION</code> constants are shared by the OPC UA command
 * that creates the vertices and by
 * {@link TinkerPopVertex#generateParameters} that registers the parameters on
 * the vertex type node.
 * </p>
 *
 * @author Andrea Ambrosini - Zona Autonoma srl
 */
public final class TinkerPopConstants {

	/** Default bind address: all the interfaces */
	public static final String _DEFAULT_BIND_HOST = "0.0.0.0";

	/** Status value: server running */
	public static final String _STATUS_RUNNING = "Running";

	/** Status value: server stopped */
	public static final String _STATUS_STOPPED = "Stopped";

	/** OPC UA folder of the plugin commands. */
	public static final String BASE_CMD_DIRECTORY = "tinkerpop";

	/** Property name for bind address/host */
	public static final String BIND_HOST_FIELD = "Bind";

	/** Description of the bind address */
	public static final String BIND_HOST_FIELD_DESCRIPTION = "Address (IP or host name) of the network interface on which "
			+ "the embedded Gremlin Server listens; 0.0.0.0 means all the interfaces. "
			+ "Changing it at runtime restarts the server. Default: "
			+ _DEFAULT_BIND_HOST;

	/** Default server port */
	public static final int DEFAULT_PORT = 1025;

	/** Vertex type label for Gremlin Server vertices */
	public static final String GREMLIN_TYPE_LABEL = "gremlin";

	/** Property name for server port */
	public static final String PORT_FIELD = "Port";

	/** Description of the server port */
	public static final String PORT_FIELD_DESCRIPTION = "TCP port (1-65535) of the embedded Gremlin Server, "
			+ "that serves WebSocket and HTTP requests with the GraphSON v3 and GraphBinary v1 serializers; "
			+ "the port must be free on the bind address, otherwise the Status of the vertex becomes Failed. "
			+ "Changing it at runtime restarts the server. Default: "
			+ DEFAULT_PORT;

	/** Status value: server failed to start */
	public static final String STATUS_FAILED = "Failed";

	/** Property name for server status */
	public static final String STATUS_FIELD = "Status";

	/** Description of the server status */
	public static final String STATUS_FIELD_DESCRIPTION = "State of the embedded Gremlin Server, updated by the "
			+ "vertex: " + _STATUS_RUNNING + ", " + _STATUS_STOPPED + " or "
			+ STATUS_FAILED + " (for example when the port is already in use)";

	private TinkerPopConstants() {
	}

}
