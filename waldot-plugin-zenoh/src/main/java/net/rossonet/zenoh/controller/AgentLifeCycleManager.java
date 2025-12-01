package net.rossonet.zenoh.controller;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.handlers.Callback;
import io.zenoh.sample.Sample;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.client.ZenohClientFacade;

/**
 * Manages the lifecycle of Zenoh Agents on Waldot OPC UA Server * @Author
 * Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class AgentLifeCycleManager extends AbstractOpcVertex {
	public static class AgentDiscoveryHandler implements Callback<Sample> {

		private final AgentLifeCycleManager lifeCycleManager;

		public AgentDiscoveryHandler(AgentLifeCycleManager lifeCycleManager) {
			this.lifeCycleManager = lifeCycleManager;
		}

		@Override
		public void run(Sample sample) {
			JSONObject payloadJson = null;
			try {
				payloadJson = new JSONObject(sample.getPayload().toString());
			} catch (final Exception e) {
				logger.error("Error parsing discovery message payload: {}", sample.getPayload(), e);
			}
			if (payloadJson == null) {
				logger.warn("Received invalid discovery message: {}", sample.getPayload());
				return;
			} else {
				lifeCycleManager.discoveryFromAgentReceived(sample.getKeyExpr().toString(), payloadJson);
			}
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(AgentLifeCycleManager.class);
	private boolean active = true;
	private boolean connected = false;
	private final WaldotGraph graph;
	private final Thread lifeCycleThread;
	private final AgentStore agentStore;
	private final ZenohClientFacade zenohClient;
	private Vertex zenohClientVertex;

	public AgentLifeCycleManager(final WaldotGraph graph, final UaNodeContext context, final NodeId nodeId,
			final QualifiedName browseName, final LocalizedText displayName, final LocalizedText description,
			final UInteger writeMask, final UInteger userWriteMask, final UByte eventNotifier, final long version,
			final AgentStore agentStore, final ZenohClientFacade zenohClient) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		this.graph = graph;
		createZenohClientVertexInGraph();
		this.agentStore = agentStore;
		this.zenohClient = zenohClient;
		zenohClient.setLifeCycleManager(this);
		agentStore.setAgentLifeCycleManager(this);
		active = true;
		lifeCycleThread = new Thread(() -> {
			while (active) {
				try {
					checkClientConnection();
					if (zenohClient.isConnected()) {
						agentStore.periodicallyCheck();
						setConnected(true);
					} else {
						setConnected(false);
					}
					Thread.sleep(agentStore.getPeriodicallyCheckIntervalMs());
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
		lifeCycleThread.start();
	}

	private void checkClientConnection() {
		if (!zenohClient.isConnected()) {
			try {
				zenohClient.init();
			} catch (final Exception e) {
				logger.error("Error initializing Zenoh client for WaldotZenohPlugin", e);
			}
		}

	}

	@Override
	public Object clone() {
		return new AgentLifeCycleManager(graph, getNodeContext(), getNodeId(), getBrowseName(), getDisplayName(),
				getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(), agentStore,
				zenohClient);
	}

	private void createZenohClientVertexInGraph() {
		zenohClientVertex = graph.addVertex("id", "zenoh_client", "label", "local client", "description",
				"Zenoh local client information", "directory", ZenohHelper.BASE_OPCUA_DIRECTORY, "online", "false");

	}

	public void discoveryFromAgentReceived(String topic, JSONObject payload) {
		logger.info("discovery message received on topic {}: {}", topic, payload.toString(2));
		registerNewAgent(payload);
	}

	public WaldotGraph getGraph() {
		return graph;

	}

	public ZenohClientFacade getZenohClient() {
		return zenohClient;

	}

	private void registerNewAgent(JSONObject discoveryMessage) {
		ZenohAgent agent;
		try {
			agent = ZenohAgent.fromDiscoveryMessage(this, discoveryMessage, ZenohHelper.AGENTS_OPCUA_DIRECTORY);
		} catch (final Exception e) {
			logger.warn("Unable to register new agent from discovery message: {}", discoveryMessage.toString(2), e);
			return;
		}
		if (agent != null) {
			if (agentStore.registerNewAgent(agent)) {
				logger.info("Registered new agent from discovery message: {}", discoveryMessage.toString(2));
				agent.setManagedVertex(graph.addVertex(agent.getAgentVertexProperties()));
				agent.sendAcknowLedgeMessage();
			} else {
				logger.warn("Agent from discovery message not registered by agentStore: {}",
						discoveryMessage.toString(2));
			}
		} else {
			logger.warn("Unable to register new agent from discovery message: {}", discoveryMessage.toString(2));
		}

	}

	private void setConnected(boolean newStatus) {
		if (connected != newStatus) {
			connected = newStatus;
			zenohClientVertex.property("online", Boolean.toString(connected));
			logger.info("Zenoh Client connection status changed to {}", Boolean.toString(connected));
			if (newStatus) {
				subscribeTopics();
				logger.info("Zenoh Client subscribed to topics");
			}
		}

	}

	private void subscribeTopics() {
		zenohClient.subscribe(ZenohHelper.AGENTS_DISCOVERY_TOPIC, new AgentDiscoveryHandler(this));

	}

}
