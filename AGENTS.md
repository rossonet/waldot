# OpenCode Configuration for WaldOT Project

This file contains custom commands and configurations for OpenCode to work effectively with the WaldOT Gradle project.

## Project Overview

WaldOT is an experimental integration between Apache TinkerPop and Eclipse Milo OPCUA library. It's a multi-module Gradle project with the following structure:

- **waldot-api**: Core API module
- **waldot-deps**: Dependencies module  
- **waldot-app**: Main application
- **waldot-client**: Client application (in clients/waldot-client)
- **wotctl**: Control client (in clients/wotctl)
- **waldot-plugin-\***: Various plugins (in plugins/)
- **zenoh-agent-lib**: Zenoh agent library
- **zenoh-acme**: Zenoh ACME client

## Custom Commands

### Build Commands
```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :waldot-api:build
./gradlew :waldot-app:build
./gradlew :waldot-client:build
./gradlew :wotctl:build

# Clean build
./gradlew clean build

# Build without problematic zenoh-acme module
./gradlew build -x :zenoh-acme:build
```

### Test Commands
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :waldot-api:test
./gradlew :waldot-deps:test

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Code Quality Commands
```bash
# Generate Javadoc
./gradlew javadoc

# Run checkstyle (if configured)
./gradlew check

# Generate sources and javadoc jars
./gradlew sourcesJar javadocJar
```

### Publishing Commands
```bash
# Publish to local repository
./gradlew publishToMavenLocal

# Publish to Maven Central (requires credentials)
./gradlew :waldot-client:publish :waldot-api:publish :waldot-deps:publish :waldot-namespace:publish :waldot-plugin-generator:publish :waldot-plugin-os:publish :waldot-plugin-tinkerpop:publish :waldot-plugin-zenoh:publish :zenoh-agent-lib:publish

# Build distribution
./gradlew distTar distZip

# Build shadow JAR (fat jar with all dependencies)
./gradlew :waldot-app:shadowJar
# Alternative equivalent command from GitHub Actions:
./gradlew clean waldot-app:shadowJar -Dorg.gradle.daemon=false

# Build native image with GraalVM (waldot-app only)
./gradlew :waldot-app:nativeCompile

# Build Docker images (multi-platform)
./gradlew :waldot-app:docker
./gradlew :waldot-app:dockerBuild
```

### Development Commands
```bash
# Eclipse project setup
./gradlew eclipse

# View project information
./gradlew view

# List all available tasks (limited due to zenoh-acme issues)
./gradlew :waldot-api:tasks --group=build
./gradlew :waldot-api:tasks --group=verification
./gradlew :waldot-api:tasks --group=publishing

# List tasks for specific modules
./gradlew :waldot-deps:tasks
./gradlew :waldot-app:tasks
./gradlew :waldot-client:tasks
./gradlew :waldot-plugin-tinkerpop:tasks
```

### Module-Specific Commands

#### API Module
```bash
# Build API with all artifacts
./gradlew :waldot-api:build :waldot-api:sourcesJar :waldot-api:javadocJar

# Test API with coverage
./gradlew :waldot-api:test :waldot-api:jacocoTestReport

# Generate API documentation
./gradlew :waldot-api:javadoc

# View API dependencies
./gradlew :waldot-api:dependencies
```

#### Namespace Module
```bash
# Build namespace and OPCUA graph components
./gradlew :waldot-namespace:build :waldot-namespace:sourcesJar :waldot-namespace:javadocJar

# Test namespace functionality
./gradlew :waldot-namespace:test

# View namespace implementation details
./gradlew :waldot-api:dependencies
```

#### Plugin Modules
```bash
# Build all plugins
./gradlew :waldot-plugin-tinkerpop:build :waldot-plugin-zenoh:build :waldot-plugin-os:build :waldot-plugin-behavior-tree:build :waldot-plugin-reinforcement-learning:build :waldot-plugin-rules-engine:build :waldot-plugin-generator:build

# Build specific plugin
./gradlew :waldot-plugin-tinkerpop:build

# Test specific plugin
./gradlew :waldot-plugin-tinkerpop:test

# Test all plugins (follows GitHub Actions pattern)
./gradlew waldot-plugin-tinkerpop:test waldot-plugin-zenoh:test waldot-plugin-os:test waldot-plugin-behavior-tree:test waldot-plugin-reinforcement-learning:test waldot-plugin-rules-engine:test waldot-plugin-generator:test
```

