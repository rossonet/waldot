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

/**
 * HistoryStrategy is an interface that defines operations for managing historical
 * data storage and retrieval in the WaldOT system.
 * 
 * <p>HistoryStrategy handles the storage and retrieval of historical data for
 * OPC UA properties. It supports reading historical values and updating
 * historical records. Implementations can use various backends like InfluxDB,
 * as seen in the waldot-history-influxdb module.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Configure history strategy
 * historyStrategy.setNamespace(waldotNamespace);
 * 
 * // Register a property for historical tracking
 * historyStrategy.registerHistoryRecord(
 *     "measurement",      // data context
 *     vertex,            // the vertex
 *     "temperature",    // property label
 *     dataValue          // current value
 * );
 * 
 * // Read historical data
 * List<HistoryReadResult> results = historyStrategy.historyRead(
 *     context, readDetails, timestamps, readValueIds
 * );
 * 
 * // Update historical data
 * List<HistoryUpdateResult> results = historyStrategy.historyUpdate(
 *     context, updateDetails
 * );
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface HistoryStrategy extends AutoCloseable {
	/**
	 * Default data context for history records.
	 */
	public static final String DEFAULT_DATA_CONTEXT = "measurement";

	/**
	 * Reads historical data from the history store.
	 * 
	 * <p>Used to query historical values of monitored items. The method returns
	 * a list of HistoryReadResult objects containing the historical data.</p>
	 * 
	 * @param context the history read context
	 * @param readDetails the read details specifying what to read
	 * @param timestamps the timestamps to return
	 * @param readValueIds the list of value IDs to read
	 * @return list of HistoryReadResult objects
	 * @see HistoryReadContext
	 * @see HistoryReadDetails
	 * @see TimestampsToReturn
	 * @see HistoryReadResult
	 */
	List<HistoryReadResult> historyRead(HistoryReadContext context, HistoryReadDetails readDetails,
			TimestampsToReturn timestamps, List<HistoryReadValueId> readValueIds);

	/**
	 * Updates historical data in the history store.
	 * 
	 * <p>Used to insert, update, or delete historical records. The method
	 * processes a list of update details and returns corresponding results.</p>
	 * 
	 * @param context the history update context
	 * @param updateDetails list of update details
	 * @return list of HistoryUpdateResult objects
	 * @see HistoryUpdateContext
	 * @see HistoryUpdateDetails
	 * @see HistoryUpdateResult
	 */
	List<HistoryUpdateResult> historyUpdate(HistoryUpdateContext context, List<HistoryUpdateDetails> updateDetails);

	/**
	 * Called when data items are created.
	 * 
	 * <p>This is triggered when new monitored items are added for tracking.
	 * Implementations can use this to set up historical data collection.</p>
	 * 
	 * @param dataItems the list of created DataItem objects
	 * @see DataItem
	 */
	void onDataItemsCreated(List<DataItem> dataItems);

	/**
	 * Called when data items are deleted.
	 * 
	 * <p>This is triggered when monitored items are removed. Implementations
	 * can use this to clean up historical data.</p>
	 * 
	 * @param dataItems the list of deleted DataItem objects
	 */
	void onDataItemsDeleted(List<DataItem> dataItems);

	/**
	 * Called when data items are modified.
	 * 
	 * <p>This is triggered when monitored item configuration changes.
	 * Implementations can use this to update historical tracking settings.</p>
	 * 
	 * @param dataItems the list of modified DataItem objects
	 */
	void onDataItemsModified(List<DataItem> dataItems);

	/**
	 * Handles OPC UA event updates.
	 * 
	 * @param sourceNode the source node of the event
	 * @see UaNode
	 */
	void opcuaUpdateEvent(UaNode sourceNode);

	/**
	 * Registers a property for historical tracking.
	 * 
	 * <p>This begins recording historical data for the specified property.
	 * The history strategy will store data values for later retrieval.</p>
	 * 
	 * @param dataContext the data context (e.g., measurement name)
	 * @param vertex the vertex containing the property
	 * @param propertyLabel the label of the property
	 * @param propertyValue the current value
	 * @see AbstractOpcVertex
	 */
	void registerHistoryRecord(String dataContext, AbstractOpcVertex vertex, String propertyLabel,
			DataValue propertyValue);

	/**
	 * Sets the namespace for this strategy.
	 * 
	 * @param namespace the WaldotNamespace to use
	 * @see WaldotNamespace
	 */
	void setNamespace(WaldotNamespace namespace);

}
