# 6. Showcase: Evolution Lab

An evolutionary algorithm written as rules. Each individual of the population
is a vertex; one rule per individual mutates it in parallel; a second compute
with one thread runs selection, crossover and statistics. The goal is to
maximise `100 - Rastrigin(x, y, z)` (maximum 100 at 0, 0, 0).

## Create

From an OPC UA client: `Commands/rules/showcase/create show case evolution`.
From a bootstrap or the `query` command:

```javascript
log.info(create_show_case_evolution.exec('', '', '', '')[0]);
```

```
show case evolution created: 40 individuals, 42 generators, 2 compute, 46 rules, 4 control vertices
```

| Argument | Default | Range | Meaning |
| -------- | ------- | ----- | ------- |
| `population-size` | 40 | 30-1000 | individuals (vertices = 3 × N + 14) |
| `delay-ms` | 200 | ≥ 10 | period of the generators |
| `target-fitness` | 99 | | fitness that stops the evolution |
| `seed` | 42 | | seed of the initial population |

## Graph (folder `rules/showcase/evolution`)

| Folder | Content |
| ------ | ------- |
| `population` | `evo-ind-0` ... `evo-ind-N`: genes `x`, `y`, `z`, `fitness`, `data` (noise) |
| `generators` | one `random` generator per individual (mutation noise, `link-to` into `data`), `evo-gen-clock` (generations), `evo-gen-rate` (mutation rate 5-50) |
| `rules/evaluate` | `evo-rule-evaluate-i`: when the noise is below the mutation rate, mutates the genes and keeps the mutation only if the fitness improves |
| `rules/breeding` | `evo-rule-select-best`, `evo-rule-crossover`, `evo-rule-stagnation`, `evo-rule-converged`, `evo-rule-stats` (`debug` = 1), `evo-rule-bad-input` (fails on purpose) |
| `compute` | `evo-eval-pool` (8 threads, evaluation rules), `evo-breeding` (1 thread, breeding rules) |
| `control` | `evo-hall-of-fame`, `evo-generation`, `evo-mutation-rate`, `evo-stats` |

Breeding priorities: converged 190, crossover 150, stagnation 100, stats 80,
bad-input 60, select-best 50. `select-best` is the most frequent rule, so it
has a low priority: with one thread the others must not starve.

## What to observe

Values after about one minute (40 individuals):

| Vertex | Values |
| ------ | ------ |
| `evo-hall-of-fame` | fitness 97.8 at x 1.0, y 0.96, z 0.01; improvements 11; stagnations 4; status `running` |
| `evo-stats` | mean 91.46, best 97.8, worst 83.13 |
| `evo-mutation-rate` | data 21.6, boost 30 |

| Rule | `queue` | `total` | `executed` | `errors` |
| ---- | ------- | ------- | ---------- | -------- |
| `evo-rule-select-best` | 1 | 281 | 281 | 0 |
| `evo-rule-crossover` | 0 | 64 | 22 | 0 |
| `evo-rule-stagnation` | 0 | 64 | 5 | 0 |
| `evo-rule-stats` | 0 | 64 | 64 | 0 |
| `evo-rule-converged` | 0 | 11 | 0 | 0 |
| `evo-rule-bad-input` | 0 | 64 | 0 | 64 |
| `evo-rule-evaluate-0` | 0 | 320 | 122 | 0 |

- `evo-hall-of-fame.fitness` grows over time; `status` becomes `converged`
  when it reaches `target-fitness` and the generators stop.
- `stagnations` and `evo-mutation-rate.boost` grow when the best does not
  improve: the mutation rate increases to escape local maxima (here the
  algorithm is near the local maximum at x ≈ 1, y ≈ 1).
- `queue` of `evo-rule-select-best` is often 1: the single thread of
  `evo-breeding` is busy.

## Try

| Action | Effect |
| ------ | ------ |
| `evo-eval-pool.threads` = 1 | evaluations are serialized, `queue` of the evaluate rules grows |
| `evo-breeding.threads` = 4 | breeding rules run in parallel |
| `evo-gen-rate.algorithm` = `stopped` | the mutation rate is fixed |
| `evo-rule-converged.condition` = `true` | at its next notification the rule sets `status` = `converged` and stops all generators |

Next: [7. Showcase: Gas Grid](07-showcase-gas-grid.md).
