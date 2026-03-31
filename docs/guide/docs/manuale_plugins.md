# WaldOT Plugin Development Guide

## Introduction

WaldOT is a Digital Twin Engine that bridges OPC UA and Apache TinkerPop graphs, creating a powerful platform where industrial automation (OT) meets modern graph databases. The plugin architecture is the cornerstone of WaldOT's extensibility, allowing developers to create custom vertex types, edges, and behaviors that integrate seamlessly into both the OPC UA address space and the TinkerPop graph.

### What is a WaldOT Plugin?

A WaldOT plugin is a Java module that extends WaldOT's functionality by:

- **Registering custom vertex types** that appear as OPC UA objects and TinkerPop vertices
- **Defining custom behaviors** using virtual threads for lightweight concurrency
- **Integrating external data sources** (sensors, databases, APIs) into the graph
- **Implementing domain-specific logic** (rules engines, data generators, protocol bridges)
- **Providing reusable commands** accessible via console or OPC UA methods

### Why Plugins?

The plugin architecture enables:

1. **Modularity**: Each plugin is self-contained with its own dependencies and lifecycle
2. **Reusability**: Plugins can be shared across different WaldOT deployments
3. **Performance**: Virtual threads (Java 21+) enable thousands of concurrent operations
4. **Bi-directional sync**: Changes propagate automatically between OPC UA and TinkerPop
5. **Live graph integration**: Plugins access the full power of Gremlin queries in real-time

## Architecture Overview

### Core Concepts

#### OPC UA + TinkerPop Integration

WaldOT creates a living bidirectional bridge:

```
OPC UA Address Space          TinkerPop Graph
┌─────────────────┐          ┌─────────────────┐
│  Objects        │ ←────→   │  Vertices       │
│  References     │ ←────→   │  Edges          │
│  Variables      │ ←────→   │  Properties     │
└─────────────────┘          └─────────────────┘
        ↕                            ↕
   OPC UA Clients              Gremlin Queries
```

**Key mappings:**
- OPC UA Objects → TinkerPop Vertices
- OPC UA References → Graph Edges
- OPC UA Variables → Vertex Properties
- Changes propagate in both directions in real-time

#### Plugin Lifecycle

```
1. Plugin Discovery
   ↓ (@WaldotPlugin annotation scanning)
2. Plugin Registration
   ↓ (PluginListener.initialize())
3. Type Registration
   ↓ (Create OPC UA type nodes)
4. Plugin Start
   ↓ (PluginListener.start())
5. Runtime Operations
   ↓ (Create vertices, handle events)
6. Plugin Stop
   ↓ (PluginListener.stop())
7. Plugin Close
   ↓ (AutoCloseable.close())
```

### Virtual Thread Architecture

**WaldOT leverages Java 21+ virtual threads for scalability:**

- **Lightweight**: ~1KB per thread (vs. ~1MB for platform threads)
- **Massive concurrency**: Create thousands of threads without resource exhaustion
- **Simple blocking code**: Use `Thread.sleep()` without blocking OS threads
- **Automatic scheduling**: JVM manages virtual thread lifecycle

**Design principle**: Each active object (generator, sensor, monitor) runs in its own virtual thread, making the codebase simple and maintainable while achieving high concurrency.

## Creating a Plugin

### Step 1: Project Setup

Create a Gradle subproject under `plugins/`:

```gradle
// plugins/my-plugin/build.gradle
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

Add to `settings.gradle`:
```gradle
include 'plugins:my-plugin'
```

### Step 2: Implement PluginListener

Create the main plugin class with `@WaldotPlugin` annotation:

```java
package net.rossonet.waldot.myplugin;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WaldotPlugin
public class MyPlugin implements AutoCloseable, PluginListener {
    private static final Logger logger = LoggerFactory.getLogger(MyPlugin.class);
    private WaldotNamespace waldotNamespace;
    
    @Override
    public void initialize(WaldotNamespace waldotNamespace) {
        this.waldotNamespace = waldotNamespace;
        logger.info("MyPlugin initialized");
        
        // Create OPC UA type nodes here
        createCustomTypeNodes();
    }
    
    @Override
    public void start() {
        logger.info("MyPlugin started");
        // Start background tasks, monitoring, etc.
    }
    
    @Override
    public void stop() {
        logger.info("MyPlugin stopped");
        // Stop background tasks
    }
    
