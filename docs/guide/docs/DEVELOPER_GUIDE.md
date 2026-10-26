# WaldOT Developer Guide

**Hands-on development guide for software engineers**

## Quick Start

### Prerequisites

- JDK 26 (the build targets Java 26; virtual threads are used throughout)
- Docker (for containerized deployment)
- Git
- IDE with Java support (IntelliJ IDEA, Eclipse, VS Code)

### 5-Minute Setup

```bash
# Clone repository
git clone https://github.com/rossonet/waldot.git
cd waldot

# Build project
./gradlew clean build

# Run WaldOT
java -jar waldot-app/build/libs/waldot-app-*.jar

# Access OPC UA server
# opc.tcp://localhost:12686/waldot
```

### Docker Quick Start

```bash
# Pull and run
docker pull rossonet/waldot:latest
docker run -p 12686:12686 -p 8443:8443 rossonet/waldot:latest

# Access
# OPC UA: opc.tcp://localhost:12686/waldot
# HTTPS: https://localhost:8443
```

---

## Core Concepts

### 1. WaldotGraph

The main interface combining TinkerPop Graph with OPC UA integration.

```java
// Open graph
WaldotGraph graph = OpcFactory.getOpcGraph(
    "file:///tmp/waldot.db",
    new LoggerHistoryStrategy()
);

// Add vertex
Vertex v = graph.addVertex();
v.property("label", "sensor1");
v.property("temperature", 25.5);

// Query
List<Vertex> sensors = graph.traversal()
    .V()
    .has("label", "sensor1")
    .toList();

// Close
graph.close();
```

### 2. WaldotVertex

Represents both a TinkerPop Vertex and an OPC UA Object.

```java
WaldotVertex sensor = (WaldotVertex) graph.addVertex();
sensor.property("label", "temp-sensor");
sensor.property("value", 20.0);

// Add edge
WaldotVertex zone = (WaldotVertex) graph.addVertex();
zone.property("label", "production-zone");
sensor.addEdge("located-in", zone);

// Observe property changes
sensor.addPropertyObserver((vertex, key, oldValue, newValue) -> {
    System.out.println(key + " changed: " + oldValue + " → " + newValue);
});
```

### 3. Gremlin Queries

```java
GraphTraversalSource g = graph.traversal();

// Find all sensors
List<Vertex> sensors = g.V()
    .has("type", "sensor")
    .toList();

// Find sensors in alarm
List<String> alarms = g.V()
    .has("type", "sensor")
    .has("status", "alarm")
    .values("label")
    .toList();

// Complex traversal
List<Path> paths = g.V()
    .has("label", "Line1")
    .out("contains")
    .has("type", "motor")
    .has("status", "alarm")
    .path()
    .by("label")
    .toList();
```

---

## Plugin Development

Plugins are discovered at startup: a top-level class annotated with
`@WaldotPlugin`, with a public no-argument constructor, located in a package
under `net.rossonet.waldot`, `plugin` or `plugins`, usually extending
`AbstractPluginListener`. It registers vertex types
(`registerTypeNode(...)`), edge types (`registerTypeEdge(...)`) and commands
(`registerCommand(...)`) in `initialize(WaldotNamespace)`.

The complete, verified walkthrough (project setup, vertex hooks, custom edges,
commands, packaging in the Docker image, tests) is in the
**[Integration Guide](INTEGRATION_GUIDE.md)**.

---

## Working with the base plugins

