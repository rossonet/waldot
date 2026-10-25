# WaldOT Data Generator Plugin

Dynamic data simulation for testing, development, and demonstrations.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![WaldOT](https://img.shields.io/badge/WaldOT-0.4.0%2B-green.svg)](../../README.md)

## Overview

The Data Generator plugin creates vertices that continuously generate changing values using configurable algorithms. Perfect for simulating sensors, devices, and dynamic data sources without physical hardware.

### Key Features

- **6 Generation Algorithms**: incremental, decremental, random, sinusoidal, triangular, stopped
- **Event Generator**: `sim:event-generator` publishes simulated OPC UA events (alarms, logs) from message lists and templates
- **Real-time Updates**: Configurable update intervals from 10ms to hours
- **Virtual Threads**: Lightweight concurrency for thousands of generators
- **OPC-UA Integration**: Full bidirectional sync with OPC-UA address space
- **Runtime Configuration**: Change algorithms and parameters without restart
- **Graph Queries**: Standard Gremlin queries on generated data

## Quick Start

```groovy
// Create temperature sensor simulator
tempSensor = graph.addVertex(
    "type", "sim:generator",
    "label", "office-temp",
    "Algorithm", "sinusoidal",
    "Min", "18",
    "Max", "26",
    "Delay", "5000"  // Update every 5 seconds
)

// Read current value
temperature = tempSensor.property("data").value()

// Change algorithm
tempSensor.property("Algorithm", "random")
```

## Algorithms

| Algorithm    | Pattern        | Use Case                    |
|--------------|----------------|-----------------------------|
| incremental  | Linear up      | Counters, timers            |
| decremental  | Linear down    | Countdown                   |
| random       | Random values  | Noisy sensors               |
| sinusoidal   | Sine wave      | Temperature cycles, AC      |
| triangular   | Triangle wave  | Sawtooth, PWM               |
| stopped      | Constant       | Paused simulation           |

## Configuration

| Property  | Type   | Default | Description             |
|-----------|--------|---------|-------------------------|
| Algorithm | String | incremental | Generation algorithm |
| Delay     | Long   | 1000    | Update interval (ms)    |
| Min       | Long   | 0       | Minimum value           |
| Max       | Long   | 20000   | Maximum value           |
| data      | Double | (random)| Generated value (read-only) |

## Examples

### Production Counter

```groovy
counter = graph.addVertex(
    "type", "sim:generator",
    "label", "items-produced",
    "Algorithm", "incremental",
    "Min", "0",
    "Max", "1000000",
    "Delay", "60000"  // Increment every minute
)
```

### Random Sensor

```groovy
vibrationSensor = graph.addVertex(
    "type", "sim:generator",
    "label", "vibration",
    "Algorithm", "random",
    "Min", "0",
    "Max", "100",
    "Delay", "100"  // 10 Hz updates
)
```

### Load Testing

```groovy
// Create 1000 generators
1000.times { i ->
    graph.addVertex(
        "type", "sim:generator",
        "label", "sensor-${i}",
        "Algorithm", "random",
        "Delay", "1000"
    )
}
```

## Event Generator (`sim:event-generator`)

The `sim:event-generator` vertex simulates a source of **OPC UA events** (alarms, log lines, device messages). At every cycle it builds one or more texts from a list of messages and a template and publishes each of them as an OPC UA event; the events reach the vertices connected with a `fire` edge (for example `syslog:sender` or `snmp:trap-sender`). The text of the last event is also written in the `data` property, so it can be copied to other vertices with a `link-to` edge. Every parameter is read again at every cycle and can be changed at runtime. The first event is generated after one `delay`, so the edges created right after the vertex receive it.

| Property | Default | Description |
| --- | --- | --- |
| `algorithm` | `sequential` | `sequential` (one event per cycle, messages in order), `random` (one random message per cycle), `burst` (`burst-size` events per cycle), `stopped` |
| `delay` | `1000` | ms between two cycles (minimum 10) |
| `jitter` | `0` | random extra wait in ms (0 - jitter) added to every cycle |
| `messages` | `event ${counter}` | messages divided by `separator` |
| `separator` | `\|` | separator of `messages` and `severities` |
| `template` | `${message}` | text of the event |
| `category` | `simulated` | name of the events (browse name), placeholders allowed |
| `event-type` | `system` | `base`, `system`, `device-failure`, `audit`, `progress`, `system-status` |
| `min-severity` / `max-severity` | `100` / `500` | random severity (1-1000) when `severities` is empty |
| `severities` | | one severity per message (the last one is repeated) |
| `burst-size` | `5` | events per cycle with `burst` |
| `max-events` | `0` | after this number of events the algorithm becomes `stopped` (0 = no limit) |
| `random-min` / `random-max` | `0` / `100` | range of `${random}` |
| `seed` | | seed of the random choices, to repeat the same sequence |

Placeholders of `messages`, `template` and `category`: `${message}`, `${counter}`, `${index}`, `${time}` (ISO 8601), `${epoch}`, `${severity}`, `${random}`, `${source}`, `${category}` and `${p:name}` (value of the property `name` of the vertex, for example fed by a `link-to` edge). Runtime properties: `data` (text of the last event), `count`, `severity`.

```groovy
boiler = graph.addVertex("type", "sim:event-generator", "label", "boiler",
    "algorithm", "sequential", "delay", "2000",
    "messages", "water temperature ${p:temperature} C|flame lost|pressure high",
    "severities", "200|700|900", "category", "boiler", "event-type", "device-failure")
temperature = graph.addVertex("type", "sim:generator", "label", "temp", "algorithm", "sinusoidal",
    "min", "60", "max", "100", "delay", "1000")
temperature.addEdge("feeds", boiler, "type", "link-to", "properties-to-monitor", "data", "data", "temperature")
boiler.addEdge("to-syslog", syslogSender, "type", "fire")
```

The vertex is created also by the OPC UA command `create event generator` (folder `simulator`).

## Documentation

Complete documentation in `docs/` directory:

- **[Quick Start](docs/QUICKSTART.md)**: Get started in 5 minutes
- **[Algorithms](docs/ALGORITHMS.md)**: Mathematical details of each algorithm
- **[Architecture](docs/ARCHITECTURE.md)**: Internal design and implementation

## Performance

- **Memory**: ~1.7KB per generator
- **CPU**: ~0.001% per generator @ 1 Hz
- **Scalability**: Tested with 100,000+ generators
- **Latency**: < 1ms jitter between updates

### Scalability Limits

| Generators | Update Rate | CPU Usage | Memory |
|-----------|-------------|-----------|--------|
| 1,000     | 1 Hz        | ~1%       | ~2 MB  |
| 10,000    | 1 Hz        | ~10%      | ~20 MB |
| 100,000   | 1 Hz        | System dependent | ~170 MB |

## Requirements

- **Java**: 21+ (for virtual threads)
- **WaldOT**: 0.4.0+
- **Dependencies**: Included in WaldOT framework

## Installation

Plugin auto-discovered via `@WaldotPlugin` annotation. Include in classpath:

```gradle
dependencies {
    implementation project(':plugins:waldot-plugin-generator')
}
```

## Use Cases

1. **Testing**: Simulate sensor behavior without hardware
2. **Development**: Develop applications with realistic data
3. **Demos**: Showcase capabilities with live changing data
4. **Load Testing**: Generate thousands of data points
5. **Rule Testing**: Verify rule engine with dynamic inputs
6. **Training**: Learn WaldOT with self-contained examples

## Troubleshooting

### Generator Not Updating

```groovy
// Check status
g.V().has("label", "my-gen")
    .valueMap("Algorithm", "Delay", "data")
    .next()

// Verify not stopped
gen.property("Algorithm", "incremental")
```

### High CPU Usage

```groovy
// Find fast generators
g.V().has("type", "sim:generator")
    .has("Delay", lt(100))
    .toList()

// Slow them down
fastGens.each { it.property("Delay", 1000) }
```

## Contributing

Contributions welcome! See main [WaldOT README](../../README.md) for guidelines.

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.

## Authors

Andrea Ambrosini - Zona Autonoma srl

## See Also

- [WaldOT Framework](../../README.md)
- [WaldOT Rules Engine](../waldot-plugin-rules-engine/README.md)
- [Apache TinkerPop](https://tinkerpop.apache.org/)

---

**WaldOT Data Generator Plugin** - Simulate dynamic data for testing and development.
