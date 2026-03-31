# WaldOT Rules Engine Examples

## Introduction

This guide provides real-world examples of using the WaldOT Rules Engine for various use cases. Each example includes complete setup, rule definitions, and explanations.

## Example 1: Temperature Monitoring System

### Scenario
Monitor temperature sensors in a data center. Alert when temperature exceeds safe thresholds with different severity levels.

### Setup

```groovy
// Create compute vertex for rule execution
compute = graph.addVertex(
    "type", "compute",
    "label", "datacenter-compute",
    "Threads", "4"
)

// Create temperature sensors
sensor1 = graph.addVertex(
    "type", "temperature-sensor",
    "label", "rack-1-temp",
    "temperature", 25.0,
    "location", "rack-1"
)

sensor2 = graph.addVertex(
    "type", "temperature-sensor",
    "label", "rack-2-temp",
    "temperature", 28.0,
    "location", "rack-2"
)
```

### Rule 1: Warning Level (80°C)

```groovy
warningRule = graph.addVertex(
    "type", "rule",
    "label", "temp-warning",
    "Condition", "temperature > 80.0 && temperature <= 90.0",
    "Action", "log.warn('Temperature warning: ' + temperature + '°C at ' + self.property('source').value())",
    "Hysteresis", "30000"  // 30 seconds
)

warningRule.addEdge("execute", compute, "Priority", "50")
sensor1.addEdge("fire", warningRule, "monitor-property", "temperature", "active", "true")
sensor2.addEdge("fire", warningRule, "monitor-property", "temperature", "active", "true")
```

### Rule 2: Critical Level (90°C)

```groovy
criticalRule = graph.addVertex(
    "type", "rule",
    "label", "temp-critical",
    "Condition", "temperature > 90.0",
    "Action", "log.error('CRITICAL temperature: ' + temperature + '°C'); commands.execute('send-alert', 'Critical temperature')",
    "Hysteresis", "10000"  // 10 seconds
)

criticalRule.addEdge("execute", compute, "Priority", "100")  // Higher priority
sensor1.addEdge("fire", criticalRule, "monitor-property", "temperature")
sensor2.addEdge("fire", criticalRule, "monitor-property", "temperature")
```

### Rule 3: Temperature Normalized

```groovy
normalRule = graph.addVertex(
    "type", "rule",
    "label", "temp-normal",
    "Condition", "temperature <= 70.0",
    "Action", "log.info('Temperature normalized: ' + temperature + '°C')",
    "Hysteresis", "60000"  // 60 seconds, avoid spam
)

normalRule.addEdge("execute", compute, "Priority", "10")  // Low priority
sensor1.addEdge("fire", normalRule, "monitor-property", "temperature")
sensor2.addEdge("fire", normalRule, "monitor-property", "temperature")
```

### Testing

```groovy
// Trigger warning
sensor1.property("temperature", 85.0)
// Output: WARN Temperature warning: 85.0°C at rack-1

// Trigger critical
sensor1.property("temperature", 95.0)
// Output: ERROR CRITICAL temperature: 95.0°C

// Normalize
sensor1.property("temperature", 65.0)
// Output: INFO Temperature normalized: 65.0°C
```

---

## Example 2: System Health Monitoring

### Scenario
Monitor system health across multiple services. Trigger alerts when too many services are unhealthy.

### Setup

```groovy
compute = graph.addVertex("type", "compute", "label", "health-compute", "Threads", "2")

// Create services
services = []
['web-server', 'database', 'cache', 'queue', 'storage'].each { name ->
    service = graph.addVertex(
        "type", "service",
        "label", name,
        "status", "RUNNING",
        "errorCount", 0
    )
    services << service
}
```

### Rule 1: Individual Service Error

```groovy
serviceErrorRule = graph.addVertex(
    "type", "rule",
    "label", "service-error-check",
    "Condition", "errorCount > 10",
    "Action", "log.error('Service ' + self.property('source').value() + ' has ' + errorCount + ' errors'); g.V(self).property('needsRestart', true).iterate()",
    "Hysteresis", "5000"
)

serviceErrorRule.addEdge("execute", compute, "Priority", "80")

services.each { service ->
    service.addEdge("fire", serviceErrorRule, "monitor-property", "errorCount")
}
```

