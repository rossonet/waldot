# WaldOT Developer Guide

**Hands-on development guide for software engineers**

## Quick Start

### Prerequisites

- Java 21+ (for virtual threads)
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

### Creating a Custom Plugin

**Step 1**: Create plugin class

```java
package com.example.myplugin;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.*;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WaldotPlugin
public class MyCustomPlugin implements AutoCloseable, PluginListener {
    private static final Logger logger = LoggerFactory.getLogger(MyCustomPlugin.class);
    private WaldotNamespace namespace;
    private UaObjectTypeNode myTypeNode;

    @Override
    public void initialize(WaldotNamespace waldotNamespace) {
        this.namespace = waldotNamespace;
        logger.info("MyCustomPlugin initialized");

        // Create OPC UA type node
        createTypeNodes();
    }

    private void createTypeNodes() {
        myTypeNode = UaObjectTypeNode.builder(namespace.getOpcUaNodeContext())
            .setNodeId(namespace.generateNodeId("ObjectTypes/MyCustomType"))
            .setBrowseName(namespace.generateQualifiedName("MyCustomType"))
            .setDisplayName(LocalizedText.english("My Custom Device"))
            .setIsAbstract(false)
            .build();

        // Add properties
        PluginListener.addParameterToTypeNode(
            namespace, myTypeNode, "value", NodeIds.Double
        );

        // Register in address space
        namespace.getStorageManager().addNode(myTypeNode);
        myTypeNode.addReference(new Reference(
            myTypeNode.getNodeId(),
            NodeIds.HasSubtype,
            NodeIds.BaseObjectType.expanded(),
            false
        ));

        // Register constructor
        namespace.getObjectTypeManager().registerObjectType(
            myTypeNode.getNodeId(),
            UaObjectNode.class,
            PluginListener.objectNodeConstructor
        );
    }

    @Override
    public boolean containsVertexType(String typeDefinitionLabel) {
        return "my:custom".equals(typeDefinitionLabel);
    }

    @Override
    public WaldotVertex createVertex(NodeId typeDefinitionNodeId, ...) {
        if (myTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
            return new MyCustomVertex(...);
        }
        return null;
    }

    @Override
    public void start() {
        logger.info("MyCustomPlugin started");
    }

    @Override
    public void stop() {
        logger.info("MyCustomPlugin stopped");
    }

    @Override
    public void close() throws Exception {
        logger.info("MyCustomPlugin closed");
    }
}
```

**Step 2**: Create custom vertex

```java
public class MyCustomVertex extends AbstractOpcVertex {
    private final ExecutorService executor;
    private volatile boolean active = true;

    public MyCustomVertex(...) {
        super(...);
        this.executor = ThreadHelper.newVirtualThreadExecutor();

        // Start background task in virtual thread
        executor.submit(this::backgroundTask);
    }

    private void backgroundTask() {
        while (active) {
            try {
                // Do work (blocking I/O is OK in virtual threads)
                double value = fetchDataFromExternalSource();

                // Update property (auto-syncs to OPC UA)
                property("value", value);

                // Sleep (doesn't block OS thread)
                Thread.sleep(5000);

            } catch (InterruptedException e) {
                active = false;
            } catch (Exception e) {
                logger.error("Error in background task", e);
            }
        }
    }

    @Override
    public void remove() {
        active = false;
        executor.shutdownNow();
        super.remove();
    }
}
```

**Step 3**: Add to build

```gradle
// settings.gradle
include 'plugins:my-custom-plugin'

// plugins/my-custom-plugin/build.gradle
plugins {
    id 'java'
}

dependencies {
    implementation project(':waldot-api')
    implementation 'org.apache.tinkerpop:gremlin-core:3.7.0'
    implementation 'org.eclipse.milo:sdk-server:0.6.13'
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

---

## Working with Generator Plugin

### Create Simulated Sensors

```groovy
// Temperature sensor (sinusoidal)
tempSensor = graph.addVertex(
    "type", "generator",
    "label", "office-temp",
    "Algorithm", "sinusoidal",
    "Min", "18",
    "Max", "26",
    "Delay", "5000"  // 5 seconds
)

// Pressure sensor (random)
pressureSensor = graph.addVertex(
    "type", "generator",
    "label", "tank-pressure",
    "Algorithm", "random",
    "Min", "0",
    "Max", "100",
    "Delay", "1000"  // 1 second
)

// Production counter (incremental)
counter = graph.addVertex(
    "type", "generator",
    "label", "items-produced",
    "Algorithm", "incremental",
    "Min", "0",
    "Max", "1000000",
    "Delay", "60000"  // 1 minute
)

