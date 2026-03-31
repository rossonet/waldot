# WaldOT Application Documentation

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Configuration](#configuration)
4. [Command-Line Parameters](#command-line-parameters)
5. [Docker Deployment](#docker-deployment)
6. [Examples](#examples)
7. [Development](#development)
8. [Troubleshooting](#troubleshooting)

## Overview

The **waldot-app** module is the main application component of the WaldOT project. It provides a deployable OPC UA server that bridges industrial automation (OT) with Apache TinkerPop graph databases.

### Key Features

- **Full OPC UA Server**: Complete OPC UA server implementation using Eclipse Milo
- **Graph Integration**: Real-time bidirectional synchronization with TinkerPop graphs
- **Plugin System**: Extensible architecture with auto-discovery of plugins
- **Command-Line Configuration**: Flexible configuration via Picocli annotations
- **Docker Ready**: Optimized for containerized deployments
- **Production Grade**: Health checks, logging, resource management

### Use Cases

- **Digital Twins**: Create live digital representations of industrial equipment
- **Predictive Maintenance**: Analyze equipment behavior patterns using graph queries
- **Process Optimization**: Real-time monitoring and rule-based automation
- **Data Integration**: Bridge OT systems with modern IT infrastructures

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         waldot-app                               │
│                                                                  │
│  ┌────────────────┐              ┌─────────────────────┐       │
│  │  MainAgent     │              │   WaldotRunner      │       │
│  │  (Entry Point) │  ──────────> │   (Configuration)   │       │
│  └────────────────┘              └──────────┬──────────┘       │
│                                              │                   │
│                                              ↓                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            WaldotOpcUaServer                              │  │
│  │  ┌────────────────┐    ┌────────────────────┐           │  │
│  │  │  OPC UA        │←→  │  TinkerPop Graph   │           │  │
│  │  │  (Milo)        │    │  (Gremlin)         │           │  │
│  │  └────────────────┘    └────────────────────┘           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                              │                   │
│                                              ↓                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Plugin System                            │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────────┐           │  │
│  │  │Generator │  │  Rules   │  │  TinkerPop   │           │  │
│  │  │          │  │  Engine  │  │   Server     │           │  │
│  │  └──────────┘  └──────────┘  └──────────────┘           │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Key Classes

#### MainAgent
- **Purpose**: Application entry point
- **Responsibilities**: 
  - Initialize Picocli command-line processing
  - Bootstrap WaldotRunner
  - Handle application lifecycle
- **Location**: `net.rossonet.agent.MainAgent`

#### WaldotRunner
- **Purpose**: Server configuration and startup
- **Responsibilities**:
  - Parse command-line parameters via Picocli
  - Create and populate configurations
  - Initialize OPC UA server
  - Start namespace and plugins
- **Location**: `net.rossonet.agent.WaldotRunner`

#### Configuration Classes
- **DefaultOpcUaConfiguration**: OPC UA server settings (waldot-namespace module)
- **DefaultHomunculusConfiguration**: WaldOT application settings (waldot-namespace module)

## Configuration

### Configuration Sources (Priority Order)

1. **Command-line arguments**: Highest priority
2. **Environment variables**: Medium priority  
3. **Default values**: Lowest priority (fallback)

### Configuration Categories

#### OPC UA Server Configuration
Controls how the OPC UA server presents itself and handles network communication:

- Server identity (name, manufacturer, product)
- Network binding (ports, addresses, hostnames)
- Security (certificates, authentication)
- Endpoint configuration

#### WaldOT Application Configuration
Controls application-specific behavior:

- Authentication and authorization
- Graph namespace and structure
- Node organization
- Command configuration
- Bootstrap settings

## Command-Line Parameters

### Quick Reference

```bash
java -jar waldot-app.jar [OPTIONS]
```

### OPC UA Server Parameters

| Parameter | Short | Type | Default | Description |
|-----------|-------|------|---------|-------------|
| `--application-name` | `-an` | String | `WaldOT OPCUA server` | Server application name |
| `--tcp-port` | `-tp` | Integer | `12686` | TCP port for OPC UA binary protocol |
| `--https-port` | `-hp` | Integer | `8443` | HTTPS port for web services |
| `--bind-addresses` | `-ba` | String | `0.0.0.0` | Comma-separated bind addresses |
| `--bind-hostname` | `-bh` | String | `127.0.0.1` | Hostname for endpoint URLs |
| `--dns-address-cert` | `-dc` | String | `127.0.0.1` | DNS address for SSL certificates |
| `--endpoint-path` | `-ep` | String | `/waldot` | URL path for endpoints |
| `--product-name` | `-pn` | String | `WaldOT` | Product name in BuildInfo |
| `--product-uri` | `-pu` | String | `urn:rossonet:waldot:uaserver` | Product URI identifier |
| `--manufacturer-name` | `-mn` | String | `Rossonet s.c.a r.l.` | Manufacturer name |
| `--security-dir` | `-sd` | String | `.security` | Security certificates directory |
| `--build-number` | `-bn` | String | `w001` | Build number identifier |

### WaldOT Application Parameters

| Parameter | Short | Type | Default | Description |
|-----------|-------|------|---------|-------------|
| `--anonymous-access` | `-aa` | Boolean | `true` | Allow anonymous connections |
| `--factory-username` | `-fu` | String | `admin` | Default admin username |
| `--factory-password` | `-fp` | String | `password123` | Default admin password |
| `--namespace-uri` | `-nu` | String | `urn:rossonet:waldot:engine` | OPC UA namespace URI |
| `--help-directory` | `-hd` | String | `/app/help` | Help documentation path |
| `--boot-url` | `-bu` | String | `file:///waldot/boot.conf` | Bootstrap configuration file |

### Node Configuration Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--root-node-id` | String | `waldot` | Root node identifier |
| `--root-node-browse-name` | String | `Gremlin Engine` | Root node browse name |
| `--root-node-display-name` | String | `Gremlin Engine` | Root node display name |
| `--asset-node-id` | String | `aas` | Asset root node identifier |
| `--asset-node-browse-name` | String | `Administration` | Asset node browse name |
| `--asset-node-display-name` | String | `Administration` | Asset node display name |
| `--interface-node-id` | String | `cmd` | Interface node identifier |
| `--interface-node-browse-name` | String | `Commands` | Interface node browse name |
| `--interface-node-display-name` | String | `Commands` | Interface node display name |

### Command Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `--about-command-label` | `about` | About command label |
| `--about-command-description` | `info about this software` | About command description |
| `--about-command-executable` | `true` | Enable about command |
| `--exec-command-label` | `exec` | Exec command label |
| `--exec-command-description` | `run system command` | Exec command description |
| `--exec-command-executable` | `true` | Enable exec command |
| `--help-command-label` | `help` | Help command label |
| `--help-command-description` | `list available commands` | Help command description |
| `--help-command-executable` | `true` | Enable help command |
| `--waldot-command-label` | `query` | Gremlin query command label |
| `--waldot-command-description` | `run Gremlin query` | Query command description |
| `--waldot-command-executable` | `true` | Enable query command |

### Standard Picocli Options

| Parameter | Description |
|-----------|-------------|
| `--help`, `-h` | Display help information |
| `--version`, `-V` | Display version information |

## Docker Deployment

### Basic Docker Run

```bash
docker run -p 12686:12686 -p 8443:8443 rossonet/waldot:latest
```

### Custom Configuration

```bash
docker run \
  -p 4840:4840 \
  -p 8080:8080 \
  rossonet/waldot:latest \
  --tcp-port 4840 \
  --https-port 8080 \
  --anonymous-access false \
  --factory-username myuser \
  --factory-password mypassword
```

### Environment Variables

Picocli parameters can also be set via environment variables:

```bash
docker run \
  -e WALDOT_TCP_PORT=4840 \
  -e WALDOT_ANONYMOUS_ACCESS=false \
  -p 4840:4840 \
  rossonet/waldot:latest
```

### Docker Compose

See the [examples directory](../examples/README.md) for complete Docker Compose configurations.

### Volume Mounts

#### Security Certificates
```bash
docker run \
  -v ./security:/app/.security \
  -p 12686:12686 \
  rossonet/waldot:latest
```

#### Bootstrap Configuration
```bash
docker run \
  -v ./boot.conf:/waldot/boot.conf:ro \
  -p 12686:12686 \
  rossonet/waldot:latest
```

#### Help Documentation
```bash
docker run \
  -v ./help:/app/help:ro \
  -p 12686:12686 \
  rossonet/waldot:latest
```

## Examples

The `examples/` directory contains ready-to-use Docker Compose configurations:

1. **default**: Basic setup with default configuration
2. **custom-ports**: Custom TCP and HTTPS ports
3. **secure**: Authentication required, no anonymous access
4. **production**: Production-ready with volumes and monitoring
5. **multiple**: Multiple WaldOT instances on different ports

See [examples/README.md](../examples/README.md) for detailed documentation.

## Development

### Building from Source

```bash
# Build the entire project
./gradlew clean build

# Build only waldot-app
./gradlew waldot-app:build

# Create fat JAR
./gradlew waldot-app:shadowJar

# Create distribution
./gradlew waldot-app:distTar
```

### Running Locally

```bash
# Run with Gradle
./gradlew waldot-app:run

# Run JAR directly
java -jar waldot-app/build/libs/waldot-app-*-all.jar
```

### Running Tests

```bash
# Run all tests
./gradlew waldot-app:test

# Run specific test
./gradlew waldot-app:test --tests "net.rossonet.agent.WaldotRunnerPicocliTest"
```

### Adding Picocli Parameters

To add new configuration parameters:

1. Add field to `WaldotRunner` class
2. Add `@Option` annotation with names and description
3. Update `runWaldot()` method to apply the parameter
4. Add getter/setter if needed
5. Update corresponding configuration class (DefaultOpcUaConfiguration or DefaultHomunculusConfiguration)
6. Add test case in `WaldotRunnerPicocliTest`
7. Update this documentation

**Example**:

```java
/**
 * Custom timeout in milliseconds.
 */
@Option(names = {"--timeout", "-t"}, 
        description = "Connection timeout in milliseconds", 
        defaultValue = "30000")
protected int connectionTimeout;
```

## Troubleshooting

### Common Issues

#### Port Already in Use

**Error**: `Address already in use`

**Solution**: 
```bash
# Find process using port
lsof -i :12686

# Kill process or use different port
java -jar waldot-app.jar --tcp-port 4840
```

#### Permission Denied on Security Directory

**Error**: `Permission denied: .security`

**Solution**:
```bash
mkdir -p .security
chmod 700 .security
```

#### Cannot Connect from OPC UA Client

**Symptoms**: Connection refused or timeout

**Solutions**:
1. Check firewall rules
2. Verify bind addresses: `--bind-addresses 0.0.0.0`
3. Ensure correct hostname: `--bind-hostname <your-hostname>`
4. Verify port mapping in Docker

#### Certificate Validation Errors

**Error**: `Certificate validation failed`

**Solution**:
```bash
# Regenerate certificates
rm -rf .security
# Restart server to generate new certificates

# Or configure correct DNS name
java -jar waldot-app.jar --dns-address-cert myserver.local
```

### Debug Logging

Enable debug logging:

```bash
java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug \
  -jar waldot-app.jar
```

### Health Check

Test if server is running:

```bash
# Check TCP port
telnet localhost 12686

# Check HTTPS port
curl -k https://localhost:8443
```

## Advanced Topics

### Custom Plugins

WaldOT supports custom plugins. See the [Plugin Development Guide](../../docs/guide/docs/manuale_plugins.md) for details.

### Bootstrap Configuration

Create a `boot.conf` file with graph initialization commands:

```groovy
// Create a temperature sensor
g.addV('generator')
  .property('type', 'generator')
  .property('Algorithm', 'sinusoidal')
  .property('Min', '18')
  .property('Max', '26')

// Create monitoring rule
g.addV('rule')
  .property('Condition', 'temperature > 25')
  .property('Action', "log.warn('High temp')")
```

### Performance Tuning

#### JVM Options

```bash
java -Xmx4g -Xms2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar waldot-app.jar
```

#### Virtual Threads

WaldOT uses Java 21 virtual threads for massive concurrency with minimal overhead.

### Security Best Practices

1. **Change default passwords** in production
2. **Disable anonymous access** for sensitive deployments
3. **Use TLS/SSL certificates** from trusted CA
4. **Mount security directory** as Docker volume
5. **Restrict network access** with firewalls
6. **Regularly update** Docker images

## API Reference

### Java API

```java
// Programmatic configuration
WaldotRunner runner = new WaldotRunner();
runner.setTcpBindPort(4840);
runner.setAnonymousAccessAllowed(false);
runner.call();
```

### REST API

Available at `https://localhost:8443/api` (coming soon)

### Gremlin API

Connect with Gremlin clients (via waldot-plugin-tinkerpop):
```groovy
:remote connect tinkerpop.server conf/remote.yaml
g.V().count()
```

## Support and Resources

- **GitHub**: https://github.com/rossonet/waldot
- **Issues**: https://github.com/rossonet/waldot/issues
- **Discussions**: https://github.com/rossonet/waldot/discussions
- **Docker Hub**: https://hub.docker.com/r/rossonet/waldot
- **Maven Central**: https://central.sonatype.com/search?q=net.rossonet.waldot

## License

WaldOT is licensed under the Apache License 2.0. See [LICENSE](../../LICENSE) for details.

## Contributing

Contributions are welcome! Please see [AGENT.md](../../AGENT.md) for guidelines.

---

**Developed by Rossonet s.c.a r.l.** - https://www.rossonet.net
