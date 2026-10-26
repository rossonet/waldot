# Examples

Step-by-step examples for a WaldOT server with the base plugins. Each page can
be run on the Docker image: save the script as `boot.conf` and start

```bash
docker run --rm --name waldot -p 12686:12686 \
  -v "$PWD/boot.conf:/waldot/boot.conf:ro" rossonet/waldot:latest
```

then open `opc.tcp://localhost:12686/waldot` with an OPC UA client and browse
`Gremlin Engine/Vertices`.

| Page | Topics |
| ---- | ------ |
| [1. Plain vertices](01-plain-vertices.md) | vertices, properties, directories, passive edges, Gremlin traversals |
| [2. Active edges](02-active-edges.md) | `link-to`, rename, deadband, delay, `link-from` |
| [3. Generators and rules](03-generators-and-rules.md) | `sim:generator`, `sim:event-generator`, `rules:rule`, `rules:compute`, `fire`, `rules:execute` |
| [4. Complete bootstrap](04-bootstrap.md) | the previous pages in one file, plugin commands, Gremlin Server |
| [5. Showcase: Dungeon Arena](05-showcase-arena.md) | rules with priorities, hysteresis, threads, errors |
| [6. Showcase: Evolution Lab](06-showcase-evolution.md) | parallel rules, evolutionary algorithm, more than 100 vertices |
| [7. Showcase: Gas Grid](07-showcase-gas-grid.md) | PI control loops, deadband and delay on links, safety rules |

Scripts use the JEXL syntax described in
[Bootstrap Configuration](https://github.com/rossonet/waldot/blob/master/waldot-namespace/BOOTSTRAP_CONFIGURATION.md):
statements end with `;`.
