package net.rossonet.waldot.gremlin.opcgraph.strategies.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;

import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for BaseClientManagementStrategy.
 *
 * Covers the bug where getAssetClientsFolderNode() returned the never
 * initialized "agentFolder" field (always null) instead of "clientFolder",
 * causing a NullPointerException in
 * HomunculusNamespace.addAssetAgentNode().
 */
class BaseClientManagementStrategyTest {

	private static Object readField(final Object target, final String fieldName) throws Exception {
		final Field f = BaseClientManagementStrategy.class.getDeclaredField(fieldName);
		f.setAccessible(true);
		return f.get(target);
	}

	@Test
	void getAssetClientsFolderNodeReturnsClientFolderField() throws Exception {
		final BaseClientManagementStrategy strategy = new BaseClientManagementStrategy();
		// before generateAssetFolders, both are null: getter must not throw
		assertNull(strategy.getAssetClientsFolderNode());

		// simulate what generateClientFolder() does: assign the clientFolder field
		final UaFolderNode fakeFolder = null; // cannot instantiate UaFolderNode without Milo context
		final Field clientFolderField = BaseClientManagementStrategy.class.getDeclaredField("clientFolder");
		clientFolderField.setAccessible(true);
		clientFolderField.set(strategy, fakeFolder);

		// the getter must return the clientFolder field content (not the removed agentFolder)
		assertSame(readField(strategy, "clientFolder"), strategy.getAssetClientsFolderNode());
	}

	@Test
	void agentFolderFieldNoLongerExists() {
		// the unused agentFolder field was removed: this guards against regression
		assertDoesNotThrow(() -> BaseClientManagementStrategy.class.getDeclaredField("clientFolder"));
		org.junit.jupiter.api.Assertions.assertThrows(NoSuchFieldException.class,
				() -> BaseClientManagementStrategy.class.getDeclaredField("agentFolder"));
	}
}
