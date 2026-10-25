package net.rossonet.waldot.test.containers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

/**
 * Base for the JUnit extensions that start an external service with
 * Testcontainers. The container is started once per JVM (singleton) and
 * stopped by the Testcontainers resource reaper at the end of the JVM. Tests
 * are disabled (not failed) when Docker is not available.
 */
abstract class AbstractSharedContainerExtension implements ExecutionCondition {

	private static final ConcurrentHashMap<String, GenericContainer<?>> CONTAINERS = new ConcurrentHashMap<>();

	private static volatile Boolean dockerAvailable;

	static boolean isDockerAvailable() {
		if (dockerAvailable == null) {
			synchronized (AbstractSharedContainerExtension.class) {
				if (dockerAvailable == null) {
					boolean available;
					try {
						available = DockerClientFactory.instance().isDockerAvailable();
					} catch (final Throwable t) {
						available = false;
					}
					dockerAvailable = available;
				}
			}
		}
		return dockerAvailable;
	}

	@SuppressWarnings("resource")
	static GenericContainer<?> shared(final String key, final Supplier<GenericContainer<?>> factory) {
		return CONTAINERS.computeIfAbsent(key, k -> {
			final GenericContainer<?> container = factory.get();
			container.start();
			return container;
		});
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext context) {
		return isDockerAvailable() ? ConditionEvaluationResult.enabled("Docker available")
				: ConditionEvaluationResult.disabled("Docker not available, " + serviceName() + " test skipped");
	}

	protected abstract String serviceName();
}
