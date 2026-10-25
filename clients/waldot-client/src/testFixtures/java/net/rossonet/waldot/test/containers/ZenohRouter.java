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
			final GenericContainer<?> container = shared(KEY,
					() -> new GenericContainer<>(DockerImageName.parse(IMAGE)).withExposedPorts(ROUTER_PORT)
							.waitingFor(Wait.forLogMessage(".*Zenoh can be reached at.*", 1)
									.withStartupTimeout(Duration.ofSeconds(60))));
			final String endpoint = "tcp/" + container.getHost() + ":" + container.getMappedPort(ROUTER_PORT);
			System.setProperty(ENDPOINT_PROPERTY, endpoint);
			return endpoint;
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
