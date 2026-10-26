# 1. Plain vertices

A plain (passive) vertex has no `type`: it only stores properties, which are
published as OPC UA variables and can be read and written by any client.

## Script

```javascript
// 01 - plain vertices
var pump = graph.addVertex('id', 'pump-1', 'label', 'pump-1', 'directory', 'plant/pumps',
    'manufacturer', 'ACME', 'power-kw', 7.5, 'running', true);
var tank = graph.addVertex('id', 'tank-1', 'label', 'tank-1', 'directory', 'plant/tanks', 'level', 0.0);

// passive edge: a relation for traversals, no data flow
pump.addEdge('fills', tank);

log.info('01 out of pump: ' + g.V().has('id', 'pump-1').out('fills').values('label').next());
```

Log:

```
INFO CONSOLE - 01 out of pump: tank-1
```

## Result in OPC UA

```
Gremlin Engine/Vertices
└── plant
    ├── pumps
    │   └── pump-1          ns=2;s=pump-1
    │       ├── manufacturer   "ACME"   (String)
    │       ├── power-kw       7.5      (Double)
    │       └── running        true     (Boolean)
    └── tanks
        └── tank-1          ns=2;s=tank-1
            └── level          0.0      (Double)
```

- `id` becomes the NodeId, `label` the browse name.
- `directory` creates the folders `plant/pumps` and `plant/tanks`.
- The value keeps the JEXL type: `'7.5'` is a String, `7.5` a Double, `true`
  a Boolean. The variables are declared as `BaseDataType`, so a client can
  write any type.

## Try

| Action | Result |
| ------ | ------ |
| write `running` = `false` from the client | the graph property changes: `g.V().has('id','pump-1').values('running').next()` returns `false` |
| `query`: `g.V().has('id','tank-1').next().property('level', 12.5)` | the OPC UA variable `level` shows 12.5 |
| `query`: `g.V().has('id','tank-1').in('fills').values('label').toList()` | `[pump-1]` |

Next: [2. Active edges](02-active-edges.md).
