# WaldOT Docker Environment Variables Reference

This document provides a complete reference for all environment variables that can be used to configure the WaldOT Docker container.

## Table of Contents
- [Quick Start](#quick-start)
- [Environment Variables List](#environment-variables-list)
  - [Network Configuration](#network-configuration)
  - [Server Identity](#server-identity)
  - [Security Configuration](#security-configuration)
  - [Authentication](#authentication)
  - [Application Settings](#application-settings)
  - [Node Configuration](#node-configuration)
  - [Command Configuration](#command-configuration)
- [Usage Examples](#usage-examples)
- [Docker Compose Examples](#docker-compose-examples)
- [Kubernetes ConfigMap Example](#kubernetes-configmap-example)

## Quick Start

### Basic Usage with Environment Variables

```bash
docker run \
  -e WALDOT_TCP_PORT=4840 \
  -e WALDOT_APPLICATION_NAME="My OPC UA Server" \
  -e WALDOT_ANONYMOUS_ACCESS=false \
  -e WALDOT_FACTORY_USERNAME=admin \
  -e WALDOT_FACTORY_PASSWORD=SecurePass123 \
  -p 4840:4840 \
  rossonet/waldot:latest
```

### Using .env File

Create a `.env` file:
```bash
WALDOT_TCP_PORT=4840
WALDOT_APPLICATION_NAME=Production Server
WALDOT_ANONYMOUS_ACCESS=false
WALDOT_FACTORY_USERNAME=admin
WALDOT_FACTORY_PASSWORD=${SECRET_PASSWORD}
```

Then run:
```bash
docker run --env-file .env -p 4840:4840 rossonet/waldot:latest
```

## Environment Variables List

### Network Configuration

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `WALDOT_TCP_PORT` | `12686` | TCP port for OPC UA binary protocol (opc.tcp://) | `4840` |
| `WALDOT_HTTPS_PORT` | `8443` | HTTPS port for web services and REST API | `8080` |
| `WALDOT_BIND_ADDRESSES` | `0.0.0.0` | Comma-separated IP addresses for binding | `0.0.0.0` or `192.168.1.100,10.0.0.50` |
| `WALDOT_BIND_HOSTNAME` | `127.0.0.1` | Hostname for OPC UA endpoint URLs | `waldot-server.local` |
| `WALDOT_ENDPOINT_PATH` | `/waldot` | URL path component for endpoints | `/production` |

**Network Configuration Tips**:
- Use standard OPC UA port `4840` for better client compatibility
- Set `WALDOT_BIND_ADDRESSES=0.0.0.0` to accept connections from any interface
- Set `WALDOT_BIND_HOSTNAME` to match your DNS name for proper certificate validation

### Server Identity

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `WALDOT_APPLICATION_NAME` | `WaldOT OPCUA server` | Server name visible to OPC UA clients | `Production OPC UA Server` |
| `WALDOT_PRODUCT_NAME` | `WaldOT` | Product name in OPC UA BuildInfo | `Industrial WaldOT` |
| `WALDOT_PRODUCT_URI` | `urn:rossonet:waldot:uaserver` | Unique product URI identifier | `urn:company:product:waldot` |
| `WALDOT_MANUFACTURER_NAME` | `Rossonet s.c.a r.l.` | Manufacturer name in BuildInfo | `ACME Corporation` |
| `WALDOT_BUILD_NUMBER` | `w001` | Build number for version tracking | `v1.2.3-prod` |

**Identity Configuration Tips**:
- Customize these for branding and identification in enterprise deployments
- Keep URIs unique across your organization
- Use semantic versioning for build numbers

### Security Configuration

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `WALDOT_SECURITY_DIR` | `.security` | Directory for certificates and keys | `/secure/certs` |
| `WALDOT_DNS_ADDRESS_CERT` | `127.0.0.1` | DNS name for SSL certificate SAN | `waldot.company.com` |

**Security Configuration Tips**:
- **Always mount security directory as Docker volume** for certificate persistence
- Set `WALDOT_DNS_ADDRESS_CERT` to match the hostname clients will use
- Protect security directory with appropriate filesystem permissions (700)

**Docker Volume Example**:
```bash
docker run \
  -v /secure/waldot:/app/.security \
  -e WALDOT_SECURITY_DIR=/app/.security \
  -e WALDOT_DNS_ADDRESS_CERT=waldot.production.com \
  rossonet/waldot:latest
```

### Authentication

| Variable | Default | Description | Security Level |
|----------|---------|-------------|----------------|
| `WALDOT_ANONYMOUS_ACCESS` | `true` | Allow connections without credentials | ⚠️ CRITICAL |
| `WALDOT_FACTORY_USERNAME` | `admin` | Default administrator username | ⚠️ HIGH |
| `WALDOT_FACTORY_PASSWORD` | `password123` | Default administrator password | 🔴 CRITICAL |

**Authentication Security Guidelines**:

#### Development Environment
```bash
WALDOT_ANONYMOUS_ACCESS=true
WALDOT_FACTORY_USERNAME=admin
WALDOT_FACTORY_PASSWORD=dev123
```

#### Production Environment (REQUIRED)
```bash
WALDOT_ANONYMOUS_ACCESS=false
WALDOT_FACTORY_USERNAME=production_admin
WALDOT_FACTORY_PASSWORD=${SECURE_RANDOM_PASSWORD}  # Use secrets management!
```

**🔴 CRITICAL SECURITY WARNINGS**:
- **NEVER use default password `password123` in production**
- **ALWAYS disable anonymous access in production**: `WALDOT_ANONYMOUS_ACCESS=false`
- **ALWAYS use strong passwords** (16+ characters, mixed case, numbers, symbols)
- **ALWAYS use secrets management** (Docker secrets, Kubernetes secrets, etc.)

### Application Settings

| Variable | Default | Description | Example |
|----------|---------|-------------|---------|
| `WALDOT_NAMESPACE_URI` | `urn:rossonet:waldot:engine` | OPC UA namespace URI for WaldOT nodes | `urn:company:waldot:production` |
| `WALDOT_HELP_DIRECTORY` | `/app/help` | Directory for help documentation files | `/custom/help` |
| `WALDOT_BOOT_URL` | `file:///waldot/boot.conf` | Bootstrap configuration file URL | `file:///config/init.conf` |
| `WALDOT_FACTS_VALID_DELAY` | `0` | Delay (ms) before facts become valid | `1000` |
| `WALDOT_FACTS_VALID_UNTIL` | `0` | Expiration time (ms) for facts (0=never) | `3600000` |

**Application Settings Tips**:
- Mount custom bootstrap config: `-v ./boot.conf:/waldot/boot.conf:ro`
- Mount custom help directory: `-v ./help:/app/help:ro`
- Use namespace URI to segregate multiple WaldOT instances

### Node Configuration

#### Root Node

| Variable | Default | Description |
|----------|---------|-------------|
| `WALDOT_ROOT_NODE_ID` | `waldot` | Root node identifier in address space |
| `WALDOT_ROOT_NODE_BROWSE_NAME` | `Gremlin Engine` | Root node browse name |
| `WALDOT_ROOT_NODE_DISPLAY_NAME` | `Gremlin Engine` | Root node display name |

#### Asset Node

| Variable | Default | Description |
|----------|---------|-------------|
| `WALDOT_ASSET_NODE_ID` | `aas` | Asset administration node identifier |
| `WALDOT_ASSET_NODE_BROWSE_NAME` | `Administration` | Asset node browse name |
| `WALDOT_ASSET_NODE_DISPLAY_NAME` | `Administration` | Asset node display name |

#### Interface Node

| Variable | Default | Description |
|----------|---------|-------------|
| `WALDOT_INTERFACE_NODE_ID` | `cmd` | Command interface node identifier |
| `WALDOT_INTERFACE_NODE_BROWSE_NAME` | `Commands` | Interface node browse name |
| `WALDOT_INTERFACE_NODE_DISPLAY_NAME` | `Commands` | Interface node display name |

**Node Configuration Example**:
```bash
# Custom node structure for production
WALDOT_ROOT_NODE_ID=production_engine
WALDOT_ROOT_NODE_BROWSE_NAME=ProductionEngine
WALDOT_ROOT_NODE_DISPLAY_NAME="Production Gremlin Engine"
WALDOT_ASSET_NODE_ID=production_assets
WALDOT_INTERFACE_NODE_ID=production_commands
```

### Command Configuration

#### About Command

| Variable | Default | Description |
|----------|---------|-------------|
| `WALDOT_ABOUT_COMMAND_LABEL` | `about` | About command label |
| `WALDOT_ABOUT_COMMAND_DESCRIPTION` | `info about this software` | About command description |
| `WALDOT_ABOUT_COMMAND_EXECUTABLE` | `true` | Enable about command |
| `WALDOT_ABOUT_COMMAND_USER_EXECUTABLE` | `true` | Allow users to execute |

#### Exec Command (⚠️ Security Sensitive)

| Variable | Default | Description | Security |
|----------|---------|-------------|----------|
| `WALDOT_EXEC_COMMAND_LABEL` | `exec` | Exec command label | ⚠️ HIGH |
| `WALDOT_EXEC_COMMAND_DESCRIPTION` | `run system command` | Exec command description | ⚠️ HIGH |
| `WALDOT_EXEC_COMMAND_EXECUTABLE` | `true` | Enable exec command | 🔴 CRITICAL |
| `WALDOT_EXEC_COMMAND_USER_EXECUTABLE` | `true` | Allow users to execute | 🔴 CRITICAL |

**🔴 EXEC COMMAND SECURITY WARNING**:
The exec command allows arbitrary system command execution. **ALWAYS disable in production**:
```bash
WALDOT_EXEC_COMMAND_EXECUTABLE=false
WALDOT_EXEC_COMMAND_USER_EXECUTABLE=false
```

#### Help Command

| Variable | Default | Description |
|----------|---------|-------------|
| `WALDOT_HELP_COMMAND_LABEL` | `help` | Help command label |
| `WALDOT_HELP_COMMAND_DESCRIPTION` | `list available commands` | Help command description |
| `WALDOT_HELP_COMMAND_EXECUTABLE` | `true` | Enable help command |
| `WALDOT_HELP_COMMAND_USER_EXECUTABLE` | `true` | Allow users to execute |

#### Gremlin Query Command (⚠️ Security Sensitive)

| Variable | Default | Description | Security |
|----------|---------|-------------|----------|
| `WALDOT_WALDOT_COMMAND_LABEL` | `query` | Query command label | ⚠️ MEDIUM |
| `WALDOT_WALDOT_COMMAND_DESCRIPTION` | `run Gremlin query` | Query command description | ⚠️ MEDIUM |
| `WALDOT_WALDOT_COMMAND_EXECUTABLE` | `true` | Enable query command | ⚠️ MEDIUM |
| `WALDOT_WALDOT_COMMAND_USER_EXECUTABLE` | `true` | Allow users to execute | ⚠️ MEDIUM |

**Gremlin Query Security Note**:
Gremlin queries have full graph access. Consider restricting in production with sensitive data:
```bash
WALDOT_WALDOT_COMMAND_USER_EXECUTABLE=false  # Admin only
```

## Usage Examples

### Example 1: Development Setup
```bash
docker run \
  -e WALDOT_TCP_PORT=12686 \
  -e WALDOT_APPLICATION_NAME="Dev WaldOT" \
  -e WALDOT_ANONYMOUS_ACCESS=true \
  -p 12686:12686 \
  rossonet/waldot:latest
```

### Example 2: Production Setup (Secure)
```bash
docker run \
  -e WALDOT_TCP_PORT=4840 \
  -e WALDOT_HTTPS_PORT=8443 \
  -e WALDOT_APPLICATION_NAME="Production OPC UA Server" \
  -e WALDOT_PRODUCT_NAME="Industrial WaldOT" \
  -e WALDOT_MANUFACTURER_NAME="ACME Corp" \
  -e WALDOT_BIND_HOSTNAME=waldot.production.local \
  -e WALDOT_DNS_ADDRESS_CERT=waldot.production.local \
  -e WALDOT_ANONYMOUS_ACCESS=false \
  -e WALDOT_FACTORY_USERNAME=prod_admin \
  -e WALDOT_FACTORY_PASSWORD=${PROD_PASSWORD} \
  -e WALDOT_EXEC_COMMAND_EXECUTABLE=false \
  -e WALDOT_EXEC_COMMAND_USER_EXECUTABLE=false \
  -v /secure/waldot:/app/.security \
  -v /config/boot-prod.conf:/waldot/boot.conf:ro \
  -p 4840:4840 \
  -p 8443:8443 \
  rossonet/waldot:latest
```

### Example 3: Custom Branding
```bash
docker run \
  -e WALDOT_APPLICATION_NAME="ACME Industrial Server" \
  -e WALDOT_PRODUCT_NAME="ACME WaldOT" \
  -e WALDOT_PRODUCT_URI="urn:acme:industrial:waldot:v2" \
  -e WALDOT_MANUFACTURER_NAME="ACME Corporation" \
  -e WALDOT_ROOT_NODE_DISPLAY_NAME="ACME Engine" \
  -e WALDOT_ENDPOINT_PATH="/acme" \
  -p 12686:12686 \
  rossonet/waldot:latest
```

### Example 4: High Security Configuration
```bash
docker run \
  -e WALDOT_ANONYMOUS_ACCESS=false \
  -e WALDOT_FACTORY_USERNAME=secure_admin \
  -e WALDOT_FACTORY_PASSWORD=${COMPLEX_PASSWORD} \
  -e WALDOT_EXEC_COMMAND_EXECUTABLE=false \
  -e WALDOT_EXEC_COMMAND_USER_EXECUTABLE=false \
  -e WALDOT_WALDOT_COMMAND_USER_EXECUTABLE=false \
  -e WALDOT_BIND_ADDRESSES=10.0.1.100 \
  -e WALDOT_DNS_ADDRESS_CERT=secure.internal.company.com \
  -v /secure/certs:/app/.security:ro \
  -p 12686:12686 \
  rossonet/waldot:latest
```

## Docker Compose Examples

### Basic Docker Compose with Environment Variables

```yaml
version: '3.8'

services:
  waldot:
    image: rossonet/waldot:latest
    container_name: waldot-prod
    ports:
      - "4840:4840"
      - "8443:8443"
    environment:
      # Network
      - WALDOT_TCP_PORT=4840
      - WALDOT_HTTPS_PORT=8443
      - WALDOT_BIND_ADDRESSES=0.0.0.0
      - WALDOT_BIND_HOSTNAME=waldot-prod
      
      # Identity
      - WALDOT_APPLICATION_NAME=Production Server
      - WALDOT_PRODUCT_NAME=Industrial WaldOT
      - WALDOT_MANUFACTURER_NAME=My Company
      
      # Security
      - WALDOT_ANONYMOUS_ACCESS=false
      - WALDOT_FACTORY_USERNAME=admin
      - WALDOT_FACTORY_PASSWORD=${ADMIN_PASSWORD}
      - WALDOT_DNS_ADDRESS_CERT=waldot-prod.local
      
      # Disable dangerous commands
      - WALDOT_EXEC_COMMAND_EXECUTABLE=false
    
    volumes:
      - waldot-security:/app/.security
      - ./boot.conf:/waldot/boot.conf:ro
    
    restart: always

volumes:
  waldot-security:
```

### Docker Compose with .env File

**.env file**:
```bash
# Network Configuration
WALDOT_TCP_PORT=4840
WALDOT_HTTPS_PORT=8443

# Server Identity
WALDOT_APPLICATION_NAME=Production OPC UA Server
WALDOT_MANUFACTURER_NAME=ACME Corporation

# Security (use secrets management for passwords!)
WALDOT_ANONYMOUS_ACCESS=false
WALDOT_FACTORY_USERNAME=prod_admin
WALDOT_FACTORY_PASSWORD=ChangeThisInProduction123!

# Disable dangerous features
WALDOT_EXEC_COMMAND_EXECUTABLE=false
```

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  waldot:
    image: rossonet/waldot:latest
    env_file: .env
    ports:
      - "${WALDOT_TCP_PORT}:${WALDOT_TCP_PORT}"
      - "${WALDOT_HTTPS_PORT}:${WALDOT_HTTPS_PORT}"
    volumes:
      - waldot-security:/app/.security
    restart: always

volumes:
  waldot-security:
```

### Docker Compose with Secrets (Recommended for Production)

```yaml
version: '3.8'

services:
  waldot:
    image: rossonet/waldot:latest
    environment:
      - WALDOT_TCP_PORT=4840
      - WALDOT_ANONYMOUS_ACCESS=false
      - WALDOT_FACTORY_USERNAME=admin
      - WALDOT_FACTORY_PASSWORD_FILE=/run/secrets/waldot_password
    secrets:
      - waldot_password
    ports:
      - "4840:4840"
    volumes:
      - waldot-security:/app/.security

secrets:
  waldot_password:
    external: true

volumes:
  waldot-security:
```

Create secret:
```bash
echo "YourSecurePassword123!" | docker secret create waldot_password -
```

## Kubernetes ConfigMap Example

### ConfigMap Definition

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: waldot-config
  namespace: production
data:
  # Network Configuration
  WALDOT_TCP_PORT: "4840"
  WALDOT_HTTPS_PORT: "8443"
  WALDOT_BIND_ADDRESSES: "0.0.0.0"
  WALDOT_BIND_HOSTNAME: "waldot-service.production.svc.cluster.local"
  WALDOT_ENDPOINT_PATH: "/waldot"
  
  # Server Identity
  WALDOT_APPLICATION_NAME: "Kubernetes WaldOT Server"
  WALDOT_PRODUCT_NAME: "WaldOT K8s Edition"
  WALDOT_MANUFACTURER_NAME: "Your Company"
  
  # Security (non-sensitive)
  WALDOT_ANONYMOUS_ACCESS: "false"
  WALDOT_DNS_ADDRESS_CERT: "waldot-service.production.svc.cluster.local"
  
  # Node Configuration
  WALDOT_ROOT_NODE_ID: "k8s_waldot"
  WALDOT_ROOT_NODE_DISPLAY_NAME: "Kubernetes Gremlin Engine"
  
  # Disable dangerous commands
  WALDOT_EXEC_COMMAND_EXECUTABLE: "false"
  WALDOT_EXEC_COMMAND_USER_EXECUTABLE: "false"
```

### Secret for Sensitive Data

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: waldot-credentials
  namespace: production
type: Opaque
stringData:
  WALDOT_FACTORY_USERNAME: "k8s_admin"
  WALDOT_FACTORY_PASSWORD: "SecureK8sPassword123!"
```

### Deployment Using ConfigMap and Secret

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: waldot
  namespace: production
spec:
  replicas: 1
  selector:
    matchLabels:
      app: waldot
  template:
    metadata:
      labels:
        app: waldot
    spec:
      containers:
      - name: waldot
        image: rossonet/waldot:latest
        ports:
        - containerPort: 4840
          name: opcua
        - containerPort: 8443
          name: https
        envFrom:
        - configMapRef:
            name: waldot-config
        - secretRef:
            name: waldot-credentials
        volumeMounts:
        - name: security
          mountPath: /app/.security
      volumes:
      - name: security
        persistentVolumeClaim:
          claimName: waldot-security-pvc
```

## Validation and Testing

### Verify Environment Variables are Applied

```bash
# Check container environment
docker exec waldot-container env | grep WALDOT

# Check logs for configuration
docker logs waldot-container | grep "Starting WaldOT"
```

### Test Connection

```bash
# Test OPC UA TCP port
telnet localhost 4840

# Test HTTPS port
curl -k https://localhost:8443
```

### Verify Configuration via OPC UA Client

1. Connect with UaExpert or Prosys Browser
2. Check Server → ServerStatus → BuildInfo
3. Verify ApplicationName, ProductName, ManufacturerName match your environment variables

## Best Practices

### Security
1. ✅ **Always use secrets management** for passwords (Docker secrets, Kubernetes secrets, vault)
2. ✅ **Never commit `.env` files** with passwords to version control
3. ✅ **Disable anonymous access** in production: `WALDOT_ANONYMOUS_ACCESS=false`
4. ✅ **Disable exec command** in production: `WALDOT_EXEC_COMMAND_EXECUTABLE=false`
5. ✅ **Use strong passwords** (16+ characters, mixed case, numbers, symbols)
6. ✅ **Rotate passwords regularly** using secrets management

### Deployment
1. ✅ **Mount security directory** as volume for certificate persistence
2. ✅ **Use named volumes** for better management
3. ✅ **Set resource limits** (CPU, memory) in production
4. ✅ **Enable health checks** to monitor server status
5. ✅ **Use restart policies** (always, unless-stopped)

### Configuration Management
1. ✅ **Use `.env` files** for non-sensitive configuration
2. ✅ **Use secrets** for passwords and sensitive data
3. ✅ **Document custom configurations** for your team
4. ✅ **Version control** configuration files (except passwords)
5. ✅ **Validate configuration** before production deployment

## Troubleshooting

### Environment Variables Not Applied

**Problem**: Container starts with default values

**Solutions**:
```bash
# Verify variable syntax
docker run -e WALDOT_TCP_PORT=4840  # Correct
docker run -e WALDOT_TCP_PORT 4840  # Wrong (missing =)

# Check Docker logs
docker logs waldot-container

# Verify environment inside container
docker exec waldot-container env | grep WALDOT
```

### Password Not Working

**Problem**: Cannot authenticate with specified password

**Solutions**:
1. Verify password doesn't contain special characters that need escaping
2. Use Docker secrets instead of environment variables
3. Check logs for authentication errors

### Port Binding Fails

**Problem**: Port already in use

**Solutions**:
```bash
# Find process using port
lsof -i :4840

# Use different port
-e WALDOT_TCP_PORT=12686
```

## Additional Resources

- Configuration Guide: [CONFIGURATION.md](CONFIGURATION.md)
- Full Documentation: [docs/README.md](docs/README.md)
- Docker Examples: [examples/README.md](examples/README.md)
- GitHub: https://github.com/rossonet/waldot

---

**Rossonet s.c.a r.l.** - https://www.rossonet.net
