# WaldOT Data Generator Plugin

Dynamic data simulation for testing, development, and demonstrations.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![WaldOT](https://img.shields.io/badge/WaldOT-0.4.0%2B-green.svg)](../../README.md)

## Overview

The Data Generator plugin creates vertices that continuously generate changing values using configurable algorithms. Perfect for simulating sensors, devices, and dynamic data sources without physical hardware.

### Key Features

- **6 Generation Algorithms**: incremental, decremental, random, sinusoidal, triangular, stopped
- **Real-time Updates**: Configurable update intervals from 10ms to hours
- **Virtual Threads**: Lightweight concurrency for thousands of generators
- **OPC-UA Integration**: Full bidirectional sync with OPC-UA address space
- **Runtime Configuration**: Change algorithms and parameters without restart
- **Graph Queries**: Standard Gremlin queries on generated data

## Quick Start

```groovy
// Create temperature sensor simulator
tempSensor = graph.addVertex(
    "type", "generator",
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
    "type", "generator",
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
    "type", "generator",
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
        "type", "generator",
        "label", "sensor-${i}",
        "Algorithm", "random",
        "Delay", "1000"
    )
}
```

## Documentation

Complete documentation in `docs/` directory:

- **[Quick Start](docs/QUICKSTART.md)**: Get started in 5 minutes
- **[Algorithms](docs/ALGORITHMS.md)**: Mathematical details of each algorithm
- **[Architecture](docs/ARCHITECTURE.md)**: Internal design and implementation
- **[API Reference](docs/API_REFERENCE.md)**: Complete API documentation
- **[Examples](docs/EXAMPLES.md)**: Real-world use cases

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
g.V().has("type", "generator")
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

Andrea Ambrosini - Rossonet s.c.a r.l.

## See Also

- [WaldOT Framework](../../README.md)
- [WaldOT Rules Engine](../waldot-plugin-rules-engine/README.md)
- [Apache TinkerPop](https://tinkerpop.apache.org/)

---

**WaldOT Data Generator Plugin** - Simulate dynamic data for testing and development.
