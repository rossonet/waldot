# JEXL Expressions Guide

## Introduction

JEXL (Java Expression Language) is the scripting language used in WaldOT Rules Engine for writing rule conditions and actions. This guide covers JEXL syntax, available functions, context variables, and best practices.

## JEXL Basics

### Simple Expressions

```groovy
// Arithmetic
temperature + 10
pressure * 2
count / 100
voltage - 5

// Comparison
temperature > 80.0
pressure == 100.0
status != 'OK'
voltage >= 220.0

// Boolean logic
temperature > 80 && pressure > 100
status == 'ERROR' || status == 'CRITICAL'
!isHealthy

// String operations
status == 'RUNNING'
name =~ 'sensor.*'  // Regex match
errorMessage =~ '.*timeout.*'
```

### Operators

#### Arithmetic Operators
| Operator | Description      | Example        |
|----------|------------------|----------------|
| `+`      | Addition         | `a + b`        |
| `-`      | Subtraction      | `a - b`        |
| `*`      | Multiplication   | `a * b`        |
| `/`      | Division         | `a / b`        |
| `%`      | Modulo           | `a % b`        |

#### Comparison Operators
| Operator | Description           | Example        |
|----------|-----------------------|----------------|
| `==`     | Equal                 | `a == b`       |
| `!=`     | Not equal             | `a != b`       |
| `<`      | Less than             | `a < b`        |
| `<=`     | Less than or equal    | `a <= b`       |
| `>`      | Greater than          | `a > b`        |
| `>=`     | Greater than or equal | `a >= b`       |
| `=~`     | Regex match           | `str =~ '.*'`  |

#### Logical Operators
| Operator | Description | Example           |
|----------|-------------|-------------------|
| `&&`     | AND         | `a && b`          |
| `\|\|`   | OR          | `a \|\| b`        |
| `!`      | NOT         | `!a`              |

#### Other Operators
| Operator | Description       | Example            |
|----------|-------------------|--------------------|
| `?:`     | Ternary           | `a ? b : c`        |
| `.`      | Property access   | `obj.property`     |
| `[]`     | Array/Map access  | `arr[0]`, `map[key]`|

### Variables and Properties

Access context variables and property values:

```groovy
// Direct variable access
temperature
pressure
status
errorCount

// Property access
self.getNodeId()
sensor.property('temperature').value()

// Nested properties
device.sensor.value
config['timeout']
```

### Conditional Logic

```groovy
// Ternary operator
temperature > 80 ? 'HOT' : 'NORMAL'

// If-else (in action)
if (temperature > 80) {
    log.warn('Temperature high');
} else {
    log.info('Temperature normal');
}

// Short-circuit evaluation
temperature > 80 && log.warn('High temperature')
```

### Functions and Methods

Call Java methods and JEXL functions:

```groovy
// String methods
status.toUpperCase()
message.contains('error')
text.substring(0, 10)
name.startsWith('sensor')
errorMessage.length()

// Math functions
Math.abs(-10)
Math.max(temperature, 80)
Math.min(pressure, 100)
Math.sqrt(value)
Math.pow(base, exponent)

// Logging
log.info('Message')
log.warn('Warning')
log.error('Error')

// Random
random.nextInt(100)
random.nextDouble()
random.nextBoolean()
```

## Context Variables

### Standard Variables

Variables available in all rule conditions and actions:

#### log (SLF4J Logger)
```groovy
// Logging methods
log.trace('Trace message')
log.debug('Debug message')
log.info('Info message')
log.warn('Warning message')
log.error('Error message')

// With parameters
log.info('Temperature: {}', temperature)
log.warn('High temp: {} exceeds threshold: {}', temperature, threshold)
```

#### g (Gremlin Traversal)
```groovy
// Graph queries
g.V().hasLabel('sensor').count().next()
g.V(self).out('manages').toList()
g.V().has('temperature', gt(80)).toList()

// Find related vertices
g.V(self).out('connected-to').has('status', 'ERROR').toList()

// Complex traversals
g.V().hasLabel('device')
  .has('location', 'building-1')
  .out('has-sensor')
  .has('temperature', gt(80))
  .toList()
```

#### graph (TinkerPop Graph)
```groovy
// Graph operations
graph.vertices().hasNext()
graph.edges().hasNext()

// Typically use 'g' traversal instead
```

#### commands (WaldOT Commands)
```groovy
// Execute console commands
commands.execute('restart-service', serviceId)
commands.execute('send-alert', alertMessage)

// Check available commands
commands.list()
```

#### self (RuleVertex)
```groovy
// Access rule vertex itself
self.getNodeId()
self.getBrowseName()
self.property('label').value()

// Navigate from rule
g.V(self).out('execute').next()  // Get compute vertex
```

