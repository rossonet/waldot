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
```

#### Plugin Modules
```bash
# Build all plugins
./gradlew :waldot-plugin-tinkerpop:build :waldot-plugin-zenoh:build :waldot-plugin-os:build :waldot-plugin-behavior-tree:build :waldot-plugin-reinforcement-learning:build :waldot-plugin-rules-engine:build :waldot-plugin-generator:build

# Build specific plugin
./gradlew :waldot-plugin-tinkerpop:build
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
```

## Known Issues

1. **GraalVM Issue**: The `zenoh-acme` module has GraalVM configuration issues that prevent task listing and execution
   - **Complete workaround**: Exclude zenoh-acme from all operations with `-x :zenoh-acme:build -x :zenoh-acme:test -x :zenoh-acme:tasks`
   - **Alternative**: Work with individual modules instead of the entire project
   - **For tasks listing**: Use module-specific task listing like `./gradlew :waldot-api:tasks`

2. **Java Version**: Project uses Java 21
   - Ensure JDK 21 is installed and active
   - Use `./gradlew -version` to verify Gradle and Java versions

3. **Javadoc Warnings**: Many Javadoc warnings due to @Author tags and other documentation issues
   - These are warnings only and don't affect functionality
   - Use `./gradlew javadoc -x :waldot-api:javadoc` to skip problematic modules if needed

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

When running tests:
1. Start with core modules: `waldot-deps`, `waldot-api`
2. Test plugins individually: `waldot-plugin-tinkerpop`
3. Test client applications
4. Skip `zenoh-acme` if GraalVM issues occur

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

Due to the zenoh-acme GraalVM issue, use these commands to discover available tasks:

```bash
# Core modules
./gradlew :waldot-deps:tasks --all
./gradlew :waldot-api:tasks --all

# Application modules
./gradlew :waldot-app:tasks --all
./gradlew :waldot-client:tasks --all
./gradlew :wotctl:tasks --all

# Plugin modules
./gradlew :waldot-plugin-tinkerpop:tasks --all
./gradlew :waldot-plugin-zenoh:tasks --all
./gradlew :waldot-plugin-os:tasks --all
./gradlew :waldot-plugin-behavior-tree:tasks --all
./gradlew :waldot-plugin-reinforcement-learning:tasks --all
./gradlew :waldot-plugin-rules-engine:tasks --all
./gradlew :waldot-plugin-generator:tasks --all

# Zenoh modules (excluding zenoh-acme)
./gradlew :zenoh-agent-lib:tasks --all
```

## Environment Variables

The project may require these environment variables for publishing:
- `GPG_PRIVATE`: GPG private key for signing
- `GPG_PUBLIC`: GPG public key for signing  
- `OSSRH_USERNAME`: Sonatype username for Maven Central publishing
- `OSSRH_PASSWORD`: Sonatype password for Maven Central publishing