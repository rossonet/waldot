# 4. Complete bootstrap

The plant of pages 1-3 in one bootstrap file, plus a Gremlin Server opened by
a plugin command. This file has been executed as it is.

## boot.conf

```javascript
// --- 01 plain vertex
var pump = graph.addVertex('id', 'pump-1', 'label', 'pump-1', 'directory', 'plant/pumps',
    'manufacturer', 'ACME', 'power-kw', 7.5, 'running', true);
var tank = graph.addVertex('id', 'tank-1', 'label', 'tank-1', 'directory', 'plant/tanks', 'level', 0.0);
pump.addEdge('fills', tank);
log.info('01 out of pump: ' + g.V().has('id', 'pump-1').out('fills').values('label').next());

// --- 02 link-to with rename, deadband, delay
var gen = graph.addVertex('id', 'level-gen', 'type', 'sim:generator', 'label', 'level-gen', 'directory', 'plant/sim',
    'algorithm', 'sinusoidal', 'min', '0', 'max', '100', 'delay', '200');
gen.addEdge('level-signal', tank, 'type', 'link-to', 'properties-to-monitor', 'data', 'data', 'level',
    'deadband-type', 'absolute', 'deadband-value', 10);
var mirror = graph.addVertex('id', 'tank-1-hmi', 'label', 'tank-1-hmi', 'directory', 'plant/hmi', 'level', 0.0);
tank.addEdge('to-hmi', mirror, 'type', 'link-to', 'properties-to-monitor', 'level', 'delay', 2000);

// --- 02 link-from: the valve pulls the command from the controller
var ctrl = graph.addVertex('id', 'ctrl-1', 'label', 'ctrl-1', 'directory', 'plant/ctrl', 'output', 0.0);
var valve = graph.addVertex('id', 'valve-1', 'label', 'valve-1', 'directory', 'plant/valves', 'opening', 0.0);
valve.addEdge('command', ctrl, 'type', 'link-from', 'properties-to-monitor', 'output', 'output', 'opening');
ctrl.property('output', 42.0);
log.info('02 valve opening after link-from: ' + g.V().has('id', 'valve-1').values('opening').next());

// --- 03 rule
var compute = graph.addVertex('id', 'plant-compute', 'type', 'rules:compute', 'label', 'plant-compute',
    'directory', 'plant/rules', 'threads', '2');
var high = graph.addVertex('id', 'tank-high', 'type', 'rules:rule', 'label', 'tank-high', 'directory', 'plant/rules',
    'condition', "g.V().has('id','tank-1').values('level').next() > 80",
    'action', "g.V().has('id','ctrl-1').next().property('output', 0.0); log.warn('tank-1 high level, valve closed')",
    'hysteresis', '1000');
compute.addEdge('runner', high, 'type', 'rules:execute', 'priority', '100');
tank.addEdge('watch-level', high, 'type', 'fire', 'properties-to-monitor', 'level');

// --- 03 event generator
var ev = graph.addVertex('id', 'alarm-sim', 'type', 'sim:event-generator', 'label', 'alarm-sim', 'directory', 'plant/sim',
    'messages', 'pump overload|valve stuck|low pressure', 'delay', '3000', 'event-type', 'device-failure');

// --- tinkerpop: label, directory, port, bind
var gs = tinkerpop_create_gremlin_server.exec('gremlin-1', 'services', '8182', '0.0.0.0');
log.info('gremlin server: ' + gs[0]);
```

## Run

```bash
docker run --rm --name waldot -p 12686:12686 -p 8182:8182 \
  -v "$PWD/boot.conf:/waldot/boot.conf:ro" rossonet/waldot:latest
```

Log:

```
INFO BOOT - Bootstrap configuration loaded in SCRIPT MODE from URL: [file:///waldot/boot.conf]
INFO CONSOLE - 01 out of pump: tank-1
INFO CONSOLE - 02 valve opening after link-from: 42.0
INFO CONSOLE - gremlin server: ns=2;i=120001
INFO BOOT - Bootstrap procedure completed successfully.
```

## Query the graph with Gremlin

```bash
curl -X POST -d '{"gremlin":"g.V().values(\"label\").fold()"}' http://127.0.0.1:8182
```

```
... "@value":["tank-1","tank-1-hmi","tank-high","alarm-sim","level-gen","pump-1","ctrl-1","plant-compute","valve-1","gremlin-1"] ...
```

The Gremlin Server works on the same graph published in OPC UA: a vertex added
with Gremlin appears in the address space.

## Notes

- Plugin commands are available as `<folder>_<command name>` with spaces
  replaced by `_`; `exec(...)` takes the OPC UA input arguments as strings.
- An error in the script stops it: the vertices created before the error
  remain. Use `try { ... } catch (var e) { ... }` for optional parts.
- `cmd.reset()` removes every vertex and edge and then runs the file again:
  the graph is rebuilt from scratch, with the same ids. A `cmd.reset()` written
  in the bootstrap file itself is ignored.
- To remove only a part of the graph use `delete directory` (or
  `graph.getWaldotNamespace().deleteDirectory('v:<directory>')`): the
  vertices and edges of the folder and of its sub folders are removed.

Next: [5. Showcase: Dungeon Arena](05-showcase-arena.md).
