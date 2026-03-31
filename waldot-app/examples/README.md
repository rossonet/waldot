# WaldOT Docker Compose Examples

This directory contains Docker Compose examples demonstrating various WaldOT OPC UA server configurations using Picocli command-line parameters.

## Available Examples

### 1. Basic Default Configuration
**File**: `docker-compose-default.yml`

Runs WaldOT with default settings - the simplest deployment.

```bash
docker-compose -f docker-compose-default.yml up
```

**Access**:
- OPC UA: `opc.tcp://localhost:12686/waldot`
- HTTPS: `https://localhost:8443`

### 2. Custom Ports Configuration
**File**: `docker-compose-custom-ports.yml`

Demonstrates custom TCP and HTTPS port configuration.

```bash
docker-compose -f docker-compose-custom-ports.yml up
```

**Access**:
- OPC UA: `opc.tcp://localhost:4840/waldot`
- HTTPS: `https://localhost:8080`

### 3. Secure Configuration (No Anonymous Access)
**File**: `docker-compose-secure.yml`

Disables anonymous access and uses custom authentication.

```bash
docker-compose -f docker-compose-secure.yml up
```

**Access**:
- OPC UA: `opc.tcp://localhost:12686/waldot`
- Username: `industrial_admin`
- Password: `SecureP@ssw0rd!`

### 4. Custom Network Configuration
**File**: `docker-compose-network.yml`

Custom network binding and hostname configuration.

```bash
docker-compose -f docker-compose-network.yml up
```

**Access**:
- OPC UA: `opc.tcp://waldot-server:12686/waldot`

### 5. Production Configuration
**File**: `docker-compose-production.yml`

Production-ready deployment with security volumes and custom branding.

```bash
docker-compose -f docker-compose-production.yml up -d
```

**Features**:
- Persistent security certificates
- Custom application branding
- Mounted help documentation
- Health check monitoring

### 6. Multiple Instances
**File**: `docker-compose-multiple.yml`

Runs multiple WaldOT instances on different ports.

```bash
docker-compose -f docker-compose-multiple.yml up
```

**Access**:
- Instance 1: `opc.tcp://localhost:12686/waldot`
- Instance 2: `opc.tcp://localhost:12687/waldot`
- Instance 3: `opc.tcp://localhost:12688/waldot`

### 7. Development Configuration
**File**: `docker-compose-dev.yml`

Development setup with volume mounts and debugging enabled.

```bash
docker-compose -f docker-compose-dev.yml up
```

## Testing with OPC UA Clients

### Using UaExpert (Windows)
1. Download from: https://www.unified-automation.com/products/development-tools/uaexpert.html
2. Add Server: File → Add Server
3. Enter endpoint: `opc.tcp://localhost:12686/waldot`
4. Connect and browse the address space

### Using Prosys OPC UA Browser (Cross-platform)
1. Download from: https://www.prosysopc.com/products/opc-ua-browser/
2. Connect to: `opc.tcp://localhost:12686/waldot`

### Using opcua-client-gui (Open Source)
```bash
pip install opcua-client
opcua-client --endpoint opc.tcp://localhost:12686/waldot
```

## Environment Variables Reference

All Picocli parameters can be set via environment variables using the format `WALDOT_<OPTION_NAME>`:

### OPC UA Configuration
| Environment Variable | Option | Default | Description |
|---------------------|--------|---------|-------------|
| `WALDOT_TCP_PORT` | `--tcp-port` | `12686` | TCP port for OPC UA |
| `WALDOT_HTTPS_PORT` | `--https-port` | `8443` | HTTPS port |
| `WALDOT_APPLICATION_NAME` | `--application-name` | `WaldOT OPCUA server` | Server name |
| `WALDOT_BIND_ADDRESSES` | `--bind-addresses` | `0.0.0.0` | Bind addresses |
| `WALDOT_BIND_HOSTNAME` | `--bind-hostname` | `127.0.0.1` | Hostname |
| `WALDOT_ENDPOINT_PATH` | `--endpoint-path` | `/waldot` | URL path |
| `WALDOT_PRODUCT_NAME` | `--product-name` | `WaldOT` | Product name |
| `WALDOT_MANUFACTURER_NAME` | `--manufacturer-name` | `Rossonet s.c.a r.l.` | Manufacturer |
| `WALDOT_SECURITY_DIR` | `--security-dir` | `.security` | Security directory |

