# WaldOT Rules Engine Plugin

Motore di regole event-driven IF-THEN per WaldOT. Le regole reagiscono a eventi e variazioni di proprietà del grafo OPC-UA; condizione e azione sono script [JEXL](https://commons.apache.org/proper/commons-jexl/), eseguiti da un pool gestito di virtual thread.

## Elementi forniti

| Elemento | Etichetta (`type`) | Descrizione |
|----------|--------------------|-------------|
| Vertice regola | `rules:rule` | Regola IF-THEN con condizione e azione JEXL |
| Vertice compute | `rules:compute` | Gestore dei thread: esegue le azioni delle regole collegate |
| Edge execute | `rules:execute` | Dal compute (sorgente) alla regola (destinazione) |
| Edge fire | `fire` | Fornito dal namespace: dal vertice sorgente alla regola |

Il plugin estende `AbstractPluginListener` (`registerTypeNode`, `registerTypeEdge`, `registerCommand`). L'istanza è raggiungibile con `WaldotRulesEnginePlugin.getInstance()`.

## Flusso

```
[vertice sorgente] --fire--> [rules:rule] (coda) <--rules:execute-- [rules:compute] --> virtual thread
```

1. una variazione di proprietà o un evento sul vertice sorgente raggiunge la regola tramite l'edge `fire`;
2. la regola accoda l'evento (con isteresi) e aggiorna `queue`;
3. l'edge `rules:execute` avvisa il compute, che ordina le regole per peso;
4. il compute esegue un evento alla volta, entro il limite `threads`;
5. l'esecuzione valuta la `condition` e, se vera, esegue l'`action`.

## Esempio

```java
Vertex compute = graph.addVertex("type", "rules:compute", "label", "main-compute", "threads", "4");
Vertex rule = graph.addVertex("type", "rules:rule", "label", "temp-alarm",
    "condition", "g.V().has('id','sensor').next().property('temperature').value() > 80.0",
    "action", "log.warn('Temperature alarm')");

// il compute esegue la regola (direzione: compute -> regola)
compute.addEdge("runner", rule, "type", "rules:execute", "priority", "100");

// quando il sensore cambia, la regola viene attivata
sensor.addEdge("watch", rule, "type", "fire", "properties-to-monitor", "temperature");
```

## Proprietà

Tutte le etichette sono minuscole e coincidono con le chiavi passate ad `addVertex`.

### `rules:rule`

| Proprietà | Tipo | Default | Modificabile | Descrizione |
|-----------|------|---------|--------------|-------------|
| `condition` | String | `true` | sì | Script JEXL che deve restituire un booleano |
| `action` | String | `log.info('action fired')` | sì | Script JEXL eseguito se la condizione è vera |
| `debug` | Int32 | 0 | sì | Livello debug (0 = disattivo) |
| `hysteresis` | Int64 | 0 | sì | Finestra in ms in cui le notifiche ripetute della stessa sorgente sono scartate |
| `queue` | Int64 | 0 | no | Eventi in attesa |
| `total` | Int64 | 0 | no | Eventi valutati |
| `executed` | Int64 | 0 | no | Azioni eseguite con successo |
| `errors` | Int64 | 0 | no | Errori (compilazione, eccezioni, condizione non booleana) |

### `rules:compute`

| Proprietà | Tipo | Default | Descrizione |
|-----------|------|---------|-------------|
| `threads` | Int32 | 1 | Numero massimo di azioni simultanee |
| `execution-timeout-ms` | Int64 | 120000 | Durata massima di un'azione |
| `factor` | Double | 100.0 | Moltiplicatore della priorità dell'edge |
| `queue` | Int64 | 0 | Regole con eventi in attesa (sola lettura) |

Valori non validi (non numerici, `threads` ≤ 0) vengono rifiutati o sostituiti dal default, con un avviso nel log.

### Edge `rules:execute`

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `priority` | 100 | Priorità della regola nel compute |

Peso di una regola: `weight = priority * factor + queue`. La regola con **peso più alto** viene eseguita per prima.

## Comandi console

| Comando | Descrizione |
|---------|-------------|
| `create rule` | Crea un vertice `rules:rule` (label, condition, action, debug, hysteresis) |
| `create compute` | Crea un vertice `rules:compute` (label, directory, threads) |
| `create compute monitored edge` | Crea un edge `rules:execute` dal compute alla regola |
| `create show case arena` | Crea l'esempio "Dungeon Arena" (16 vertici, argomento `delay-ms`) |
| `create show case evolution` | Crea l'esempio "Evolution Lab" (algoritmo evolutivo, oltre 100 vertici; argomenti `population-size`, `delay-ms`, `target-fitness`, `seed`) |
| `create show case gas grid` | Crea l'esempio "Gas Grid" (rete di distribuzione del gas con loop controller PI sulle valvole, `link-to`/`link-from` con deadband e delay; argomenti `stations`, `delay-ms`, `setpoint`, `pipe-delay-ms`, `gas-type`, `compare`). Guida per la demo: [SHOWCASE_GAS_GRID.md](docs/SHOWCASE_GAS_GRID.md) |

I comandi `show case` creano grafi "vivi" (generatori `sim:generator` + edge `link-to` + `fire` + `rules:execute`), organizzati nelle directory `rules/showcase/<scenario>/...`, e rifiutano la creazione se lo scenario esiste già. Vedi [Esempi](docs/EXAMPLES.md#show-case).

## Variabili JEXL

Ogni esecuzione ha un contesto proprio: le variabili assegnate non passano alla successiva.

`log`, `g` (traversal), `graph`, `cmd`, `self` (la regola), `math`, `rand` (`java.util.Random`) e tutti i comandi console, per nome (inclusi quelli dei plugin caricati dopo).

## Debug

Con `debug > 0` la regola pubblica un evento OPC-UA per ogni fase: `BEFORE/AFTER_CONDITION_COMPILE`, `BEFORE/AFTER_CONDITION_EXECUTION`, `BEFORE/AFTER_ACTION_COMPILE`, `BEFORE/AFTER_ACTION_EXECUTION` e le varianti `*_EXCEPTION`. Con `debug > 1` le fasi sono anche scritte nel log.

## Documentazione

- [Quickstart](docs/QUICKSTART.md)
- [Architettura](docs/ARCHITECTURE.md)
- [Riferimento API](docs/API_REFERENCE.md)
- [Espressioni JEXL](docs/JEXL_EXPRESSIONS.md)
- [Esempi](docs/EXAMPLES.md)

## Test

```
./gradlew :waldot-plugin-rules-engine:test
```
