# WaldOT Architecture Overview

**Strategic Technical Guide for Decision Makers and Architects**

## Executive Summary

WaldOT is a Digital Twin Engine that bridges the gap between Operational Technology (OT) and Information Technology (IT) by creating a bidirectional, real-time synchronization between OPC UA industrial systems and Apache TinkerPop graph databases.

### Business Value Proposition

- **Unified Data Model**: Single source of truth for industrial and analytical data
- **Real-Time Insights**: Complex graph queries on live OPC UA data without ETL pipelines
- **Edge Computing**: Low-latency processing directly on industrial devices
- **Reduced Integration Costs**: Standard protocols (OPC UA + Gremlin) eliminate custom integrations
- **Scalability**: From single devices to distributed enterprise deployments
- **Future-Proof**: Built on open standards (OPC UA, Apache TinkerPop, Java 21)

---

## Architecture Layers

### Layer 1: OPC UA Server (Industrial Interface)

**Technology**: Eclipse Milo OPC UA SDK

**Purpose**: Expose industrial data using the OPC UA standard protocol

**Capabilities**:

- Full OPC UA server implementation (binary protocol, security, subscriptions)
- Standard OPC UA address space with Objects, Variables, References
- Event system for real-time notifications
- Security: X.509 certificates, user authentication, encryption
- Compatible with all OPC UA clients (UaExpert, Prosys, custom applications)

**Deployment Scenarios**:

- Standalone OPC UA server for legacy system integration
- Gateway between proprietary protocols and OPC UA
- Edge device for local data aggregation

---

### Layer 2: TinkerPop Graph Database (Analytical Engine)

**Technology**: Apache TinkerPop 3.x

**Purpose**: Represent industrial data as a queryable graph structure

**Capabilities**:

- Vertices (nodes) represent industrial objects (machines, sensors, zones)
- Edges (relationships) represent connections (contains, monitors, controls)
- Properties store dynamic data (temperature, pressure, status)
- Gremlin query language for complex traversals
- Graph algorithms (PageRank, shortest path, community detection)
- Multiple storage backends (in-memory, RocksDB, Neo4j, JanusGraph)

**Use Cases**:

- Correlation analysis across distant equipment
- Root cause analysis with graph traversal
- Asset relationship mapping
- Predictive maintenance pattern detection

---

### Layer 3: Bidirectional Synchronization (Core Innovation)

**Purpose**: Keep OPC UA and TinkerPop in perfect sync

**Synchronization Flow**:

```
OPC UA Client → Write Variable → OPC UA Server
                                      ↓
                                 WaldOT Sync
                                      ↓
                              TinkerPop Graph
                                      ↓
                              Gremlin Listeners
                                      ↓
                              Rules Engine / Analytics

Gremlin Query → Modify Graph → WaldOT Sync
                                      ↓
                              OPC UA Server
                                      ↓
                              OPC UA Clients (notified)
```

**Key Features**:

- **Real-time**: Changes propagate in milliseconds
- **Bidirectional**: Modifications from either side sync to the other
- **Consistent**: ACID properties maintained where supported by storage backend
- **Event-driven**: Observers and listeners for reactive programming

---

### Layer 4: Plugin System (Extensibility)

**Technology**: Java 21 with @WaldotPlugin annotation-based discovery

**Purpose**: Extend WaldOT with domain-specific functionality

**Architecture**:

```
┌─────────────────────────────────────────────────────┐
│              Plugin Manager                          │
│  • Auto-discovery (@WaldotPlugin)                   │
│  • Lifecycle management (init → start → stop)       │
│  • Type registration in OPC UA namespace            │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│              Available Plugins                       │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │  Generator   │  │ Rules Engine │  │ TinkerPop │ │
│  │  Plugin      │  │   Plugin     │  │  Server   │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
│  ┌──────────────┐  ┌──────────────┐                │
│  │   Custom     │  │   Custom     │                │
│  │  Protocol    │  │  Analytics   │                │
│  └──────────────┘  └──────────────┘                │
└─────────────────────────────────────────────────────┘
```

**Plugin Capabilities**:

- Register custom vertex types (e.g., "PLC", "Sensor", "Actuator")
- Implement custom behaviors (polling, event processing, calculations)
- Integrate external systems (MQTT, Modbus, REST APIs, databases)
- Provide reusable commands accessible via OPC UA methods

