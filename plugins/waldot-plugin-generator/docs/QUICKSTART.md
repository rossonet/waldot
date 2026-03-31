# WaldOT Data Generator Plugin - Quick Start Guide

## Introduction

The Data Generator plugin simulates dynamic data sources for testing, development, and demos. Create virtual sensors that generate realistic data patterns without physical hardware.

## Prerequisites

- WaldOT framework 0.4.0+
- waldot-plugin-generator plugin loaded
- Java 21+ (for virtual threads)

## Your First Generator

### Step 1: Create a Simple Generator

```groovy
// Create incrementing counter from 0 to 100, updating every second
generator = graph.addVertex(
    "type", "generator",
    "label", "counter-1",
    "Algorithm", "incremental",
    "Min", "0",
    "Max", "100",
    "Delay", "1000"  // 1 second
)
```

### Step 2: Read Generated Values

```groovy
// Read current value
value = generator.property("data").value()
println "Current value: ${value}"

// Wait and read again
Thread.sleep(2000)
newValue = generator.property("data").value()
println "New value: ${newValue}"  // Should be value + 2
```

### Step 3: Change Algorithm at Runtime

```groovy
// Switch to random generation
generator.property("Algorithm", "random")

// Values now random between 0 and 100
```

## Common Patterns

### Pattern 1: Temperature Sensor Simulation

```groovy
tempSensor = graph.addVertex(
    "type", "generator",
    "label", "office-temp",
    "Algorithm", "sinusoidal",  // Smooth cycles
    "Min", "18",
    "Max", "26",
    "Delay", "5000"  // Update every 5 seconds
)
```

### Pattern 2: Random Noise Generator

```groovy
noiseSensor = graph.addVertex(
    "type", "generator",
    "label", "vibration-sensor",
    "Algorithm", "random",
    "Min", "0",
    "Max", "100",
    "Delay", "100"  // Fast updates (10 Hz)
)
```

### Pattern 3: Production Counter

```groovy
productionCounter = graph.addVertex(
    "type", "generator",
    "label", "items-produced",
    "Algorithm", "incremental",
    "Min", "0",
    "Max", "1000000",  // Million items
    "Delay", "60000"  // Increment every minute
)
```

### Pattern 4: Stopped Generator (Static Value)

```groovy
constant = graph.addVertex(
    "type", "generator",
    "label", "reference-voltage",
    "Algorithm", "stopped",
    "Min", "0",
    "Max", "5",
    "Delay", "1000"
)
// Value stays constant at initial random value
```

## Configuration Reference

### Algorithm Options

| Algorithm    | Description                    | Use Case                          |
|--------------|--------------------------------|-----------------------------------|
| incremental  | Value increases, wraps at max  | Counters, sequential IDs          |
| decremental  | Value decreases, wraps at min  | Countdown timers                  |
| random       | Random value in range          | Noisy sensors, unpredictable data |
| sinusoidal   | Smooth sine wave               | Cyclic patterns (temp, voltage)   |
| triangular   | Linear ramps up/down           | Sawtooth patterns                 |
| stopped      | No generation, static value    | Reference values, paused sensors  |

### Properties

| Property   | Type   | Default | Range        | Description                |
|------------|--------|---------|--------------|----------------------------|
| Algorithm  | String | incremental | See above | Generation algorithm       |
| Delay      | Long   | 1000    | >= 10 ms     | Update interval (ms)       |
| Min        | Long   | 0       | Any          | Minimum value              |
| Max        | Long   | 20000   | > Min        | Maximum value              |
| data       | Double | (random)| [Min, Max]   | Generated value (read-only)|

## OPC-UA Integration

### Browsing Generators

Connect with any OPC-UA client (UAExpert, Prosys, etc.):

```
opc.tcp://localhost:4840
└── waldot-namespace
    └── generators/
        └── counter-1/
            ├── Algorithm = "incremental"
            ├── Delay = 1000
            ├── Min = 0
            ├── Max = 100
            └── data = 47  (current value)
```

### Reading Values

Subscribe to `data` property for real-time updates:

```csharp
// Pseudo-code (OPC-UA client)
node = client.getNode("counter-1")
subscription = client.createSubscription()
subscription.monitor(node.getProperty("data"))
subscription.onDataChange(value => {
    console.log("New value: " + value)
})
```

### Writing Configuration

Change parameters via OPC-UA:

