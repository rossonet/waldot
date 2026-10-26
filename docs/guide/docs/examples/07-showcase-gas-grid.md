# 7. Showcase: Gas Grid

A gas distribution network controlled by **PI loop controllers** written as
rules. It is designed for demos to gas utilities: plants, transmitters, valves
and controllers are vertices; measurements travel on `link-to`/`link-from`
edges with deadband and delay; control and safety logic are rules.

> The model is didactic, **not a hydraulic simulation**: the process is a
> first-order system and all values (pressures, demands, thresholds, delays)
> are indicative.

## Create

From an OPC UA client: `Commands/rules/showcase/create show case gas grid`.
From a bootstrap or the `query` command:

```javascript
log.info(create_show_case_gas_grid.exec('', '', '', '', '', '')[0]);
```

```
show case gas grid created: 4 stations, gas metano, 58 vertices
```

| Argument | Default | Range | Meaning |
| -------- | ------- | ----- | ------- |
| `stations` | 4 | 2-20 | pressure reduction stations |
| `delay-ms` | 200 | ≥ 10 | simulation step |
| `setpoint` | 25 | 5-100 | downstream pressure (mbar) |
| `pipe-delay-ms` | 300 | 0-10000 | dead time of the pipe |
| `gas-type` | `metano` | `metano`, `metano-blu` | gas profile |
| `compare` | `true` | `true`, `false` | the last station has no deadband, for comparison |

Invalid arguments create nothing (`argument 'stations' must be between 2 and 20`).
Vertices: `14 + 11 × stations`.

### Gas profiles (indicative values)

| Parameter | `metano` | `metano-blu` (20 % hydrogen) |
| --------- | -------- | ---------------------------- |
| relative density | 0.60 | 0.48 |
| flow factor at the same opening | 1.00 | 1.12 |
| nominal demand | 50 | 57.5 |
| ESD threshold over the setpoint | +30 % | +25 % |
| transmitter deadband | 2.0 mbar | 1.2 mbar |
| demand deadband | 5 % | 3 % |
| upstream pressure deadband | 0.3 bar | 0.2 bar |
| dead time | `pipe-delay-ms` × 1.0 | `pipe-delay-ms` × 1.2 |

The profile is stored on the `gas-network` vertex.

## Graph (folder `rules/showcase/gas-grid`)

```
supply/         gas-gate-gen (upstream pressure), gas-clock (simulation step), gas-scan (scan cycle)
transmission/   gas-trunk (high pressure line)
stations/
  station-i/    gas-sensor-i, gas-valve-i, gas-ctrl-i, gas-plant-i
consumers/      gas-consumer-i + gas-demand-gen-i (demand profile)
safety/         gas-leak-monitor
supervisory/    gas-network (profile, network setpoint, load), gas-stats
rules/
  control/      gas-rule-pi-i, gas-rule-mode-guard-i
  safety/       gas-rule-esd-trip-i, gas-rule-esd-reset-i, gas-rule-leak-detect
  process/      gas-rule-plant-i
  supervisory/  gas-rule-balancer, gas-rule-stats, gas-rule-bad-sensor
compute/        gas-control-loop, gas-process, gas-supervisory
links/          link-to and link-from edges
triggers/       fire edges
```

| Vertex | Main properties | Role |
| ------ | --------------- | ---- |
| `gas-plant-i` | `pressure`, `upstream` | simulated process: the real downstream pressure |
| `gas-sensor-i` | `pressure` | transmitter: sees the pressure with dead time and deadband |
| `gas-ctrl-i` | `setpoint`, `mode`, `fault`, `integral`, `output`, `bias`, `kp`, `ki`, `esd-trips` | loop controller state |
| `gas-valve-i` | `opening` (0-100 %) | control valve |
| `gas-consumer-i` | `demand` | demand of the station |

### Data flow of a station

```
gas-gate-gen --link-to--> gas-trunk --link-to--> gas-plant-i.upstream
gas-demand-gen-i --link-to--> gas-consumer-i.demand
                     rule "plant" (valve opening, upstream, demand) -> gas-plant-i.pressure
gas-plant-i.pressure --link-to (delay + deadband)--> gas-sensor-i.pressure --fire--> PI / guard / ESD rules
gas-network.setpoint --link-to--> gas-ctrl-i.setpoint
gas-ctrl-i.output --link-from (deadband 0.5 %)--> gas-valve-i.opening --> rule "plant"
```

The loop is closed: the valve changes the pressure, the transmitter sees it
later, the controller corrects the valve.

## Rules

