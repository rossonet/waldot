# WaldOT Documentation Hub

**Digital Twin Engine bridging OPC UA and Apache TinkerPop Graphs**

## Documentation by Audience

WaldOT documentation is organized by user role to provide targeted, relevant information:

### 🎯 For Decision Makers & Architects

**[Architecture Overview](ARCHITECTURE_OVERVIEW.md)** - Strategic view of WaldOT capabilities

- Business value proposition
- Technical architecture and design patterns
- Integration scenarios and use cases
- Scalability and performance characteristics
- Security and compliance considerations

**Target audience**: CTOs, Solution Architects, Technical Leads, Project Managers

---

### 👨‍💻 For Developers

**[Developer Guide](DEVELOPER_GUIDE.md)** - Hands-on development with WaldOT

- Quick start and setup
- Core concepts and APIs
- Plugin development tutorial
- Code examples and patterns
- Testing and debugging
- Best practices

**[Integration Guide](INTEGRATION_GUIDE.md)** - How to write a WaldOT plugin (verified step-by-step example)

- Plugin architecture deep dive
- Virtual threads and concurrency
- OPC UA type registration
- Real-world plugin examples

**Target audience**: Software Engineers, DevOps Engineers, Integration Specialists

---

### 👤 For End Users & Operators

**[User Guide](USER_GUIDE.md)** - Operating manual

- Start the server, OPC UA address space
- Vertices, active edges (`link-to`, `link-from`, `fire`), plugin types
- Bootstrap file, commands, Gremlin Server
- Rules engine, monitoring, operations, troubleshooting

**[Examples](examples/README.md)** - Step-by-step pages verified on the Docker image

- Plain vertices, active edges, generators and rules, complete bootstrap
- Showcases: Dungeon Arena, Evolution Lab, Gas Grid

**[Configuration Reference](CONFIGURATION.md)** - All server options

**Target audience**: System Administrators, OT Engineers, Operators

---

## Quick Navigation

### Getting Started

- [Start the server](USER_GUIDE.md#1-start-the-server)
- [Bootstrap file](USER_GUIDE.md#4-bootstrap-file)
- [Configuration Reference](CONFIGURATION.md) - every command-line option, with defaults

### Core Features

- [Generator Plugin](https://github.com/rossonet/waldot/tree/master/plugins/waldot-plugin-generator) - Data simulation
- [Rules Engine Plugin](https://github.com/rossonet/waldot/tree/master/plugins/waldot-plugin-rules-engine) - Event-driven automation
- [TinkerPop Plugin](https://github.com/rossonet/waldot/tree/master/plugins/waldot-plugin-tinkerpop) - Gremlin server access

### Examples

- [Step-by-step examples](examples/README.md) - plain vertices, active edges, rules, bootstrap
- Showcases: [Dungeon Arena](examples/05-showcase-arena.md), [Evolution Lab](examples/06-showcase-evolution.md), [Gas Grid](examples/07-showcase-gas-grid.md)
- [Docker Compose examples](https://github.com/rossonet/waldot/tree/master/waldot-app/examples) - server deployment variants

### Module Documentation

Each module keeps its technical README next to the code:

| Module | Content |
| ------ | ------- |
| [waldot-api](https://github.com/rossonet/waldot/tree/master/waldot-api) | Public contracts used by runtimes and plugins |
| [waldot-namespace](https://github.com/rossonet/waldot/tree/master/waldot-namespace) | OPC UA ↔ graph bridge, Milo strategies, bootstrap ([syntax](https://github.com/rossonet/waldot/blob/master/waldot-namespace/BOOTSTRAP_CONFIGURATION.md)) |
| [waldot-runner](https://github.com/rossonet/waldot/tree/master/waldot-runner) | Picocli runner with all server options |
| [waldot-app](https://github.com/rossonet/waldot/tree/master/waldot-app) | Server application, distribution and Docker examples |

---

## Key Concepts

### What is WaldOT?

WaldOT is a **Digital Twin Engine** that creates a living bridge between:

- **OPC UA** (industrial automation standard)
- **Apache TinkerPop** (graph database framework)

This enables:

- Real-time bidirectional synchronization between OPC UA and graph databases
- Complex graph queries on industrial data using Gremlin
- Event-driven automation with the rules engine
- Edge computing capabilities for low-latency processing

### Core Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     WaldOT Framework                         │
│                                                              │
│  ┌──────────────┐              ┌─────────────────────┐     │
│  │   OPC UA     │              │   TinkerPop         │     │
│  │   Server     │  ←────────→  │   Graph             │     │
│  │ (Eclipse Milo)│   Bi-Sync   │ (Apache TinkerPop)  │     │
│  └──────────────┘              └─────────────────────┘     │
│                                                              │
│  ┌──────────────────────────────────────────────────┐      │
│  │              Plugin System                        │      │
│  │  • Generator (data simulation)                    │      │
│  │  • Rules Engine (event-driven automation)         │      │
│  │  • TinkerPop Server (Gremlin access)              │      │
│  │  • Custom plugins...                              │      │
│  └──────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### Key Features

1. **Unified OT/IT Data Model**
   - OPC UA Objects → TinkerPop Vertices
   - OPC UA References → Graph Edges
   - OPC UA Variables → Vertex Properties

2. **Powerful Graph Queries**
   - Gremlin query language for complex traversals
   - Real-time queries on live industrial data
   - Graph algorithms (PageRank, shortest path, community detection)

3. **Event-Driven Rules Engine**
   - IF-THEN-THAT rules with JEXL expressions
   - Priority-based event processing
   - Hysteresis deduplication
   - Full graph access in rule conditions and actions

4. **Data Generation & Simulation**
   - 6 algorithms (random, sinusoidal, triangular, etc.)
   - Perfect for testing without physical hardware
   - Thousands of concurrent generators with virtual threads

5. **Edge-First Architecture**
   - Runs on-device (RTUs, industrial PCs, edge gateways)
   - Low latency, offline capable
   - Bandwidth efficient

---

## Technology Stack

- **Java 26** (build target) - virtual threads for massive concurrency
- **Eclipse Milo** - OPC UA server implementation
- **Apache TinkerPop 3.x** - Graph database framework
- **Apache Commons JEXL** - Expression language for rules
- **Docker** - Containerized deployment

---

## Community & Support

- **GitHub**: [rossonet/waldot](https://github.com/rossonet/waldot)
- **Docker Hub**: [rossonet/waldot](https://hub.docker.com/r/rossonet/waldot)
- **Maven Central**: [net.rossonet.waldot](https://central.sonatype.com/search?q=net.rossonet.waldot)
- **Issues**: [GitHub Issues](https://github.com/rossonet/waldot/issues)

---

## License

WaldOT is released under the **Apache License 2.0**.

---

## Project Sponsor

[![Zona Autonoma srl](https://raw.githubusercontent.com/rossonet/images/main/artwork/rossonet-logo/png/rossonet-logo_280_115.png)](https://www.rossonet.net)

**Zona Autonoma srl** - Industrial IoT and Edge Computing Solutions
