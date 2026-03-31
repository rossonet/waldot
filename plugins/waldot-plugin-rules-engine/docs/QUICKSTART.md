# WaldOT Rules Engine Quick Start Guide

## Introduction

This guide will help you create your first IF-THEN-THAT rule using the WaldOT Rules Engine. You'll learn how to set up a rule that monitors a temperature sensor and logs warnings when the temperature exceeds a threshold.

## Prerequisites

- WaldOT framework installed and running
- waldot-plugin-rules-engine plugin loaded
- Basic understanding of WaldOT graph concepts (vertices, edges)
- Familiarity with OPC-UA concepts (optional but helpful)

## Basic Concepts

### Components

The rules engine has two main vertex types:

1. **RuleVertex** (`type: "rule"`): Represents an IF-THEN rule
   - Contains condition (IF) and action (THEN) as JEXL expressions
   - Receives events from monitored sources
   - Queues actions for execution

2. **ComputeVertex** (`type: "compute"`): Thread manager for execution
   - Manages virtual thread pool
   - Executes rule actions with priority
   - Handles timeouts and concurrency

### Edges

Two edge types connect the components:

1. **fire**: Connects source → RuleVertex (monitors events/properties)
2. **execute**: Connects RuleVertex → ComputeVertex (routes for execution)

## Your First Rule: Temperature Alarm

### Step 1: Create a ComputeVertex

First, create a compute node to manage rule execution:

```groovy
// Using Gremlin console
compute = graph.addVertex(
    "type", "compute",
    "label", "main-compute",
    "Threads", "4",  // 4 concurrent threads
    "execution-timeout-ms", "120000",  // 2 minute timeout
    "Factor", "100.0"  // Priority factor
)
```

### Step 2: Create a RuleVertex

Create a rule that checks if temperature exceeds 80°C:

```groovy
tempAlarmRule = graph.addVertex(
    "type", "rule",
    "label", "temp-alarm",
    "Condition", "temperature > 80.0",
    "Action", "log.warn('Temperature alarm: ' + temperature + '°C')",
    "Priority", "100",
    "Hysteresis", "5000",  // 5 seconds deduplication
    "Debug", "0"  // Debug off (0=off, 1=events, 2=events+logs)
)
```

### Step 3: Connect Rule to Compute

Connect the rule to compute for execution:

```groovy
tempAlarmRule.addEdge("execute", compute, "Priority", "100")
```

### Step 4: Monitor a Temperature Sensor

Assuming you have a temperature sensor vertex:

```groovy
// Find existing temperature sensor
tempSensor = g.V().hasLabel("temperature-sensor").has("label", "sensor1").next()

// Connect sensor to rule with fire edge
tempSensor.addEdge("fire", tempAlarmRule, 
    "monitor-property", "temperature",  // Property to monitor
    "active", "true",  // Enable monitoring
    "priority", "100"  // Event priority
)
```

### Step 5: Test the Rule

Change the temperature property to trigger the rule:

```groovy
// Set temperature to 85°C (exceeds threshold)
tempSensor.property("temperature", 85.0)

// Check rule metrics
tempAlarmRule.property("total").value()      // Total events received
tempAlarmRule.property("executed").value()   // Actions executed
tempAlarmRule.property("errors").value()     // Errors
tempAlarmRule.property("queue").value()      // Current queue size
```

You should see a warning in the logs:
```
WARN  Temperature alarm: 85.0°C
```

## Rule Lifecycle

Understanding what happens when a rule executes:

```
1. Temperature changes: 85.0°C
2. FireMonitoredEdge detects change
3. Condition check: temperature > 80.0 → true
4. Action enqueued in priority queue
5. ComputeVertex polls action
6. Virtual thread executes action
7. Log warning message
8. Metrics updated (total++, executed++)
```

## Common Patterns

### Pattern 1: Simple Threshold

```groovy
rule = graph.addVertex(
    "type", "rule",
    "label", "high-pressure-alarm",
    "Condition", "pressure > 100.0",
    "Action", "log.error('Pressure too high: ' + pressure)"
)
```

### Pattern 2: Range Check

```groovy
rule = graph.addVertex(
    "type", "rule",
    "label", "temperature-range-check",
    "Condition", "temperature < 10.0 || temperature > 90.0",
    "Action", "log.warn('Temperature out of range: ' + temperature)"
)
```

### Pattern 3: Multiple Conditions

```groovy
rule = graph.addVertex(
    "type", "rule",
    "label", "critical-state",
    "Condition", "temperature > 80.0 && pressure > 100.0",
    "Action", "log.error('CRITICAL: High temp and pressure')"
)
```

### Pattern 4: Graph Query in Action

```groovy
rule = graph.addVertex(
    "type", "rule",
    "label", "shutdown-related",
    "Condition", "temperature > 100.0",
    "Action", "g.V(self).out('manages').property('shutdown', true)"
)
```

### Pattern 5: Command Execution

```groovy
rule = graph.addVertex(
    "type", "rule",
    "label", "restart-service",
    "Condition", "errorCount > 10",
    "Action", "commands.execute('restart-service', serviceId)"
)
```

## JEXL Context Variables

Available in conditions and actions:

| Variable   | Type                 | Description                      |
|------------|----------------------|----------------------------------|
| `log`      | Logger               | SLF4J logger for logging         |
| `g`        | GraphTraversal       | Gremlin traversal for queries    |
| `graph`    | Graph                | TinkerPop graph instance         |
| `commands` | CommandsFunction     | WaldOT console commands          |
| `self`     | RuleVertex           | Reference to this rule           |
| `Math`     | Math class           | Java Math functions              |
| `random`   | ThreadLocalRandom    | Random number generator          |

### Property Values

When a property changes, its value is available in the JEXL context:

