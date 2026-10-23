# Scenario dimostrativo "Gas Grid"

Rete di distribuzione del gas regolata da **loop controller PI** scritti come regole del rule engine di WaldOT.
Il documento è pensato per chi presenta la demo a un cliente del settore gas: spiega cosa viene creato, cosa guardare e cosa
modificare dal vivo per mostrare come funziona il rule engine.

> **Nota importante.** Il modello è didattico, **non è una simulazione idraulica**. Il processo è un semplice sistema del primo
> ordine e tutti i valori (pressioni, domande, soglie, ritardi) sono **indicativi**. Lo scopo è spiegare il funzionamento del
> rule engine con un problema che il cliente conosce bene, non dimensionare una rete.

---

## 1. Obiettivo della demo

Mostrare, su un caso familiare, che in WaldOT:

1. la **rete è un grafo**: impianti, sensori, valvole e controllori sono vertici; i collegamenti sono archi;
2. la **logica di controllo è dichiarata nel grafo**: ogni regola (`rules:rule`) ha una condizione e un'azione scritte in JEXL;
3. un **evento** (il cambio di una misura) attiva la regola attraverso un arco `fire`;
4. un **thread manager** (`rules:compute`) decide quante regole girano in parallelo e con quale priorità;
5. i dati si propagano tra vertici con gli archi `link-to` e `link-from`, con **banda morta** (deadband) e **ritardo** (delay);
6. si possono **cambiare parametri a caldo** e vedere subito l'effetto.

## 2. Glossario

| Termine | Significato nella demo |
|---|---|
| **City gate** | Punto di consegna dalla rete di trasporto. Qui è un generatore che simula la pressione di monte (circa 22–26 bar). |
| **PRS / REMI** | Stazione di riduzione e misura. Ogni stazione ha trasmettitore di pressione, valvola regolatrice e controllore. |
| **Setpoint** | Pressione a valle desiderata (mbar). Default 25 mbar (0,025 bar, rete di 4ª specie). |
| **Loop controller** | Ciclo di regolazione: misura → errore → calcolo → apertura valvola. |
| **PI** | Controllore proporzionale-integrale (`kp` = 0,4; `ki` = 0,1). |
| **Anti-windup** | L'integrale non cresce quando l'uscita è già satura (0 % o 100 %). |
| **Deadband (banda morta)** | Variazioni più piccole della soglia non vengono propagate. Riduce rumore e carico. |
| **Tempo morto / delay** | Ritardo tra causa ed effetto: qui è il tempo che la pressione impiega ad arrivare al trasmettitore. |
| **ESD** | Emergency Shut Down: chiusura di sicurezza in sovrapressione. |
| **AUTO / MANUAL / ESD** | Modi del controllore. In MANUAL l'uscita è congelata (guasto sensore); in ESD la valvola è chiusa. |

## 3. Come si avvia

Comando di console (directory `rules/showcase`): **`create show case gas grid`**.

| Argomento | Default | Intervallo | Significato |
|---|---|---|---|
| `stations` | 4 | 2–20 | Numero di stazioni di riduzione |
| `delay-ms` | 200 | ≥ 10 | Passo della simulazione (ms) |
| `setpoint` | 25 | 5–100 | Pressione a valle desiderata (mbar) |
| `pipe-delay-ms` | 300 | 0–10000 | Tempo morto del tubo (ms) |
| `gas-type` | `metano` | `metano`, `metano-blu` | Profilo del gas (vedi sotto) |
| `compare` | `true` | `true`, `false` | L'ultima stazione non ha deadband, per confronto |

Gli argomenti non validi producono un messaggio e **non creano nulla**. Se lo scenario esiste già, il comando risponde
`already exists` e non modifica il grafo (per ricrearlo, rimuovere prima i vertici con id `gas-…`).

Vertici creati: `14 + 11 × stazioni` (con 4 stazioni: **58 vertici**).

### Profili del gas (valori indicativi)

| Parametro | `metano` (default) | `metano-blu` |
|---|---|---|
| Idrogeno | 0 % | 20 % |
| Densità relativa | 0,60 | 0,48 |
| Portata equivalente a pari apertura | 1,00 | 1,12 |
| Domanda nominale | 50 | 57,5 (×1,15) |
| Soglia ESD sul setpoint | +30 % | +25 % |
| Deadband trasmettitore | 2,0 mbar | 1,2 mbar |
| Deadband domanda | 5 % | 3 % |
| Deadband pressione di monte | 0,3 bar | 0,2 bar |
| Tempo morto | `pipe-delay-ms` × 1,0 | `pipe-delay-ms` × 1,2 |

