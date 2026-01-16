package net.rossonet.agent;

import java.util.List;

import org.eclipse.milo.opcua.sdk.server.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.items.MonitoredItem;

import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.models.WaldotCommand;

public class TestNamespaceListener implements NamespaceListener {

	@Override
	public void onBootstrapProcedureCompleted() {
		System.out.println("--LISTENER-- Bootstrap procedure completed");
		System.out.flush();
	}

	@Override
	public void onCommandRegistered(WaldotCommand command) {
		System.out.println("--LISTENER-- Command registered: " + command);
		System.out.flush();
	}

	@Override
	public void onCommandRemoved(WaldotCommand command) {
		System.out.println("--LISTENER-- Command removed: " + command);
		System.out.flush();
	}

	@Override
	public void onDataItemsCreated(List<DataItem> dataItems) {
		System.out.println("--LISTENER-- Data items created: " + dataItems);
		System.out.flush();
	}

	@Override
	public void onDataItemsDeleted(List<DataItem> dataItems) {
		System.out.println("--LISTENER-- Data items deleted: " + dataItems);
		System.out.flush();
	}

	@Override
	public void onDataItemsModified(List<DataItem> dataItems) {
		System.out.println("--LISTENER-- Data items modified: " + dataItems);
		System.out.flush();
	}

	@Override
	public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
		System.out.println("--LISTENER-- Monitoring mode changed: " + monitoredItems);
		System.out.flush();
	}

	@Override
	public void onNamespaceCreated(ManagedNamespaceWithLifecycle namespace) {
		System.out.println("--LISTENER-- Namespace created: " + namespace.getNamespaceUri());
		System.out.flush();
	}

	@Override
	public void onNamespaceReset() {
		System.out.println("--LISTENER-- Namespace reset");
		System.out.flush();
	}
}