```groovy
// If monitoring "temperature" property:
"Condition", "temperature > 80.0"  // Direct access to property value

// If monitoring "status" property:
"Condition", "status == 'ERROR'"

// Multiple properties in same rule:
"Condition", "temperature > 80.0 && pressure > 100.0"
```

## Configuration Options

### RuleVertex Properties

| Property    | Type    | Default     | Description                          |
|-------------|---------|-------------|--------------------------------------|
| Condition   | String  | "true"      | JEXL expression (must return boolean)|
| Action      | String  | log.info()  | JEXL expression to execute           |
| Priority    | Integer | 100         | Rule priority (higher = more urgent) |
| Hysteresis  | Long    | 0           | Event deduplication window (ms)      |
| Debug       | Integer | 0           | Debug level (0=off, 1=events, 2=all) |

### ComputeVertex Properties

| Property            | Type    | Default | Description                      |
|---------------------|---------|---------|----------------------------------|
| Threads             | Integer | 1       | Thread pool size                 |
| execution-timeout-ms| Long    | 120000  | Action timeout (ms)              |
| Factor              | Double  | 100.0   | Priority factor multiplier       |

### FireMonitoredEdge Properties

| Property          | Type    | Default | Description                          |
|-------------------|---------|---------|--------------------------------------|
| monitor-property  | String  | (none)  | Property name to monitor             |
| active            | Boolean | true    | Enable/disable monitoring            |
| priority          | Integer | 100     | Event priority                       |
| deadband          | Double  | 0.0     | Minimum change to trigger (absolute) |
| delay             | Long    | 0       | Delay before firing (ms)             |

### ComputeMonitoredEdge Properties

| Property | Type    | Default | Description      |
|----------|---------|---------|------------------|
| Priority | Integer | 100     | Execution priority|

## Debugging Rules

### Enable Debug Events

```groovy
rule.property("debug", 1)  // Enable OPC-UA debug events
rule.property("debug", 2)  // Enable debug events + log messages
```

Debug events published for each phase:
- Condition compile/execution
- Action compile/execution
- Exception details

### Monitor Metrics

```groovy
// Check rule metrics
println "Total events: ${rule.property('total').value()}"
println "Executed actions: ${rule.property('executed').value()}"
println "Errors: ${rule.property('errors').value()}"
println "Queue size: ${rule.property('queue').value()}"

// Check compute metrics
println "Dirty nodes: ${compute.property('queue').value()}"
```

### Log from Rules

```groovy
// Add logging to conditions and actions
"Condition", "log.info('Checking temperature: ' + temperature); temperature > 80.0"
"Action", "log.warn('Temperature alarm: ' + temperature); log.info('Sending notification')"
```

## Performance Tips

### 1. Keep Conditions Fast

Bad (slow):
```groovy
"Condition", "g.V().count().next() > 1000"  // Queries entire graph
```

Good (fast):
```groovy
"Condition", "nodeCount > 1000"  // Direct property check
```

### 2. Use Hysteresis for Noisy Sensors

```groovy
// Without hysteresis: 100 events/second = 100 actions/second
rule.property("hysteresis", 0)

// With hysteresis: 100 events/second = 1 action/second
rule.property("hysteresis", 1000)  // 1 second window
```

### 3. Avoid Blocking Operations

Bad (blocks thread):
```groovy
"Action", "Thread.sleep(1000); log.info('Delayed')"
```

Good (non-blocking):
```groovy
"Action", "log.info('Immediate')"
```

### 4. Adjust Thread Pool Size

```groovy
// Few long-running actions: small pool
compute.property("threads", 2)

// Many short actions: larger pool
compute.property("threads", 8)

// CPU-bound actions: CPU count
compute.property("threads", Runtime.getRuntime().availableProcessors())
```

### 5. Set Appropriate Priorities

```groovy
// Critical alarms: high priority
alarmRule.addEdge("execute", compute, "Priority", "1000")

// Low-priority logging: low priority
logRule.addEdge("execute", compute, "Priority", "10")
```

## Troubleshooting

### Rule Not Executing

**Check FireMonitoredEdge is active:**
```groovy
edge = tempSensor.edges(Direction.OUT, "fire").next()
edge.property("active").value()  // Should be true
```

**Check property name matches:**
```groovy
edge.property("monitor-property").value()  // Must match actual property
```

**Enable debug to see events:**
```groovy
rule.property("debug", 2)
```

### Slow Execution

**Check queue size:**
```groovy
rule.property("queue").value()  // High = backlog
compute.property("queue").value()  // High = saturation
```

**Increase threads:**
```groovy
compute.property("threads", 8)
```

**Simplify condition/action:**
```groovy
// Avoid complex graph queries in conditions
```

### Timeout Errors

**Increase timeout:**
```groovy
compute.property("execution-timeout-ms", 300000)  // 5 minutes
```

**Check for blocking operations:**
```groovy
// Remove Thread.sleep(), blocking I/O, etc.
```

## Next Steps

Now that you've created your first rule, explore more advanced topics:

1. **[JEXL Expressions Guide](JEXL_EXPRESSIONS.md)**: Learn advanced JEXL syntax and functions
2. **[Examples](EXAMPLES.md)**: Real-world use cases and patterns
3. **[Architecture](ARCHITECTURE.md)**: Deep dive into system design
4. **[API Reference](API_REFERENCE.md)**: Complete API documentation

## Additional Resources

- **WaldOT Documentation**: Main framework documentation
- **JEXL Reference**: Apache Commons JEXL documentation
- **Gremlin Docs**: TinkerPop Gremlin graph traversal language
- **OPC-UA Specification**: OPC Unified Architecture standard

---

*Quick Start Guide for WaldOT Rules Engine version 0.4.0+*
