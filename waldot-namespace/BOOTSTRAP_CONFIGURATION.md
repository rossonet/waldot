# WaldOT Bootstrap Configuration

At startup WaldOT reads a **bootstrap file** and executes it to build the
initial graph: vertices, edges, generators, rules, folders. The file is loaded
by `SingleFileBootstrapStrategy` from the URL given with `--boot-url`
(default `file:///waldot/boot.conf`) when the server starts and after
`cmd.reset()`. The reset first removes every vertex and edge of the graph
(generators, rules and monitored edges stop), resets the plugins and then runs
the file again, so the graph is rebuilt from scratch. A `cmd.reset()` called
by the bootstrap file itself is ignored.

Embedded graphs created with `OpcFactory.getOpcGraph()` do **not** use that
default: they load `file:///tmp/boot.conf`, or the file named by
`-Dwaldot.boot.file`.

The file is executed by the WaldOT console engine, which is based on
**Apache Commons JEXL 3** (not Groovy). The same engine runs the `query`
OPC UA command and the console, so everything that works in a bootstrap file
also works there. Rule `condition` / `action` scripts use the same engine.

The syntax below matches `BootstrapScriptModeTests` and
`SingleFileBootstrapStrategyDetectTest`. Log excerpts are typical, not a
byte-for-byte transcript of one run (NodeIds in particular are not stable).
---

## Formats

The format is detected automatically from the content.

### Line mode

- one expression per line, each executed independently;
- lines starting with `#` and empty lines are ignored;
- an error on a line is logged and the following lines are still executed;
- variables cannot be used (an assignment switches to script mode).

```
# line mode: one expression per line
graph.addVertex('id', 'l1', 'label', 'l1')
graph.addVertex('id', 'l2', 'label', 'l2')
log.info('line mode ok: ' + cmd.getVerticesCount())
```

Log:

```
INFO BOOT - Bootstrap configuration loaded in LINE-BY-LINE MODE from URL: file:///waldot/boot.conf
INFO CONSOLE - line mode ok: 2
INFO BOOT - Bootstrap procedure completed successfully.
```

An error on a line is logged and the following lines are still executed.
The agent status becomes `FAULT` and the success line above is replaced by
`Bootstrap procedure completed with N error(s).`

### Script mode

The whole file is executed as **one JEXL script**:

- statements **must end with `;`** (a statement that spans several lines
  without `;` produces `ambiguous statement error`);
- comments: `//`, `/* ... */` and `##`. A line starting with a single `#` is
  **not** valid in script mode (`tokenization error in '#'`);
- variables (`var x = ...;`), functions, loops, conditions, `try/catch`;
- an error stops the whole script: the vertices already created remain, the
  following statements are not executed (use `try/catch` around optional
  parts). The agent status becomes `FAULT` and WaldOT logs
  `Bootstrap procedure completed with 1 error(s).` instead of the success line.

### Automatic detection

Script mode is selected when the content, ignoring lines whose first
non-space character is `#` and ignoring text inside `'...'`, `"..."` and
backtick templates, contains at least one of:

1. `//` or `/*` (a URL such as `https://...` inside a string does **not** count);
2. `def name(` (Groovy; see the warning below);
3. an assignment at the beginning of a line (`name = ...`, `var name = ...`;
   `==` does not count);
4. a method chain continued on an indented line (`.foo(...)` followed by a
   line starting with `.bar(`);
5. `if (`, `for (`, `while (`, `times {`;
6. a block `{ ... }` with at least 20 characters.

Otherwise line mode is used. To force script mode start the file with a `//`
comment; to force line mode use only `#` comments and one expression per line,
and do not put `//`, assignments or control structures outside strings.

> **Warning**: detection rule 2 recognizes `def`, but `def` is Groovy syntax
> and is **rejected by JEXL** (`ambiguous statement error`). Define functions
> as shown below.

> **Warning (Gremlin terminal steps)**: JEXL does not resolve the terminal
> methods of a Gremlin traversal (`next()`, `iterate()`, `toList()`,
> `hasNext()`, ...): they are `default` methods inherited through multiple
> interfaces and the JEXL uberspect does not find them
> (`JEXL error : next`). Prefer the `WaldotGraph` API
> (`graph.addVertex(...)`, `graph.vertices(...)`) or iterate the traversal
> with `for (var v : g.V()) { ... }`, which works. Do not call `.next()`
> on a traversal, including inside a rule `condition`.
---

## Script context

