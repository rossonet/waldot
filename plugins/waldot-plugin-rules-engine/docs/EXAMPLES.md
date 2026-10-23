# Esempi

## Allarme di temperatura

```java
Vertex compute = graph.addVertex("type", "rules:compute", "label", "main", "threads", "2");
Vertex alarm = graph.addVertex("type", "rules:rule", "label", "temp-alarm",
    "condition", "g.V().has('id','sensor').next().property('temperature').value() > 80.0",
    "action", "log.warn('Temperature alarm')");
compute.addEdge("runner", alarm, "type", "rules:execute", "priority", "100");
sensor.addEdge("watch", alarm, "type", "fire", "properties-to-monitor", "temperature");
```

## Priorità tra regole

La regola con peso più alto (`priority * factor + queue`) è servita per prima.

```java
compute.addEdge("runner", alarmRule, "type", "rules:execute", "priority", "100");
compute.addEdge("runner", logRule,   "type", "rules:execute", "priority", "10");
```

## Isteresi

Scarta le notifiche ripetute della stessa sorgente entro 500 ms:

```java
graph.addVertex("type", "rules:rule", "label", "debounced",
    "condition", "true", "action", "log.info('changed')", "hysteresis", "500");
```

## Regola che scrive sul grafo

```java
Vertex rule = graph.addVertex("id", "rule", "type", "rules:rule", "counter", "0",
    "condition", "g.V().has('id','a').next().property('value').value() == 10",
    "action", "counter = cmd.toNumber(g.V().has('id','b').next().property('value').value()).intValue(); "
            + "graph.addVertex('name','vertice N. ' + counter,'id',counter); self.property('counter',counter)");
```

Vedi `WaldotRulesEngineBaseTest` per l'esempio completo con due regole e un compute.

## Debug

```java
graph.addVertex("type", "rules:rule", "label", "dbg", "debug", "2",
    "condition", "true", "action", "log.info('x')");
```

Con `debug = 2` gli eventi di fase sono pubblicati e scritti nel log.

## Modifica a runtime

```java
rule.property("condition", "false");   // lo script viene ricompilato
compute.property("threads", "8");      // nuovo limite di concorrenza
```

## Show case

I comandi `create show case arena` e `create show case evolution` costruiscono due scenari completi che si muovono da soli. Servono per studiare il plugin sul grafo. Usano `waldot-plugin-generator` (`sim:generator`) per i dati in movimento, edge `link-to` (generatore → stato, proprietà `data`), edge `fire` (stato → regola) ed edge `rules:execute` (compute → regola). Se l'id marcatore dello scenario esiste già, il comando risponde `already exists` e non crea nulla.

### Dungeon Arena (`arena-*`, 16 vertici)

Tower defense. Argomento: `delay-ms` (default 250, minimo 10).

| Directory | Contenuto |
|-----------|-----------|
| `rules/showcase/arena/state` | `arena-tower` (hp, kills, spells, data = mana), `arena-wave` (data = nemici, difficulty), `arena-player` (level, gold, score, defeats), `arena-turn` |
| `rules/showcase/arena/generators` | `random` (ondate), `sinusoidal` (mana), `incremental` (turni) |
| `rules/showcase/arena/rules` | 7 regole |
| `rules/showcase/arena/compute` | `arena-combat` (4 thread), `arena-economy` (1 thread) |
| `rules/showcase/arena/triggers` | (gli edge `fire`) |

Regole: `spawn-wave` (aumenta la difficoltà), `fire-spell` (hysteresis 300 ms), `tower-damage`, `level-up`, `game-over` (la partita riparte), `turn-tick` e `broken`, che fallisce sempre per mostrare il contatore `errors`. Le condizioni false aumentano solo `total`.

### Evolution Lab (`evo-*`, 104 vertici con la popolazione minima)

Algoritmo evolutivo che massimizza `100 - Rastrigin(x, y, z)` (massimo 100 in 0,0,0). Argomenti: `population-size` (default 40, minimo 30, massimo 1000), `delay-ms` (default 200), `target-fitness` (default 99) e `seed` (default 42). I vertici sono `3N + 14`.

- `population`: N individui (geni `x`, `y`, `z`, `fitness`, `data` = rumore).
- `generators`: N generatori `random` (rumore di mutazione, link-to sull'individuo), un `incremental` (orologio delle generazioni) e un `sinusoidal` (tasso di mutazione 5..50).
- `rules/evaluate`: una regola per individuo; se il rumore è sotto il tasso di mutazione muta i geni e tiene la mutazione solo se la fitness migliora. Girano in parallelo sul compute `evo-eval-pool` (8 thread).
- `rules/breeding`: eseguite una alla volta dal compute `evo-breeding` (1 thread): `select-best` (hall of fame, hysteresis 150 ms), `crossover` (ogni 3 generazioni sostituisce il peggiore con la media tra il migliore e un individuo casuale), `stagnation` (alza `boost` del tasso di mutazione), `converged` (ferma i generatori), `stats` (`debug = 1`) e `bad-input`, che legge un vertice inesistente per mostrare `errors`.
- `control`: `evo-hall-of-fame`, `evo-generation`, `evo-mutation-rate`, `evo-stats`.
- Le priorità della regola `select-best` sono basse perché è la più frequente: con un solo thread le altre non devono restare in coda.

## Test

`WaldotRulesEngineShowCaseTest` verifica la struttura dei due scenari (numero di vertici, directory, thread), il rifiuto della seconda creazione e degli argomenti non validi e che i grafi si muovano davvero (contatori, hall of fame, convergenza, errori della regola rotta).


`WaldotRulesEngineBehaviorTest` verifica: tipi dei vertici, default e valori non validi, peso in funzione della priorità, condizione vera/falsa, errori di condizione e azione, modifiche a runtime, più thread, rimozione di una regola e chiusura del compute. `WaldotRulesEngineBaseTest` è lo scenario completo con due regole.
