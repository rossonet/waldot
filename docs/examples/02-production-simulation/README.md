# Example 2: Production Line Simulation

**Simulate a complete production line with quality control and automated alarms**

## Overview

This example simulates a production line with multiple stations, quality sensors, and automated quality control rules.

### Features

- **4 Production Stations**: Cutting, Assembly, Testing, Packaging
- **8 Simulated Sensors**: Speed, temperature, quality metrics
- **Quality Control Rules**: Automated defect detection and line stop
- **Production Metrics**: Real-time throughput and quality tracking

## Quick Start

```bash
cd /work/waldot/docs/examples/02-production-simulation
docker-compose up -d
```

**Access**: `opc.tcp://localhost:12687/waldot`

## Architecture

```
Production Line:
[Cutting] → [Assembly] → [Testing] → [Packaging]
    ↓           ↓            ↓            ↓
  Sensors    Sensors      Sensors      Sensors
    ↓           ↓            ↓            ↓
         Quality Control Rules
              ↓
    Automated Actions (Stop/Alert)
```

## Sensors

| Station   | Sensors        | Algorithm           | Purpose                           |
| --------- | -------------- | ------------------- | --------------------------------- |
| Cutting   | speed, quality | incremental, random | Monitor cutting speed and quality |
| Assembly  | speed, quality | incremental, random | Monitor assembly rate             |
| Testing   | defect-rate    | random              | Detect defects                    |
| Packaging | speed, quality | incremental, random | Monitor packaging                 |

## Rules

- **quality-check**: Trigger if quality < 95%
- **defect-alarm**: Trigger if defect-rate > 5%
- **line-stop**: Emergency stop if quality < 90%
- **throughput-monitor**: Track production rate every 30s

## Gremlin Queries

```groovy
// Total production count
g.V().has('type','generator').has('label',containing('speed')).values('data').sum()

// Quality metrics
g.V().has('type','generator').has('label',containing('quality')).values('data').mean()

// Defect count
g.V().has('type','alert').has('severity','DEFECT').count()
```

---

**See**: [docker-compose.yml](docker-compose.yml) | [boot.conf](boot.conf)
