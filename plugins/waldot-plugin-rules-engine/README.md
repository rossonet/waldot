# WaldOT Rules Engine Plugin

Event-driven IF-THEN-THAT rule execution system for WaldOT framework.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![WaldOT](https://img.shields.io/badge/WaldOT-0.4.0%2B-green.svg)](../../README.md)

## Overview

The WaldOT Rules Engine plugin enables sophisticated automation and monitoring by implementing IF-THEN-THAT style rules that react to OPC-UA events and property changes in a graph-based architecture. Rules are expressed using JEXL (Java Expression Language) for maximum flexibility and power.

### Key Features

- **Event-Driven Architecture**: Rules react to OPC-UA events and property changes in real-time
- **JEXL Expressions**: Powerful scripting for both conditions (IF) and actions (THEN)
- **Priority Queuing**: Events processed by priority with hysteresis deduplication
- **Thread Management**: Virtual thread pool for lightweight concurrent execution
- **Monitoring Edges**: FireMonitoredEdge filters events, ComputeMonitoredEdge routes to execution
- **Debug Support**: Comprehensive debug events and metrics for troubleshooting
- **Graph Integration**: Full access to TinkerPop graph and Gremlin traversal in rules
- **OPC-UA Native**: Rules exposed as OPC-UA objects with standard properties and events

### Architecture

```
[Source Node] → [FireMonitoredEdge] → [RuleVertex] → [ComputeMonitoredEdge] → [ComputeVertex]
   (event)         (filters)          (enqueues)          (routes)            (executes)
```

**Components:**
- **RuleVertex**: IF-THEN rule with JEXL condition and action
- **ComputeVertex**: Thread manager with priority execution
- **ComputeMonitoredEdge**: Connects rules to compute for execution
- **FireMonitoredEdge**: Monitors sources and fires rules (from waldot-namespace)

## Quick Start

### 1. Create Compute Vertex

```groovy
compute = graph.addVertex(
    "type", "compute",
    "label", "main-compute",
    "Threads", "4",  // 4 concurrent threads
    "execution-timeout-ms", "120000",  // 2 minute timeout
    "Factor", "100.0"  // Priority factor
)
```

### 2. Create Rule

```groovy
rule = graph.addVertex(
    "type", "rule",
    "label", "temp-alarm",
    "Condition", "temperature > 80.0",
    "Action", "log.warn('Temperature alarm: ' + temperature + '°C')",
    "Priority", "100",
    "Hysteresis", "5000",  // 5 seconds deduplication
    "Debug", "0"  // Debug level (0=off, 1=events, 2=all)
)
```

### 3. Connect Rule to Compute

```groovy
rule.addEdge("execute", compute, "Priority", "100")
```

### 4. Monitor Source

```groovy
tempSensor = g.V().hasLabel("temperature-sensor").has("label", "sensor1").next()

tempSensor.addEdge("fire", rule, 
    "monitor-property", "temperature",
    "active", "true",
    "priority", "100"
)
```

### 5. Test

```groovy
tempSensor.property("temperature", 85.0)
// Output: WARN Temperature alarm: 85.0°C
```

## Rule Components

### RuleVertex Properties

| Property    | Type    | Default     | Description                          |
|-------------|---------|-------------|--------------------------------------|
| `Condition` | String  | "true"      | JEXL expression (must return boolean)|
| `Action`    | String  | log.info()  | JEXL expression to execute           |
| `Priority`  | Integer | 100         | Rule priority                        |
| `Hysteresis`| Long    | 0           | Event deduplication window (ms)      |
| `Debug`     | Integer | 0           | Debug level (0=off, 1=events, 2=all)|
| `Queue`     | Long    | 0           | Current queue size (read-only)       |
| `Total`     | Long    | 0           | Total events received (read-only)    |
| `Executed`  | Long    | 0           | Actions executed (read-only)         |
| `Errors`    | Long    | 0           | Errors during execution (read-only)  |

### ComputeVertex Properties

| Property              | Type    | Default | Description                      |
|-----------------------|---------|---------|----------------------------------|
| `Threads`             | Integer | 1       | Thread pool size                 |
| `execution-timeout-ms`| Long    | 120000  | Action timeout (ms)              |
| `Factor`              | Double  | 100.0   | Priority factor multiplier       |
| `Queue`               | Integer | 0       | Dirty nodes in queue (read-only) |

### JEXL Context Variables

Available in all rule conditions and actions:

| Variable   | Type                 | Description                      |
|------------|----------------------|----------------------------------|
| `log`      | Logger               | SLF4J logger for logging         |
| `g`        | GraphTraversal       | Gremlin traversal for queries    |
| `graph`    | Graph                | TinkerPop graph instance         |
| `commands` | CommandsFunction     | WaldOT console commands          |
| `self`     | RuleVertex           | Reference to this rule           |
| `Math`     | Math class           | Java Math functions              |
| `random`   | ThreadLocalRandom    | Random number generator          |

## Examples

### Example 1: Temperature Threshold

```groovy
rule = graph.addVertex(
    "type", "rule",
    "label", "temp-warning",
    "Condition", "temperature > 80.0",
    "Action", "log.warn('High temperature: ' + temperature)"
)
```

### Example 2: Multiple Conditions

```groovy
rule = graph.addVertex(
    "type", "rule",
    "label", "critical-state",
    "Condition", "temperature > 80.0 && pressure > 100.0",
    "Action", "log.error('CRITICAL'); commands.execute('shutdown')"
)
```

### Example 3: Graph Query

```groovy
rule = graph.addVertex(
    "type", "rule",
    "label", "multi-sensor-failure",
    "Condition", "g.V().hasLabel('sensor').has('status', 'ERROR').count().next() > 5",
    "Action", "log.error('Multiple sensor failures detected')"
)
```

### Example 4: State Machine

```groovy
// Trigger alarm
alarmRule = graph.addVertex(
    "type", "rule",
    "label", "alarm-trigger",
    "Condition", "temperature > 80.0 && state != 'ALARMED'",
    "Action", "self.property('state', 'ALARMED'); log.warn('Alarm triggered')"
)

// Clear alarm
clearRule = graph.addVertex(
    "type", "rule",
    "label", "alarm-clear",
    "Condition", "temperature <= 70.0 && state == 'ALARMED'",
    "Action", "self.property('state', 'NORMAL'); log.info('Alarm cleared')"
)
```

## Priority Mechanism

ComputeVertex prioritizes rules based on:

```
weight = edge_priority × priority_factor + queue_size
```

**Example:**
- Rule A: edge_priority=100, queue_size=5, factor=100.0 → weight=10,005
- Rule B: edge_priority=50, queue_size=10, factor=100.0 → weight=5,010

Rule A is processed first (higher weight).

## Hysteresis Mechanism

Hysteresis prevents event flooding by deduplicating events within a time window:

```groovy
rule.property("hysteresis", 5000)  // 5 seconds
```

If 100 events arrive within 5 seconds, only 1 is queued and processed.

## Debug Features

Enable debug to publish OPC-UA events for each execution phase:

```groovy
rule.property("debug", 1)  // Enable debug events
rule.property("debug", 2)  // Enable debug events + log messages
```

Debug events published:
- `BEFORE_CONDITION_COMPILE` / `AFTER_CONDITION_COMPILE`
- `BEFORE_CONDITION_EXECUTION` / `AFTER_CONDITION_EXECUTION`
- `BEFORE_ACTION_COMPILE` / `AFTER_ACTION_COMPILE`
- `BEFORE_ACTION_EXECUTION` / `AFTER_ACTION_EXECUTION`
- `*_EXCEPTION` variants for errors

## Documentation

Complete documentation available in the `docs/` directory:

- **[Quick Start Guide](docs/QUICKSTART.md)**: Get started in 5 minutes
- **[JEXL Expressions Guide](docs/JEXL_EXPRESSIONS.md)**: Complete JEXL reference and best practices
- **[Examples](docs/EXAMPLES.md)**: Real-world use cases and patterns
- **[Architecture](docs/ARCHITECTURE.md)**: Deep dive into system design and internals
- **[API Reference](docs/API_REFERENCE.md)**: Complete API documentation

## Performance

### Characteristics

- **Memory**: ~1KB per virtual thread, ~100 bytes per queued event
- **Latency**: <1ms for simple conditions, depends on action complexity
- **Throughput**: Thousands of events per second
- **Scalability**: Handles thousands of concurrent rules and threads

### Optimization Tips

1. **Keep conditions simple**: Fast boolean checks
2. **Use hysteresis**: Prevent flooding from noisy sensors
3. **Avoid blocking in actions**: No sleeps, waits, or blocking I/O
4. **Set appropriate priorities**: Critical rules get high priority
5. **Adjust thread pool**: Match workload (CPU-bound vs I/O-bound)

## Requirements

- **Java**: 21 or higher (for virtual threads)
- **WaldOT**: 0.4.0 or higher
- **Dependencies**:
  - Apache Commons JEXL 3.x
  - Apache TinkerPop 3.x
  - Eclipse Milo (OPC-UA)

## Building

```bash
./gradlew build
```

## Installation

The plugin is automatically discovered by WaldOT via the `@WaldotPlugin` annotation. Simply include it in your WaldOT installation classpath.

```gradle
dependencies {
    implementation project(':plugins:waldot-plugin-rules-engine')
}
```

## Configuration

No external configuration required. All configuration is done via vertex and edge properties at runtime.

## Thread Safety

The plugin is fully thread-safe:
- Concurrent data structures for queues and collections
- Virtual threads for isolated action execution
- OPC-UA framework handles property synchronization

## Troubleshooting

### Rule Not Executing

1. Check FireMonitoredEdge is active: `edge.property("active").value()`
2. Verify property name matches: `edge.property("monitor-property").value()`
3. Enable debug: `rule.property("debug", 2)`

### Slow Execution

1. Check queue sizes: `rule.property("queue").value()`, `compute.property("queue").value()`
2. Increase threads: `compute.property("threads", 8)`
3. Simplify conditions/actions

### Timeout Errors

1. Increase timeout: `compute.property("execution-timeout-ms", 300000)`
2. Check for blocking operations in actions
3. Simplify action logic

## Contributing

Contributions welcome! Please see the main [WaldOT README](../../README.md) for contribution guidelines.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) file for details.

## Authors

- Andrea Ambrosini - Rossonet s.c.a r.l. - Initial development

## See Also

- [WaldOT Framework](../../README.md)
- [Apache Commons JEXL](https://commons.apache.org/proper/commons-jexl/)
- [Apache TinkerPop](https://tinkerpop.apache.org/)
- [Eclipse Milo](https://github.com/eclipse/milo)

---

**WaldOT Rules Engine Plugin** - Event-driven automation for industrial IoT and beyond.
