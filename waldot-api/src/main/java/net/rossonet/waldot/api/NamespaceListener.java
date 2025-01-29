package net.rossonet.waldot.api;

import java.util.List;

import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;

import net.rossonet.waldot.api.models.WaldotCommand;

public interface NamespaceListener {

	default void onBootstrapProcedureCompleted() {
	}

	default void onCommandRegistered(WaldotCommand command) {
	}

	default void onCommandRemoved(WaldotCommand command) {
	}

	default void onDataItemsCreated(List<DataItem> dataItems) {
	}

	default void onDataItemsDeleted(List<DataItem> dataItems) {
	}

	default void onDataItemsModified(List<DataItem> dataItems) {
	}

	default void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
	}

	default void onNamespaceCreated(ManagedNamespaceWithLifecycle namespace) {
	}

	default void onNamespaceReset() {
	}

}
