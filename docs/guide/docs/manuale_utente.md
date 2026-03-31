# Manuale Utente WaldOT

## Indice

1. [Introduzione](#introduzione)
2. [Installazione e Avvio](#installazione-e-avvio)
3. [Configurazione del Container](#configurazione-del-container)
4. [File di Bootstrap boot.conf](#file-di-bootstrap-bootconf)
5. [Plugin Integrati](#plugin-integrati)
6. [Connessione con Client OPC UA](#connessione-con-client-opc-ua)
7. [Esempi Pratici](#esempi-pratici)
8. [Risoluzione Problemi](#risoluzione-problemi)

---

## Introduzione

WaldOT è un server OPC UA innovativo che integra:

- **OPC UA** (Eclipse Milo) per la comunicazione industriale standard
- **Apache TinkerPop** per la gestione dei dati come grafo
- **Gremlin** come linguaggio di query grafo
- **Sistema di Plugin** per funzionalità estensibili

### Cosa Puoi Fare con WaldOT

- Creare **Digital Twin** di impianti industriali
- Implementare **regole di automazione** (IF-THEN-THAT)
- **Simulare sensori** per test e sviluppo
- Eseguire **query complesse** sui dati con Gremlin
- Integrare sistemi OT e IT moderni

---

## Installazione e Avvio

### Prerequisiti

- Docker installato sul sistema
- Porta 12686 disponibile (OPC UA TCP)
- Porta 8443 disponibile (HTTPS, opzionale)

### Avvio Rapido

```bash
docker pull rossonet/waldot:latest
docker run -p 12686:12686 -p 8443:8443 rossonet/waldot:latest
```

**Endpoint OPC UA**: `opc.tcp://localhost:12686/waldot`

### Verifica Funzionamento

```bash
# Verifica che il container sia in esecuzione
docker ps | grep waldot

# Controlla i log
docker logs <container-id>

# Test connettività porta
telnet localhost 12686
```

Se vedi "Connected to localhost" il server è attivo!

---

## Configurazione del Container

WaldOT può essere configurato tramite **variabili d'ambiente** che corrispondono ai parametri Picocli del server.

### Variabili d'Ambiente Principali

#### Configurazione di Rete

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `WALDOT_TCP_PORT` | `12686` | Porta TCP per OPC UA (opc.tcp://) |
| `WALDOT_HTTPS_PORT` | `8443` | Porta HTTPS per servizi web |
| `WALDOT_BIND_ADDRESSES` | `0.0.0.0` | Indirizzi IP di ascolto |
| `WALDOT_BIND_HOSTNAME` | `127.0.0.1` | Hostname negli endpoint URL |
| `WALDOT_ENDPOINT_PATH` | `/waldot` | Path URL endpoint |

**Esempio - Porta Standard OPC UA**:
```bash
docker run \
  -e WALDOT_TCP_PORT=4840 \
  -p 4840:4840 \
  rossonet/waldot:latest
```

#### Identità del Server

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `WALDOT_APPLICATION_NAME` | `WaldOT OPCUA server` | Nome server visibile ai client |
| `WALDOT_PRODUCT_NAME` | `WaldOT` | Nome prodotto |
| `WALDOT_MANUFACTURER_NAME` | `Rossonet s.c.a r.l.` | Nome produttore |

**Esempio - Branding Personalizzato**:
```bash
docker run \
  -e WALDOT_APPLICATION_NAME="Server Produzione Linea 1" \
  -e WALDOT_PRODUCT_NAME="WaldOT Industriale" \
  -e WALDOT_MANUFACTURER_NAME="La Tua Azienda" \
  -p 12686:12686 \
  rossonet/waldot:latest
```

#### Sicurezza e Autenticazione

| Variabile | Default | Descrizione | Sicurezza |
|-----------|---------|-------------|-----------|
| `WALDOT_ANONYMOUS_ACCESS` | `true` | Permetti accesso anonimo | ⚠️ CRITICO |
| `WALDOT_FACTORY_USERNAME` | `admin` | Username amministratore | ⚠️ ALTO |
| `WALDOT_FACTORY_PASSWORD` | `password123` | Password amministratore | 🔴 CRITICO |
| `WALDOT_SECURITY_DIR` | `.security` | Directory certificati SSL | ⚠️ MEDIO |

**⚠️ ATTENZIONE SICUREZZA**: 
- **MAI usare la password di default in produzione!**
- **Disabilitare sempre l'accesso anonimo in ambienti produttivi**

**Esempio - Configurazione Sicura**:
```bash
docker run \
  -e WALDOT_ANONYMOUS_ACCESS=false \
  -e WALDOT_FACTORY_USERNAME=admin_produzione \
  -e WALDOT_FACTORY_PASSWORD=Password_Sicura_2024! \
  -v /percorso/sicuro/security:/app/.security \
  -p 12686:12686 \
  rossonet/waldot:latest
```

#### Configurazione Applicativa

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `WALDOT_NAMESPACE_URI` | `urn:rossonet:waldot:engine` | URI namespace OPC UA |
| `WALDOT_BOOT_URL` | `file:///waldot/boot.conf` | File configurazione avvio |
| `WALDOT_HELP_DIRECTORY` | `/app/help` | Directory documentazione |

### Docker Compose Completo

Crea un file `docker-compose.yml`:

```yaml
version: '3.8'

services:
  waldot:
    image: rossonet/waldot:latest
    container_name: waldot-production
    
    ports:
      - "12686:12686"  # OPC UA
      - "8443:8443"    # HTTPS
    
    environment:
      # Rete
      - WALDOT_TCP_PORT=12686
      - WALDOT_BIND_ADDRESSES=0.0.0.0
      
      # Identità
      - WALDOT_APPLICATION_NAME=Server WaldOT Produzione
      - WALDOT_PRODUCT_NAME=WaldOT Industrial
      - WALDOT_MANUFACTURER_NAME=La Mia Azienda
      
      # Sicurezza
      - WALDOT_ANONYMOUS_ACCESS=false
      - WALDOT_FACTORY_USERNAME=admin
      - WALDOT_FACTORY_PASSWORD=${WALDOT_PASSWORD}  # Da .env
      
      # Bootstrap
      - WALDOT_BOOT_URL=file:///waldot/boot.conf
    
    volumes:
      # Certificati persistenti
      - waldot-security:/app/.security
      # Configurazione iniziale personalizzata
      - ./boot.conf:/waldot/boot.conf:ro
      # Help personalizzato
      - ./help:/app/help:ro
    
    restart: unless-stopped
    
    healthcheck:
      test: ["CMD", "sh", "-c", "netstat -an | grep 12686 || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  waldot-security:
    driver: local
```

Crea un file `.env` nella stessa directory:
```bash
WALDOT_PASSWORD=Password_Sicura_Da_Non_Committare!
```

Avvia con:
```bash
docker-compose up -d
```

---

## File di Bootstrap boot.conf

Il file `/waldot/boot.conf` viene eseguito all'avvio del server e permette di:

- Creare la struttura iniziale del grafo
- Configurare vertici e edge (nodi OPC UA)
- Inizializzare plugin (generator, rules-engine, tinkerpop)
- Definire relazioni tra componenti

### Sintassi del File

Il file usa la sintassi **Gremlin/Groovy**. Ogni comando crea elementi nel grafo che sono automaticamente sincronizzati con l'address space OPC UA.

### Struttura Base

```groovy
// Commenti con //

// Creare un vertice
g.addV('tipo')
  .property('chiave', 'valore')
  .property('altra_chiave', 123)

// Creare una relazione (edge)
v1.addEdge('etichetta_relazione', v2, 'proprietà', 'valore')
```

### Esempio 1: Configurazione Minima

```groovy
// File: boot.conf
// Configurazione minima per WaldOT

// Crea un nodo di test
g.addV('test')
  .property('label', 'nodo-test')
  .property('descrizione', 'Nodo di prova')
  .property('valore', 42)
```

Dopo l'avvio, troverai il nodo `test` nell'address space OPC UA.

### Esempio 2: Sensore di Temperatura Simulato

```groovy
// Crea un generatore che simula un sensore di temperatura
tempSensor = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'sensore-temperatura-ufficio')
  .property('Algorithm', 'sinusoidal')  // Onda sinusoidale
  .property('Min', '18')                // 18°C minimo
  .property('Max', '26')                // 26°C massimo
  .property('Delay', '5000')            // Aggiorna ogni 5 secondi
  .next()

// Il valore generato sarà nella proprietà "data"
```

### Esempio 3: Sistema di Monitoraggio Completo

```groovy
// ===================================
// Sistema di monitoraggio temperatura
// con allarme automatico
// ===================================

// 1. Crea il generatore di temperatura
tempSensor = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'temp-sala-server')
  .property('Algorithm', 'random')
  .property('Min', '18')
  .property('Max', '30')
  .property('Delay', '2000')  // Ogni 2 secondi
  .next()

// 2. Crea il gestore di esecuzione regole (compute)
compute = g.addV('compute')
  .property('type', 'compute')
  .property('label', 'gestore-regole')
  .property('Threads', '4')                    // 4 thread concorrenti
  .property('execution-timeout-ms', '120000')  // Timeout 2 minuti
  .property('Factor', '100.0')                 // Fattore priorità
  .next()

// 3. Crea regola di allarme temperatura alta
highTempRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'allarme-temperatura-alta')
  .property('Condition', 'temperature > 28.0')  // Condizione IF
  .property('Action', "log.warn('ALLARME: Temperatura alta ' + temperature + '°C')")
  .property('Priority', '100')    // Alta priorità
  .property('Hysteresis', '10000') // Deduplica 10 secondi
  .property('Debug', '0')          // Debug off
  .next()

// 4. Crea regola temperatura normale
normalTempRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'temperatura-normale')
  .property('Condition', 'temperature >= 18.0 && temperature <= 25.0')
  .property('Action', "log.info('Temperatura normale: ' + temperature + '°C')")
  .property('Priority', '50')
  .property('Hysteresis', '30000')  // Deduplica 30 secondi
  .next()

// 5. Connetti le regole al compute per l'esecuzione
highTempRule.addEdge('execute', compute, 'Priority', '100')
normalTempRule.addEdge('execute', compute, 'Priority', '50')

// 6. Attiva il monitoraggio del sensore
tempSensor.addEdge('fire', highTempRule, 
  'monitor-property', 'temperature',  // Monitora proprietà "temperature"
  'active', 'true',
  'priority', '100'
)

tempSensor.addEdge('fire', normalTempRule,
  'monitor-property', 'temperature',
  'active', 'true',
  'priority', '50'
)

// Log di conferma
log.info("Sistema di monitoraggio temperatura configurato")
log.info("Sensore: temp-sala-server")
log.info("Regole attive: allarme-temperatura-alta, temperatura-normale")
```

### Esempio 4: Linea Produzione con Contatori

```groovy
// ====================================
// Simulazione linea di produzione
// ====================================

// Contatore pezzi prodotti
piecesCounter = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'contatore-pezzi')
  .property('Algorithm', 'incremental')  // Incrementa linearmente
  .property('Min', '0')
  .property('Max', '10000')
  .property('Delay', '2000')  // +1 ogni 2 secondi
  .next()

// Contatore scarti
defectsCounter = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'contatore-scarti')
  .property('Algorithm', 'random')
  .property('Min', '0')
  .property('Max', '50')
  .property('Delay', '10000')  // Aggiorna ogni 10 secondi
  .next()

// Sensore velocità linea (RPM)
speedSensor = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'velocita-linea')
  .property('Algorithm', 'sinusoidal')
  .property('Min', '50')   // 50 RPM min
  .property('Max', '120')  // 120 RPM max
  .property('Delay', '1000')
  .next()

// Compute per regole
productionCompute = g.addV('compute')
  .property('type', 'compute')
  .property('label', 'compute-produzione')
  .property('Threads', '2')
  .next()

// Regola: Troppi scarti
highDefectsRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'allarme-scarti-alti')
  .property('Condition', 'defects > 30')
  .property('Action', "log.error('ALLARME QUALITA: Scarti elevati: ' + defects)")
  .property('Priority', '100')
  .property('Hysteresis', '15000')
  .next()

// Connetti regola
highDefectsRule.addEdge('execute', productionCompute, 'Priority', '100')
defectsCounter.addEdge('fire', highDefectsRule,
  'monitor-property', 'data',  // Il generatore usa "data" come proprietà
  'active', 'true'
)

log.info("Linea produzione simulata configurata")
```

### Esempio 5: Server Gremlin per Query Esterne

```groovy
// ====================================
// Abilita accesso Gremlin esterno
// ====================================

// Crea un Gremlin Server sulla porta 8182
gremlinServer = g.addV('gremlin')
  .property('type', 'gremlin')
  .property('label', 'gremlin-server-pubblico')
  .property('Port', '8182')
  .property('Bind', '0.0.0.0')  // Ascolta su tutte le interfacce
  .next()

log.info("Gremlin Server avviato su porta 8182")
log.info("Connetti con: ws://localhost:8182/gremlin")
```

Dopo questa configurazione puoi connetterti con:
- Gremlin Console
- Graph-Explorer (https://graphexp.io/)
- Client TinkerPop (Java, Python, JavaScript)

### Esempio 6: Sistema Multi-Zona

```groovy
// ====================================
// Monitoraggio Multi-Zona
// ====================================

// Crea compute centrale
centralCompute = g.addV('compute')
  .property('type', 'compute')
  .property('label', 'compute-centrale')
  .property('Threads', '8')
  .next()

// Funzione helper per creare zona
def creaZona(nome, tempMin, tempMax) {
  // Sensore temperatura
  sensor = g.addV('generator')
    .property('type', 'generator')
    .property('label', "temp-${nome}")
    .property('Algorithm', 'random')
    .property('Min', tempMin.toString())
    .property('Max', tempMax.toString())
    .property('Delay', '3000')
    .next()
  
  // Regola allarme
  rule = g.addV('rule')
    .property('type', 'rule')
    .property('label', "allarme-${nome}")
    .property('Condition', "temperature > ${tempMax - 2}")
    .property('Action', "log.warn('Zona ${nome}: Temp alta ' + temperature + '°C')")
    .property('Priority', '100')
    .next()
  
  // Connessioni
  rule.addEdge('execute', centralCompute, 'Priority', '100')
  sensor.addEdge('fire', rule, 
    'monitor-property', 'temperature',
    'active', 'true'
  )
  
  log.info("Zona ${nome} configurata")
}

// Crea 3 zone
creaZona('uffici', '18', '26')
creaZona('magazzino', '10', '30')
creaZona('sala-server', '18', '24')

log.info("Sistema multi-zona attivo")
```

### Montare il File boot.conf

**Opzione 1: Volume Mount**
```bash
docker run \
  -v /percorso/locale/boot.conf:/waldot/boot.conf:ro \
  -p 12686:12686 \
  rossonet/waldot:latest
```

**Opzione 2: Docker Compose**
```yaml
services:
  waldot:
    image: rossonet/waldot:latest
    volumes:
      - ./boot.conf:/waldot/boot.conf:ro
```

**Opzione 3: Variabile d'Ambiente (path alternativo)**
```bash
docker run \
  -e WALDOT_BOOT_URL=file:///config/mio-boot.conf \
  -v /percorso/locale/mio-boot.conf:/config/mio-boot.conf:ro \
  -p 12686:12686 \
  rossonet/waldot:latest
```

### Debug del boot.conf

Se il file non funziona, controlla i log:

```bash
docker logs <container-id> 2>&1 | grep -i "boot"
```

Errori comuni:
- **Sintassi Groovy errata**: Controlla parentesi e virgolette
- **Proprietà obbligatorie mancanti**: `type`, `label` sono spesso richiesti
- **Riferimenti a variabili non definite**: Usa `.next()` per salvare vertici

---

## Plugin Integrati

WaldOT include 3 plugin principali che estendono le sue funzionalità.

### 1. Plugin Generator (waldot-plugin-generator)

**Scopo**: Simula sensori e dispositivi che generano dati dinamici.

#### Algoritmi Disponibili

| Algoritmo | Comportamento | Caso d'Uso |
|-----------|---------------|------------|
| `incremental` | Incremento lineare | Contatori, timer |
| `decremental` | Decremento lineare | Countdown |
| `random` | Valori casuali | Sensori rumorosi |
| `sinusoidal` | Onda sinusoidale | Temperatura ciclica, AC |
| `triangular` | Onda triangolare | PWM, sawtooth |
| `stopped` | Valore costante | Pausa simulazione |

#### Proprietà Configurabili

| Proprietà | Tipo | Default | Descrizione |
|-----------|------|---------|-------------|
| `Algorithm` | String | `incremental` | Algoritmo di generazione |
| `Delay` | Long | `1000` | Intervallo aggiornamento (ms) |
| `Min` | Long | `0` | Valore minimo |
| `Max` | Long | `20000` | Valore massimo |
| `data` | Double | (random) | Valore generato (read-only) |

#### Esempio Pratico: Sala Macchine

```groovy
// Temperatura ambiente - ciclo giorno/notte
ambientTemp = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'temp-ambiente')
  .property('Algorithm', 'sinusoidal')
  .property('Min', '18')
  .property('Max', '26')
  .property('Delay', '60000')  // Ogni minuto
  .next()

// Pressione - leggere variazioni casuali
pressure = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'pressione')
  .property('Algorithm', 'random')
  .property('Min', '950')  // hPa
  .property('Max', '1050')
  .property('Delay', '10000')
  .next()

// Contatore ore macchina
machineHours = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'ore-macchina')
  .property('Algorithm', 'incremental')
  .property('Min', '0')
  .property('Max', '100000')
  .property('Delay', '3600000')  // +1 ogni ora
  .next()

// Velocità rotazione (variabile)
rotationSpeed = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'velocita-rpm')
  .property('Algorithm', 'triangular')
  .property('Min', '0')
  .property('Max', '3000')
  .property('Delay', '500')  // Aggiorna 2 volte/sec
  .next()

log.info("Sensori sala macchine configurati")
```

#### Cambiare Algoritmo a Runtime

Puoi modificare il comportamento tramite client OPC UA scrivendo la proprietà `Algorithm`:

```groovy
// Da Gremlin Console o altra connessione
tempSensor = g.V().has('label', 'temp-ambiente').next()
tempSensor.property('Algorithm', 'random')  // Cambia a random
tempSensor.property('Delay', '2000')        // Più veloce
```

### 2. Plugin Rules Engine (waldot-plugin-rules-engine)

**Scopo**: Implementare logica IF-THEN-THAT per automazione e monitoraggio.

#### Componenti Principali

**RuleVertex**: Definisce la regola
- `Condition`: Espressione JEXL che ritorna boolean (IF)
- `Action`: Espressione JEXL da eseguire (THEN)
- `Priority`: Priorità esecuzione (più alto = prima)
- `Hysteresis`: Deduplica eventi (ms)

**ComputeVertex**: Gestisce l'esecuzione
- `Threads`: Numero thread concorrenti
- `execution-timeout-ms`: Timeout azione (ms)
- `Factor`: Moltiplicatore priorità

#### Variabili JEXL Disponibili

Nelle espressioni `Condition` e `Action` puoi usare:

| Variabile | Tipo | Descrizione |
|-----------|------|-------------|
| `log` | Logger | Logger SLF4J per messaggi |
| `g` | GraphTraversal | Query Gremlin |
| `graph` | Graph | Istanza grafo TinkerPop |
| `commands` | CommandsFunction | Comandi console WaldOT |
| `self` | RuleVertex | Riferimento a questa regola |
| `Math` | Math | Funzioni matematiche Java |
| `random` | ThreadLocalRandom | Numeri casuali |

Inoltre, tutte le proprietà del nodo monitorato sono disponibili come variabili.

#### Esempio 1: Allarme Temperatura

```groovy
// Compute
compute = g.addV('compute')
  .property('type', 'compute')
  .property('label', 'compute-allarmi')
  .property('Threads', '2')
  .next()

// Sensore
tempSensor = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'sensore-temp')
  .property('Algorithm', 'random')
  .property('Min', '15')
  .property('Max', '35')
  .property('Delay', '3000')
  .next()

// Regola: Temperatura troppo alta
highTempRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'temp-alta')
  .property('Condition', 'temperature > 30.0')
  .property('Action', "log.error('ALLARME: Temperatura ' + temperature + '°C supera soglia!'); self.property('last_alarm', System.currentTimeMillis())")
  .property('Priority', '100')
  .property('Hysteresis', '5000')  // Max 1 allarme ogni 5 sec
  .next()

// Regola: Temperatura troppo bassa
lowTempRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'temp-bassa')
  .property('Condition', 'temperature < 18.0')
  .property('Action', "log.warn('ATTENZIONE: Temperatura bassa ' + temperature + '°C')")
  .property('Priority', '80')
  .property('Hysteresis', '10000')
  .next()

// Connetti al compute
highTempRule.addEdge('execute', compute, 'Priority', '100')
lowTempRule.addEdge('execute', compute, 'Priority', '80')

// Attiva monitoraggio
tempSensor.addEdge('fire', highTempRule,
  'monitor-property', 'temperature',
  'active', 'true'
)
tempSensor.addEdge('fire', lowTempRule,
  'monitor-property', 'temperature',
  'active', 'true'
)
```

#### Esempio 2: Condizioni Multiple

```groovy
// Regola con AND logico
criticalRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'stato-critico')
  .property('Condition', 'temperature > 30.0 && pressure > 1020')
  .property('Action', "log.error('STATO CRITICO: Temp=' + temperature + ' Pressione=' + pressure)")
  .property('Priority', '200')  // Massima priorità
  .next()

// Connetti e monitora entrambe le proprietà
criticalRule.addEdge('execute', compute, 'Priority', '200')
tempSensor.addEdge('fire', criticalRule, 'monitor-property', 'temperature', 'active', 'true')
pressureSensor.addEdge('fire', criticalRule, 'monitor-property', 'pressure', 'active', 'true')
```

#### Esempio 3: Query Grafo nelle Regole

```groovy
// Regola che conta sensori in errore
multiFailRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'multi-failure')
  .property('Condition', "g.V().has('type', 'generator').has('status', 'ERROR').count().next() > 3")
  .property('Action', "var count = g.V().has('type', 'generator').has('status', 'ERROR').count().next(); log.error('ALLARME SISTEMA: ' + count + ' sensori in errore!')")
  .property('Priority', '150')
  .next()

multiFailRule.addEdge('execute', compute, 'Priority', '150')
// Questa regola si attiva su qualsiasi cambio di proprietà
anySensor.addEdge('fire', multiFailRule, 'monitor-property', 'status', 'active', 'true')
```

#### Debug delle Regole

Attiva il debug per vedere l'esecuzione:

```groovy
rule.property('Debug', '1')  // Eventi OPC UA di debug
rule.property('Debug', '2')  // Eventi + log dettagliati
```

Controlla le statistiche:

```groovy
stats = g.V().has('label', 'temp-alta').valueMap('Queue', 'Total', 'Executed', 'Errors').next()
// Queue: Eventi in coda
// Total: Eventi totali ricevuti
// Executed: Azioni eseguite
// Errors: Errori durante esecuzione
```

### 3. Plugin TinkerPop (waldot-plugin-tinkerpop)

**Scopo**: Abilitare l'accesso al grafo WaldOT da client TinkerPop esterni.

#### Creazione Server Gremlin

```groovy
gremlinServer = g.addV('gremlin')
  .property('type', 'gremlin')
  .property('label', 'server-gremlin')
  .property('Port', '8182')
  .property('Bind', '0.0.0.0')
  .next()
```

#### Proprietà Configurabili

| Proprietà | Tipo | Default | Descrizione |
|-----------|------|---------|-------------|
| `Port` | Integer | `1025` | Porta Gremlin Server |
| `Bind` | String | `0.0.0.0` | Indirizzo bind |
| `Status` | String | - | Stato server (read-only) |

#### Client Supportati

1. **Gremlin Console** (Groovy interattivo)
2. **Graph-Explorer** (UI web per visualizzazione)
3. **Driver TinkerPop** (Java, Python, JavaScript, .NET, Go)
4. **Apache Zeppelin** (Notebook con interprete Gremlin)

#### Esempio: Connessione da Gremlin Console

```bash
# Scarica Gremlin Console
wget https://dlcdn.apache.org/tinkerpop/3.7.3/apache-tinkerpop-gremlin-console-3.7.3-bin.zip
unzip apache-tinkerpop-gremlin-console-3.7.3-bin.zip
cd apache-tinkerpop-gremlin-console-3.7.3

# Avvia console
bin/gremlin.sh

# Connetti a WaldOT
:remote connect tinkerpop.server conf/remote.yaml session
:remote console

# Ora puoi eseguire query
g.V().count()
g.V().has('type', 'generator').values('label').toList()
```

#### Esempio: Visualizzazione con Graph-Explorer

1. Apri https://graphexp.io/ nel browser
2. Inserisci WebSocket URL: `ws://localhost:8182/gremlin`
3. Clicca "Connect"
4. Esplora il grafo visualmente!

#### Esempio: Multi-Server

```groovy
// Server pubblico per esterni
publicServer = g.addV('gremlin')
  .property('type', 'gremlin')
  .property('label', 'gremlin-pubblico')
  .property('Port', '8182')
  .property('Bind', '0.0.0.0')
  .next()

// Server interno solo localhost
internalServer = g.addV('gremlin')
  .property('type', 'gremlin')
  .property('label', 'gremlin-interno')
  .property('Port', '8183')
  .property('Bind', '127.0.0.1')
  .next()
```

#### Query da Python

```python
from gremlin_python.driver import client, serializer

# Connetti a WaldOT
gremlin_client = client.Client(
    'ws://localhost:8182/gremlin',
    'g',
    message_serializer=serializer.GraphSONSerializersV3d0()
)

# Esegui query
results = gremlin_client.submit('g.V().count()').all().result()
print(f"Vertici totali: {results[0]}")

# Query più complessa
generators = gremlin_client.submit(
    "g.V().has('type', 'generator').valueMap('label', 'Algorithm')"
).all().result()

for gen in generators:
    print(f"Generator: {gen}")
```

---

## Connessione con Client OPC UA

### Client OPC UA Consigliati

#### 1. UaExpert (Windows)

**Download**: https://www.unified-automation.com/products/development-tools/uaexpert.html

**Connessione**:
1. Scarica e installa UaExpert
2. Apri UaExpert
3. Menu: **Server → Add**
4. Seleziona **Custom Discovery**
5. Inserisci URL: `opc.tcp://localhost:12686/waldot`
6. Clicca **OK**
7. Trascina il server su **Project**
8. Espandi l'albero per navigare i nodi

**Visualizzare Dati Live**:
- Trascina una variabile sul pannello **Data Access View**
- Vedrai il valore aggiornarsi in tempo reale

#### 2. Prosys OPC UA Browser (Cross-platform)

**Download**: https://www.prosysopc.com/products/opc-ua-browser/

**Connessione**:
1. Scarica e installa
2. Avvia Prosys Browser
3. Menu: **Connection → Connect to server**
4. URL: `opc.tcp://localhost:12686/waldot`
5. Security Mode: seleziona in base alla configurazione
6. Click **Connect**

#### 3. opcua-client-gui (Python, Open Source)

```bash
# Installa
pip install opcua-client

# Avvia GUI
opcua-client --endpoint opc.tcp://localhost:12686/waldot
```

### Struttura Address Space

Dopo la connessione, troverai questa struttura:

```
Objects
└── Gremlin Engine (root node configurabile)
    ├── Administration (asset root)
    │   └── [Asset nodes...]
    ├── Commands (interface root)
    │   ├── about
    │   ├── exec
    │   ├── help
    │   └── query (Gremlin)
    └── [Vertici del grafo]
        ├── generator nodes
        ├── rule nodes
        ├── compute nodes
        └── ...
```

### Leggere Valori

**Da UaExpert**:
1. Naviga fino al nodo desiderato (es. sensore-temperatura-ufficio)
2. Espandi il nodo
3. Trova la proprietà `data` (per generator) o altre proprietà
4. Clicca destro → **Add to Data Access View**
5. Vedi il valore che si aggiorna

**Esempio via Python opcua**:

```python
from opcua import Client

# Connetti
client = Client("opc.tcp://localhost:12686/waldot")
client.connect()

# Naviga
root = client.get_root_node()
objects = client.get_objects_node()

# Trova un nodo per browse name
children = objects.get_children()
for child in children:
    print(child.get_browse_name())

# Leggi valore di un generatore (esempio)
# Devi conoscere il NodeId o navigare fino al nodo
temp_node = client.get_node("ns=1;s=sensore-temperatura-ufficio")
value = temp_node.get_value()
print(f"Temperatura: {value}")

client.disconnect()
```

### Scrivere Valori

Puoi modificare le proprietà dei nodi tramite client OPC UA:

**Da UaExpert**:
1. Naviga fino alla proprietà
2. Clicca destro → **Write**
3. Inserisci nuovo valore
4. Clicca **Write**

**Esempio: Cambiare algoritmo generator**:
1. Trova nodo `sensore-temperatura-ufficio`
2. Trova proprietà `Algorithm`
3. Scrivi nuovo valore: `"random"`
4. Il generatore cambia comportamento immediatamente!

### Eseguire Comandi OPC UA

WaldOT espone metodi eseguibili sotto `Commands`:

**about**: Mostra informazioni sul server
**help**: Lista comandi disponibili
**query**: Esegue query Gremlin

**Esempio: Eseguire query Gremlin da UaExpert**:
1. Naviga a `Objects/Gremlin Engine/Commands/query`
2. Clicca destro sul metodo → **Call Method**
3. Inserisci la query Gremlin come parametro: `g.V().count()`
4. Clicca **Call**
5. Vedi il risultato nell'output

### Sottoscrizioni e Monitoraggio

I client OPC UA possono creare subscription per essere notificati dei cambiamenti:

**Da UaExpert**:
1. Aggiungi nodo a **Data Access View**
2. Clicca destro → **Subscribe**
3. Configura sampling interval (es. 1000ms)
4. Ricevi notifiche automatiche quando il valore cambia

---

## Esempi Pratici

### Esempio Completo 1: Impianto di Climatizzazione

**Scenario**: Monitorare temperatura e umidità di 3 stanze con automazione.

**File: boot.conf**

```groovy
// ===================================
// IMPIANTO CLIMATIZZAZIONE
// ===================================

// Compute centrale
hvacCompute = g.addV('compute')
  .property('type', 'compute')
  .property('label', 'hvac-compute')
  .property('Threads', '4')
  .next()

// Funzione per creare stanza
def creaStanza(nome, tempTarget) {
  // Sensore temperatura
  temp = g.addV('generator')
    .property('type', 'generator')
    .property('label', "temp-${nome}")
    .property('Algorithm', 'random')
    .property('Min', '18')
    .property('Max', '28')
    .property('Delay', '5000')
    .next()
  
  // Sensore umidità
  humidity = g.addV('generator')
    .property('type', 'generator')
    .property('label', "humidity-${nome}")
    .property('Algorithm', 'sinusoidal')
    .property('Min', '30')  // %
    .property('Max', '70')
    .property('Delay', '8000')
    .next()
  
  // Regola: Raffrescamento necessario
  coolRule = g.addV('rule')
    .property('type', 'rule')
    .property('label', "cool-${nome}")
    .property('Condition', "temperature > ${tempTarget + 2}")
    .property('Action', "log.info('${nome}: Attivo raffrescamento (temp=' + temperature + '°C)'); self.property('hvac_mode', 'COOLING')")
    .property('Priority', '100')
    .property('Hysteresis', '10000')
    .next()
  
  // Regola: Riscaldamento necessario
  heatRule = g.addV('rule')
    .property('type', 'rule')
    .property('label', "heat-${nome}")
    .property('Condition', "temperature < ${tempTarget - 2}")
    .property('Action', "log.info('${nome}: Attivo riscaldamento (temp=' + temperature + '°C)'); self.property('hvac_mode', 'HEATING')")
    .property('Priority', '100')
    .property('Hysteresis', '10000')
    .next()
  
  // Regola: Deumidificazione
  dehumRule = g.addV('rule')
    .property('type', 'rule')
    .property('label', "dehum-${nome}")
    .property('Condition', 'humidity > 65')
    .property('Action', "log.warn('${nome}: Umidità alta ' + humidity + '%'); self.property('hvac_mode', 'DEHUMIDIFY')")
    .property('Priority', '80')
    .property('Hysteresis', '20000')
    .next()
  
  // Connetti regole a compute
  coolRule.addEdge('execute', hvacCompute, 'Priority', '100')
  heatRule.addEdge('execute', hvacCompute, 'Priority', '100')
  dehumRule.addEdge('execute', hvacCompute, 'Priority', '80')
  
  // Attiva monitoraggio
  temp.addEdge('fire', coolRule, 'monitor-property', 'temperature', 'active', 'true')
  temp.addEdge('fire', heatRule, 'monitor-property', 'temperature', 'active', 'true')
  humidity.addEdge('fire', dehumRule, 'monitor-property', 'humidity', 'active', 'true')
  
  log.info("Stanza ${nome} configurata (target ${tempTarget}°C)")
}

// Crea 3 stanze
creaStanza('ufficio', 22)
creaStanza('sala-riunioni', 20)
creaStanza('magazzino', 18)

// Abilita Gremlin Server per monitoring esterno
gremlin = g.addV('gremlin')
  .property('type', 'gremlin')
  .property('Port', '8182')
  .property('Bind', '0.0.0.0')
  .next()

log.info("Sistema HVAC attivo. Gremlin Server su porta 8182")
```

**Docker Compose**:

```yaml
version: '3.8'

services:
  waldot-hvac:
    image: rossonet/waldot:latest
    container_name: hvac-monitoring
    
    ports:
      - "12686:12686"  # OPC UA
      - "8182:8182"    # Gremlin
    
    environment:
      - WALDOT_APPLICATION_NAME=Sistema HVAC
      - WALDOT_PRODUCT_NAME=WaldOT HVAC Control
    
    volumes:
      - ./boot-hvac.conf:/waldot/boot.conf:ro
      - waldot-hvac-security:/app/.security
    
    restart: unless-stopped

volumes:
  waldot-hvac-security:
```

**Test**:
1. `docker-compose up -d`
2. Connetti con UaExpert a `opc.tcp://localhost:12686/waldot`
3. Naviga fino ai nodi temp-ufficio, temp-sala-riunioni, ecc.
4. Vedi i valori che cambiano e le regole che si attivano nei log
5. Apri Graph-Explorer su `ws://localhost:8182/gremlin` per visualizzare

### Esempio Completo 2: Monitoraggio Produzione

**Scenario**: Linea produzione con contatori e analisi qualità.

**File: boot-production.conf**

```groovy
// ===================================
// LINEA PRODUZIONE CON ANALISI QUALITA
// ===================================

// Compute
prodCompute = g.addV('compute')
  .property('type', 'compute')
  .property('label', 'production-compute')
  .property('Threads', '6')
  .next()

// === CONTATORI ===

// Pezzi prodotti totali
totalProduced = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'pezzi-totali')
  .property('Algorithm', 'incremental')
  .property('Min', '0')
  .property('Max', '1000000')
  .property('Delay', '3000')  // +1 ogni 3 secondi
  .next()

// Pezzi conformi
goodPieces = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'pezzi-conformi')
  .property('Algorithm', 'incremental')
  .property('Min', '0')
  .property('Max', '1000000')
  .property('Delay', '3100')  // Poco più lento
  .next()

// Pezzi scartati
defectPieces = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'pezzi-scartati')
  .property('Algorithm', 'incremental')
  .property('Min', '0')
  .property('Max', '50000')
  .property('Delay', '30000')  // +1 ogni 30 secondi (pochi scarti)
  .next()

// === SENSORI PROCESSO ===

// Temperatura stampo
moldTemp = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'temp-stampo')
  .property('Algorithm', 'sinusoidal')
  .property('Min', '180')  // °C
  .property('Max', '220')
  .property('Delay', '2000')
  .next()

// Pressione pressa
pressure = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'pressione-pressa')
  .property('Algorithm', 'random')
  .property('Min', '80')  // bar
  .property('Max', '120')
  .property('Delay', '1000')
  .next()

// Velocità ciclo (pezzi/ora)
cycleSpeed = g.addV('generator')
  .property('type', 'generator')
  .property('label', 'velocita-ciclo')
  .property('Algorithm', 'triangular')
  .property('Min', '100')
  .property('Max', '200')
  .property('Delay', '5000')
  .next()

// === REGOLE QUALITA ===

// Calcola tasso di scarto
defectRateRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'calcolo-tasso-scarto')
  .property('Condition', "defects > 0 && total > 0")
  .property('Action', """
    var total = g.V().has('label', 'pezzi-totali').next().property('data').value();
    var defects = g.V().has('label', 'pezzi-scartati').next().property('data').value();
    var rate = (defects / total) * 100;
    log.info('Tasso scarto: ' + rate.toFixed(2) + '%');
    self.property('defect_rate', rate);
  """)
  .property('Priority', '50')
  .property('Hysteresis', '60000')  // Calcola ogni minuto
  .next()

// Allarme: Troppi scarti
highDefectRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'allarme-scarti-alti')
  .property('Condition', """
    var rate = g.V().has('label', 'calcolo-tasso-scarto').next().property('defect_rate').orElse(0.0);
    return rate > 5.0;
  """)
  .property('Action', """
    var rate = g.V().has('label', 'calcolo-tasso-scarto').next().property('defect_rate').value();
    log.error('ALLARME QUALITA: Tasso scarto ' + rate.toFixed(2) + '% supera limite 5%!');
    log.error('AZIONE: Verifica parametri processo');
  """)
  .property('Priority', '200')
  .property('Hysteresis', '30000')
  .next()

// Temperatura fuori range
tempOutOfRangeRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'temp-fuori-range')
  .property('Condition', 'temperature < 185 || temperature > 215')
  .property('Action', "log.warn('Temperatura stampo fuori range ottimale: ' + temperature + '°C')")
  .property('Priority', '100')
  .property('Hysteresis', '15000')
  .next()

// Pressione anomala
pressureAnomalyRule = g.addV('rule')
  .property('type', 'rule')
  .property('label', 'pressione-anomala')
  .property('Condition', 'pressure < 85 || pressure > 115')
  .property('Action', "log.warn('Pressione anomala: ' + pressure + ' bar')")
  .property('Priority', '100')
  .property('Hysteresis', '10000')
  .next()

// === CONNESSIONI ===

// Connetti regole a compute
defectRateRule.addEdge('execute', prodCompute, 'Priority', '50')
highDefectRule.addEdge('execute', prodCompute, 'Priority', '200')
tempOutOfRangeRule.addEdge('execute', prodCompute, 'Priority', '100')
pressureAnomalyRule.addEdge('execute', prodCompute, 'Priority', '100')

// Attiva monitoring
defectPieces.addEdge('fire', defectRateRule, 'monitor-property', 'data', 'active', 'true')
totalProduced.addEdge('fire', defectRateRule, 'monitor-property', 'data', 'active', 'true')
defectPieces.addEdge('fire', highDefectRule, 'monitor-property', 'data', 'active', 'true')
moldTemp.addEdge('fire', tempOutOfRangeRule, 'monitor-property', 'temperature', 'active', 'true')
pressure.addEdge('fire', pressureAnomalyRule, 'monitor-property', 'pressure', 'active', 'true')

// Gremlin Server per dashboard esterne
gremlin = g.addV('gremlin')
  .property('type', 'gremlin')
  .property('Port', '8182')
  .property('Bind', '0.0.0.0')
  .next()

log.info("=================================")
log.info("Sistema Monitoraggio Produzione")
log.info("=================================")
log.info("Contatori: pezzi-totali, pezzi-conformi, pezzi-scartati")
log.info("Sensori: temp-stampo, pressione-pressa, velocita-ciclo")
log.info("Regole qualità attive")
log.info("Gremlin Server: porta 8182")
log.info("=================================")
```

**Utilizzo**:
1. Monta questo file come boot.conf
2. Connetti con client OPC UA
3. Crea dashboard che legge i contatori
4. Monitora gli allarmi di qualità
5. Usa Gremlin per analisi: `g.V().has('label', 'calcolo-tasso-scarto').valueMap()`

---

## Risoluzione Problemi

### Container Non Si Avvia

**Sintomo**: `docker ps` non mostra il container

**Diagnosi**:
```bash
docker logs <container-id>
```

**Cause Comuni**:
1. **Porta già in uso**:
   ```bash
   # Verifica
   lsof -i :12686
   # Soluzione
   docker run -e WALDOT_TCP_PORT=4840 -p 4840:4840 rossonet/waldot
   ```

2. **File boot.conf non valido**:
   - Controlla sintassi Groovy
   - Testa localmente con Gremlin Console

3. **Permessi volume**:
   ```bash
   # Verifica permessi
   ls -la /path/to/mount
   # Correggi
   chmod -R 755 /path/to/mount
   ```

### Client OPC UA Non Si Connette

**Sintomo**: "Connection refused" o "Timeout"

**Verifiche**:

1. **Server effettivamente in ascolto**:
   ```bash
   docker exec <container-id> netstat -an | grep 12686
   ```

2. **Firewall**:
   ```bash
   # Linux
   sudo ufw allow 12686/tcp
   
   # Windows
   # Apri Windows Firewall, aggiungi regola in entrata porta 12686
   ```

3. **Docker network**:
   ```bash
   # Verifica bind address
   docker exec <container-id> env | grep WALDOT_BIND
   
   # Deve essere 0.0.0.0 per accesso esterno
   ```

4. **Endpoint URL corretto**:
   - Deve essere: `opc.tcp://localhost:12686/waldot`
   - Nota il `/waldot` finale (configurabile con WALDOT_ENDPOINT_PATH)

### Certificati SSL Invalidi

**Sintomo**: Client rifiuta connessione per certificato non fidato

**Soluzioni**:

1. **Client in modalità No Security**:
   - UaExpert: Security Mode = None
   - Prosys: Security Policy = None

2. **Accetta certificato**:
   - Prima connessione, client chiede conferma
   - Accetta certificato server

3. **Rigenera certificati**:
   ```bash
   docker exec <container-id> rm -rf /app/.security
   docker restart <container-id>
   ```

### Generatori Non Aggiornano

**Sintomo**: Proprietà `data` del generator non cambia

**Diagnosi**:
```groovy
// Da Gremlin Console o query OPC UA
g.V().has('type', 'generator')
  .has('label', 'mio-generatore')
  .valueMap('Algorithm', 'Delay', 'data')
```

**Soluzioni**:

1. **Algoritmo "stopped"**:
   ```groovy
   gen.property('Algorithm', 'random')
   ```

2. **Delay troppo alto**:
   ```groovy
   gen.property('Delay', '1000')  // Aggiorna ogni secondo
   ```

3. **Proprietà non create**:
   - Verifica che type='generator' e properties siano corrette nel boot.conf

### Regole Non Si Attivano

**Sintomo**: Action non viene eseguita nonostante Condition vera

**Diagnosi**:

1. **Abilita debug**:
   ```groovy
   rule.property('Debug', '2')
   ```

2. **Controlla statistiche**:
   ```groovy
   g.V().has('label', 'mia-regola')
     .valueMap('Queue', 'Total', 'Executed', 'Errors')
   ```

**Soluzioni**:

1. **FireMonitoredEdge non attivo**:
   ```groovy
   // Verifica
   edge = sensor.outE('fire').where(inV().has('label', 'mia-regola')).next()
   edge.property('active').value()  // Deve essere 'true'
   
   // Correggi
   edge.property('active', 'true')
   ```

2. **Proprietà monitorata errata**:
   ```groovy
   // Deve corrispondere alla proprietà del source node
   edge.property('monitor-property', 'temperature')  // Corretto
   edge.property('monitor-property', 'temp')         // Sbagliato se prop è "temperature"
   ```

3. **Hysteresis troppo alto**:
   ```groovy
   // Eventi entro hysteresis vengono deduplicate
   rule.property('Hysteresis', '1000')  // Riduci a 1 secondo
   ```

4. **Compute non connesso**:
   ```groovy
   // Verifica edge execute
   g.V().has('label', 'mia-regola')
     .outE('execute')
     .inV()
     .valueMap('label', 'Threads')
   ```

### Performance Scarse

**Sintomo**: Server lento, alta latenza, CPU alta

**Diagnosi**:
```bash
# CPU e memoria
docker stats <container-id>

# Numero vertici
docker exec <container-id> curl -s http://localhost:8182/... # con query
```

**Soluzioni**:

1. **Troppi generatori veloci**:
   ```groovy
   // Trova generatori con Delay < 100ms
   g.V().has('type', 'generator').has('Delay', lt(100)).toList()
   
   // Rallenta
   g.V().has('type', 'generator').has('Delay', lt(100))
     .property('Delay', '1000')
     .iterate()
   ```

2. **Aumenta thread Compute**:
   ```groovy
   g.V().has('type', 'compute').property('Threads', '8').iterate()
   ```

3. **Regole troppo complesse**:
   - Semplifica espressioni JEXL
   - Evita query Gremlin pesanti nelle Action

4. **Limiti risorse Docker**:
   ```yaml
   # In docker-compose.yml
   deploy:
     resources:
       limits:
         cpus: '4.0'
         memory: 4G
   ```

### Log e Debug

**Abilita log dettagliati**:

```bash
docker run \
  -e JAVA_OPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG" \
  -p 12686:12686 \
  rossonet/waldot:latest
```

**Segui log in tempo reale**:
```bash
docker logs -f <container-id>
```

**Salva log su file**:
```bash
docker logs <container-id> > waldot.log 2>&1
```

---

## Risorse Aggiuntive

### Link Utili

- **Docker Hub**: https://hub.docker.com/r/rossonet/waldot
- **GitHub**: https://github.com/rossonet/waldot
- **Documentazione Completa**: https://github.com/rossonet/waldot/tree/master/docs
- **Apache TinkerPop**: https://tinkerpop.apache.org/
- **Eclipse Milo**: https://github.com/eclipse/milo

### Client OPC UA

- **UaExpert**: https://www.unified-automation.com/products/development-tools/uaexpert.html
- **Prosys Browser**: https://www.prosysopc.com/products/opc-ua-browser/
- **opcua-client (Python)**: `pip install opcua-client`

### Strumenti Gremlin

- **Gremlin Console**: https://tinkerpop.apache.org/downloads.html
- **Graph-Explorer**: https://graphexp.io/
- **Apache Zeppelin**: https://zeppelin.apache.org/

### Community

- **Issues**: https://github.com/rossonet/waldot/issues
- **Discussions**: https://github.com/rossonet/waldot/discussions

---

## Prossimi Passi

1. **Esplora i Plugin**: Sperimenta con generator, rules-engine e tinkerpop
2. **Crea Dashboard**: Usa Gremlin Server + Graph-Explorer per visualizzazioni
3. **Integra con Sistemi Esistenti**: Connetti PLC, SCADA tramite OPC UA
4. **Sviluppa Plugin Personalizzati**: Segui la [Guida Plugin](manuale_plugins.md)

---

**Buon lavoro con WaldOT!**

*Manuale a cura di Rossonet s.c.a r.l.*  
*Versione documento: 1.0*  
*Data: 2024*
