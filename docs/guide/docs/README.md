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

**[Plugin Development Manual](manuale_plugins.md)** - Complete plugin development guide (Italian)

- Plugin architecture deep dive
- Virtual threads and concurrency
- OPC UA type registration
- Real-world plugin examples

**Target audience**: Software Engineers, DevOps Engineers, Integration Specialists

---

### 👤 For End Users & Operators

**[User Guide](USER_GUIDE.md)** - Operating and configuring WaldOT

- Installation and deployment
- Configuration options
- Using the OPC UA interface
- Gremlin query basics
- Monitoring and troubleshooting
- Common workflows

**[User Manual (Italian)](manuale_utente.md)** - Complete user manual in Italian

- Comprehensive configuration guide
- Bootstrap configuration examples
- Plugin usage instructions

**Target audience**: System Administrators, OT Engineers, Operators

---

## Quick Navigation

### Getting Started

- [Quick Start Guide](USER_GUIDE.md#quick-start)
- [Docker Deployment](USER_GUIDE.md#docker-deployment)
- [First Graph Creation](USER_GUIDE.md#creating-your-first-graph)

### Core Features

- [Generator Plugin](../../../plugins/waldot-plugin-generator/README.md) - Data simulation
- [Rules Engine Plugin](../../../plugins/waldot-plugin-rules-engine/README.md) - Event-driven automation
- [TinkerPop Plugin](../../../plugins/waldot-plugin-tinkerpop/README.md) - Gremlin server access

### Examples

- [Example 1: Industrial Monitoring](../../examples/01-industrial-monitoring/README.md)
- [Example 2: Production Simulation](../../examples/02-production-simulation/README.md)
- [Example 3: Energy Monitoring](../../examples/03-energy-monitoring/README.md)
- [Example 4: Quality Control](../../examples/04-quality-control/README.md)
- [Example 5: Predictive Maintenance](../../examples/05-predictive-maintenance/README.md)

### Reference Documentation

- [WaldOT Reference (English)](../../reference/waldot/README_EN.md)
- [WaldOT Reference (Italian)](../../reference/waldot/README_IT.md)
- [Configuration Reference](../../../waldot-app/CONFIGURATION.md)
- [Environment Variables](../../../waldot-app/ENVIRONMENT_VARIABLES.md)
- [Bootstrap Configuration](../../../waldot-namespace/BOOTSTRAP_CONFIGURATION.md)

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

- **Java 21+** - Virtual threads for massive concurrency
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

[![Rossonet s.c.a r.l.](https://raw.githubusercontent.com/rossonet/images/main/artwork/rossonet-logo/png/rossonet-logo_280_115.png)](https://www.rossonet.net)

**Rossonet s.c.a r.l.** - Industrial IoT and Edge Computing Solutions