#### Client Applications
```bash
# Build client applications
./gradlew :waldot-client:build :wotctl:build

# Build individual clients
./gradlew :waldot-client:build
./gradlew :wotctl:build

# Run applications (if configured)
./gradlew :waldot-app:run
./gradlew :wotctl:run

# Test client applications (follows GitHub Actions pattern)
./gradlew waldot-client:test wotctl:test
```

#### Zenoh Modules
```bash
# Build zenoh agent library
./gradlew :zenoh-agent-lib:build :zenoh-agent-lib:sourcesJar :zenoh-agent-lib:javadocJar

# Test zenoh agent library
./gradlew :zenoh-agent-lib:test

# Build zenoh ACME client
./gradlew :zenoh-acme:build

# Test zenoh ACME client
./gradlew :zenoh-acme:test
```

## Known Issues

1. **Javadoc Warnings**: Many Javadoc warnings due to @Author tags and HTML validation errors
   - These are warnings only and don't affect functionality
   - Use `./gradlew javadoc -x :waldot-api:javadoc` to skip problematic modules if needed

2. **Java Version**: Project uses Java 21
   - Ensure JDK 21 is installed and active
   - Use `./gradlew -version` to verify Gradle and Java versions

3. **Gradle Deprecation Warnings**: Project uses deprecated Gradle features incompatible with Gradle 9.0
   - Consider upgrading to Gradle 9.x when possible
   - Use `--warning-mode all` to see specific deprecation warnings

## GitHub Actions Testing Strategy

The project uses a comprehensive testing approach with individual test jobs for each module:

### Test Jobs (`test-on-master-with-gradle.yml`)
- `test_api`: Core API testing
- `test_namespace`: Namespace functionality
- `test_client`: Client application testing  
- `test_app`: Main application testing
- `test_plugin-*`: Individual plugin testing for all plugins
- All tests use JDK 21 Temurin with Gradle daemon disabled

### Test Execution Pattern
```bash
# Each module tested individually (following GitHub Actions pattern)
./gradlew waldot-api:test -Dorg.gradle.daemon=false --info
./gradlew waldot-namespace:test -Dorg.gradle.daemon=false --info
./gradlew waldot-client:test -Dorg.gradle.daemon=false --info
./gradlew waldot-app:test -Dorg.gradle.daemon=false --info
./gradlew waldot-plugin-tinkerpop:test -Dorg.gradle.daemon=false --info
# ... for all plugins
```

## Testing Strategy

Now that GraalVM issues are resolved, comprehensive testing is possible:

### GitHub Actions Testing Pattern (from test-on-master-with-gradle.yml)
```bash
# Each module tested individually with JDK 21 Temurin and daemon disabled
./gradlew waldot-api:test -Dorg.gradle.daemon=false --info
./gradlew waldot-namespace:test -Dorg.gradle.daemon=false --info
./gradlew waldot-client:test -Dorg.gradle.daemon=false --info
./gradlew waldot-app:test -Dorg.gradle.daemon=false --info
./gradlew waldot-plugin-tinkerpop:test -Dorg.gradle.daemon=false --info
./gradlew waldot-plugin-zenoh:test -Dorg.gradle.daemon=false --info
./gradlew waldot-plugin-os:test -Dorg.gradle.daemon=false --info
./gradlew waldot-plugin-behavior-tree:test -Dorg.gradle.daemon=false --info
./gradlew waldot-plugin-reinforcement-learning:test -Dorg.gradle.daemon=false --info
./gradlew waldot-plugin-rules-engine:test -Dorg.gradle.daemon=false --info
./gradlew waldot-plugin-generator:test -Dorg.gradle.daemon=false --info
./gradlew waldot-plugin-tinkerpop:test -Dorg.gradle.daemon=false --info
./gradlew wotctl:test -Dorg.gradle.daemon=false --info
./gradlew zenoh-agent-lib:test -Dorg.gradle.daemon=false --info
./gradlew zenoh-acme:test -Dorg.gradle.daemon=false --info
```

