# WaldOT - Reference Documentation

## Table of Contents

1. [Project Overview](#project-overview)
2. [General Architecture](#general-architecture)
3. [OPC UA + TinkerPop](#opc-ua--tinkerpop)
4. [Plugin System](#plugin-system)
5. [Virtual Threads](#virtual-threads)
6. [Available Plugins](#available-plugins)
7. [Use Cases](#use-cases)

---

## Project Overview

### What is WaldOT?

**WaldOT** (Waldorf OT - Operational Technology) is an open-source Digital Twin engine that innovatively integrates industrial automation (OT) with modern data analysis technologies through graph databases.

The project creates a bidirectional bridge between:
- **OPC UA** (industrial standard for OT communication)
- **Apache TinkerPop** (graph database framework)

### Project Vision

WaldOT revolutionizes how industrial data is represented and analyzed, transforming hierarchical OPC UA address spaces into **living graphs** queryable in real-time with the Gremlin language.

### Why WaldOT?

#### Problem
Traditional industrial systems expose data through OPC UA - a powerful but hierarchical protocol. This makes it difficult to:
- Find correlations between devices distant in the hierarchical tree
- Execute complex queries on multiple relationships
- Implement business logic based on graph patterns
- Integrate OT data with modern IT systems

#### Solution
WaldOT represents the entire OPC UA address space as a **graph database**:

```
OPC UA                          TinkerPop
─────────────────────          ───────────────────
Objects         →              Vertices
References      →              Edges
Variables       →              Properties
```

Changes propagate **in real-time in both directions**:
- Changes via OPC UA → update the graph
- Gremlin queries that modify the graph → update OPC UA

---

## General Architecture

### Architectural Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        WaldOT Framework                          │
│                                                                  │
│  ┌────────────────┐              ┌─────────────────────┐       │
│  │   OPC UA       │              │   TinkerPop         │       │
│  │   Server       │  ←────────→  │   Graph             │       │
│  │  (Eclipse Milo)│   Bi-Sync    │  (Apache TinkerPop) │       │
│  └────────┬───────┘              └──────────┬──────────┘       │
│           │                                  │                   │
│           └──────────────┬───────────────────┘                  │
│                          ↓                                       │
│           ┌──────────────────────────────┐                     │
│           │      Plugin Manager          │                     │
│           │  - Auto-discovery            │                     │
│           │  - Lifecycle management      │                     │
│           │  - Type registration         │                     │
│           └──────────────┬───────────────┘                     │
│                          ↓                                       │
│   ┌──────────────────────────────────────────────────┐         │
│   │                    Plugins                        │         │
│   │  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │         │
│   │  │Generator │  │  Rules   │  │  TinkerPop   │   │         │
│   │  │          │  │  Engine  │  │   Server     │   │         │
│   │  └──────────┘  └──────────┘  └──────────────┘   │         │
│   │         ... other custom plugins ...             │         │
│   └──────────────────────────────────────────────────┘         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
         ↓                                    ↓
┌─────────────────┐                  ┌─────────────────┐
│  OPC UA Clients │                  │ Gremlin Clients │
│  - UaExpert     │                  │  - Console      │
│  - Prosys       │                  │  - Drivers      │
│  - Custom       │                  │  - Graph-Explorer│
└─────────────────┘                  └─────────────────┘
```

### Main Components

#### 1. WaldOT Framework Core

**waldot-api**: Public APIs for plugin developers
- `PluginListener` interface
- `@WaldotPlugin` annotation
- Models: `WaldotGraph`, `WaldotVertex`, `WaldotEdge`, `WaldotNamespace`

**waldot-namespace**: OPC UA namespace implementation
- OPC UA node management
- Bidirectional OPC UA ↔ Graph synchronization
- Event and property handling

**waldot-app**: Main application
- Server bootstrap
- Plugin loading
- Configuration

#### 2. OPC UA Server (Eclipse Milo)

Complete OPC UA server that:
- Exposes the address space according to OPC UA standard
- Manages client connections
- Publishes events
- Supports subscriptions

#### 3. TinkerPop Graph

Graph database that:
- Stores vertices, edges, and properties
- Supports Gremlin queries
- Can use different storage backends (in-memory, RocksDB, Neo4j, etc.)

#### 4. Plugin Manager

Plugin management system that:
- Scans classpath for `@WaldotPlugin` annotations
- Initializes plugins in order
- Manages lifecycle (initialize → start → stop → close)
- Registers custom types in OPC UA address space

---

## OPC UA + TinkerPop

### Conceptual Mapping

#### OPC UA Objects → TinkerPop Vertices

An OPC UA object becomes a vertex in the graph:

**OPC UA**:
```
ObjectNode {
  NodeId: "ns=2;s=Motor1"
  BrowseName: "Motor1"
  DisplayName: "Production Motor 1"
}
```

**TinkerPop**:
```groovy
Vertex {
  id: "ns=2;s=Motor1"
  label: "motor"
  properties: {
    "name": "Production Motor 1",
    "rpm": 1500,
    "temperature": 45.5
  }
}
```

#### OPC UA References → TinkerPop Edges

OPC UA references become edges in the graph:

**OPC UA**:
```
ProductionLine --[Contains]--> Motor1
Motor1 --[HasComponent]--> TemperatureSensor
```

**TinkerPop**:
```groovy
productionLine --[contains]--> motor1
motor1 --[hasComponent]--> temperatureSensor
```

#### OPC UA Variables → Vertex Properties

OPC UA variables become vertex properties:

**OPC UA**:
```
Motor1/RPM = 1500 (UInt16)
Motor1/Temperature = 45.5 (Double)
Motor1/Status = "RUNNING" (String)
```

**TinkerPop**:
```groovy
motor1.property("rpm", 1500)
motor1.property("temperature", 45.5)
motor1.property("status", "RUNNING")
```

### Bidirectional Synchronization

#### From OPC UA to Graph

When an OPC UA client writes a variable:

```
1. OPC UA Client: Write Motor1/Temperature = 50.0
2. WaldOT detects the change
3. Updates vertex property: motor1.property("temperature", 50.0)
4. Propagates to graph listeners
```

#### From Graph to OPC UA

When a Gremlin query modifies the graph:

```groovy
1. Gremlin: g.V().has('id', 'Motor1').property('temperature', 55.0)
2. WaldOT detects the change
3. Updates OPC UA variable: Motor1/Temperature = 55.0
4. Notifies subscribed OPC UA clients
```

### Benefits of Dual View

#### For OT Engineers
- Use familiar OPC UA tools (UaExpert, Prosys)
- Configure devices via standard OPC UA
- Monitor in real-time with OPC UA subscriptions

#### For Data Scientists / IT Developers
- Complex queries with Gremlin
- Correlation analysis with graph algorithms
- Integration with modern frameworks (Spring, GraphQL, REST)

---

## Plugin System

### Plugin Philosophy

WaldOT is designed as an **extensible framework** where plugins are first-class citizens. Each plugin can:

1. **Register new vertex types** in the OPC UA address space
2. **Implement custom behaviors** for vertices
3. **Integrate external data sources** (sensors, APIs, databases)
4. **Provide commands** executable via console or OPC UA

### Anatomy of a Plugin

A WaldOT plugin consists of:

```
my-plugin/
├── src/main/java/
│   └── net/rossonet/waldot/myplugin/
│       ├── MyPlugin.java              # Main class @WaldotPlugin
│       ├── MyCustomVertex.java        # Custom vertex implementation
│       └── MyCustomEdge.java          # (optional) Custom edge
├── src/main/resources/
│   └── META-INF/
│       └── services/                   # Service loader (optional)
└── build.gradle                        # Plugin dependencies
```

#### Main Plugin Class

```java
@WaldotPlugin  // Annotation for auto-discovery
public class MyPlugin implements AutoCloseable, PluginListener {
    
    private WaldotNamespace waldotNamespace;
    private UaObjectTypeNode myTypeNode;
    
    @Override
    public void initialize(WaldotNamespace waldotNamespace) {
        // 1. Store namespace reference
        this.waldotNamespace = waldotNamespace;
        
        // 2. Create OPC UA type nodes
        createTypeNodes();
        
        // 3. Register commands (optional)
        registerCommands();
    }
    
    @Override
    public WaldotVertex createVertex(...) {
        // Factory method to create custom vertices
        if (myTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
            return new MyCustomVertex(...);
        }
        return null;
    }
    
    @Override
    public void start() {
        // Start background tasks, monitoring, etc.
    }
    
    @Override
    public void stop() {
        // Stop background tasks
    }
    
    @Override
    public void close() throws Exception {
        // Clean up resources
    }
}
```

### Registering OPC UA Types

Plugins register new object types in the OPC UA address space:

```java
private void createTypeNodes() {
    // Create ObjectType node
    myTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
        .setNodeId(waldotNamespace.generateNodeId("ObjectTypes/MyCustomType"))
        .setBrowseName(waldotNamespace.generateQualifiedName("MyCustomType"))
        .setDisplayName(LocalizedText.english("My Custom Device"))
        .setIsAbstract(false)
        .build();
    
    // Add properties to the type
    PluginListener.addParameterToTypeNode(
        waldotNamespace, 
        myTypeNode, 
        "value",        // Property name
        NodeIds.Double  // OPC UA type
    );
    
    // Register in address space
    waldotNamespace.getStorageManager().addNode(myTypeNode);
    myTypeNode.addReference(new Reference(
        myTypeNode.getNodeId(),
        NodeIds.HasSubtype,
        NodeIds.BaseObjectType.expanded(),
        false
    ));
    
    // Register constructor
    waldotNamespace.getObjectTypeManager().registerObjectType(
        myTypeNode.getNodeId(),
        UaObjectNode.class,
        PluginListener.objectNodeConstructor
    );
}
```

### Plugin Lifecycle

```
┌──────────────────────────────────────────────────────────┐
│ 1. DISCOVERY                                              │
│    WaldOT scans classpath for @WaldotPlugin             │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│ 2. INITIALIZATION                                         │
│    plugin.initialize(waldotNamespace)                    │
│    - Creates OPC UA type nodes                            │
│    - Registers commands                                   │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│ 3. START                                                  │
│    plugin.start()                                         │
│    - Starts background threads                            │
│    - Begins monitoring                                    │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│ 4. RUNTIME                                                │
│    - Creates vertices on demand                           │
│    - Handles events                                       │
│    - Executes commands                                    │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│ 5. STOP                                                   │
│    plugin.stop()                                          │
│    - Stops background threads                             │
│    - Pauses operations                                    │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│ 6. CLOSE                                                  │
│    plugin.close()                                         │
│    - Releases resources                                   │
│    - Closes connections                                   │
└──────────────────────────────────────────────────────────┘
```

---

## Virtual Threads

### What is a Virtual Thread?

**Virtual threads** (introduced in Java 21) are lightweight threads managed by the JVM rather than the operating system.

#### Comparison: Platform Thread vs Virtual Thread

| Feature | Platform Thread | Virtual Thread |
|---|---|---|
| **Stack size** | ~1 MB | ~1 KB |
| **Maximum count** | Thousands | Millions |
| **Creation** | Expensive (~1ms) | Cheap (~1μs) |
| **Blocking I/O** | Blocks OS thread | Doesn't block OS thread |
| **Scheduling** | Operating system | JVM |

### Why Virtual Threads in WaldOT?

WaldOT creates **active objects** that operate continuously:
- Generators simulating sensors
- Monitors polling external APIs
- Rule processors reacting to events

**Without virtual threads**:
```java
// 10,000 sensors = 10,000 platform threads = ~10 GB RAM ❌
for (int i = 0; i < 10000; i++) {
    new Thread(() -> monitorSensor(i)).start();  // BAD!
}
```

**With virtual threads**:
```java
// 10,000 sensors = 10,000 virtual threads = ~10 MB RAM ✅
ExecutorService executor = ThreadHelper.newVirtualThreadExecutor();
for (int i = 0; i < 10000; i++) {
    executor.submit(() -> monitorSensor(i));  // GOOD!
}
```

### Usage Patterns

#### Pattern 1: Polling Loop

```java
public class SensorVertex extends WaldotVertex {
    private final ExecutorService executor;
    private volatile boolean active = true;
    
    public SensorVertex(ExecutorService executor, ...) {
        super(...);
        this.executor = executor;
        
        // Start polling in virtual thread
        executor.submit(this::pollingSensorLoop);
    }
    
    private void pollingSensorLoop() {
        while (active) {
            try {
                // Blocking operation - OK in virtual thread
                SensorData data = httpClient.get(sensorUrl).block();
                
                // Update properties (auto-sync to OPC UA)
                property("temperature", data.getTemperature());
                property("humidity", data.getHumidity());
                
                // Sleep - OK in virtual thread (doesn't block OS thread)
                Thread.sleep(5000);
                
            } catch (InterruptedException e) {
                active = false;
            } catch (Exception e) {
                logger.error("Sensor polling error", e);
            }
        }
    }
}
```

#### Pattern 2: Event-Driven

```java
public class MqttBridgeVertex extends WaldotVertex {
    private final MqttClient mqttClient;
    
    public MqttBridgeVertex(...) {
        super(...);
        
        // MQTT connection (blocking I/O - OK in virtual thread)
        mqttClient = new MqttClient(broker, clientId);
        mqttClient.connect();
        
        // Callback executed in virtual thread automatically
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // Update graph
                JSONObject json = new JSONObject(new String(message.getPayload()));
                json.keys().forEachRemaining(key -> {
                    property(key, json.get(key));
                });
            }
        });
    }
}
```

#### Pattern 3: Continuous Generator

```java
public class DataGeneratorVertex extends WaldotVertex {
    private volatile boolean active = true;
    
    public DataGeneratorVertex(ExecutorService executor, ...) {
        super(...);
        executor.submit(this::generateLoop);
    }
    
    private void generateLoop() {
        double value = 0.0;
        while (active) {
            try {
                // Generate value
                value = Math.sin(System.currentTimeMillis() / 1000.0) * 50 + 50;
                
                // Update (sync to OPC UA)
                property("data", value);
                
                // Pause
                Thread.sleep(delayMs);
                
            } catch (InterruptedException e) {
                active = false;
            }
        }
    }
}
```

### Best Practices

#### ✅ DO

1. **Use virtual threads for blocking I/O**
```java
executor.submit(() -> {
    String data = httpClient.get(url).block();  // OK!
    processData(data);
});
```

2. **Create as many virtual threads as needed**
```java
// Perfectly fine to create 100,000 virtual threads
for (int i = 0; i < 100000; i++) {
    executor.submit(() -> doWork());
}
```

3. **Use sleep() freely**
```java
while (active) {
    doWork();
    Thread.sleep(1000);  // OK in virtual thread!
}
```

#### ❌ DON'T

1. **DON'T use fixed-size pools for virtual threads**
```java
// WRONG!
ExecutorService executor = Executors.newFixedThreadPool(10);
```

2. **DON'T use virtual threads for CPU-intensive work**
```java
// WRONG - use platform threads for CPU-intensive work
executor.submit(() -> {
    complexMathCalculation();  // Occupies CPU for long time
});
```

3. **DON'T forget to handle interruptions**
```java
// WRONG
while (true) {
    doWork();
    Thread.sleep(1000);  // If interrupted, exception ignored
}

// RIGHT
while (active) {
    try {
        doWork();
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        active = false;  // Exit cleanly
    }
}
```

---

## Available Plugins

### 1. waldot-plugin-generator

**Purpose**: Simulate dynamic data for testing and development

#### Features
- 6 generation algorithms: incremental, decremental, random, sinusoidal, triangular, stopped
- Configurable update intervals (10ms - hours)
- Thousands of concurrent generators with virtual threads
- Full OPC UA synchronization

#### Usage Example

```groovy
// Temperature sensor simulator
tempSensor = graph.addVertex(
    "type", "generator",
    "label", "office-temperature",
    "Algorithm", "sinusoidal",
    "Min", "18",
    "Max", "26",
    "Delay", "5000"  // Update every 5 seconds
)

// Read generated value
temperature = tempSensor.property("data").value()

// Change algorithm at runtime
tempSensor.property("Algorithm", "random")
```

#### Algorithms

| Algorithm | Pattern | Use Case |
|---|---|---|
| `incremental` | Linear increasing | Counters, timers |
| `decremental` | Linear decreasing | Countdown |
| `random` | Random values | Noisy sensors |
| `sinusoidal` | Sine wave | Temperature cycles, AC |
| `triangular` | Triangle wave | Sawtooth, PWM |
| `stopped` | Constant | Pause simulation |

#### Architecture

```
DataGeneratorVertex
├── Virtual Thread (infinite loop)
│   └── while (active) {
│         generateValue(algorithm);
│         property("data", value);  // Sync to OPC UA
│         sleep(delay);
│       }
└── Properties
    ├── Algorithm: generation algorithm
    ├── Delay: update interval (ms)
    ├── Min: minimum value
    ├── Max: maximum value
    └── data: generated value (read-only)
```

### 2. waldot-plugin-rules-engine

**Purpose**: Event-driven IF-THEN-THAT rule execution engine

#### Features
- JEXL expressions for conditions and actions
- Priority queue with hysteresis deduplication
- Virtual thread pool for concurrent execution
- Full Gremlin graph access in rules
- OPC UA debug events for troubleshooting

#### Architecture

```
[Source Node] → [FireMonitoredEdge] → [RuleVertex] → [ComputeMonitoredEdge] → [ComputeVertex]
   (event)            (filters)          (enqueues)         (routes)            (executes)
```

**Components**:
- **RuleVertex**: IF-THEN rule with JEXL expressions
- **ComputeVertex**: Thread manager with priority execution
- **ComputeMonitoredEdge**: Connects rules to compute for execution
- **FireMonitoredEdge**: Monitors sources and fires rules

#### Usage Example

```groovy
// 1. Create compute vertex
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
    "Action", "log.warn('Temperature alarm: ' + temperature + '°C')",
    "Priority", "100",
    "Hysteresis", "5000"  // 5 second deduplication
)

// 3. Connect rule to compute
rule.addEdge("execute", compute, "Priority", "100")

// 4. Monitor sensor
tempSensor.addEdge("fire", rule, 
    "monitor-property", "temperature",
    "active", "true"
)

// 5. Test
tempSensor.property("temperature", 85.0)
// Output: WARN Temperature alarm: 85.0°C
```

#### JEXL Variables Available

| Variable | Type | Description |
|---|---|---|
| `log` | Logger | SLF4J logger for logging |
| `g` | GraphTraversal | Gremlin traversal for queries |
| `graph` | Graph | TinkerPop graph instance |
| `commands` | CommandsFunction | WaldOT console commands |
| `self` | RuleVertex | Reference to this rule |
| `Math` | Math | Java Math functions |
| `random` | ThreadLocalRandom | Random number generator |

#### Priority Mechanism

```
weight = edge_priority × priority_factor + queue_size
```

**Example**:
- Rule A: priority=100, queue=5, factor=100.0 → weight=10,005
- Rule B: priority=50, queue=10, factor=100.0 → weight=5,010

Rule A is processed first (higher weight).

### 3. waldot-plugin-tinkerpop

**Purpose**: Embedded Gremlin Server for remote client access

#### Features
- Embedded Gremlin Server as graph vertex
- Protocols: WebSocket + HTTP
- Serializers: GraphSON v3 + GraphBinary v1
- Compatibility with all standard TinkerPop clients
- Live bidirectional synchronization

#### Usage Example

```groovy
// Create Gremlin Server
gremlinServer = graph.addVertex(
    "type", "gremlin",
    "label", "main-server",
    "Port", "8182",
    "Bind", "0.0.0.0"
)

// Connect from Gremlin Console
:remote connect tinkerpop.server conf/remote.yaml
:remote console
g.V().count()

// Query from Java
Cluster cluster = Cluster.build()
    .addContactPoint("localhost")
    .port(8182)
    .create();
GraphTraversalSource g = traversal()
    .withRemote(DriverRemoteConnection.using(cluster, "g"));
long count = g.V().count().next();
```

#### Architecture

```
┌────────────────────────────────────┐
│     WaldOT Graph (shared)         │
└────────┬───────────────────────────┘
         │
    ┌────┴─────┐
    │          │
┌───▼──┐   ┌──▼────┐
│OPC UA│   │Gremlin│
│Server│   │Server │
└───┬──┘   └───┬───┘
    │          │
┌───▼──┐   ┌──▼────┐
│OPC UA│   │Gremlin│
│Client│   │Client │
└──────┘   └───────┘
```

---

## Use Cases

### 1. Predictive Maintenance

**Scenario**: Monitor industrial equipment and predict failures

```groovy
// Equipment with sensors
equipment = graph.addVertex(
    "type", "equipment",
    "label", "pump-1",
    "vibration", 2.5,
    "temperature", 45.0,
    "pressure", 75.0,
    "runningHours", 1000
)

// Rule: vibration anomaly
vibrationRule = graph.addVertex(
    "type", "rule",
    "Condition", "vibration > 5.0",
    "Action", "log.warn('Abnormal vibration: possible bearing failure'); g.V(self).property('maintenanceRequired', true).iterate()"
)

// Rule: temperature + pressure combination
criticalRule = graph.addVertex(
    "type", "rule",
    "Condition", "temperature > 80.0 && pressure > 100.0",
    "Action", "log.error('CRITICAL: high temperature AND pressure. Shutdown required.'); commands.execute('emergency-shutdown', 'pump-1')"
)

// Rule: predictive maintenance
predictiveRule = graph.addVertex(
    "type", "rule",
    "Condition", "runningHours > 5000 && (vibration > 4.0 || temperature > 70.0)",
    "Action", "log.info('Predictive maintenance recommended for pump-1'); g.V(self).property('scheduleMaintenance', true).iterate()"
)
```

### 2. Smart Building

**Scenario**: HVAC and lighting automation based on occupancy

```groovy
// Building zone
zone = graph.addVertex(
    "type", "zone",
    "label", "office-1",
    "occupancy", 0,
    "temperature", 22.0,
    "lightLevel", 0,
    "hvacMode", "AUTO"
)

// Rule: turn on lights when occupied
lightingRule = graph.addVertex(
    "type", "rule",
    "Condition", "occupancy > 0 && lightLevel < 300",
    "Action", "var zoneName = self.property('source').value(); log.info('Turning on lights in ' + zoneName); g.V(self).property('lightLevel', 800).iterate()"
)

// Rule: turn off lights when empty
lightsOffRule = graph.addVertex(
    "type", "rule",
    "Condition", "occupancy == 0 && lightLevel > 0",
    "Action", "g.V(self).property('lightLevel', 0).iterate()",
    "Hysteresis", "60000"  // Wait 1 minute before turning off
)

// Rule: cooling when needed
hvacCoolingRule = graph.addVertex(
    "type", "rule",
    "Condition", "occupancy > 0 && temperature > 24.0 && hvacMode == 'AUTO'",
    "Action", "g.V(self).property('hvacMode', 'COOLING').iterate()"
)
```

### 3. Energy Analysis

**Scenario**: Aggregate and analyze consumption per production line

```groovy
// Gremlin queries for analysis
// Total consumption per production line
g.V().has('type', 'production_line')
  .group()
    .by('name')
    .by(out('contains').values('energy_kwh').sum())

// Find inefficient equipment
g.V().has('type', 'equipment')
  .has('energy_kwh', gt(1000))
  .has('production_output', lt(100))
  .values('name')

// Consumption trend in last 24 hours
g.V().has('type', 'energy_meter')
  .has('timestamp', within(now - 86400000, now))
  .values('kwh')
  .mean()
```

### 4. Product Traceability

**Scenario**: Trace material genealogy and lots

```groovy
// Find all batches that used a specific lot
g.V().has('lot_number', 'LOT12345')
  .in('usedIn')
  .in('producedBy')
  .values('batch_id')

// Trace product path
g.V().has('product_id', 'PROD-001')
  .repeat(out('processedBy'))
  .until(has('type', 'final_product'))
  .path()
    .by('name')

// Find potentially defective products
g.V().has('lot_number', 'FAULTY-LOT')
  .in('usedIn')
  .out('produced')
  .values('product_id')
```

---

## References and Resources

### Official Documentation

- **WaldOT GitHub**: [https://github.com/rossonet/waldot](https://github.com/rossonet/waldot)
- **Apache TinkerPop**: [https://tinkerpop.apache.org/](https://tinkerpop.apache.org/)
- **Eclipse Milo**: [https://github.com/eclipse/milo](https://github.com/eclipse/milo)
- **OPC UA Specification**: [https://reference.opcfoundation.org/](https://reference.opcfoundation.org/)

### Plugin Guides

- [Plugin Development Manual](../../guide/docs/manuale_plugins.md) - Complete plugin development guide
- [waldot-plugin-generator](../../plugins/waldot-plugin-generator/README.md) - Data generator plugin
- [waldot-plugin-rules-engine](../../plugins/waldot-plugin-rules-engine/README.md) - Rules engine plugin
- [waldot-plugin-tinkerpop](../../plugins/waldot-plugin-tinkerpop/README.md) - Gremlin server plugin

### Containers and Distribution

- **Docker Hub**: [https://hub.docker.com/r/rossonet/waldot](https://hub.docker.com/r/rossonet/waldot)
- **Maven Central**: [https://central.sonatype.com/search?q=net.rossonet.waldot](https://central.sonatype.com/search?q=net.rossonet.waldot)

### Community and Support

- **Issue Tracker**: [https://github.com/rossonet/waldot/issues](https://github.com/rossonet/waldot/issues)
- **Discussions**: [https://github.com/rossonet/waldot/discussions](https://github.com/rossonet/waldot/discussions)

---

## Project Sponsor

[![Rossonet s.c.a r.l.](https://raw.githubusercontent.com/rossonet/images/main/artwork/rossonet-logo/png/rossonet-logo_280_115.png)](https://www.rossonet.net)

**WaldOT** is developed and maintained by **Rossonet s.c.a r.l.**

---

*WaldOT Documentation - Version 0.6.1*  
*Copyright © 2024 Rossonet s.c.a r.l. - Licensed under Apache License 2.0*
