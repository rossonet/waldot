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

**Example: Temperature Monitoring** (bootstrap script, JEXL)
```javascript
// compute vertex: executes the rules connected to it
var compute = graph.addVertex('id', 'main-compute', 'type', 'rules:compute', 'label', 'main-compute', 'threads', '4');

// rule: IF temperature > 80 THEN log warning
var rule = graph.addVertex('id', 'temp-alarm', 'type', 'rules:rule', 'label', 'temp-alarm',
    'condition', "g.V().has('id','boiler').values('temperature').next() > 80.0",
    'action', "log.warn('High temperature alert')",
    'hysteresis', '5000');

// the compute executes the rule
compute.addEdge('runner', rule, 'type', 'rules:execute', 'priority', '100');

// every change of boiler.temperature fires the rule
boiler.addEdge('watch', rule, 'type', 'fire', 'properties-to-monitor', 'temperature');
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
docker run -p 12686:12686 -p 8443:8443 rossonet/waldot:latest
```

Access:
- **OPC UA Server**: `opc.tcp://localhost:12686/waldot`
- **Gremlin Console**: Available via waldot-plugin-tinkerpop (port 8182)
- **HTTPS API**: `https://localhost:8443`

### Docker with Custom Bootstrap Configuration

WaldOT builds the initial graph from a bootstrap file (local file or URL),
executed as a [JEXL](https://commons.apache.org/proper/commons-jexl/) script.
Statements end with `;`:

```javascript
// boot.conf
var createSensor = function(name, algorithm, min, max) {
  return graph.addVertex('id', name, 'type', 'sim:generator', 'label', name, 'directory', 'sensors',
      'algorithm', algorithm, 'min', min, 'max', max, 'delay', '1000');
};

createSensor('temp-office', 'sinusoidal', '18', '26');
createSensor('pressure-tank', 'random', '0', '100');

log.info('Sensors configured successfully');
```

Mount your configuration file:
```bash
docker run \
  -v ./my-boot.conf:/waldot/boot.conf:ro \
  -p 12686:12686 \
  rossonet/waldot:latest
```

Or use a remote URL with the `--boot-url` option (every argument after the
image name is passed to WaldOT as an option):
```bash
docker run -p 12686:12686 rossonet/waldot:latest \
  --boot-url https://example.com/waldot/boot.conf
```

All server options (ports, identity, security, commands) are listed in the
[Configuration Reference](docs/guide/docs/CONFIGURATION.md).

A legacy line mode (one expression per line, `#` comments) is also supported.
Syntax and format detection: [Bootstrap Configuration](waldot-namespace/BOOTSTRAP_CONFIGURATION.md);
step-by-step examples: [Examples](docs/guide/docs/examples/README.md).

### Build from Source

```bash
git clone https://github.com/rossonet/waldot.git
cd waldot
./gradlew clean build
./gradlew :waldot-app:shadowJar
java -jar waldot-app/build/libs/waldot-app-*-all.jar --help
```

### Maven/Gradle Dependency

```xml
<!-- Maven -->
<dependency>
    <groupId>net.rossonet.waldot</groupId>
    <artifactId>waldot-api</artifactId>
    <version>0.6.2</version>
</dependency>
```

```gradle
// Gradle
implementation 'net.rossonet.waldot:waldot-api:0.6.2'
```

## Use Cases

### 1. Predictive Maintenance
Query historical patterns and correlations:
```groovy
// Equipment with a failure event connected to an overheated sensor
g.V().has('kind', 'equipment')
  .where(out('hasEvent').has('kind', 'failure'))
  .where(in('monitors').has('kind', 'temperature_sensor').has('value', gt(90)))
  .values('name')
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

#### Core Documentation
The full documentation site is built with MkDocs from [`docs/guide`](docs/guide) (`cd docs/guide && mkdocs serve`).

- **[Documentation Hub](docs/guide/docs/README.md)** - Entry point, organized by audience
- **[Architecture Overview](docs/guide/docs/ARCHITECTURE_OVERVIEW.md)** - Architecture, deployment scenarios, security
- **[User Guide](docs/guide/docs/USER_GUIDE.md)** - Operating manual
- **[Examples](docs/guide/docs/examples/README.md)** - Step-by-step examples and showcases
- **[Configuration Reference](docs/guide/docs/CONFIGURATION.md)** - All command-line options with defaults
- **[Developer Guide](docs/guide/docs/DEVELOPER_GUIDE.md)** - Development workflow and APIs
- **[Integration Guide](docs/guide/docs/INTEGRATION_GUIDE.md)** - How to write a WaldOT plugin: vertex types, edge types, commands, packaging, tests

#### Module Documentation
- [waldot-api](waldot-api/README.md) - Public contracts for runtimes and plugins
- [waldot-namespace](waldot-namespace/README.md) - OPC UA / graph bridge and Milo strategies ([bootstrap syntax](waldot-namespace/BOOTSTRAP_CONFIGURATION.md))
- [waldot-runner](waldot-runner/README.md) - Picocli runner and server options
- [waldot-app](waldot-app/README.md) - Server application, distribution and Docker examples

#### Plugin Documentation
- [waldot-plugin-generator](plugins/waldot-plugin-generator/README.md) - Data generator plugin for testing and simulation
- [waldot-plugin-rules-engine](plugins/waldot-plugin-rules-engine/README.md) - Event-driven IF-THEN-THAT rule execution
- [waldot-plugin-tinkerpop](plugins/waldot-plugin-tinkerpop/README.md) - Embedded Gremlin Server for remote access

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
```javascript
var rule = graph.addVertex('id', 'critical', 'type', 'rules:rule', 'label', 'critical',
    'condition', "g.V().has('id','reactor').values('temperature').next() > 80.0",
    'action', "log.error('CRITICAL state detected')");
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
```javascript
// simulated temperature sensor: new value in 'data' every 5 s
var tempSensor = graph.addVertex('id', 'temp-1', 'type', 'sim:generator', 'label', 'temp-1',
    'algorithm', 'sinusoidal', 'min', '18', 'max', '26', 'delay', '5000');
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
```javascript
// Gremlin Server on port 8182 (command: label, directory, port, bind)
tinkerpop_create_gremlin_server.exec('gremlin-1', 'services', '8182', '0.0.0.0');
// then: curl -X POST -d '{"gremlin":"g.V().count()"}' http://localhost:8182
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

WaldOT's **plugin architecture** enables custom vertex types, behaviors, and integrations. Plugins leverage **Java 26 virtual threads** for massive concurrency - run thousands of active objects with minimal resource overhead.

**Key Features:**
- **Auto-discovery**: Plugins annotated with `@WaldotPlugin` are automatically loaded
- **Virtual threads**: Lightweight concurrency for polling, monitoring, event processing
- **Bi-directional sync**: Plugin vertices automatically sync with OPC UA
- **Full graph access**: Plugins can use Gremlin queries for complex logic

**Example Plugin:**

```java
@WaldotPlugin
public class MyIndustrialPlugin implements AutoCloseable, PluginListener {
    private final ExecutorService executor = ThreadHelper.newVirtualThreadExecutor();
    
    @Override
    public void initialize(WaldotNamespace namespace) {
        // Register custom "Conveyor" vertex type in OPC UA
        createConveyorTypeNode(namespace);
    }
    
    @Override
    public WaldotVertex createVertex(NodeId typeNodeId, ...) {
        // Create vertex with virtual thread for continuous operation
        return new ConveyorVertex(executor, ...);  // Runs in virtual thread
    }
    
    @Override
    public void close() throws Exception {
        executor.shutdownNow();  // Clean shutdown
    }
}
```

**Learn More:**
- **[Integration Guide](docs/guide/docs/INTEGRATION_GUIDE.md)** - Step-by-step plugin example, lifecycle, packaging and tests
- **[Architecture Overview](docs/guide/docs/ARCHITECTURE_OVERVIEW.md)** - Architecture details and advanced topics

### Integration Capabilities

- **REST API**: HTTP endpoints for external systems
- **Gremlin Server**: WebSocket protocol (port 8182)
- **OPC UA Client**: Connect to other OPC UA servers
- **Message Bus**: Zenoh pub/sub connector for edge-to-cloud
- **JDBC**: Export graph data to SQL databases
- **GraphQL**: Query via GraphQL over HTTP

## Performance & Scalability

- **Edge-optimized**: Runs on devices with 1GB+ RAM
- **Virtual threads**: Java 26 runtime for massive concurrency
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

Please see the [Developer Guide](docs/guide/docs/DEVELOPER_GUIDE.md) for development guidelines.

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

[![Zona Autonoma srl](https://raw.githubusercontent.com/rossonet/images/main/artwork/rossonet-logo/png/rossonet-logo_280_115.png)](https://www.rossonet.net)


