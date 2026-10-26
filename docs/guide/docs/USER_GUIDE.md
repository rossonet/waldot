# WaldOT User Guide

Operating manual of a WaldOT server with the base plugins (generator, rules
engine, TinkerPop server). It explains how the graph is mapped to OPC UA, how
to create vertices and edges, how to use the active elements and what to
observe while the server runs.

Every example has been executed on the `rossonet/waldot` image. Step-by-step
pages are in [Examples](examples/README.md).

---

## 1. Start the server

```bash
docker run -d --name waldot -p 12686:12686 rossonet/waldot:latest
```

| Item | Value |
| ---- | ----- |
| OPC UA endpoint | `opc.tcp://<host>:12686/waldot` |
| Security | anonymous access enabled, user `admin` / `password123` (change them, see [Configuration](CONFIGURATION.md#security-and-authentication)) |
| Bootstrap file | `/waldot/boot.conf` (see [Bootstrap](#4-bootstrap-file)) |
| Working directory | `/app`, certificates in `/app/.security` |

With a bootstrap file and a fixed security directory:

```bash
docker run -d --name waldot \
  -p 12686:12686 \
  -v "$PWD/boot.conf:/waldot/boot.conf:ro" \
  -v waldot-security:/app/.security \
  rossonet/waldot:latest \
  --anonymous-access false --factory-password "change-me"
```

Every argument after the image name is a WaldOT option: the complete list is
in the [Configuration Reference](CONFIGURATION.md).

The server is ready when the log contains:

```
INFO BOOT - Bootstrap procedure completed successfully.
INFO WaldOT runner - WaldOT OPC UA Server started successfully
```

---

## 2. The graph in the OPC UA address space

WaldOT is a TinkerPop graph whose vertices and edges are OPC UA nodes.

```
Objects
├── Gremlin Engine                   root of the graph (ns=2;s=waldot)
│   ├── Vertices
│   │   ├── add vertex               method
│   │   └── <directory>/<subdir>/... folders created by the "directory" property
│   │       └── <vertex>             one object per vertex (ns=2;s=<id>)
│   │           └── <property>       one variable per property
│   ├── Edges
│   └── Variables
└── Commands                         OPC UA methods, grouped by folder
    ├── manage        query, delete directory, edges/...
    ├── information   help, about
    ├── simulator     create generator, create event generator
    ├── rules         create rule, create compute, edges/..., showcase/...
    └── tinkerpop     create gremlin server
```

| Graph element | OPC UA |
| ------------- | ------ |
| vertex `id` | NodeId `ns=2;s=<id>` (numeric `ns=2;i=...` when the id is not given) |
| vertex `label` | browse name and display name (cannot be changed) |
| `directory` property | folder under `Vertices` (`plant/pumps` → `Vertices/plant/pumps`) |
| other properties | variables of the vertex object; writable from a client |
| edge | reference between the two vertex nodes |
| vertex events | OPC UA events emitted by the vertex object |

Any OPC UA client can browse, read, write and subscribe: UaExpert, Prosys
Browser, `asyncua` (Python), Eclipse Milo, Node-RED.

---

## 3. Vertices and edges

### Passive and active elements

| Kind | Created with | Behaviour |
| ---- | ------------ | --------- |
| passive vertex | no `type` | stores properties, exposed as OPC UA variables |
| active vertex | `type` handled by a plugin | runs logic (generates data, evaluates rules, opens a server) |
| passive edge | no `type` | relation used by Gremlin traversals |
| active edge | `type` = `link-to`, `link-from`, `fire`, `rules:execute` | propagates values or events between vertices |

### Vertex keys

| Key | Meaning |
| --- | ------- |
| `id` | NodeId identifier, unique |
| `label` | name of the vertex |
| `type` | active type: `sim:generator`, `sim:event-generator`, `rules:rule`, `rules:compute`, `gremlin` |
| `directory` | OPC UA folder, levels separated by `/` |
| any other key | property: string, number or boolean. Use lowercase keys. |

### Active types of the base distribution

| `type` | Plugin | Main properties |
| ------ | ------ | --------------- |
| `sim:generator` | generator | `algorithm` (`incremental`, `decremental`, `random`, `sinusoidal`, `triangular`, `stopped`), `min`, `max`, `delay` (ms); output in `data` |
| `sim:event-generator` | generator | `messages` (separated by `\|`), `delay`, `event-type`, `algorithm` (`sequential`, `random`, `burst`, `stopped`); runtime `data`, `count`, `severity` |
| `rules:rule` | rules engine | `condition`, `action` (JEXL), `hysteresis`, `debug`; counters `queue`, `total`, `executed`, `errors` |
| `rules:compute` | rules engine | `threads`, `execution-timeout-ms`, `factor`; counter `queue` |
| `gremlin` | TinkerPop | Gremlin Server on `port` (default 1025), `bind` (default `0.0.0.0`) |

Complete property tables:
[generator](https://github.com/rossonet/waldot/tree/master/plugins/waldot-plugin-generator),
[rules engine](https://github.com/rossonet/waldot/tree/master/plugins/waldot-plugin-rules-engine),
[TinkerPop](https://github.com/rossonet/waldot/tree/master/plugins/waldot-plugin-tinkerpop).

### Active edges

Edges are created from the source vertex:
`source.addEdge('<label>', target, 'type', '<edge type>', ...)`.

| `type` | Observes | Effect |
| ------ | -------- | ------ |
| `link-to` | properties of the **source** | copies the value into the **target** |
| `link-from` | properties of the **target** | copies the value into the **source** (the source "pulls") |
| `fire` | properties and events of the **source** | sends the change to the **target** (a rule queues it) |
| `rules:execute` | (compute → rule) | the compute executes the rule with the given `priority` |

Properties of `link-to`, `link-from` and `fire`:

| Edge property | Default | Meaning |
| ------------- | ------- | ------- |
| `properties-to-monitor` | all | observed properties, separated by `,`; `*` = all |
| `<property>` = `<name>` | same name | rename: `'data', 'level'` writes the source `data` into the target `level` |
| `deadband-value` | none | minimum change to propagate: a number (`10`) or a numeric string (`'10'`) |
| `deadband-type` | `absolute` | `absolute` or `percentage` (of the last propagated value) |
| `delay` | 0 | the value is written after the given ms |
| `active` | `true` | `false` disables the edge |
| `active-property` | `true` | `false` stops property notifications |
| `active-event` | `true` | `false` stops event notifications (`fire`) |
| `priority` | 10 | priority of the notification (`fire`) |

---

## 4. Bootstrap file

The bootstrap file builds the graph at startup and after a reset. It is a
[JEXL](https://commons.apache.org/proper/commons-jexl/) script: statements end
with `;`, comments are `//`.

```javascript
// boot.conf
var gen = graph.addVertex('id', 'level-gen', 'type', 'sim:generator', 'label', 'level-gen',
    'directory', 'plant/sim', 'algorithm', 'sinusoidal', 'min', '0', 'max', '100', 'delay', '200');
var tank = graph.addVertex('id', 'tank-1', 'label', 'tank-1', 'directory', 'plant/tanks', 'level', 0.0);
gen.addEdge('level-signal', tank, 'type', 'link-to', 'properties-to-monitor', 'data', 'data', 'level',
    'deadband-type', 'absolute', 'deadband-value', 10);
log.info('plant ready: ' + cmd.getVerticesCount() + ' vertices');
```

Syntax, detection of the format, available variables and errors:
[Bootstrap Configuration](https://github.com/rossonet/waldot/blob/master/waldot-namespace/BOOTSTRAP_CONFIGURATION.md).

---

## 5. Creating elements at runtime

### From an OPC UA client: commands

| Command | Folder | Input arguments |
| ------- | ------ | --------------- |
| `query` | `manage` | JEXL expression, e.g. `graph.addVertex('id','v1','label','v1')` |
| `delete directory` | `manage` | folder NodeId (`v:<directory>`, also `ns=2;s=v:<directory>`): removes the vertices and edges of the folder and of its sub folders, then the folder |
| `create link monitored edge` | `manage/edges` | label, SourceNode, TargetNode, directory, properties-to-monitor, active-property, inverted, deadband-type, deadband-value, active-event |
| `create fire monitored edge` | `manage/edges` | label, SourceNode, TargetNode, directory, properties-to-monitor, active-property, deadband-type, deadband-value, active-event |
| `create compute monitored edge` | `rules/edges` | label, SourceNode (compute), TargetNode (rule), directory, priority |
| `create generator` | `simulator` | label, directory, algorithm, delay, min, max, post-node-id-data |
| `create event generator` | `simulator` | label, directory and the event generator properties |
| `create rule` | `rules` | label, directory, condition, action, debug, hysteresis |
| `create compute` | `rules` | label, directory, threads, execution-timeout-ms, factor |
| `create gremlin server` | `tinkerpop` | label, directory, port, bind |
| `create show case arena` / `evolution` / `gas grid` | `rules/showcase` | see [Examples](examples/README.md) |
| `about`, `help` | `information` | none |

Empty arguments take the default value. Creation commands return the NodeId
of the new vertex or edge, or a text starting with `error:` (for example
`error: target vertex 'ns=2;s=missing' not found`).

Edge commands:

- `SourceNode` and `TargetNode` are NodeId arguments (`ns=2;s=pump-1`); from
  the console the plain id (`'pump-1'`) is accepted too;
- the edge always goes from `SourceNode` to `TargetNode`. With `inverted` =
  `false` it is a `link-to` (the source writes into the target), with `true` a
  `link-from` (the source reads from the target). A `fire` edge observes the
  source and notifies the target;
- `deadband-value` is a Double (`0` or empty: every change is propagated).

Example with Python `asyncua`:

```python
import asyncio
from asyncua import Client, ua

async def main():
    async with Client(url="opc.tcp://127.0.0.1:12686/waldot") as c:
        manage = c.get_node("ns=2;s=c:manage")
        query = ua.NodeId.from_string("ns=2;s=query")
        result = await manage.call_method(query, ua.Variant(
            "graph.addVertex('id','from-opcua','label','from-opcua','directory','opcua')",
            ua.VariantType.String))
        print(result)            # ['v[ns=2;s=from-opcua]', '']
        about = await c.get_node("ns=2;s=c:information").call_method(
            ua.NodeId.from_string("ns=2;s=about"))
        print(about)             # name, description, license, ..., commit, version, branch
        # link-to edge pump-1 -> tank-1 on property 'v' (both vertices must exist)
        edges = c.get_node("ns=2;s=c:manage/edges")
        link = await edges.call_method(
            ua.NodeId.from_string("ns=2;s=create link monitored edge"),
            ua.Variant("pump-to-tank", ua.VariantType.String),
            ua.Variant(ua.NodeId("pump-1", 2), ua.VariantType.NodeId),
            ua.Variant(ua.NodeId("tank-1", 2), ua.VariantType.NodeId),
            ua.Variant("", ua.VariantType.String),          # directory
            ua.Variant("v", ua.VariantType.String),         # properties-to-monitor
            ua.Variant(True, ua.VariantType.Boolean),       # active-property
            ua.Variant(False, ua.VariantType.Boolean),      # inverted: false = link-to
            ua.Variant("", ua.VariantType.String),          # deadband-type
            ua.Variant(0.0, ua.VariantType.Double),         # deadband-value
            ua.Variant(True, ua.VariantType.Boolean))       # active-event
        print(link)              # ns=2;s=pump-1:pump-to-tank:tank-1

asyncio.run(main())
```

### From the bootstrap or the `query` command

All commands are available in JEXL as `<folder>_<name>` objects with an
`exec(...)` method:

```javascript
var r = simulator_create_generator.exec('cmd-gen', 'plant/sensors', 'incremental', '1000', '0', '10', '');
log.info('created ' + r[0]);
```

### From a Gremlin client

Create a `gremlin` vertex (or run `create gremlin server`): a TinkerPop Gremlin
Server exposes the same graph.

```javascript
tinkerpop_create_gremlin_server.exec('gremlin-1', 'services', '8182', '0.0.0.0');
```

```bash
docker run -d -p 12686:12686 -p 8182:8182 -v "$PWD/boot.conf:/waldot/boot.conf:ro" rossonet/waldot:latest
curl -X POST -d '{"gremlin":"g.V().count()"}' http://127.0.0.1:8182
```

The Gremlin Server has no authentication: publish its port only on trusted
networks.

---

## 6. Rules engine

```
[source vertex] --fire--> [rules:rule] <--rules:execute-- [rules:compute] --> virtual threads
```

1. A property of the source vertex changes; the `fire` edge notifies the rule.
2. The rule queues the notification (`queue`); notifications from the same
   source within `hysteresis` ms are discarded.
3. The compute takes the rules with queued events, highest weight first:
   `weight = priority × factor + queue`.
4. At most `threads` rules run at the same time; each execution evaluates
   `condition` and, when `true`, runs `action`.

Variables in `condition` and `action`: `g`, `graph`, `log`, `cmd`, `self`
(the rule vertex), `math`, `rand` and all commands. Each execution has its
own context: keep state in vertex properties (`self.property('n', 1)`).

```javascript
var compute = graph.addVertex('id', 'plant-compute', 'type', 'rules:compute', 'label', 'plant-compute',
    'directory', 'plant/rules', 'threads', '2');
var high = graph.addVertex('id', 'tank-high', 'type', 'rules:rule', 'label', 'tank-high', 'directory', 'plant/rules',
    'condition', "g.V().has('id','tank-1').values('level').next() > 80",
    'action', "g.V().has('id','ctrl-1').next().property('output', 0.0); log.warn('tank-1 high level, valve closed')",
    'hysteresis', '1000');
compute.addEdge('runner', high, 'type', 'rules:execute', 'priority', '100');
tank.addEdge('watch-level', high, 'type', 'fire', 'properties-to-monitor', 'level');
```

### Rule counters

| Variable | Meaning | What to check |
| -------- | ------- | ------------- |
| `queue` | notifications waiting | grows continuously → compute too slow (raise `threads`) or hysteresis too low |
| `total` | evaluations | does not grow → the `fire` edge or the `rules:execute` edge is missing |
| `executed` | actions completed | stays at 0 with `total` growing → condition always false |
| `errors` | compilation or runtime errors | grows → read the log, set `debug` to 2 |

`condition` and `action` can be changed at runtime from a client: the script is
compiled again. With `debug` = 1 every phase is published as an OPC UA event,
with `debug` = 2 it is also written in the log.

---

## 7. Monitoring

| What | Where |
| ---- | ----- |
| values of the plant | variables of the vertices, OPC UA subscription |
| simulated alarms | events of `sim:event-generator` vertices (subscribe to events of the vertex or of the server) |
| rules health | `queue`, `total`, `executed`, `errors` of each rule; `queue` of each compute |
| software version | `about` command (name, license, git commit, version, branch) |
| server log | `docker logs waldot`; categories `BOOT`, `CONSOLE`, `WaldOT runner` |

Useful queries (`query` command):

```javascript
g.V().count().next()
g.V().has('type', 'rules:rule').values('label').toList()
g.V().has('id', 'tank-high').values('errors').next()
g.V().has('id', 'tank-high').inE().toList()
cmd.listCommands()
```

---

## 8. Operations

| Task | How |
| ---- | --- |
| change the graph at startup | edit `boot.conf`, restart the container |
| rebuild the graph | restart the container |
| remove a group of vertices | `delete directory` on their folder, or `query` with a loop, see below |
| run the bootstrap again | `cmd.reset()`: every vertex and edge is removed (generators, rules and monitors stop), the plugins are reset and the bootstrap runs again |
| keep the OPC UA certificate | mount a volume on `/app/.security` |
| restrict access | `--anonymous-access false` (only username/password or certificate sessions), `--factory-password ...` |
| disable a command | `--<cmd>-command-executable false` ([Configuration](CONFIGURATION.md#built-in-opc-ua-commands)) |

Remove everything created in a directory (and its sub folders), for example
the Arena showcase, from the console:

```javascript
graph.getWaldotNamespace().deleteDirectory('v:rules/showcase/arena')
// returns 'ns=2;s=v:rules/showcase/arena'; the showcase can be created again
```

or remove all the vertices whose id starts with `arena-`:

```javascript
var n = 0; for (var v : g.V().toList()) { if (v.id().getIdentifier().toString().startsWith('arena-')) { v.remove(); n = n + 1; } } n;
```

The graph lives in memory: changes made at runtime are lost at restart unless
they are also in the bootstrap file.

---

## 9. Troubleshooting

| Symptom | Cause | Fix |
| ------- | ----- | --- |
| `ambiguous statement error` at boot | missing `;` or Groovy syntax in the bootstrap | see [Bootstrap Configuration](https://github.com/rossonet/waldot/blob/master/waldot-namespace/BOOTSTRAP_CONFIGURATION.md#troubleshooting) |
| the target of a `link-to` does not change | wrong `properties-to-monitor`, or deadband too large | check the property names and `deadband-value` |
| edge command returns `error: ... vertex ... not found` | wrong NodeId of `SourceNode`/`TargetNode` | pass `ns=2;s=<id>` of existing vertices |
| anonymous client rejected with `BadIdentityTokenInvalid` | server started with `--anonymous-access false` | connect with username/password or a certificate |
| rule `total` stays at 0 | missing `fire` or `rules:execute` edge | check edges with `g.V().has('id','<rule>').inE().toList()` |
| rule `errors` grows | JEXL error in condition/action | set `debug` to 2 and read the log |
| `Vertex with id already exists` | duplicated id | use unique ids |
| `already exists` from a showcase command | the scenario is already in the graph | `delete directory` on `v:rules/showcase/<arena\|evolution\|gas-grid>` (see [Operations](#8-operations)), `cmd.reset()` or restart |
| client cannot connect | port not published, or wrong host in the endpoint | publish `-p 12686:12686`, set `--bind-hostname` |

---

## Related documentation

- [Examples](examples/README.md)
- [Configuration Reference](CONFIGURATION.md)
- [Bootstrap Configuration](https://github.com/rossonet/waldot/blob/master/waldot-namespace/BOOTSTRAP_CONFIGURATION.md)
- [Architecture Overview](ARCHITECTURE_OVERVIEW.md)
- [Integration Guide](INTEGRATION_GUIDE.md) - writing plugins
