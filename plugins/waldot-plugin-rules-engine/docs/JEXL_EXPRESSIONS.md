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

## Contesti

WaldOT esegue script JEXL in più punti, e ognuno ha le sue variabili. Le
variabili di ogni contesto si leggono dal server con `wotctl jexl contexts`
(tool `jexl_contexts`); `wotctl jexl check --context CONTESTO` controlla uno
script senza eseguirlo.

| Contesto | Dove | `g`, `graph`, `log`, `cmd`, `math`, `rand` | `self` | Altre variabili |
|---|---|---|---|---|
| `console` | `query`, `script run`, file di bootstrap | sì | no | i comandi della console per nome |
| `rule` | condizione e azione di `rules:rule` | sì | la regola | i comandi della console |
| `iec104-command` | `set!action` di `iec104:command` | sì | il comando | `value`, `ioa`, `asduType`, `commonAddress`, `select`, `deactivation`, `qualifier`, `timestamp`, `asdu`, i comandi della console |
| `iec104-action` | azioni interne del centro di controllo IEC 104 | sì | il centro di controllo | `value`, `state`, `recovered` (secondo l'azione) |
| `modbus-point` | `set!action` di `modbus:point` | sì | il punto | `value`, `area`, `address`, `unitId`, i comandi della console |

Fino alla versione precedente la console non aveva `math` e `rand`, e le azioni
IEC 104 e Modbus non avevano `cmd`: uno script provato nella console poteva non
funzionare in una regola o in un'azione, e viceversa.

## Messaggi d'errore

Gli errori indicano riga, colonna, causa ed estratto della riga:

```
riga 2, colonna 9: variabile 'value' nulla
  var y = value + 2;
          ^
```

Le cause più comuni: `variabile 'x' non definita` (manca nel contesto o è
scritta male), `variabile 'x' nulla`, `operatore '+' con un operando nullo
(espressione che inizia con 'self.x')`, `errore di sintassi vicino a ...`,
`errore nella chiamata di 'toNumber': NumberFormatException: ...`.

## Trappole

- **Parole riservate.** `and`, `or`, `not`, `eq`, `ne`, `lt`, `gt`, `le`, `ge`,
  `div`, `mod`, `empty`, `size` sono operatori: `var mod = x % 2` è un errore di
  sintassi. `jexl check` lo segnala con il nome della variabile.
- **Array fissi.** `[a, b]` crea un array di dimensione fissa: `add` fallisce.
  Per una lista modificabile si scrive `[a, b, ...]` (vuota: `[...]`).
- **Numeri e booleani da OPC UA.** I valori scritti da un client OPC UA possono
  arrivare come stringhe. Il modo sicuro è `cmd.toNumber(x).doubleValue()` per i
  numeri e `('' + x) == 'true'` per i booleani.
- **Navigazione sicura.** `a.b` con `a` nullo, o un metodo che non esiste,
  restituisce `null` senza errore: un `math.max(...)` in un contesto senza `math`
  diventa `null` in silenzio. `jexl check` segnala le variabili che il contesto
  non fornisce.
- **Assegnazioni senza `var`.** `x = 1` crea una variabile globale del contesto
  (nella console resta tra una query e l'altra); `var x = 1` è locale. `script
  run` esegue su una copia del contesto, quindi non lascia variabili.
- **Valore restituito.** Uno script restituisce il valore dell'ultima
  espressione; una `function` restituisce l'ultima espressione del suo corpo,
  anche senza `return`.

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

Ogni errore scrive anche `last-error` (fase e messaggio con riga, colonna ed
estratto) e `last-error-time`. `last-error` resta fino al comando di vertice
`reset-counters`, che azzera anche `total`, `executed` ed `errors`. Il comando
di vertice `run-once` esegue subito la regola nel suo contesto e restituisce
esito, risultati ed errore. Da wotctl: `wotctl rules status` (tabella con i
delta dall'ultima lettura), `wotctl rules reset`, `wotctl rules run-once`.

## Consigli

- Con `debug` ≥ 1 gli errori di compilazione e di esecuzione sono pubblicati come eventi OPC-UA.
- Le azioni lunghe occupano uno dei `threads` del compute: imposta `execution-timeout-ms` adeguato.
- Gli script compilati sono in cache e vengono ricompilati solo quando `condition` o `action` cambiano.
