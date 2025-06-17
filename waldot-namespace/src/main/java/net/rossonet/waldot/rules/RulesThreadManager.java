package net.rossonet.waldot.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;

import net.rossonet.waldot.api.rules.Rule;
import net.rossonet.waldot.api.rules.WaldotStepLogger;

public class RulesThreadManager implements AutoCloseable {

	private boolean active = true;

	private final Thread controlThread;

	private final ExecutorService executor;

	private final Logger logger;

	private final Map<NodeId, Rule> rules;

	private final Map<Future<WaldotStepLogger>, Long> timeoutRunners = new HashMap<>();

	public RulesThreadManager(final Map<NodeId, Rule> rules, final Logger logger) {
		this.logger = logger;
		this.rules = rules;
		executor = Executors.newThreadPerTaskExecutor(new DefaultThreadFactory());
		controlThread = getThread();

	}

	private synchronized void checkRules() throws InterruptedException {
		if (rules != null && !rules.isEmpty()) {
			for (final Rule rule : rules.values()) {
				if (rule.isDirty()) {
					final Callable<WaldotStepLogger> newRunner = rule.getNewRunner();
					final Future<WaldotStepLogger> future = executor.submit(newRunner);
					timeoutRunners.put(future, System.currentTimeMillis() + rule.getExecutionTimeout());
				}
			}
		} else {
			logger.debug("No rules to check, waiting 5 seconds before next check");
			Thread.sleep(5000L);
		}
	}

	private synchronized void checkThread() {
		if (!timeoutRunners.isEmpty()) {
			final long currentTime = System.currentTimeMillis();
			for (final Entry<Future<WaldotStepLogger>, Long> runner : timeoutRunners.entrySet()) {
				if (runner.getValue() < currentTime) {
					runner.getKey().cancel(true);
				}
			}
			final Set<Future<WaldotStepLogger>> toRemove = new HashSet<>();
			for (final Entry<Future<WaldotStepLogger>, Long> runner : timeoutRunners.entrySet()) {
				if (runner.getKey().isDone()) {
					toRemove.add(runner.getKey());
				}
			}
			for (final Future<WaldotStepLogger> future : toRemove) {
				try {
					logSteps(future.get(100, TimeUnit.MILLISECONDS));
				} catch (final Exception e) {
					logger.error("Error during the step log trascoding", e);
				}
				timeoutRunners.remove(future);
			}
		}

	}

	@Override
	public void close() throws Exception {
		stop();

	}

	private Thread getThread() {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				Thread.currentThread().setName("RulesThreadManager");
				Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
				while (active) {
					try {
						checkRules();
						checkThread();
					} catch (final Throwable e) {
						logger.error("Error in RulesThreadManager", e);
					}
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
		}

	}

}