**Development Effort**:

- Simple plugin: 1-2 days
- Complex integration: 1-2 weeks
- Full documentation and examples provided

---

## Core Plugins

### 1. Generator Plugin

**Purpose**: Simulate dynamic data for testing and development

**Features**:

- 6 algorithms: incremental, decremental, random, sinusoidal, triangular, stopped
- Configurable update intervals (10ms to hours)
- Virtual threads for thousands of concurrent generators
- Perfect for testing without physical hardware

**Use Cases**:

- Development and testing
- Training and demonstrations
- Load testing
- Rule engine validation

**Performance**: 100,000+ generators on standard hardware

---

### 2. Rules Engine Plugin

**Purpose**: Event-driven IF-THEN-THAT automation

**Features**:

- JEXL expressions for conditions and actions
- Priority-based event queuing
- Hysteresis deduplication (prevent event flooding)
- Full graph access in rules (Gremlin queries)
- Virtual thread execution for concurrency
- Debug events for troubleshooting

**Architecture**:

```
[Source Node] → [FireMonitoredEdge] → [RuleVertex] → [ComputeMonitoredEdge] → [ComputeVertex]
   (event)         (filters)          (condition)          (routes)            (executes action)
```

**Use Cases**:

- Real-time alerting (temperature > threshold)
- Complex condition monitoring (multiple sensors)
- Automated responses (shutdown on critical state)
- Data validation and quality checks
- State machine implementation

**Performance**: Thousands of rules, sub-millisecond latency for simple conditions

---

### 3. TinkerPop Server Plugin

**Purpose**: Enable remote Gremlin client access

**Features**:

- Embedded Gremlin Server as graph vertices
- WebSocket protocol (port 8182 by default)
- GraphSON v3 and GraphBinary v1 serializers
- Compatible with standard TinkerPop clients
- Graph visualization tools support (Graph-Explorer, Gephi)

**Use Cases**:

- Remote graph queries from applications
- Integration with BI tools
- Graph visualization
- Multi-user access to WaldOT graph

---

## Concurrency Model: Virtual Threads

**Technology**: Java 21 Project Loom Virtual Threads

**Why Virtual Threads?**

Traditional platform threads:

- ~1 MB stack size per thread
- OS-managed scheduling
- Limited to thousands of threads
- Blocking I/O blocks OS thread

Virtual threads:

- ~1 KB stack size per thread
- JVM-managed scheduling
- Millions of threads possible
- Blocking I/O doesn't block OS thread

**Impact on WaldOT**:

```
Scenario: 10,000 sensors polling every second

Platform Threads:
- 10,000 threads × 1 MB = ~10 GB RAM
- Context switching overhead
- Not feasible

Virtual Threads:
- 10,000 threads × 1 KB = ~10 MB RAM
- Minimal overhead
- Easily achievable
```

**Design Pattern**:

```java
// Each active object (generator, monitor, rule) runs in its own virtual thread
ExecutorService executor = ThreadHelper.newVirtualThreadExecutor();

// Create thousands of concurrent tasks
for (int i = 0; i < 100000; i++) {
    executor.submit(() -> {
        while (active) {
            // Blocking operations are OK in virtual threads
            data = fetchSensorData();  // Blocking I/O
            updateGraph(data);
            Thread.sleep(1000);  // Blocking sleep
        }
    });
}
```

**Benefits**:

- Simple, readable code (no async/await complexity)
- Massive concurrency with minimal resources
- Perfect for I/O-bound workloads (sensors, APIs, databases)

---

## Deployment Architectures

### Architecture 1: Edge Device

**Scenario**: Single industrial device (RTU, PLC, edge gateway)

```
┌─────────────────────────────────────┐
│        Edge Device                   │
│  ┌───────────────────────────────┐  │
│  │  WaldOT Container             │  │
│  │  • OPC UA Server              │  │
│  │  • Local graph (RocksDB)      │  │
│  │  • Rules engine               │  │
│  │  • Data generators            │  │
│  └───────────────────────────────┘  │
│                                      │
│  Hardware: 1GB RAM, 2 CPU cores     │
└─────────────────────────────────────┘
         ↓ (optional)
    Cloud/SCADA
```

