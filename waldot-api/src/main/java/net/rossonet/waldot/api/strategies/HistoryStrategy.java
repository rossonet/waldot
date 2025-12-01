package net.rossonet.waldot.api.strategies;

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

public interface HistoryStrategy {

	List<HistoryReadResult> historyRead(HistoryReadContext context, HistoryReadDetails readDetails,
			TimestampsToReturn timestamps, List<HistoryReadValueId> readValueIds);

	List<HistoryUpdateResult> historyUpdate(HistoryUpdateContext context, List<HistoryUpdateDetails> updateDetails);

	void onDataItemsCreated(List<DataItem> dataItems);

	void onDataItemsDeleted(List<DataItem> dataItems);

	void onDataItemsModified(List<DataItem> dataItems);

	void opcuaUpdateEvent(UaNode sourceNode);

}
