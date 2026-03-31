# WaldOT Rules Engine API Reference

## Overview

Complete API reference for WaldOT Rules Engine classes, methods, properties, and constants.

---

## Plugin Class

### WaldotRulesEnginePlugin

Main plugin class that bootstraps the rules engine.

**Package:** `net.rossonet.waldot.rules`

**Implements:** `PluginListener`, `AutoCloseable`

**Annotations:** `@WaldotPlugin`

#### Constants

| Constant | Type | Value | Description |
|----------|------|-------|-------------|
| `ACTION_FIELD` | String | "Action" | Property name for rule action |
| `CONDITION_FIELD` | String | "Condition" | Property name for rule condition |
| `PRIORITY_FIELD` | String | "Priority" | Property name for priority |
| `HYSTERESIS_LABEL` | String | "Hysteresis" | Property name for hysteresis time |
| `DEBUG_LEVEL_LABEL` | String | "Debug" | Property name for debug level |
| `QUEUE_SIZE_LABEL` | String | "Queue" | Property name for queue size |
| `TOTAL_SIZE_LABEL` | String | "Total" | Property name for total events |
| `ACTION_EXECUTED_SIZE_LABEL` | String | "Executed" | Property name for executed count |
| `ERRORS_SIZE_LABEL` | String | "Errors" | Property name for error count |
| `THREAD_POOL_SIZE_FIELD` | String | "Threads" | Property name for thread pool size |
| `EXECUTION_TIMEOUT_MS_FIELD` | String | "execution-timeout-ms" | Property name for execution timeout |
| `PRIORITY_FACTOR_FIELD` | String | "Factor" | Property name for priority factor |
| `DEFAULT_CONDITION_VALUE` | String | "true" | Default condition expression |
| `DEFAULT_ACTION_VALUE` | String | "log.info('action fired')" | Default action expression |
| `DEFAULT_PRIORITY_VALUE` | int | 100 | Default priority |
| `DEFAULT_DELAY_BEFORE_EVALUATION` | int | 0 | Default evaluation delay (ms) |
| `DEFAULT_DELAY_BEFORE_EXECUTE` | int | 0 | Default execution delay (ms) |
| `DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE` | int | 1 | Default thread pool size |
| `DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE` | long | 120000 | Default timeout (2 minutes) |
| `DEFAULT_PRIORITY_FACTOR_IN_COMPUTE` | double | 100.0 | Default priority factor |
| `DEFAULT_CLEAR_FACTS_AFTER_EXECUTION` | boolean | false | Clear facts after execution |

#### Methods

##### baseJexlContext()
```java
public ClonableMapContext baseJexlContext()
```
Returns the base JEXL context shared by all rules.

**Returns:** Base JEXL context with global variables

##### getJexlEngine()
```java
public JexlEngine getJexlEngine()
```
Returns the JEXL engine instance.

**Returns:** JEXL engine for compiling and executing scripts

##### close()
```java
@Override
public void close() throws Exception
```
Closes the plugin and removes all monitored edges.

---

## Vertex Classes

### RuleVertex

Represents an IF-THEN-THAT rule in the graph.

**Package:** `net.rossonet.waldot.rules.vertices`

**Extends:** `ComputableFireableAbstractOpcVertex`

**Implements:** `AutoCloseable`

#### Properties

| Property | Type | Default | Access | Description |
|----------|------|---------|--------|-------------|
| `Condition` | String | "true" | Read/Write | JEXL condition expression |
| `Action` | String | log.info() | Read/Write | JEXL action expression |
| `Hysteresis` | Long | 0 | Read/Write | Event deduplication time (ms) |
| `Debug` | Integer | 0 | Read/Write | Debug level (0=off, 1=events, 2=all) |
| `Priority` | Integer | 100 | Read/Write | Rule priority |
| `Queue` | Long | 0 | Read-only | Current queue size |
| `Total` | Long | 0 | Read-only | Total events received |
| `Executed` | Long | 0 | Read-only | Actions executed |
| `Errors` | Long | 0 | Read-only | Errors during execution |

#### Constructor

```java
public RuleVertex(
    WaldotRulesEnginePlugin waldotRulesEnginePlugin,
    WaldotGraph graph,
    UaNodeContext context,
    NodeId nodeId,
    QualifiedName browseName,
    LocalizedText displayName,
    LocalizedText description,
    UInteger writeMask,
    UInteger userWriteMask,
    UByte eventNotifier,
    long version,
    Object[] propertyKeyValues
)
```

