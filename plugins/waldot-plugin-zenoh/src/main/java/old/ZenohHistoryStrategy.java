package old;

import java.util.List;

import org.eclipse.milo.opcua.sdk.server.AddressSpace.HistoryReadContext;
import org.eclipse.milo.opcua.sdk.server.AddressSpace.HistoryUpdateContext;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResult;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateResult;

import net.rossonet.waldot.api.annotation.WaldotHistoryStrategy;
import net.rossonet.waldot.api.strategies.HistoryStrategy;

@WaldotHistoryStrategy
public class ZenohHistoryStrategy implements HistoryStrategy {

	private AgentLifeCycleManager lifeCycleManager;

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public List<HistoryReadResult> historyRead(HistoryReadContext context, HistoryReadDetails readDetails,
			TimestampsToReturn timestamps, List<HistoryReadValueId> readValueIds) {
		// TODO utilizzare il bus zenoh per leggere la history
		return null;
	}

	@Override
	public List<HistoryUpdateResult> historyUpdate(HistoryUpdateContext context,
			List<HistoryUpdateDetails> updateDetails) {
		// TODO utilizzare il bus zenoh per leggere la history
		return null;
	}

	public void initialize(AgentLifeCycleManager lifeCycleManager) {
		this.lifeCycleManager = lifeCycleManager;

	}

	@Override
	public void onDataItemsCreated(List<DataItem> dataItems) {
		// TODO utilizzare il bus zenoh per leggere la history

	}

	@Override
	public void onDataItemsDeleted(List<DataItem> dataItems) {
		// TODO utilizzare il bus zenoh per leggere la history

	}

	@Override
	public void onDataItemsModified(List<DataItem> dataItems) {
		// TODO utilizzare il bus zenoh per leggere la history

	}

	@Override
	public void opcuaUpdateEvent(UaNode sourceNode) {
		// TODO utilizzare il bus zenoh per leggere la history

	}

}