| Name | Object | Use |
| ---- | ------ | --- |
| `graph` | `WaldotGraph` | `graph.addVertex('key', 'value', ...)`, vertex/edge lookup |
| `g` | Gremlin traversal source | `g.V().has('label', 'x')` as the source of `for (var v : ...)`, `g.addV(...)` |
| `log` | console logger | `log.info(...)`, `log.warn(...)`, `log.error(...)` |
| `cmd` | console helper | `cmd.getVerticesCount()`, `cmd.getEdgesCount()`, `cmd.listCommands()`, `cmd.toNumber(x)`, `cmd.echo(x)`, `cmd.help()` |
| `<directory>_<command>` | every registered command | invoked with `.exec(...)`, e.g. `simulator_create_generator.exec(...)`, `query.exec(...)`, `about.exec()` |

The console name is `TextHelper.cleanText(directory + "_" + command)`:
lower case, and every run of characters other than letters, digits, `_`,
`;` and `:` becomes a single `_`. `create generator` in the `simulator`
folder is therefore `simulator_create_generator`. The name is also printed at
startup as `Registering console command: [...]`. `exec(...)` takes the input
arguments of the OPC UA method as strings, in order, and returns an array:
element `0` is the result (for creation commands the NodeId of the vertex,
for example `ns=2;s=cmd-gen` when the id is a string, or `error: ...`).
---

## Vertex and edge syntax

Vertices are created with key/value pairs:

```javascript
var v = graph.addVertex('id', 'temp-1', 'type', 'sim:generator', 'label', 'temp-1',
    'directory', 'plant/sensors', 'algorithm', 'sinusoidal', 'min', '15', 'max', '30', 'delay', '2000');
```

| Key | Meaning |
| --- | ------- |
| `id` | identifier of the OPC UA node (`ns=2;s=temp-1`); generated when omitted |
| `type` | vertex type handled by a plugin (`sim:generator`, `sim:event-generator`, `rules:rule`, `rules:compute`, `gremlin`, ...); omit it for a plain data vertex |
| `label` | label, also OPC UA browse/display name; cannot be changed later |
| `directory` | OPC UA folder of the vertex, levels separated by `/` |
| any other key | vertex property, exposed as an OPC UA variable (use lowercase keys) |

Edges are created from the source vertex:

```javascript
source.addEdge('<label>', target, 'type', '<edge type>', ...properties);
```

Active edge types of WaldOT base: `link-to`, `link-from`, `fire`
(namespace) and `rules:execute` (rules engine).

---

## Complete script example

```javascript
// WaldOT bootstrap (script mode, JEXL)
## double-hash comments are allowed too
/* block comments
   as well */
var sensors = ['office', 'warehouse', 'lab'];

// function stored in a variable
var createSensor = function(name, algorithm) {
  var v = graph.addVertex('id', 'temp-' + name, 'type', 'sim:generator', 'label', 'temp-' + name,
      'directory', 'plant/sensors', 'algorithm', algorithm, 'min', '15', 'max', '30', 'delay', '2000');
  log.info(`created sensor ${name}`);
  return v;
};

// lambda syntax
var plain = (id, dir) -> graph.addVertex('id', id, 'label', id, 'directory', dir);

for (var s : sensors) {
  createSensor(s, s == 'lab' ? 'random' : 'sinusoidal');
}

var i = 0;
while (i < 2) {
  plain('area-' + i, 'plant/areas');
  i = i + 1;
}

var labPresent = false;
for (var v : g.V().has('label', 'temp-lab')) {
  labPresent = true;
}
if (labPresent) {
  log.info('lab sensor present, vertices: ' + cmd.getVerticesCount());
}

// plugin command: label, directory, algorithm, delay, min, max, post-node-id-data
var gen = simulator_create_generator.exec('cmd-gen', 'plant/sensors', 'incremental', '1000', '0', '10', '');
log.info('command result: ' + gen[0]);
```

Log:

```
INFO BOOT - Bootstrap configuration loaded in SCRIPT MODE from URL: file:///waldot/boot.conf
INFO CONSOLE - created sensor office
INFO CONSOLE - created sensor warehouse
INFO CONSOLE - created sensor lab
INFO CONSOLE - lab sensor present, vertices: 5
INFO CONSOLE - command result: ns=2;s=cmd-gen
INFO BOOT - Bootstrap procedure completed successfully.
```

`ns=2;s=cmd-gen` is the shape of a string NodeId for id `cmd-gen`. A numeric
id would be `ns=2;i=...` and is assigned by the server, so do not hard-code it.

### Generator, link, rule