#### Methods

##### sendDebugEvent()
```java
public void sendDebugEvent(DebugEventType eventType, String message)
```
Publishes OPC-UA debug event when Debug > 0.

**Parameters:**
- `eventType`: Debug event type (BEFORE_CONDITION_COMPILE, etc.)
- `message`: Event message (can be null)

##### updateTotal()
```java
public void updateTotal()
```
Increments total events counter and updates OPC-UA property.

##### updateExecuted()
```java
public void updateExecuted()
```
Increments executed actions counter and updates OPC-UA property.

##### updateThrowable()
```java
public void updateThrowable()
```
Increments error counter and updates OPC-UA property.

#### Nested Types

##### DebugEventType (enum)
```java
public enum DebugEventType {
    BEFORE_CONDITION_COMPILE,
    AFTER_CONDITION_COMPILE,
    AFTER_CONDITION_COMPILE_EXCEPTION,
    BEFORE_CONDITION_EXECUTION,
    AFTER_CONDITION_EXECUTION,
    AFTER_CONDITION_EXECUTION_EXCEPTION,
    BEFORE_ACTION_COMPILE,
    AFTER_ACTION_COMPILE,
    AFTER_ACTION_COMPILE_EXCEPTION,
    BEFORE_ACTION_EXECUTION,
    AFTER_ACTION_EXECUTION,
    AFTER_ACTION_EXECUTION_EXCEPTION
}
```

---

### ComputeVertex

Thread manager vertex that executes rule actions.

**Package:** `net.rossonet.waldot.rules.vertices`

**Extends:** `AbstractOpcVertex`

**Implements:** `AutoCloseable`

#### Properties

| Property | Type | Default | Access | Description |
|----------|------|---------|--------|-------------|
| `Threads` | Integer | 1 | Read/Write | Thread pool size |
| `execution-timeout-ms` | Long | 120000 | Read/Write | Action timeout (ms) |
| `Factor` | Double | 100.0 | Read/Write | Priority factor multiplier |
| `Queue` | Integer | 0 | Read-only | Dirty nodes in queue |

#### Constructor

```java
public ComputeVertex(
    WaldotGraph graph,
    UaNodeContext context,
    NodeId nodeId,
    QualifiedName browseName,
    LocalizedText displayName,
    LocalizedText description,
    UInteger writeMask,
    UInteger userWriteMask,
    UByte eventNotifier,
    long version,
    Object[] propertyKeyValues
)
```

#### Methods

##### notifyQueueSizeChange()
```java
public void notifyQueueSizeChange(
    UaNode sourceNode,
    String queueSizeLabel,
    DataValue queueSize,
    int executorEdgePriority
)
```
Handles queue size change notification from ComputeMonitoredEdge.

**Parameters:**
- `sourceNode`: RuleVertex with queue change
- `queueSizeLabel`: Property label ("queue")
- `queueSize`: New queue size value
- `executorEdgePriority`: Edge priority for weight calculation

##### calcolateWeight()
```java
public int calcolateWeight(int executorEdgePriority, int queueSize)
```
Calculates execution weight for dirty node prioritization.

**Parameters:**
- `executorEdgePriority`: Priority from execute edge
- `queueSize`: Number of pending events

**Returns:** Weight = executorEdgePriority × priorityFactor + queueSize

**Formula:** `weight = (int)(executorEdgePriority * priorityFactor + queueSize)`

##### isActive()
```java
public boolean isActive()
```
Checks if compute vertex is active.

**Returns:** True if active and processing

##### close()
```java
@Override
public void close() throws Exception
```
Stops thread manager and cancels all running actions.

---

### ComputableFireableAbstractOpcVertex

Abstract base class for vertices that receive events and enqueue actions.

**Package:** `net.rossonet.waldot.rules.vertices`

**Extends:** `AbstractOpcVertex`

**Implements:** `AutoCloseable`

#### Methods

##### fireEvent()
```java
@Override
public void fireEvent(UaNode node, BaseEventType event, int priority)
```
Handles OPC-UA event firing.

**Parameters:**
- `node`: OPC-UA node that generated event
- `event`: OPC-UA event
- `priority`: Event priority

##### fireProperty()
```java
@Override
public void fireProperty(
    UaNode node,
    String propertyLabel,
    DataValue value,
    int priority
)
```
Handles OPC-UA property change firing.

