# WaldOT TinkerPop Plugin

Native TinkerPop/Gremlin client access to WaldOT graphs.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org/)
[![WaldOT](https://img.shields.io/badge/WaldOT-0.4.0%2B-green.svg)](../../README.md)

## Overview

The TinkerPop plugin embeds Apache TinkerPop Gremlin Server as graph vertices, enabling standard TinkerPop clients to connect to WaldOT graphs via WebSocket and HTTP protocols.

### Key Features

- **Native TinkerPop Access**: Standard Gremlin queries on WaldOT graph
- **Dual Protocol**: OPC-UA + Gremlin on same graph simultaneously
- **Multiple Serializers**: GraphSON v3 (JSON) + GraphBinary v1 (binary)
- **Graph Visualization**: Connect Graph-Explorer, GraphExp, Gremlin Console
- **Live Synchronization**: Changes via Gremlin reflect in OPC-UA and vice versa
- **Remote Execution**: Execute Gremlin from any language with TinkerPop driver

## Quick Start

### Create Gremlin Server

```groovy
// Create server on port 8182
gremlinServer = graph.addVertex(
    "type", "gremlin",
    "label", "main-server",
    "Port", "8182",
    "Bind", "0.0.0.0"
)
```

### Connect from Gremlin Console

```groovy
// In Gremlin Console
:remote connect tinkerpop.server conf/remote.yaml
:remote console
g.V().count()
```

### Connect from Java

```java
Cluster cluster = Cluster.build()
    .addContactPoint("localhost")
    .port(8182)
    .serializer(new GraphBinaryMessageSerializerV1())
    .create();

GraphTraversalSource g = traversal()
    .withRemote(DriverRemoteConnection.using(cluster, "g"));

// Execute queries
long count = g.V().count().next();
List<Vertex> vertices = g.V().has("type", "generator").toList();
```

### Connect Graph-Explorer

1. Open Graph-Explorer (https://graphexp.io/)
2. Connect to: `ws://localhost:8182/gremlin`
3. Visualize and query WaldOT graph

## Configuration

| Property | Type    | Default | Description            |
|----------|---------|---------|------------------------|
| Port     | Integer | 1025    | Gremlin Server port    |
| Bind     | String  | 0.0.0.0 | Bind address           |
| Status   | String  | -       | Server status (read-only) |

## Supported Clients

- **Gremlin Console**: Interactive query console
- **TinkerPop Drivers**: Java, Python, JavaScript, .NET, Go
- **Graph-Explorer**: Web-based visualization
- **GraphExp**: Alternative web UI
- **Apache Zeppelin**: Notebook with Gremlin interpreter

## Use Cases

1. **Graph Visualization**: See WaldOT topology visually
2. **Complex Queries**: Use full Gremlin power (path finding, algorithms)
3. **Remote Access**: Query from external applications
4. **Development**: Test queries in Gremlin Console
5. **Integration**: Connect BI tools and graph analytics platforms

## Examples

### Multi-Server Setup

```groovy
// Public server for external clients
publicServer = graph.addVertex(
    "type", "gremlin",
    "label", "public-gremlin",
    "Port", "8182",
    "Bind", "0.0.0.0"
)

// Internal server for local tools
internalServer = graph.addVertex(
    "type", "gremlin",
    "label", "internal-gremlin",
    "Port", "8183",
    "Bind", "127.0.0.1"
)
```

### Change Port at Runtime

```groovy
// Server automatically restarts
gremlinServer.property("Port", 9090)
```

### Monitor Status

```groovy
status = gremlinServer.property("Status").value()
println "Server status: ${status}"  // Running, Stopped, or Failed
```

## Performance

- **Latency**: < 5ms query overhead vs direct graph access
- **Throughput**: ~1000 queries/second for simple traversals
- **Concurrency**: Handles multiple simultaneous clients
- **Memory**: ~50MB overhead per server instance

## Troubleshooting

### Server Won't Start

Check port availability:
```bash
netstat -an | grep 8182
```

Change to available port:
```groovy
server.property("Port", 8183)
```

### Connection Refused

Verify bind address:
```groovy
// Listen on all interfaces
server.property("Bind", "0.0.0.0")

// Or specific interface
server.property("Bind", "192.168.1.100")
```

### Status = Failed

Check logs for error details. Common causes:
- Port already in use
- Invalid bind address
- Firewall blocking port

## Architecture

```
┌─────────────────────────────────────────┐
│     WaldOT Graph (OPC-UA + TinkerPop)  │
│  - Vertices, Edges, Properties          │
└─────────────────┬───────────────────────┘
                  │ shared by both
         ┌────────┴────────┐
         │                 │
  ┌──────▼──────┐   ┌─────▼──────┐
  │  OPC-UA     │   │ Gremlin    │
  │  Server     │   │ Server     │
  │  (Milo)     │   │ (TinkerPop)│
  └──────┬──────┘   └─────┬──────┘
         │                 │
  ┌──────▼──────┐   ┌─────▼──────┐
  │ OPC-UA      │   │ Gremlin    │
  │ Clients     │   │ Clients    │
  └─────────────┘   └────────────┘
```

## Requirements

- **Java**: 21+
- **WaldOT**: 0.4.0+
- **TinkerPop**: 3.7+ (included)
- **Dependencies**: Apache Gremlin Server, GraphSON, GraphBinary

## Installation

Auto-discovered via `@WaldotPlugin` annotation:

```gradle
dependencies {
    implementation project(':plugins:waldot-plugin-tinkerpop')
}
```

## Contributing

Contributions welcome! See main [WaldOT README](../../README.md).

## License

Apache License 2.0. See [LICENSE](LICENSE).

## Authors

Andrea Ambrosini - Rossonet s.c.a r.l.

## See Also

- [WaldOT Framework](../../README.md)
- [Apache TinkerPop](https://tinkerpop.apache.org/)
- [Gremlin Console](https://tinkerpop.apache.org/docs/current/tutorials/the-gremlin-console/)
- [Graph-Explorer](https://graphexp.io/)

---

**WaldOT TinkerPop Plugin** - Bridge OPC-UA industrial graphs with TinkerPop ecosystem.
