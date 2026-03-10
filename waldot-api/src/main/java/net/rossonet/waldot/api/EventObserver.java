package net.rossonet.waldot.api;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

/**
 * EventObserver interface for handling events from OPC UA nodes.
 * Implementations of this interface should define how to handle events fired by
 * nodes in the OPC UA server.
 * 
 * <p>EventObserver is notified when OPC UA events are fired on vertices.
 * This allows implementations to react to events, log them, or trigger
 * additional processing.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * public class TemperatureEventObserver implements EventObserver {
 *     @Override
 *     public void fireEvent(UaNode node, BaseEventType event) {
 *         System.out.println("Event from: " + node.getNodeId());
 *         System.out.println("Event type: " + event.getClass().getSimpleName());
 *         System.out.println("Event time: " + event.getTime());
 *         
 *         // Extract event data and process
 *         if (event instanceof MyTemperatureEvent) {
 *             MyTemperatureEvent tempEvent = (MyTemperatureEvent) event;
 *             handleTemperatureEvent(tempEvent);
 *         }
 *     }
 * }
 * 
 * // Register on a vertex
 * vertex.addEventObserver(new TemperatureEventObserver());
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see WaldotVertex
 * @see BaseEventType
 */
public interface EventObserver {

	/**
	 * Called when an OPC UA event is fired.
	 * 
	 * <p>This callback is invoked when an event occurs on a vertex that
	 * this observer is registered with.</p>
	 * 
	 * @param node the source node that fired the event
	 * @param event the event that was fired
	 * @see UaNode
	 * @see BaseEventType
	 */
	void fireEvent(UaNode node, BaseEventType event);

}