#### Math (Java Math Class)
```groovy
// Mathematical functions
Math.abs(value)
Math.ceil(value)
Math.floor(value)
Math.round(value)
Math.max(a, b)
Math.min(a, b)
Math.pow(base, exponent)
Math.sqrt(value)
Math.sin(angle)
Math.cos(angle)
Math.PI
Math.E
```

#### random (ThreadLocalRandom)
```groovy
// Random numbers
random.nextInt()
random.nextInt(100)  // 0-99
random.nextLong()
random.nextDouble()  // 0.0-1.0
random.nextBoolean()
random.nextGaussian()
```

### Property Values

When monitoring a property, its value is directly accessible:

```groovy
// Monitoring "temperature" property
"Condition", "temperature > 80.0"

// Monitoring "status" property
"Condition", "status == 'ERROR'"

// Multiple properties (if rule monitors multiple sources)
"Condition", "temperature > 80 && pressure > 100"
```

### Event Values

When monitoring events (not property changes), event data is available:

```groovy
// Access event fields
event.getMessage()
event.getSeverity()
event.getSourceName()
event.getTime()

// Check event type
event.getEventType() == NodeIds.AlarmEventType
```

## Conditions (IF Clause)

Conditions must return a boolean value (true/false).

### Simple Conditions

```groovy
// Threshold
"Condition", "temperature > 80.0"

// Equality
"Condition", "status == 'ERROR'"

// Range
"Condition", "temperature >= 20.0 && temperature <= 80.0"

// Negation
"Condition", "!isHealthy"
```

### Complex Conditions

```groovy
// Multiple conditions with AND
"Condition", "temperature > 80.0 && pressure > 100.0 && status == 'RUNNING'"

// Multiple conditions with OR
"Condition", "status == 'ERROR' || status == 'CRITICAL' || status == 'FAILED'"

// Mixed boolean logic
"Condition", "(temperature > 80 || pressure > 100) && status == 'RUNNING'"

// Regex matching
"Condition", "errorMessage =~ '.*timeout.*' || errorMessage =~ '.*connection.*'"
```

### Graph Query Conditions

```groovy
// Count related vertices
"Condition", "g.V(self).out('manages').count().next() > 10"

// Check if related vertex exists
"Condition", "g.V(self).out('connected-to').has('status', 'ERROR').hasNext()"

// Multiple graph queries
"Condition", "g.V().hasLabel('sensor').has('status', 'ERROR').count().next() > 5"
```

### Ternary Conditions

```groovy
// Use ternary for complex logic, but must return boolean
"Condition", "temperature > (status == 'CRITICAL' ? 70.0 : 80.0)"

// Not recommended: hard to read
"Condition", "status == 'ERROR' ? temperature > 70 : temperature > 90"
```

## Actions (THEN Clause)

Actions execute when condition is true. Can perform multiple operations.

### Logging Actions

```groovy
// Simple logging
"Action", "log.warn('Temperature alarm: ' + temperature)"

// Multiple log levels
"Action", "log.info('Checking temperature'); log.warn('Temperature high: ' + temperature)"

// Formatted logging
"Action", "log.error('CRITICAL: temp={} pressure={}', temperature, pressure)"
```

### Graph Modification Actions

```groovy
// Update property on related vertex
"Action", "g.V(self).out('manages').property('shutdown', true).iterate()"

// Create new vertex
"Action", "graph.addVertex('type', 'alert', 'message', 'High temperature', 'severity', 'HIGH')"

// Add edge
"Action", "self.addEdge('triggered-by', sourceVertex)"

// Remove edge
"Action", "g.V(self).outE('active').drop().iterate()"

// Update multiple properties
"Action", "g.V(self).property('lastAlert', System.currentTimeMillis()).property('alertCount', alertCount + 1).iterate()"
```

### Command Execution Actions

```groovy
// Execute single command
"Action", "commands.execute('restart-service', serviceId)"

// Multiple commands
"Action", "commands.execute('stop-service', serviceId); commands.execute('start-service', serviceId)"

// Conditional command execution
"Action", "if (temperature > 100) commands.execute('emergency-shutdown')"
```

### Complex Actions

```groovy
// Multi-line action with variable
"Action", "var msg = 'Temperature: ' + temperature + '°C'; log.warn(msg); commands.execute('send-alert', msg)"

// Conditional action
"Action", "if (temperature > 100) { log.error('CRITICAL'); commands.execute('shutdown'); } else { log.warn('WARNING'); }"

// Loop through related vertices
"Action", "g.V(self).out('manages').toList().forEach(v -> v.property('status', 'STOPPED'))"
```

## Best Practices

