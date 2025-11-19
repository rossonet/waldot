package net.rossonet.zenoh.client;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.Session;
import io.zenoh.exceptions.ZError;
import io.zenoh.handlers.Callback;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.pubsub.PutOptions;
import io.zenoh.sample.Sample;
import net.rossonet.zenoh.WaldotZenohException;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.controller.AgentLifeCycleManager;

public class ZenohClientFacade {
	private final static Logger logger = LoggerFactory.getLogger(ZenohClientFacade.class);

	private AgentLifeCycleManager agentLifeCycleManager;

	private transient Session session;

	public Session getSession() {
		return session;
	}

	public void init() throws ZError {
		if (agentLifeCycleManager == null) {
			throw new IllegalStateException("LifeCycleManager not set");
		}
		this.session = ZenohHelper.createClient();
		logger.info("Zenoh session created {}", session.info());
	}

	public boolean isConnected() {
		try {
			return session != null && !session.isClosed() && session.info().routersZid() != null
					&& session.info().routersZid().size() > 0;
		} catch (final Throwable e) {
			logger.error("Error checking Zenoh session connection", e);
		}
		return false;
	}

	public void sendMessage(String topic, JSONObject message, PutOptions putOptions) throws WaldotZenohException {
		try {
			session.put(KeyExpr.tryFrom(topic), message.toString(), putOptions);
		} catch (final ZError e) {
			throw new WaldotZenohException("Error sending acknowledge message to topic " + topic, e);
		}

	}

	public void setLifeCycleManager(AgentLifeCycleManager agentLifeCycleManager) {
		this.agentLifeCycleManager = agentLifeCycleManager;

	}

	public void subscribe(String topic, Callback<Sample> handler) {
		if (!isConnected()) {
			throw new IllegalStateException("Zenoh client not connected");
		}
		try {
			session.declareSubscriber(KeyExpr.tryFrom(topic), handler);
		} catch (final ZError e) {
			logger.error("Error declaring subscriber on topic {}", topic, e);
			throw new RuntimeException(e);
		}

	}

}
