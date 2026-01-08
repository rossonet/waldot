package net.rossonet.waldot.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;

import net.rossonet.waldot.api.rules.Rule;
import net.rossonet.waldot.api.rules.WaldotStepLogger;
import net.rossonet.waldot.utils.ThreadHelper;

public class RulesThreadManager implements AutoCloseable {

	private static final long TIME_OUT_LOG_FUTURE = 100L;

	private boolean active = true;

	private final Thread controlThread;

	private final ExecutorService executor;

	private final Logger logger;

	private final Map<NodeId, Rule> rules;

	private final Map<Future<WaldotStepLogger>, Long> runFutures = new HashMap<>();

	public RulesThreadManager(final Map<NodeId, Rule> rules, final Logger logger) {
		this.logger = logger;
		this.rules = rules;
		executor = ThreadHelper.newVirtualThreadExecutor();
		controlThread = getThread();
	}

	private synchronized void checkRules() throws InterruptedException {
		if (rules != null && !rules.isEmpty()) {
			for (final Rule rule : rules.values()) {
				if (rule.isDirty()) {
					final Callable<WaldotStepLogger> newRunner = rule.getNewRunner();
					final Future<WaldotStepLogger> future = executor.submit(newRunner);
					runFutures.put(future, System.currentTimeMillis() + rule.getExecutionTimeout());
				}
			}
		} else {
			logger.debug("No rules to check, waiting 5 seconds before next check");
			Thread.sleep(5000L);
		}
	}

	private synchronized void checkThread() {
		if (!runFutures.isEmpty()) {
			final long currentTime = System.currentTimeMillis();
			for (final Entry<Future<WaldotStepLogger>, Long> runner : runFutures.entrySet()) {
				if (runner.getValue() < currentTime) {
					runner.getKey().cancel(true);
					logger.warn("Rule execution timeout");
				}
			}
			final Set<Future<WaldotStepLogger>> toRemove = new HashSet<>();
			for (final Entry<Future<WaldotStepLogger>, Long> runner : runFutures.entrySet()) {
				if (runner.getKey().isDone()) {
					toRemove.add(runner.getKey());
				}
			}
			for (final Future<WaldotStepLogger> future : toRemove) {
				try {
					final WaldotStepLogger waldotStepRegister = future.get(TIME_OUT_LOG_FUTURE, TimeUnit.MILLISECONDS);
					if (waldotStepRegister != null) {
						logSteps(waldotStepRegister);
					} else {
						logger.warn("The step log is null for future: " + future);
					}
				} catch (final Exception e) {
					logger.error("Error during the step log trascoding", e);
				}
				runFutures.remove(future);
			}
		}

	}

	@Override
	public void close() throws Exception {
		stop();
	}

	private Thread getThread() {
		return ThreadHelper.ofVirtual().name("RULES TM", 0).unstarted(() -> {
			Thread.currentThread().setName("RULES TM");
			Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
			logger.info("Rules Engine Thread Manager started with thread name " + Thread.currentThread().getName()
					+ " and priority " + Thread.currentThread().getPriority());
			while (active) {
				try {
					checkRules();
					checkThread();
				} catch (final Throwable e) {
					logger.error("Error in Rules Thread Manager", e);
				}
			}
		});
	}

	private void logSteps(final WaldotStepLogger waldotStepRegister) {
		logger.info(waldotStepRegister.toString());
	}

	public void start() {
		controlThread.start();
	}

	public void stop() {
		if (controlThread != null) {
			active = false;
			logger.info("Stopping Rules Thread Manager");
		}
		if (executor != null) {
			executor.shutdown();
			logger.info("Rules Thread Manager stopped");
		}
	}

}
