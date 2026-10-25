package net.rossonet.waldot.test;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Initializes {@link WaldotTestEnvironment} when the JUnit launcher session is
 * opened, before any test class is loaded. Registered via
 * META-INF/services/org.junit.platform.launcher.LauncherSessionListener.
 */
public class WaldotTestLauncherSessionListener implements LauncherSessionListener {

	@Override
	public void launcherSessionOpened(final LauncherSession session) {
		WaldotTestEnvironment.init();
	}
}
