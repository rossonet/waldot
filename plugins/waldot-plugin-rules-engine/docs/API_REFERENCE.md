# Riferimento API

## `WaldotRulesEnginePlugin`

Estende `AbstractPluginListener`. Annotato `@WaldotPlugin`.

| Metodo | Descrizione |
|--------|-------------|
| `static getInstance()` | Ultima istanza inizializzata (null se non inizializzato) |
| `static getInstance(WaldotNamespace)` | Plugin registrato nel namespace, con ripiego su `getInstance()` |
| `getJexlEngine()` | Motore JEXL condiviso |
| `baseJexlContext()` | Contesto base (`log`, `g`, `graph`, `cmd`, `math`, `rand`) |
| `newJexlContext(Object self)` | Copia del contesto base con `self` e i comandi console aggiornati |
| `initialize(WaldotNamespace)` | Registra tipi, edge e comandi |
| `close()` | Azzera l'istanza statica e chiude la classe base |

### Costanti

| Costante | Valore |
|----------|--------|
| `RULE_NODE_PARAMETER` | `rules:rule` |
| `COMPUTE_NODE_PARAMETER` | `rules:compute` |
| `EXECUTE_EDGE_LABEL` | `rules:execute` |
| `FIRE_EDGE_PARAMETER` | `fire` |
| `CONDITION_FIELD` | `condition` |
| `ACTION_FIELD` | `action` |
| `DEBUG_LEVEL_LABEL` | `debug` |
| `HYSTERESIS_LABEL` | `hysteresis` |
| `QUEUE_SIZE_LABEL` | `queue` |
| `TOTAL_SIZE_LABEL` | `total` |
| `ACTION_EXECUTED_SIZE_LABEL` | `executed` |
| `ERRORS_SIZE_LABEL` | `errors` |
| `THREAD_POOL_SIZE_FIELD` | `threads` |
| `EXECUTION_TIMEOUT_MS_FIELD` | `execution-timeout-ms` |
| `PRIORITY_FACTOR_FIELD` | `factor` |
| `PRIORITY_FIELD` | `priority` (proprietà dell'edge) |
| `BASE_CMD_DIRECTORY` | `rules` |

Default: `DEFAULT_CONDITION_VALUE = "true"`, `DEFAULT_ACTION_VALUE = "log.info('action fired')"`, `DEFAULT_THREAD_POOL_SIZE_IN_COMPUTE = 1`, `DEFAULT_EXECUTION_TIMEOUT_MS_IN_COMPUTE = 120000`, `DEFAULT_PRIORITY_FACTOR_IN_COMPUTE = 100.0`, `DEFAULT_PRIORITY_VALUE = 100`.

## `RuleVertex` (`rules:rule`)

Estende `ComputableFireableAbstractOpcVertex`.

| Proprietà | Tipo | Default | Accesso |
|-----------|------|---------|---------|
| `condition` | String | `true` | lettura/scrittura |
| `action` | String | `log.info('action fired')` | lettura/scrittura |
| `debug` | Int32 | 0 | lettura/scrittura |
| `hysteresis` | Int64 | 0 | lettura/scrittura |
| `queue` | Int64 | 0 | sola lettura |
| `total` | Int64 | 0 | sola lettura |
| `executed` | Int64 | 0 | sola lettura |
| `errors` | Int64 | 0 | sola lettura |

Un cambio di `condition` o `action` invalida lo script compilato. Un valore non numerico per `debug` o `hysteresis` è rifiutato.

| Metodo | Descrizione |
|--------|-------------|
| `fireEvent(UaNode, BaseEventType, int priority)` | Accoda un evento |
| `fireProperty(UaNode, String, DataValue, int priority)` | Accoda una variazione di proprietà |
| `close()` | Chiude la regola e svuota la coda |

### `DebugEventType`

`BEFORE_CONDITION_COMPILE`, `AFTER_CONDITION_COMPILE`, `AFTER_CONDITION_COMPILE_EXCEPTION`, `BEFORE_CONDITION_EXECUTION`, `AFTER_CONDITION_EXECUTION`, `AFTER_CONDITION_EXECUTION_EXCEPTION`, `BEFORE_ACTION_COMPILE`, `AFTER_ACTION_COMPILE`, `AFTER_ACTION_COMPILE_EXCEPTION`, `BEFORE_ACTION_EXECUTION`, `AFTER_ACTION_EXECUTION`, `AFTER_ACTION_EXECUTION_EXCEPTION`.

`AFTER_ACTION_EXECUTION` indica un'azione riuscita; la variante `_EXCEPTION` un'azione fallita.

## `ComputeVertex` (`rules:compute`)

Implementa `EdgeMonitoredVertex`.

| Proprietà | Tipo | Default | Accesso |
|-----------|------|---------|---------|
| `threads` | Int32 | 1 | lettura/scrittura (> 0) |
| `execution-timeout-ms` | Int64 | 120000 | lettura/scrittura |
| `factor` | Double | 100.0 | lettura/scrittura |
| `queue` | Int64 | 0 | sola lettura |

Peso: `priority * factor + queue`; la regola con peso più alto è servita per prima.

## Edge `rules:execute`

Sorgente: `rules:compute`. Destinazione: `rules:rule`. Proprietà: `priority` (default 100). Crea un `ComputeMonitoredEdge`.

## Comandi console

| Comando | Argomenti |
|---------|-----------|
| `create rule` | label, condition, action, debug, hysteresis |
| `create compute` | label, directory, threads, execution-timeout-ms, factor |
| `create compute monitored edge` | label, source (compute), target (regola), directory, priority |

Directory dei comandi: `rules` (e `rules/edges` per l'edge). Il risultato è l'id del vertice o dell'edge creato.

Quando un'azione supera `execution-timeout-ms` viene annullata e conteggiata in `errors` della regola.