**Characteristics**:

- Low latency (<1ms for local rules)
- Offline capable
- Minimal bandwidth usage
- Local data processing

**Use Cases**:

- Remote sites with limited connectivity
- Real-time control loops
- Edge analytics
- Data aggregation before cloud upload

---

### Architecture 2: Gateway/Aggregator

**Scenario**: Multiple devices aggregated through WaldOT gateway

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   PLC 1     │  │   PLC 2     │  │  Sensor N   │
│ (OPC UA)    │  │ (Modbus)    │  │  (MQTT)     │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │
       └────────────────┼────────────────┘
                        ↓
         ┌──────────────────────────────┐
         │   WaldOT Gateway             │
         │  • Protocol bridges          │
         │  • Data normalization        │
         │  • Graph aggregation         │
         │  • Rules engine              │
         └──────────────┬───────────────┘
                        ↓
                  SCADA / Cloud
```

**Characteristics**:

- Protocol translation (Modbus, MQTT, OPC UA)
- Data normalization
- Local buffering
- Bandwidth optimization

**Use Cases**:

- Legacy system integration
- Multi-protocol environments
- Data preprocessing
- Local analytics before cloud

---

### Architecture 3: Distributed Enterprise

**Scenario**: Multiple WaldOT instances with centralized graph database

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  WaldOT     │  │  WaldOT     │  │  WaldOT     │
│  Edge 1     │  │  Edge 2     │  │  Edge N     │
│ (local DB)  │  │ (local DB)  │  │ (local DB)  │
└──────┬──────┘  └──────┬──────┘  └──────┬──────┘
       │                │                │
       └────────────────┼────────────────┘
                        ↓
         ┌──────────────────────────────┐
         │   Central TinkerPop Graph    │
         │   (Neo4j / JanusGraph)       │
         │  • Enterprise-wide view      │
         │  • Cross-site analytics      │
         │  • Historical data           │
         └──────────────────────────────┘
```

**Characteristics**:

- Distributed data collection
- Centralized analytics
- Scalable to thousands of devices
- Enterprise-wide visibility

**Use Cases**:

- Multi-site manufacturing
- Fleet management
- Enterprise asset management
- Centralized monitoring

---

## Integration Scenarios

### Scenario 1: OPC UA to Cloud

**Problem**: Legacy OPC UA devices need cloud connectivity

**Solution**: WaldOT as OPC UA client + cloud bridge

```
[OPC UA Devices] → [WaldOT OPC UA Client] → [Graph Processing] → [Cloud API]
```

**Benefits**:

- No changes to existing OPC UA infrastructure
- Data transformation and filtering at edge
- Bandwidth optimization
- Offline buffering

---

### Scenario 2: Graph Analytics on Industrial Data

**Problem**: Need complex queries on industrial data (e.g., "find all motors connected to Line 1 in alarm state")

**Solution**: WaldOT exposes OPC UA as queryable graph

```
[OPC UA Data] → [WaldOT Graph] → [Gremlin Queries] → [Analytics / BI Tools]
```

**Example Query**:

```groovy
// Find all motors in alarm state connected to Line 1
g.V().has('name', 'Line1')
  .out('contains')
  .has('type', 'motor')
  .has('status', 'alarm')
  .values('name')
```

**Benefits**:

- No ETL pipeline required
- Real-time queries on live data
- Complex relationship traversal
- Graph algorithms (PageRank, community detection)

---

### Scenario 3: Event-Driven Automation

**Problem**: Need automated responses to complex conditions

**Solution**: WaldOT Rules Engine

```
[Sensors] → [WaldOT Graph] → [Rules Engine] → [Automated Actions]
```

**Example Rule**:

```groovy
// IF temperature > 80 AND pressure > 100 THEN shutdown
rule = graph.addVertex(
    "type", "rule",
    "Condition", "temperature > 80.0 && pressure > 100.0",
    "Action", "commands.execute('shutdown'); log.error('CRITICAL')"
)
```

**Benefits**:

- Low latency (edge processing)
- Complex multi-sensor conditions
- Full graph access in rules
- Priority-based execution

---

## Performance Characteristics

### Scalability

