# WaldOT Rules Engine Architecture

## Overview

The WaldOT Rules Engine is a sophisticated event-driven rule execution system built on the WaldOT framework. It implements IF-THEN-THAT style rules that react to OPC-UA events and property changes in a graph-based architecture.

## Core Components

### 1. WaldotRulesEnginePlugin

The main plugin class that bootstraps the rules engine and provides core infrastructure.

**Responsibilities:**
- Registers vertex types (`rule` and `compute`) with the WaldOT framework
- Registers edge type (`execute`) for connecting rules to compute nodes
- Provides JEXL engine with base context containing global variables
- Manages monitored edge lifecycle
- Coordinates between OPC-UA namespace and TinkerPop graph

**Key Features:**
- Automatic plugin discovery via `@WaldotPlugin` annotation
- JEXL base context with shared variables: `log`, `g` (traversal), `graph`, `commands`, `Math`, `random`
- Thread-safe edge management with synchronized collections
- Graceful shutdown and resource cleanup

### 2. RuleVertex

Represents an IF-THEN-THAT rule in the graph.

**Architecture:**
```
[Source Node] --> [FireMonitoredEdge] --> [RuleVertex] --> [ComputeMonitoredEdge] --> [ComputeVertex]
    (event/property)     (filters)         (enqueues)          (routes)              (executes)
```

**Properties:**
- `Condition`: JEXL expression for IF clause (must return boolean)
- `Action`: JEXL expression for THEN clause
- `Hysteresis`: Time window (ms) for event deduplication
- `Debug`: Debug level (0=off, 1=events, 2=events+logs)
- `Queue`: Current queue size (read-only metric)
- `Total`: Total events received (read-only metric)
- `Executed`: Actions executed successfully (read-only metric)
- `Errors`: Errors during execution (read-only metric)

**Execution Flow:**
1. **Event arrival**: FireMonitoredEdge filters and fires RuleVertex
2. **Enqueue**: Event enqueued in hysteresis priority queue
3. **Condition compile**: JEXL condition script compiled (cached)
4. **Condition evaluate**: Condition executed with JEXL context
5. **Action compile**: If condition true, JEXL action compiled (cached)
6. **Action execute**: Action executed with JEXL context
7. **Metrics update**: Total, executed, errors counters updated

**JEXL Context:**
Rules execute with a cloned context containing:
- `log`: SLF4J logger
- `g`: Gremlin traversal source
- `graph`: TinkerPop graph instance
- `commands`: WaldOT console commands
- `self`: Reference to this RuleVertex
- `Math`: Java Math class
- `random`: ThreadLocalRandom

**Debug Events:**
When `Debug > 0`, RuleVertex publishes OPC-UA events for each phase:
- `BEFORE_CONDITION_COMPILE` / `AFTER_CONDITION_COMPILE`
- `BEFORE_CONDITION_EXECUTION` / `AFTER_CONDITION_EXECUTION`
- `BEFORE_ACTION_COMPILE` / `AFTER_ACTION_COMPILE`
- `BEFORE_ACTION_EXECUTION` / `AFTER_ACTION_EXECUTION`
- `*_EXCEPTION` variants for error cases

### 3. ComputeVertex

Thread manager that executes rule actions with priority queuing.

**Architecture:**
```
[RuleVertex 1] --execute(Priority=100)--> [ComputeMonitoredEdge 1]
[RuleVertex 2] --execute(Priority=50)---> [ComputeMonitoredEdge 2] --> [ComputeVertex]
[RuleVertex N] --execute(Priority=10)---> [ComputeMonitoredEdge N]          |
                                                                             v
                                                                  [Priority Queue (Dirty Nodes)]
                                                                             |
                                                                             v
                                                                  [Virtual Thread Pool]
```

**Properties:**
- `Threads`: Thread pool size (default: 1)
- `execution-timeout-ms`: Action timeout in ms (default: 120000)
- `Factor`: Priority factor multiplier (default: 100.0)
- `Queue`: Number of dirty nodes awaiting execution (read-only)

**Priority Mechanism:**
```
weight = edge_priority × priority_factor + queue_size
```

**Example:**
- RuleVertex A: edge_priority=100, queue_size=5, factor=100.0 → weight = 10,005
- RuleVertex B: edge_priority=50, queue_size=10, factor=100.0 → weight = 5,010

Higher weight = higher priority. Rule A is processed first.

**Thread Management:**
- Uses Java 21+ virtual threads for lightweight concurrency
- Configurable thread pool size limits concurrent actions
- Automatic timeout cancellation for hung actions
- Tracks execution time per action for timeout detection

