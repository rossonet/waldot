# 2. Active edges

Active edges copy values between vertices when a property changes. They need
the vertices of [page 1](01-plain-vertices.md).

| Edge | Observes | Writes |
| ---- | -------- | ------ |
| `link-to` | source | target |
| `link-from` | target | source |

## link-to with rename and deadband

```javascript
// 02 - generator -> tank level, only changes larger than 10
var gen = graph.addVertex('id', 'level-gen', 'type', 'sim:generator', 'label', 'level-gen', 'directory', 'plant/sim',
    'algorithm', 'sinusoidal', 'min', '0', 'max', '100', 'delay', '200');
gen.addEdge('level-signal', tank, 'type', 'link-to', 'properties-to-monitor', 'data', 'data', 'level',
    'deadband-type', 'absolute', 'deadband-value', 10);
```

- `properties-to-monitor` = `data`: only `data` of the generator is observed.
- `'data', 'level'`: the value is written in `level` of the tank (rename).
- `deadband-value` = 10: a value is propagated only when it differs by more
  than 10 from the last value of `level`. Numbers and numeric strings
  (`'10'`) are both accepted.

Observed values (generator every 200 ms):

| `level-gen.data` | `tank-1.level` |
| ---------------- | -------------- |
| 27.80 | 27.80 |
| 62.69 | 62.69 |
| 96.00 | 91.34 (change smaller than 10, not propagated) |

With `deadband-type` = `percentage` the threshold is a percentage of the last
value.

## link-to with delay

```javascript
var mirror = graph.addVertex('id', 'tank-1-hmi', 'label', 'tank-1-hmi', 'directory', 'plant/hmi', 'level', 0.0);
tank.addEdge('to-hmi', mirror, 'type', 'link-to', 'properties-to-monitor', 'level', 'delay', 2000);
```

`tank-1-hmi.level` follows `tank-1.level` 2 seconds later: use it to model
transport delays (pipes, networks, slow transmitters).

## link-from

```javascript
// the valve pulls the command of the controller
var ctrl = graph.addVertex('id', 'ctrl-1', 'label', 'ctrl-1', 'directory', 'plant/ctrl', 'output', 0.0);
var valve = graph.addVertex('id', 'valve-1', 'label', 'valve-1', 'directory', 'plant/valves', 'opening', 0.0);
valve.addEdge('command', ctrl, 'type', 'link-from', 'properties-to-monitor', 'output', 'output', 'opening');
ctrl.property('output', 42.0);
log.info('02 valve opening after link-from: ' + g.V().has('id', 'valve-1').values('opening').next());
```

```
INFO CONSOLE - 02 valve opening after link-from: 42.0
```

The edge goes from the valve to the controller, but the data flows from the
controller (`output`) to the valve (`opening`). Use `link-from` when the
consumer owns the relation (an actuator reading its setpoint).

## Edge properties

| Property | Default | Meaning |
| -------- | ------- | ------- |
| `properties-to-monitor` | all | observed properties, separated by `,`; `*` = all |
| `<source property>` | same name | name of the destination property |
| `deadband-value` | none | minimum change (number or numeric string) |
| `deadband-type` | `absolute` | `absolute` or `percentage` |
| `delay` | 0 | ms before writing |
| `active` | `true` | `false` disables the edge |
| `active-property` | `true` | `false` stops property notifications |

All of them can be changed at runtime, for example from the `query` command:
`g.E().hasLabel('to-hmi').next().property('delay', 500)`.

## Creating the edges with the commands

The same edges can be created at runtime with the OPC UA methods in
`Commands/manage/edges` (or from the console with
`create_link_monitored_edge.exec(...)`). The edge always goes from
`SourceNode` to `TargetNode`; `inverted` chooses the direction of the data:

```javascript
// link-to: tank-1.level -> tank-1-hmi.level
create_link_monitored_edge.exec('to-hmi-2', 'ns=2;s=tank-1', 'ns=2;s=tank-1-hmi',
    'plant/edges', 'level', 'true', 'false', 'absolute', '0.5', 'true');
// link-from: valve-1 reads ctrl-1.output
create_link_monitored_edge.exec('command-2', 'valve-1', 'ctrl-1',
    '', 'output', 'true', 'true', '', '', 'true');
```

Arguments: label, SourceNode, TargetNode, directory, properties-to-monitor,
active-property, inverted, deadband-type, deadband-value, active-event. The
result is the NodeId of the edge (`ns=2;s=tank-1:to-hmi-2:tank-1-hmi`) or a
text starting with `error:` when a vertex does not exist. Renaming
(`'output', 'opening'`) and `delay` are not command arguments: set them
afterwards as edge properties.

Next: [3. Generators and rules](03-generators-and-rules.md).
