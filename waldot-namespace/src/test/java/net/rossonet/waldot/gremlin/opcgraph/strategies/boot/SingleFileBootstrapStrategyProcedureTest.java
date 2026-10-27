package net.rossonet.waldot.gremlin.opcgraph.strategies.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.BootstrapStrategy.AgentStatus;

/**
 * Status of {@link SingleFileBootstrapStrategy} without starting an OPC UA server.
 */
class SingleFileBootstrapStrategyProcedureTest {

	@TempDir
	Path tmp;

	@Test
	void expressionFailureIsFaultAndDoesNotStopLineMode() throws Exception {
		final Path file = tmp.resolve("boot.conf");
		Files.writeString(file, "ok\nbad\nok-after\n");
		final List<String> executed = new ArrayList<>();
		final SingleFileBootstrapStrategy strategy = strategy(file.toUri().toString(), expression -> {
			executed.add(expression);
			if ("bad".equals(expression)) {
				throw new IllegalStateException("bad line");
			}
			return "done";
		});

		strategy.runBootstrapProcedure();

		assertEquals(List.of("ok", "bad", "ok-after"), executed);
		assertEquals(AgentStatus.FAULT, strategy.getAgentStatus());
	}

	@Test
	void missingFileIsFault() {
		final SingleFileBootstrapStrategy strategy = strategy(tmp.resolve("missing.conf").toUri().toString(),
				expression -> expression);
		strategy.runBootstrapProcedure();
		assertEquals(AgentStatus.FAULT, strategy.getAgentStatus());
	}

	@Test
	void remoteReadTimeoutIsFault() throws Exception {
		final String previousConnect = System.setProperty(SingleFileBootstrapStrategy.CONNECT_TIMEOUT_PROPERTY, "500");
		final String previousRead = System.setProperty(SingleFileBootstrapStrategy.READ_TIMEOUT_PROPERTY, "200");
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			final Thread acceptor = new Thread(() -> {
				try (var socket = server.accept()) {
					Thread.sleep(5_000);
				} catch (final Exception ignored) {
					// the client times out and closes first
				}
			});
			acceptor.setDaemon(true);
			acceptor.start();
			final String url = "http://127.0.0.1:" + server.getLocalPort() + "/boot.conf";
			final long started = System.nanoTime();
			final SingleFileBootstrapStrategy strategy = strategy(url, expression -> expression);
			strategy.runBootstrapProcedure();
			final long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
			assertEquals(AgentStatus.FAULT, strategy.getAgentStatus());
			assertTrue(elapsedMs < 5_000, "bootstrap waited " + elapsedMs + " ms");
		} finally {
			restore(SingleFileBootstrapStrategy.CONNECT_TIMEOUT_PROPERTY, previousConnect);
			restore(SingleFileBootstrapStrategy.READ_TIMEOUT_PROPERTY, previousRead);
		}
	}

	@Test
	void successfulFileIsReady() throws Exception {
		final Path file = tmp.resolve("boot.conf");
		Files.writeString(file, "graph.addVertex('id', 'a')\n");
		final SingleFileBootstrapStrategy strategy = strategy(file.toUri().toString(), expression -> "ok");
		strategy.runBootstrapProcedure();
		assertEquals(AgentStatus.READY, strategy.getAgentStatus());
	}

	private static void restore(final String key, final String previous) {
		if (previous == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, previous);
		}
	}

	private static SingleFileBootstrapStrategy strategy(final String url,
			final java.util.function.Function<String, Object> expressions) {
		final WaldotNamespace namespace = (WaldotNamespace) Proxy.newProxyInstance(
				WaldotNamespace.class.getClassLoader(), new Class<?>[] { WaldotNamespace.class }, (proxy, method, args) -> {
					return switch (method.getName()) {
					case "getBootstrapUrl" -> url;
					case "getBootLogger" -> LoggerFactory.getLogger(SingleFileBootstrapStrategyProcedureTest.class);
					case "runExpression" -> expressions.apply((String) args[0]);
					default -> defaultValue(method.getReturnType());
					};
				});
		final SingleFileBootstrapStrategy strategy = new SingleFileBootstrapStrategy();
		strategy.initialize(namespace);
		return strategy;
	}

	private static Object defaultValue(final Class<?> type) {
		if (!type.isPrimitive()) {
			return null;
		}
		if (type == boolean.class) {
			return false;
		}
		if (type == long.class) {
			return 0L;
		}
		if (type == double.class) {
			return 0d;
		}
		if (type == float.class) {
			return 0f;
		}
		return 0;
	}
}