### Local Testing Commands
```bash
# Test all modules
./gradlew test

# Test specific module
./gradlew :waldot-api:test
./gradlew :waldot-app:test
./gradlew :waldot-client:test
./gradlew :wotctl:test

# Test with coverage
./gradlew test jacocoTestReport

# Test specific module with coverage
./gradlew :waldot-api:test :waldot-api:jacocoTestReport

# Test all plugins (follows GitHub Actions pattern)
./gradlew waldot-plugin-tinkerpop:test waldot-plugin-zenoh:test waldot-plugin-os:test waldot-plugin-behavior-tree:test waldot-plugin-reinforcement-learning:test waldot-plugin-rules-engine:test waldot-plugin-generator:test

# Test client applications (follows GitHub Actions pattern)
./gradlew waldot-client:test wotctl:test

# Test zenoh modules (follows GitHub Actions pattern)
./gradlew zenoh-agent-lib:test zenoh-acme:test
```

### Recommended Testing Workflow
1. **Core API**: Start with `waldot-deps`, `waldot-api`
2. **Namespace**: Test OPCUA graph integration
3. **TinkerPop Plugin**: Validate graph processing capabilities
4. **Client Applications**: Test WaldOT client and wotctl
5. **Plugin Ecosystem**: Test all plugins (os, zenoh, behavior-tree, etc.)
6. **Zenoh Integration**: Test zenoh-agent-lib and zenoh-acme

### Test Dependencies and Modules
```bash
# Test dependency analysis
./gradlew :waldot-api:dependencies

# View all project dependencies
./gradlew dependencies

# Test specific module dependencies
./gradlew :waldot-app:dependencies
```

### Performance Testing
```bash
# Run tests with performance monitoring
./gradlew test --info

# Run tests with specific JVM settings
./gradlew test -Dorg.gradle.jvmargs="-Xmx2g -XX:+UseG1GC"
```

### Integration Testing Workflow
```bash
# Full integration test (all modules)
./gradlew clean build test

# Integration test with Docker (if configured)
./gradlew :waldot-app:dockerBuild dockerRun
```

## Development Workflow

1. **Setup**: Run `./gradlew eclipse` for IDE setup
2. **Development**: Work on individual modules
3. **Testing**: Run `./gradlew test` for specific modules
4. **Building**: Use `./gradlew build -x :zenoh-acme:build` for full build
5. **Quality**: Run `./gradlew test jacocoTestReport` for coverage

## Docker Commands and Workflows

### Local Docker Development
```bash
# Build WaldOT application image locally
docker build -t waldot:latest .

# Run WaldOT container with default configuration
docker run -p 12686:12686 waldot:latest

# Build multi-platform image locally (requires buildx)
docker buildx build --platform linux/amd64,linux/arm64 -t waldot:multi .
```

### Production Docker Images
The project maintains two main Docker images:

#### 1. WaldOT Application (`rossonet/waldot:latest`)
- **Multi-architecture**: Supports `linux/amd64` and `linux/arm64`
- **Base Image**: Eclipse Temurin JRE 21 Alpine
- **Build Process**: Multi-stage build with Gradle
- **Ports**: Exposes 12686 (OPCUA/TCP)
- **Entry Point**: `net.rossonet.agent.MainAgent`

#### 2. Zenoh Router (`rossonet/zenohd:latest`)
- **Multi-architecture**: Separate builds for `linux/amd64` (intel) and `linux/arm64` (arm)
- **Base Image**: Debian Trixie Slim
- **Components**: Core Zenoh + filesystem, rocksdb, influxdb, MQTT plugins
- **Ports**: Exposes 7447 (Zenoh) and 8000 (REST API)
- **Build Source**: `docs/examples/zenoh/Dockerfile`

### GitHub Actions Docker Workflows

#### WaldOT Image Build (`publish-to-docker-hub.yml`)
```yaml
# Multi-platform build for linux/arm64,linux/amd64
docker buildx build --platform linux/arm64,linux/amd64 -t rossonet/waldot:latest .
# Includes Trivy security scanning
# Generates CycloneDX SBOM
```