### Rule 2: Multiple Services Down

```groovy
multiServiceRule = graph.addVertex(
    "type", "rule",
    "label", "multi-service-down",
    "Condition", "g.V().hasLabel('service').has('status', 'ERROR').count().next() >= 2",
    "Action", "log.error('CRITICAL: Multiple services down'); commands.execute('escalate-alert', 'Multiple services failing')",
    "Hysteresis", "30000"
)

multiServiceRule.addEdge("execute", compute, "Priority", "100")

services.each { service ->
    service.addEdge("fire", multiServiceRule, "monitor-property", "status")
}
```

### Rule 3: Auto-Restart Service

```groovy
autoRestartRule = graph.addVertex(
    "type", "rule",
    "label", "auto-restart",
    "Condition", "needsRestart == true",
    "Action", "var serviceName = self.property('source').value(); log.info('Restarting service: ' + serviceName); commands.execute('restart-service', serviceName); g.V(self).property('needsRestart', false).property('errorCount', 0).iterate()"
)

autoRestartRule.addEdge("execute", compute, "Priority", "90")

services.each { service ->
    service.addEdge("fire", autoRestartRule, "monitor-property", "needsRestart")
}
```

### Testing

```groovy
// Trigger individual service error
services[0].property("errorCount", 15)
// Output: ERROR Service web-server has 15 errors

// Trigger multiple services down
services[0].property("status", "ERROR")
services[1].property("status", "ERROR")
// Output: CRITICAL: Multiple services down

// Check auto-restart triggered
services[0].property("needsRestart").value()  // true
// Output: INFO Restarting service: web-server
```

---

## Example 3: Network Traffic Monitoring

### Scenario
Monitor network traffic and detect anomalies based on bandwidth usage patterns.

### Setup

```groovy
compute = graph.addVertex("type", "compute", "label", "network-compute", "Threads", "4")

// Create network interfaces
interfaces = []
['eth0', 'eth1', 'eth2'].each { name ->
    iface = graph.addVertex(
        "type", "network-interface",
        "label", name,
        "bytesIn", 0L,
        "bytesOut", 0L,
        "packetsDropped", 0
    )
    interfaces << iface
}
```

### Rule 1: High Bandwidth Usage

```groovy
highBandwidthRule = graph.addVertex(
    "type", "rule",
    "label", "high-bandwidth",
    "Condition", "bytesIn > 1000000000",  // 1GB
    "Action", "var gbIn = bytesIn / 1000000000.0; log.warn('High bandwidth on ' + self.property('source').value() + ': ' + String.format('%.2f GB', gbIn))",
    "Hysteresis", "60000"
)

highBandwidthRule.addEdge("execute", compute, "Priority", "50")

interfaces.each { iface ->
    iface.addEdge("fire", highBandwidthRule, "monitor-property", "bytesIn")
}
```

### Rule 2: Packet Loss Detection

```groovy
packetLossRule = graph.addVertex(
    "type", "rule",
    "label", "packet-loss",
    "Condition", "packetsDropped > 100",
    "Action", "log.error('Packet loss detected on ' + self.property('source').value() + ': ' + packetsDropped + ' packets dropped'); commands.execute('check-network-health')",
    "Hysteresis", "30000"
)

packetLossRule.addEdge("execute", compute, "Priority", "80")

interfaces.each { iface ->
    iface.addEdge("fire", packetLossRule, "monitor-property", "packetsDropped")
}
```

### Rule 3: Traffic Imbalance

```groovy
imbalanceRule = graph.addVertex(
    "type", "rule",
    "label", "traffic-imbalance",
    "Condition", "bytesIn > 0 && bytesOut > 0 && Math.abs(bytesIn - bytesOut) / Math.max(bytesIn, bytesOut) > 0.8",
    "Action", "log.warn('Traffic imbalance detected: IN=' + bytesIn + ' OUT=' + bytesOut)",
    "Hysteresis", "120000"
)

imbalanceRule.addEdge("execute", compute, "Priority", "30")

interfaces.each { iface ->
    iface.addEdge("fire", imbalanceRule, "monitor-property", "bytesIn")
    iface.addEdge("fire", imbalanceRule, "monitor-property", "bytesOut")
}
```

### Testing