```javascript
// simulated sensor: a new value in 'data' every 5 s
var sensor = graph.addVertex('id', 'temp-sensor-1', 'type', 'sim:generator', 'label', 'temp-sensor-1',
    'directory', 'plant/sensors', 'algorithm', 'sinusoidal', 'min', '18', 'max', '26', 'delay', '5000');

// plain data vertex
var room = graph.addVertex('id', 'control-room', 'label', 'control-room', 'directory', 'plant/areas',
    'temperature', '0');

// link-to: every change of sensor.data is copied into room.temperature
sensor.addEdge('feeds', room, 'type', 'link-to', 'properties-to-monitor', 'data', 'data', 'temperature');

// rules engine: compute vertex + rule + rules:execute edge
var compute = graph.addVertex('id', 'main-compute', 'type', 'rules:compute', 'label', 'main-compute',
    'directory', 'plant/rules', 'threads', '2');
var alarm = graph.addVertex('id', 'temp-alarm', 'type', 'rules:rule', 'label', 'temp-alarm',
    'directory', 'plant/rules',
    'condition', "var hot = false; for (var v : g.V().has('id', 'control-room')) { var raw = v.property('temperature').value(); hot = raw != null && Double.parseDouble(raw.toString()) > 25.0; } hot",
    'action', "log.warn('High temperature in control room')",
    'hysteresis', '10000');
compute.addEdge('runner', alarm, 'type', 'rules:execute', 'priority', '100');

// fire: the rule is queued every time room.temperature changes
room.addEdge('watch', alarm, 'type', 'fire', 'properties-to-monitor', 'temperature');
```

The same graph (without `var`) is shipped as
`waldot-app/examples/boot-production.conf`.

### Error handling

```javascript
// try/catch
try {
  graph.addVertex('id', 'dup', 'label', 'dup');
  graph.addVertex('id', 'dup', 'label', 'dup');
} catch (var e) {
  log.warn('caught: ' + e.getMessage());
}
log.info('vertices=' + cmd.getVerticesCount() + ' commands=' + cmd.listCommands().size());
```
```
WARN CONSOLE - caught: Vertex with id already exists: NodeId{ns=2, id=dup}
```

---

## Loading the file

### Docker

```bash
# default location: /waldot/boot.conf
docker run -p 12686:12686 -v "$PWD/boot.conf:/waldot/boot.conf:ro" rossonet/waldot:latest

# another path
docker run -p 12686:12686 -v "$PWD/custom.conf:/config/custom.conf:ro" \
  rossonet/waldot:latest --boot-url file:///config/custom.conf

# remote file
docker run -p 12686:12686 rossonet/waldot:latest \
  --boot-url https://raw.githubusercontent.com/org/repo/main/waldot/boot.conf
```

Any URL supported by Java (`file://`, `http://`, `https://`) can be used.
Prefer HTTPS and trusted sources: the file can run any console command,
including `cmd.exit()`, which stops the JVM.

Remote URLs use a connect timeout of 10 seconds and a read timeout of
30 seconds (`-Dwaldot.boot.connect-timeout-ms`, `-Dwaldot.boot.read-timeout-ms`;
a value `<= 0` or not numeric is ignored and the default is kept). `file:`
URLs are not subject to those timeouts. A URL that cannot be read leaves the
agent status at `FAULT` and does not log a successful bootstrap.

### Missing or empty file

If the file cannot be read WaldOT starts with an empty graph:

```
ERROR BOOT - Error reading bootstrap configuration from URL: file:///waldot/boot.conf
WARN BOOT - the bootstrap configuration is empty or null. No commands to execute.
```

---

## Troubleshooting

| Log message | Cause | Fix |
| ----------- | ----- | --- |
| `ambiguous statement error` | missing `;` in script mode, or Groovy `def` | end every statement with `;`, use `var f = function(...) {...};` |
| `tokenization error in '#'` | `#` comment in script mode | use `//` or `##` |
| `Vertex with id already exists` | duplicated `id` | use unique ids or `try/catch` |
| `Changing label from X to Y is not allowed` | `g.addV('X').property('label','Y')` | the label of `addV` wins; use `graph.addVertex('label', ...)` |
| `No valid bootstrap configuration found` | line-mode file with only comments | add expressions or remove the file |
| `Bootstrap procedure completed with N error(s).` | one or more expressions threw; status is `FAULT` | fix the expression; line mode still runs the following lines, script mode stops at the first error |
| `Error reading bootstrap configuration` | URL missing, unreadable, or timed out | check `--boot-url` and, for HTTP, the timeout properties |

Check which mode was chosen:

```bash
docker logs <container> 2>&1 | grep "Bootstrap configuration loaded"
```

---

## Related documentation

- [Configuration Reference](../docs/guide/docs/CONFIGURATION.md) - `--boot-url` and the other server options
- [Integration Guide](../docs/guide/docs/INTEGRATION_GUIDE.md) - commands of custom plugins in bootstrap scripts
- [waldot-namespace README](README.md) - module overview
