package old;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.Session;
import io.zenoh.exceptions.ZError;
import io.zenoh.handlers.Callback;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.pubsub.CallbackSubscriber;
import io.zenoh.sample.Sample;
import net.rossonet.zenoh.ZenohHelper;

public class ZenohClientFacade {
	private final static Logger logger = LoggerFactory.getLogger(ZenohClientFacade.class);

	private AgentLifeCycleManager agentLifeCycleManager;

	private transient Session session;

	private final JSONObject zenohConfiguration;

	public ZenohClientFacade(final JSONObject zenohConfiguration) {
		this.zenohConfiguration = zenohConfiguration;
	}

	public Session getSession() {
		return session;
	}

	public void init() throws ZError {
		if (agentLifeCycleManager == null) {
			throw new IllegalStateException("LifeCycleManager not set");
		}
		this.session = ZenohHelper.createClient(zenohConfiguration);
		logger.info("Zenoh session created with router {}", session.info().routersZid());
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

	public void setLifeCycleManager(final AgentLifeCycleManager agentLifeCycleManager) {
		this.agentLifeCycleManager = agentLifeCycleManager;

	}

	public CallbackSubscriber subscribe(final String topic, final Callback<Sample> handler) {
		if (!isConnected()) {
			throw new IllegalStateException("Zenoh client not connected");
		}
		try {
			return session.declareSubscriber(KeyExpr.tryFrom(topic), handler);
		} catch (final ZError e) {
			logger.error("Error declaring subscriber on topic {}", topic, e);
			throw new RuntimeException(e);
		}

	}

}