"Metano blu" è inteso come metano con una quota di idrogeno. Il profilo è memorizzato sul vertice `gas-network` e usato dalle
regole e dai collegamenti.

## 4. Mappa del grafo

Tutto è sotto `rules/showcase/gas-grid/`, organizzato come uno schema d'impianto:

```
rules/showcase/gas-grid/
├── supply/            gas-gate-gen (pressione di monte), gas-clock (passo simulazione), gas-scan (scansione cicli)
├── transmission/      gas-trunk (dorsale ad alta pressione)
├── stations/
│   ├── station-0/     gas-sensor-0, gas-valve-0, gas-ctrl-0, gas-plant-0
│   ├── station-1/     …
│   └── station-N/     (l'ultima senza deadband se compare=true)
├── consumers/         gas-consumer-i (utenze) + gas-demand-gen-i (profilo di domanda)
├── safety/            gas-leak-monitor (allarme perdite)
├── supervisory/       gas-network (profilo, setpoint di rete, carico), gas-stats (statistiche)
├── rules/
│   ├── control/       gas-rule-pi-i, gas-rule-mode-guard-i
│   ├── safety/        gas-rule-esd-trip-i, gas-rule-esd-reset-i, gas-rule-leak-detect
│   ├── process/       gas-rule-plant-i
│   └── supervisory/   gas-rule-balancer, gas-rule-stats, gas-rule-bad-sensor
├── compute/           gas-control-loop, gas-process, gas-supervisory (+ archi rules:execute)
├── links/             archi link-to e link-from
└── triggers/          archi fire
```

### Ruolo dei vertici di una stazione

| Vertice | Proprietà principali | Ruolo |
|---|---|---|
| `gas-plant-i` | `pressure`, `upstream` | Processo simulato: la pressione a valle reale |
| `gas-sensor-i` | `pressure` | Trasmettitore: vede la pressione con tempo morto e deadband |
| `gas-ctrl-i` | `setpoint`, `mode`, `fault`, `integral`, `last-error`, `output`, `bias`, `kp`, `ki`, `esd-trips` | Stato del loop controller |
| `gas-valve-i` | `opening` (0–100 %) | Valvola regolatrice |
| `gas-consumer-i` | `demand` | Domanda delle utenze della stazione |

### Flusso dei dati (una stazione)

```
 gas-gate-gen ──link-to──▶ gas-trunk ──link-to──▶ gas-plant-i
 (24 bar ± 2)   deadband    pressione   deadband      upstream
                assoluto                 1 %
                                                          │ regola "plant" (processo)
 gas-demand-gen-i ─link-to─▶ gas-consumer-i ──────────────┤  usa: apertura valvola,
                  deadband %    demand                    │       pressione di monte, domanda
                                                          ▼
                                         gas-plant-i.pressure
                                                          │ link-to con DELAY (tempo morto)
                                                          │ + deadband assoluto
                                                          ▼
                                                  gas-sensor-i.pressure
                                                          │ fire
                                                          ▼
 gas-network.setpoint ─link-to▶ gas-ctrl-i ◀── regola PI / guard / ESD
                                      │ output
                                      ▼ link-from (deadband 0,5 %)
                                 gas-valve-i.opening ──▶ (rientra nella regola "plant")
```

Il ciclo è chiuso: la valvola cambia la pressione, il trasmettitore la vede (in ritardo), il controllore corregge la valvola.

## 5. Come funzionano i collegamenti `link-to` e `link-from`

| Proprietà dell'arco | Effetto | Dove è usata |
|---|---|---|
| `properties-to-monitor` | Quale proprietà viene osservata | tutti |
| rinomina (proprietà con il nome della sorgente) | La proprietà di destinazione ha un nome diverso (es. `data` → `pressure`) | gate→trunk, domanda→utenza, trunk→plant, valvola |
| `deadband-type` + `deadband-value` | Passa solo la variazione maggiore della soglia, `absolute` o `percentage` | trasmettitore (2 mbar), domanda (5 %), monte (0,3 bar), valvola (0,5 %) |
| `delay` | Il valore arriva alla destinazione dopo N millisecondi | tubo plant→sensore (tempo morto) |

- **`link-to`**: il valore della sorgente viene copiato nella destinazione.
- **`link-from`**: il valore della *destinazione* viene copiato nella sorgente. La valvola usa `link-from` per leggere `output`
  dal controllore: l'attuatore "tira" il comando.

## 6. Le regole