    @Override
    public void close() throws Exception {
        logger.info("MyPlugin closed");
        // Clean up resources
    }
    
    private void createCustomTypeNodes() {
        // Will implement in next section
    }
}
```

### Step 3: Register Custom Vertex Types

Define OPC UA type nodes that map to TinkerPop vertices:

```java
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.sdk.core.Reference;

public class MyPlugin implements AutoCloseable, PluginListener {
    public static final String MY_SENSOR_TYPE_LABEL = "my-sensor";
    private UaObjectTypeNode mySensorTypeNode;
    
    private void createCustomTypeNodes() {
        // Create OPC UA ObjectType node
        mySensorTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
            .setNodeId(waldotNamespace.generateNodeId("ObjectTypes/MySensorObjectType"))
            .setBrowseName(waldotNamespace.generateQualifiedName("MySensorObjectType"))
            .setDisplayName(LocalizedText.english("My Sensor"))
            .setIsAbstract(false)
            .build();
        
        // Add standard label property
        PluginListener.addParameterToTypeNode(
            waldotNamespace, 
            mySensorTypeNode, 
            "label", 
            NodeIds.String
        );
        
        // Add custom properties
        PluginListener.addParameterToTypeNode(
            waldotNamespace, 
            mySensorTypeNode, 
            "temperature", 
            NodeIds.Double
        );
        
        PluginListener.addParameterToTypeNode(
            waldotNamespace, 
            mySensorTypeNode, 
            "humidity", 
            NodeIds.Double
        );
        
        // Register in OPC UA address space
        waldotNamespace.getStorageManager().addNode(mySensorTypeNode);
        mySensorTypeNode.addReference(new Reference(
            mySensorTypeNode.getNodeId(),
            NodeIds.HasSubtype,
            NodeIds.BaseObjectType.expanded(),
            false
        ));
        
        // Register constructor
        waldotNamespace.getObjectTypeManager().registerObjectType(
            mySensorTypeNode.getNodeId(),
            UaObjectNode.class,
            PluginListener.objectNodeConstructor
        );
    }
    
    @Override
    public boolean containsVertexType(String typeDefinitionLabel) {
        return MY_SENSOR_TYPE_LABEL.equals(typeDefinitionLabel);
    }
    
    @Override
    public boolean containsVertexTypeNode(NodeId typeDefinitionNodeId) {
        return mySensorTypeNode.getNodeId().equals(typeDefinitionNodeId);
    }
    
    @Override
    public NodeId getVertexTypeNode(String typeDefinitionLabel) {
        if (MY_SENSOR_TYPE_LABEL.equals(typeDefinitionLabel)) {
            return mySensorTypeNode.getNodeId();
        }
        return null;
    }
}
```

### Step 4: Implement Custom Vertex

Create a vertex class that extends `WaldotVertex`:

```java
package net.rossonet.waldot.myplugin;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotVertex;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

public class MySensorVertex extends WaldotVertex implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(MySensorVertex.class);
    
    private final ExecutorService executor;
    private volatile boolean active = true;
    private double temperature = 20.0;
    private double humidity = 50.0;
    
    public MySensorVertex(ExecutorService executor, WaldotGraph graph, 
                         UaNodeContext context, NodeId nodeId, 
                         QualifiedName browseName, LocalizedText displayName,
                         LocalizedText description, UInteger writeMask, 
                         UInteger userWriteMask, UByte eventNotifier,
                         long version, Object[] propertyKeyValues) {
        super(graph, context, nodeId, browseName, displayName, description,
              writeMask, userWriteMask, eventNotifier, version, propertyKeyValues);
        
        this.executor = executor;
        
        // Parse initial property values
        parseProperties(propertyKeyValues);
        
        // Start sensor simulation in virtual thread
        executor.submit(this::sensorLoop);
        
        logger.info("MySensorVertex created: {}", displayName.getText());
    }
    
    private void parseProperties(Object[] propertyKeyValues) {
        if (propertyKeyValues != null) {
            for (int i = 0; i < propertyKeyValues.length - 1; i += 2) {
                String key = propertyKeyValues[i].toString();
                Object value = propertyKeyValues[i + 1];
                
                switch (key) {
                    case "temperature":
                        temperature = Double.parseDouble(value.toString());
                        break;
                    case "humidity":
                        humidity = Double.parseDouble(value.toString());
                        break;
                }
            }
        }
    }
    
    /**
     * Sensor simulation loop - runs in virtual thread
     */
    private void sensorLoop() {
        while (active) {
            try {
                // Simulate sensor readings with slight variations
                temperature += (Math.random() - 0.5) * 0.5;
                humidity += (Math.random() - 0.5) * 2.0;
                
                // Clamp values
                temperature = Math.max(15.0, Math.min(35.0, temperature));
                humidity = Math.max(30.0, Math.min(80.0, humidity));
                
                // Update properties (automatically syncs to OPC UA)
                property("temperature", temperature);
                property("humidity", humidity);
                
                // Sleep for update interval (virtual thread - doesn't block OS thread)
                Thread.sleep(5000);  // Update every 5 seconds
                
            } catch (InterruptedException e) {
                logger.info("Sensor loop interrupted");
                active = false;
            } catch (Exception e) {
                logger.error("Error in sensor loop", e);
            }
        }
    }
    
    @Override
    public void close() throws Exception {
        active = false;
        logger.info("MySensorVertex closed");
    }
}
```

### Step 5: Implement Vertex Factory

Add vertex creation to the plugin:

```java
public class MyPlugin implements AutoCloseable, PluginListener {
    private final ExecutorService executor = ThreadHelper.newVirtualThreadExecutor();
    