**Dirty Node Queue:**
ComputeVertex maintains a priority blocking queue of "dirty nodes" (RuleVertices with pending events). The queue is ordered by weight, ensuring high-priority and backed-up rules get processed first.

### 4. ComputableFireableAbstractOpcVertex

Abstract base class for vertices that receive events and enqueue actions.

**Key Features:**
- **Event reception**: Receives OPC-UA events and property changes via `fireEvent()` and `fireProperty()`
- **Priority queuing**: Events queued by priority using `HysteresisPriorityQueue`
- **Hysteresis support**: Time-based deduplication to prevent event flooding
- **Action generation**: Abstract methods for subclasses to create executable actions

**Hysteresis Mechanism:**
When hysteresis is enabled (hysteresisTimeMs > 0), duplicate events within the time window are automatically deduplicated:
- Hysteresis = 1000ms: Only 1 event/second queued even if 100 arrive
- Prevents rule flooding from rapidly changing sensors
- Configurable per RuleVertex via `Hysteresis` property

### 5. ComputeMonitoredEdge

Monitored edge connecting RuleVertex to ComputeVertex.

**Architecture:**
```
[RuleVertex] --execute--> [ComputeMonitoredEdge] --notifies--> [ComputeVertex]
     |                            |                                  |
     v (property change)          v (observes)                      v (polls)
[Queue size]  -----------------> [propertyChanged]  ------------> [take event]
```

**Monitoring Mechanism:**
1. Observes RuleVertex `queue` property
2. When queue size changes, `propertyChanged()` is called
3. Calculates edge priority from edge properties
4. Notifies ComputeVertex with queue size and priority
5. ComputeVertex adds RuleVertex to dirty nodes priority queue
6. ComputeVertex later polls events from RuleVertex for execution

**Priority Propagation:**
The edge priority (from edge property `Priority`) is propagated to ComputeVertex to calculate execution weight. This allows different rules to have different priorities.

### 6. Supporting Classes

#### FireableAction
Abstract Runnable representing an executable rule action:
- Tracks starting time for timeout detection
- Executed in virtual thread pool
- Subclassed by `RuleVertexFireableAction` with condition/action logic

#### RunnableEvent
Container for event data and associated action:
- Immutable and thread-safe
- Two types: `EVENT` (OPC-UA event) and `PROPERTY_CHANGE` (property change)
- Wraps FireableAction for execution
- Enqueued in hysteresis priority queue

## Event Flow Architecture

### Complete Flow: Source to Execution

```
┌─────────────────┐
│  Source Vertex  │  (e.g., Temperature Sensor)
│  (OPC-UA Node)  │
└────────┬────────┘
         │ property change: temperature = 85.0
         v
┌─────────────────────┐
│ FireMonitoredEdge   │  (from waldot-namespace)
│  - monitor-property │  Filters:
│  - deadband: 5.0    │  - Active flag check
│  - delay: 0ms       │  - Property match (temperature)
└────────┬────────────┘  - Deadband threshold (5.0)
         │ PASS → fire()  - Optional delay
         v
┌─────────────────────┐
│    RuleVertex       │  Rule: "temp-alarm"
│  - Condition: "temperature > 80.0"
│  - Action: "log.warn('Alarm: ' + temperature)"
│  - Hysteresis: 5000ms
│  - Priority: 100    │
└────────┬────────────┘
         │ fireProperty(node, "temperature", value, priority)
         v
┌─────────────────────┐
│ HysteresisPriorityQueue │  Deduplicates events within 5s
│  - Check hysteresis │  Queues by priority
│  - Enqueue event    │
└────────┬────────────┘
         │ queue size increases: 0 → 1
         v
┌─────────────────────┐
│ ComputeMonitoredEdge│  Observes "queue" property
│  - Priority: 100    │
└────────┬────────────┘
         │ propertyChanged(node, "queue", 1)
         v
┌─────────────────────┐
│   ComputeVertex     │  Thread Manager
│  - Threads: 4       │  Calculates weight:
│  - Factor: 100.0    │  weight = 100 × 100.0 + 1 = 10,001
└────────┬────────────┘
         │ Add to dirty nodes priority queue
         v
┌─────────────────────┐
│ Priority Queue      │  Orders by weight
│ (Dirty Nodes)       │  Higher weight = sooner execution
└────────┬────────────┘
         │ ComputeThreadManager.run() polling loop
         v
┌─────────────────────┐
│ Thread Available?   │  Check: runners.size() < threads?
└────────┬────────────┘
         │ YES → take dirty node
         v
┌─────────────────────┐
│ RuleVertex.poll()   │  Retrieve RunnableEvent from queue
└────────┬────────────┘
         │ RunnableEvent with FireableAction
         v
┌─────────────────────┐
│ Virtual Thread Pool │  Submit action for execution
│ executor.submit()   │
└────────┬────────────┘
         │ RuleVertexFireableAction.run()
         v
┌─────────────────────┐
│ Compile Condition   │  JEXL: compiledCondition = jexl.createScript(condition)
│ (if not cached)     │  Cache for reuse
└────────┬────────────┘
         │ compiledCondition.execute(jexlContext)
         v
┌─────────────────────┐
│ Evaluate Condition  │  Context: {temperature: 85.0, log, g, graph, self, ...}
│ "temperature > 80.0"│  Result: true
└────────┬────────────┘
         │ IF true → continue
         │ IF false → return (skip action)
         v
┌─────────────────────┐
│ Compile Action      │  JEXL: compiledAction = jexl.createScript(action)
│ (if not cached)     │  Cache for reuse
└────────┬────────────┘
         │ compiledAction.execute(jexlContext)
         v
┌─────────────────────┐
│ Execute Action      │  log.warn('Alarm: ' + temperature)
│ "log.warn(...)"     │  Output: "Alarm: 85.0"
└────────┬────────────┘
         │ Update metrics
         v
┌─────────────────────┐
│ Update Metrics      │  Total++, Executed++
│ - total = 1         │  OPC-UA properties updated
│ - executed = 1      │
│ - errors = 0        │
└─────────────────────┘
```