Ogni regola ha una **condizione** (booleano) e un'**azione**, entrambe in JEXL. La regola scatta quando un arco `fire` segnala il
cambio di una proprietà, e viene eseguita da un `rules:compute`.

| Regola | Scatta quando | Condizione | Azione |
|---|---|---|---|
| `gas-rule-pi-i` | cambia `sensor.pressure` (e ogni scansione) | modo `AUTO` | Calcola l'errore, aggiorna l'integrale con anti-windup, scrive `output` (0–100 %) |
| `gas-rule-mode-guard-i` | cambia `sensor.pressure` | lo stato di guasto cambia | Misura < 0 o > 200: guasto `sensor`, modo `MANUAL` (uscita congelata); rientra in `AUTO` quando la misura è valida |
| `gas-rule-esd-trip-i` | cambia `sensor.pressure` | pressione > setpoint × (1 + margine ESD) | Modo `ESD`, integrale e uscita a 0, conta `esd-trips` |
| `gas-rule-esd-reset-i` | cambia `sensor.pressure` | modo `ESD` e pressione < 50 % del setpoint | Torna in `AUTO` |
| `gas-rule-plant-i` | ogni passo del clock | sempre | Modello del processo: aggiorna `plant.pressure` (vedi sotto) |
| `gas-rule-balancer` | ogni passo del clock | sempre | Media della domanda e adatta il setpoint di rete (isteresi 1 s) |
| `gas-rule-stats` | ogni passo del clock | sempre | Errore medio, apertura media, trip ESD totali (isteresi 1 s) |
| `gas-rule-leak-detect` | cambia l'apertura media | apertura media > 90 % cambia stato dell'allarme | Allarme `leak` / `none`, conta gli allarmi |
| `gas-rule-bad-sensor` | ogni passo del clock | legge un trasmettitore che non esiste | **Errore volontario**: mostra il contatore `errors` |

### Il controllore PI

```
errore   = setpoint − pressione_misurata
integrale = integrale + errore            (non aggiornato se l'uscita è satura nella direzione dell'errore)
uscita   = clamp( bias + kp·errore + ki·integrale , 0 , 100 )
```

`bias` è l'apertura di partenza per raggiungere il setpoint (calcolata dal comando).

### Il processo simulato (modello didattico)

```
target   = 0,9 · apertura · (monte / 24) · fattore_portata − 0,25 · (domanda − nominale) − 0,25 · perdita / stazioni
pressione = pressione + 0,3 · (target − pressione)       (a ogni passo del clock)
```

La pressione sale con l'apertura e con la pressione di monte, scende con la domanda e con le perdite. Il tempo morto è dato
dall'arco `link-to` con `delay` verso il trasmettitore.

## 7. I thread manager (`rules:compute`)

| Compute | Thread | Regole eseguite (priorità) | Perché |
|---|---|---|---|
| `gas-control-loop` | **1** | ESD trip (300), guard (250), ESD reset (200), PI (100) | I controllori non devono sovrapporsi e la **sicurezza ha la precedenza** |
| `gas-process` | **4** | modelli del processo (100) | Calcoli indipendenti, eseguiti **in parallelo** |
| `gas-supervisory` | **1** | leak (60), balancer (50), stats (40), bad-sensor (10) | Logiche lente, **priorità bassa** |

Le regole di sicurezza non hanno isteresi: una notifica scartata potrebbe nascondere la sovrapressione.

## 8. Cosa osservare: percorso guidato (circa 10 minuti)

1. **Avvio.** Lanciare `create show case gas grid`. Aprire la directory `rules/showcase/gas-grid`. Mostrare la struttura per
   directory come uno schema d'impianto. I generatori sono già attivi.
2. **La rete si muove.** Osservare `gas-gate-gen` (pressione di monte), `gas-trunk.pressure`, `gas-plant-0.upstream`,
   `gas-consumer-0.demand`. Notare che i valori arrivano **attraverso gli archi**, non scritti a mano.
3. **Il loop insegue il setpoint.** Guardare `gas-sensor-0.pressure`, `gas-ctrl-0.output`, `gas-valve-0.opening`. La pressione si
   porta vicino a 25 mbar. Mostrare che `gas-valve-0.opening` segue `gas-ctrl-0.output` grazie al `link-from`.
4. **Effetto della deadband.** Confrontare i contatori `total` di `gas-rule-pi-0` (con deadband) e della regola dell'**ultima
   stazione** (senza). Quella senza deadband riceve molte più notifiche a parità di regolazione.