| Metric         | Value | Notes                                        |
| -------------- | ----- | -------------------------------------------- |
| Vertices       | 1M+   | Depends on storage backend                   |
| Edges          | 10M+  | Depends on storage backend                   |
| Generators     | 100K+ | Limited by CPU, not memory (virtual threads) |
| Rules          | 10K+  | Concurrent execution with virtual threads    |
| OPC UA Clients | 100+  | Standard Eclipse Milo limits                 |
| Events/sec     | 10K+  | Depends on rule complexity                   |

### Latency

| Operation                        | Latency | Notes                 |
| -------------------------------- | ------- | --------------------- |
| Property update (OPC UA → Graph) | <1ms    | In-memory graph       |
| Property update (Graph → OPC UA) | <1ms    | In-memory graph       |
| Simple rule execution            | <1ms    | Boolean condition     |
| Complex rule (Gremlin query)     | 1-10ms  | Depends on query      |
| Graph traversal (local)          | <10ms   | Depends on complexity |

### Resource Requirements

| Deployment                 | RAM   | CPU      | Storage |
| -------------------------- | ----- | -------- | ------- |
| Minimal (edge)             | 512MB | 1 core   | 100MB   |
| Standard                   | 2GB   | 2 cores  | 1GB     |
| Large (10K vertices)       | 4GB   | 4 cores  | 10GB    |
| Enterprise (100K vertices) | 16GB+ | 8+ cores | 100GB+  |

---

## Security Considerations

### OPC UA Security

- **Encryption**: TLS 1.2+ for all OPC UA connections
- **Authentication**: X.509 certificates, username/password
- **Authorization**: Role-based access control (RBAC)
- **PKI**: Full Public Key Infrastructure support
- **Security Policies**: Sign, SignAndEncrypt, None (configurable)

### Application Security

- **Default Credentials**: MUST be changed in production (`WALDOT_FACTORY_PASSWORD`)
- **Anonymous Access**: Disable in production (`WALDOT_ANONYMOUS_ACCESS=false`)
- **Exec Command**: Disable in production (`WALDOT_EXEC_COMMAND_EXECUTABLE=false`)
- **Network Isolation**: Bind to specific interfaces, use firewalls
- **Secrets Management**: Use Docker secrets, Kubernetes secrets, or vault

### Compliance

- **IEC 62443**: Industrial security standards compatible
- **GDPR**: No personal data collected by default
- **Audit Logging**: All operations logged via SLF4J

---

## Technology Comparison

### WaldOT vs. Traditional SCADA

| Feature        | WaldOT              | Traditional SCADA           |
| -------------- | ------------------- | --------------------------- |
| Data Model     | Graph (flexible)    | Hierarchical (rigid)        |
| Query Language | Gremlin (powerful)  | SQL (limited relationships) |
| Real-time      | Yes                 | Yes                         |
| Edge Computing | Yes                 | Limited                     |
| Extensibility  | Plugin architecture | Vendor-specific             |
| Standards      | OPC UA, TinkerPop   | Proprietary                 |
| Cost           | Open-source         | Expensive licenses          |

### WaldOT vs. Time-Series Databases

| Feature                  | WaldOT            | Time-Series DB    |
| ------------------------ | ----------------- | ----------------- |
| Relationships            | Native (graph)    | Limited (tags)    |
| Complex Queries          | Gremlin traversal | Aggregations only |
| Real-time Updates        | Bidirectional     | Write-only        |
| OPC UA Integration       | Native            | Requires gateway  |
| Graph Algorithms         | Yes               | No                |
| Time-series Optimization | Via plugins       | Native            |

### WaldOT vs. Pure Graph Databases

| Feature              | WaldOT      | Neo4j / JanusGraph   |
| -------------------- | ----------- | -------------------- |
| OPC UA Integration   | Native      | Requires custom code |
| Industrial Protocols | Via plugins | Not supported        |
| Edge Deployment      | Optimized   | Heavy                |
| Rules Engine         | Built-in    | External             |
| Data Simulation      | Built-in    | External             |
| Learning Curve       | Moderate    | Steep                |

---

## Migration Strategies

### From Existing OPC UA Infrastructure

**Phase 1: Parallel Deployment**

