# syntax=docker/dockerfile:1
# ==============================================================================
# WaldOT base image (waldot-app)
#
# Build (BuildKit / buildx required):
#   docker buildx build --load -t rossonet/waldot:latest .
#
# Run:
#   docker run --rm -p 12686:12686 -p 8443:8443 rossonet/waldot:latest
#
# Every argument after the image name is passed to WaldOT as a command line
# option, for example:
#   docker run --rm -p 4840:4840 rossonet/waldot:latest --tcp-port 4840
#
# JVM options can be set with the standard JDK_JAVA_OPTIONS variable:
#   docker run -e JDK_JAVA_OPTIONS="-Xmx2g" rossonet/waldot:latest
#
# Bootstrap configuration: mount a file on /waldot/boot.conf (default value of
# --boot-url) or pass --boot-url with another file:// URL.
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: build the waldot-app distribution (Java 26 toolchain)
# ------------------------------------------------------------------------------
FROM --platform=$BUILDPLATFORM eclipse-temurin:26-jdk AS builder
# Git metadata shown by the "about" command. Read from the .git directory of
# the build context when available; otherwise (e.g. the repository is a git
# submodule) pass them explicitly:
#   --build-arg GIT_COMMIT=$(git rev-parse HEAD) \
#   --build-arg GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
ARG GIT_COMMIT=unknown
ARG GIT_BRANCH=unknown
WORKDIR /workspace
COPY . /workspace
RUN GIT_COMMIT="$GIT_COMMIT" GIT_BRANCH="$GIT_BRANCH" \
    ./gradlew --no-daemon clean waldot-app:distTar -x test \
 && mkdir -p /dist \
 && tar -xf waldot-app/build/distributions/*.tar -C /dist --strip-components=1

# ------------------------------------------------------------------------------
# Stage 2: runtime image
# ------------------------------------------------------------------------------
FROM eclipse-temurin:26-jre-alpine

LABEL org.opencontainers.image.title="WaldOT" \
      org.opencontainers.image.description="WaldOT base: graph engine exposed as OPC UA server with generator, rules engine and TinkerPop plugins" \
      org.opencontainers.image.vendor="Zona Autonoma srl" \
      org.opencontainers.image.source="https://github.com/rossonet/waldot" \
      org.opencontainers.image.licenses="Apache-2.0"

RUN apk upgrade --no-cache \
 && addgroup -g 1000 -S waldot \
 && adduser -u 1000 -S -G waldot -h /app waldot \
 && mkdir -p /app/help /app/.security /waldot \
 && echo "Andrea Ambrosini - Zona Autonoma srl" > /app/help/author.txt \
 && chown -R waldot:waldot /app /waldot

# Help files served by the "help" OPC UA command (--help-directory /app/help)
COPY --chown=waldot:waldot ./docker/HELP.txt /app/help/index.txt
COPY --chown=waldot:waldot ./LICENSE /app/help/license.txt

# Application libraries (waldot-app + plugins + dependencies), read-only for the runtime user
COPY --from=builder /dist/lib /app/lib

USER waldot
WORKDIR /app

# OPC UA binary (opc.tcp) and HTTPS endpoints (defaults of --tcp-port / --https-port)
EXPOSE 12686 8443

# Certificates and PKI (--security-dir defaults to .security, i.e. /app/.security)
VOLUME ["/app/.security"]

ENTRYPOINT ["java", \
  "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
  "--add-opens", "java.base/java.util=ALL-UNNAMED", \
  "--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED", \
  "-Djava.net.preferIPv4Stack=true", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-cp", "/app/lib/*", \
  "net.rossonet.agent.MainAgent"]
CMD []