    @Override
    public WaldotVertex createVertex(NodeId typeDefinitionNodeId, 
                                    WaldotGraph graph,
                                    UaNodeContext context, NodeId nodeId,
                                    QualifiedName browseName, 
                                    LocalizedText displayName,
                                    LocalizedText description, 
                                    UInteger writeMask,
                                    UInteger userWriteMask, 
                                    UByte eventNotifier,
                                    long version, Object[] propertyKeyValues) {
        if (!containsVertexTypeNode(typeDefinitionNodeId)) {
            return null;
        }
        
        if (mySensorTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
            return new MySensorVertex(executor, graph, context, nodeId,
                browseName, displayName, description, writeMask,
                userWriteMask, eventNotifier, version, propertyKeyValues);
        }
        
        return null;
    }
    
    @Override
    public void close() throws Exception {
        executor.shutdownNow();
        logger.info("MyPlugin closed");
    }
}
```

## Integrating External Data

### Pattern 1: Polling External Source

Example: Reading from REST API or database in virtual thread

```java
public class ExternalDataVertex extends WaldotVertex implements AutoCloseable {
    private final ExecutorService executor;
    private volatile boolean active = true;
    private final String apiUrl;
    
    public ExternalDataVertex(..., Object[] propertyKeyValues) {
        super(...);
        this.executor = executor;
        this.apiUrl = extractProperty(propertyKeyValues, "apiUrl", "http://localhost:8080/api");
        
        // Start polling in virtual thread
        executor.submit(this::pollExternalSource);
    }
    
    private void pollExternalSource() {
        while (active) {
            try {
                // Fetch data from external source
                Map<String, Object> data = fetchDataFromAPI(apiUrl);
                
                // Update graph properties (syncs to OPC UA automatically)
                data.forEach((key, value) -> property(key, value));
                
                // Sleep interval (virtual thread)
                Thread.sleep(10000);  // Poll every 10 seconds
                
            } catch (InterruptedException e) {
                active = false;
            } catch (Exception e) {
                logger.error("Error polling external source", e);
                property("error", e.getMessage());
            }
        }
    }
    
    private Map<String, Object> fetchDataFromAPI(String url) {
        // Implementation using HttpClient, JDBC, etc.
        // This can be blocking - we're in a virtual thread!
        return httpClient.get(url).bodyAsMap();
    }
    
    @Override
    public void close() throws Exception {
        active = false;
    }
}
```

### Pattern 2: Event-Driven Integration

Example: Subscribing to MQTT or message queue

```java
public class MqttBridgeVertex extends WaldotVertex implements AutoCloseable {
    private final MqttClient mqttClient;
    
    public MqttBridgeVertex(..., Object[] propertyKeyValues) {
        super(...);
        String broker = extractProperty(propertyKeyValues, "broker", "tcp://localhost:1883");
        String topic = extractProperty(propertyKeyValues, "topic", "#");
        
        // Initialize MQTT client
        mqttClient = new MqttClient(broker, "waldot-" + UUID.randomUUID());
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // Parse message and update vertex properties
                JSONObject json = new JSONObject(new String(message.getPayload()));
                json.keys().forEachRemaining(key -> {
                    property(key, json.get(key));
                });
            }
        });
        
        mqttClient.connect();
        mqttClient.subscribe(topic);
    }
    
    @Override
    public void close() throws Exception {
        mqttClient.disconnect();
        mqttClient.close();
    }
}
```

### Pattern 3: Reactive Streams

Example: Processing stream of events with backpressure

```java
import java.util.concurrent.Flow.*;