#### Zenoh Image Build (`zenoh-to-docker-hub.yml`)
```yaml
# Separate platform builds then unified manifest
# Build arm64: rossonet/zenohd:arm
# Build amd64: rossonet/zenohd:intel
# Create unified manifest: rossonet/zenohd:latest
```

### Security and Compliance
- **Trivy Scanning**: Automated vulnerability scanning on all images
- **CycloneDX SBOM**: Software Bill of Materials generation
- **Multi-platform Security**: Scans performed on amd64 architecture

### Docker Build Configuration Files

#### WaldOT Dockerfile Structure:
```dockerfile
# Multi-stage build
FROM eclipse-temurin:21-jdk AS builder  # Build stage
FROM eclipse-temurin:21-jre-alpine AS initial  # Runtime prep
FROM eclipse-temurin:21-jre-alpine  # Final image
```

#### Zenoh Router Dockerfile Structure:
```dockerfile
# Rust-based multi-stage build
FROM rust:latest AS builder  # Compile Rust components
FROM debian:trixie-slim AS base  # Prepare runtime
FROM debian:trixie-slim  # Final minimal image
```

## Module-Specific Task Discovery

Now that GraalVM issues are resolved, all tasks are available:

```bash
# List all available tasks (working at root level)
./gradlew tasks --all

# Task discovery for specific modules
./gradlew :waldot-api:tasks --all
./gradlew :waldot-app:tasks --all
./gradlew :waldot-client:tasks --all
./gradlew :wotctl:tasks --all
./gradlew :waldot-namespace:tasks --all
./gradlew :zenoh-acme:tasks --all

# Plugin modules
./gradlew :waldot-plugin-tinkerpop:tasks --all
./gradlew :waldot-plugin-zenoh:tasks --all
./gradlew :waldot-plugin-os:tasks --all
./gradlew :waldot-plugin-behavior-tree:tasks --all
./gradlew :waldot-plugin-reinforcement-learning:tasks --all
./gradlew :waldot-plugin-rules-engine:tasks --all
./gradlew :waldot-plugin-generator:tasks --all

# Zenoh modules
./gradlew :zenoh-agent-lib:tasks --all

# List tasks by category
./gradlew tasks --group=build
./gradlew tasks --group=verification
./gradlew tasks --group=publishing
./gradlew tasks --group=application

# Get help for specific tasks
./gradlew help --task <task-name>
```

## Environment Variables

The project may require these environment variables for publishing:
- `GPG_PRIVATE`: GPG private key for signing
- `GPG_PUBLIC`: GPG public key for signing  
- `OSSRH_USERNAME`: Sonatype username for Maven Central publishing
- `OSSRH_PASSWORD`: Sonatype password for Maven Central publishing

- `DOCKER_HUB_TOKEN`: Docker Hub access token for image publishing

## Additional Resources

### Documentation References
- **TinkerPop Provider Guide**: https://tinkerpop.apache.org/docs/current/dev/provider/
- **Eclipse Milo Documentation**: https://github.com/eclipse-milo/milo
- **OPCUA Specification**: https://reference.opcfoundation.org/Core/Part3/v104/docs/
- **Java 21 Documentation**: https://openjdk.org/projects/jdk/21/
- **Gradle Build Tool**: https://gradle.org/guides/
- **Docker Buildx**: https://docs.docker.com/buildx/working-with-buildx/
- **JReleaser**: https://jreleaser.org/guide/

### Key Implementation Patterns (from TinkerPop Guide)

#### Core Structure Implementation
```java
// Required pattern from TinkerPop provider guide
public class WaldotGraph implements Graph {
    // Must have static Graph open(Configuration) method
    // Or use @GraphFactoryClass annotation
}

// Use helper classes consistently
ElementHelper.element(label, vertex)
GraphComputerHelper.submit(graph, computer)
VertexProgramHelper.createVertexProgram(methods)
```

#### Eclipse Milo Integration Best Practices
```java
// Server configuration using HOCON format
OpcUaServerConfig config = OpcUaServerConfig.builder()
    .setApplicationUri("opc.tcp://0.0.0.0:12686/waldot")
    .build();

// Client configuration with automatic reconnection
OpcUaClientConfig config = OpcUaClientConfig.builder()
    .setEndpointUrl("opc.tcp://localhost:12686/waldot")
    .build();
```

