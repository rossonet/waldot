# Example 3: Energy Monitoring System

**Monitor energy consumption across multiple buildings with aggregation and alerting**

## Overview

Real-time energy monitoring system with consumption tracking, peak detection, and cost calculation.

### Features

- **3 Buildings**: Office, Factory, Warehouse
- **Energy Metrics**: Power (kW), Voltage, Current per building
- **Aggregation Rules**: Total consumption, peak detection, cost calculation
- **Alerts**: High consumption, voltage anomalies

## Quick Start

```bash
cd /work/waldot/docs/examples/03-energy-monitoring
docker-compose up -d
```

**Access**: `opc.tcp://localhost:12688/waldot`

## Sensors

| Building  | Metrics                 | Range                           | Update |
| --------- | ----------------------- | ------------------------------- | ------ |
| Office    | Power, Voltage, Current | 10-50 kW, 220-240V, 45-230A     | 5s     |
| Factory   | Power, Voltage, Current | 100-500 kW, 380-420V, 260-1300A | 5s     |
| Warehouse | Power, Voltage, Current | 20-100 kW, 220-240V, 90-450A    | 5s     |

## Rules

- **total-consumption**: Calculate total power every 10s
- **peak-detection**: Alert if total > 600 kW
- **cost-calculation**: Calculate energy cost (€0.15/kWh)
- **voltage-anomaly**: Alert if voltage out of range

## Gremlin Queries

```groovy
// Total power consumption
g.V().has('type','generator').has('label',containing('power')).values('data').sum()

// Average voltage
g.V().has('type','generator').has('label',containing('voltage')).values('data').mean()

// Energy cost (last hour)
g.V().has('type','cost-record').has('timestamp',gt(System.currentTimeMillis()-3600000)).values('cost').sum()
```

---

**See**: [docker-compose.yml](docker-compose.yml) | [boot.conf](boot.conf)
