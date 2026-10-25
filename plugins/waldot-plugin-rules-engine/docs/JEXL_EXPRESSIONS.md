# Espressioni JEXL

Condizione e azione sono script JEXL. La condizione deve restituire un **booleano**; ogni altro risultato conta come errore (`errors`).

## Variabili

| Nome | Descrizione |
|------|-------------|
| `log` | Logger SLF4J |
| `g` | Gremlin traversal source |
| `graph` | Il grafo WaldOT |
| `cmd` | Comandi WaldOT |
| `self` | Il vertice regola in esecuzione |
| `math` | Classe `java.lang.Math` |
| `rand` | `java.util.Random` |
| comandi console | Per nome, anche quelli dei plugin caricati dopo |

Ogni esecuzione ha un contesto proprio: le variabili assegnate in uno script non sono visibili alle esecuzioni successive. Per conservare uno stato usa una proprietà del vertice (`self.property('counter', n)`).

## Condizioni

```
true
g.V().has('id','a').next().property('value').value() == 10
g.V().hasLabel('sensor').has('status','ERROR').count().next() > 5
```

## Azioni

Più istruzioni si separano con `;`.

```
log.info('working with ' + cmd.getVerticesCount().toString() + ' vertices');
counter = cmd.toNumber(g.V().has('id','b').next().property('value').value()).intValue();
graph.addVertex('name', 'vertice N. ' + counter, 'id', counter);
self.property('counter', counter)
```

## Errori

| Situazione | Effetto |
|------------|---------|
| Condizione non compilabile | `errors` +1, evento `AFTER_CONDITION_COMPILE_EXCEPTION` |
| Condizione non booleana o con eccezione | `errors` +1, evento `AFTER_CONDITION_EXECUTION_EXCEPTION` |
| Azione non compilabile | `errors` +1, evento `AFTER_ACTION_COMPILE_EXCEPTION` |
| Eccezione nell'azione | `errors` +1, evento `AFTER_ACTION_EXECUTION_EXCEPTION` |
| Azione oltre `execution-timeout-ms` | Azione annullata, `errors` +1 |

`executed` aumenta solo se l'azione termina con successo.

## Consigli

- Con `debug` ≥ 1 gli errori di compilazione e di esecuzione sono pubblicati come eventi OPC-UA.
- Le azioni lunghe occupano uno dei `threads` del compute: imposta `execution-timeout-ms` adeguato.
- Gli script compilati sono in cache e vengono ricompilati solo quando `condition` o `action` cambiano.
