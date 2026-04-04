# WaldOT Documentation

**Complete documentation for WaldOT - Digital Twin Engine bridging OPC UA and Apache TinkerPop**

---

## 📚 Documentation Structure

This directory contains comprehensive documentation organized for different audiences and use cases.

### Quick Navigation

- **[Documentation Hub](guide/docs/README.md)** - Start here! Central navigation for all documentation
- **[Examples](examples/README.md)** - 5 production-ready Docker Compose examples
- **[Reference Documentation](reference/waldot/README_EN.md)** - Complete technical reference

---

## 🎯 Choose Your Path

### I'm a Decision Maker / Architect

**Start here**: [Architecture Overview](guide/docs/ARCHITECTURE_OVERVIEW.md)

Learn about:

- Business value and ROI
- Technical architecture
- Deployment scenarios
- Security and compliance
- Technology comparison
- Decision framework

**Time investment**: 30-45 minutes reading

---

### I'm a Developer

**Start here**: [Developer Guide](guide/docs/DEVELOPER_GUIDE.md)

Learn about:

- Quick start (5 minutes)
- Core concepts and APIs
- Plugin development
- Code examples
- Testing and debugging
- Best practices

**Time investment**: 2-3 hours hands-on

**Then explore**: [Examples](examples/) for real-world implementations

---

### I'm an End User / Operator

**Start here**: [User Guide](guide/docs/USER_GUIDE.md)

Learn about:

- Docker deployment
- Configuration
- OPC UA interface
- Monitoring and troubleshooting
- Security best practices

**Time investment**: 1-2 hours reading + hands-on

**Then try**: [Example 01 - Industrial Monitoring](examples/01-industrial-monitoring/) for a quick demo

---

## 🚀 Quick Start

### Run Your First Example

```bash
# Clone repository
git clone https://github.com/rossonet/waldot.git
cd waldot/docs/examples/01-industrial-monitoring

# Start WaldOT
docker-compose up -d

# Access OPC UA
# opc.tcp://localhost:12686/waldot

# View logs
docker-compose logs -f waldot

# Stop
docker-compose down
```

### Build Documentation Site

```bash
cd waldot/docs/guide

# Install MkDocs
pip install mkdocs mkdocs-material mkdocs-with-pdf

# Serve locally
mkdocs serve
# Access: http://localhost:8000

# Build static site
mkdocs build
```

---

## 📖 Documentation Sections

### 1. Guide Documentation (`guide/`)

**Main Guides**:

- [Documentation Hub](guide/docs/README.md) - Central navigation
- [Architecture Overview](guide/docs/ARCHITECTURE_OVERVIEW.md) - For architects and decision makers
- [Developer Guide](guide/docs/DEVELOPER_GUIDE.md) - For software engineers
- [User Guide](guide/docs/USER_GUIDE.md) - For operators and administrators

**Italian Documentation**:

- [Manuale Utente](guide/docs/manuale_utente.md) - Complete user manual (Italian)
- [Guida Plugin](guide/docs/manuale_plugins.md) - Plugin development guide (Italian)
- [Guida Integrazione](guide/docs/manuale_integrazione.md) - Integration guide (Italian)

**Configuration**:

- [mkdocs.yml](guide/mkdocs.yml) - MkDocs configuration with complete navigation

---

### 2. Examples (`examples/`)

**5 Production-Ready Examples**:

| Example                                                            | Complexity          | Port  | Description                          |
| ------------------------------------------------------------------ | ------------------- | ----- | ------------------------------------ |
| [01 - Industrial Monitoring](examples/01-industrial-monitoring/)   | ⭐⭐ Beginner       | 12686 | Multi-zone monitoring with alerts    |
| [02 - Production Simulation](examples/02-production-simulation/)   | ⭐⭐⭐ Intermediate | 12687 | Production line with quality control |
| [03 - Energy Monitoring](examples/03-energy-monitoring/)           | ⭐⭐⭐ Intermediate | 12688 | Energy consumption and cost tracking |
| [04 - Quality Control](examples/04-quality-control/)               | ⭐⭐⭐⭐ Advanced   | 12689 | Batch tracking and traceability      |
| [05 - Predictive Maintenance](examples/05-predictive-maintenance/) | ⭐⭐⭐⭐⭐ Expert   | 12690 | Failure prediction and maintenance   |

Each example includes:

- ✅ Complete README with architecture
- ✅ Ready-to-run `docker-compose.yml`
- ✅ Bootstrap configuration (`boot.conf`)
- ✅ Gremlin query examples
- ✅ Troubleshooting guide

---

### 3. Reference Documentation (`reference/`)

**Technical Reference**:

- [WaldOT Reference (English)](reference/waldot/README_EN.md) - Complete technical reference
- [WaldOT Reference (Italian)](reference/waldot/README_IT.md) - Documentazione tecnica completa
- [DTDL Specifications](reference/digitaltwins/dtdl/) - Digital Twin Definition Language specs

---

## 🔧 Plugin Documentation

**Core Plugins**:

- [Generator Plugin](../plugins/waldot-plugin-generator/README.md) - Data simulation
- [Rules Engine Plugin](../plugins/waldot-plugin-rules-engine/README.md) - Event-driven automation
- [TinkerPop Server Plugin](../plugins/waldot-plugin-tinkerpop/README.md) - Gremlin server access

**Plugin Development**:

- [Plugin Development Guide](guide/docs/manuale_plugins.md) - Complete guide (Italian)
- [Developer Guide - Plugin Section](guide/docs/DEVELOPER_GUIDE.md#plugin-development) - Quick tutorial (English)

---

## 📋 Configuration Reference

**Application Configuration**:

- [Configuration Guide](../waldot-app/CONFIGURATION.md) - All configuration parameters
- [Environment Variables](../waldot-app/ENVIRONMENT_VARIABLES.md) - Docker environment variables
- [Bootstrap Configuration](../waldot-namespace/BOOTSTRAP_CONFIGURATION.md) - Startup configuration

---

## 🎓 Learning Path

### Beginner Path (2-3 hours)

1. Read [User Guide](guide/docs/USER_GUIDE.md) (30 min)
2. Run [Example 01 - Industrial Monitoring](examples/01-industrial-monitoring/) (30 min)
3. Connect with OPC UA client (UaExpert/Prosys) (30 min)
4. Experiment with Gremlin queries (30 min)
5. Modify example configuration (30 min)

### Intermediate Path (1-2 days)

1. Read [Developer Guide](guide/docs/DEVELOPER_GUIDE.md) (2 hours)
2. Run all 5 examples (3 hours)
3. Study bootstrap configurations (2 hours)
4. Create custom rules (2 hours)
5. Develop simple plugin (4 hours)

### Advanced Path (1 week)

1. Read [Architecture Overview](guide/docs/ARCHITECTURE_OVERVIEW.md) (2 hours)
2. Read [Plugin Development Guide](guide/docs/manuale_plugins.md) (4 hours)
3. Study existing plugins source code (8 hours)
4. Develop custom plugin (16 hours)
5. Deploy to production environment (8 hours)

---

## 🌐 Additional Resources

### Online Resources

- **GitHub Repository**: https://github.com/rossonet/waldot
- **Docker Hub**: https://hub.docker.com/r/rossonet/waldot
- **Maven Central**: https://central.sonatype.com/search?q=net.rossonet.waldot
- **Issues & Support**: https://github.com/rossonet/waldot/issues

### External Documentation

- [Apache TinkerPop](https://tinkerpop.apache.org/docs/current/reference/)
- [Gremlin Query Language](https://tinkerpop.apache.org/docs/current/reference/#graph-traversal-steps)
- [Eclipse Milo OPC UA](https://github.com/eclipse/milo)
- [OPC UA Specification](https://reference.opcfoundation.org/)

---

## 📊 Documentation Statistics

- **Total Documentation Files**: 44 markdown files
- **New Documentation Created**: 9 comprehensive guides
- **Total Lines**: ~3,500+ lines of documentation
- **Examples**: 5 production-ready Docker Compose examples
- **Languages**: English + Italian
- **Audience Coverage**: 3 distinct user personas

---

## 🤝 Contributing

We welcome contributions to improve documentation!

**How to contribute**:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

**Documentation guidelines**:

- Follow existing structure and style
- Maintain audience-specific organization
- Include code examples where appropriate
- Test all examples before submitting
- Update navigation in `mkdocs.yml`

---

## 📝 License

All documentation is released under the **Apache License 2.0**.

---

## 🏢 Project Sponsor

[![Rossonet s.c.a r.l.](https://raw.githubusercontent.com/rossonet/images/main/artwork/rossonet-logo/png/rossonet-logo_280_115.png)](https://www.rossonet.net)

**Rossonet s.c.a r.l.**  
Industrial IoT and Edge Computing Solutions  
https://www.rossonet.net

---

## 📞 Support

- **Documentation Issues**: [GitHub Issues](https://github.com/rossonet/waldot/issues)
- **General Questions**: [GitHub Discussions](https://github.com/rossonet/waldot/discussions)
- **Commercial Support**: contact@rossonet.net

---

**WaldOT Documentation** - Complete, comprehensive, and ready to use.

**Version**: 1.0  
**Last Updated**: 2024-04-04  
**Maintained by**: Rossonet s.c.a r.l.