public class StreamProcessorVertex extends WaldotVertex 
        implements Subscriber<SensorReading>, AutoCloseable {
    
    private Subscription subscription;
    
    public StreamProcessorVertex(..., Publisher<SensorReading> publisher) {
        super(...);
        
        // Subscribe to reactive stream
        publisher.subscribe(this);
    }
    
    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);  // Request first item
    }
    
    @Override
    public void onNext(SensorReading reading) {
        // Process reading and update graph
        property("temperature", reading.getTemperature());
        property("timestamp", reading.getTimestamp());
        
        // Request next item (backpressure control)
        subscription.request(1);
    }
    
    @Override
    public void onError(Throwable throwable) {
        logger.error("Stream error", throwable);
        property("error", throwable.getMessage());
    }
    
    @Override
    public void onComplete() {
        logger.info("Stream completed");
        property("status", "completed");
    }
    
    @Override
    public void close() throws Exception {
        if (subscription != null) {
            subscription.cancel();
        }
    }
}
```

## Real-World Examples

### Example 1: waldot-plugin-generator

**Purpose**: Simulate dynamic data for testing without hardware

**Key features**:
- 6 generation algorithms (incremental, decremental, random, sinusoidal, triangular, stopped)
- Configurable update intervals
- Thousands of concurrent generators using virtual threads

**Vertex implementation** (simplified):

```java
public class DataGeneratorVertex extends WaldotVertex implements AutoCloseable {
    private final ExecutorService executor;
    private volatile boolean active = true;
    private Algorithm algorithm = Algorithm.incremental;
    private long delay = 1000L;
    private double min = 0.0;
    private double max = 100.0;
    private double currentValue;
    
    public DataGeneratorVertex(ExecutorService executor, ...) {
        super(...);
        this.executor = executor;
        parseProperties(propertyKeyValues);
        
        // Start generation in virtual thread
        executor.submit(this::generationLoop);
    }
    
    private void generationLoop() {
        while (active) {
            try {
                // Generate next value based on algorithm
                switch (algorithm) {
                    case incremental:
                        currentValue = (currentValue + 1) % (max - min) + min;
                        break;
                    case random:
                        currentValue = Math.random() * (max - min) + min;
                        break;
                    case sinusoidal:
                        currentValue = (max - min) / 2 * Math.sin(seed++) + (max + min) / 2;
                        break;
                    // ... other algorithms
                }
                
                // Update property (syncs to OPC UA)
                property("data", currentValue);
                
                // Sleep (virtual thread - lightweight)
                Thread.sleep(delay);
                
            } catch (InterruptedException e) {
                active = false;
            }
        }
    }
    
    @Override
    public void close() throws Exception {
        active = false;
    }
}
```

**Usage**:
```groovy
// Create temperature sensor simulator
tempSensor = graph.addVertex(
    "type", "generator",
    "label", "office-temp",
    "Algorithm", "sinusoidal",
    "Min", "18",
    "Max", "26",
    "Delay", "5000"
)

// Read generated value
temperature = tempSensor.property("data").value()
```

**Virtual thread benefit**: Can create 10,000+ generators without performance degradation.

### Example 2: waldot-plugin-rules-engine

**Purpose**: Event-driven IF-THEN-THAT rule execution

**Key features**:
- JEXL expressions for conditions and actions
- Priority queuing with hysteresis deduplication
- Virtual thread pool for concurrent rule execution
- Full Gremlin graph access in rules

**Architecture**:
```
[Source Vertex] → [FireMonitoredEdge] → [RuleVertex] → [ComputeMonitoredEdge] → [ComputeVertex]
   (property)         (filters)          (enqueues)         (routes)            (executes)
```

**RuleVertex implementation** (simplified):

```java
public class RuleVertex extends ComputableFireableAbstractOpcVertex {
    private String condition = "true";
    private String action = "log.info('Rule fired')";
    private JexlEngine jexl;
    private JexlScript compiledCondition;
    private JexlScript compiledAction;
    
