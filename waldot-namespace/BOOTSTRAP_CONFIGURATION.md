# WaldOT Bootstrap Configuration Guide

## Overview

The WaldOT bootstrap configuration system has been enhanced to support both legacy line-by-line execution and modern Groovy script mode with automatic format detection.

## Configuration Formats

### Format 1: Line-by-Line Mode (Legacy)

**When Used**: Automatically detected for simple configurations with hash comments (`#`)

**Characteristics**:
- One command per line
- Hash comments: `# This is a comment`
- No variables or functions
- Backward compatible with existing configurations

**Example**:
```groovy
# Legacy configuration format
graph.addVertex('id', 'sensor1', 'label', 'temperature-sensor')
graph.addVertex('id', 'sensor2', 'label', 'pressure-sensor')

# Create edge
sensor1.addEdge('connected-to', sensor2)
```

**Execution**: Each non-comment line is executed independently as a Groovy expression.

### Format 2: Script Mode (Recommended)

**When Used**: Automatically detected when the file contains:
- Function definitions: `def myFunction()`
- Variable assignments: `sensor = ...`
- Groovy comments: `//` or `/* */`
- Multi-line method chains with indentation
- Control structures: `if`, `for`, `while`, `.times {}`
- Closures: `{ ... }`

**Characteristics**:
- Full Groovy script support
- Functions, variables, loops, conditionals
- Groovy-style comments
- Multi-line constructs
- More powerful and flexible

**Example**:
```groovy
// Modern script mode configuration

// Function to create a temperature sensor
def createTempSensor(name, min, max, algorithm) {
  sensor = g.addV('generator')
    .property('type', 'generator')
    .property('label', name)
    .property('Algorithm', algorithm)
    .property('Min', min.toString())
    .property('Max', max.toString())
    .property('Delay', '5000')
    .next()
  
  log.info("Created sensor: ${name}")
  return sensor
}

// Create multiple sensors with a loop
sensors = []
['office', 'warehouse', 'server-room'].each { location ->
  sensor = createTempSensor(
    "temp-${location}",
    '15',
    '30',
    'sinusoidal'
  )
  sensors.add(sensor)
}

// Create compute vertex
compute = g.addV('compute')
  .property('type', 'compute')
  .property('label', 'main-compute')
  .property('Threads', '4')
  .next()

// Create rules for each sensor
sensors.each { sensor ->
  rule = g.addV('rule')
    .property('type', 'rule')
    .property('label', "alarm-${sensor.property('label').value()}")
    .property('Condition', 'temperature > 28.0')
    .property('Action', "log.warn('High temp in ' + location)")
    .property('Priority', '100')
    .next()
  
  // Connect rule to compute
  rule.addEdge('execute', compute, 'Priority', '100')
  
  // Monitor sensor
  sensor.addEdge('fire', rule,
    'monitor-property', 'temperature',
    'active', 'true'
  )
}

log.info("Configuration complete: ${sensors.size()} sensors, ${sensors.size()} rules")
```

**Execution**: The entire file is executed as a single Groovy script.

## Automatic Format Detection

The `SingleFileBootstrapStrategy` automatically analyzes the configuration file content and selects the appropriate execution mode.

**Detection Logic**:

```java
Script Mode is selected if the content contains:
1. Groovy comments: // or /* */
2. Function definitions: def functionName(...)
3. Variable assignments: varName = value (at line start)
4. Multi-line method chains: .property(...)\n  .property(...)
5. Control structures: if (...), for (...), while (...), .times { }
6. Closures with significant content: { ... }

Otherwise, Line-by-Line Mode is used for backward compatibility
```

## Loading Configuration

### Local Files

```bash
# Default location
docker run rossonet/waldot:latest
# Loads file:///waldot/boot.conf

# Custom location with volume mount
docker run \
  -v ./my-config.groovy:/waldot/boot.conf:ro \
  rossonet/waldot:latest

# Custom path via environment variable
docker run \
  -e WALDOT_BOOT_URL=file:///config/custom.groovy \
  -v ./custom.groovy:/config/custom.groovy:ro \
  rossonet/waldot:latest
```

### Remote URLs

**New Feature**: Load configuration from HTTP/HTTPS URLs

