FROM  --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS builder
COPY . /workspace
RUN cd /workspace && echo "build project" && ./gradlew clean waldot-app:generateGitProperties waldot-app:distTar -Dorg.gradle.daemon=false

FROM eclipse-temurin:21-jre-alpine AS initial
RUN apk update
RUN apk upgrade
RUN mkdir -p /app
RUN mkdir -p /app/help
COPY ./docker/HELP.txt /app/help/index.txt
RUN echo "Andrea Ambrosini - Rossonet s.c.a r.l." > /app/help/author.txt
COPY ./LICENSE /app/help/license.txt
COPY --from=builder /workspace/waldot-app/build/distributions/*.tar /tmp/
RUN tar -xf /tmp/*.tar -C ./app --strip-components=1 && rm -rf /tmp/*.tar

FROM eclipse-temurin:21-jre-alpine

# ==============================================================================
# WaldOT Environment Variables - OPC UA Server Configuration
# ==============================================================================
# These environment variables can be set to configure the WaldOT OPC UA server
# at container startup. They correspond to Picocli command-line parameters.
# If not set, default values will be used.
# ==============================================================================

# Network Configuration
# TCP port for OPC UA binary protocol endpoint (opc.tcp://)
ENV WALDOT_TCP_PORT=""
# HTTPS port for OPC UA web services and REST API
ENV WALDOT_HTTPS_PORT=""
# Comma-separated list of IP addresses for server binding (e.g., 0.0.0.0 for all interfaces)
ENV WALDOT_BIND_ADDRESSES=""
# Hostname for OPC UA endpoint URLs and SSL certificates
ENV WALDOT_BIND_HOSTNAME=""
# URL path component for OPC UA endpoints (e.g., /waldot)
ENV WALDOT_ENDPOINT_PATH=""

# Server Identity Configuration
# OPC UA application name identifying this server instance
ENV WALDOT_APPLICATION_NAME=""
# Product name in OPC UA server BuildInfo
ENV WALDOT_PRODUCT_NAME=""
# Unique product URI for OPC UA Application Description
ENV WALDOT_PRODUCT_URI=""
# Manufacturer name in OPC UA BuildInfo
ENV WALDOT_MANUFACTURER_NAME=""
# Build number for version tracking in OPC UA BuildInfo
ENV WALDOT_BUILD_NUMBER=""

# Security Configuration
# DNS address or hostname used for SSL certificate generation (Subject Alternative Name)
ENV WALDOT_DNS_ADDRESS_CERT=""
# Directory path for OPC UA security certificates, private keys, and PKI trust lists
# Docker note: Mount as volume to persist security credentials across restarts
ENV WALDOT_SECURITY_DIR=""

# ==============================================================================
# WaldOT Environment Variables - Application Configuration
# ==============================================================================

# Authentication and Authorization
# Allow anonymous client connections without authentication (true/false)
# Security note: Set to false in production environments
ENV WALDOT_ANONYMOUS_ACCESS=""
# Default administrator username for initial setup
# Security warning: Change in production deployments!
ENV WALDOT_FACTORY_USERNAME=""
# Default administrator password
# Security warning: MUST change in production! This is a critical security risk if unchanged
ENV WALDOT_FACTORY_PASSWORD=""

# Application Settings
# OPC UA namespace URI for WaldOT-specific nodes and types
ENV WALDOT_NAMESPACE_URI=""
# Directory containing help documentation files
ENV WALDOT_HELP_DIRECTORY=""
# File URL for bootstrap configuration to load on startup
# Format: file:// URL pointing to configuration file
ENV WALDOT_BOOT_URL=""

# Rules Engine Configuration
# Delay in milliseconds before facts become valid in rules engine
ENV WALDOT_FACTS_VALID_DELAY=""
# Time in milliseconds until facts expire in rules engine (0=never expires)
ENV WALDOT_FACTS_VALID_UNTIL=""

# ==============================================================================
# WaldOT Environment Variables - Node Configuration
# ==============================================================================

# Root Node Configuration
# NodeId for the WaldOT root node in OPC UA address space
ENV WALDOT_ROOT_NODE_ID=""
# Browse name for the root node (identifier without spaces)
ENV WALDOT_ROOT_NODE_BROWSE_NAME=""
# Human-readable display name for the root node
ENV WALDOT_ROOT_NODE_DISPLAY_NAME=""

# Asset Root Node Configuration
# NodeId for asset administration root node
ENV WALDOT_ASSET_NODE_ID=""
# Browse name for asset root node
ENV WALDOT_ASSET_NODE_BROWSE_NAME=""
# Display name for asset root node
ENV WALDOT_ASSET_NODE_DISPLAY_NAME=""

# Interface Root Node Configuration
# NodeId for command interface root node
ENV WALDOT_INTERFACE_NODE_ID=""
# Browse name for interface root node
ENV WALDOT_INTERFACE_NODE_BROWSE_NAME=""
# Display name for interface root node
ENV WALDOT_INTERFACE_NODE_DISPLAY_NAME=""

# ==============================================================================
# WaldOT Environment Variables - Command Configuration
# ==============================================================================

# About Command Configuration
# Label for the about command
ENV WALDOT_ABOUT_COMMAND_LABEL=""
# Description for the about command
ENV WALDOT_ABOUT_COMMAND_DESCRIPTION=""
# Enable about command execution (true/false)
ENV WALDOT_ABOUT_COMMAND_EXECUTABLE=""
# Allow users to execute about command (true/false)
ENV WALDOT_ABOUT_COMMAND_USER_EXECUTABLE=""

# Exec Command Configuration
# Label for the exec system command
# Security warning: This command allows executing system commands - restrict in production!
ENV WALDOT_EXEC_COMMAND_LABEL=""
# Description for the exec command
ENV WALDOT_EXEC_COMMAND_DESCRIPTION=""
# Enable exec command execution (true/false)
ENV WALDOT_EXEC_COMMAND_EXECUTABLE=""
# Allow users to execute system commands (true/false)
# Security warning: Dangerous if enabled - allows arbitrary command execution!
ENV WALDOT_EXEC_COMMAND_USER_EXECUTABLE=""

# Help Command Configuration
# Label for the help command that lists available commands
ENV WALDOT_HELP_COMMAND_LABEL=""
# Description for the help command
ENV WALDOT_HELP_COMMAND_DESCRIPTION=""
# Enable help command execution (true/false)
ENV WALDOT_HELP_COMMAND_EXECUTABLE=""
# Allow users to execute help command (true/false)
ENV WALDOT_HELP_COMMAND_USER_EXECUTABLE=""

# Waldot/Gremlin Query Command Configuration
# Label for Gremlin query execution command
# This command allows executing Gremlin queries from OPC UA clients
ENV WALDOT_WALDOT_COMMAND_LABEL=""
# Description for Gremlin query command
ENV WALDOT_WALDOT_COMMAND_DESCRIPTION=""
# Enable Gremlin query execution (true/false)
ENV WALDOT_WALDOT_COMMAND_EXECUTABLE=""
# Allow users to execute Gremlin queries (true/false)
# Security note: Queries have full graph access - consider restricting in production
ENV WALDOT_WALDOT_COMMAND_USER_EXECUTABLE=""

# ==============================================================================
# Container Configuration
# ==============================================================================

# Expose default OPC UA TCP port
EXPOSE 12686

# Expose default HTTPS port
EXPOSE 8443

# Set working directory
WORKDIR /app

# Copy application from initial stage
COPY --from=initial / /

# Set entry point and default command
# The MainAgent class will read environment variables via Picocli
ENTRYPOINT ["java"]
CMD ["-cp","/app/lib/*","-XX:+UnlockExperimentalVMOptions","-Djava.net.preferIPv4Stack=true","-XshowSettings:vm","-Djava.security.egd=file:/dev/./urandom", "net.rossonet.agent.MainAgent"]
