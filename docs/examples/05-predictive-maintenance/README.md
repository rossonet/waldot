# Example 5: Predictive Maintenance

**Detect equipment failure patterns and predict maintenance needs**

## Overview

Advanced predictive maintenance system using vibration analysis, temperature monitoring, and pattern detection.

### Features

- **Equipment Monitoring**: 4 motors with vibration and temperature sensors
- **Pattern Detection**: Identify failure precursors
- **Maintenance Scheduling**: Automated maintenance recommendations
- **Failure Prediction**: Predict failures 24-48h in advance

## Quick Start

```bash
cd /work/waldot/docs/examples/05-predictive-maintenance
docker-compose up -d
```

**Access**: `opc.tcp://localhost:12690/waldot`

## Architecture

```
Equipment (Motors)
       ↓
Sensors (Vibration, Temperature, Current)
       ↓
Pattern Detection Rules
       ↓
Maintenance Recommendations
```

## Sensors per Motor

- **vibration**: 0-100 mm/s (random, higher = worse)
- **temperature**: 40-90°C (sinusoidal, spikes indicate issues)
- **current**: 10-50A (random, anomalies indicate problems)
- **runtime-hours**: Incremental counter

## Rules

- **vibration-warning**: Alert if vibration > 70 mm/s
- **temperature-critical**: Alert if temperature > 80°C
- **pattern-detection**: Detect simultaneous high vibration + temperature
- **maintenance-scheduler**: Schedule maintenance based on runtime
- **failure-prediction**: Predict failure if multiple indicators abnormal

## Gremlin Queries

```groovy
// Find motors needing maintenance
g.V().has('type','motor').has('maintenance-needed','true').values('label')

// Equipment health score
g.V().has('type','motor').project('motor','health')
  .by('label')
  .by(out('has-sensor').has('label',containing('vibration')).values('data').mean())

// Failure predictions
g.V().has('type','prediction').has('severity','HIGH').valueMap()

// Maintenance history
g.V().has('type','maintenance-record').order().by('timestamp',desc).limit(10).valueMap()
```

---

**See**: [docker-compose.yml](docker-compose.yml) | [boot.conf](boot.conf)
