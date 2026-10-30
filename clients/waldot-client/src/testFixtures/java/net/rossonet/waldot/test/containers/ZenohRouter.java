package net.rossonet.waldot.test.containers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts (once per JVM) a Zenoh router in Docker and tags the test class as
 * "integration". The router endpoint is available with
 * {@link ZenohRouter.Extension#endpoint()} and in the system property
 * {@value ZenohRouter.Extension#ENDPOINT_PROPERTY}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@ExtendWith(ZenohRouter.Extension.class)
public @interface ZenohRouter {

	class Extension extends AbstractSharedContainerExtension implements BeforeAllCallback {

		public static final String ENDPOINT_PROPERTY = "waldot.test.zenoh.endpoint";
		public static final String IMAGE = System.getProperty("waldot.test.zenoh.image", "eclipse/zenoh:1.10.1");
		private static final String KEY = "zenoh";
		public static final int ROUTER_PORT = 7447;

		/** Starts the router if needed and returns its endpoint (tcp/host:port). */
		@SuppressWarnings("resource")
		public static String endpoint() {
			final GenericContainer<?> container = shared(KEY, () -> newRouter(null));
			final String endpoint = endpoint(container);
			System.setProperty(ENDPOINT_PROPERTY, endpoint);
			return endpoint;
		}

		/**
		 * Endpoint of a started router as seen from the host (tcp/host:mappedPort).
		 */
		public static String endpoint(final GenericContainer<?> router) {
			return "tcp/" + router.getHost() + ":" + router.getMappedPort(ROUTER_PORT);
		}

		/**
		 * Endpoint of a router as seen by the containers of its Docker network
		 * (tcp/alias:7447).
		 */
		public static String networkEndpoint(final String alias) {
			return "tcp/" + alias + ":" + ROUTER_PORT;
		}

		/**
		 * A new (not started, not shared) router. With a network the router joins it
		 * with the given aliases, so the containers of that network reach it on
		 * {@link #networkEndpoint(String)}; the port is published on the host
		 * anyway. The caller starts and stops it.
		 *
		 * @param network null for the default bridge
		 * @param aliases network aliases (ignored without network)
		 */
		@SuppressWarnings("resource")
		public static GenericContainer<?> newRouter(final Network network, final String... aliases) {
			final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(IMAGE))
					.withExposedPorts(ROUTER_PORT).waitingFor(Wait.forLogMessage(".*Zenoh can be reached at.*", 1)
							.withStartupTimeout(Duration.ofSeconds(60)));
			if (network != null) {
				container.withNetwork(network);
				if (aliases != null && aliases.length > 0) {
					container.withNetworkAliases(aliases);
				}
			}
			return container;
		}

		@Override
		public void beforeAll(final ExtensionContext context) {
			endpoint();
		}

		@Override
		protected String serviceName() {
			return "Zenoh router";
		}
	}
}
