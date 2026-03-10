package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

/**
 * PropertyObserver interface for handling changes in properties of OPC UA
 * nodes. Implementations of this interface should define how to handle property
 * changes for nodes in the OPC UA server.
 * 
 * <p>This observer is notified whenever a property value changes on an OPC UA node.
 * It's used for monitoring property changes, triggering updates, and maintaining
 * consistency between the graph and OPC UA address space.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public class MyPropertyObserver implements PropertyObserver {
 *     @Override
 *     public void propertyChanged(UaNode sourceNode, String propertyLabel, DataValue value) {
 *         System.out.println("Property changed: " + propertyLabel);
 *         System.out.println("New value: " + value.getValue());
 *         System.out.println("Source node: " + sourceNode.getNodeId());
 *         
 *         // Perform custom logic on property change
 *         if ("temperature".equals(propertyLabel)) {
 *             handleTemperatureChange(sourceNode, value);
 *         }
 *     }
 * }
 * 
 * // Register observer on a vertex
 * WaldotVertex vertex = ...;
 * vertex.addPropertyObserver(new MyPropertyObserver());
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface PropertyObserver {

	/**
	 * Called when a property value changes on an OPC UA node.
	 * 
	 * <p>This method is invoked when a property's value is modified. The notification
	 * includes the source node, the property label, and the new value.</p>
	 * 
	 * <p>Note: This callback may be invoked from multiple threads. Implementations
	 * should be thread-safe or synchronize as needed.</p>
	 * 
	 * @param sourceNode the OPC UA node that contains the changed property
	 * @param propertyLabel the label/name of the property that changed
	 * @param value the new DataValue containing the property value and metadata
	 * @see UaNode
	 * @see DataValue
	 */
	void propertyChanged(UaNode sourceNode, String propertyLabel, DataValue value);

}