**Parameters:**
- `node`: OPC-UA node with property change
- `propertyLabel`: Property name
- `value`: New property value
- `priority`: Event priority

##### offer()
```java
public boolean offer(RunnableEvent message, int priority)
```
Enqueues event in priority queue with hysteresis.

**Parameters:**
- `message`: Event to enqueue
- `priority`: Event priority

**Returns:** True if enqueued successfully

##### poll()
```java
public RunnableEvent poll()
```
Retrieves and removes next event from queue (non-blocking).

**Returns:** Next event or null if queue empty

##### take()
```java
public RunnableEvent take() throws InterruptedException
```
Retrieves and removes next event from queue (blocking).

**Returns:** Next event (waits if queue empty)

**Throws:** InterruptedException if interrupted

##### getHysteresisTimeMs()
```java
public long getHysteresisTimeMs()
```
Returns hysteresis time in milliseconds.

**Returns:** Hysteresis time (0 = disabled)

##### isHisteresisEnabled()
```java
public boolean isHisteresisEnabled()
```
Checks if hysteresis is enabled.

**Returns:** True if hysteresis > 0

##### close()
```java
@Override
public void close() throws Exception
```
Cleans up priority queue.

#### Abstract Methods

Subclasses must implement:

##### getRunnableEvent()
```java
protected abstract FireableAction getRunnableEvent(
    UaNode node,
    BaseEventType event
)
```
Creates executable action for OPC-UA event.

**Parameters:**
- `node`: Source node
- `event`: OPC-UA event

**Returns:** Executable action

##### getRunnablePropertyEvent()
```java
protected abstract FireableAction getRunnablePropertyEvent(
    UaNode node,
    String propertyLabel
)
```
Creates executable action for property change.

**Parameters:**
- `node`: Source node
- `propertyLabel`: Property that changed

**Returns:** Executable action

---

## Edge Classes

### ComputeMonitoredEdge

Monitored edge connecting RuleVertex to ComputeVertex.

**Package:** `net.rossonet.waldot.rules.edges`

**Extends:** `MonitoredEdge`

#### Constructor

```java
public ComputeMonitoredEdge(
    WaldotNamespace engine,
    WaldotEdge edge,
    WaldotVertex sourceVertex,
    WaldotVertex targetVertex
)
```

**Parameters:**
- `engine`: WaldOT namespace
- `edge`: Underlying graph edge
- `sourceVertex`: Source vertex (RuleVertex)
- `targetVertex`: Target vertex (ComputeVertex)

#### Methods

##### propertyChanged()
```java
@Override
public void propertyChanged(
    UaNode node,
    String label,
    DataValue value
)
```
Handles property change notifications from RuleVertex.

**Parameters:**
- `node`: RuleVertex node
- `label`: Property name ("queue")
- `value`: New queue size

##### fireEvent()
```java
@Override
public void fireEvent(UaNode node, BaseEventType event)
```
Not used in ComputeMonitoredEdge.

---

## Event Classes

### FireableAction

Abstract base class for executable rule actions.

**Package:** `net.rossonet.waldot.rules.events`

**Implements:** `Runnable`

#### Methods

##### getStartingTime()
```java
public long getStartingTime()
```
Returns execution starting time in milliseconds.

**Returns:** Starting time (System.currentTimeMillis())

##### setStartingTime()
```java
public void setStartingTime(long timeMillis)
```
Sets execution starting time.

**Parameters:**
- `timeMillis`: Starting time in milliseconds

##### run()
```java
@Override
public abstract void run()
```
Executes the action (implemented by subclass).

---

### RunnableEvent

Container for event data and associated action.

**Package:** `net.rossonet.waldot.rules.events`

#### Constructors

##### For OPC-UA Events
```java
public RunnableEvent(
    UaNode node,
    BaseEventType event,
    FireableAction action
)
```

**Parameters:**
- `node`: Source OPC-UA node
- `event`: OPC-UA event
- `action`: Executable action

##### For Property Changes
```java
public RunnableEvent(
    UaNode node,
    String label,
    Object value,
    FireableAction action
)
```

**Parameters:**
- `node`: Source OPC-UA node
- `label`: Property name
- `value`: New property value
- `action`: Executable action

#### Methods

##### getAction()
```java
public FireableAction getAction(long startingTimeMs)
```
Returns executable action with starting time set.

**Parameters:**
- `startingTimeMs`: Execution starting time

