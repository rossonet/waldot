# WaldOT OPC UA Server

Runnable WaldOT server application, packaged as a distribution, fat jar,
Debian package and Docker image.

## Content

- `net.rossonet.agent.MainAgent`: entry point, runs `WaldotRunner` through
  Picocli and exits with its return code;
- `net.rossonet.agent.WaldotRunner`: concrete runner based on
  `AbstractWaldotRunner` (module [`waldot-runner`](../waldot-runner/README.md));
- bundled plugins: `waldot-plugin-generator`, `waldot-plugin-rules-engine`,
  `waldot-plugin-tinkerpop`;
- [`examples/`](examples/README.md): Docker Compose deployment variants.

## Build

```bash
# tests
./gradlew :waldot-app:test

# distribution archive (used by the Dockerfile)
./gradlew :waldot-app:distTar

# fat jar
./gradlew :waldot-app:shadowJar
java -jar waldot-app/build/libs/waldot-app-<version>-all.jar --help
```

The project targets Java 26 (see `build.gradle`).

## Run

Default endpoint: `opc.tcp://127.0.0.1:12686/waldot`.

```bash
java -jar waldot-app/build/libs/waldot-app-<version>-all.jar \
  --tcp-port 4840 \
  --boot-url file:///path/to/boot.conf \
  --anonymous-access false \
  --factory-password 'S3cure!'
```

## Docker

The repository `Dockerfile` builds the distribution with
`eclipse-temurin:26-jdk` and runs it on `eclipse-temurin:26-jre-alpine` as
the non-root user `waldot` (uid `1000`):

```bash
docker buildx build --load -t rossonet/waldot:latest .
```

The image `ENTRYPOINT` starts `net.rossonet.agent.MainAgent`, so every argument
after the image name is a WaldOT option. JVM options go in the standard
`JDK_JAVA_OPTIONS` variable; no other environment variable is read.

```bash
docker run -p 4840:4840 \
  -v "$PWD/boot.conf:/waldot/boot.conf:ro" \
  -e JDK_JAVA_OPTIONS="-Xmx1g" \
  rossonet/waldot:latest --tcp-port 4840 --anonymous-access false
```

| Path | Content |
| ---- | ------- |
| `/app/lib` | waldot-app, plugins (generator, rules engine, TinkerPop) and dependencies |
| `/app/help` | help files served by the `help` command |
| `/app/.security` | certificates and PKI (volume) |
| `/waldot/boot.conf` | bootstrap file (default `--boot-url`), mount it read-only |

Exposed ports: `12686` (OPC UA TCP) and `8443` (HTTPS). Compose examples are in
[`examples/`](examples/README.md).

All options, defaults and Docker notes are documented in the
[Configuration Reference](../docs/guide/docs/CONFIGURATION.md); bootstrap
syntax is in
[`waldot-namespace/BOOTSTRAP_CONFIGURATION.md`](../waldot-namespace/BOOTSTRAP_CONFIGURATION.md).

## Security

Defaults are meant for development: anonymous access enabled, administrator
`admin` / `password123`, `exec` command (operating-system commands) enabled.
In production set at least:

```
--anonymous-access false
--factory-password <secret>
--exec-command-executable false
--exec-command-user-executable false
```

and mount the security directory (`/app/.security` in Docker) on a persistent
volume.
