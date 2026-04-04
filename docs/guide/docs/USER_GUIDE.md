# WaldOT User Guide

**Operating and configuring WaldOT for end users and system administrators**

## Quick Start

### Docker Deployment (Recommended)

```bash
# Pull latest image
docker pull rossonet/waldot:latest

# Run with default settings
docker run -p 12686:12686 -p 8443:8443 rossonet/waldot:latest

# Access
# OPC UA: opc.tcp://localhost:12686/waldot
# HTTPS: https://localhost:8443
```

### Production Deployment

```bash
docker run \
  -p 4840:4840 \
  -p 8443:8443 \
  -v /secure/waldot:/app/.security \
  -v /config/boot.conf:/waldot/boot.conf:ro \
  -e WALDOT_TCP_PORT=4840 \
  -e WALDOT_APPLICATION_NAME="Production Server" \
  -e WALDOT_ANONYMOUS_ACCESS=false \
  -e WALDOT_FACTORY_USERNAME=admin \
  -e WALDOT_FACTORY_PASSWORD=${SECURE_PASSWORD} \
  rossonet/waldot:latest
```

---

## Configuration

### Environment Variables

| Variable                  | Default                    | Description                            |
| ------------------------- | -------------------------- | -------------------------------------- |
| `WALDOT_TCP_PORT`         | 12686                      | OPC UA TCP port                        |
| `WALDOT_HTTPS_PORT`       | 8443                       | HTTPS port                             |
| `WALDOT_APPLICATION_NAME` | "WaldOT OPCUA server"      | Server name                            |
| `WALDOT_ANONYMOUS_ACCESS` | true                       | Allow anonymous connections            |
| `WALDOT_FACTORY_USERNAME` | "admin"                    | Admin username                         |
| `WALDOT_FACTORY_PASSWORD` | "password123"              | Admin password ⚠️ CHANGE IN PRODUCTION |
| `WALDOT_BOOT_URL`         | "file:///waldot/boot.conf" | Bootstrap configuration                |

**Complete reference**: [Environment Variables](../../../waldot-app/ENVIRONMENT_VARIABLES.md)

### Bootstrap Configuration

Create `boot.conf` with Groovy script:

```groovy
// Create temperature sensors
def createSensor(name, min, max) {
  g.addV('generator')
    .property('type', 'generator')
    .property('label', name)
    .property('Algorithm', 'sinusoidal')
    .property('Min', min.toString())
    .property('Max', max.toString())
    .property('Delay', '5000')
    .next()
}

// Create sensors
createSensor('temp-office', '18', '26')
createSensor('temp-warehouse', '10', '30')
createSensor('temp-server-room', '18', '24')

log.info("Sensors configured")
```

Mount configuration:

```bash
docker run \
  -v ./boot.conf:/waldot/boot.conf:ro \
  rossonet/waldot:latest
```

Or use remote URL:

```bash
docker run \
  -e WALDOT_BOOT_URL=https://example.com/config.groovy \
  rossonet/waldot:latest
```

---

## Using OPC UA Interface

### Connect with OPC UA Client

**Recommended clients**:

- **UaExpert** (Windows): https://www.unified-automation.com/products/development-tools/uaexpert.html
- **Prosys Browser** (Cross-platform): https://www.prosysopc.com/products/opc-ua-browser/

**Connection**:

1. Open OPC UA client
2. Add server: `opc.tcp://localhost:12686/waldot`
3. Security: None (development) or Sign & Encrypt (production)
4. Credentials: admin / password123 (change in production!)

### Browse Address Space

```
Objects
└── Gremlin Engine
    ├── Administration
    │   └── (asset management)
    ├── Commands
    │   ├── about
    │   ├── help
    │   └── query
    └── (your vertices)
        ├── sensor1
        │   ├── label
        │   ├── temperature
        │   └── status
        └── sensor2
            └── ...
```

### Read/Write Values