    @Override
    protected RunnableEvent getRunnablePropertyEvent(...) {
        return new RunnableEvent(
            EventType.PROPERTY_CHANGE,
            priority,
            new RuleVertexFireableAction(sourceNode, propertyLabel, value)
        );
    }
    
    private class RuleVertexFireableAction extends FireableAction {
        @Override
        public void run() {
            try {
                // Create JEXL context with variables
                JexlContext context = createContext();
                context.set("temperature", propertyValue);
                context.set("g", graph.traversal());
                context.set("log", logger);
                context.set("self", RuleVertex.this);
                
                // Evaluate condition
                if (compiledCondition == null) {
                    compiledCondition = jexl.createScript(condition);
                }
                Boolean result = (Boolean) compiledCondition.execute(context);
                
                // Execute action if condition true
                if (Boolean.TRUE.equals(result)) {
                    if (compiledAction == null) {
                        compiledAction = jexl.createScript(action);
                    }
                    compiledAction.execute(context);
                }
                
            } catch (Exception e) {
                logger.error("Rule execution error", e);
            }
        }
    }
}
```

**ComputeVertex** (thread manager):

```java
public class ComputeVertex extends WaldotVertex {
    private int threads = 1;
    private ExecutorService executor;
    private PriorityBlockingQueue<DirtyNode> dirtyNodes;
    
    public ComputeVertex(...) {
        super(...);
        this.executor = ThreadHelper.newVirtualThreadExecutor();
        this.dirtyNodes = new PriorityBlockingQueue<>();
        
        // Start compute manager in virtual thread
        executor.submit(this::computeLoop);
    }
    
    private void computeLoop() {
        while (active) {
            try {
                // Take highest priority dirty node
                DirtyNode dirtyNode = dirtyNodes.take();
                
                // Poll event from rule
                RunnableEvent event = dirtyNode.getRule().poll();
                
                if (event != null) {
                    // Submit action to thread pool (virtual thread)
                    Future<?> future = executor.submit(event.getAction());
                    
                    // Track for timeout
                    activeRunners.put(future, event.getAction());
                }
                
            } catch (InterruptedException e) {
                active = false;
            }
        }
    }
}
```

**Usage**:
```groovy
// Create compute vertex
compute = graph.addVertex(
    "type", "compute",
    "label", "main-compute",
    "Threads", "4"
)

// Create rule
rule = graph.addVertex(
    "type", "rule",
    "label", "temp-alarm",
    "Condition", "temperature > 80.0",
    "Action", "log.warn('High temperature: ' + temperature)",
    "Priority", "100",
    "Hysteresis", "5000"
)

// Connect rule to compute
rule.addEdge("execute", compute, "Priority", "100")

// Monitor temperature sensor
tempSensor.addEdge("fire", rule, "monitor-property", "temperature")
```

**Virtual thread benefit**: Handles thousands of concurrent rules and events without blocking.

### Example 3: waldot-plugin-tinkerpop

**Purpose**: Embed Gremlin Server as graph vertex for remote access

**Key features**:
- Standard TinkerPop client connectivity (WebSocket)
- Multiple serializers (GraphSON, GraphBinary)
- Dual access: OPC UA + Gremlin on same graph
- Live bi-directional synchronization

**Implementation** (simplified):

```java
public class GremlinServerVertex extends WaldotVertex implements AutoCloseable {
    private GremlinServer server;
    private int port = 8182;
    private String bind = "0.0.0.0";
    
    public GremlinServerVertex(...) {
        super(...);
        parseProperties(propertyKeyValues);
        
        // Start Gremlin Server
        startServer();
        
        // Monitor property changes
        addPropertyChangeListener("Port", this::onPortChanged);
    }
    
    private void startServer() {
        try {
            Settings settings = Settings.build()
                .host(bind)
                .port(port)
                .scriptEngines(createEngineSettings())
                .serializers(createSerializerSettings())
                .create();
            
            server = new GremlinServer(settings);
            server.start().join();
            
            property("Status", "Running");
            logger.info("Gremlin Server started on {}:{}", bind, port);
            
        } catch (Exception e) {
            property("Status", "Failed");
            logger.error("Failed to start Gremlin Server", e);
        }
    }
    
    private void onPortChanged(Object newPort) {
        // Restart server with new port
        stopServer();
        this.port = Integer.parseInt(newPort.toString());
        startServer();
    }
    
