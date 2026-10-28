package net.rossonet.waldot.gremlin.opcgraph.services;

import java.util.Map;

import net.rossonet.waldot.api.models.WaldotNamespace;

/**
 * Switch of the Gremlin command services ({@link OpcCommandServiceFactory}):
 * when it is off, {@code call("waldot.command", ...)} fails before running
 * anything.
 * <p>
 * The flag {@value #ENABLED_KEY} is read, in order, from:
 * <ol>
 * <li>the namespace configuration
 * ({@link WaldotNamespace#getConfiguration()});</li>
 * <li>the Java system property with the same name;</li>
 * <li>the default {@value #DEFAULT_ENABLED} (safe default: commands must be
 * enabled explicitly).</li>
 * </ol>
 * The value is read at every service creation and never cached, so it can be
 * changed at runtime.
 */
public final class GremlinCommandSettings {

	/** Configuration key and system property of the flag. */
	public static final String ENABLED_KEY = "waldot.gremlin.commands.enabled";

	/** Value used when the flag is not configured. */
	public static final boolean DEFAULT_ENABLED = false;

	/**
	 * @param waldotNamespace the namespace, may be {@code null} (only the system
	 *                        property and the default are used)
	 * @return {@code true} when the Gremlin command services may run commands
	 */
	public static boolean isEnabled(final WaldotNamespace waldotNamespace) {
		final String configured = configured(waldotNamespace);
		if (configured != null) {
			return Boolean.parseBoolean(configured);
		}
		final String property = System.getProperty(ENABLED_KEY);
		if (property != null && !property.isBlank()) {
			return Boolean.parseBoolean(property.trim());
		}
		return DEFAULT_ENABLED;
	}

	private static String configured(final WaldotNamespace waldotNamespace) {
		if (waldotNamespace == null) {
			return null;
		}
		final Map<String, String> configuration = waldotNamespace.getConfiguration();
		if (configuration == null) {
			return null;
		}
		final String value = configuration.get(ENABLED_KEY);
		return value == null || value.isBlank() ? null : value.trim();
	}

	private GremlinCommandSettings() {
	}
}
