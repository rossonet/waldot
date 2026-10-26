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

### 4. Production Configuration
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

### 5. Multiple Instances
**File**: `docker-compose-multiple.yml`

Runs multiple WaldOT instances on different ports.

```bash
docker-compose -f docker-compose-multiple.yml up
```

**Access**:
- Instance 1: `opc.tcp://localhost:12686/waldot`
- Instance 2: `opc.tcp://localhost:12687/waldot`
- Instance 3: `opc.tcp://localhost:12688/waldot`

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

## Passing Options to the Container

The image `ENTRYPOINT` already contains the `java` launcher, the class path
and the main class (`net.rossonet.agent.MainAgent`); `CMD` is empty. Every
item of a Compose `command:` (or every argument after the image name in
`docker run`) is passed to WaldOT as a command line option:

```yaml
command:
  - "--tcp-port"
  - "4840"
  - "--anonymous-access"
  - "false"
```

```bash
docker run --rm -p 4840:4840 rossonet/waldot:latest --tcp-port 4840
```

Configuration is done **only** through command line options; JVM options
go in the standard `JDK_JAVA_OPTIONS` variable. Boolean options accept an
explicit value (`--anonymous-access false`); used alone they mean `true`.

The container runs as the non-root user `waldot` (uid/gid `1000`). Host
directories mounted for writing (for example `./security` on
`/app/.security`) must be writable by uid `1000`.

The complete list of options with defaults is in the
[Configuration Reference](../../docs/guide/docs/CONFIGURATION.md).

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
sudo chown 1000:1000 security
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
Add JVM tuning parameters with the standard `JDK_JAVA_OPTIONS` variable,
read by the `java` launcher:

```yaml
environment:
  - JDK_JAVA_OPTIONS=-Xmx2g -Xms1g -XX:+UseG1GC
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
- Documentation: https://github.com/rossonet/waldot/tree/master/docs/guide
- Community: https://github.com/rossonet/waldot/discussions
