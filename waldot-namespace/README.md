# WaldOT Namespace

The `waldot-namespace` module is the runtime bridge between OPC UA and the in-memory TinkerPop graph used by WaldOT.

## What this module does

- exposes graph vertices, edges, properties, and commands as OPC UA nodes;
- executes bootstrap scripts at startup;
- manages graph mutation commands (`query`, `help`, `about`, edge helpers);
- provides two Milo strategies:
  - `base`: legacy implementation;
  - `optimized`: hardened implementation with better consistency and indexing.

## Runtime architecture

Main integration classes:

- `net.rossonet.waldot.namespaces.HomunculusNamespace`: namespace lifecycle and orchestration;
- `net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraph`: graph facade;
- `net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerBaseStrategy`;
- `net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerOptimizedStrategy`.

## Default strategy policy

The factory now defaults to **optimized** strategy.

- Override with system property:
  - `-Dwaldot.milo.strategy=optimized` (default)
  - `-Dwaldot.milo.strategy=base`

Selection logic is implemented in `OpcFactory.createMiloStrategy()`.

## Bootstrap configuration

Bootstrap is loaded from URL (`file://`, `http://`, `https://`) and auto-detects mode:

- line-by-line mode (legacy): one expression per line, `#` comments;
- script mode (recommended): the whole file is one JEXL script, statements end with `;`.

Both modes are executed by the console engine (Apache Commons JEXL). A failed expression sets the bootstrap status to `FAULT` and is not reported as a successful procedure. Remote URLs use a connect timeout of 10 s and a read timeout of 30 s (`waldot.boot.connect-timeout-ms`, `waldot.boot.read-timeout-ms`).

See `BOOTSTRAP_CONFIGURATION.md` for syntax, detection rules, and examples.

## Build and test

From repository root:

```bash
./gradlew :waldot-namespace:test
```

Run coverage report and verification:

```bash
./gradlew :waldot-namespace:jacocoTestReport
./gradlew :waldot-namespace:jacocoTestCoverageVerification
```

## Coverage policy

This module uses a pragmatic quality gate (complex integration tests):

- line coverage >= `28%`;
- branch coverage >= `12%`.

Thresholds are configured in `waldot-namespace/build.gradle` and are meant
to be raised progressively towards the 80%/70% target.

## Notes for contributors

- keep command/input mappings explicit and tested, especially for monitored-edge commands;
- preserve graph/OPC UA parity when adding properties or references;
- prefer `optimized` strategy behavior as reference semantics for new tests.