- **Read**: Browse to variable node, view current value
- **Write**: Double-click value, enter new value
- **Subscribe**: Right-click node → Create Subscription

---

## Gremlin Query Basics

### Via OPC UA Method

1. Browse to `Commands/query`
2. Call method with Gremlin query as parameter
3. View result in output

**Example queries**:

```groovy
// Count all vertices
g.V().count()

// Find all sensors
g.V().has('type', 'sensor').values('label')

// Find sensors in alarm
g.V().has('type', 'sensor').has('status', 'alarm').values('label')

// Get sensor value
g.V().has('label', 'sensor1').values('temperature')
```

### Via Gremlin Console (Advanced)

If TinkerPop plugin is enabled:

```bash
# Install Gremlin Console
wget https://downloads.apache.org/tinkerpop/3.7.0/apache-tinkerpop-gremlin-console-3.7.0-bin.zip
unzip apache-tinkerpop-gremlin-console-3.7.0-bin.zip
cd apache-tinkerpop-gremlin-console-3.7.0

# Connect
bin/gremlin.sh
:remote connect tinkerpop.server conf/remote.yaml
:remote console

# Query
g.V().count()
```

---

## Working with Plugins

### Generator Plugin (Data Simulation)

**Create simulated sensor**:

```groovy
// Via bootstrap config or OPC UA method
sensor = graph.addVertex(
    "type", "generator",
    "label", "my-sensor",
    "Algorithm", "sinusoidal",  // or: random, incremental, triangular
    "Min", "0",
    "Max", "100",
    "Delay", "1000"  // Update every 1 second
)
```

**Algorithms**:

- `incremental`: Linear increase
- `decremental`: Linear decrease
- `random`: Random values
- `sinusoidal`: Sine wave
- `triangular`: Triangle wave
- `stopped`: Constant value

**Change algorithm at runtime**:

```groovy
sensor.property("Algorithm", "random")
sensor.property("Delay", "5000")  // Change update interval
```

### Rules Engine Plugin (Automation)

**Create monitoring rule**:

```groovy
// 1. Create compute manager
compute = graph.addVertex(
    "type", "compute",
    "label", "main-compute",
    "Threads", "4"
)

// 2. Create rule
rule = graph.addVertex(
    "type", "rule",
    "label", "temp-alarm",
    "Condition", "temperature > 80.0",
    "Action", "log.warn('High temperature: ' + temperature)",
    "Priority", "100",
    "Hysteresis", "5000"
)

// 3. Connect rule to compute
rule.addEdge("execute", compute, "Priority", "100")

// 4. Monitor sensor
sensor.addEdge("fire", rule,
    "monitor-property", "temperature",
    "active", "true"
)
```

**Monitor rule execution**:

```groovy
// Check metrics
queueSize = rule.property("Queue").value()
totalEvents = rule.property("Total").value()
executed = rule.property("Executed").value()
errors = rule.property("Errors").value()
```

---

## Docker Compose Examples

### Basic Setup

```yaml
version: "3.8"

services:
  waldot:
    image: rossonet/waldot:latest
    ports:
      - "12686:12686"
      - "8443:8443"
    environment:
      - WALDOT_APPLICATION_NAME=My WaldOT Server
    volumes:
      - waldot-security:/app/.security
    restart: always

volumes:
  waldot-security:
```

### Production Setup

```yaml
version: "3.8"

services:
  waldot:
    image: rossonet/waldot:latest
    ports:
      - "4840:4840"
      - "8443:8443"
    environment:
      - WALDOT_TCP_PORT=4840
      - WALDOT_APPLICATION_NAME=Production Server
      - WALDOT_ANONYMOUS_ACCESS=false
      - WALDOT_FACTORY_USERNAME=admin
      - WALDOT_FACTORY_PASSWORD=${ADMIN_PASSWORD}
      - WALDOT_EXEC_COMMAND_EXECUTABLE=false
    volumes:
      - waldot-security:/app/.security
      - ./boot.conf:/waldot/boot.conf:ro
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "https://localhost:8443"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  waldot-security:
```