### Key Points

1. **Filtering**: FireMonitoredEdge filters events before firing RuleVertex
2. **Deduplication**: Hysteresis prevents flooding from rapid property changes
3. **Prioritization**: Edge priority and queue size determine execution order
4. **Concurrency**: Virtual threads enable lightweight parallel execution
5. **Caching**: JEXL scripts compiled once and cached for performance
6. **Metrics**: Comprehensive counters track rule execution statistics
7. **Timeout**: Actions exceeding timeout are cancelled automatically
8. **Debug**: Optional event publishing for execution phase visibility

## Design Patterns

### 1. Observer Pattern
- ComputeMonitoredEdge observes RuleVertex queue property
- FireMonitoredEdge observes source node events/properties
- Property observers notify edges of changes

### 2. Producer-Consumer Pattern
- RuleVertex produces events (enqueues in priority queue)
- ComputeVertex consumes events (dequeues and executes)
- Priority blocking queue coordinates between producers and consumers

### 3. Priority Queue Pattern
- Events queued by priority within RuleVertex
- Dirty nodes queued by weight within ComputeVertex
- Ensures urgent rules and backed-up rules get processed first

### 4. Strategy Pattern
- FireableAction is abstract strategy for action execution
- RuleVertexFireableAction implements concrete strategy
- Allows different action types without changing queue logic

### 5. Virtual Thread Pattern (Java 21+)
- Lightweight threads for concurrent rule execution
- Executor service with virtual thread factory
- Scales to thousands of concurrent actions

### 6. Hysteresis Pattern
- Time-based deduplication using HysteresisPriorityQueue
- Prevents event flooding from rapidly changing sources
- Configurable per rule for fine-grained control

## Thread Safety

### Concurrent Data Structures
- `ConcurrentHashMap<NodeId, ComputableFireableAbstractOpcVertex>`: Served fireable nodes
- `PriorityBlockingQueue<DirtyNode>`: Dirty nodes queue (thread-safe)
- `ConcurrentHashMap<Future<?>, FireableAction>`: Active runners
- `Collections.synchronizedMap()`: Active edges map

### Synchronization Points
- RuleVertex queue: `HysteresisPriorityQueue` (internally synchronized)
- ComputeVertex dirty nodes: `PriorityBlockingQueue` (thread-safe by design)
- Property updates: WaldOT framework handles OPC-UA synchronization

### Virtual Threads
- Each action executes in isolated virtual thread
- No shared state between actions (except graph, which is thread-safe)
- Timeout tracking via `Future.cancel(true)`

## Performance Characteristics

### Memory Usage
- **Low memory footprint**: Virtual threads use ~1KB stack (vs. 1MB for platform threads)
- **Efficient queuing**: Priority queues use heap data structure (O(log n) operations)
- **Script caching**: Compiled JEXL scripts reused across executions

### Execution Time
- **JEXL compilation**: ~1-10ms per script (cached after first use)
- **JEXL execution**: ~0.1-1ms for simple expressions
- **Event enqueue**: O(log n) due to priority queue
- **Event dequeue**: O(log n) due to priority queue

### Scalability
- **Rules**: Handles thousands of concurrent rules
- **Events**: Processes thousands of events per second
- **Threads**: Scales to thousands of virtual threads
- **Priority queue**: Efficient even with large queues (O(log n))

