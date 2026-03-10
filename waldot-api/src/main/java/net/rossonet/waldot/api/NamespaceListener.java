package net.rossonet.waldot.api;

import java.util.List;

import org.eclipse.milo.opcua.sdk.server.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

import net.rossonet.waldot.api.models.WaldotCommand;

/**
 * NamespaceListener interface for handling events related to the Waldot
 * namespace.
 * 
 * <p>NamespaceListener provides callbacks for various namespace events,
 * including bootstrap completion, command registration, data item changes,
 * and namespace updates. Implementations can use these callbacks to react
 * to changes in the WaldOT system.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public class MyNamespaceListener implements NamespaceListener {
 *     @Override
 *     public void onNamespaceCreated(ManagedNamespaceWithLifecycle namespace) {
 *         System.out.println("Namespace created: " + namespace.getNamespaceUri());
 *     }
 *     
 *     @Override
 *     public void onBootstrapProcedureCompleted() {
 *         System.out.println("Bootstrap completed - system ready!");
 *     }
 *     
 *     @Override
 *     public void onCommandRegistered(WaldotCommand command) {
 *         System.out.println("Command registered: " + command.getConsoleCommand());
 *     }
 *     
 *     @Override
 *     public void onDataItemsCreated(List<DataItem> dataItems) {
 *         System.out.println("Created " + dataItems.size() + " monitored items");
 *     }
 * }
 * 
 * // Register listener
 * namespace.addListener(new MyNamespaceListener());
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see WaldotNamespace
 * @see ManagedNamespaceWithLifecycle
 */
public interface NamespaceListener {

	/**
	 * Called when the bootstrap procedure completes successfully.
	 * 
	 * <p>This indicates that the WaldOT agent has finished initializing
	 * and is ready for operation.</p>
	 */
	default void onBootstrapProcedureCompleted() {
	}

	/**
	 * Called when a command is registered.
	 * 
	 * @param command the registered WaldotCommand
	 * @see WaldotCommand
	 */
	default void onCommandRegistered(final WaldotCommand command) {
	}

	/**
	 * Called when a command is removed.
	 * 
	 * @param command the removed WaldotCommand
	 */
	default void onCommandRemoved(final WaldotCommand command) {
	}

	/**
	 * Called when data items (monitored items) are created.
	 * 
	 * <p>This occurs when new properties are subscribed for monitoring.</p>
	 * 
	 * @param dataItems the list of created DataItem objects
	 * @see DataItem
	 */
	default void onDataItemsCreated(final List<DataItem> dataItems) {
	}

	/**
	 * Called when data items are deleted.
	 * 
	 * <p>This occurs when monitored items are removed.</p>
	 * 
	 * @param dataItems the list of deleted DataItem objects
	 */
	default void onDataItemsDeleted(final List<DataItem> dataItems) {
	}

	/**
	 * Called when data items are modified.
	 * 
	 * <p>This occurs when monitored item configuration changes.</p>
	 * 
	 * @param dataItems the list of modified DataItem objects
	 */
	default void onDataItemsModified(final List<DataItem> dataItems) {
	}

	/**
	 * Called when monitoring mode changes for monitored items.
	 * 
	 * @param monitoredItems the list of affected MonitoredItem objects
	 * @see MonitoredItem
	 */
	default void onMonitoringModeChanged(final List<MonitoredItem> monitoredItems) {
	}

	/**
	 * Called when the namespace is created.
	 * 
	 * @param namespace the created namespace
	 * @see ManagedNamespaceWithLifecycle
	 */
	default void onNamespaceCreated(final ManagedNamespaceWithLifecycle namespace) {
	}

	/**
	 * Called when the namespace is reset.
	 * 
	 * <p>This occurs when resetNameSpace() is called on the namespace.</p>
	 */
	default void onNamespaceReset() {
	}

	/**
	 * Called when a node is updated in the OPC UA address space.
	 * 
	 * @param sourceNode the updated node
	 * @see UaNode
	 */
	default void onUpdateNode(UaNode sourceNode) {
	}

}