// Read current value
currentTemp = tempSensor.property("data").value()
```

---

## Working with Rules Engine

### Create Rules

```groovy
// 1. Create compute vertex (execution manager)
compute = graph.addVertex(
    "type", "compute",
    "label", "main-compute",
    "Threads", "4"
)

// 2. Create rule
rule = graph.addVertex(
    "type", "rule",
    "label", "temp-alarm",
    "Condition", "temperature > 80.0",
    "Action", "log.warn('High temperature: ' + temperature + '°C')",
    "Priority", "100",
    "Hysteresis", "5000"  // 5 sec deduplication
)

// 3. Connect rule to compute
rule.addEdge("execute", compute, "Priority", "100")

// 4. Monitor sensor
tempSensor.addEdge("fire", rule,
    "monitor-property", "temperature",
    "active", "true"
)
```

### Complex Rules with Gremlin

```groovy
// Rule: Detect multiple sensor failures
rule = graph.addVertex(
    "type", "rule",
    "label", "multi-sensor-failure",
    "Condition", "g.V().has('type','sensor').has('status','ERROR').count().next() > 5",
    "Action", """
        count = g.V().has('type','sensor').has('status','ERROR').count().next();
        log.error('Multiple sensor failures: ' + count);
        graph.addVertex('type','alert','severity','CRITICAL','count',count);
    """
)
```

### JEXL Context Variables

Available in all rules:

| Variable   | Type              | Description         |
| ---------- | ----------------- | ------------------- |
| `log`      | Logger            | SLF4J logger        |
| `g`        | GraphTraversal    | Gremlin traversal   |
| `graph`    | Graph             | TinkerPop graph     |
| `commands` | CommandsFunction  | WaldOT commands     |
| `self`     | RuleVertex        | Current rule        |
| `Math`     | Math              | Java Math functions |
| `random`   | ThreadLocalRandom | Random numbers      |

---

## Bootstrap Configuration

### Script Mode (Recommended)

```groovy
// boot.conf - Full Groovy script

// Function to create monitoring zone
def createZone(name, tempMin, tempMax) {
  // Sensor
  sensor = g.addV('generator')
    .property('type', 'generator')
    .property('label', "temp-${name}")
    .property('Algorithm', 'random')
    .property('Min', tempMin.toString())
    .property('Max', tempMax.toString())
    .next()

  // Rule
  rule = g.addV('rule')
    .property('type', 'rule')
    .property('Condition', "temperature > ${tempMax - 2}")
    .property('Action', "log.warn('High temp in ${name}')")
    .next()

  // Compute
  compute = g.addV('compute')
    .property('type', 'compute')
    .property('Threads', '2')
    .next()

  // Connect
  rule.addEdge('execute', compute)
  sensor.addEdge('fire', rule, 'monitor-property', 'temperature')

  return [sensor, rule, compute]
}

// Create zones
['office', 'warehouse', 'production'].each { name ->
  createZone(name, 18, 26)
}

log.info("Configuration complete")
```

### Load from Remote URL

```bash
docker run \
  -e WALDOT_BOOT_URL=https://raw.githubusercontent.com/org/repo/main/config.groovy \
  rossonet/waldot:latest
```

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
  -e JAVA_OPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG" \
  rossonet/waldot:latest

# Java
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG \
     -jar waldot-app.jar
```

### Debug Rules

```groovy
// Enable debug events
rule.property("Debug", 2)  // 0=off, 1=events, 2=all

// Check metrics
queueSize = rule.property("Queue").value()
totalEvents = rule.property("Total").value()
executed = rule.property("Executed").value()
errors = rule.property("Errors").value()
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

- [Plugin Development Manual](manuale_plugins.md) - Complete guide (Italian)
- [API JavaDoc](https://javadoc.io/doc/net.rossonet.waldot/waldot-api/latest/index.html)

---

## Examples

See [Examples Directory](../../examples/) for complete Docker Compose examples:

1. [Industrial Monitoring](../../examples/01-industrial-monitoring/)
2. [Production Simulation](../../examples/02-production-simulation/)
3. [Energy Monitoring](../../examples/03-energy-monitoring/)
4. [Quality Control](../../examples/04-quality-control/)
5. [Predictive Maintenance](../../examples/05-predictive-maintenance/)

---

## Community

- **GitHub**: https://github.com/rossonet/waldot
- **Issues**: https://github.com/rossonet/waldot/issues
- **Discussions**: https://github.com/rossonet/waldot/discussions

---

**Happy coding!** 🚀