Vertices of the base plugins are created with `graph.addVertex` in a bootstrap
script, in the `query` command or in Java code. Keys are lowercase; scripts are
[JEXL](https://commons.apache.org/proper/commons-jexl/) and statements end
with `;`.

### Generator

```javascript
// sinusoidal temperature, new value in 'data' every 5 s
var temp = graph.addVertex('id', 'office-temp', 'type', 'sim:generator', 'label', 'office-temp',
    'algorithm', 'sinusoidal', 'min', '18', 'max', '26', 'delay', '5000');
// production counter, +1 every minute
var counter = graph.addVertex('id', 'items-produced', 'type', 'sim:generator', 'label', 'items-produced',
    'algorithm', 'incremental', 'min', '0', 'max', '1000000', 'delay', '60000');
log.info('temperature: ' + temp.value('data'));
```

### Rules engine

```javascript
var compute = graph.addVertex('id', 'main-compute', 'type', 'rules:compute', 'label', 'main-compute', 'threads', '4');
var rule = graph.addVertex('id', 'temp-alarm', 'type', 'rules:rule', 'label', 'temp-alarm',
    'condition', "g.V().has('id','office-temp').values('data').next() > 25.0",
    'action', "log.warn('High temperature')",
    'hysteresis', '5000');
compute.addEdge('runner', rule, 'type', 'rules:execute', 'priority', '100');
temp.addEdge('watch', rule, 'type', 'fire', 'properties-to-monitor', 'data');
```

The same code in Java:

```java
Vertex compute = graph.addVertex("id", "main-compute", "type", "rules:compute", "label", "main-compute", "threads", "4");
Vertex rule = graph.addVertex("id", "temp-alarm", "type", "rules:rule", "label", "temp-alarm",
        "condition", "g.V().has('id','office-temp').values('data').next() > 25.0",
        "action", "log.warn('High temperature')", "hysteresis", "5000");
compute.addEdge("runner", rule, "type", "rules:execute", "priority", "100");
temp.addEdge("watch", rule, "type", "fire", "properties-to-monitor", "data");
```

Variables available in `condition` and `action`:

| Variable | Description |
| -------- | ----------- |
| `log` | logger |
| `g` | Gremlin traversal source |
| `graph` | the WaldOT graph |
| `cmd` | console helper (`getVerticesCount()`, `toNumber(x)`, ...) |
| `self` | the rule vertex |
| `math` | `java.lang.Math` |
| `rand` | `java.util.Random` |
| commands | every console command, as `<folder>_<name>` |

Properties, weights, counters and debug:
[rules engine README](https://github.com/rossonet/waldot/tree/master/plugins/waldot-plugin-rules-engine)
and [User Guide](USER_GUIDE.md#6-rules-engine).

### Bootstrap

Syntax, format detection and troubleshooting:
[Bootstrap Configuration](https://github.com/rossonet/waldot/blob/master/waldot-namespace/BOOTSTRAP_CONFIGURATION.md).
Complete verified scripts: [Examples](examples/README.md).

---

## Testing

### Unit Testing Plugins

```java
@Test
public void testCustomVertex() {
    // Create test graph
    WaldotGraph graph = OpcFactory.getOpcGraph(
        "file:///tmp/test.db",
        new LoggerHistoryStrategy()
    );

    // Create vertex
    Vertex v = graph.addVertex("type", "my:custom");
    v.property("value", 42.0);

    // Assert
    assertEquals(42.0, v.property("value").value());

    // Cleanup
    graph.close();
}
```

### Regression tests

Fixed bugs are covered by tests that go through the same path as a user:

| Test | Module | Covers |
| ---- | ------ | ------ |
| `regression.AnonymousAccessRegressionTest` | `waldot-namespace` | `--anonymous-access false`: no anonymous token policy on the endpoints, anonymous sessions rejected by a real Milo client, accepted by default |
| `regression.EdgeCommandsRegressionTest` | `waldot-namespace` | `create link/fire monitored edge` called over OPC UA with NodeId arguments, edge direction, value propagation, `error:` results |
| `regression.DeleteDirectoryAndResetRegressionTest` | `waldot-namespace` | `delete directory` removes vertices and edges of the folder tree; `cmd.reset()` empties the graph before the bootstrap (both Milo strategies) |
| `WaldotRulesEngineEdgeCommandTest` | rules engine | `create compute monitored edge` with NodeId arguments |

`RawOpcUaClient` (test package `net.rossonet.waldot.regression`) connects a
plain Milo client to the test server with a chosen identity: use it for new
tests that must reproduce what an external OPC UA client does.

```bash
./gradlew :waldot-namespace:test --tests 'net.rossonet.waldot.regression.*'
./gradlew :waldot-plugin-rules-engine:test --tests '*EdgeCommandTest'
```

### Integration Testing with Testcontainers

```java
@Test
public void testWaldotContainer() {
    try (GenericContainer<?> waldot = new GenericContainer<>("rossonet/waldot:latest")
            .withExposedPorts(12686)) {

        waldot.start();

        // Connect OPC UA client
        String endpoint = "opc.tcp://" + waldot.getHost() + ":" +
                         waldot.getMappedPort(12686) + "/waldot";

        // Test connection
        // ...
    }
}
```

---

## Best Practices

### 1. Use Virtual Threads for I/O

```java
// ✅ GOOD: Virtual thread for blocking I/O
ExecutorService executor = ThreadHelper.newVirtualThreadExecutor();
executor.submit(() -> {
    while (active) {
        String data = httpClient.get(url).block();  // Blocking OK
        processData(data);
        Thread.sleep(1000);  // Blocking OK
    }
});

// ❌ BAD: Platform thread pool
ExecutorService executor = Executors.newFixedThreadPool(10);  // Limited scalability
```

### 2. Handle Interruptions

```java
// ✅ GOOD: Proper interruption handling
while (active) {
    try {
        doWork();
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        active = false;  // Exit gracefully
        Thread.currentThread().interrupt();  // Restore interrupt status
    }
}

// ❌ BAD: Ignoring interruptions
while (true) {
    doWork();
    Thread.sleep(1000);  // Exception ignored
}
```

### 3. Clean Up Resources

```java
// ✅ GOOD: Proper cleanup
@Override
public void close() throws Exception {
    active = false;
    executor.shutdownNow();
    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        logger.warn("Executor did not terminate");
    }
}
```

### 4. Use Property Observers

```java
// ✅ GOOD: React to property changes
vertex.addPropertyObserver((v, key, oldValue, newValue) -> {
    if ("temperature".equals(key) && (Double)newValue > 80.0) {
        logger.warn("High temperature detected");
    }
});
```

---

## Debugging

### Enable Debug Logging

```bash
# Docker
docker run \
  -e JDK_JAVA_OPTIONS="-Dorg.slf4j.simpleLogger.defaultLogLevel=debug" \
  rossonet/waldot:latest

# Java (fat jar built with ./gradlew :waldot-app:shadowJar)
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
     -jar waldot-app/build/libs/waldot-app-<version>-all.jar
```

### Debug Rules

```javascript
// enable debug: 0 = off, 1 = OPC UA events, 2 = events and log
g.V().has('id', 'temp-alarm').next().property('debug', 2);

// counters
var rule = g.V().has('id', 'temp-alarm').next();
log.info('queue=' + rule.value('queue') + ' total=' + rule.value('total')
    + ' executed=' + rule.value('executed') + ' errors=' + rule.value('errors'));
```

### Connect with OPC UA Client

- **UaExpert**: https://www.unified-automation.com/products/development-tools/uaexpert.html
- **Prosys Browser**: https://www.prosysopc.com/products/opc-ua-browser/

---

## API Reference

### Key Interfaces

- `WaldotGraph` - Main graph interface
- `WaldotVertex` - Vertex interface
- `WaldotEdge` - Edge interface
- `WaldotNamespace` - Namespace management
- `PluginListener` - Plugin extension point

### Key Classes

- `AbstractOpcVertex` - Base class for custom vertices
- `AbstractOpcEdge` - Base class for custom edges
- `ThreadHelper` - Virtual thread utilities
- `OpcFactory` - Graph factory

### Complete Documentation

- [Integration Guide](INTEGRATION_GUIDE.md) - How to write a plugin
- [API JavaDoc](https://javadoc.io/doc/net.rossonet.waldot/waldot-api/latest/index.html)

---

## Examples

- [Docker Compose examples](https://github.com/rossonet/waldot/tree/master/waldot-app/examples) - server deployment variants
- [Rules engine showcase: gas grid](https://github.com/rossonet/waldot/tree/master/plugins/waldot-plugin-rules-engine/docs/SHOWCASE_GAS_GRID.md)
- [Generator quick start](https://github.com/rossonet/waldot/tree/master/plugins/waldot-plugin-generator/docs/QUICKSTART.md)
- [Configuration Reference](CONFIGURATION.md)

---

## Community

- **GitHub**: https://github.com/rossonet/waldot
- **Issues**: https://github.com/rossonet/waldot/issues
- **Discussions**: https://github.com/rossonet/waldot/discussions

---

**Happy coding!** 🚀
