package net.rossonet.waldot.rules.events;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;

/**
 * Container for a rule event and its associated executable action.
 * <p>
 * RunnableEvent incapsula un evento OPC-UA (event o property change) insieme
 * all'azione eseguibile generata dal RuleVertex. Viene messo in coda nella
 * priority queue con isteresi e successivamente estratto dal ComputeVertex
 * per l'esecuzione.
 * </p>
 * 
 * <h2>Event Types</h2>
 * <ul>
 *   <li><b>EVENT</b>: OPC-UA event notification</li>
 *   <li><b>PROPERTY_CHANGE</b>: OPC-UA property value change</li>
 * </ul>
 * 
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Created by ComputableFireableAbstractOpcVertex.fireEvent/fireProperty</li>
 *   <li>Contains event/property data + FireableAction to execute</li>
 *   <li>Enqueued in RuleVertex priority queue (with hysteresis deduplication)</li>
 *   <li>Retrieved by ComputeVertex via take() or poll()</li>
 *   <li>Starting time set on FireableAction</li>
 *   <li>Action executed in thread pool</li>
 * </ol>
 * 
 * <h2>Immutability</h2>
 * <p>
 * RunnableEvent is immutable and thread-safe. All fields are final and set
 * during construction.
 * </p>
 * 
 * @see FireableAction
 * @see ComputableFireableAbstractOpcVertex
 * @see net.rossonet.waldot.jexl.HysteresisPriorityQueue
 * 
 * @author Andrea Ambrosini - Rossonet s.c.a r.l.
 * @since 0.4.0
 */
public final class RunnableEvent {

	/**
	 * Type of event: OPC-UA event or property change.
	 */
	public enum TypeEvent {
		/** OPC-UA event notification */
		EVENT,
		/** OPC-UA property value change */
		PROPERTY_CHANGE
	}

	// Azione eseguibile associata all'evento
	private final FireableAction action;
	// Evento OPC-UA (solo per TypeEvent.EVENT)
	private final BaseEventType event;
	// Label proprietà (solo per TypeEvent.PROPERTY_CHANGE)
	private final String label;
	// Nodo OPC-UA sorgente
	private final UaNode node;
	// Tipo di evento
	private final TypeEvent type;
	// Valore proprietà (solo per TypeEvent.PROPERTY_CHANGE)
	private final Object value;

	/**
	 * Creates a RunnableEvent for an OPC-UA event.
	 * <p>
	 * Costruttore per eventi OPC-UA.
	 * </p>
	 * 
	 * @param node source OPC-UA node
	 * @param event OPC-UA event
	 * @param action executable action for this event
	 */
	public RunnableEvent(UaNode node, BaseEventType event, FireableAction action) {
		this.type = TypeEvent.EVENT;
		this.node = node;
		this.action = action;
		this.event = event;
		this.label = null;
		this.value = null;
	}

	/**
	 * Creates a RunnableEvent for a property change.
	 * <p>
	 * Costruttore per modifiche di proprietà OPC-UA.
	 * </p>
	 * 
	 * @param node source OPC-UA node
	 * @param label property name that changed
	 * @param value new property value
	 * @param action executable action for this property change
	 */
	public RunnableEvent(UaNode node, String label, Object value, FireableAction action) {
		this.type = TypeEvent.PROPERTY_CHANGE;
		this.node = node;
		this.action = action;
		this.event = null;
		this.label = label;
		this.value = value;
	}

	/**
	 * Returns the executable action with starting time set.
	 * <p>
	 * Imposta il tempo di inizio sull'azione e la ritorna per l'esecuzione.
	 * Chiamato da ComputeVertex quando l'azione viene estratta dalla coda.
	 * </p>
	 * 
	 * @param startingTimeMs execution starting time in milliseconds
	 * @return executable action with starting time set
	 */
	public FireableAction getAction(long startingTimeMs) {
		action.setStartingTime(startingTimeMs);
		return action;
	}

	public BaseEventType getEvent() {
		return event;
	}

	public String getLabel() {
		return label;
	}

	public UaNode getNode() {
		return node;
	}

	public TypeEvent getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

}
