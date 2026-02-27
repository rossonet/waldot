package net.rossonet.waldot.api.strategies;

import java.util.List;

import org.eclipse.milo.opcua.sdk.server.AddressSpace.HistoryReadContext;
import org.eclipse.milo.opcua.sdk.server.AddressSpace.HistoryUpdateContext;
import org.eclipse.milo.opcua.sdk.server.items.DataItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResult;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateResult;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractOpcVertex;

public interface HistoryStrategy extends AutoCloseable {
	public static final String DEFAULT_DATA_CONTEXT = "measurement";

	List<HistoryReadResult> historyRead(HistoryReadContext context, HistoryReadDetails readDetails,
			TimestampsToReturn timestamps, List<HistoryReadValueId> readValueIds);

	List<HistoryUpdateResult> historyUpdate(HistoryUpdateContext context, List<HistoryUpdateDetails> updateDetails);

	void onDataItemsCreated(List<DataItem> dataItems);

	void onDataItemsDeleted(List<DataItem> dataItems);

	void onDataItemsModified(List<DataItem> dataItems);

	void opcuaUpdateEvent(UaNode sourceNode);

	void registerHistoryRecord(String dataContext, AbstractOpcVertex vertex, String propertyLabel,
			DataValue propertyValue);

	void setNamespace(WaldotNamespace namespace);

}
