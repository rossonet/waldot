package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history;

import java.util.Collections;
import java.util.List;

import org.eclipse.milo.opcua.sdk.server.AddressSpace.HistoryReadContext;
import org.eclipse.milo.opcua.sdk.server.AddressSpace.HistoryUpdateContext;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResult;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateResult;

import net.rossonet.waldot.api.annotation.WaldotHistoryStrategy;
import net.rossonet.waldot.api.strategies.HistoryStrategy;

@WaldotHistoryStrategy
public class BaseHistoryStrategy implements HistoryStrategy {

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public List<HistoryReadResult> historyRead(HistoryReadContext context, HistoryReadDetails readDetails,
			TimestampsToReturn timestamps, List<HistoryReadValueId> readValueIds) {

		final HistoryReadResult result = new HistoryReadResult(
				new StatusCode(StatusCodes.Bad_HistoryOperationUnsupported), null, null);

		return Collections.nCopies(readValueIds.size(), result);

	}

	@Override
	public List<HistoryUpdateResult> historyUpdate(HistoryUpdateContext context,
			List<HistoryUpdateDetails> updateDetails) {

		final HistoryUpdateResult result = new HistoryUpdateResult(
				new StatusCode(StatusCodes.Bad_HistoryOperationUnsupported), null, null);

		return Collections.nCopies(updateDetails.size(), result);

	}

	@Override
	public void onDataItemsCreated(List<DataItem> dataItems) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDataItemsDeleted(List<DataItem> dataItems) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDataItemsModified(List<DataItem> dataItems) {
		// TODO Auto-generated method stub

	}

	@Override
	public void opcuaUpdateEvent(UaNode sourceNode) {
		// TODO Auto-generated method stub

	}

}
