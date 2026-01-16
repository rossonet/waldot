package net.rossonet.waldot.tinkerpop;

import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.util.Gremlin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaldotGremlinServer extends GremlinServer {
	public static String getHeader() {
		final StringBuilder builder = new StringBuilder();
		builder.append(Gremlin.version() + "\r\n");
		builder.append("WaldOT\r\n");
		return builder.toString();
	}

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public WaldotGremlinServer(Settings settings) {
		super(settings);
		logger.info("Initializing Waldot Gremlin Server");
	}

}