### Required Test Annotations (from TinkerPop)
```java
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_COMPUTER)
```

### Compliance Checklists

#### ✅ TinkerPop Provider Requirements Checklist
- [ ] Core interfaces implemented (Graph, Vertex, Edge, Property)
- [ ] GraphFactory compatibility provided
- [ ] Features implementation correct
- [ ] JVM test suite compatibility
- [ ] Gherkin test suite compatibility

#### ✅ Eclipse Milo Integration Checklist
- [ ] Module separation (client/server/SDK only)
- [ ] Security configuration management
- [ ] Certificate management in /security/pki/
- [ ] Subscription transfer mechanisms implemented
- [ ] Large-scale monitoring considerations

### Performance Guidelines

#### 🚀 High-Priority Optimizations
- Override inefficient default methods
- Use proper exception handling
- Implement transaction support for consistency
- Use connection pooling for scalability

#### 🔍 Medium-Priority Improvements
- Fix Javadoc warnings
- Address HTML validation errors
- Clean up compiler warnings
- Improve code documentation

#### 📝 Low-Priority Tasks
- Add missing @param documentation
- Standardize toString() implementations
- Consolidate duplicate code patterns

### Modular Architecture Recommendations

```
waldot-tinkerpop/     # TinkerPop provider implementation
waldot-opcua/         # OPCUA server/client components  
waldot-core/           # Shared components and bridge logic
waldot-app/             # Main application module
```

### Integration Testing Strategy

1. **Unit Tests**: Test TinkerPop compliance
2. **Integration Tests**: Test OPCUA connectivity
3. **Performance Tests**: Large-scale graph operations
4. **End-to-End Tests**: Full workflow validation

### Security Best Practices

1. **Authentication**: Support anonymous, username/password, X.509 certificates
2. **Configuration**: HOCON format for both OPCUA and TinkerPop
3. **Certificate Management**: Proper PKI directory structure

## TinkerPop Provider Requirements & Best Practices