    @Override
    public void close() throws Exception {
        stopServer();
    }
    
    private void stopServer() {
        if (server != null) {
            server.stop().join();
            property("Status", "Stopped");
        }
    }
}
```

**Usage**:
```groovy
// Create Gremlin Server vertex
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
```

**No virtual threads needed**: Gremlin Server has its own thread pool, vertex just manages lifecycle.

## Virtual Thread Best Practices

### 1. Use Virtual Threads for Blocking I/O

**Good** - Virtual thread with blocking operation:
```java
private void pollSensor() {
    while (active) {
        try {
            // Blocking call - OK in virtual thread
            SensorData data = httpClient.get(sensorUrl).block();
            property("value", data.getValue());
            
            // Sleep - OK in virtual thread
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            active = false;
        }
    }
}
```

**Bad** - Platform thread with blocking operation:
```java
// DON'T create platform threads for blocking I/O
Thread platformThread = new Thread(this::pollSensor);  // BAD!
platformThread.start();
```

### 2. Don't Pool Virtual Threads

**Good** - Create virtual threads as needed:
```java
ExecutorService executor = ThreadHelper.newVirtualThreadExecutor();

// Create thousands of virtual threads
for (int i = 0; i < 10000; i++) {
    executor.submit(() -> doWork());
}
```

**Bad** - Fixed thread pool for virtual threads:
```java
// DON'T limit virtual threads with fixed pool
ExecutorService executor = Executors.newFixedThreadPool(10);  // BAD for VT!
```

### 3. Avoid CPU-Intensive Work in Virtual Threads

**Good** - Use virtual threads for I/O, platform threads for CPU:
```java
// Virtual thread for I/O
executor.submit(() -> {
    data = fetchFromDatabase();  // Blocking I/O
    processData(data);  // Light processing
});

// Platform thread pool for CPU-intensive work
ExecutorService cpuPool = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
);
cpuPool.submit(() -> {
    complexCalculation();  // CPU-intensive
});
```

### 4. Monitor Virtual Thread Count

```java
// Log virtual thread metrics
logger.info("Active virtual threads: {}", Thread.getAllStackTraces().size());

// JMX monitoring
ManagementFactory.getThreadMXBean().getThreadCount();
```

### 5. Handle Interruption Properly

```java
private void workerLoop() {
    while (active) {
        try {
            doWork();
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Restore interrupt flag
            Thread.currentThread().interrupt();
            active = false;
            logger.info("Worker interrupted");
        }
    }
}
```

## Testing Plugins

### Unit Tests

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class MySensorVertexTest {
    private WaldotGraph graph;
    private MySensorVertex sensor;
    
    @BeforeEach
    void setUp() {
        graph = OpcFactory.getOpcGraph("memory://test");
        sensor = (MySensorVertex) graph.addVertex(
            "type", "my-sensor",
            "label", "test-sensor"
        );
    }
    
    @Test
    void testSensorUpdates() throws Exception {
        // Wait for sensor to update
        Thread.sleep(6000);
        
        // Verify properties are set
        assertNotNull(sensor.property("temperature").value());
        assertNotNull(sensor.property("humidity").value());
        
        // Verify values in range
        double temp = (Double) sensor.property("temperature").value();
        assertTrue(temp >= 15.0 && temp <= 35.0);
    }
    
    @Test
    void testPropertySync() {
        // Set property manually
        sensor.property("temperature", 25.5);
        
        // Verify property is readable
        assertEquals(25.5, sensor.property("temperature").value());
    }
}
```

### Integration Tests

```java
@Test
void testOpcUaSynchronization() throws Exception {
    // Create vertex via graph
    Vertex vertex = graph.addVertex("type", "my-sensor", "label", "opc-test");
    
    // Connect OPC UA client
    OpcUaClient client = OpcUaClient.create(opcServerUrl);
    client.connect().get();
    
    // Browse to vertex in OPC UA address space
    UaNode node = client.getAddressSpace()
        .getNode(new NodeId(waldotNamespace, "opc-test"));
    assertNotNull(node);
    
    // Read property via OPC UA
    UaVariable tempVar = (UaVariable) node.getComponent("temperature");
    DataValue value = tempVar.readValue();
    
    // Verify sync with graph
    assertEquals(
        vertex.property("temperature").value(),
        value.getValue().getValue()
    );
    
    // Write property via OPC UA
    tempVar.writeValue(new DataValue(new Variant(30.0)));
    
    // Verify sync to graph
    Thread.sleep(100);  // Allow sync time
    assertEquals(30.0, vertex.property("temperature").value());
}
```

