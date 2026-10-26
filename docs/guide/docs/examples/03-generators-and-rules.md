# 3. Generators and rules

This page adds active vertices to the plant of pages
[1](01-plain-vertices.md) and [2](02-active-edges.md): a rule that closes the
valve when the tank is too full, and a simulated alarm source.

## Rule

```javascript
// 03 - compute + rule + edges
var compute = graph.addVertex('id', 'plant-compute', 'type', 'rules:compute', 'label', 'plant-compute',
    'directory', 'plant/rules', 'threads', '2');
var high = graph.addVertex('id', 'tank-high', 'type', 'rules:rule', 'label', 'tank-high', 'directory', 'plant/rules',
    'condition', "g.V().has('id','tank-1').values('level').next() > 80",
    'action', "g.V().has('id','ctrl-1').next().property('output', 0.0); log.warn('tank-1 high level, valve closed')",
    'hysteresis', '1000');

// the compute executes the rule
compute.addEdge('runner', high, 'type', 'rules:execute', 'priority', '100');
// every change of tank-1.level notifies the rule
tank.addEdge('watch-level', high, 'type', 'fire', 'properties-to-monitor', 'level');
```

```
level-gen --link-to--> tank-1 --fire--> tank-high <--rules:execute-- plant-compute
                                            | action
                                            v
                                     ctrl-1.output = 0 --link-from--> valve-1.opening = 0
```

Log when the level exceeds 80 (the `log` of a rule writes with the rules
engine logger, in the thread of the rule):

```
[ns=2;s=tank-high] WARN net.rossonet.waldot.rules.WaldotRulesEnginePlugin - tank-1 high level, valve closed
```

Counters of `tank-high` after about 20 seconds:

| `queue` | `total` | `executed` | `errors` |
| ------- | ------- | ---------- | -------- |
| 0 | 50 | 7 | 0 |

- `total` counts the evaluations (one per notification not discarded by the
  1 s hysteresis);
- `executed` counts the evaluations with a true condition;
- `errors` stays at 0.

### Try

| Action | Result |
| ------ | ------ |
| write `condition` = `false` on `tank-high` | `executed` stops growing; the script is compiled again |
| write `debug` = 2 | every phase of the rule is published as an OPC UA event and logged |
| write `threads` = 4 on `plant-compute` | up to 4 rules of the compute run at the same time |
| write an invalid condition (`g.V(`) | `errors` grows |

## Event generator

```javascript
var ev = graph.addVertex('id', 'alarm-sim', 'type', 'sim:event-generator', 'label', 'alarm-sim', 'directory', 'plant/sim',
    'messages', 'pump overload|valve stuck|low pressure', 'delay', '3000', 'event-type', 'device-failure');
```

Every 3 seconds `alarm-sim` publishes an OPC UA event of type
`DeviceFailureEventType` with the next message. Subscribe to the events of the
vertex with the client. The variables show the last event:

| Variable | Example |
| -------- | ------- |
| `data` | `low pressure` |
| `count` | `18` |
| `severity` | `351` |

Events can be sent to other vertices with a `fire` edge, for example to a rule:

```javascript
ev.addEdge('alarms', someRule, 'type', 'fire');
```

## Generator algorithms

| `algorithm` | Values |
| ----------- | ------ |
| `incremental` / `decremental` | from `min` to `max` (or back) |
| `random` | random between `min` and `max` |
| `sinusoidal` / `triangular` | wave between `min` and `max` |
| `stopped` | the last value stays |

`algorithm`, `min`, `max` and `delay` can be changed at runtime.

Next: [4. Complete bootstrap](04-bootstrap.md).