```csharp
node.getProperty("Algorithm").write("random")
node.getProperty("Delay").write(500)  // 0.5 seconds
```

## Gremlin Queries

### Find All Generators

```groovy
generators = g.V().has("type", "generator").toList()
println "Total generators: ${generators.size()}"
```

### Filter by Algorithm

```groovy
randomGens = g.V()
    .has("type", "generator")
    .has("Algorithm", "random")
    .toList()
```

### Get Current Values

```groovy
values = g.V()
    .has("type", "generator")
    .valueMap("label", "data")
    .toList()

values.each { println "${it}" }
```

### Change All Delays

```groovy
g.V().has("type", "generator").property("Delay", 2000).iterate()
```

## Performance Tips

### 1. Appropriate Update Frequency

```groovy
// BAD: Too fast, wastes CPU
fastGen.property("Delay", 1)  // 1000 updates/sec

// GOOD: Match real sensor frequency
goodGen.property("Delay", 1000)  // 1 update/sec
```

### 2. Batch Creation

```groovy
// Create 1000 generators efficiently
1000.times { i ->
    graph.addVertex(
        "type", "generator",
        "label", "sensor-${i}",
        "Algorithm", "random",
        "Delay", "1000"
    )
}
```

### 3. Stop Unused Generators

```groovy
// Pause generation without removing
generator.property("Algorithm", "stopped")

// Resume later
generator.property("Algorithm", "sinusoidal")
```

## Troubleshooting

### Generator Not Updating

**Check algorithm:**
```groovy
alg = generator.property("Algorithm").value()
println "Algorithm: ${alg}"
// If "stopped", change it
```

**Check delay:**
```groovy
delay = generator.property("Delay").value()
println "Delay: ${delay} ms"
// Must be >= 10ms
```

### Values Out of Range

Values should always be in [Min, Max]. If not:

```groovy
min = generator.property("Min").value()
max = generator.property("Max").value()
data = generator.property("data").value()

if (data < min || data > max) {
    println "ERROR: value ${data} outside [${min}, ${max}]"
    // Restart generator
    generator.remove()
    // Create new one
}
```

### High CPU Usage

```groovy
// Find generators with low delay
fastGens = g.V()
    .has("type", "generator")
    .has("Delay", lt(100))
    .toList()

println "Fast generators (< 100ms): ${fastGens.size()}"

// Increase their delay
fastGens.each { it.property("Delay", 1000) }
```

## Next Steps

- **[Algorithms Guide](ALGORITHMS.md)**: Deep dive into generation algorithms
- **[Examples](EXAMPLES.md)**: Real-world use cases
- **[API Reference](API_REFERENCE.md)**: Complete API documentation
- **[Architecture](ARCHITECTURE.md)**: Internal design details

## Common Use Cases

### 1. Testing Rule Engine

```groovy
// Create sensor that triggers rules
sensor = graph.addVertex(
    "type", "generator",
    "label", "test-sensor",
    "Algorithm", "incremental",
    "Min", "0",
    "Max", "150",
    "Delay", "1000"
)

// Create rule: alert when > 100
rule = graph.addVertex(
    "type", "rule",
    "Condition", "temperature > 100",
    "Action", "log.warn('High temperature')"
)

// Connect
sensor.addEdge("fire", rule, "monitor-property", "data")
```

### 2. Load Testing

```groovy
// Create 10,000 generators
10000.times { i ->
    graph.addVertex(
        "type", "generator",
        "label", "load-gen-${i}",
        "Algorithm", ["random", "incremental", "sinusoidal"][i % 3],
        "Delay", "1000"
    )
}

// Monitor system resources
```

### 3. Demo Dashboard

```groovy
// Create variety of sensors for demo
temps = (1..5).collect { i ->
    graph.addVertex(
        "type", "generator",
        "label", "temp-${i}",
        "Algorithm", "sinusoidal",
        "Min", "15",
        "Max", "30",
        "Delay", "${1000 + i * 200}"  // Slightly different frequencies
    )
}

pressures = (1..3).collect { i ->
    graph.addVertex(
        "type", "generator",
        "label", "pressure-${i}",
        "Algorithm", "triangular",
        "Min", "95",
        "Max", "105",
        "Delay", "2000"
    )
}
```

---

*Quick Start Guide for WaldOT Data Generator Plugin version 0.4.0+*