### Core Implementation Requirements
Based on TinkerPop provider specifications (https://tinkerpop.apache.org/docs/current/dev/provider/), WaldOT must implement:

1. **Structure API Implementation**: 
   - Core interfaces: `Graph`, `Vertex`, `Edge`, `Property`, `VertexProperty`
   - Transaction support if implementing transactional features
   - Implementation named `WaldOTGraph` following naming conventions

2. **GraphFactory Compatibility**:
   - Provide static `Graph open(Configuration)` method
   - Or use `@GraphFactoryClass` annotation for automatic discovery
   - Proper configuration handling through `Configuration` objects

3. **Features Implementation**:
   - Correctly implement `Features` (Graph, Vertex, Edge features)
   - Ensure test cases handle particular feature implementations properly
   - Use proper feature detection in `Graph.features()`

4. **Testing & Validation**:
   - Include both JVM and Gherkin test suites using `gremlin-test`
   - Implement required test annotations:
     ```java
     @Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_STANDARD)
     @Graph.OptIn(Graph.OptIn.SUITE_PROCESS_STANDARD)
     @Graph.OptIn(Graph.OptIn.SUITE_PROCESS_COMPUTER)
     ```
   - Extend `AbstractGraphProvider` for test suite integration

### Code Quality Guidelines

1. **Helper Classes**: Use TinkerPop static helper classes:
   - `ElementHelper` for element operations
   - `GraphComputerHelper` for computation operations
   - `VertexProgramHelper` for traversal programs
   - `StringHelper` for consistent `toString()` implementations

2. **Exception Handling**: Use TinkerPop's built-in exception classes:
   - `IllegalArgumentException` for invalid parameters
   - `NoSuchElementException` for missing elements
   - `IllegalStateException` for invalid state operations

3. **Default Methods**: Override inefficient default methods on interfaces to improve performance

## Eclipse Milo OPCUA Integration Best Practices

### Architecture Patterns

Based on Eclipse Milo documentation (https://github.com/eclipse-milo/milo):

1. **Module Separation**:
   - Client development: Use `milo-sdk-client` only
   - Server development: Use `milo-sdk-server` only  
   - Core protocol: Use transient dependencies only
   - WaldOT should use `milo-sdk-server` for server components

2. **Server Implementation Best Practices**:
   - **Namespace Management**: Implement custom `NodeManager` for OPCUA address space
   - **Subscription Management**: Use built-in subscription transfer mechanisms
   - **Security Configuration**: 
     - Use HOCON format for server configuration
     - Store certificates in `/security/pki/` directories
     - Support both unsecured and X.509 secured connections

3. **Client Implementation Patterns**:
   - **Connection Management**: 
     - Use `OpcUaClient.create()` with proper `OpcUaClientConfig`
     - Implement automatic reconnection using built-in state machine
     - Handle subscription transfers with `SubscriptionListener.onSubscriptionTransferFailed()`
   - **Large-Scale Monitoring**: For 1M+ monitored items across 200+ servers:
     - Customize `MonitoredItem` behavior through inheritance
     - Implement efficient data change notification
     - Use proper resource management and cleanup

### Performance & Scalability

1. **Large-Scale Applications**: 
   - Implement efficient data change notification
   - Use proper subscription management for high-frequency updates
   - Resource leak prevention through proper lifecycle management

2. **Memory Management**: 
   - Proper cleanup of subscriptions and monitored items
   - Use connection pooling for multiple clients
   - Monitor and tune JVM parameters for production workloads

## WaldOT-Specific Implementation Recommendations

### TinkerPop Integration Architecture

1. **Graph Structure**:
   - `WaldOTGraph` class: Main TinkerPop implementation
   - `OpcUaGraphAdapter`: Bridges OPCUA address space to graph structure
   - Map OPCUA nodes to vertices with proper type mapping
   - Convert OPCUA attributes to vertex properties
   - Use references as edges between nodes

2. **OPCUA Address Space Design**:
   - Organize OPCUA nodes to reflect graph structure
   - Use proper namespace URIs: `urn:rossonet:waldot:agent`
   - Implement efficient node browsing and discovery
   - Support dynamic node creation/removal through graph operations

3. **Event System Integration**:
   - Use OPCUA events for graph change notifications
   - Map OPCUA events to TinkerPop graph events
   - Implement proper event filtering and subscription management

### Testing Strategy

1. **Unit Tests**: Test TinkerPop compliance with `gremlin-test` suite
2. **Integration Tests**: Test OPCUA connectivity and data exchange
3. **Performance Tests**: Validate large-scale graph operations and OPCUA subscriptions
4. **Compliance Tests**: Ensure OPCUA specification compliance

### Security & Configuration

1. **Security**: Implement proper OPCUA security policies and certificate management
2. **Configuration**: Use HOCON format for both OPCUA and TinkerPop configuration
3. **Authentication**: Support multiple OPCUA authentication methods:
   - Anonymous access
   - Username/password authentication
   - X.509 certificate authentication

### Module Organization

```
waldot-tinkerpop/     # TinkerPop provider implementation
waldot-opcua/         # OPCUA server/client components  
waldot-core/           # Shared components and bridge logic
waldot-app/             # Main application module
```

### Key Classes Implementation

1. **Core TinkerPop**:
   - `WaldOTGraph`: Main graph implementation
   - `WaldOTVertex`: Vertex implementation with OPCUA integration
   - `WaldOTEdge`: Edge implementation for node relationships
   - `WaldOTProperty`: Property implementation

2. **OPCUA Integration**:
   - `OpcUaGraphAdapter`: Bridges OPCUA to graph structure
   - `WaldOTNamespace`: OPCUA namespace management
   - `GraphChangeNotifier`: Handles change notifications

3. **Configuration**:
   - `DefaultOpcUaConfiguration`: OPCUA server configuration
   - `DefaultWaldotConfiguration`: Combined configuration system

### Code Quality Improvements

1. **Javadoc Cleanup**: 
   - Remove @Author tags and fix HTML validation errors
   - Add missing @param documentation for all public methods
   - Use proper JavaDoc formatting and examples

2. **Compiler Warnings**: 
   - Address deprecated API usage with proper replacements
   - Fix unchecked operations warnings where appropriate
   - Implement proper exception handling

3. **Build System**: 
   - Consider upgrading to Gradle 9.x to address deprecated features
   - Optimize multi-module build performance
   - Implement proper CI/CD pipeline with automated testing