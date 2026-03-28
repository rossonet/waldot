# WaldOT - Developer Agent Documentation

## Project Overview

WaldOT is an innovative **Digital Twin** engine that merges the power of **Apache TinkerPop graph databases** with **OPC UA** connectivity for Operational Technology (OT) environments. It creates a unified graph-based representation where OPC UA nodes become live TinkerPop vertices, enabling real-time industrial data analysis through powerful graph traversal queries.

**Version**: 0.4.13  
**License**: Apache License 2.0  
**Language**: Java 21  
**Build System**: Gradle 8.4

## Architecture

### Core Components

WaldOT is organized as a multi-module Gradle project:

```
WaldOT/
├── waldot-api/          # Core API and base abstractions
├── waldot-deps/         # Dependencies management
├── waldot-namespace/    # OPC UA namespace + TinkerPop integration
├── waldot-app/          # Application runtime
├── clients/
│   ├── waldot-client/   # Java client library
│   └── wotctl/          # CLI control tool
└── plugins/
    ├── waldot-plugin-tinkerpop/      # TinkerPop/Gremlin server
    ├── waldot-plugin-rules-engine/   # Rules engine (IF-THEN logic)
    └── waldot-plugin-generator/      # Random data generator
```

### Technology Stack

- **OPC UA Implementation**: Eclipse Milo (https://github.com/eclipse/milo)
- **Graph Database**: Apache TinkerPop 3.x (https://tinkerpop.apache.org)
- **Query Language**: Gremlin (graph traversal)
- **Rule Engine**: JEXL 3.x (Java Expression Language)
- **Transport**: OPC UA protocol on port 4840
- **APIs**: Gremlin WebSocket (8182), REST (8080)

## Key Features

### 1. OPC UA ↔ TinkerPop Bridge

Every OPC UA node in the address space is mapped to a TinkerPop vertex:
- **OPC UA Objects** → Graph Vertices
- **OPC UA References** → Graph Edges
- **OPC UA Variables** → Vertex Properties
- **NodeId** → Vertex ID

This bidirectional mapping enables:
- Navigation via OPC UA clients (UaExpert, OPC Scout, etc.)
- Query via Gremlin traversals
- Real-time updates in both directions

**Location**: `waldot-namespace/src/main/java/net/rossonet/waldot/gremlin/opcgraph/structure/`

### 2. Rules Engine Plugin

The rules engine (`waldot-plugin-rules-engine`) provides event-driven automation:

#### Core Concepts

**Rule Vertex** (`RuleVertex.java`):
- **Condition**: JEXL expression returning boolean (e.g., `g.V().has('id', 'a').next().property('value').value() == 10`)
- **Action**: JEXL script executed when condition is true
- **Hysteresis**: Debounce time to prevent repeated firing
- **Priority**: Execution priority in the compute queue
- **Debug Level**: Configurable event logging (0-6)

**Compute Vertex** (`ComputeVertex.java`):
- Thread pool manager for rule execution
- Priority-based scheduling (weight = priority × factor + queue_size)
- Timeout management (default 120 seconds)
- Virtual threads for scalability

**Fire Edge**:
- Connects data sources to rules
- Triggers rule evaluation on property changes
- Supports multiple triggers per rule

**Execute Edge**:
- Connects Compute vertex to Rule vertices
- Defines execution priority

#### JEXL Context Variables

Available in rule conditions and actions:
```javascript
log         // SLF4J logger instance
g           // GraphTraversalSource for Gremlin queries
graph       // WaldotGraph instance
cmd         // Command utilities (getVerticesCount, toNumber, etc.)
Math        // java.lang.Math
random      // ThreadLocalRandom.current()
self        // The current RuleVertex instance
```

#### Example Rule

```javascript
// Condition: temperature sensor exceeds threshold
g.V().has('id', 'tempSensor1').next().property('value').value() > 80

// Action: create alarm vertex and log
counter = cmd.toNumber(g.V().has('id', 'tempSensor1').next().property('value').value()).intValue();
graph.addVertex('label', 'alarm', 'name', 'HighTemp', 'temperature', counter, 'timestamp', System.currentTimeMillis());
log.warn('High temperature alert: ' + counter + '°C');
self.property('lastAlert', counter)
```

**Testing**: `plugins/waldot-plugin-rules-engine/src/test/java/net/rossonet/waldot/rules/WaldotRulesEngineBaseTest.java`

### 3. Plugin System

WaldOT uses a plugin architecture based on the `PluginListener` interface:

**Plugin Lifecycle**:
1. Annotation `@WaldotPlugin` marks the plugin class
2. `initialize(WaldotNamespace)` called on startup
3. Plugin registers custom vertex/edge types via OPC UA TypeNodes
4. `createVertex()` factory method called when vertices are instantiated
5. `notifyAddEdge()` / `notifyRemoveEdge()` for edge lifecycle
6. `close()` called on shutdown

**Key Methods**:
```java
void initialize(WaldotNamespace waldotNamespace)
boolean containsVertexType(String typeDefinitionLabel)
NodeId getVertexTypeNode(String typeDefinitionLabel)
WaldotVertex createVertex(NodeId typeDefinitionNodeId, ...)
void notifyAddEdge(WaldotEdge edge, WaldotVertex source, WaldotVertex target, ...)
```

**Location**: `waldot-api/src/main/java/net/rossonet/waldot/api/PluginListener.java`

### 4. Graph Queries with Gremlin

All TinkerPop traversal operations are supported:

```groovy
// Find all sensors with temperature > 50
g.V().has('type', 'sensor').has('temperature', gt(50))

// Navigate relationships
g.V().has('name', 'Line1').out('contains').has('type', 'motor')

// Complex pattern matching
g.V().has('status', 'alarm')
  .as('alarms')
  .in('monitors')
  .as('sensors')
  .select('alarms', 'sensors')
  .by('name')

// Aggregations
g.V().has('type', 'sensor')
  .values('temperature')
  .mean()
```

**Factory**: `waldot-namespace/src/main/java/net/rossonet/waldot/gremlin/opcgraph/structure/OpcFactory.java`

## Development Guide

### Building the Project

```bash
# Clone repository
git clone https://github.com/rossonet/waldot.git
cd waldot

# Build all modules
./gradlew clean build

# Run tests
./gradlew test

# Run specific plugin tests
./gradlew :waldot-plugin-rules-engine:test

# Build shadow JAR (all-in-one)
./gradlew shadowJar
```

### Running Tests

The project includes comprehensive unit tests:

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew jacocoTestReport

# View results
open build/reports/tests/test/index.html
```

Key test: `WaldotRulesEngineBaseTest.baseRuleTest()` demonstrates:
- Creating Rule and Compute vertices
- Connecting sensors to rules via Fire edges
- Dynamic rule evaluation
- Property updates triggering actions
- Queue management and execution ordering

### Creating a Custom Plugin

1. **Create Plugin Class**:
```java
@WaldotPlugin
public class MyCustomPlugin implements PluginListener, AutoCloseable {
    private WaldotNamespace waldotNamespace;
    
    @Override
    public void initialize(WaldotNamespace namespace) {
        this.waldotNamespace = namespace;
        createCustomTypeNode();
    }
    
    private void createCustomTypeNode() {
        UaObjectTypeNode customType = UaObjectTypeNode.builder(
            waldotNamespace.getOpcUaNodeContext())
            .setNodeId(waldotNamespace.generateNodeId("ObjectTypes/MyCustomType"))
            .setBrowseName(waldotNamespace.generateQualifiedName("MyCustomType"))
            .setDisplayName(LocalizedText.english("Custom Type"))
            .setIsAbstract(false)
            .build();
            
        // Add properties
        PluginListener.addParameterToTypeNode(
            waldotNamespace, customType, "customProperty", NodeIds.String);
            
        // Register in address space
        waldotNamespace.getStorageManager().addNode(customType);
        customType.addReference(new Reference(
            customType.getNodeId(), 
            NodeIds.HasSubtype, 
            NodeIds.BaseObjectType.expanded(), 
            false));
            
        waldotNamespace.getObjectTypeManager().registerObjectType(
            customType.getNodeId(), 
            UaObjectNode.class, 
            objectNodeConstructor);
    }
    
    @Override
    public boolean containsVertexType(String typeLabel) {
        return "mycustom".equals(typeLabel);
    }
    
    @Override
    public WaldotVertex createVertex(NodeId typeNodeId, WaldotGraph graph, ...) {
        return new MyCustomVertex(graph, context, nodeId, ...);
    }
    
    @Override
    public void close() throws Exception {
        // Cleanup resources
    }
}
```

2. **Implement Custom Vertex**:
```java
public class MyCustomVertex extends AbstractOpcVertex {
    public MyCustomVertex(WaldotGraph graph, UaNodeContext context, 
                         NodeId nodeId, ...) {
        super(graph, context, nodeId, browseName, displayName, 
              description, writeMask, userWriteMask, eventNotifier, version);
        // Initialize custom logic
    }
    
    @Override
    public void notifyPropertyValueChanging(String label, DataValue value) {
        super.notifyPropertyValueChanging(label, value);
        // Custom property change handling
    }
}
```

3. **Add to Build**:
```gradle
// settings.gradle
include 'waldot-plugin-mycustom'
project(':waldot-plugin-mycustom').projectDir = file('plugins/waldot-plugin-mycustom')
```

### Bootstrapping a Graph

WaldOT uses a bootstrap strategy pattern:

```java
// From file
WaldotGraph g = OpcFactory.getOpcGraph(
    "file:///tmp/boot.conf", 
    new LoggerHistoryStrategy()
);

// Programmatically
WaldotGraph g = OpcFactory.getOpcGraph();
Vertex sensor = g.addVertex("id", "sensor1", "type", "temperature", "value", 20.5);
Vertex alarm = g.addVertex("id", "alarm1", "type", "alarm", "threshold", 50.0);
sensor.addEdge("triggers", alarm);
```

## Code Organization

### Package Structure

```
net.rossonet.waldot
├── api/
│   ├── annotation/          # Plugin annotations (@WaldotPlugin, etc.)
│   ├── auth/                # Authentication validators
│   ├── configuration/       # Configuration interfaces
│   ├── models/              # Core interfaces (WaldotGraph, WaldotVertex, etc.)
│   └── strategies/          # Strategy interfaces (MiloStrategy, HistoryStrategy, etc.)
├── opc/                     # OPC UA base implementations
│   ├── AbstractOpcVertex.java
│   ├── AbstractOpcEdge.java
│   └── WaldotOpcUaServer.java
├── gremlin/opcgraph/        # TinkerPop implementation
│   ├── structure/           # Graph, Vertex, Edge implementations
│   ├── process/             # Graph traversal logic
│   └── strategies/          # Execution strategies
├── rules/                   # Rules engine
│   ├── vertices/            # RuleVertex, ComputeVertex
│   ├── edges/               # ComputeMonitoredEdge
│   └── events/              # FireableAction, RunnableEvent
├── jexl/                    # JEXL integration
└── utils/                   # Utilities (logging, network, SSL, etc.)
```

### Key Interfaces

**WaldotGraph** extends `org.apache.tinkerpop.gremlin.structure.Graph`:
- Bridges OPC UA and TinkerPop
- Manages namespace lifecycle
- Provides traversal source

**WaldotVertex** extends `org.apache.tinkerpop.gremlin.structure.Vertex`:
- Backed by OPC UA ObjectNode
- Properties sync to OPC UA Variables
- Supports property observers

**WaldotEdge** extends `org.apache.tinkerpop.gremlin.structure.Edge`:
- OPC UA Reference mapping
- Type-based monitoring (via MonitoredEdge)

**WaldotNamespace**:
- Central registry for plugins
- OPC UA server management
- Graph instance provider
- Command registry

## Best Practices

### 1. Rule Design

- **Keep conditions simple**: Complex logic should be in actions
- **Use hysteresis**: Prevent rapid re-triggering (e.g., 1000ms)
- **Handle null values**: Check existence before accessing properties
- **Use debug levels**: Start with level 6 for development, 0 for production
- **Leverage JEXL functions**: Use `cmd.*` utilities for type conversions

### 2. Graph Modeling

- **Use meaningful IDs**: String IDs like "sensor1" not just numbers
- **Label vertices**: Use `T.label` to categorize vertex types
- **Index key properties**: Consider which properties need fast lookup
- **Model relationships explicitly**: Use typed edges (e.g., "monitors", "triggers")
- **Keep property names consistent**: Use conventions across vertex types

### 3. Performance

- **Batch operations**: Use Gremlin transactions for bulk updates
- **Limit traversal depth**: Use `.limit()`, `.range()` on deep traversals
- **Use vertex/edge caches**: The namespace provides caching
- **Monitor queue sizes**: Check Compute vertex queue property
- **Tune thread pools**: Adjust Compute vertex `threads` property based on load

### 4. Testing

- **Test rules in isolation**: Create minimal graph structures
- **Use fixed delays**: Add `Thread.sleep()` for async assertions
- **Check both OPC UA and Graph**: Verify consistency with `checkOpcUaVertexExists()` and `checkVertexExists()`
- **Validate counters**: Assert on `total`, `executed`, `errors`, `queue` properties
- **Clean up**: Ensure `close()` and cleanup in `@AfterEach`

## Common Patterns

### Pattern 1: Threshold Monitoring

```java
// Create sensor
Vertex sensor = g.addVertex("id", "temp1", "value", 20);

// Create alarm rule
Vertex alarm = g.addVertex(
    "id", "tempAlarm",
    "type", "rule",
    "condition", "g.V().has('id','temp1').next().property('value').value() > 50",
    "action", "log.error('Temperature exceeded!'); graph.addVertex('label','alert','sensor','temp1')"
);

// Create compute manager
Vertex compute = g.addVertex("id", "compute1", "type", "compute", "threads", 2);

// Wire up
sensor.addEdge("fire", alarm);
compute.addEdge("execute", alarm, "priority", 100);
```

### Pattern 2: Data Aggregation

```java
// Action that calculates average
String action = """
    temps = g.V().has('type','sensor').values('temperature').toList();
    avg = temps.stream().mapToDouble(d -> d).average().orElse(0.0);
    graph.addVertex('type','report','avgTemp',avg,'timestamp',System.currentTimeMillis());
    log.info('Average temperature: ' + avg);
""";
```

### Pattern 3: State Machine

```java
// Action with state transitions
String action = """
    currentState = self.property('state').value();
    if (currentState == 'idle' && inputValue > threshold) {
        self.property('state', 'active');
        log.info('Transitioning to active');
    } else if (currentState == 'active' && inputValue < threshold) {
        self.property('state', 'idle');
        log.info('Transitioning to idle');
    }
""";
```

## Troubleshooting

### Issue: Rules not firing

**Checks**:
1. Verify Fire edges exist: `g.V().has('id','rule').inE('fire')`
2. Check condition syntax: Set `debug` to 6, inspect OPC UA events
3. Confirm Compute vertex is running: Check `queue` property
4. Verify input changes: Add logging in condition

### Issue: OPC UA connection timeout

**Solutions**:
- Check port 4840 is not in use: `lsof -i :4840`
- Wait for full startup: Add `Thread.sleep(500)` after `OpcFactory.getOpcGraph()`
- Review security settings: Check certificate trust
- Enable trace logging: `LogHelper.changeJulLogLevel("fine")`

### Issue: Graph not persisting

**Solutions**:
- Check bootstrap file: Verify path in `OpcFactory.getOpcGraph("file:///path/boot.conf", ...)`
- Ensure write permissions: Bootstrap file directory must be writable
- Use explicit close: Call `g.getWaldotNamespace().close()`

## Resources

### Documentation
- OPC UA Specification: https://reference.opcfoundation.org/Core/Part3/v104/docs/4
- TinkerPop Docs: https://tinkerpop.apache.org/docs/current/reference/
- Gremlin Recipes: https://tinkerpop.apache.org/docs/current/recipes/
- Eclipse Milo: https://github.com/eclipse/milo

### Project Links
- GitHub: https://github.com/rossonet/waldot
- Docker Hub: https://hub.docker.com/r/rossonet/waldot
- Maven Central: https://central.sonatype.com/search?q=net.rossonet.waldot

### Key Files
- **Plugin API**: `waldot-api/src/main/java/net/rossonet/waldot/api/PluginListener.java`
- **Rules Engine**: `plugins/waldot-plugin-rules-engine/src/main/java/net/rossonet/waldot/rules/`
- **Graph Factory**: `waldot-namespace/src/main/java/net/rossonet/waldot/gremlin/opcgraph/structure/OpcFactory.java`
- **Test Examples**: `plugins/waldot-plugin-rules-engine/src/test/java/net/rossonet/waldot/rules/WaldotRulesEngineBaseTest.java`

## Contributing

1. **Fork & Branch**: Create feature branches from `master`
2. **Code Style**: Follow existing conventions (Java 21, 4-space indent)
3. **Tests Required**: Add JUnit 5 tests for new features
4. **Documentation**: Update relevant README.md files
5. **Commit Messages**: Use descriptive messages (e.g., "Add hysteresis support to RuleVertex")
6. **Pull Request**: Submit PR with clear description of changes

All tests must pass: `./gradlew test`

---

**Project Sponsor**: [Rossonet s.c.a r.l.](https://www.rossonet.net)
