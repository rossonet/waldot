# WaldOT Namespace

The `waldot-namespace` module is the runtime bridge between OPC UA and the in-memory TinkerPop graph used by WaldOT.

## What this module does

- exposes graph vertices, edges, properties, and commands as OPC UA nodes;
- executes bootstrap scripts at startup;
- manages graph mutation commands (`query`, `help`, `about`, edge helpers);
- exchanges structured data with the clients (JSON queries, snapshots, SVG/DOT drawings of the graph);
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

## OPC UA commands from Gremlin

The commands of vertices and edges (`delete`, `property`, plugin commands) and
the global console commands can be run from any Gremlin query with the
TinkerPop `call()` step. `OpcGraph` always registers two services:

- `waldot.command` (`OpcCommandServiceFactory`): runs a command;
- `waldot.commands` (`OpcCommandListServiceFactory`): lists the commands
  (`command`, `description`, `inputs`), runs nothing, always available.

```javascript
// JEXL console / bootstrap
g.V('device-1').call('waldot.command', {'command': 'property', 'args': ['color', 'red']}).toList();
g.V().has('type', 'tmp').call('waldot.command', {'command': 'delete'}).toList();
g.call('waldot.command', {'command': 'about'}).toList();   // global command
g.V('device-1').call('waldot.commands').toList();
```

```groovy
// remote Gremlin Server
g.V('device-1').call('waldot.command', [command: 'property', args: ['color', 'red']]).toList()
```

- **Flag**: `waldot.gremlin.commands.enabled`, default `false`. Read at every
  query from the namespace configuration, then the system property with the
  same name (`GremlinCommandSettings`). When it is off the query fails with an
  `IllegalStateException` naming the flag.
- **Barrier mode**: mid traversal all the elements are collected first, then
  the command runs once per element (bulk ignored), in order, in the query
  thread. Optional parameter `ChunkSize` to run smaller groups.
- **Result**: one map per invocation with `element` (NodeId as
  `toParseableString()`, `null` for global commands), `command`, `outputs`
  (output argument name, `out0`... when missing, to value) and `error`
  (`null` or the message). Unknown/ambiguous commands, removed elements,
  `Executable`/`UserExecutable` false, missing arguments and exceptions are
  reported in `error` and do not stop the other invocations.
- **No OPC UA user context**: commands run with `runCommand(null, args)`, so
  there is no OPC UA session nor user; only `Executable`/`UserExecutable` are
  checked.
- **Java**: `WaldotTraversalDsl.exec(command, args...)` and `commands()` are
  shortcuts for the same `call()` steps.

See `docs/guide/docs/CONFIGURATION.md` for the full reference.

## Drawing the graph (`render graph`)

The data exchange command `render graph` (folder `information`, see
`DataExchangeProtocol`) draws the graph, or a part of it, as a standalone SVG
or as Graphviz DOT. Its only structured input is `options`, a JSON object
parsed by `GraphRenderOptions` (waldot-api, shared with the client). Every key
is optional and unknown keys are rejected:

| Section      | Keys                                                                                   |
|--------------|----------------------------------------------------------------------------------------|
| `format`     | `svg` (default) or `dot`                                                               |
| `vertices`   | `roots`, `depth`, `direction`, `types`, `excludeTypes`, `labels`, `directories`, `match`, `where`, `expression`, `includeEphemeral`, `includeIsolated`, `max` |
| `edges`      | `labels`, `excludeLabels`, `types`                                                     |
| `properties` | `vertex`, `edge` (key globs), `includeRuntime` (`run!...`), `hideReserved`, `maxValueLength`, `tooltips` |
| `layout`     | `algorithm` (`layered`, `force`, `stress`, `mrtree`, `radial`), `direction`, `groupBy` (`directory`, `type`), `spacing` |
| `style`      | `theme` (`auto`, `light`, `dark`), `colorBy`, `legend`, `edgeLabels`, `title`, `highlight` |

The command vertices are never drawn; the ephemeral elements and the runtime
properties only on request. With `roots` the vertices are those reached within
`depth` hops along the accepted edges. Only the edges between drawn vertices are
drawn, the others are reported in the `warnings` output. The SVG layout is
computed by Eclipse ELK (`net.rossonet.waldot.commands.render`), so no Graphviz
is needed on the server. `-Dwaldot.commands.exchange.max-render-vertices`
(default 5000) caps `vertices.max`. A drawing bigger than `blockSize` is
downloaded with `read graph block`, like a snapshot.

From the client: `client.graph().render(options)`; from the command line:
`waldot render -f graph.svg ...`.

## Closing the namespace, the plugins and the vertices

`HomunculusNamespace.close()` releases everything the namespace started, so a
closed namespace (and the whole address space of its OPC UA server) can be
garbage collected. It runs only once (a second call does nothing), every step
is isolated (an exception is logged and the shutdown goes on) and steps
longer than 1 s are logged with their duration.

Order:

1. namespace timer, bootstrap procedure strategy and console strategy;
2. `close()` of every `AutoCloseable` vertex of the graph: the vertices are
   not removed from the graph (no address space change, no event), each one
   is closed at most once;
3. for every plugin `PluginListener.stop()` and then, if the plugin
   implements `AutoCloseable`, `close()`;
4. a second pass on the vertices created meanwhile (for example by a plugin
   task still running during step 3);
5. client management, OPC mapping and history strategies;
6. OPC UA server;
7. static per-namespace registry (`MiloSingleServerBaseReferenceNodeBuilder.unregister`,
   the legacy static references move to another open namespace or become
   `null`);
8. Gremlin graph (`OpcGraph.close()`, idempotent).

Semantics that plugins and vertices must respect:

- `stop()` is a restartable pause (also used by `resetNameSpace()` and
  `unregisterPlugin(...)`), `close()` is the definitive release. Both are
  idempotent and must not throw after a failed startup;
- removing a vertex closes it: the default of
  `WaldotVertex.notifyRemoveVertex()` calls `close()` on `AutoCloseable`
  vertices, so `close()` must also be idempotent;
- nothing started by a plugin or a vertex (threads, executors, sockets,
  timers, static fields, listeners of external libraries) may keep a
  reference to the namespace after the close;
- `OpcFactory` closes the namespace when its startup fails.

Regression tests: `NamespaceCloseRegressionTest` (no thread, no port and no
reachable namespace after the close, idempotence) and
`NamespaceLifecycleRegressionTest`. The plugin modules cover their own
resources (for example `GeneratorCloseReleasesNamespaceTest` in
`waldot-plugin-generator`).

## Build and test

From repository root:

```bash
./gradlew :waldot-namespace:test
```

The test JVM runs with `maxHeapSize = 512m`: every test closes its namespace,
so the heap holds one namespace at a time (before the close fixes the tests
needed 2 GB because every closed namespace stayed reachable).

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