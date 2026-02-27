package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history;

import java.util.Collections;
import java.util.List;

import org.eclipse.milo.opcua.sdk.server.AddressSpace.HistoryReadContext;
import org.eclipse.milo.opcua.sdk.server.AddressSpace.HistoryUpdateContext;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResult;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.annotation.WaldotHistoryStrategy;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.HistoryStrategy;
import net.rossonet.waldot.opc.AbstractOpcVertex;

@WaldotHistoryStrategy
public class LoggerHistoryStrategy implements HistoryStrategy {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void close() throws Exception {
		logger.info("Closing LoggerHistoryStrategy");
	}

	@Override
	public List<HistoryReadResult> historyRead(HistoryReadContext context, HistoryReadDetails readDetails,
			TimestampsToReturn timestamps, List<HistoryReadValueId> readValueIds) {
		final HistoryReadResult result = new HistoryReadResult(
				new StatusCode(StatusCodes.Bad_HistoryOperationUnsupported), null, null);
		logger.info("HistoryRead called with {} readValueIds, returning Bad_HistoryOperationUnsupported",
				readValueIds.size());
		return Collections.nCopies(readValueIds.size(), result);

	}

	@Override
	public List<HistoryUpdateResult> historyUpdate(HistoryUpdateContext context,
			List<HistoryUpdateDetails> updateDetails) {
		final HistoryUpdateResult result = new HistoryUpdateResult(
				new StatusCode(StatusCodes.Bad_HistoryOperationUnsupported), null, null);
		logger.info("HistoryUpdate called with {} updateDetails, returning Bad_HistoryOperationUnsupported",
				updateDetails.size());
		return Collections.nCopies(updateDetails.size(), result);

	}

	@Override
	public void onDataItemsCreated(List<DataItem> dataItems) {
		logger.info("onDataItemsCreated called with {} dataItems", dataItems.size());
	}

	@Override
	public void onDataItemsDeleted(List<DataItem> dataItems) {
		logger.info("onDataItemsDeleted called with {} dataItems", dataItems.size());
	}

	@Override
	public void onDataItemsModified(List<DataItem> dataItems) {
		logger.info("onDataItemsModified called with {} dataItems", dataItems.size());
	}

	@Override
	public void opcuaUpdateEvent(UaNode sourceNode) {
		logger.info("opcuaUpdateEvent called for node {}", sourceNode.getBrowseName().getName());
	}

	@Override
	public void registerHistoryRecord(String dataContext, AbstractOpcVertex vertex, String propertyLabel,
			DataValue propertyValue) {
		logger.info(
				"registerHistoryRecord called with dataContext: {}, vertex: {}, propertyLabel: {}, propertyValue: {}",
				dataContext, vertex, propertyLabel, propertyValue);

	}

	@Override
	public void setNamespace(WaldotNamespace namespace) {
		// not needed for this strategy

	}

}
