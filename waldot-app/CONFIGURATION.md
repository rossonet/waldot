# WaldOT Configuration Quick Reference

## Table of Contents
- [Quick Start](#quick-start)
- [Configuration Methods](#configuration-methods)
- [OPC UA Parameters](#opc-ua-parameters)
- [WaldOT Parameters](#waldot-parameters)
- [Common Scenarios](#common-scenarios)

## Quick Start

### Default Configuration
```bash
docker run -p 12686:12686 rossonet/waldot:latest
```
Access: `opc.tcp://localhost:12686/waldot`

### Custom Port
```bash
docker run -p 4840:4840 rossonet/waldot:latest --tcp-port 4840
```
Access: `opc.tcp://localhost:4840/waldot`

### Secure Mode
```bash
docker run -p 12686:12686 rossonet/waldot:latest \
  --anonymous-access false \
  --factory-username admin \
  --factory-password SecurePass123
```

## Configuration Methods

WaldOT supports three configuration methods (in priority order):

### 1. Command-Line Arguments (Highest Priority)
```bash
java -jar waldot-app.jar --tcp-port 4840 --anonymous-access false
```

### 2. Environment Variables
```bash
export WALDOT_TCP_PORT=4840
export WALDOT_ANONYMOUS_ACCESS=false
java -jar waldot-app.jar
```

### 3. Default Values (Lowest Priority)
Configured in `DefaultOpcUaConfiguration` and `DefaultHomunculusConfiguration` classes.

## OPC UA Parameters

### Network Configuration

#### TCP Port (`--tcp-port`, `-tp`)
- **Default**: 12686
- **Purpose**: OPC UA binary protocol port
- **Standard**: 4840 (OPC UA standard port)
- **Example**: `--tcp-port 4840`

#### HTTPS Port (`--https-port`, `-hp`)
- **Default**: 8443
- **Purpose**: Web services and REST API
- **Example**: `--https-port 8080`

#### Bind Addresses (`--bind-addresses`, `-ba`)
- **Default**: 0.0.0.0
- **Purpose**: Network interfaces to bind
- **Examples**:
  - All interfaces: `--bind-addresses 0.0.0.0`
  - Specific: `--bind-addresses 192.168.1.100`
  - Multiple: `--bind-addresses 192.168.1.100,10.0.0.50`

#### Bind Hostname (`--bind-hostname`, `-bh`)
- **Default**: 127.0.0.1
- **Purpose**: Hostname in endpoint URLs
- **Example**: `--bind-hostname myserver.local`

### Server Identity

#### Application Name (`--application-name`, `-an`)
- **Default**: "WaldOT OPCUA server"
- **Purpose**: Server name visible to clients
- **Example**: `--application-name "Production Server 1"`

#### Product Name (`--product-name`, `-pn`)
- **Default**: "WaldOT"
- **Purpose**: Product name in BuildInfo
- **Example**: `--product-name "Industrial WaldOT"`

#### Manufacturer Name (`--manufacturer-name`, `-mn`)
- **Default**: "Rossonet s.c.a r.l."
- **Purpose**: Manufacturer name in BuildInfo
- **Example**: `--manufacturer-name "Your Company"`

### Security

#### Security Directory (`--security-dir`, `-sd`)
- **Default**: ".security"
- **Purpose**: PKI certificates and keys location
- **Example**: `--security-dir /secure/certs`
- **Docker**: Mount as volume for persistence

#### DNS Address for Certificates (`--dns-address-cert`, `-dc`)
- **Default**: 127.0.0.1
- **Purpose**: DNS name in SSL certificates
- **Example**: `--dns-address-cert waldot.company.com`

## WaldOT Parameters

### Authentication

#### Anonymous Access (`--anonymous-access`, `-aa`)
- **Default**: true
- **Purpose**: Allow connections without credentials
- **Production**: Set to `false` for secure deployments
- **Example**: `--anonymous-access false`

#### Factory Username (`--factory-username`, `-fu`)
- **Default**: "admin"
- **Purpose**: Default administrator username
- **Security**: Change in production!
- **Example**: `--factory-username industrial_admin`

#### Factory Password (`--factory-password`, `-fp`)
- **Default**: "password123"
- **Purpose**: Default administrator password
- **Security**: MUST change in production!
- **Example**: `--factory-password "SecureP@ssw0rd!"`

### Application Settings

#### Namespace URI (`--namespace-uri`, `-nu`)
- **Default**: "urn:rossonet:waldot:engine"
- **Purpose**: OPC UA namespace identifier
- **Example**: `--namespace-uri "urn:company:waldot"`

#### Help Directory (`--help-directory`, `-hd`)
- **Default**: "/app/help"
- **Purpose**: Help documentation files location
- **Example**: `--help-directory /custom/help`

#### Bootstrap URL (`--boot-url`, `-bu`)
- **Default**: "file:///waldot/boot.conf"
- **Purpose**: Startup configuration file
- **Example**: `--boot-url file:///config/custom-boot.conf`

## Common Scenarios

### Scenario 1: Development Setup
```bash
docker run \
  -p 12686:12686 \
  -p 8443:8443 \
  -v $(pwd)/security:/app/.security \
  rossonet/waldot:latest \
  --application-name "Dev Server" \
  --anonymous-access true
```

### Scenario 2: Production Deployment
```bash
docker run \
  -p 4840:4840 \
  -p 8443:8443 \
  -v /secure/waldot:/app/.security \
  -v /config/boot.conf:/waldot/boot.conf:ro \
  rossonet/waldot:latest \
  --tcp-port 4840 \
  --application-name "Production OPC UA Server" \
  --product-name "Industrial WaldOT" \
  --manufacturer-name "Company Name" \
  --anonymous-access false \
  --factory-username production_admin \
  --factory-password "${WALDOT_ADMIN_PASSWORD}" \
  --bind-hostname production-server.company.com \
  --dns-address-cert production-server.company.com
```

### Scenario 3: Multiple Instances
```bash
# Instance 1
docker run -d --name waldot-1 \
  -p 12686:12686 \
  rossonet/waldot:latest \
  --application-name "WaldOT Instance 1"

# Instance 2
docker run -d --name waldot-2 \
  -p 12687:12687 \
  rossonet/waldot:latest \
  --tcp-port 12687 \
  --application-name "WaldOT Instance 2"
```

### Scenario 4: Custom Branding
```bash
docker run \
  -p 12686:12686 \
  rossonet/waldot:latest \
  --application-name "ACME Industrial Server" \
  --product-name "ACME WaldOT" \
  --product-uri "urn:acme:industrial:waldot" \
  --manufacturer-name "ACME Corporation" \
  --root-node-display-name "ACME Engine" \
  --endpoint-path "/acme"
```

### Scenario 5: High Security
```bash
docker run \
  -p 12686:12686 \
  -v /secure/certs:/app/.security:ro \
  rossonet/waldot:latest \
  --anonymous-access false \
  --factory-username admin \
  --factory-password "${COMPLEX_PASSWORD}" \
  --bind-addresses 10.0.1.100 \
  --dns-address-cert secure.internal.company.com
```

## Environment Variables

All parameters can be set via environment variables using `WALDOT_` prefix:

```bash
export WALDOT_TCP_PORT=4840
export WALDOT_HTTPS_PORT=8080
export WALDOT_APPLICATION_NAME="My Server"
export WALDOT_ANONYMOUS_ACCESS=false
export WALDOT_FACTORY_USERNAME=admin
export WALDOT_FACTORY_PASSWORD=secret
export WALDOT_NAMESPACE_URI="urn:mycompany:waldot"

docker run -p 4840:4840 rossonet/waldot:latest
```

## Docker Compose Example

```yaml
version: '3.8'

services:
  waldot:
    image: rossonet/waldot:latest
    container_name: waldot-production
    ports:
      - "4840:4840"
      - "8443:8443"
    command:
      - "--tcp-port"
      - "4840"
      - "--application-name"
      - "Production Server"
      - "--anonymous-access"
      - "false"
      - "--factory-username"
      - "admin"
      - "--factory-password"
      - "${ADMIN_PASSWORD}"
    volumes:
      - waldot-security:/app/.security
      - ./boot.conf:/waldot/boot.conf:ro
    restart: always

volumes:
  waldot-security:
```

## Validation

### Test Connection
```bash
# TCP port
telnet localhost 12686

# HTTPS port
curl -k https://localhost:8443
```

### View Logs
```bash
docker logs waldot
```

### OPC UA Client Test
Use UaExpert, Prosys Browser, or:
```bash
pip install opcua-client
opcua-client --endpoint opc.tcp://localhost:12686/waldot
```

## Troubleshooting

### Port Conflicts
```bash
# Find what's using the port
lsof -i :12686

# Use different port
--tcp-port 4840
```

### Connection Refused
- Check bind addresses: `--bind-addresses 0.0.0.0`
- Check firewall rules
- Verify Docker port mapping

### Certificate Errors
- Ensure DNS name matches: `--dns-address-cert your-hostname`
- Regenerate certificates: `rm -rf .security/` and restart

### Authentication Failures
- Verify credentials match
- Check anonymous access setting
- Review OPC UA client security policy

## Best Practices

### Security
1. ✅ Always change default passwords in production
2. ✅ Disable anonymous access for sensitive systems
3. ✅ Use TLS/SSL with valid certificates
4. ✅ Restrict bind addresses to specific interfaces
5. ✅ Mount security directory as Docker volume

### Performance
1. ✅ Allocate sufficient memory (2GB minimum)
2. ✅ Use persistent volumes for certificates
3. ✅ Monitor with health checks
4. ✅ Tune JVM options for large deployments

### Maintenance
1. ✅ Regular Docker image updates
2. ✅ Backup security directory
3. ✅ Monitor logs for errors
4. ✅ Document custom configurations

## Additional Resources

- Full Documentation: [docs/README.md](docs/README.md)
- Docker Examples: [examples/README.md](examples/README.md)
- GitHub: https://github.com/rossonet/waldot
- Issues: https://github.com/rossonet/waldot/issues

---

**Rossonet s.c.a r.l.** - https://www.rossonet.net
