package net.rossonet.waldot.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.configuration.OpcConfiguration;
import net.rossonet.waldot.api.configuration.WaldotConfiguration;

/**
 * Default configurations: documented defaults and setter/getter contract of
 * every property.
 */
class DefaultConfigurationsTest {

	/**
	 * Calls every public setter with a distinct value and checks that the
	 * matching getter returns it.
	 *
	 * @return the number of properties checked
	 */
	private static int assertSettersRoundTrip(final Object configuration) throws Exception {
		final List<String> checked = new ArrayList<>();
		int counter = 0;
		for (final Method setter : configuration.getClass().getMethods()) {
			if (!setter.getName().startsWith("set") || setter.getParameterCount() != 1
					|| Modifier.isStatic(setter.getModifiers())
					|| setter.getDeclaringClass() != configuration.getClass()) {
				continue;
			}
			final String property = setter.getName().substring(3);
			final Method getter = configuration.getClass().getMethod("get" + property);
			final Object value = sampleValue(setter.getParameterTypes()[0], ++counter);
			setter.invoke(configuration, value);
			assertEquals(value, getter.invoke(configuration), property);
			checked.add(property);
		}
		return checked.size();
	}

	private static Object sampleValue(final Class<?> type, final int seed) throws Exception {
		if (type == String.class) {
			return "value-" + seed;
		}
		if (type == Boolean.class || type == boolean.class) {
			return seed % 2 == 0;
		}
		if (type == long.class || type == Long.class) {
			return (long) seed * 1000L;
		}
		if (type == int.class || type == Integer.class) {
			return 10_000 + seed;
		}
		if (type == UInteger.class) {
			return UInteger.valueOf(seed);
		}
		if (type == java.net.URL.class) {
			return URI.create("file:///tmp/boot-" + seed + ".conf").toURL();
		}
		if (type == DateTime.class) {
			return new DateTime(seed * 1_000_000L);
		}
		throw new IllegalArgumentException("no sample for " + type);
	}

	@Test
	void homunculusDefaults() {
		final WaldotConfiguration configuration = DefaultHomunculusConfiguration.getDefault();
		assertNotSame(configuration, DefaultHomunculusConfiguration.getDefault());
		assertEquals(DefaultHomunculusConfiguration.DEFAULT_ABOUT_COMMAND_LABEL, configuration.getAboutCommandLabel());
		assertEquals(DefaultHomunculusConfiguration.DEFAULT_ABOUT_COMMAND_DESCRIPTION,
				configuration.getAboutCommandDescription());
		assertEquals(DefaultHomunculusConfiguration.DEFAULT_WRITE_MASK, configuration.getAboutCommandWriteMask());
		assertEquals("help", configuration.getHelpCommandLabel());
		assertEquals("query", configuration.getWaldotCommandLabel());
		assertEquals("exec", configuration.getExecCommandLabel());
		assertEquals("delete directory", configuration.getDeleteDirectoryLabel());
		assertEquals("os_check_delay", configuration.getOsCheckDelayCommandLabel());
		assertEquals("urn:rossonet:waldot:engine", configuration.getManagerNamespaceUri());
		assertEquals("waldot", configuration.getRootNodeId());
		assertEquals("cmd", configuration.getInterfaceRootNodeId());
		assertEquals("aas", configuration.getAssetRootNodeId());
		assertEquals(DefaultHomunculusConfiguration.DEFAULT_HELP_DIRECTORY, configuration.getHelpDirectory());
		assertTrue(configuration.getAnonymousAccessAllowed());
		assertNull(configuration.getBootUrl());
		assertNull(configuration.getZenohConfiguration());
		assertEquals(0L, configuration.getDefaultFactsValidDelayMs());
		assertEquals(0L, configuration.getDefaultFactsValidUntilMs());
		assertNotNull(configuration.getFactoryUsername());
		assertNotNull(configuration.getFactoryPassword());
	}

	@Test
	void homunculusSettersRoundTrip() throws Exception {
		final DefaultHomunculusConfiguration configuration = (DefaultHomunculusConfiguration) DefaultHomunculusConfiguration
				.getDefault();
		// every configurable property must have a working setter/getter pair
		assertTrue(assertSettersRoundTrip(configuration) >= 50);
	}

	@Test
	void homunculusAnonymousAccessCanBeDisabled() {
		final DefaultHomunculusConfiguration configuration = (DefaultHomunculusConfiguration) DefaultHomunculusConfiguration
				.getDefault();
		configuration.setAnonymousAccessAllowed(false);
		assertFalse(configuration.getAnonymousAccessAllowed());
	}

	@Test
	void opcUaDefaults() {
		final OpcConfiguration configuration = DefaultOpcUaConfiguration.getDefault();
		assertNotSame(configuration, DefaultOpcUaConfiguration.getDefault());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_APPLICATION_NAME, configuration.getApplicationName());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_BIND_ADDRESSES, configuration.getBindAddresses());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_BIND_HOSTNAME, configuration.getBindHostname());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_BUILD_DATE, configuration.getBuildDate());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_BUILD_NUMBER, configuration.getBuildNumber());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_ADDRESS_CERTIFICATE_GENERATOR,
				configuration.getDnsAddressCertificateGenerator());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_HTTPS_BIND_PORT, configuration.getHttpsBindPort());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_MANUFACTURER_NAME, configuration.getManufacturerName());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_PATH, configuration.getPath());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_PRODUCT_NAME, configuration.getProductName());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_PRODUCT_URI, configuration.getProductUri());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_SECURITY_DIR, configuration.getSecurityTempDir());
		assertEquals(DefaultOpcUaConfiguration.DEFAULT_TCP_BIND_PORT, configuration.getTcpBindPort());
	}

	@Test
	void opcUaSettersRoundTrip() throws Exception {
		final DefaultOpcUaConfiguration configuration = (DefaultOpcUaConfiguration) DefaultOpcUaConfiguration
				.getDefault();
		assertEquals(13, assertSettersRoundTrip(configuration));
	}
}