| Rule | Fired by | Condition | Action |
| ---- | -------- | --------- | ------ |
| `gas-rule-pi-i` | `sensor.pressure` and scan cycle | mode `AUTO` | error, integral with anti-windup, `output` 0-100 % |
| `gas-rule-mode-guard-i` | `sensor.pressure` | fault state changes | measure < 0 or > 200: `fault` = `sensor`, mode `MANUAL`; back to `AUTO` with a valid measure |
| `gas-rule-esd-trip-i` | `sensor.pressure` | pressure > setpoint × (1 + ESD margin) | mode `ESD`, output 0, `esd-trips` +1 |
| `gas-rule-esd-reset-i` | `sensor.pressure` | mode `ESD` and pressure < 50 % of the setpoint | back to `AUTO` |
| `gas-rule-plant-i` | clock | always | process model |
| `gas-rule-balancer` | clock | always | network setpoint from the mean demand (hysteresis 1 s) |
| `gas-rule-stats` | clock | always | mean error, mean opening, total ESD trips (hysteresis 1 s) |
| `gas-rule-leak-detect` | mean opening | mean opening > 90 % changes the alarm state | `alarm` = `leak` / `none` |
| `gas-rule-bad-sensor` | clock | reads a missing transmitter | fails on purpose: `errors` grows |

PI controller: `error = setpoint - pressure`,
`output = clamp(bias + kp·error + ki·integral, 0, 100)` with `kp` = 0.4,
`ki` = 0.1; the integral is not updated when the output is saturated.

Process model, at every clock step:
`target = 0.9 · opening · (upstream / 24) · flow_factor - 0.25 · (demand - nominal) - 0.25 · leak / stations`,
`pressure = pressure + 0.3 · (target - pressure)`.

| Compute | Threads | Rules (priority) | Why |
| ------- | ------- | ---------------- | --- |
| `gas-control-loop` | 1 | ESD trip (300), guard (250), ESD reset (200), PI (100) | controllers must not overlap, safety first |
| `gas-process` | 4 | plant models (100) | independent, in parallel |
| `gas-supervisory` | 1 | leak (60), balancer (50), stats (40), bad-sensor (10) | slow logic, low priority |

Safety rules have no hysteresis: a discarded notification could hide an
overpressure.

## What to observe

Values after about one minute with the defaults:

| Station | `sensor.pressure` | `ctrl.setpoint` | `ctrl.mode` | `ctrl.output` | `valve.opening` | PI `total` |
| ------- | ----------------- | --------------- | ----------- | ------------- | --------------- | ---------- |
| 0 (deadband) | 23.76 | 25.2 | AUTO | 29.1 | 28.95 | 138 |
| 3 (no deadband) | 24.51 | 25.2 | AUTO | 30.19 | 30.36 | 398 |

- The pressure follows the setpoint; `valve.opening` follows `ctrl.output`
  through the `link-from` edge.
- Station 3 has no deadband: its PI rule is evaluated about three times more
  often for the same regulation.
- `gas-stats`: `mean-abs-error` 2.75, `mean-opening` 29.44, `esd-trips` 0.
- `gas-rule-bad-sensor`: `errors` = `total`, `executed` = 0 (JEXL exceptions
  in the log are expected for this rule).

## Live interventions

Verified on the default scenario:

| Intervention | Result |
| ------------ | ------ |
| write 90 on `gas-sensor-1.pressure` | immediately `gas-ctrl-1.mode` = `ESD`, `output` = 0, `esd-trips` = 1; the other stations stay `AUTO`; after a few seconds the pressure falls and the controller is back in `AUTO` |
| write -5 on `gas-sensor-1.pressure` | `fault` = `sensor`, mode `MANUAL` until the next valid measure from the plant (a few hundred ms) |
| write 600 on `gas-leak-monitor.leak` | the valves open to compensate; when `gas-stats.mean-opening` exceeds 90 % (about 40 s) `gas-leak-monitor.alarm` = `leak`; write 0 to cancel |
| write 40 on `gas-network.base-setpoint` | the balancer distributes the new setpoint (41.2 with the current demand) to all controllers; the pressures follow (43.8 after 25 s) |
| change `kp`, `ki` of `gas-ctrl-i` | faster or more oscillating response |
| change `deadband-value` or `delay` of the edge `pipe-to-transmitter-i` | more or fewer PI executions, more or less oscillation |
| write `MANUAL` on `gas-ctrl-i.mode` | the PI rule stops acting |
| `gas-demand-gen-i.algorithm` = `stopped` | the demand stays constant |

## Rules engine concepts in the demo

| Concept | Where |
| ------- | ----- |
| `condition` / `action` | PI, guard, ESD, leak |
| `fire` edge | `triggers/`: the measure fires the controllers |
| `rules:compute` with `threads` | 1 thread for the loops, 4 for the process |
| `rules:execute` with `priority` | ESD (300) before PI (100) |
| hysteresis | balancer and statistics (1 s) |
| counters | `queue`, `total`, `executed`, `errors` on every rule |
| runtime changes | `setpoint`, `kp`, `ki`, `deadband-value`, `delay` |

## Limits

- No pressure losses, line pack, temperature or real gas composition.
- Upstream pressure is fixed around 24 bar with a slow oscillation.
- The PI is discrete with a variable step (executions depend on events) and
  is not tuned for a real plant.
- `metano-blu` changes only some coefficients (flow, demand, thresholds,
  delay).

To create the scenario again run `delete directory` on
`v:rules/showcase/gas-grid` (every vertex and edge of the folder tree is
removed), use `cmd.reset()` or restart the server (see
[Operations](../USER_GUIDE.md#8-operations)). Tests: `WaldotRulesEngineGasGridTest`.
