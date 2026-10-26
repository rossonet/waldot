# WaldOT Configuration Reference

This page documents every runtime parameter accepted by the WaldOT server
(`waldot-app`). The source of truth is
`waldot-runner/src/main/java/net/rossonet/agent/AbstractWaldotRunner.java`:
all options are declared there with [Picocli](https://picocli.info/) and
inherited by `net.rossonet.agent.WaldotRunner`.

## How configuration is applied

WaldOT reads its configuration from three sources:

| Source | Example | Notes |
| ------ | ------- | ----- |
| Command-line options | `--tcp-port 4840` | Primary mechanism, parsed by Picocli. |
| Java system properties | `-Dwaldot.milo.strategy=base` | Low-level switches of the namespace module. |
| Bootstrap file | `--boot-url file:///waldot/boot.conf` | Initial graph content (see [Bootstrap](#bootstrap-file)). |

Picocli also supports `@file` argument files (`showAtFileInUsageHelp` is
enabled), so a long option list can be stored in a file and passed as
`@/path/to/waldot.args`.

Environment variables are **not** used to configure WaldOT: every runtime
parameter is a command-line option. In Docker, options are simply appended
after the image name (see [Docker usage](#docker-usage)).

Boolean options (`--anonymous-access` and all `*-executable` /
`*-user-executable` flags) accept an optional value: `--anonymous-access false`
disables the feature, the option alone (`--anonymous-access`) means `true`.

To print the full, always up-to-date option list:

```bash
# Docker image
docker run --rm rossonet/waldot:latest --help
# fat jar built with ./gradlew waldot-app:shadowJar
java -jar waldot-app/build/libs/waldot-app-<version>-all.jar --help
```

## Network options

| Option | Short | Default | Description |
| ------ | ----- | ------- | ----------- |
| `--tcp-port` | `-tp` | `12686` | TCP port of the OPC UA binary endpoint (`opc.tcp://`). |
| `--https-port` | `-hp` | `8443` | HTTPS port for OPC UA web services. |
| `--bind-addresses` | `-ba` | `0.0.0.0` | Comma-separated list of bind addresses. |
| `--bind-hostname` | `-bh` | `127.0.0.1` | Hostname published in endpoint URLs and certificates. |
| `--endpoint-path` | `-ep` | `/waldot` | Path component of the endpoint URL. |

The resulting endpoint is `opc.tcp://<bind-hostname>:<tcp-port><endpoint-path>`,
for example `opc.tcp://127.0.0.1:12686/waldot`.

## Server identity

| Option | Short | Default | Description |
| ------ | ----- | ------- | ----------- |
| `--application-name` | `-an` | `WaldOT OPCUA server` | OPC UA application name. |
| `--product-name` | `-pn` | `WaldOT` | Product name in `BuildInfo`. |
| `--product-uri` | `-pu` | `urn:rossonet:waldot:uaserver` | Product URI in the application description. |
| `--manufacturer-name` | `-mn` | `Zona Autonoma srl` | Manufacturer name in `BuildInfo`. |
| `--build-number` | `-bn` | `w001` | Build number in `BuildInfo`. |
| `--namespace-uri` | `-nu` | `urn:rossonet:waldot:engine` | URI of the WaldOT namespace. |

## Security and authentication

| Option | Short | Default | Description |
| ------ | ----- | ------- | ----------- |
| `--security-dir` | `-sd` | `.security` | Directory for certificates, private keys and PKI trust lists (relative to the working directory, `/app` in Docker). |
| `--dns-address-cert` | `-dc` | `127.0.0.1` | DNS name used as Subject Alternative Name of the generated certificate. |
| `--anonymous-access` | `-aa` | `true` | Allow anonymous OPC UA sessions. |
| `--factory-username` | `-fu` | `admin` | Initial administrator user name. |
| `--factory-password` | `-fp` | `password123` | Initial administrator password. **Change it in production.** |

!!! danger "Production hardening"
    With the default values anonymous access is enabled, the administrator
    password is public and the `exec` command (which runs operating-system
    commands) is enabled. Always override `--anonymous-access`,
    `--factory-password` and `--exec-command-executable` outside development.

!!! note "Anonymous access disabled"
    With `--anonymous-access false` the endpoints no longer advertise the
    anonymous user token policy and anonymous `ActivateSession` requests are
    rejected with `BadIdentityTokenInvalid`. Clients must connect with
    username/password (`--factory-username` / `--factory-password`) or with an
    X.509 certificate. Example:

    ```bash
    docker run -d --name waldot -p 12686:12686 rossonet/waldot:latest \
      --anonymous-access false --factory-password 'S3cret!'
    ```

## Application settings

| Option | Short | Default | Description |
| ------ | ----- | ------- | ----------- |
| `--boot-url` | `-bu` | `file:///waldot/boot.conf` | URL of the bootstrap file (`file://`, `http://`, `https://`). |
| `--help-directory` | `-hd` | `/app/help` | Directory with the help files exposed by the `help` command. |
| `--facts-valid-delay` | | `0` | Default delay (ms) before a fact becomes valid in the rules engine. |
| `--facts-valid-until` | | `0` | Default validity (ms) of a fact in the rules engine (`0` = never expires). |

## Address-space root nodes

| Option | Default | Description |
| ------ | ------- | ----------- |
| `--root-node-id` | `waldot` | NodeId of the WaldOT root folder. |
| `--root-node-browse-name` | `Gremlin Engine` | Browse name of the root folder. |
| `--root-node-display-name` | `Gremlin Engine` | Display name of the root folder. |
| `--asset-node-id` | `aas` | NodeId of the asset administration folder. |
| `--asset-node-browse-name` | `Administration` | Browse name of the asset folder. |
| `--asset-node-display-name` | `Administration` | Display name of the asset folder. |
| `--interface-node-id` | `cmd` | NodeId of the commands folder. |
| `--interface-node-browse-name` | `Commands` | Browse name of the commands folder. |
| `--interface-node-display-name` | `Commands` | Display name of the commands folder. |

## Built-in OPC UA commands

Each built-in command is an OPC UA method under the commands folder and is
configured by four options with the same pattern:
`--<cmd>-command-label`, `--<cmd>-command-description`,
`--<cmd>-command-executable`, `--<cmd>-command-user-executable`.

| Command (`<cmd>`) | Default label | Default description | Purpose |
| ----------------- | ------------- | ------------------- | ------- |
| `about` | `about` | `info about this software` | Software information. |
| `help` | `help` | `list available commands` | Lists available commands / help files. |
| `waldot` | `query` | `run Gremlin query` | Executes a JEXL expression (with `g`, `graph`, `log`, `cmd`) on the graph. |
| `exec` | `exec` | `run system command` | Executes an operating-system command. |

All `*-executable` and `*-user-executable` flags default to `true`.

Example: disable the `exec` command entirely

```bash
--exec-command-executable false --exec-command-user-executable false
```

## Java system properties

| Property | Default | Description |
| -------- | ------- | ----------- |
| `waldot.milo.strategy` | `optimized` | Milo strategy used by the namespace: `optimized` or `base` (legacy). Unknown values fall back to `optimized`. |
| `waldot.boot.file` | `/tmp/boot.conf` | Bootstrap file used by `OpcFactory.getOpcGraph()` (embedded/test usage only, not by the server runner). |

## Bootstrap file

The bootstrap file is loaded at startup from `--boot-url`. Its format is
detected automatically:

- **script mode** (recommended): the whole file is one
  [JEXL](https://commons.apache.org/proper/commons-jexl/) script with
  variables, functions, loops and `//` comments; every statement must end
  with `;`;
- **line mode** (legacy): one expression per line, `#` comments.

The file is executed again by `cmd.reset()`, after every vertex and edge of the
graph has been removed.

The detailed syntax and detection rules are maintained in
[`waldot-namespace/BOOTSTRAP_CONFIGURATION.md`](https://github.com/rossonet/waldot/blob/master/waldot-namespace/BOOTSTRAP_CONFIGURATION.md).

## Docker usage

The image is built from the repository `Dockerfile` (multi-stage build:
`eclipse-temurin:26-jdk` to compile, `eclipse-temurin:26-jre-alpine` at
runtime):

```bash
docker buildx build --load -t rossonet/waldot:latest .
```

Image layout:

| Item | Value |
| ---- | ----- |
| Working directory | `/app` |
| Libraries | `/app/lib` (waldot-app, generator, rules engine and TinkerPop plugins, dependencies) |
| Help files | `/app/help` (`index.txt`, `author.txt`, `license.txt`) |
| Security directory | `/app/.security` (declared as volume) |
| Bootstrap file | `/waldot/boot.conf` (mount it, default of `--boot-url`) |
| Exposed ports | `12686` (OPC UA TCP), `8443` (HTTPS) |
| User | `waldot` (uid/gid `1000`, non-root) |
| Entrypoint | `java --add-opens ... -cp /app/lib/* net.rossonet.agent.MainAgent` |
| Default command | empty |

Every argument after the image name is passed to WaldOT as an option:

```bash
docker run -p 4840:4840 \
  -v "$PWD/boot.conf:/waldot/boot.conf:ro" \
  rossonet/waldot:latest \
  --tcp-port 4840 \
  --anonymous-access false \
  --factory-password 'S3cure!' \
  --exec-command-executable false
```

The same applies to `command:` in Docker Compose:

```yaml
services:
  waldot:
    image: rossonet/waldot:latest
    ports:
      - "4840:4840"
    volumes:
      - waldot-security:/app/.security
      - ./boot.conf:/waldot/boot.conf:ro
    command:
      - "--tcp-port"
      - "4840"
volumes:
  waldot-security:
```

JVM options (heap size, system properties) are supplied through the standard
`JDK_JAVA_OPTIONS` variable, read by the `java` launcher itself:

```bash
docker run -e JDK_JAVA_OPTIONS="-Xmx2g -Dwaldot.milo.strategy=optimized" rossonet/waldot:latest
```

If no file is mounted on `/waldot/boot.conf` the server starts with an empty
graph and logs `the bootstrap configuration is empty or null`.

Host directories mounted for writing (for example a bind mount on
`/app/.security`) must be writable by uid `1000`.

Ready-to-use Compose files are available in
[`waldot-app/examples`](https://github.com/rossonet/waldot/tree/master/waldot-app/examples).

## Exit codes

`net.rossonet.agent.MainAgent` terminates the JVM with the code returned by
Picocli: `0` on normal shutdown, `1` for an exception during execution,
`2` for invalid command-line arguments.