### Bottlenecks
- **JEXL execution**: Complex scripts can slow down execution
- **Thread pool size**: Limits concurrent actions (configurable)
- **Queue polling**: Single ComputeThreadManager per ComputeVertex (can create multiple)
- **OPC-UA synchronization**: Property updates synchronized by OPC-UA framework

## Configuration Best Practices

### Rule Configuration
1. **Keep conditions simple**: Fast boolean checks
2. **Avoid blocking in actions**: No sleeps, waits, or blocking I/O
3. **Use hysteresis**: Prevent flooding from noisy sensors
4. **Set appropriate priorities**: Critical rules get high priority
5. **Enable debug sparingly**: Debug level 2 generates many logs

### Compute Configuration
1. **Thread pool sizing**: Start with CPU core count
2. **Timeout setting**: 2x expected max action duration
3. **Priority factor**: Balance priority vs. queue size (default 100.0 works well)
4. **Multiple compute nodes**: Distribute load across multiple ComputeVertices

### Edge Configuration
1. **FireMonitoredEdge filters**: Use deadband to reduce noise
2. **Execute edge priorities**: Assign based on rule criticality
3. **Monitor specific properties**: Avoid monitoring all properties

## Integration with WaldOT

### OPC-UA Integration
- Rules exposed as OPC-UA objects with properties
- Debug events published to OPC-UA event stream
- Properties readable/writable via OPC-UA clients
- Follows OPC-UA type system and namespace conventions

### TinkerPop Graph Integration
- Rules and computes are TinkerPop vertices
- Execute edges are TinkerPop edges
- Gremlin traversal available in JEXL context (`g`)
- Graph queries for finding related vertices

### Plugin System Integration
- `@WaldotPlugin` annotation for auto-discovery
- Implements `PluginListener` interface
- Registers vertex and edge types
- Handles edge lifecycle notifications

## Extension Points

### Custom Vertex Types
Extend `ComputableFireableAbstractOpcVertex` to create custom rule types:
- Implement `getRunnableEvent()` and `getRunnablePropertyEvent()`
- Add custom properties and logic
- Register with plugin system

### Custom Edge Types
Create custom monitored edges by extending `MonitoredEdge`:
- Override `propertyChanged()` for custom filtering
- Add custom routing logic
- Register with plugin system

### Custom JEXL Functions
Add custom functions to JEXL context:
- Register in `WaldotRulesEnginePlugin.registerJexlEngine()`
- Available in all rule conditions and actions
- Can wrap complex logic for reuse

### Custom Actions
Extend `FireableAction` for specialized action types:
- Override `run()` with custom execution logic
- Add timeout tracking and metrics
- Return from factory methods in custom vertex

## Troubleshooting

### Common Issues

**Rule not executing:**
- Check FireMonitoredEdge is active
- Verify property name matches
- Check deadband threshold
- Enable debug level 2 to see events

**Slow execution:**
- Check JEXL script complexity
- Verify no blocking operations in actions
- Monitor thread pool saturation (Queue size)
- Increase thread pool size or add compute nodes

**Event flooding:**
- Enable hysteresis on RuleVertex
- Increase deadband on FireMonitoredEdge
- Add delay on FireMonitoredEdge
- Check sensor noise issues

**Timeout errors:**
- Increase execution-timeout-ms
- Simplify action scripts
- Check for blocking operations
- Review action duration metrics

### Debug Techniques

1. **Enable debug events**: Set `Debug=1` or `Debug=2` on RuleVertex
2. **Monitor metrics**: Watch `Queue`, `Total`, `Executed`, `Errors` properties
3. **Check OPC-UA events**: Subscribe to debug events in OPC-UA client
4. **Log JEXL context**: Use `log.info()` in conditions/actions
5. **Gremlin queries**: Use `g.V().hasLabel('rule')` to inspect rules

## Future Enhancements

### Planned Features
1. **Rule chaining**: Output of one rule triggers another
2. **State machines**: Multi-state rules with transitions
3. **Time-based conditions**: Scheduled rules and time windows
4. **Aggregations**: Count, sum, average over time windows
5. **External integrations**: REST APIs, databases, message queues
6. **Rule versioning**: Track rule changes over time
7. **A/B testing**: Run multiple rule variants for testing
8. **Machine learning**: Predict rule outcomes and optimize

### Performance Improvements
1. **JEXL precompilation**: Compile scripts at startup
2. **Batch execution**: Process multiple events in single transaction
3. **Parallel compilation**: Compile multiple rules concurrently
4. **Adaptive priorities**: Dynamically adjust priorities based on load
5. **Load balancing**: Distribute rules across multiple compute nodes

---

*This architecture document describes WaldOT Rules Engine version 0.4.0+*
