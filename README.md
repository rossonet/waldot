# WaldOT

**A Digital Twin Engine Bridging OPC UA and Apache TinkerPop Graphs**

[![WaldOT logo](https://raw.githubusercontent.com/rossonet/waldot/refs/heads/master/docs/artwork/logo.png)](https://github.com/rossonet/waldot)

WaldOT is an innovative open-source project that seamlessly integrates industrial automation (OT - Operational Technology) with modern data analysis through graph databases. Built on [Apache TinkerPop](https://tinkerpop.apache.org/gremlin.html) and [Eclipse Milo OPC UA](https://projects.eclipse.org/projects/iot.milo), it transforms OPC UA address spaces into queryable, reactive graph structures.

## Why WaldOT?

### Unified OT/IT Data Model
Traditional industrial systems expose data through OPC UA - a powerful but hierarchical protocol. WaldOT revolutionizes this by representing the entire OPC UA address space as a **living graph database**, where:
- **OPC UA Objects** become **TinkerPop Vertices**
- **OPC UA References** become **Graph Edges**  
- **OPC UA Variables** become **Vertex Properties**
- Changes propagate in real-time in both directions

This enables industrial engineers and data scientists to work with the same data using their preferred tools: OPC UA clients for configuration, Gremlin queries for analysis.

### Powerful Graph Queries with TinkerPop

WaldOT unlocks the full potential of **Apache TinkerPop's Gremlin query language** for industrial data. Instead of navigating rigid hierarchies, you can traverse complex relationships with expressive queries:

```groovy
// Find all motors in alarm state connected to Line 1
g.V().has('name', 'Line1')
  .out('contains')
  .has('type', 'motor')
  .has('status', 'alarm')
  .values('name')

// Calculate average temperature across all sensors in a zone
g.V().has('zone', 'production')
  .has('type', 'temperature_sensor')
  .values('value')
  .mean()

// Find correlation patterns between equipment failures
g.V().has('event', 'failure')
  .as('failed')
  .in('connectedTo')
  .where(out('causedBy').as('failed'))
  .path()
```

These queries run **directly on the OPC UA server**, without external databases or ETL pipelines.

### Reactive Rules Engine

WaldOT includes a sophisticated **rules engine** via the `waldot-plugin-rules-engine` plugin that enables edge computing logic without external systems. Define IF-THEN-THAT rules using JEXL expressions with full access to graph traversal:

**Example: Temperature Monitoring**
```groovy
// Create compute vertex for rule execution
compute = graph.addVertex(
    "type", "compute",
    "label", "main-compute",
    "Threads", "4"
)

// Create rule: IF temperature > 80 THEN log warning
rule = graph.addVertex(
    "type", "rule",
    "label", "temp-alarm",
    "Condition", "temperature > 80.0",
    "Action", "log.warn('High temperature alert: ' + temperature + '°C')",
    "Priority", "100",
    "Hysteresis", "5000"  // 5 second deduplication
)

// Connect rule to compute for execution
rule.addEdge("execute", compute, "Priority", "100")

// Monitor temperature sensor
tempSensor.addEdge("fire", rule, "monitor-property", "temperature")
```

**Rules Engine Features:**
- **Event-driven**: React to OPC-UA events and property changes in real-time
- **JEXL expressions**: Powerful scripting for conditions and actions
- **Priority queuing**: Events processed by priority with hysteresis deduplication
- **Thread management**: Virtual thread pool for lightweight concurrent execution
- **Graph integration**: Full Gremlin traversal access in rules
- **Debug support**: OPC-UA debug events and comprehensive metrics
- **Edge computing**: Runs on-device with low latency and offline capability

**Documentation**: See [waldot-plugin-rules-engine](plugins/waldot-plugin-rules-engine/README.md) for complete guide

### Edge-First Architecture

WaldOT is designed to run **on-device** - directly on RTUs (Remote Terminal Units), industrial PCs, or edge gateways:
- **Low latency**: No cloud round-trips for rule evaluation
- **Offline capable**: Operates without internet connectivity
- **Bandwidth efficient**: Only sends aggregated/filtered data upstream
- **Secure**: Data processing happens within the OT network perimeter

### Standards-Based & Extensible

Built on proven open standards:
- **OPC UA** for industrial connectivity (Eclipse Milo)
- **Apache TinkerPop 3.x** for graph operations
- **Gremlin** for query language
- **JEXL** for rule expressions
- **Plugin architecture** for custom extensions

## Quick Start

### Docker

```bash
docker pull rossonet/waldot:latest
docker run -p 4840:4840 -p 8182:8182 -p 8080:8080 rossonet/waldot
```

Access:
- **OPC UA Server**: `opc.tcp://localhost:4840`
- **Gremlin Console**: `ws://localhost:8182/gremlin`
- **REST API**: `http://localhost:8080/api`

### Build from Source

```bash
git clone https://github.com/rossonet/waldot.git
cd waldot
./gradlew clean build
java -jar waldot-app/build/libs/waldot-app-*.jar
```

### Maven/Gradle Dependency

```xml
<!-- Maven -->
<dependency>
    <groupId>net.rossonet.waldot</groupId>
    <artifactId>waldot-api</artifactId>
    <version>0.6.1</version>
</dependency>
```

```gradle
// Gradle
implementation 'net.rossonet.waldot:waldot-api:0.6.1'
```

## Use Cases

### 1. Predictive Maintenance
Query historical patterns and correlations:
```groovy
// Find equipment that failed within 24h after temperature spike
g.V().has('type', 'equipment')
  .where(
    out('hasEvent').has('type', 'failure').as('failure')
    .V().has('type', 'temperature_sensor')
      .has('value', gt(90))
      .has('timestamp', within(failure.timestamp - 86400000, failure.timestamp))
  )
```

### 2. Energy Optimization
Aggregate and analyze consumption:
```groovy
// Total energy consumption per production line
g.V().has('type', 'production_line')
  .group()
    .by('name')
    .by(out('contains').values('energy_kwh').sum())
```

### 3. Quality Control
Trace product genealogy:
```groovy
// Find all batches that used a specific raw material lot
g.V().has('lot_number', 'LOT12345')
  .in('usedIn')
  .in('producedBy')
  .values('batch_id')
```

### 4. Real-Time Alerting
React to complex conditions:
```javascript
// Rule: Detect anomalous pump behavior
// Condition:
g.V().has('id', 'pump1').next().property('vibration').value() > threshold &&
g.V().has('id', 'pump1').next().property('flow').value() < minFlow

// Action:
graph.addVertex('type', 'maintenance_request', 'equipment', 'pump1', 'priority', 'high');
// Send notification via REST/MQTT/etc.
```

## Resources

### Documentation
- [Agent Documentation](AGENT.md) - Developer guide for AI agents and contributors
- [Architecture Overview](docs/) - Detailed technical documentation

### Links
- **Docker Images**: [DockerHub - WaldOT](https://hub.docker.com/r/rossonet/waldot) | [Zenoh Router](https://hub.docker.com/r/rossonet/zenohd)
- **Maven Central**: [WaldOT Artifacts](https://central.sonatype.com/search?q=net.rossonet.waldot)
- **GitHub Repository**: [rossonet/waldot](https://github.com/rossonet/waldot)

## Example: Complete Digital Twin Application

Here's a minimal example showing WaldOT's key features:

```java
// 1. Initialize WaldOT graph
WaldotGraph graph = OpcFactory.getOpcGraph("file:///tmp/waldot.db", new LoggerHistoryStrategy());

// 2. Create industrial equipment model
Vertex productionLine = graph.addVertex(
    "id", "line1", 
    "type", "production_line", 
    "name", "Assembly Line 1"
);

Vertex motor1 = graph.addVertex(
    "id", "motor1", 
    "type", "motor",
    "rpm", 1500,
    "temperature", 45.5,
    "status", "running"
);

Vertex sensor1 = graph.addVertex(
    "id", "temp_sensor_1",
    "type", "temperature_sensor", 
    "value", 45.5,
    "unit", "celsius"
);

// 3. Create relationships
productionLine.addEdge("contains", motor1);
motor1.addEdge("monitors", sensor1);

// 4. Define reactive rule
Vertex rule = graph.addVertex(
    "id", "overheat_rule",
    "type", "rule",
    "name", "Motor Overheat Detection",
    "condition", "g.V().has('id','temp_sensor_1').next().property('value').value() > 75",
    "action", """
        temp = g.V().has('id','temp_sensor_1').next().property('value').value();
        log.warn('Motor overheating detected: ' + temp + '°C');
        g.V().has('id','motor1').next().property('status', 'alarm');
        graph.addVertex('type','alert','equipment','motor1','temperature',temp,'timestamp',System.currentTimeMillis());
    """,
    "hysteresis", 5000  // 5 second debounce
);

// 5. Create compute manager
Vertex compute = graph.addVertex(
    "id", "compute_manager",
    "type", "compute",
    "threads", 2
);

// 6. Wire rule execution
sensor1.addEdge("fire", rule);              // Trigger on sensor change
compute.addEdge("execute", rule, "priority", 100);  // Execution priority

// 7. Now the system is live! Query from Gremlin or OPC UA client
// Gremlin query:
graph.traversal()
    .V().has('type', 'motor')
    .has('status', 'alarm')
    .values('name')
    .toList();

// OPC UA: Browse to opc.tcp://localhost:4840 and navigate the address space
```

## Available Plugins

WaldOT's plugin architecture enables extensibility for domain-specific functionality. Currently available plugins:

### waldot-plugin-rules-engine

Event-driven IF-THEN-THAT rule execution system with JEXL expressions, priority queuing, and virtual thread management.

**Features:**
- Event-driven rule execution
- JEXL scripting for conditions and actions
- Priority-based event queuing with hysteresis
- Virtual thread pool for concurrent execution
- Full graph access via Gremlin traversal
- OPC-UA integration with debug events

**Documentation:** [plugins/waldot-plugin-rules-engine/README.md](plugins/waldot-plugin-rules-engine/README.md)

**Quick Example:**
```groovy
rule = graph.addVertex(
    "type", "rule",
    "Condition", "temperature > 80.0 && pressure > 100.0",
    "Action", "log.error('CRITICAL state detected')"
)
```

### waldot-plugin-generator

Dynamic data simulation for testing, development, and demonstrations.

**Features:**
- 6 generation algorithms (incremental, decremental, random, sinusoidal, triangular, stopped)
- Real-time configurable update intervals
- Virtual threads for thousands of concurrent generators
- Full OPC-UA synchronization
- Perfect for testing without physical hardware

**Documentation:** [plugins/waldot-plugin-generator/README.md](plugins/waldot-plugin-generator/README.md)

**Quick Example:**
```groovy
tempSensor = graph.addVertex(
    "type", "generator",
    "Algorithm", "sinusoidal",
    "Min", "18",
    "Max", "26",
    "Delay", "5000"  // Simulates temperature sensor
)
```

### waldot-plugin-tinkerpop

Native TinkerPop/Gremlin client access to WaldOT graphs.

**Features:**
- Embedded Gremlin Server as graph vertices
- Dual protocol: OPC-UA + Gremlin simultaneously
- GraphSON v3 and GraphBinary v1 serializers
- Graph visualization tools support (Graph-Explorer)
- Standard TinkerPop client compatibility
- Live bidirectional synchronization

**Documentation:** [plugins/waldot-plugin-tinkerpop/README.md](plugins/waldot-plugin-tinkerpop/README.md)

**Quick Example:**
```groovy
gremlinServer = graph.addVertex(
    "type", "gremlin",
    "Port", "8182",
    "Bind", "0.0.0.0"  // Enables TinkerPop client connections
)

// Connect from Gremlin Console, drivers, or Graph-Explorer
```

---

## Advanced Features

### TinkerPop Graph Algorithms

WaldOT supports TinkerPop's graph algorithms for industrial analytics:

```groovy
// Shortest path between equipment
g.V().has('name', 'Pump1')
  .repeat(out().simplePath())
  .until(has('name', 'Tank5'))
  .path()
  .by('name')

// PageRank to find most connected equipment
g.V().pageRank().by('rank').values('rank').order().desc()

// Community detection for grouping related assets
g.V().connectedComponent().by('component').group().by('component')
```

### Extensibility via Plugins

Create custom vertex types and behaviors:

```java
@WaldotPlugin
public class MyIndustrialPlugin implements PluginListener {
    @Override
    public void initialize(WaldotNamespace namespace) {
        // Register custom "Conveyor" vertex type in OPC UA
        createConveyorTypeNode(namespace);
    }
    
    @Override
    public WaldotVertex createVertex(NodeId typeNodeId, ...) {
        return new ConveyorVertex(...);  // Custom logic
    }
}
```

### Integration Capabilities

- **REST API**: HTTP endpoints for external systems
- **Gremlin Server**: WebSocket protocol (port 8182)
- **OPC UA Client**: Connect to other OPC UA servers
- **Message Bus**: Zenoh pub/sub connector for edge-to-cloud
- **JDBC**: Export graph data to SQL databases
- **GraphQL**: Query via GraphQL over HTTP

## Performance & Scalability

- **Edge-optimized**: Runs on devices with 1GB+ RAM
- **Virtual threads**: Java 21 for massive concurrency
- **Persistent storage**: RocksDB backend for large graphs
- **Streaming**: Process millions of data points without memory issues
- **Distributed**: Cluster support via TinkerPop providers (Neo4j, JanusGraph, etc.)

## Development Tools

[![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-blue?logo=gitpod)](https://gitpod.io/#https://github.com/rossonet/waldot)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/b00164ee3a36444b920764db52634ebb)](https://app.codacy.com/gh/rossonet/waldot/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

### CI/CD Status

[![Test all subprojects with Gradle](https://github.com/rossonet/waldot/actions/workflows/test-branch-with-gradle.yml/badge.svg)](https://github.com/rossonet/waldot/actions/workflows/test-branch-with-gradle.yml)
[![Build WaldOT shadowJar](https://github.com/rossonet/waldot/actions/workflows/build-shadowjar-app.yml/badge.svg)](https://github.com/rossonet/waldot/actions/workflows/build-shadowjar-app.yml)
[![Build and publish WaldOT docker image to Docker Hub](https://github.com/rossonet/waldot/actions/workflows/publish-to-docker-hub.yml/badge.svg)](https://github.com/rossonet/waldot/actions/workflows/publish-to-docker-hub.yml)
[![Publish Java artifacts to Maven Central](https://github.com/rossonet/waldot/actions/workflows/publish-to-maven.yml/badge.svg)](https://github.com/rossonet/waldot/actions/workflows/publish-to-maven.yml)

## Contributing

We welcome contributions! Whether you're:
- Adding new plugins
- Improving documentation
- Reporting bugs
- Suggesting features

Please see [AGENT.md](AGENT.md) for development guidelines.

**Key contribution areas**:
- Additional storage connectors (Neo4j, JanusGraph, etc.)
- Rule engine extensions (SQL-like DSL, visual editor)
- Dashboard integrations (Grafana, real-time graph visualization)
- Protocol bridges (MQTT, Modbus, BACnet)
- AI/ML model integration

## References

- [Apache TinkerPop Documentation](https://tinkerpop.apache.org/docs/current/dev/provider/)
- [OPC UA Address Space Specification](https://reference.opcfoundation.org/Core/Part3/v104/docs/4)
- [Eclipse Milo GitHub](https://github.com/eclipse/milo)
- [Gremlin Query Language](https://tinkerpop.apache.org/docs/current/reference/#graph-traversal-steps)

## License

WaldOT is released under the **Apache License 2.0**.

See [LICENSE](LICENSE) file for details.

## Project Sponsor

[![Rossonet s.c.a r.l.](https://raw.githubusercontent.com/rossonet/images/main/artwork/rossonet-logo/png/rossonet-logo_280_115.png)](https://www.rossonet.net)

---

**Ready to bridge your OT and IT worlds? Get started with WaldOT today!**