**Complete examples**: [Examples Directory](../../examples/)

---

## Monitoring and Troubleshooting

### View Logs

```bash
# Docker logs
docker logs <container-name>

# Follow logs
docker logs -f <container-name>

# Last 100 lines
docker logs --tail 100 <container-name>
```

### Health Check

```bash
# Test OPC UA port
telnet localhost 12686

# Test HTTPS port
curl -k https://localhost:8443

# Check container status
docker ps
docker inspect <container-name>
```

### Common Issues

**Port already in use**:

```bash
# Find process using port
lsof -i :12686

# Use different port
-e WALDOT_TCP_PORT=4840
```

**Connection refused**:

- Check firewall rules
- Verify Docker port mapping: `-p 12686:12686`
- Check bind addresses: `-e WALDOT_BIND_ADDRESSES=0.0.0.0`

**Authentication failures**:

- Verify credentials match
- Check anonymous access setting
- Review OPC UA client security policy

**Certificate errors**:

- Ensure DNS name matches: `-e WALDOT_DNS_ADDRESS_CERT=your-hostname`
- Regenerate certificates: `rm -rf .security/` and restart

---

## Security Best Practices

### Production Checklist

- [ ] Change default password: `WALDOT_FACTORY_PASSWORD`
- [ ] Disable anonymous access: `WALDOT_ANONYMOUS_ACCESS=false`
- [ ] Disable exec command: `WALDOT_EXEC_COMMAND_EXECUTABLE=false`
- [ ] Use TLS/SSL with valid certificates
- [ ] Restrict bind addresses to specific interfaces
- [ ] Mount security directory as Docker volume
- [ ] Use secrets management (Docker secrets, Kubernetes secrets)
- [ ] Regular security updates (pull latest image)
- [ ] Monitor logs for suspicious activity

### Secure Password Management

**Docker Secrets** (recommended):

```bash
# Create secret
echo "YourSecurePassword123!" | docker secret create waldot_password -

# Use in compose
services:
  waldot:
    secrets:
      - waldot_password
    environment:
      - WALDOT_FACTORY_PASSWORD_FILE=/run/secrets/waldot_password

secrets:
  waldot_password:
    external: true
```

---

## Backup and Recovery

### Backup Security Certificates

```bash
# Backup security directory
docker cp <container-name>:/app/.security ./backup-security/

# Or use volume backup
docker run --rm \
  -v waldot-security:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/security-backup.tar.gz /data
```

### Backup Graph Database

```bash
# Backup graph data (if using persistent storage)
docker cp <container-name>:/app/data ./backup-data/
```

### Restore

```bash
# Restore security certificates
docker cp ./backup-security/ <container-name>:/app/.security

# Or restore volume
docker run --rm \
  -v waldot-security:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/security-backup.tar.gz -C /
```

---

## Performance Tuning

### JVM Options

```bash
docker run \
  -e JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC" \
  rossonet/waldot:latest
```

### Resource Limits

```yaml
services:
  waldot:
    image: rossonet/waldot:latest
    deploy:
      resources:
        limits:
          cpus: "4"
          memory: 4G
        reservations:
          cpus: "2"
          memory: 2G
```

---

## Additional Resources

- [Configuration Reference](../../../waldot-app/CONFIGURATION.md)
- [Environment Variables](../../../waldot-app/ENVIRONMENT_VARIABLES.md)
- [Bootstrap Configuration](../../../waldot-namespace/BOOTSTRAP_CONFIGURATION.md)
- [Developer Guide](DEVELOPER_GUIDE.md)
- [Architecture Overview](ARCHITECTURE_OVERVIEW.md)
- [Examples](../../examples/)

---

**Support**: [GitHub Issues](https://github.com/rossonet/waldot/issues)