```groovy
// Trigger high bandwidth
interfaces[0].property("bytesIn", 1500000000L)
// Output: WARN High bandwidth on eth0: 1.50 GB

// Trigger packet loss
interfaces[1].property("packetsDropped", 250)
// Output: ERROR Packet loss detected on eth1: 250 packets dropped

// Trigger imbalance
interfaces[2].property("bytesIn", 1000000L)
interfaces[2].property("bytesOut", 100000L)
// Output: WARN Traffic imbalance detected: IN=1000000 OUT=100000
```

---

## Example 4: Industrial IoT Sensor Array

### Scenario
Monitor industrial sensors for equipment maintenance. Predict failures based on vibration, temperature, and pressure readings.

### Setup

```groovy
compute = graph.addVertex("type", "compute", "label", "iot-compute", "Threads", "8")

// Create industrial equipment
equipment = graph.addVertex(
    "type", "equipment",
    "label", "pump-1",
    "vibration", 2.5,
    "temperature", 45.0,
    "pressure", 75.0,
    "runningHours", 1000
)
```

### Rule 1: Vibration Anomaly

```groovy
vibrationRule = graph.addVertex(
    "type", "rule",
    "label", "vibration-check",
    "Condition", "vibration > 5.0",
    "Action", "log.warn('Abnormal vibration: ' + vibration + ' mm/s. Possible bearing failure.'); g.V(self).property('maintenanceRequired', true).iterate()",
    "Hysteresis", "15000"
)

vibrationRule.addEdge("execute", compute, "Priority", "90")
equipment.addEdge("fire", vibrationRule, "monitor-property", "vibration", "deadband", "0.5")
```

### Rule 2: Temperature + Pressure Combined

```groovy
combinedRule = graph.addVertex(
    "type", "rule",
    "label", "thermal-pressure-alarm",
    "Condition", "temperature > 80.0 && pressure > 100.0",
    "Action", "log.error('CRITICAL: High temperature AND high pressure. Immediate shutdown required.'); commands.execute('emergency-shutdown', 'pump-1')",
    "Hysteresis", "5000"
)

combinedRule.addEdge("execute", compute, "Priority", "100")
equipment.addEdge("fire", combinedRule, "monitor-property", "temperature")
equipment.addEdge("fire", combinedRule, "monitor-property", "pressure")
```

### Rule 3: Predictive Maintenance

```groovy
predictiveRule = graph.addVertex(
    "type", "rule",
    "label", "predictive-maintenance",
    "Condition", "runningHours > 5000 && (vibration > 4.0 || temperature > 70.0)",
    "Action", "log.info('Predictive maintenance recommended for pump-1. Running hours: ' + runningHours); g.V(self).property('scheduleMaintenance', true).iterate()",
    "Hysteresis", "3600000"  // 1 hour
)

predictiveRule.addEdge("execute", compute, "Priority", "40")
equipment.addEdge("fire", predictiveRule, "monitor-property", "runningHours")
equipment.addEdge("fire", predictiveRule, "monitor-property", "vibration")
equipment.addEdge("fire", predictiveRule, "monitor-property", "temperature")
```

### Testing

```groovy
// Normal operation
equipment.property("vibration", 3.0)
equipment.property("temperature", 50.0)
equipment.property("pressure", 80.0)

// Trigger vibration alarm
equipment.property("vibration", 6.5)
// Output: WARN Abnormal vibration: 6.5 mm/s. Possible bearing failure.

// Trigger critical combined alarm
equipment.property("temperature", 85.0)
equipment.property("pressure", 105.0)
// Output: CRITICAL: High temperature AND high pressure. Immediate shutdown required.

// Trigger predictive maintenance
equipment.property("runningHours", 5500)
equipment.property("vibration", 4.5)
// Output: INFO Predictive maintenance recommended for pump-1. Running hours: 5500
```

---

## Example 5: Smart Building Automation

### Scenario
Automate HVAC, lighting, and security systems based on occupancy, time, and environmental conditions.

### Setup

```groovy
compute = graph.addVertex("type", "compute", "label", "building-compute", "Threads", "4")

// Create building zones
zones = []
['lobby', 'office-1', 'office-2', 'conference-room'].each { name ->
    zone = graph.addVertex(
        "type", "zone",
        "label", name,
        "occupancy", 0,
        "temperature", 22.0,
        "lightLevel", 0,
        "hvacMode", "AUTO"
    )
    zones << zone
}
```

