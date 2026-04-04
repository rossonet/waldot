# Example 1: Industrial Monitoring System

**Complete monitoring solution with simulated sensors and automated alerting**

## Overview

This example demonstrates a complete industrial monitoring system using WaldOT's Generator and Rules Engine plugins. It simulates a production facility with multiple zones, each equipped with temperature and pressure sensors, and implements automated alerting rules.

### Features

- **3 Production Zones**: Office, Warehouse, Production Floor
- **6 Simulated Sensors**: Temperature and pressure for each zone
- **Automated Alerting**: Rules trigger warnings when thresholds are exceeded
- **Multi-Level Alarms**: Warning, Critical, and Emergency levels
- **Real-Time Monitoring**: All data accessible via OPC UA

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    WaldOT Container                      │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Generator Plugin (6 sensors)                     │  │
│  │  • temp-office (sinusoidal 18-26°C)              │  │
│  │  • pressure-office (random 950-1050 mbar)        │  │
│  │  • temp-warehouse (random 10-35°C)               │  │
│  │  • pressure-warehouse (random 950-1050 mbar)     │  │
│  │  • temp-production (sinusoidal 20-40°C)          │  │
│  │  • pressure-production (random 950-1050 mbar)    │  │
│  └──────────────────────────────────────────────────┘  │
│                          ↓                               │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Rules Engine (6 rules)                          │  │
│  │  • temp-warning (>30°C)                          │  │
│  │  • temp-critical (>35°C)                         │  │
│  │  • pressure-warning (<970 or >1030 mbar)        │  │
│  │  • multi-zone-alarm (2+ zones in alarm)         │  │
│  │  • emergency-shutdown (temp>40°C)               │  │
│  │  • system-health-check (every 60s)              │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
└─────────────────────────────────────────────────────────┘
                          ↓
              OPC UA: opc.tcp://localhost:12686/waldot
```

---

## Quick Start

### 1. Deploy

```bash
cd /work/waldot/docs/examples/01-industrial-monitoring
docker-compose up -d
```

### 2. Access

- **OPC UA**: `opc.tcp://localhost:12686/waldot`
- **HTTPS**: `https://localhost:8443`

### 3. Connect with OPC UA Client

1. Open UaExpert or Prosys Browser
2. Connect to `opc.tcp://localhost:12686/waldot`
3. Browse to `Objects/Gremlin Engine`
4. Explore sensors and rules

### 4. View Logs

```bash
docker-compose logs -f waldot
```

### 5. Stop

```bash
docker-compose down
```

---

## Files

- `docker-compose.yml` - Docker Compose configuration
- `boot.conf` - Bootstrap configuration (Groovy script)
- `README.md` - This file

---

## Configuration Details

### Sensors

| Sensor              | Type        | Algorithm  | Min      | Max       | Update Interval |
| ------------------- | ----------- | ---------- | -------- | --------- | --------------- |
| temp-office         | Temperature | sinusoidal | 18°C     | 26°C      | 5s              |
| pressure-office     | Pressure    | random     | 950 mbar | 1050 mbar | 2s              |
| temp-warehouse      | Temperature | random     | 10°C     | 35°C      | 5s              |
| pressure-warehouse  | Pressure    | random     | 950 mbar | 1050 mbar | 2s              |
| temp-production     | Temperature | sinusoidal | 20°C     | 40°C      | 5s              |
| pressure-production | Pressure    | random     | 950 mbar | 1050 mbar | 2s              |

### Rules

| Rule                | Condition                | Action                   | Priority |
| ------------------- | ------------------------ | ------------------------ | -------- |
| temp-warning        | temperature > 30         | Log warning              | 100      |
| temp-critical       | temperature > 35         | Log error + create alert | 200      |
| pressure-warning    | pressure < 970 OR > 1030 | Log warning              | 100      |
| multi-zone-alarm    | 2+ zones in alarm        | Log critical + notify    | 300      |
| emergency-shutdown  | temperature > 40         | Log emergency + shutdown | 500      |
| system-health-check | Every 60 seconds         | Log system status        | 50       |

---

## Testing Scenarios

### Scenario 1: Normal Operation

**Expected**: All sensors generate values within normal ranges, no alarms triggered.

**Verify**:

```bash
docker-compose logs waldot | grep -i "info"
```

### Scenario 2: Temperature Warning

**Trigger**: Wait for `temp-warehouse` to exceed 30°C (random algorithm)

**Expected**: Log message "High temperature warning"

**Verify**:

```bash
docker-compose logs waldot | grep -i "high temperature"
```

### Scenario 3: Critical Temperature

**Trigger**: Wait for `temp-production` to exceed 35°C (sinusoidal peak)

**Expected**:

- Log message "CRITICAL temperature"
- Alert vertex created in graph

**Verify**:

```bash
docker-compose logs waldot | grep -i "critical"
```

### Scenario 4: Multi-Zone Alarm

**Trigger**: Multiple zones exceed thresholds simultaneously

**Expected**: Log message "Multiple zones in alarm state"

**Verify**:

```bash
docker-compose logs waldot | grep -i "multiple zones"
```

---

## Gremlin Queries

### Query All Sensors

```groovy
g.V().has('type', 'generator').valueMap('label', 'data')
```

### Query Sensors in Alarm

```groovy
g.V().has('type', 'generator')
  .has('data', gt(30))
  .values('label')
```

### Count Active Rules

```groovy
g.V().has('type', 'rule').count()
```

### View Rule Metrics

```groovy
g.V().has('type', 'rule')
  .valueMap('label', 'Total', 'Executed', 'Errors')
```

### Find All Alerts

```groovy
g.V().has('type', 'alert')
  .order().by('timestamp', desc)
  .limit(10)
  .valueMap()
```

---

## Customization

### Add New Zone

Edit `boot.conf`:

```groovy
// Add new zone
createMonitoringZone('server-room', 18, 24, 950, 1050)
```

### Change Thresholds

Edit `boot.conf`:

```groovy
// Modify temperature warning threshold
.property('Condition', 'temperature > 25.0')  // Lower threshold
```

### Add Custom Rule

Edit `boot.conf`:

```groovy
// Add humidity monitoring
humidityRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'humidity-alarm')
  .property('Condition', 'humidity > 80.0')
  .property('Action', "log.warn('High humidity: ' + humidity)")
  .property('Priority', '100')
  .next()
```

---

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker-compose logs waldot

# Check port conflicts
lsof -i :12686

# Restart
docker-compose restart
```

### No Data in Sensors

```bash
# Verify generators are running
docker-compose exec waldot sh -c "echo 'g.V().has(\"type\",\"generator\").count()' | ..."

# Check logs for errors
docker-compose logs waldot | grep -i error
```

### Rules Not Firing

```bash
# Enable debug mode
# Edit docker-compose.yml, add:
# - JAVA_OPTS=-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG

# Restart
docker-compose restart

# Check rule metrics
# Via OPC UA: Browse to rule vertex, check Total/Executed properties
```

---

## Next Steps

- Explore [Example 2: Production Simulation](../02-production-simulation/)
- Learn about [Rules Engine](../../../plugins/waldot-plugin-rules-engine/README.md)
- Read [Developer Guide](../../guide/docs/DEVELOPER_GUIDE.md)

---

**WaldOT Industrial Monitoring Example** - Rossonet s.c.a r.l.
