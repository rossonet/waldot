# Architettura

## Plugin

`WaldotRulesEnginePlugin` estende `AbstractPluginListener`. In `initialize`:

- `registerTypeNode(RuleVertex.class, "WaldOTRuleObjectType", "Rule", "rules:rule")`
- `registerTypeNode(ComputeVertex.class, "WaldOTComputeObjectType", "Thread Manager", "rules:compute")`
- `registerTypeEdge("rules:execute")`
- `registerCommand(...)` per i comandi di creazione (`create rule`, `create compute`, `create compute monitored edge`) e i tre comandi show case;
- `JexlContextRegistry.register(RULE_CONTEXT)`: il contesto `rule` di `jexl check`, con le variabili di condizione e azione.

La creazione dei vertici avviene per reflection dalla classe base, con il costruttore standard a 11 argomenti. Per questo i vertici non ricevono il plugin: lo ottengono con `WaldotRulesEnginePlugin.getInstance()` (come `LinuxOsPlugin`).

Il plugin possiede il motore JEXL e il contesto base (`log`, `g`, `graph`, `cmd`, `math`, `rand`). `newJexlContext(self)` crea una copia per ogni esecuzione e aggiorna i comandi console, anche quelli di plugin caricati dopo.

## Vertici

Le proprietà sono dichiarate in due punti, entrambi nella classe del vertice:

- `generateParameters(namespace, typeNode)` (statico): `PluginListener.addParameterToTypeNode`, per il type node;
- costruttore: `addManagedProperty`, con validazione e callback sulla modifica.

### `ComputableFireableAbstractOpcVertex`

Base di `RuleVertex`. Contiene una `HysteresisPriorityQueue<RunnableEvent>`:

- `fireEvent` / `fireProperty` accodano un `RunnableEvent`;
- ripetizioni della stessa sorgente entro `hysteresis` ms sono scartate;
- `queue` è aggiornata a ogni `offer`/`poll`/`take`;
- la pulizia periodica della coda aggiorna il proprio timestamp (`lastCleanUpTimeMs`).

### `RuleVertex`

Per ogni evento viene creata una `RuleVertexFireableAction` che:

1. compila la condizione (script in cache, invalidato quando cambia `condition`);
2. valuta la condizione con un contesto proprio; un risultato non booleano è un errore;
3. se vera, compila ed esegue l'azione e poi incrementa `executed`;
4. aggiorna `total` ed `errors`; ogni errore scrive anche `last-error` (fase e messaggio di `JexlErrorFormatter`, con riga, colonna ed estratto) e `last-error-time`.

Con `debug > 0` pubblica un evento per ogni fase (vedi `DebugEventType`).

La stessa esecuzione (`RuleVertex.execute`, risultato `Execution`: stato,
risultato della condizione, risultato dell'azione, errore) è usata dal comando
di vertice `run-once`, che la lancia in un virtual thread e attende al massimo
`RUN_ONCE_TIMEOUT_MS` (30 s). Il comando di vertice `reset-counters` azzera i
contatori (anche gli `AtomicLong` interni) e l'ultimo errore.

### `ComputeVertex`

Implementa `EdgeMonitoredVertex`: alla creazione di un edge `rules:execute` di cui è sorgente crea un `ComputeMonitoredEdge` e lo associa all'edge; alla rimozione lo chiude.

Un thread gestore:

1. prende dalla coda `dirtyNodes` la regola con **peso più alto** (`weight = priority * factor + queue`);
2. rispetta il limite `threads`: se è raggiunto attende senza prendere altri eventi;
3. esegue un evento della regola in un virtual thread;
4. controlla a intervalli i timeout (`execution-timeout-ms`), annulla le azioni in ritardo e incrementa `errors` della regola; rimuove anche i future conclusi con coda vuota;
5. alla chiusura viene interrotto e termina l'executor.

## Edge

`ComputeMonitoredEdge` collega regola e compute: quando `queue` della regola cambia, chiama `ComputeVertex.notifyQueueSizeChange` con la priorità letta dall'edge.

## Thread e concorrenza

- ogni azione gira in un virtual thread;
- ogni esecuzione ha un `ClonableMapContext` proprio;
- `compiledCondition`, `compiledAction`, `closed` e `active` sono `volatile`;
- il contesto base usa un `java.util.Random` (thread-safe) al posto di `ThreadLocalRandom`, che non può essere condiviso tra thread.

## Estensione di `RuleVertex`

Una sottoclasse può sostituire il motore JEXL (`engine()`) e il contesto di
ogni esecuzione (`newContext()`), entrambi `protected`: serve quando le
regole arrivano da una sorgente non fidata e devono girare in una sandbox,
senza `g`, `graph` e comandi della console. Le sottoclassi possono anche
avvolgere `execute(BooleanSupplier)` per leggere gli ingressi prima della
condizione e usare il risultato dopo l'azione; contatori, `last-error` ed
eventi di debug restano quelli di `RuleVertex`. Il `ComputeVertex` può
eseguire più eventi della stessa regola in parallelo se `threads` > 1: una
sottoclasse con stato deve serializzare le proprie esecuzioni.

Esempio: le logiche degli XML della macchina MP104 (`mp104:logic`, motori
`mp104:rule-engine` sottoclassi di `ComputeVertex`), con blocchi
dichiarativi ed espressioni in una sandbox, descritte in
`waldot-plugin-mp104/docs/logiche.md` del repository wot-addons; lo show
case XML (`esempi/rules`) riprende le idee del Gas Grid (soglie di
pressione, blocco di emergenza, regolazione PI).
