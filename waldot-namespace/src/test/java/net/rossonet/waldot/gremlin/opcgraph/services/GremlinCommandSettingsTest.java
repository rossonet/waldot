package net.rossonet.waldot.gremlin.opcgraph.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.configuration.WaldotConfiguration;
import net.rossonet.waldot.configuration.DefaultHomunculusConfiguration;
import net.rossonet.waldot.testutil.FakeNamespaces;

/**
 * Order of the sources of {@link GremlinCommandSettings#ENABLED_KEY}:
 * namespace configuration, system property, default {@code false}.
 */
class GremlinCommandSettingsTest {

	private String savedProperty;

	@BeforeEach
	void saveProperty() {
		savedProperty = System.getProperty(GremlinCommandSettings.ENABLED_KEY);
		System.clearProperty(GremlinCommandSettings.ENABLED_KEY);
	}

	@AfterEach
	void restoreProperty() {
		if (savedProperty == null) {
			System.clearProperty(GremlinCommandSettings.ENABLED_KEY);
		} else {
			System.setProperty(GremlinCommandSettings.ENABLED_KEY, savedProperty);
		}
	}

	private static WaldotConfiguration configuration(final String value) {
		final WaldotConfiguration configuration = DefaultHomunculusConfiguration.getDefault();
		if (value != null) {
			configuration.put(GremlinCommandSettings.ENABLED_KEY, value);
		}
		return configuration;
	}

	@Test
	void disabledByDefault() {
		assertFalse(GremlinCommandSettings.DEFAULT_ENABLED);
		assertFalse(GremlinCommandSettings.isEnabled(null));
		assertFalse(GremlinCommandSettings.isEnabled(FakeNamespaces.empty()));
		assertFalse(GremlinCommandSettings.isEnabled(FakeNamespaces.withConfiguration(configuration(null))));
	}

	@Test
	void systemPropertyIsUsedWithoutConfiguration() {
		System.setProperty(GremlinCommandSettings.ENABLED_KEY, " true ");
		assertTrue(GremlinCommandSettings.isEnabled(null));
		assertTrue(GremlinCommandSettings.isEnabled(FakeNamespaces.empty()));
		assertTrue(GremlinCommandSettings.isEnabled(FakeNamespaces.withConfiguration(configuration(" "))));
		System.setProperty(GremlinCommandSettings.ENABLED_KEY, "no");
		assertFalse(GremlinCommandSettings.isEnabled(null));
	}

	@Test
	void configurationWinsOverSystemProperty() {
		System.setProperty(GremlinCommandSettings.ENABLED_KEY, "true");
		assertFalse(GremlinCommandSettings.isEnabled(FakeNamespaces.withConfiguration(configuration("false"))));
		System.setProperty(GremlinCommandSettings.ENABLED_KEY, "false");
		assertTrue(GremlinCommandSettings.isEnabled(FakeNamespaces.withConfiguration(configuration("TRUE"))));
	}

	@Test
	void valueIsReadEveryTime() {
		final WaldotConfiguration configuration = configuration("true");
		final var namespace = FakeNamespaces.withConfiguration(configuration);
		assertTrue(GremlinCommandSettings.isEnabled(namespace));
		configuration.put(GremlinCommandSettings.ENABLED_KEY, "false");
		assertFalse(GremlinCommandSettings.isEnabled(namespace));
	}
}
