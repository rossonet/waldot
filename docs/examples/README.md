# WaldOT Examples

**Complete Docker Compose examples showcasing Generator and Rules Engine plugins**

## Overview

This directory contains 5 complete, production-ready examples demonstrating WaldOT's capabilities in real-world industrial scenarios. Each example uses Docker Compose for easy deployment and includes:

- Complete `docker-compose.yml` configuration
- Bootstrap configuration (`boot.conf`) with Groovy scripts
- Detailed README with architecture, queries, and troubleshooting
- Ready-to-run setup (just `docker-compose up -d`)

---

## Examples

### 1. [Industrial Monitoring](01-industrial-monitoring/)

**Complexity**: ⭐⭐ Beginner-friendly

**Description**: Complete monitoring system for a multi-zone industrial facility

**Features**:

- 3 production zones (Office, Warehouse, Production Floor)
- 6 simulated sensors (temperature + pressure per zone)
- Multi-level alerting (Warning, Critical, Emergency)
- Real-time monitoring via OPC UA

**Use Case**: Learn WaldOT basics, understand generator and rules engine integration

**Port**: `12686`

```bash
cd 01-industrial-monitoring
docker-compose up -d
```

---

### 2. [Production Simulation](02-production-simulation/)

**Complexity**: ⭐⭐⭐ Intermediate

**Description**: Simulate a complete production line with quality control

**Features**:

- 4 production stations (Cutting, Assembly, Testing, Packaging)
- 8 sensors (speed + quality per station)
- Automated quality control and line stop
- Production metrics tracking

**Use Case**: Quality control, production optimization, defect detection

**Port**: `12687`

```bash
cd 02-production-simulation
docker-compose up -d
```

---

### 3. [Energy Monitoring](03-energy-monitoring/)

**Complexity**: ⭐⭐⭐ Intermediate

**Description**: Real-time energy consumption monitoring and cost calculation

**Features**:

- 3 buildings (Office, Factory, Warehouse)
- Energy metrics (Power, Voltage, Current)
- Total consumption aggregation
- Peak detection and cost calculation

**Use Case**: Energy management, cost optimization, sustainability reporting

**Port**: `12688`

```bash
cd 03-energy-monitoring
docker-compose up -d
```

---

### 4. [Quality Control & Traceability](04-quality-control/)

**Complexity**: ⭐⭐⭐⭐ Advanced

**Description**: Track product quality and genealogy through production

**Features**:

- Batch tracking system
- 4 quality checkpoints
- Defect detection and quarantine
- Product genealogy with graph traversal

**Use Case**: Food safety, pharmaceutical compliance, automotive traceability

**Port**: `12689`

```bash
cd 04-quality-control
docker-compose up -d
```

---

### 5. [Predictive Maintenance](05-predictive-maintenance/)

**Complexity**: ⭐⭐⭐⭐⭐ Expert

**Description**: Predict equipment failures using pattern detection

**Features**:

- 4 motors with vibration, temperature, current sensors
- Pattern detection (multiple indicators)
- Failure prediction (24-48h advance warning)
- Maintenance scheduling based on runtime

**Use Case**: Reduce downtime, optimize maintenance, prevent catastrophic failures

**Port**: `12690`

```bash
cd 05-predictive-maintenance
docker-compose up -d
```

---

## Quick Start Guide

### Prerequisites

- Docker 20.10+
- Docker Compose 1.29+
- OPC UA client (UaExpert, Prosys Browser) - optional but recommended

### Run an Example

```bash
# Navigate to example directory
cd 01-industrial-monitoring

# Start WaldOT
docker-compose up -d

# View logs
docker-compose logs -f waldot

# Stop
docker-compose down
```

### Access

Each example exposes:

- **OPC UA**: `opc.tcp://localhost:<PORT>/waldot` (see example README for port)
- **HTTPS**: `https://localhost:844X` (X = 3-7 depending on example)

### Connect with OPC UA Client

1. Open UaExpert or Prosys Browser
2. Add server: `opc.tcp://localhost:<PORT>/waldot`
3. Security: None (development mode)
4. Credentials: admin / monitor123 (or as specified in example)
5. Browse to `Objects/Gremlin Engine`

---

## Common Features Across Examples

### Generator Plugin

All examples use the **waldot-plugin-generator** for data simulation:

**Algorithms**:

- `incremental`: Linear increase (counters, runtime hours)
- `decremental`: Linear decrease (countdown timers)
- `random`: Random values (pressure, quality metrics)
- `sinusoidal`: Sine wave (temperature cycles)
- `triangular`: Triangle wave (sawtooth patterns)
- `stopped`: Constant value (paused simulation)

**Configuration**:

```groovy
sensor = graph.addVertex(
    "type", "generator",
    "label", "my-sensor",
    "Algorithm", "sinusoidal",
    "Min", "0",
    "Max", "100",
    "Delay", "5000"  // Update every 5 seconds
)
```

### Rules Engine Plugin

All examples use the **waldot-plugin-rules-engine** for automation:

**Components**:

- **RuleVertex**: IF-THEN rule with JEXL expressions
- **ComputeVertex**: Thread manager for rule execution
- **FireMonitoredEdge**: Connects sensors to rules
- **ComputeMonitoredEdge**: Connects rules to compute

**Configuration**:

```groovy
// Create compute manager
compute = graph.addVertex(
    "type", "compute",
    "label", "main-compute",
    "Threads", "4"
)

// Create rule
rule = graph.addVertex(
    "type", "rule",
    "label", "my-rule",
    "Condition", "temperature > 80.0",
    "Action", "log.warn('High temperature: ' + temperature)",
    "Priority", "100"
)

// Connect rule to compute
rule.addEdge("execute", compute, "Priority", "100")

// Monitor sensor
sensor.addEdge("fire", rule,
    "monitor-property", "data",
    "active", "true"
)
```

---

## Gremlin Query Examples

### Basic Queries

```groovy
// Count all vertices
g.V().count()

// Find all sensors
g.V().has('type', 'generator').values('label')

// Find all rules
g.V().has('type', 'rule').values('label')

// Get sensor value
g.V().has('label', 'sensor1').values('data')
```

### Advanced Queries

```groovy
// Find sensors in alarm state
g.V().has('type', 'generator')
  .has('data', gt(80))
  .values('label')

// Calculate average temperature
g.V().has('type', 'generator')
  .has('label', containing('temp'))
  .values('data')
  .mean()

// Find all alerts in last hour
g.V().has('type', 'alert')
  .has('timestamp', gt(System.currentTimeMillis() - 3600000))
  .valueMap()

// Trace relationships
g.V().has('label', 'sensor1')
  .out('located-in')
  .values('label')
```

### Rule Metrics

```groovy
// View rule execution statistics
g.V().has('type', 'rule')
  .valueMap('label', 'Total', 'Executed', 'Errors', 'Queue')

// Find rules with errors
g.V().has('type', 'rule')
  .has('Errors', gt(0))
  .values('label')
```

---

## Customization

### Modify Sensor Parameters

Edit `boot.conf`:

```groovy
// Change update interval
sensor.property("Delay", "10000")  // 10 seconds

// Change algorithm
sensor.property("Algorithm", "random")

// Change range
sensor.property("Min", "0")
sensor.property("Max", "50")
```

### Add New Rules

Edit `boot.conf`:

```groovy
// Add custom rule
customRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'my-custom-rule')
  .property('Condition', 'myValue > 100')
  .property('Action', 'log.info("Custom action")')
  .property('Priority', '100')
  .next()

// Connect to compute
customRule.addEdge('execute', compute, 'Priority', '100')

// Monitor sensor
sensor.addEdge('fire', customRule,
    'monitor-property', 'data',
    'active', 'true'
)
```

### Change Ports

Edit `docker-compose.yml`:

```yaml
ports:
  - "4840:4840" # Change from 12686 to 4840
environment:
  - WALDOT_TCP_PORT=4840 # Update environment variable
```

---

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker-compose logs waldot

# Check port conflicts
lsof -i :12686

# Remove old containers
docker-compose down -v
docker-compose up -d
```

### No Data in Sensors

```bash
# Verify generators are running
docker-compose exec waldot sh

# Check logs for errors
docker-compose logs waldot | grep -i error
```

### Rules Not Firing

```bash
# Enable debug logging
# Edit docker-compose.yml:
environment:
  - JAVA_OPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG

# Restart
docker-compose restart

# Check rule metrics via OPC UA
# Browse to rule vertex, check Total/Executed properties
```

### OPC UA Connection Issues

```bash
# Test port
telnet localhost 12686

# Check firewall
sudo ufw allow 12686/tcp

# Verify container is running
docker ps | grep waldot
```

---

## Performance Tips

### Resource Allocation

```yaml
# docker-compose.yml
services:
  waldot:
    deploy:
      resources:
        limits:
          cpus: "4"
          memory: 4G
        reservations:
          cpus: "2"
          memory: 2G
```

### JVM Tuning

```yaml
environment:
  - JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC
```

### Sensor Update Intervals

- **High frequency** (100-1000ms): Use for critical monitoring
- **Medium frequency** (1000-5000ms): Use for standard monitoring
- **Low frequency** (>5000ms): Use for slow-changing values

---

## Next Steps

### Learn More

- [WaldOT Documentation](../guide/docs/README.md)
- [Developer Guide](../guide/docs/DEVELOPER_GUIDE.md)
- [Architecture Overview](../guide/docs/ARCHITECTURE_OVERVIEW.md)
- [User Guide](../guide/docs/USER_GUIDE.md)

### Plugin Documentation

- [Generator Plugin](../../plugins/waldot-plugin-generator/README.md)
- [Rules Engine Plugin](../../plugins/waldot-plugin-rules-engine/README.md)
- [TinkerPop Plugin](../../plugins/waldot-plugin-tinkerpop/README.md)

### Community

- **GitHub**: https://github.com/rossonet/waldot
- **Docker Hub**: https://hub.docker.com/r/rossonet/waldot
- **Issues**: https://github.com/rossonet/waldot/issues

---

## License

All examples are released under the **Apache License 2.0**.

---

## Project Sponsor

[![Rossonet s.c.a r.l.](https://raw.githubusercontent.com/rossonet/images/main/artwork/rossonet-logo/png/rossonet-logo_280_115.png)](https://www.rossonet.net)

**Rossonet s.c.a r.l.** - Industrial IoT and Edge Computing Solutions

---

**WaldOT Examples** - Complete, production-ready demonstrations of WaldOT capabilities