### 1. Keep Conditions Simple and Fast

**Good:**
```groovy
"Condition", "temperature > 80.0"
"Condition", "temperature > threshold && isActive"
```

**Bad (slow):**
```groovy
"Condition", "g.V().count().next() > 1000"  // Queries entire graph
"Condition", "g.V().hasLabel('sensor').toList().stream().allMatch(v -> v.property('status').value() == 'OK')"
```

### 2. Return Boolean from Conditions

**Good:**
```groovy
"Condition", "temperature > 80.0"  // Returns true/false
"Condition", "status == 'ERROR'"
```

**Bad:**
```groovy
"Condition", "temperature"  // Returns number, not boolean
"Condition", "log.info('Checking')"  // Returns void
```

### 3. Avoid Side Effects in Conditions

**Good:**
```groovy
"Condition", "temperature > 80.0"  // Pure check
```

**Bad:**
```groovy
"Condition", "log.info('Checking'); temperature > 80.0"  // Side effect (logging)
"Condition", "counter++; counter > 10"  // Side effect (mutation)
```

### 4. Use Meaningful Variable Names

**Good:**
```groovy
"Action", "var alertMessage = 'Temperature alarm: ' + temperature + '°C'; log.warn(alertMessage)"
```

**Bad:**
```groovy
"Action", "var x = 'Temperature alarm: ' + temperature + '°C'; log.warn(x)"
```

### 5. Handle Null Values

```groovy
// Check for null
"Condition", "temperature != null && temperature > 80.0"

// Use ternary for default
"Condition", "(temperature != null ? temperature : 0.0) > 80.0"

// Safe navigation
"Condition", "sensor?.property('temperature')?.value() > 80.0"
```

### 6. Avoid Blocking Operations

**Good (non-blocking):**
```groovy
"Action", "log.info('Temperature: ' + temperature)"
"Action", "commands.execute('send-alert', message)"
```

**Bad (blocking):**
```groovy
"Action", "Thread.sleep(1000); log.info('Delayed')"  // Blocks thread
"Action", "var url = new URL('http://example.com'); url.openConnection()"  // Blocking I/O
```

### 7. Limit Graph Queries in Actions

**Good:**
```groovy
"Action", "g.V(self).property('lastAlert', System.currentTimeMillis()).iterate()"  // Single update
```

**Bad:**
```groovy
"Action", "g.V().hasLabel('sensor').toList().forEach(v -> v.property('updated', true))"  // Updates all sensors
```

### 8. Use JEXL Comments

```groovy
// Single-line comment
"Condition", "temperature > 80.0  // Check if temperature exceeds threshold"

// Multi-line comment
"Action", "/* Log warning message */ log.warn('High temperature'); /* Send alert */ commands.execute('alert')"
```

## Common Patterns

### Pattern 1: Threshold with Hysteresis

```groovy
// Use rule hysteresis property instead of logic
rule.property("hysteresis", 5000)  // 5 second deduplication
"Condition", "temperature > 80.0"
```

### Pattern 2: State Machine

```groovy
// Track state in vertex property
"Condition", "temperature > 80.0 && state != 'ALARMED'"
"Action", "self.property('state', 'ALARMED'); log.warn('Alarm triggered')"

// Reset action (separate rule)
"Condition", "temperature <= 70.0 && state == 'ALARMED'"
"Action", "self.property('state', 'NORMAL'); log.info('Alarm cleared')"
```

### Pattern 3: Aggregation

```groovy
// Count errors in last time window
"Condition", "g.V().hasLabel('error').has('timestamp', gt(System.currentTimeMillis() - 60000)).count().next() > 10"
"Action", "log.error('More than 10 errors in last minute')"
```

### Pattern 4: Rate Limiting

```groovy
// Check last execution time
"Condition", "temperature > 80.0 && (System.currentTimeMillis() - lastExecution) > 60000"
"Action", "self.property('lastExecution', System.currentTimeMillis()); log.warn('Temperature high')"
```

### Pattern 5: Cascade Actions

```groovy
// Trigger multiple related actions
"Action", "var devices = g.V(self).out('controls').toList(); devices.forEach(d -> d.property('shutdown', true)); log.warn('Shutdown ' + devices.size() + ' devices')"
```

### Pattern 6: Conditional Severity

```groovy
// Different actions based on severity
"Condition", "temperature > 70.0"
"Action", "if (temperature > 100) { log.error('CRITICAL'); commands.execute('shutdown'); } else if (temperature > 90) { log.warn('HIGH'); commands.execute('reduce-load'); } else { log.info('ELEVATED'); }"
```

## Advanced Techniques

### Lambda Expressions