### Rule 1: Occupancy-Based Lighting

```groovy
lightingRule = graph.addVertex(
    "type", "rule",
    "label", "auto-lighting",
    "Condition", "occupancy > 0 && lightLevel < 300",
    "Action", "var zoneName = self.property('source').value(); log.info('Turning on lights in ' + zoneName); g.V(self).property('lightLevel', 800).iterate()",
    "Hysteresis", "10000"
)

lightingRule.addEdge("execute", compute, "Priority", "50")

zones.each { zone ->
    zone.addEdge("fire", lightingRule, "monitor-property", "occupancy")
}
```

### Rule 2: Lights Off When Empty

```groovy
lightsOffRule = graph.addVertex(
    "type", "rule",
    "label", "lights-off-empty",
    "Condition", "occupancy == 0 && lightLevel > 0",
    "Action", "var zoneName = self.property('source').value(); log.info('Turning off lights in empty ' + zoneName); g.V(self).property('lightLevel', 0).iterate()",
    "Hysteresis", "60000"  // Wait 1 minute before turning off
)

lightsOffRule.addEdge("execute", compute, "Priority", "30")

zones.each { zone ->
    zone.addEdge("fire", lightsOffRule, "monitor-property", "occupancy")
}
```

### Rule 3: HVAC Temperature Control

```groovy
hvacCoolingRule = graph.addVertex(
    "type", "rule",
    "label", "hvac-cooling",
    "Condition", "occupancy > 0 && temperature > 24.0 && hvacMode == 'AUTO'",
    "Action", "var zoneName = self.property('source').value(); log.info('Activating cooling in ' + zoneName); g.V(self).property('hvacMode', 'COOLING').iterate()",
    "Hysteresis", "30000"
)

hvacCoolingRule.addEdge("execute", compute, "Priority", "60")

zones.each { zone ->
    zone.addEdge("fire", hvacCoolingRule, "monitor-property", "temperature")
    zone.addEdge("fire", hvacCoolingRule, "monitor-property", "occupancy")
}
```

### Rule 4: Energy Saving Mode

```groovy
energySavingRule = graph.addVertex(
    "type", "rule",
    "label", "energy-saving",
    "Condition", "g.V().hasLabel('zone').has('occupancy', 0).count().next() == g.V().hasLabel('zone').count().next()",
    "Action", "log.info('All zones empty. Activating energy saving mode.'); g.V().hasLabel('zone').property('hvacMode', 'ECO').property('lightLevel', 0).iterate()",
    "Hysteresis", "300000"  // Wait 5 minutes
)

energySavingRule.addEdge("execute", compute, "Priority", "20")

zones.each { zone ->
    zone.addEdge("fire", energySavingRule, "monitor-property", "occupancy")
}
```

### Testing

```groovy
// Someone enters office-1
zones[1].property("occupancy", 1)
// Output: INFO Turning on lights in office-1

// Temperature rises
zones[1].property("temperature", 25.5)
// Output: INFO Activating cooling in office-1

// Everyone leaves all zones
zones.each { it.property("occupancy", 0) }
// Wait 5 minutes...
// Output: INFO All zones empty. Activating energy saving mode.
```

---

## Example 6: Security Event Correlation

### Scenario
Correlate multiple security events to detect potential intrusion attempts.

### Setup

```groovy
compute = graph.addVertex("type", "compute", "label", "security-compute", "Threads", "6")

// Create security sensors
sensors = []
['door-1', 'door-2', 'window-1', 'motion-1'].each { name ->
    sensor = graph.addVertex(
        "type", "security-sensor",
        "label", name,
        "triggered", false,
        "eventCount", 0,
        "lastEvent", 0L
    )
    sensors << sensor
}
```

### Rule 1: Single Sensor Alert

```groovy
singleAlertRule = graph.addVertex(
    "type", "rule",
    "label", "single-alert",
    "Condition", "triggered == true",
    "Action", "var sensorName = self.property('source').value(); log.warn('Security sensor triggered: ' + sensorName); g.V(self).property('eventCount', eventCount + 1).property('lastEvent', System.currentTimeMillis()).iterate()",
    "Hysteresis", "5000"
)

singleAlertRule.addEdge("execute", compute, "Priority", "70")

sensors.each { sensor ->
    sensor.addEdge("fire", singleAlertRule, "monitor-property", "triggered")
}
```