### WaldOT Application Configuration
| Environment Variable | Option | Default | Description |
|---------------------|--------|---------|-------------|
| `WALDOT_ANONYMOUS_ACCESS` | `--anonymous-access` | `true` | Allow anonymous |
| `WALDOT_FACTORY_USERNAME` | `--factory-username` | `admin` | Default username |
| `WALDOT_FACTORY_PASSWORD` | `--factory-password` | `password123` | Default password |
| `WALDOT_NAMESPACE_URI` | `--namespace-uri` | `urn:rossonet:waldot:engine` | Namespace URI |
| `WALDOT_HELP_DIRECTORY` | `--help-directory` | `/app/help` | Help files path |
| `WALDOT_BOOT_URL` | `--boot-url` | `file:///waldot/boot.conf` | Bootstrap config |

### Node Configuration
| Environment Variable | Option | Default | Description |
|---------------------|--------|---------|-------------|
| `WALDOT_ROOT_NODE_ID` | `--root-node-id` | `waldot` | Root node ID |
| `WALDOT_ROOT_NODE_BROWSE_NAME` | `--root-node-browse-name` | `Gremlin Engine` | Root browse name |
| `WALDOT_ASSET_NODE_ID` | `--asset-node-id` | `aas` | Asset node ID |
| `WALDOT_INTERFACE_NODE_ID` | `--interface-node-id` | `cmd` | Interface node ID |

## Troubleshooting

### Port Already in Use
If you see "port already allocated" error:
```bash
# Find process using the port
lsof -i :12686

# Or use a different port
docker-compose -f docker-compose-custom-ports.yml up
```

### Permission Denied for Security Directory
Ensure the security directory has correct permissions:
```bash
mkdir -p security
chmod 700 security
```

### Container Exits Immediately
Check logs for errors:
```bash
docker-compose logs waldot
```

### Cannot Connect from OPC UA Client
Verify firewall rules and network configuration:
```bash
# Test port connectivity
telnet localhost 12686
```

## Advanced Usage

### Custom Bootstrap Configuration
Create a custom `boot.conf` file and mount it:

```yaml
volumes:
  - ./custom-boot.conf:/waldot/boot.conf:ro
```

### Persistent Security Certificates
Mount a volume for certificates:

```yaml
volumes:
  - ./security:/app/.security
```

### Custom Help Documentation
Provide custom help files:

```yaml
volumes:
  - ./custom-help:/app/help:ro
```

## Performance Tuning

### JVM Options
Add JVM tuning parameters:

```yaml
environment:
  - JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC
```

### Increase Virtual Threads
For high-load scenarios, tune thread pools in your bootstrap configuration.

## Integration Testing

### Testing with Python
```python
from opcua import Client

client = Client("opc.tcp://localhost:12686/waldot")
client.connect()

# Browse root
root = client.get_root_node()
print("Root:", root)

# Query nodes
objects = client.get_objects_node()
print("Objects:", objects.get_children())

client.disconnect()
```

### Testing with Node.js
```javascript
const { OPCUAClient } = require("node-opcua");

const client = OPCUAClient.create({
    endpointMustExist: false,
});

const endpointUrl = "opc.tcp://localhost:12686/waldot";

await client.connect(endpointUrl);
const session = await client.createSession();

// Browse address space
const browseResult = await session.browse("RootFolder");
console.log("Nodes:", browseResult.references);

await session.close();
await client.disconnect();
```

## Monitoring

### Health Check
All examples include health checks. Check status:

```bash
docker-compose ps
```

### Logs
View real-time logs:

```bash
docker-compose logs -f waldot
```

### Metrics
Access metrics endpoint:

```bash
curl http://localhost:8443/metrics
```

## Cleanup

Stop and remove containers:

```bash
docker-compose down
```

Remove volumes:

```bash
docker-compose down -v
```

## Contributing

Add more examples by:
1. Creating a new `docker-compose-<name>.yml` file
2. Documenting it in this README
3. Testing thoroughly
4. Submitting a pull request

## License

These examples are part of the WaldOT project, licensed under Apache License 2.0.

## Support

- GitHub Issues: https://github.com/rossonet/waldot/issues
- Documentation: https://github.com/rossonet/waldot/docs
- Community: https://github.com/rossonet/waldot/discussions
