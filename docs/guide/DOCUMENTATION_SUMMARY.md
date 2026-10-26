# WaldOT Documentation Summary

Maintainer notes on how the WaldOT documentation is organized.

## Layout

| Location | Content | Published by MkDocs |
| -------- | ------- | ------------------- |
| `docs/guide/docs/` | User, operator, architect and developer guides | yes |
| `docs/guide/mkdocs.yml` | Site, navigation and PDF configuration | - |
| `<module>/README.md` | Technical notes kept next to the code of each module | linked (GitHub) |
| `waldot-namespace/BOOTSTRAP_CONFIGURATION.md` | Bootstrap file syntax and format detection | linked (GitHub) |
| `plugins/<plugin>/README.md`, `plugins/<plugin>/docs/` | Plugin guides, API, examples | linked (GitHub) |
| `waldot-app/examples/` | Docker Compose deployment variants | linked (GitHub) |
| `docs/reference/digitaltwins/` | Third-party specifications (DTDL) | no |
| `docs/artwork/` | Logos and graphics | no |

MkDocs only renders files inside `docs/guide/docs/`; module and plugin
documents are referenced through absolute GitHub URLs so that links work both
on GitHub and on the generated site.

## Guide pages

| Page | Audience | Notes |
| ---- | -------- | ----- |
| `README.md` | everybody | Documentation hub |
| `ARCHITECTURE_OVERVIEW.md` | architects, decision makers | Architecture, deployment, security |
| `USER_GUIDE.md` | operators | Operating manual: address space, vertices and edges, bootstrap, commands, rules engine, monitoring, troubleshooting |
| `examples/` | operators | Seven step-by-step pages (plain vertices, active edges, generators and rules, bootstrap, three showcases); every script verified on the Docker image |
| `CONFIGURATION.md` | operators, integrators | Every command-line option with its default, Docker notes |
| `DEVELOPER_GUIDE.md` | developers | Development workflow, plugins, testing |
| `INTEGRATION_GUIDE.md` | plugin developers | How to write a plugin: discovery, lifecycle, vertices, edges, commands, packaging, tests (example verified in a container) |

## Source of truth

- Server options: `waldot-runner/src/main/java/net/rossonet/agent/AbstractWaldotRunner.java`
  (Picocli `@Option` annotations). `CONFIGURATION.md` must be updated whenever
  an option is added or a default changes.
- Milo strategy selection: `OpcFactory.createMiloStrategy()` and the
  `waldot.milo.strategy` system property.
- Bootstrap format detection: `SingleFileBootstrapStrategy`.
- Docker image layout: `Dockerfile` (Temurin 26, non-root user `waldot`,
  `ENTRYPOINT` with `java ... net.rossonet.agent.MainAgent`, empty `CMD`:
  arguments after the image name are WaldOT options).

## Known gaps (October 2026)

- Generator and rules-engine plugin READMEs still contain Groovy examples.

Fixed in 0.6.2 and covered by regression tests (see the Developer Guide,
"Regression tests"): `--anonymous-access false` now rejects anonymous
sessions; the edge commands resolve their NodeId arguments and create the edge
from source to target; `delete directory` removes the vertices and edges of the
folder tree; `cmd.reset()` empties the graph before running the bootstrap.


## Build

```bash
cd docs/guide
pip install mkdocs mkdocs-material mkdocs-with-pdf
mkdocs serve            # http://localhost:8000
mkdocs build            # site/ and PDF (with-pdf plugin)
```

Pages present in `docs/guide/docs/` but not listed in `nav` are reported by
MkDocs as informational messages; the build must otherwise be free of
warnings.

---

**WaldOT version**: 0.6.2