### Rule 2: Multiple Sensors (Intrusion Detection)

```groovy
intrusionRule = graph.addVertex(
    "type", "rule",
    "label", "intrusion-detection",
    "Condition", "g.V().hasLabel('security-sensor').has('triggered', true).count().next() >= 2",
    "Action", "log.error('INTRUSION DETECTED: Multiple sensors triggered'); commands.execute('trigger-alarm'); commands.execute('notify-security')",
    "Hysteresis", "10000"
)

intrusionRule.addEdge("execute", compute, "Priority", "100")

sensors.each { sensor ->
    sensor.addEdge("fire", intrusionRule, "monitor-property", "triggered")
}
```

### Rule 3: Rapid Event Sequence

```groovy
rapidSequenceRule = graph.addVertex(
    "type", "rule",
    "label", "rapid-sequence",
    "Condition", "eventCount > 5 && (System.currentTimeMillis() - lastEvent) < 60000",
    "Action", "var sensorName = self.property('source').value(); log.error('Rapid event sequence on ' + sensorName + ': ' + eventCount + ' events'); commands.execute('investigate-sensor', sensorName)",
    "Hysteresis", "30000"
)

rapidSequenceRule.addEdge("execute", compute, "Priority", "80")

sensors.each { sensor ->
    sensor.addEdge("fire", rapidSequenceRule, "monitor-property", "eventCount")
}
```

### Testing

```groovy
// Single sensor trigger
sensors[0].property("triggered", true)
// Output: WARN Security sensor triggered: door-1

// Multiple sensors (intrusion)
sensors[1].property("triggered", true)
sensors[2].property("triggered", true)
// Output: ERROR INTRUSION DETECTED: Multiple sensors triggered

// Rapid sequence
6.times { sensors[3].property("eventCount", it + 1) }
sensors[3].property("lastEvent", System.currentTimeMillis())
// Output: ERROR Rapid event sequence on motion-1: 6 events
```

---

## Best Practices from Examples

### 1. Use Appropriate Hysteresis

- **Short hysteresis (5-10s)**: Critical alerts requiring immediate action
- **Medium hysteresis (30-60s)**: Important warnings
- **Long hysteresis (5-60min)**: Low-priority notifications or energy-saving

### 2. Set Meaningful Priorities

- **100**: Critical safety/security issues
- **80-90**: Important operational alerts
- **50-70**: Standard monitoring
- **10-40**: Informational or low-priority events

### 3. Combine Multiple Conditions

Use `&&` and `||` to create sophisticated rules that consider multiple factors:

```groovy
"Condition", "temperature > 80 && pressure > 100 && status == 'RUNNING'"
```

### 4. Use Graph Queries Wisely

Avoid expensive queries in conditions. Cache results when possible:

```groovy
// Good: Cache result
"Condition", "var count = g.V().hasLabel('error').count().next(); count > 10"

// Bad: Query twice
"Condition", "g.V().hasLabel('error').count().next() > 10 && g.V().hasLabel('error').count().next() < 100"
```

### 5. Update State in Actions

Use actions to maintain state for future rule evaluations:

```groovy
"Action", "g.V(self).property('lastAlert', System.currentTimeMillis()).property('alertCount', alertCount + 1).iterate()"
```

### 6. Log with Context

Include relevant information in log messages:

```groovy
"Action", "log.warn('Sensor: ' + sensorName + ', Value: ' + temperature + '°C, Threshold: 80°C')"
```

---

## Conclusion

These examples demonstrate the flexibility and power of the WaldOT Rules Engine for various automation and monitoring scenarios. Adapt these patterns to your specific use cases and combine multiple rules for sophisticated behavior.

For more information, see:
- [Quick Start Guide](QUICKSTART.md)
- [JEXL Expressions Guide](JEXL_EXPRESSIONS.md)
- [Architecture Documentation](ARCHITECTURE.md)
- [API Reference](API_REFERENCE.md)

---

*Examples for WaldOT Rules Engine version 0.4.0+*
