package net.rossonet.zenoh.rpc.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.Session;
import io.zenoh.Zenoh;
import io.zenoh.config.WhatAmI;
import io.zenoh.exceptions.ZError;
import io.zenoh.handlers.Callback;
import io.zenoh.keyexpr.KeyExpr;
import io.zenoh.pubsub.CallbackSubscriber;
import io.zenoh.sample.Sample;
import io.zenoh.scouting.Hello;
import io.zenoh.scouting.ScoutOptions;
import net.rossonet.zenoh.ZenohHelper;
import net.rossonet.zenoh.rpc.controller.AgentLifeCycleManager;

public class ZenohClientFacade {
	private final static Logger logger = LoggerFactory.getLogger(ZenohClientFacade.class);

	public static List<Hello> scouting(final long milliseconds) throws ZError, InterruptedException {
		final ScoutOptions scoutOptions = new ScoutOptions();
		scoutOptions.setWhatAmI(Set.of(WhatAmI.Peer, WhatAmI.Router));
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