**Returns:** Action ready for execution

##### getNode()
```java
public UaNode getNode()
```
Returns source OPC-UA node.

##### getType()
```java
public TypeEvent getType()
```
Returns event type (EVENT or PROPERTY_CHANGE).

##### getEvent()
```java
public BaseEventType getEvent()
```
Returns OPC-UA event (null for property changes).

##### getLabel()
```java
public String getLabel()
```
Returns property label (null for events).

##### getValue()
```java
public Object getValue()
```
Returns property value (null for events).

#### Nested Types

##### TypeEvent (enum)
```java
public enum TypeEvent {
    EVENT,           // OPC-UA event notification
    PROPERTY_CHANGE  // OPC-UA property value change
}
```

---

## Usage Examples

### Creating a Rule Programmatically

```java
// Get plugin instance
WaldotRulesEnginePlugin plugin = waldotNamespace.getPlugin(WaldotRulesEnginePlugin.class);

// Create rule vertex
RuleVertex rule = new RuleVertex(
    plugin,
    graph,
    context,
    nodeId,
    browseName,
    displayName,
    description,
    writeMask,
    userWriteMask,
    eventNotifier,
    version,
    new Object[]{
        "condition", "temperature > 80.0",
        "action", "log.warn('High temperature')",
        "hysteresis", "5000"
    }
);
```

### Creating via Gremlin

```groovy
rule = graph.addVertex(
    "type", "rule",
    "label", "my-rule",
    "Condition", "temperature > 80.0",
    "Action", "log.warn('Alert')",
    "Hysteresis", "5000",
    "Debug", "1"
)
```

### Accessing JEXL Context

```java
// From within a rule action
ClonableMapContext context = plugin.baseJexlContext();
Logger log = (Logger) context.get("log");
GraphTraversalSource g = (GraphTraversalSource) context.get("g");
```

### Custom Action Implementation

```java
public class CustomFireableAction extends FireableAction {
    @Override
    public void run() {
        long startTime = getStartingTime();
        // Custom action logic
        log.info("Action started at: " + startTime);
    }
}
```

---

## Thread Safety

### Thread-Safe Classes
- `ComputeVertex`: Uses concurrent collections
- `ComputableFireableAbstractOpcVertex`: Thread-safe queue operations
- `HysteresisPriorityQueue`: Internally synchronized
- `PriorityBlockingQueue`: Thread-safe by design

### Synchronization Points
- Property updates: Synchronized by OPC-UA framework
- Queue operations: Synchronized by queue implementations
- Edge monitoring: Observer pattern with thread-safe collections

---

## Performance Notes

### Caching
- **JEXL scripts**: Compiled once, cached per rule
- **Context cloning**: Shallow clone for performance
- **Graph queries**: Not cached (use with caution)

### Memory Usage
- **Virtual threads**: ~1KB stack per thread
- **Queue entries**: ~100 bytes per RunnableEvent
- **Compiled scripts**: ~1-10KB per script

### Execution Time
- **Condition evaluation**: < 1ms for simple expressions
- **Action execution**: Depends on action complexity
- **Queue operations**: O(log n) for priority queue
- **Weight calculation**: O(1)

---

## Error Handling

### Exception Types

1. **Compilation errors**: JEXL syntax errors
   - Caught during condition/action compilation
   - Logged and counted in Errors metric
   - Debug event published if Debug > 0

2. **Execution errors**: Runtime errors in JEXL
   - Caught during condition/action execution
   - Logged and counted in Errors metric
   - Debug event published with stack trace

3. **Timeout errors**: Action exceeds timeout
   - Future cancelled by ComputeVertex
   - Logged as warning
   - Action interrupted

### Error Recovery

- **Compilation error**: Rule disabled until script fixed
- **Execution error**: Next event still processed
- **Timeout error**: Action cancelled, next event processed

---

## Migration Guide

### From 0.3.x to 0.4.x

No breaking changes. New features:
- Debug event publishing
- Enhanced metrics (Total, Executed, Errors)
- Improved hysteresis mechanism

### Deprecated APIs

None currently.

---

## See Also

- [Quick Start Guide](QUICKSTART.md)
- [JEXL Expressions Guide](JEXL_EXPRESSIONS.md)
- [Examples](EXAMPLES.md)
- [Architecture](ARCHITECTURE.md)

---

*API Reference for WaldOT Rules Engine version 0.4.0+*
