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
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface NamespaceListener {

	default void onBootstrapProcedureCompleted() {
	}

	default void onCommandRegistered(final WaldotCommand command) {
	}

	default void onCommandRemoved(final WaldotCommand command) {
	}

	default void onDataItemsCreated(final List<DataItem> dataItems) {
	}

	default void onDataItemsDeleted(final List<DataItem> dataItems) {
	}

	default void onDataItemsModified(final List<DataItem> dataItems) {
	}

	default void onMonitoringModeChanged(final List<MonitoredItem> monitoredItems) {
	}

	default void onNamespaceCreated(final ManagedNamespaceWithLifecycle namespace) {
	}

	default void onNamespaceReset() {
	}

	default void onUpdateNode(UaNode sourceNode) {
	}

}