```bash
# Load from GitHub
docker run \
  -e WALDOT_BOOT_URL=https://raw.githubusercontent.com/org/repo/main/waldot-config.groovy \
  rossonet/waldot:latest

# Load from web server
docker run \
  -e WALDOT_BOOT_URL=https://config.example.com/waldot/production.groovy \
  rossonet/waldot:latest

# Load from GitLab
docker run \
  -e WALDOT_BOOT_URL=https://gitlab.com/org/repo/-/raw/main/config.groovy \
  rossonet/waldot:latest
```

**Benefits of Remote URLs**:
- Centralized configuration management
- Version control integration (Git)
- Quick deployment without rebuilding Docker images
- Easy configuration updates across multiple instances
- Configuration-as-Code workflows

**Security Considerations**:
- Always use HTTPS for sensitive configurations
- Validate configuration sources
- Consider authentication for private repositories
- Use environment variables for secrets, not hardcoded in config files

## Available Variables in Scripts

When executing in either mode, the following variables are available:

| Variable | Type | Description |
|----------|------|-------------|
| `g` | GraphTraversal | Gremlin graph traversal source |
| `graph` | Graph | TinkerPop graph instance |
| `log` | Logger | SLF4J logger for logging messages |
| `commands` | CommandsFunction | WaldOT console commands |

**Example Usage**:
```groovy
// Use g for graph traversal
sensor = g.addV('generator').property('label', 'my-sensor').next()

// Use graph for graph operations
vertex = graph.addVertex('id', 'test', 'label', 'test-vertex')

// Use log for logging
log.info("Configuration starting...")
log.warn("This is a warning")
log.error("This is an error")

// Use commands to execute WaldOT commands
// result = commands.execute('about')
```

## Migration Guide

### From Line Mode to Script Mode

**Old Format (Line Mode)**:
```groovy
# boot.conf
graph.addVertex('id', 'sensor1', 'label', 'temp', 'value', 20)
graph.addVertex('id', 'sensor2', 'label', 'pressure', 'value', 100)
graph.addVertex('id', 'sensor3', 'label', 'flow', 'value', 50)
```

**New Format (Script Mode)**:
```groovy
// boot.groovy

// Function for reusability
def createSensor(id, label, value) {
  graph.addVertex('id', id, 'label', label, 'value', value)
}

// Cleaner code with loops
[
  ['sensor1', 'temp', 20],
  ['sensor2', 'pressure', 100],
  ['sensor3', 'flow', 50]
].each { config ->
  createSensor(config[0], config[1], config[2])
}

log.info("3 sensors created")
```

**Advantages**:
- More maintainable
- Reusable functions
- Better structure
- Easier to read and modify
- Full Groovy language features

## Best Practices

### 1. Use Script Mode for Complex Configurations

```groovy
// Good: Script mode with functions
def createMonitoringZone(name, tempRange, pressureRange) {
  // Sensor
  sensor = g.addV('generator')
    .property('type', 'generator')
    .property('label', "sensor-${name}")
    .next()
  
  // Rule
  rule = g.addV('rule')
    .property('type', 'rule')
    .property('Condition', "temp > ${tempRange[1]} || pressure > ${pressureRange[1]}")
    .property('Action', "log.warn('Alarm in ${name}')")
    .next()
  
  return [sensor, rule]
}

// Use the function
zones = ['office', 'warehouse', 'production'].collect { name ->
  createMonitoringZone(name, [18, 26], [950, 1050])
}
```

### 2. Use Comments Effectively

```groovy
// ====================================
// Production Environment Configuration
// Version: 2.0
// Last Updated: 2024-03-31
// ====================================

// Section 1: Sensors
// Create temperature sensors for each zone
tempSensors = createTemperatureSensors()

// Section 2: Rules
// Configure monitoring rules
setupMonitoringRules(tempSensors)

// Section 3: Integration
// Enable Gremlin server for external access
enableGremlinServer(8182)
```

### 3. Organize Large Configurations

```groovy
// ====================================
// CONFIGURATION STRUCTURE
// ====================================

// === HELPER FUNCTIONS ===

def createSensor(name, type) {
  // implementation
}

def createRule(sensor, condition) {
  // implementation
}

// === MAIN CONFIGURATION ===

// 1. Create infrastructure
compute = createCompute()

// 2. Create sensors
sensors = createAllSensors()

// 3. Create rules
rules = createAllRules(sensors, compute)

// 4. Connect everything
connectComponents(sensors, rules, compute)

// === LOGGING ===

log.info("Configuration Summary:")
log.info("  Sensors: ${sensors.size()}")
log.info("  Rules: ${rules.size()}")
log.info("  Compute threads: ${compute.property('Threads').value()}")
```

