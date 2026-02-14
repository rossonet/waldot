package net.rossonet.waldot.zenoh;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.Config;
import io.zenoh.Session;
import io.zenoh.Zenoh;
import io.zenoh.config.WhatAmI;
import io.zenoh.exceptions.ZError;
import io.zenoh.scouting.Hello;
import io.zenoh.scouting.ScoutOptions;
import net.rossonet.waldot.WaldotZenohPlugin;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;

public class ZenohClientVertex extends AbstractOpcVertex implements AutoCloseable {
	public static Session createClient(final Config zenohConfig, final boolean debug) throws ZError {
		if (debug) {
			System.setProperty("RUST_LOG", "zenoh=debug");
			Zenoh.initLogFromEnvOr("debug");
		} else {
			Zenoh.initLogFromEnvOr("error");
		}
		final Session session = Zenoh.open(zenohConfig);
		return session;
	}

	public static Session createClient(final JSONObject zenohClientConfig) throws ZError {
		if (zenohClientConfig == null) {
			final Config config = Config.loadDefault();
			return createClient(config, false);
		} else {
			final Config config = Config.fromJson5(zenohClientConfig.toString());
			return createClient(config, false);
		}
	}

	public static Config createDefaultConfig() {
		return Config.loadDefault();
	}

	public static void generateParameters(WaldotNamespace waldotNamespace, UaObjectTypeNode dockerTypeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				WaldotZenohPlugin.ZENOH_CLIENT_CONFIGURATION_PATH_FIELD, NodeIds.String);
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				WaldotZenohPlugin.ZENOH_CLIENT_CONFIGURATION_JSON_FIELD, NodeIds.String);
	}

	public static List<Hello> scouting(final long milliseconds) throws ZError, InterruptedException {
		final ScoutOptions scoutOptions = new ScoutOptions();
		scoutOptions.setWhatAmI(Set.of(WhatAmI.Peer, WhatAmI.Router, WhatAmI.Client));
		final var scout = Zenoh.scout(scoutOptions);
		final BlockingQueue<Optional<Hello>> receiver = scout.getReceiver();
		final List<Hello> hellos = new ArrayList<>();
		try {
			final long start = System.currentTimeMillis();
			while (System.currentTimeMillis() - start < milliseconds) {
				final Optional<Hello> wrapper = receiver.take();
				if (wrapper.isEmpty()) {
					break;
				}
				final Hello hello = wrapper.get();
				hellos.add(hello);
			}
		} finally {
			scout.stop();
		}
		return hellos;
	}

	private String baseDirectory;
	private String clientConfigurationJson;
	private final QualifiedProperty<String> clientConfigurationJsonProperty;
	private String clientConfigurationPath;

	private final QualifiedProperty<String> clientConfigurationPathProperty;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final WaldotNamespace waldotNamespace;

	public ZenohClientVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId, QualifiedName browseName,
			LocalizedText displayName, LocalizedText description, UInteger writeMask, UInteger userWriteMask,
			UByte eventNotifier, long version, Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		waldotNamespace = graph.getWaldotNamespace();
		baseDirectory = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				MiloStrategy.DIRECTORY_PARAMETER.toLowerCase());
		if (baseDirectory == null || baseDirectory.isEmpty()) {
			baseDirectory = "zenoh";
		}
		clientConfigurationPath = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotZenohPlugin.ZENOH_CLIENT_CONFIGURATION_PATH_FIELD.toLowerCase());
		clientConfigurationPathProperty = new QualifiedProperty<String>(getNamespace().getNamespaceUri(),
				WaldotZenohPlugin.ZENOH_CLIENT_CONFIGURATION_PATH_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				String.class);
		setProperty(clientConfigurationPathProperty, clientConfigurationPath);
		clientConfigurationJson = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotZenohPlugin.ZENOH_CLIENT_CONFIGURATION_JSON_FIELD.toLowerCase());
		clientConfigurationJsonProperty = new QualifiedProperty<String>(getNamespace().getNamespaceUri(),
				WaldotZenohPlugin.ZENOH_CLIENT_CONFIGURATION_JSON_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				String.class);
		setProperty(clientConfigurationJsonProperty, clientConfigurationJson);
	}

	@Override
	public Object clone() {
		return new ZenohClientVertex(graph, getNodeContext(), getNodeId(), getBrowseName(), getDisplayName(),
				getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(),
				getPropertiesAsStringArray());
	}

	@Override
	public void close() throws Exception {
		// TODO: implement any necessary cleanup logic here
	}

	@Override
	public void notifyPropertyValueChanging(String label, DataValue value) {
		super.notifyPropertyValueChanging(label, value);
		if (label.equals(WaldotZenohPlugin.ZENOH_CLIENT_CONFIGURATION_PATH_FIELD.toLowerCase())) {
			clientConfigurationPath = value.getValue().getValue().toString();
			setProperty(clientConfigurationPathProperty, clientConfigurationPath);
		}
		if (label.equals(WaldotZenohPlugin.ZENOH_CLIENT_CONFIGURATION_JSON_FIELD.toLowerCase())) {
			clientConfigurationJson = value.getValue().getValue().toString();
			setProperty(clientConfigurationJsonProperty, clientConfigurationJson);
		}

	}
}
