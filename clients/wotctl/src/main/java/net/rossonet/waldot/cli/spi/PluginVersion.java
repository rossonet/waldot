package net.rossonet.waldot.cli.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Version read from a properties resource written by the build, that is
 * available also in the native binary (which has no manifest), if the resource
 * is registered in the native-image metadata.
 */
public final class PluginVersion {

	/**
	 * @param owner    class whose class loader reads the resource
	 * @param resource absolute path of the resource with the property
	 *                 {@code version}
	 * @return the version, null if the resource or the property is missing
	 */
	public static String fromResource(final Class<?> owner,
			final String resource) {
		try (InputStream input = owner.getResourceAsStream(resource)) {
			if (input != null) {
				final Properties properties = new Properties();
				properties.load(input);
				final String version = properties.getProperty("version");
				if (version != null && !version.isBlank()) {
					return version.trim();
				}
			}
		} catch (final IOException e) {
			// unknown version
		}
		return null;
	}

	private PluginVersion() {
	}
}