### Load Tests

```java
@Test
void testManyGenerators() throws Exception {
    // Create 1000 generators
    List<Vertex> generators = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
        generators.add(graph.addVertex(
            "type", "generator",
            "label", "gen-" + i,
            "Delay", "1000"
        ));
    }
    
    // Wait for all to update
    Thread.sleep(2000);
    
    // Verify all are generating
    for (Vertex gen : generators) {
        assertNotNull(gen.property("data").value());
    }
    
    // Check memory usage
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    assertTrue(usedMemory < 100 * 1024 * 1024);  // < 100MB
}
```

## Deployment

### Building Plugin

```bash
./gradlew :plugins:my-plugin:build
```

### Installing Plugin

1. **Embedded in main application**:
```gradle
// waldot-app/build.gradle
dependencies {
    implementation project(':plugins:my-plugin')
}
```

2. **Standalone JAR**:
```bash
# Build fat JAR
./gradlew :plugins:my-plugin:shadowJar

# Copy to plugins directory
cp plugins/my-plugin/build/libs/my-plugin-all.jar /opt/waldot/plugins/
```

3. **Auto-discovery**:
Plugins annotated with `@WaldotPlugin` are automatically discovered at runtime via classpath scanning.

### Docker Deployment

```dockerfile
FROM eclipse-temurin:21-jre

# Copy main application
COPY waldot-app/build/libs/waldot-app.jar /app/waldot.jar

# Copy plugin
COPY plugins/my-plugin/build/libs/my-plugin.jar /app/plugins/

# Run
CMD ["java", "-jar", "/app/waldot.jar"]
```

## Troubleshooting

### Plugin Not Loading

**Check classpath**:
```bash
java -cp waldot-app.jar:plugins/* -verbose:class net.rossonet.waldot.Main
```

**Verify annotation**:
```java
@WaldotPlugin  // Must be present!
public class MyPlugin implements PluginListener {
```

### Virtual Thread Issues

**Too many threads**:
```java
// Monitor thread count
jcmd <pid> Thread.print | grep "virtual" | wc -l
```

**Thread leaks**:
```java
// Ensure proper cleanup
@Override
public void close() throws Exception {
    active = false;  // Stop loops
    executor.shutdownNow();  // Terminate executor
}
```

### OPC UA Sync Issues

**Property not syncing**:
```java
// Use property() method, not direct field access
property("temperature", value);  // Correct - syncs to OPC UA

this.temperature = value;  // Wrong - doesn't sync
```

**Type mismatch**:
```java
// Ensure OPC UA type matches Java type
addParameterToTypeNode(namespace, typeNode, "count", NodeIds.Int64);
property("count", 42L);  // Use Long, not Integer
```

## Conclusion

WaldOT's plugin architecture combines the power of OPC UA industrial standards with TinkerPop graph capabilities, all leveraging Java 21+ virtual threads for unprecedented scalability and simplicity.

### Key Takeaways

1. **Plugins extend WaldOT** by registering custom vertex types in both OPC UA and TinkerPop
2. **Virtual threads enable massive concurrency** - create thousands of active objects without performance issues
3. **Bi-directional sync** happens automatically between OPC UA and graph
4. **Integration is simple** - use blocking I/O naturally in virtual threads
5. **Real-world examples** (generator, rules-engine, tinkerpop) demonstrate production patterns

### Next Steps

- Study the example plugins in `plugins/` directory
- Read the [WaldOT Architecture](../../README.md) documentation
- Explore the [waldot-api](../../waldot-api/) Javadoc
- Join the community at [github.com/rossonet/waldot](https://github.com/rossonet/waldot)

### Resources

- [Apache TinkerPop Documentation](https://tinkerpop.apache.org/docs/current/dev/provider/)
- [Eclipse Milo GitHub](https://github.com/eclipse/milo)
- [Java Virtual Threads](https://openjdk.org/jeps/444)
- [OPC UA Specification](https://reference.opcfoundation.org/)

---

*WaldOT Plugin Development Guide - Version 0.6.1*
*Andrea Ambrosini - Rossonet s.c.a.r.l.*
