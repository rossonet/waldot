package net.rossonet.waldot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.server.Settings.SerializerSettings;
import org.apache.tinkerpop.gremlin.server.channel.WsAndHttpChannelizer;
import org.apache.tinkerpop.gremlin.util.ser.GraphBinaryMessageSerializerV1;
import org.apache.tinkerpop.gremlin.util.ser.GraphSONMessageSerializerV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;

@WaldotPlugin
public class WaldotTinkerPopPlugin implements PluginListener {
	static WaldotNamespace mainNamespace;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private GremlinServer server;

	private WaldotNamespace waldotNamespace;

	@Override
	public void initialize(WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		logger.info("Initializing Waldot TinkerPop Plugin");
	}

	@Override
	public void start() {
		final Settings overriddenSettings = new Settings();
		overriddenSettings.channelizer = WsAndHttpChannelizer.class.getName();
		overriddenSettings.graphManager = WaldotGraphManager.class.getName();
		final Map<String, Object> arg = new HashMap<>();
		arg.put("graph", waldotNamespace.getGremlinGraph());
		arg.put("g", waldotNamespace.getGremlinGraph().traversal());
		final Map<String, Object> method2arg = new HashMap<>();
		method2arg.put("bindings", arg);
		overriddenSettings.scriptEngines.get("gremlin-groovy").plugins
				.put("org.apache.tinkerpop.gremlin.jsr223.BindingsGremlinPlugin", method2arg);
		final SerializerSettings settingSerializerJson = new SerializerSettings();
		settingSerializerJson.className = GraphSONMessageSerializerV1.class.getName();
		settingSerializerJson.config = Collections.singletonMap("ioRegistries",
				List.of("net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1",
						"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV2",
						"net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV3"));
		final SerializerSettings settingSerializerBin = new SerializerSettings();
		settingSerializerBin.className = GraphBinaryMessageSerializerV1.class.getName();
		settingSerializerBin.config = new HashMap<>();
		settingSerializerBin.config.put("ioRegistries",
				List.of("net.rossonet.waldot.gremlin.opcgraph.structure.OpcIoRegistryV1"));
		settingSerializerBin.config.put("custom", List.of(
				"org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;net.rossonet.waldot.gremlin.opcgraph.ser.NodeIdCustomTypeSerializer"));
		overriddenSettings.serializers = new ArrayList<>();
		overriddenSettings.serializers.add(settingSerializerBin);
		overriddenSettings.serializers.add(settingSerializerJson);
		WaldotTinkerPopPlugin.mainNamespace = waldotNamespace;
		this.server = new WaldotGremlinServer(overriddenSettings);
		try {
			server.start().get();
			logger.info("Gremlin Server started: " + server.toString());
		} catch (final Exception e) {
			logger.error("Failed to start Gremlin Server", e);
		}
	}

	@Override
	public void stop() {
		if (server != null) {
			try {
				server.stop().get();
			} catch (final Exception e) {
				logger.error("Failed to stop Gremlin Server", e);
			}
		}
	}

}
