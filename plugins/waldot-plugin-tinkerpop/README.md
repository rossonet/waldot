# WaldOT TinkerPop Plugin

Native TinkerPop/Gremlin client access to WaldOT graphs.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25%2B-orange.svg)](https://openjdk.org/)
[![WaldOT](https://img.shields.io/badge/WaldOT-0.4.0%2B-green.svg)](../../README.md)

## Overview

The TinkerPop plugin embeds Apache TinkerPop Gremlin Server as graph vertices, enabling standard TinkerPop clients to connect to WaldOT graphs via WebSocket and HTTP protocols.

### Key Features

- **Native TinkerPop Access**: Standard Gremlin queries on WaldOT graph
- **Dual Protocol**: OPC-UA + Gremlin on same graph simultaneously
- **Multiple Serializers**: GraphSON v3 (JSON) + GraphBinary v1 (binary, without elements, see Limitations)
- **Graph Visualization**: Connect Graph-Explorer, GraphExp, Gremlin Console
- **Live Synchronization**: Changes via Gremlin reflect in OPC-UA and vice versa
- **Remote Execution**: Execute Gremlin from any language with TinkerPop driver

## Quick Start

### Create Gremlin Server

```groovy
// Create server on port 8182 (default: 1025), reachable only from localhost
gremlinServer = graph.addVertex(
    "type", "gremlin",
    "label", "main-server",
    "Port", "8182",
    "Bind", "127.0.0.1"
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
// the WaldOT ids are OPC UA NodeIds: to receive vertices and edges use GraphSON v3
// with the WaldOT IoRegistry (counts and plain values work with any serializer)
Cluster cluster = Cluster.build()
    .addContactPoint("localhost")
    .port(8182)
    .serializer(new GraphSONMessageSerializerV3(
        GraphSONMapper.build().addRegistry(OpcIoRegistryV3.instance())))
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
| Port     | Integer | 1025    | Gremlin Server port (1-65535); a change restarts the server |
| Bind     | String  | 0.0.0.0 | Bind address; a change restarts the server |
| status   | String  | -       | Server status: Running, Stopped or Failed (written by the vertex) |

The vertex can also be created with the OPC UA command `tinkerpop/create gremlin server`
(parameters `Port` and `Bind`).

## Security

The embedded server has **no authentication and no TLS**, and it accepts Gremlin-Groovy
scripts: whoever reaches the port can run arbitrary code in the WaldOT JVM and change the
whole graph. Outside trusted networks set `Bind` to `127.0.0.1` (or to an interface reachable
only by trusted hosts).

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
// The server restarts on the new port (on a separate thread: the change
// can also come from a client of the server itself)
gremlinServer.property("Port", 9090)
```

### Monitor Status

```groovy
status = gremlinServer.property("status").value()
println "Server status: ${status}"  // Running, Stopped, or Failed
```

## Lifecycle

- The server starts when the vertex is created; the creation waits for the bind of the port.
  If the bind fails (port in use, invalid address) the server is released and `status` is `Failed`.
- Removing the vertex stops the server and waits for the release of the port. The stop
  never closes the WaldOT graph: the graph belongs to the namespace (`WaldotGraphManager`
  detaches it before the stop of the server).
- Closing the namespace stops every Gremlin Server.

## Limitations

- GraphBinary v1 is available, but the OPC UA `NodeId` (the id of every vertex and edge) has
  no GraphBinary serializer registered yet: with GraphBinary only results without elements
  (counts, values, maps of values) can be returned. GraphSON v3 serializes `NodeId`.
- The WaldOT graph has no transactions and no `GraphComputer` (OLAP traversals are not supported).
- Several servers in the same JVM share the static parts of Gremlin Server (op processors,
  metrics): stopping one of them also closes the sessions of the others.

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

- **Java**: 25+
- **WaldOT**: 0.4.0+
- **TinkerPop**: 3.8.2 (included)
- **Dependencies**: Apache Gremlin Server, GraphSON, GraphBinary

## Installation

Auto-discovered via `@WaldotPlugin` annotation:

```gradle
dependencies {
    implementation project(':waldot-plugin-tinkerpop')
}
```

## Contributing

Contributions welcome! See main [WaldOT README](../../README.md).

## License

Apache License 2.0. See [LICENSE](LICENSE).

## Authors

Andrea Ambrosini - Zona Autonoma srl

## See Also

- [WaldOT Framework](../../README.md)
- [Apache TinkerPop](https://tinkerpop.apache.org/)
- [Gremlin Console](https://tinkerpop.apache.org/docs/current/tutorials/the-gremlin-console/)
- [Graph-Explorer](https://graphexp.io/)

---

**WaldOT TinkerPop Plugin** - Bridge OPC-UA industrial graphs with TinkerPop ecosystem.