5. **Effetto del ritardo.** Spiegare che il trasmettitore vede la pressione dopo `pipe-delay-ms`. Rilanciare con
   `pipe-delay-ms` alto (es. 1500) e osservare l'oscillazione: è il tempo morto del processo.
6. **Sovrapressione ed ESD.** Scrivere 90 su `gas-sensor-0.pressure`. Il controllore passa a `ESD` (valvola chiusa),
   `esd-trips` sale, le altre stazioni non sono toccate. Quando la pressione scende sotto il 50 % del setpoint torna `AUTO`.
7. **Guasto del trasmettitore.** Scrivere −5 su `gas-sensor-0.pressure`: `fault = sensor`, modo `MANUAL`, uscita congelata.
   Con una misura valida torna `AUTO`.
8. **Perdita.** Scrivere 600 su `gas-leak-monitor.leak`: le valvole si aprono al massimo, l'apertura media supera il 90 % e
   `gas-leak-monitor.alarm` diventa `leak`. Rimettere 0 per annullare.
9. **I contatori del rule engine.** Su ogni regola: `queue` (in coda), `total` (valutazioni), `executed` (azioni riuscite),
   `errors` (guasti). `gas-rule-bad-sensor` ha `errors` che cresce e `executed` fermo a 0: è l'errore voluto.

> Nei log compaiono eccezioni JEXL per la regola `gas-rule-bad-sensor`: sono **normali**, la regola è stata scritta per fallire.

## 9. Interventi dal vivo per il cliente

| Modifica | Come | Effetto atteso |
|---|---|---|
| Cambiare il setpoint di tutta la rete | `gas-network.base-setpoint` (es. 40) | Il bilanciatore lo ridistribuisce via `link-to` ai controllori; le pressioni lo raggiungono |
| Cambiare i guadagni | `gas-ctrl-i.kp`, `gas-ctrl-i.ki` | Risposta più rapida o più oscillante |
| Stringere o allargare la deadband | proprietà `deadband-value` dell'arco `pipe-to-transmitter-i` | Più o meno esecuzioni del loop |
| Cambiare il tempo morto | proprietà `delay` dello stesso arco | Più o meno oscillazione |
| Forzare modo manuale | `gas-ctrl-i.mode` = `MANUAL` | La regola PI non agisce più |
| Fermare un generatore | `algorithm` = `stopped` su `gas-demand-gen-i` | La domanda resta ferma |
| Aumentare la debug di una regola | `debug` della regola | Eventi e log di ogni fase |

## 10. Mappatura sui concetti del rule engine

| Concetto del rule engine | Dove si vede nella demo |
|---|---|
| Regola `condition` / `action` | PI, guard, ESD, leak |
| Arco `fire` | `triggers/`: la misura attiva i controllori |
| `rules:compute` con `threads` | 1 thread per i loop, 4 per il processo |
| Arco `rules:execute` con `priority` | ESD (300) prima del PI (100) |
| Isteresi | bilanciatore e statistiche (1 s) |
| Contatori `queue` / `total` / `executed` / `errors` | su ogni regola |
| Proprietà modificabili a caldo | `setpoint`, `kp`, `ki`, `deadband-value`, `delay` |

## 11. Limiti del modello

- Non è una simulazione idraulica: niente perdite di carico, accumulo nelle condotte, temperatura o composizione reale.
- Unità e valori sono indicativi; la pressione di monte è fissa intorno a 24 bar con una lenta oscillazione.
- Il controllore è un PI discreto, a passo variabile (le esecuzioni dipendono dagli eventi). Non è sintonizzato per un impianto reale.
- Il profilo `metano-blu` modifica soltanto alcuni coefficienti (portata, domanda, soglie, ritardo).

## 12. Domande frequenti

**Perché a volte il PI viene eseguito di rado?** Con la deadband il trasmettitore non notifica variazioni piccole. Per questo
c'è anche una scansione periodica (`gas-scan`), come il ciclo di un PLC.

**Perché la regola ESD non ha isteresi?** Perché una notifica scartata potrebbe nascondere la sovrapressione. La sicurezza
non deve perdere eventi.

**Posso usare più stazioni?** Sì, fino a 20 (circa 234 vertici). Il tempo di avvio cresce linearmente.

**Come si ripete la demo?** Il comando rifiuta una seconda creazione. Rimuovere i vertici `gas-…` o riavviare il server.

**Dove sono i test?** `WaldotRulesEngineGasGridTest` verifica la struttura, le proprietà degli archi, i due profili e il
comportamento (inseguimento del setpoint, deadband, ritardo, ESD, guasto sensore, perdita, contatori).