```groovy
// Filter with lambda
"Action", "var highTempSensors = g.V().hasLabel('sensor').toList().stream().filter(v -> v.property('temperature').value() > 80).collect(Collectors.toList())"

// Map with lambda
"Action", "var temps = g.V().hasLabel('sensor').toList().stream().map(v -> v.property('temperature').value()).collect(Collectors.toList())"
```

### Collection Operations

```groovy
// Iterate list
"Action", "var sensors = g.V().hasLabel('sensor').toList(); sensors.forEach(s -> log.info(s.property('label').value()))"

// Filter and count
"Action", "var errorCount = g.V().hasLabel('device').toList().stream().filter(v -> v.property('status').value() == 'ERROR').count()"
```

### String Formatting

```groovy
// String concatenation
"Action", "var msg = 'Temperature: ' + temperature + '°C, Pressure: ' + pressure + ' bar'; log.warn(msg)"

// String format (Java style)
"Action", "var msg = String.format('Temp: %.2f°C, Pressure: %.1f bar', temperature, pressure); log.warn(msg)"
```

### Type Conversion

```groovy
// Parse numbers
"Condition", "Integer.parseInt(statusCode) > 400"
"Condition", "Double.parseDouble(value) > 80.0"

// Convert to string
"Action", "var msg = 'Count: ' + Integer.toString(count); log.info(msg)"
```

### Error Handling

```groovy
// Try-catch in action
"Action", "try { commands.execute('risky-operation'); log.info('Success'); } catch (e) { log.error('Failed: ' + e.message); }"

// Safe access with null check
"Condition", "sensor != null && sensor.property('temperature') != null && sensor.property('temperature').value() > 80"
```

## Performance Considerations

### Fast Operations (< 1ms)
- Arithmetic: `a + b`, `a * b`
- Comparison: `a > b`, `a == b`
- Boolean logic: `a && b`, `a || b`
- Property access: `temperature`, `self.property('label')`

### Medium Operations (1-10ms)
- String operations: `str.contains()`, `str.substring()`
- Simple graph queries: `g.V(self).out().count().next()`
- Math functions: `Math.sqrt()`, `Math.pow()`

### Slow Operations (> 10ms)
- Large graph queries: `g.V().hasLabel('sensor').toList()`
- Collection iterations: `.forEach()`, `.stream().filter()`
- Command execution: `commands.execute()`
- Graph modifications: `graph.addVertex()`, `vertex.addEdge()`

### Optimization Tips

1. **Cache computed values:**
   ```groovy
   // Bad: Computes twice
   "Condition", "g.V(self).out().count().next() > 10 && g.V(self).out().count().next() < 100"
   
   // Good: Compute once
   "Condition", "var count = g.V(self).out().count().next(); count > 10 && count < 100"
   ```

2. **Use indexed properties:**
   ```groovy
   // Fast: Uses index
   "Condition", "g.V().has('nodeId', nodeId).hasNext()"
   
   // Slow: Full scan
   "Condition", "g.V().toList().stream().anyMatch(v -> v.property('customField').value() == targetValue)"
   ```

3. **Limit result sets:**
   ```groovy
   // Bad: Returns all
   "Action", "g.V().hasLabel('sensor').toList()"
   
   // Good: Limits to 100
   "Action", "g.V().hasLabel('sensor').limit(100).toList()"
   ```

## Troubleshooting

### Syntax Errors

**Error: "Expected ')'"**
```groovy
// Wrong
"Condition", "temperature > 80 && (pressure > 100"

// Fixed
"Condition", "temperature > 80 && (pressure > 100)"
```

**Error: "Unknown variable"**
```groovy
// Wrong: typo in variable name
"Condition", "temprature > 80"

// Fixed
"Condition", "temperature > 80"
```

### Type Errors

**Error: "Cannot compare X with Y"**
```groovy
// Wrong: comparing string with number
"Condition", "'80' > 70"

// Fixed
"Condition", "80 > 70"
```

**Error: "Boolean expected, got X"**
```groovy
// Wrong: condition returns number
"Condition", "temperature"

// Fixed
"Condition", "temperature > 80"
```

### Null Pointer Errors

```groovy
// Wrong: sensor might be null
"Condition", "sensor.property('temperature').value() > 80"

// Fixed: check for null
"Condition", "sensor != null && sensor.property('temperature') != null && sensor.property('temperature').value() > 80"
```

## References

- **Apache Commons JEXL**: https://commons.apache.org/proper/commons-jexl/
- **Gremlin Documentation**: https://tinkerpop.apache.org/gremlin.html
- **SLF4J Logger**: https://www.slf4j.org/
- **Java Math Class**: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html

---

*JEXL Expressions Guide for WaldOT Rules Engine version 0.4.0+*
