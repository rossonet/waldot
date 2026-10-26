# 5. Showcase: Dungeon Arena

A small tower-defense game made only of generators, edges and rules. It shows
priorities, two computes with a different number of threads, hysteresis,
debug and a rule that always fails.

## Create

From an OPC UA client: `Commands/rules/showcase/create show case arena`.
From a bootstrap or the `query` command:

```javascript
log.info(create_show_case_arena.exec('')[0]);
```

```
show case arena created: 3 generators, 4 state vertices, 2 compute, 7 rules
```

| Argument | Default | Meaning |
| -------- | ------- | ------- |
| `delay-ms` | 250 (minimum 10) | period of the generators; the turn clock uses twice this value |

A second call answers
`show case arena already exists: remove its vertices before creating it again`.
Remove the scenario with `delete directory` on `v:rules/showcase/arena` (or
`graph.getWaldotNamespace().deleteDirectory('v:rules/showcase/arena')` from
the console): its 16 vertices and 17 edges are removed and the command can be
called again.

## Graph (16 vertices, folder `rules/showcase/arena`)

| Folder | Vertices |
| ------ | -------- |
| `state` | `arena-tower` (`data` = mana, `hp`, `kills`, `spells`), `arena-wave` (`data` = enemies, `difficulty`), `arena-player` (`level`, `gold`, `score`, `defeats`), `arena-turn` (`data`) |
| `generators` | `arena-gen-wave` (random 0-100), `arena-gen-mana` (sinusoidal 0-100), `arena-gen-turn` (incremental); `link-to` edges into the state vertices |
| `compute` | `arena-combat` (4 threads), `arena-economy` (1 thread) |
| `rules` | 7 rules, below |
| `triggers` | `fire` edges |

| Rule | Fired by | Condition | Action | Compute (priority) |
| ---- | -------- | --------- | ------ | ------------------ |
| `arena-rule-fire-spell` | `tower.data` | mana > 80 and enemies > 20 | `kills` +1, `spells` +1; hysteresis 300 ms | combat (200) |
| `arena-rule-tower-damage` | `wave.data` | enemies > 50 + 5 × level | `hp` decreases | combat (150) |
| `arena-rule-spawn-wave` | `wave.data` | enemies > 70 and difficulty < 10 | `difficulty` +1 | combat (100) |
| `arena-rule-broken` | `turn.data` | `1 / 0 > 0` (always fails) | never executed | combat (10) |
| `arena-rule-game-over` | `tower.hp` | hp ≤ 0 | `defeats` +1, the game restarts | economy (200) |
| `arena-rule-level-up` | `tower.kills` | kills multiple of 5 | `level` +1, `gold` += 10 × level; `debug` = 1 | economy (100) |
| `arena-rule-turn-tick` | `turn.data` | turn multiple of 10 | `score` += 10 × level | economy (50) |

## What to observe

Values after about one minute:

| Vertex | Values |
| ------ | ------ |
| `arena-tower` | hp 72, kills 3, spells 31 |
| `arena-wave` | difficulty 7 |
| `arena-player` | level 1, gold 110, score 170, defeats 4 |

| Rule | `total` | `executed` | `errors` |
| ---- | ------- | ---------- | -------- |
| `arena-rule-spawn-wave` | 255 | 42 | 0 |
| `arena-rule-tower-damage` | 255 | 100 | 0 |
| `arena-rule-fire-spell` | 128 | 31 | 0 |
| `arena-rule-game-over` | 104 | 4 | 0 |
| `arena-rule-turn-tick` | 127 | 13 | 0 |
| `arena-rule-level-up` | 35 | 5 | 0 |
| `arena-rule-broken` | 127 | 0 | 127 |

- `total` > `executed`: most notifications have a false condition.
- `fire-spell` has half the evaluations of `spawn-wave` although both follow a
  generator with the same period: the 300 ms hysteresis discards notifications.
- `arena-rule-broken` shows how a failing rule looks: `errors` = `total`,
  `executed` = 0, a JEXL exception in the log.
- `arena-rule-level-up` (`debug` = 1) publishes an OPC UA event for each phase:
  subscribe to the events of the vertex.

## Try

| Action | Effect |
| ------ | ------ |
| `arena-gen-wave.algorithm` = `stopped` | no new waves, the tower recovers |
| `arena-economy.threads` = 4 | economy rules run in parallel |
| `arena-rule-broken.condition` = `false` | `errors` stops growing |
| `arena-rule-fire-spell.hysteresis` = 0 | more spells, `total` grows faster |
| priority of the edge `runs-broken` = 300 | the broken rule is served first in `arena-combat` |

Next: [6. Showcase: Evolution Lab](06-showcase-evolution.md).