### 4. Use Logging for Debugging

```groovy
// Start
log.info("=== Configuration Start ===")

// During execution
sensors.each { sensor ->
  log.debug("Processing sensor: ${sensor.property('label').value()}")
}

// End
log.info("=== Configuration Complete ===")
log.info("Created ${count} vertices, ${edgeCount} edges")
```

### 5. Handle Errors Gracefully

```groovy
def safeCreateSensor(name) {
  try {
    sensor = g.addV('generator')
      .property('type', 'generator')
      .property('label', name)
      .next()
    log.info("Created sensor: ${name}")
    return sensor
  } catch (Exception e) {
    log.error("Failed to create sensor ${name}: ${e.message}")
    return null
  }
}

// Use with null-safe operations
sensors = ['s1', 's2', 's3']
  .collect { safeCreateSensor(it) }
  .findAll { it != null }  // Filter out failures
```

## Testing Configurations

### Test Locally

```bash
# Create test configuration
cat > test-config.groovy << 'EOF'
// Test configuration
sensor = graph.addVertex('id', 'test', 'label', 'test-sensor')
log.info("Test sensor created: ${sensor}")
EOF

# Run WaldOT with test config
docker run \
  -v ./test-config.groovy:/waldot/boot.conf:ro \
  -p 12686:12686 \
  --name waldot-test \
  rossonet/waldot:latest

# Check logs
docker logs waldot-test

# Cleanup
docker stop waldot-test
docker rm waldot-test
```

### Debug Script Mode Detection

Enable debug logging to see which mode is selected:

```bash
docker run \
  -v ./boot.conf:/waldot/boot.conf:ro \
  -e JAVA_OPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG" \
  rossonet/waldot:latest
```

Look for log messages:
- `Bootstrap configuration loaded in SCRIPT MODE`
- `Bootstrap configuration loaded in LINE-BY-LINE MODE`

## Examples

### Example 1: Simple Development Setup

```groovy
// dev-config.groovy
// Quick development environment

// Create a few test sensors
g.addV('generator')
  .property('type', 'generator')
  .property('label', 'dev-temp')
  .property('Algorithm', 'random')
  .next()

g.addV('generator')
  .property('type', 'generator')
  .property('label', 'dev-pressure')
  .property('Algorithm', 'sinusoidal')
  .next()

log.info("Development environment ready")
```

### Example 2: Production Multi-Zone

See the extensive examples in the [User Manual](../docs/guide/docs/manuale_utente.md).

## Troubleshooting

### Configuration Not Loading

**Check logs**:
```bash
docker logs <container-id> 2>&1 | grep -i "bootstrap"
```

**Common issues**:
1. **URL format**: Ensure proper URL format (`file:///` or `https://`)
2. **File permissions**: Check file is readable
3. **Network**: For remote URLs, verify network connectivity
4. **Syntax errors**: Check Groovy syntax in script mode

### Script vs Line Mode Detection

If the wrong mode is detected:

**Force Script Mode**:
- Add a Groovy comment: `// Script mode`
- Or use a variable: `dummy = true`

**Force Line Mode**:
- Remove all `//` comments
- Remove variable assignments
- Use only `#` comments
- Keep one command per line

### Debugging Script Execution

Add debug logging:

```groovy
log.info("=== Script Start ===")

def myFunction() {
  log.debug("Entering myFunction")
  // ...
  log.debug("Exiting myFunction")
}

log.info("=== Script End ===")
```

## Related Documentation

- [User Manual](../docs/guide/docs/manuale_utente.md) - Complete bootstrap examples
- [waldot-app Configuration Guide](../waldot-app/CONFIGURATION.md) - All configuration parameters
- [waldot-app Environment Variables](../waldot-app/ENVIRONMENT_VARIABLES.md) - Docker environment variables

---

**WaldOT Bootstrap Configuration** - Flexible configuration system with automatic format detection  
*Rossonet s.c.a r.l.* - https://www.rossonet.net
