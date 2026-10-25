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
 * Starts (once per JVM) an InfluxDB 3 Core server in Docker, without
 * authentication and with in-memory storage, and tags the test class as
 * "integration". The URL is available with {@link InfluxDb3.Extension#url()}
 * and in the system property {@value InfluxDb3.Extension#URL_PROPERTY}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@ExtendWith(InfluxDb3.Extension.class)
public @interface InfluxDb3 {

	class Extension extends AbstractSharedContainerExtension implements BeforeAllCallback {

		public static final int HTTP_PORT = 8181;
		public static final String IMAGE = System.getProperty("waldot.test.influxdb.image",
				"influxdb:3.12.1-core");
		private static final String KEY = "influxdb3";
		public static final String URL_PROPERTY = "waldot.test.influxdb.url";

		@Override
		public void beforeAll(final ExtensionContext context) {
			url();
		}

		@Override
		protected String serviceName() {
			return "InfluxDB 3";
		}

		/** Starts the server if needed and returns its URL (http://host:port). */
		@SuppressWarnings("resource")
		public static String url() {
			final GenericContainer<?> container = shared(KEY,
					() -> new GenericContainer<>(DockerImageName.parse(IMAGE)).withExposedPorts(HTTP_PORT)
							.withCommand("influxdb3", "serve", "--node-id", "node0", "--object-store", "memory",
									"--without-auth")
							.waitingFor(Wait.forHttp("/health").forPort(HTTP_PORT).forStatusCode(200)
									.withStartupTimeout(Duration.ofSeconds(90))));
			final String url = "http://" + container.getHost() + ":" + container.getMappedPort(HTTP_PORT);
			System.setProperty(URL_PROPERTY, url);
			return url;
		}
	}
}