- Deploy WaldOT as OPC UA client
- Mirror existing OPC UA data to graph
- Validate data consistency
- No changes to existing systems

**Phase 2: Analytics Integration**

- Develop Gremlin queries for analytics
- Integrate with BI tools
- Train users on graph queries

**Phase 3: Gradual Migration**

- Migrate non-critical systems to WaldOT
- Implement rules engine for automation
- Decommission legacy systems incrementally

### From SQL Databases

**Phase 1: Data Modeling**

- Map SQL tables to graph vertices
- Map foreign keys to graph edges
- Identify relationship patterns

**Phase 2: ETL Pipeline**

- Extract data from SQL
- Transform to graph structure
- Load into WaldOT

**Phase 3: Live Sync**

- Implement change data capture (CDC)
- Sync SQL changes to graph
- Validate consistency

---

## Roadmap and Future Enhancements

### Planned Features

- **Additional Storage Backends**: Cassandra, ArangoDB
- **Enhanced Security**: OAuth2, SAML integration
- **Cloud Connectors**: AWS IoT, Azure IoT Hub, Google Cloud IoT
- **Protocol Bridges**: Modbus, BACnet, MQTT native support
- **Graph Visualization**: Built-in web UI for graph exploration
- **Machine Learning Integration**: TensorFlow, PyTorch model deployment
- **Distributed Computing**: Apache Spark integration for large-scale analytics

### Community Contributions Welcome

- Protocol adapters
- Storage backend implementations
- Analytics plugins
- Visualization tools
- Documentation improvements

---

## Decision Framework

### When to Use WaldOT

✅ **Good Fit**:

- Complex relationship queries needed
- Multiple data sources to integrate
- Edge computing requirements
- Real-time analytics on industrial data
- Event-driven automation
- Flexible, evolving data models

❌ **Not Ideal**:

- Pure time-series data (use InfluxDB, TimescaleDB)
- Simple SCADA (use traditional SCADA)
- No relationship complexity (use SQL database)
- Extremely high throughput (>100K events/sec sustained)

### Evaluation Checklist

- [ ] Do you need complex relationship queries?
- [ ] Do you have multiple industrial protocols to integrate?
- [ ] Do you need edge computing capabilities?
- [ ] Do you need event-driven automation?
- [ ] Do you have Java 21+ runtime available?
- [ ] Do you have OPC UA infrastructure or plan to adopt it?
- [ ] Do you need flexible, evolving data models?

If you answered "yes" to 3+ questions, WaldOT is likely a good fit.

---

## Getting Started

### Proof of Concept (1 week)

**Day 1-2**: Installation and basic setup

- Deploy WaldOT Docker container
- Connect OPC UA client (UaExpert)
- Create first graph vertices

**Day 3-4**: Data simulation and rules

- Configure generator plugin
- Create simple rules
- Test event-driven automation

**Day 5**: Integration testing

- Connect to existing OPC UA devices
- Develop Gremlin queries
- Evaluate performance

**Day 6-7**: Documentation and presentation

- Document findings
- Prepare architecture proposal
- Present to stakeholders

### Pilot Project (1 month)

**Week 1**: Infrastructure setup

- Production-grade deployment
- Security configuration
- Monitoring and logging

**Week 2-3**: Integration

- Connect to production OPC UA devices
- Develop custom plugins if needed
- Implement rules engine logic

**Week 4**: Validation and handover

- Performance testing
- User training
- Documentation

---

## Conclusion

WaldOT represents a paradigm shift in industrial data management by unifying OPC UA and graph databases. Its plugin architecture, virtual thread concurrency, and edge-first design make it ideal for modern Industrial IoT deployments.

**Key Takeaways**:

- Unified OT/IT data model reduces integration complexity
- Graph queries enable insights impossible with traditional systems
- Edge computing capabilities reduce latency and bandwidth
- Open standards ensure future-proof architecture
- Plugin system provides unlimited extensibility

**Next Steps**:

1. Review [Developer Guide](DEVELOPER_GUIDE.md) for hands-on development
2. Explore [Examples](../../examples/) for real-world use cases
3. Join the community on [GitHub](https://github.com/rossonet/waldot)

---

**Questions? Contact**: [Rossonet s.c.a r.l.](https://www.rossonet.net)
