package net.rossonet.waldot;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.zenoh.irpc.mpl.BaseAgentStore;
import net.rossonet.zenoh.irpc.mpl.ZenohHistoryStrategy;
import net.rossonet.zenoh.rpc.client.ZenohClientFacade;
import net.rossonet.zenoh.rpc.controller.AgentLifeCycleManager;

/**
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
@WaldotPlugin
public class WaldotZenohPlugin implements AutoCloseable, PluginListener {

	private final static Logger logger = LoggerFactory.getLogger(WaldotZenohPlugin.class);

	private ZenohHistoryStrategy historyStrategy;

	private AgentLifeCycleManager lifeCycleManager;

	protected WaldotNamespace waldotNamespace;

	@Override
	public void close() throws Exception {
		logger.info("WaldotZenohPlugin closed");
	}

	private void createZenohAssetObject(final WaldotNamespace waldotNamespace) {
		// crea la cartella zenoh
		final UaFolderNode zenohFolder = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId("asset.zenoh_folder"), waldotNamespace.generateQualifiedName("Zenoh"),
				LocalizedText.english("Zenoh BUS agent administration folder"));
		final ZenohClientFacade zenohClient = new ZenohClientFacade(
				waldotNamespace.getConfiguration().getZenohConfiguration() != null
						? new JSONObject(waldotNamespace.getConfiguration().getZenohConfiguration())
						: null);
		lifeCycleManager = new AgentLifeCycleManager(waldotNamespace.getGremlinGraph(),
				waldotNamespace.getOpcUaNodeContext(), waldotNamespace.generateNodeId("asset.zenoh_bus_manager"),
				waldotNamespace.generateQualifiedName("Zenoh Event Manager"),
				LocalizedText.english("Zenoh Agent Manager"),
				LocalizedText.english("Zenoh Client lifecycle agent manager"), //
				UInteger.valueOf(0), UInteger.valueOf(0), //
				UByte.valueOf(0), 1L, //
				new BaseAgentStore(), //
				zenohClient);
		waldotNamespace.getStorageManager().addNode(lifeCycleManager);
		zenohFolder.addOrganizes(lifeCycleManager);
		waldotNamespace.addAssetAgentNode(zenohFolder);
		if (waldotNamespace.getHistoryStrategy() instanceof ZenohHistoryStrategy) {
			this.historyStrategy = (ZenohHistoryStrategy) waldotNamespace.getHistoryStrategy();
			historyStrategy.initialize(lifeCycleManager);
			logger.info("ZenohHistoryStrategy initialized in WaldotZenohPlugin");
		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		// createZenohAssetObject(waldotNamespace);
	}

}
