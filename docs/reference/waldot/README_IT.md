# WaldOT - Documentazione di Riferimento

## Indice

1. [Panoramica del Progetto](#panoramica-del-progetto)
2. [Architettura Generale](#architettura-generale)
3. [OPC UA + TinkerPop](#opc-ua--tinkerpop)
4. [Sistema a Plugin](#sistema-a-plugin)
5. [Virtual Threads](#virtual-threads)
6. [Plugin Disponibili](#plugin-disponibili)
7. [Casi d'Uso](#casi-duso)

---

## Panoramica del Progetto

### Cos'è WaldOT?

**WaldOT** (Waldorf OT - Operational Technology) è un motore Digital Twin open-source che integra in modo innovativo il mondo dell'automazione industriale (OT) con le moderne tecnologie di analisi dati attraverso database a grafo.

Il progetto crea un ponte bidirezionale tra:
- **OPC UA** (standard industriale per comunicazione OT)
- **Apache TinkerPop** (framework per database a grafo)

### Visione del Progetto

WaldOT rivoluziona il modo in cui i dati industriali vengono rappresentati e analizzati, trasformando gli spazi di indirizzamento OPC UA gerarchici in **grafi viventi** interrogabili in tempo reale con il linguaggio Gremlin.

### Perché WaldOT?

#### Problema
I sistemi industriali tradizionali espongono i dati attraverso OPC UA - un protocollo potente ma gerarchico. Questo rende difficile:
- Trovare correlazioni tra dispositivi distanti nell'albero gerarchico
- Eseguire query complesse su relazioni multiple
- Implementare logiche di business basate su pattern di grafo
- Integrare dati OT con sistemi IT moderni

#### Soluzione
WaldOT rappresenta l'intero spazio di indirizzamento OPC UA come **database a grafo**:

```
OPC UA                          TinkerPop
─────────────────────          ───────────────────
Oggetti         →              Vertici (Vertices)
Riferimenti     →              Archi (Edges)
Variabili       →              Proprietà (Properties)
```

Le modifiche si propagano **in tempo reale in entrambe le direzioni**:
- Modifiche via OPC UA → aggiornano il grafo
- Query Gremlin che modificano il grafo → aggiornano OPC UA

---

## Architettura Generale

### Schema Architetturale

```
┌─────────────────────────────────────────────────────────────────┐
│                        WaldOT Framework                          │
│                                                                  │
│  ┌────────────────┐              ┌─────────────────────┐       │
│  │   OPC UA       │              │   TinkerPop         │       │
│  │   Server       │  ←────────→  │   Graph             │       │
│  │  (Eclipse Milo)│   Bi-Sync    │  (Apache TinkerPop) │       │
│  └────────┬───────┘              └──────────┬──────────┘       │
│           │                                  │                   │
│           └──────────────┬───────────────────┘                  │
│                          ↓                                       │
│           ┌──────────────────────────────┐                     │
│           │      Plugin Manager          │                     │
│           │  - Auto-discovery            │                     │
│           │  - Lifecycle management      │                     │
│           │  - Type registration         │                     │
│           └──────────────┬───────────────┘                     │
│                          ↓                                       │
│   ┌──────────────────────────────────────────────────┐         │
│   │                    Plugins                        │         │
│   │  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │         │
│   │  │Generator │  │  Rules   │  │  TinkerPop   │   │         │
│   │  │          │  │  Engine  │  │   Server     │   │         │
│   │  └──────────┘  └──────────┘  └──────────────┘   │         │
│   │         ... altri plugin personalizzati ...      │         │
│   └──────────────────────────────────────────────────┘         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
         ↓                                    ↓
┌─────────────────┐                  ┌─────────────────┐
│  Client OPC UA  │                  │  Client Gremlin │
│  - UaExpert     │                  │  - Console      │
│  - Prosys       │                  │  - Drivers      │
│  - Custom       │                  │  - Graph-Explorer│
└─────────────────┘                  └─────────────────┘
```

### Componenti Principali

#### 1. WaldOT Framework Core

**waldot-api**: API pubbliche per sviluppatori plugin
- Interfaccia `PluginListener`
- Annotazione `@WaldotPlugin`
- Modelli: `WaldotGraph`, `WaldotVertex`, `WaldotEdge`, `WaldotNamespace`

**waldot-namespace**: Implementazione dello spazio dei nomi OPC UA
- Gestione nodi OPC UA
- Sincronizzazione bidirezionale OPC UA ↔ Grafo
- Gestione eventi e proprietà

**waldot-app**: Applicazione principale
- Bootstrap del server
- Caricamento plugin
- Configurazione

#### 2. OPC UA Server (Eclipse Milo)

Server OPC UA completo che:
- Espone l'address space secondo lo standard OPC UA
- Gestisce connessioni client
- Pubblica eventi
- Supporta sottoscrizioni (subscriptions)

#### 3. TinkerPop Graph

Database a grafo che:
- Memorizza vertici, archi e proprietà
- Supporta query Gremlin
- Può usare diversi storage backend (in-memory, RocksDB, Neo4j, ecc.)

#### 4. Plugin Manager

Sistema di gestione plugin che:
- Scansiona il classpath per annotazioni `@WaldotPlugin`
- Inizializza i plugin in ordine
- Gestisce il ciclo di vita (initialize → start → stop → close)
- Registra tipi custom nell'address space OPC UA

---

## OPC UA + TinkerPop

### Mappatura Concettuale

#### Oggetti OPC UA → Vertici TinkerPop

Un oggetto OPC UA diventa un vertice nel grafo:

**OPC UA**:
```
ObjectNode {
  NodeId: "ns=2;s=Motor1"
  BrowseName: "Motor1"
  DisplayName: "Production Motor 1"
}
```

**TinkerPop**:
```groovy
Vertex {
  id: "ns=2;s=Motor1"
  label: "motor"
  properties: {
    "name": "Production Motor 1",
    "rpm": 1500,
    "temperature": 45.5
  }
}
```

#### Riferimenti OPC UA → Archi TinkerPop

I riferimenti OPC UA diventano archi nel grafo:

**OPC UA**:
```
ProductionLine --[Contains]--> Motor1
Motor1 --[HasComponent]--> TemperatureSensor
```

**TinkerPop**:
```groovy
productionLine --[contains]--> motor1
motor1 --[hasComponent]--> temperatureSensor
```

#### Variabili OPC UA → Proprietà Vertici

Le variabili OPC UA diventano proprietà dei vertici:

**OPC UA**:
```
Motor1/RPM = 1500 (UInt16)
Motor1/Temperature = 45.5 (Double)
Motor1/Status = "RUNNING" (String)
```

**TinkerPop**:
```groovy
motor1.property("rpm", 1500)
motor1.property("temperature", 45.5)
motor1.property("status", "RUNNING")
```

### Sincronizzazione Bidirezionale

#### Da OPC UA al Grafo

Quando un client OPC UA scrive una variabile:

```
1. Client OPC UA: Write Motor1/Temperature = 50.0
2. WaldOT rileva la modifica
3. Aggiorna la proprietà del vertice: motor1.property("temperature", 50.0)
4. Propaga ai listener del grafo
```

#### Dal Grafo a OPC UA

Quando viene eseguita una query Gremlin che modifica il grafo:

```groovy
1. Gremlin: g.V().has('id', 'Motor1').property('temperature', 55.0)
2. WaldOT rileva la modifica
3. Aggiorna la variabile OPC UA: Motor1/Temperature = 55.0
4. Notifica ai client OPC UA sottoscritti
```

### Vantaggi della Doppia Vista

#### Per Ingegneri OT
- Usano gli strumenti OPC UA familiari (UaExpert, Prosys)
- Configurano dispositivi via OPC UA standard
- Monitorano in tempo reale con sottoscrizioni OPC UA

#### Per Data Scientist / Sviluppatori IT
- Query complesse con Gremlin
- Analisi delle correlazioni con algoritmi di grafo
- Integrazione con framework moderni (Spring, GraphQL, REST)

---

## Sistema a Plugin

### Filosofia dei Plugin

WaldOT è progettato come un **framework estensibile** dove i plugin sono cittadini di prima classe. Ogni plugin può:

1. **Registrare nuovi tipi di vertici** nell'address space OPC UA
2. **Implementare comportamenti custom** per i vertici
3. **Integrare sorgenti dati esterne** (sensori, API, database)
4. **Fornire comandi** eseguibili via console o OPC UA

### Anatomia di un Plugin

Un plugin WaldOT è composto da:

```
my-plugin/
├── src/main/java/
│   └── net/rossonet/waldot/myplugin/
│       ├── MyPlugin.java              # Classe principale @WaldotPlugin
│       ├── MyCustomVertex.java        # Implementazione vertice custom
│       └── MyCustomEdge.java          # (opzionale) Arco custom
├── src/main/resources/
│   └── META-INF/
│       └── services/                   # Service loader (opzionale)
└── build.gradle                        # Dipendenze plugin
```

#### Classe Plugin Principale

```java
@WaldotPlugin  // Annotazione per auto-discovery
public class MyPlugin implements AutoCloseable, PluginListener {
    
    private WaldotNamespace waldotNamespace;
    private UaObjectTypeNode myTypeNode;
    
    @Override
    public void initialize(WaldotNamespace waldotNamespace) {
        // 1. Salvare riferimento al namespace
        this.waldotNamespace = waldotNamespace;
        
        // 2. Creare nodi tipo OPC UA
        createTypeNodes();
        
        // 3. Registrare comandi (opzionale)
        registerCommands();
    }
    
    @Override
    public WaldotVertex createVertex(...) {
        // Factory method per creare vertici custom
        if (myTypeNode.getNodeId().equals(typeDefinitionNodeId)) {
            return new MyCustomVertex(...);
        }
        return null;
    }
    
    @Override
    public void start() {
        // Avviare task in background, monitoraggio, ecc.
    }
    
    @Override
    public void stop() {
        // Fermare task in background
    }
    
    @Override
    public void close() throws Exception {
        // Pulizia risorse
    }
}
```

### Registrazione Tipi OPC UA

I plugin registrano nuovi tipi di oggetti nell'address space OPC UA:

```java
private void createTypeNodes() {
    // Creare ObjectType node
    myTypeNode = UaObjectTypeNode.builder(waldotNamespace.getOpcUaNodeContext())
        .setNodeId(waldotNamespace.generateNodeId("ObjectTypes/MyCustomType"))
        .setBrowseName(waldotNamespace.generateQualifiedName("MyCustomType"))
        .setDisplayName(LocalizedText.english("My Custom Device"))
        .setIsAbstract(false)
        .build();
    
    // Aggiungere proprietà al tipo
    PluginListener.addParameterToTypeNode(
        waldotNamespace, 
        myTypeNode, 
        "value",      // Nome proprietà
        NodeIds.Double  // Tipo OPC UA
    );
    
    // Registrare nell'address space
    waldotNamespace.getStorageManager().addNode(myTypeNode);
    myTypeNode.addReference(new Reference(
        myTypeNode.getNodeId(),
        NodeIds.HasSubtype,
        NodeIds.BaseObjectType.expanded(),
        false
    ));
    
    // Registrare costruttore
    waldotNamespace.getObjectTypeManager().registerObjectType(
        myTypeNode.getNodeId(),
        UaObjectNode.class,
        PluginListener.objectNodeConstructor
    );
}
```

### Ciclo di Vita Plugin

```
┌──────────────────────────────────────────────────────────┐
│ 1. DISCOVERY                                              │
│    WaldOT scansiona classpath cercando @WaldotPlugin    │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│ 2. INITIALIZATION                                         │
│    plugin.initialize(waldotNamespace)                    │
│    - Crea nodi tipo OPC UA                                │
│    - Registra comandi                                     │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│ 3. START                                                  │
│    plugin.start()                                         │
│    - Avvia thread in background                           │
│    - Inizia monitoraggio                                  │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│ 4. RUNTIME                                                │
│    - Crea vertici su richiesta                            │
│    - Gestisce eventi                                      │
│    - Esegue comandi                                       │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│ 5. STOP                                                   │
│    plugin.stop()                                          │
│    - Ferma thread in background                           │
│    - Pausa operazioni                                     │
└────────────────────┬─────────────────────────────────────┘
                     ↓
┌──────────────────────────────────────────────────────────┐
│ 6. CLOSE                                                  │
│    plugin.close()                                         │
│    - Rilascia risorse                                     │
│    - Chiude connessioni                                   │
└──────────────────────────────────────────────────────────┘
```

---

## Virtual Threads

### Cos'è un Virtual Thread?

I **virtual thread** (introdotti in Java 21) sono thread leggeri gestiti dalla JVM anziché dal sistema operativo.

#### Confronto: Platform Thread vs Virtual Thread

| Caratteristica | Platform Thread | Virtual Thread |
|---|---|---|
| **Dimensione stack** | ~1 MB | ~1 KB |
| **Numero massimo** | Migliaia | Milioni |
| **Creazione** | Costosa (~1ms) | Economica (~1μs) |
| **Blocking I/O** | Blocca OS thread | Non blocca OS thread |
| **Scheduling** | Sistema operativo | JVM |

### Perché Virtual Threads in WaldOT?

WaldOT crea **oggetti attivi** che operano continuamente:
- Generatori che simulano sensori
- Monitor che interrogano API esterne
- Processori di regole che reagiscono a eventi

**Senza virtual threads**:
```java
// 10,000 sensori = 10,000 platform threads = ~10 GB RAM ❌
for (int i = 0; i < 10000; i++) {
    new Thread(() -> monitorSensor(i)).start();  // PESSIMO!
}
```

**Con virtual threads**:
```java
// 10,000 sensori = 10,000 virtual threads = ~10 MB RAM ✅
ExecutorService executor = ThreadHelper.newVirtualThreadExecutor();
for (int i = 0; i < 10000; i++) {
    executor.submit(() -> monitorSensor(i));  // OTTIMO!
}
```

### Pattern di Utilizzo

#### Pattern 1: Loop di Polling

```java
public class SensorVertex extends WaldotVertex {
    private final ExecutorService executor;
    private volatile boolean active = true;
    
    public SensorVertex(ExecutorService executor, ...) {
        super(...);
        this.executor = executor;
        
        // Avvia polling in virtual thread
        executor.submit(this::pollingSensorLoop);
    }
    
    private void pollingSensorLoop() {
        while (active) {
            try {
                // Operazione bloccante - OK in virtual thread
                SensorData data = httpClient.get(sensorUrl).block();
                
                // Aggiorna proprietà (sync automatico a OPC UA)
                property("temperature", data.getTemperature());
                property("humidity", data.getHumidity());
                
                // Sleep - OK in virtual thread (non blocca OS thread)
                Thread.sleep(5000);
                
            } catch (InterruptedException e) {
                active = false;
            } catch (Exception e) {
                logger.error("Errore polling sensore", e);
            }
        }
    }
}
```

#### Pattern 2: Event-Driven

```java
public class MqttBridgeVertex extends WaldotVertex {
    private final MqttClient mqttClient;
    
    public MqttBridgeVertex(...) {
        super(...);
        
        // Connessione MQTT (blocking I/O - OK in virtual thread)
        mqttClient = new MqttClient(broker, clientId);
        mqttClient.connect();
        
        // Callback eseguito in virtual thread automaticamente
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // Aggiorna grafo
                JSONObject json = new JSONObject(new String(message.getPayload()));
                json.keys().forEachRemaining(key -> {
                    property(key, json.get(key));
                });
            }
        });
    }
}
```

#### Pattern 3: Generatore Continuo

```java
public class DataGeneratorVertex extends WaldotVertex {
    private volatile boolean active = true;
    
    public DataGeneratorVertex(ExecutorService executor, ...) {
        super(...);
        executor.submit(this::generateLoop);
    }
    
    private void generateLoop() {
        double value = 0.0;
        while (active) {
            try {
                // Genera valore
                value = Math.sin(System.currentTimeMillis() / 1000.0) * 50 + 50;
                
                // Aggiorna (sync a OPC UA)
                property("data", value);
                
                // Pausa
                Thread.sleep(delayMs);
                
            } catch (InterruptedException e) {
                active = false;
            }
        }
    }
}
```

### Best Practices

#### ✅ DA FARE

1. **Usare virtual thread per I/O bloccante**
```java
executor.submit(() -> {
    String data = httpClient.get(url).block();  // OK!
    processData(data);
});
```

2. **Creare quanti virtual thread servono**
```java
// Va benissimo creare 100,000 virtual threads
for (int i = 0; i < 100000; i++) {
    executor.submit(() -> doWork());
}
```

3. **Usare sleep() tranquillamente**
```java
while (active) {
    doWork();
    Thread.sleep(1000);  // OK in virtual thread!
}
```

#### ❌ NON FARE

1. **NON usare pool a dimensione fissa per virtual threads**
```java
// SBAGLIATO!
ExecutorService executor = Executors.newFixedThreadPool(10);
```

2. **NON usare virtual thread per lavoro CPU-intensive**
```java
// SBAGLIATO - usa platform thread per CPU-intensive
executor.submit(() -> {
    complexMathCalculation();  // Occupa CPU a lungo
});
```

3. **NON dimenticare di gestire interruzioni**
```java
// SBAGLIATO
while (true) {
    doWork();
    Thread.sleep(1000);  // Se interrotto, eccezione ignorata
}

// GIUSTO
while (active) {
    try {
        doWork();
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        active = false;  // Esci pulitamente
    }
}
```

---

## Plugin Disponibili

### 1. waldot-plugin-generator

**Scopo**: Simulare dati dinamici per test e sviluppo

#### Funzionalità
- 6 algoritmi di generazione: incrementale, decrementale, random, sinusoidale, triangolare, fermo
- Intervalli di aggiornamento configurabili (10ms - ore)
- Migliaia di generatori concorrenti con virtual thread
- Sincronizzazione OPC UA completa

#### Esempio d'Uso

```groovy
// Simulatore di sensore di temperatura
tempSensor = graph.addVertex(
    "type", "generator",
    "label", "ufficio-temperatura",
    "Algorithm", "sinusoidal",
    "Min", "18",
    "Max", "26",
    "Delay", "5000"  // Aggiorna ogni 5 secondi
)

// Leggi valore generato
temperatura = tempSensor.property("data").value()

// Cambia algoritmo a runtime
tempSensor.property("Algorithm", "random")
```

#### Algoritmi

| Algoritmo | Pattern | Caso d'Uso |
|---|---|---|
| `incremental` | Lineare crescente | Contatori, timer |
| `decremental` | Lineare decrescente | Countdown |
| `random` | Valori casuali | Sensori rumorosi |
| `sinusoidal` | Onda sinusoidale | Cicli temperatura, AC |
| `triangular` | Onda triangolare | Sawtooth, PWM |
| `stopped` | Costante | Pausa simulazione |

#### Architettura

```
DataGeneratorVertex
├── Virtual Thread (loop infinito)
│   └── while (active) {
│         generateValue(algorithm);
│         property("data", value);  // Sync a OPC UA
│         sleep(delay);
│       }
└── Properties
    ├── Algorithm: algoritmo generazione
    ├── Delay: intervallo aggiornamento (ms)
    ├── Min: valore minimo
    ├── Max: valore massimo
    └── data: valore generato (read-only)
```

### 2. waldot-plugin-rules-engine

**Scopo**: Motore di regole event-driven IF-THEN-THAT

#### Funzionalità
- Espressioni JEXL per condizioni e azioni
- Coda a priorità con deduplicazione hysteresis
- Pool di virtual thread per esecuzione concorrente
- Accesso completo al grafo Gremlin nelle regole
- Eventi di debug OPC UA per troubleshooting

#### Architettura

```
[Nodo Sorgente] → [FireMonitoredEdge] → [RuleVertex] → [ComputeMonitoredEdge] → [ComputeVertex]
   (evento)            (filtri)          (accoda)           (instrada)            (esegue)
```

**Componenti**:
- **RuleVertex**: Regola IF-THEN con espressioni JEXL
- **ComputeVertex**: Gestore thread con esecuzione a priorità
- **ComputeMonitoredEdge**: Connette regole a compute per esecuzione
- **FireMonitoredEdge**: Monitora sorgenti e attiva regole

#### Esempio d'Uso

```groovy
// 1. Crea vertice compute
compute = graph.addVertex(
    "type", "compute",
    "label", "main-compute",
    "Threads", "4"
)

// 2. Crea regola
rule = graph.addVertex(
    "type", "rule",
    "label", "allarme-temperatura",
    "Condition", "temperature > 80.0",
    "Action", "log.warn('Allarme temperatura: ' + temperature + '°C')",
    "Priority", "100",
    "Hysteresis", "5000"  // Deduplicazione 5 secondi
)

// 3. Connetti regola a compute
rule.addEdge("execute", compute, "Priority", "100")

// 4. Monitora sensore
tempSensor.addEdge("fire", rule, 
    "monitor-property", "temperature",
    "active", "true"
)

// 5. Test
tempSensor.property("temperature", 85.0)
// Output: WARN Allarme temperatura: 85.0°C
```

#### Variabili JEXL Disponibili

| Variabile | Tipo | Descrizione |
|---|---|---|
| `log` | Logger | Logger SLF4J per logging |
| `g` | GraphTraversal | Gremlin traversal per query |
| `graph` | Graph | Istanza grafo TinkerPop |
| `commands` | CommandsFunction | Comandi console WaldOT |
| `self` | RuleVertex | Riferimento a questa regola |
| `Math` | Math | Funzioni matematiche Java |
| `random` | ThreadLocalRandom | Generatore numeri casuali |

#### Meccanismo Priorità

```
peso = priorità_arco × fattore_priorità + dimensione_coda
```

**Esempio**:
- Regola A: priorità=100, coda=5, fattore=100.0 → peso=10,005
- Regola B: priorità=50, coda=10, fattore=100.0 → peso=5,010

La regola A viene elaborata prima (peso maggiore).

### 3. waldot-plugin-tinkerpop

**Scopo**: Server Gremlin embedded per accesso client remoto

#### Funzionalità
- Server Gremlin embedded come vertice del grafo
- Protocolli: WebSocket + HTTP
- Serializzatori: GraphSON v3 + GraphBinary v1
- Compatibilità con tutti i client TinkerPop standard
- Sincronizzazione live bidirezionale

#### Esempio d'Uso

```groovy
// Crea server Gremlin
gremlinServer = graph.addVertex(
    "type", "gremlin",
    "label", "main-server",
    "Port", "8182",
    "Bind", "0.0.0.0"
)

// Connetti da Gremlin Console
:remote connect tinkerpop.server conf/remote.yaml
:remote console
g.V().count()

// Query da Java
Cluster cluster = Cluster.build()
    .addContactPoint("localhost")
    .port(8182)
    .create();
GraphTraversalSource g = traversal()
    .withRemote(DriverRemoteConnection.using(cluster, "g"));
long count = g.V().count().next();
```

#### Architettura

```
┌────────────────────────────────────┐
│     WaldOT Graph (condiviso)      │
└────────┬───────────────────────────┘
         │
    ┌────┴─────┐
    │          │
┌───▼──┐   ┌──▼────┐
│OPC UA│   │Gremlin│
│Server│   │Server │
└───┬──┘   └───┬───┘
    │          │
┌───▼──┐   ┌──▼────┐
│OPC UA│   │Gremlin│
│Client│   │Client │
└──────┘   └───────┘
```

---

## Casi d'Uso

### 1. Manutenzione Predittiva

**Scenario**: Monitorare equipaggiamento industriale e predire guasti

```groovy
// Equipaggiamento con sensori
equipment = graph.addVertex(
    "type", "equipment",
    "label", "pump-1",
    "vibration", 2.5,
    "temperature", 45.0,
    "pressure", 75.0,
    "runningHours", 1000
)

// Regola: anomalia vibrazione
vibrationRule = graph.addVertex(
    "type", "rule",
    "Condition", "vibration > 5.0",
    "Action", "log.warn('Vibrazione anomala: possibile guasto cuscinetto'); g.V(self).property('maintenanceRequired', true).iterate()"
)

// Regola: combinazione temperatura + pressione
criticalRule = graph.addVertex(
    "type", "rule",
    "Condition", "temperature > 80.0 && pressure > 100.0",
    "Action", "log.error('CRITICO: temperatura E pressione alte. Spegnimento necessario.'); commands.execute('emergency-shutdown', 'pump-1')"
)

// Regola: manutenzione predittiva
predictiveRule = graph.addVertex(
    "type", "rule",
    "Condition", "runningHours > 5000 && (vibration > 4.0 || temperature > 70.0)",
    "Action", "log.info('Manutenzione consigliata per pump-1'); g.V(self).property('scheduleMaintenance', true).iterate()"
)
```

### 2. Smart Building

**Scenario**: Automazione HVAC e illuminazione basata su occupazione

```groovy
// Zone edificio
zone = graph.addVertex(
    "type", "zone",
    "label", "ufficio-1",
    "occupancy", 0,
    "temperature", 22.0,
    "lightLevel", 0,
    "hvacMode", "AUTO"
)

// Regola: accendi luci quando c'è occupazione
lightingRule = graph.addVertex(
    "type", "rule",
    "Condition", "occupancy > 0 && lightLevel < 300",
    "Action", "var zoneName = self.property('source').value(); log.info('Accendo luci in ' + zoneName); g.V(self).property('lightLevel', 800).iterate()"
)

// Regola: spegni luci quando vuoto
lightsOffRule = graph.addVertex(
    "type", "rule",
    "Condition", "occupancy == 0 && lightLevel > 0",
    "Action", "g.V(self).property('lightLevel', 0).iterate()",
    "Hysteresis", "60000"  // Aspetta 1 minuto prima di spegnere
)

// Regola: raffrescamento quando necessario
hvacCoolingRule = graph.addVertex(
    "type", "rule",
    "Condition", "occupancy > 0 && temperature > 24.0 && hvacMode == 'AUTO'",
    "Action", "g.V(self).property('hvacMode', 'COOLING').iterate()"
)
```

### 3. Analisi Energetica

**Scenario**: Aggregare e analizzare consumi per linea produttiva

```groovy
// Query Gremlin per analisi
// Consumo totale per linea produttiva
g.V().has('type', 'production_line')
  .group()
    .by('name')
    .by(out('contains').values('energy_kwh').sum())

// Trova equipaggiamento inefficiente
g.V().has('type', 'equipment')
  .has('energy_kwh', gt(1000))
  .has('production_output', lt(100))
  .values('name')

// Trend consumo nelle ultime 24 ore
g.V().has('type', 'energy_meter')
  .has('timestamp', within(now - 86400000, now))
  .values('kwh')
  .mean()
```

### 4. Tracciabilità Prodotto

**Scenario**: Tracciare genealogia materiali e lotti

```groovy
// Trova tutti i batch che hanno usato un lotto specifico
g.V().has('lot_number', 'LOT12345')
  .in('usedIn')
  .in('producedBy')
  .values('batch_id')

// Traccia percorso prodotto
g.V().has('product_id', 'PROD-001')
  .repeat(out('processedBy'))
  .until(has('type', 'final_product'))
  .path()
    .by('name')

// Trova prodotti potenzialmente difettosi
g.V().has('lot_number', 'FAULTY-LOT')
  .in('usedIn')
  .out('produced')
  .values('product_id')
```

---

## Riferimenti e Risorse

### Documentazione Ufficiale

- **WaldOT GitHub**: [https://github.com/rossonet/waldot](https://github.com/rossonet/waldot)
- **Apache TinkerPop**: [https://tinkerpop.apache.org/](https://tinkerpop.apache.org/)
- **Eclipse Milo**: [https://github.com/eclipse/milo](https://github.com/eclipse/milo)
- **OPC UA Specification**: [https://reference.opcfoundation.org/](https://reference.opcfoundation.org/)

### Guide Plugin

- [Manuale Sviluppo Plugin](../../guide/docs/manuale_plugins.md) - Guida completa sviluppo plugin
- [waldot-plugin-generator](../../plugins/waldot-plugin-generator/README.md) - Plugin generatore dati
- [waldot-plugin-rules-engine](../../plugins/waldot-plugin-rules-engine/README.md) - Plugin motore regole
- [waldot-plugin-tinkerpop](../../plugins/waldot-plugin-tinkerpop/README.md) - Plugin server Gremlin

### Container e Distribuzione

- **Docker Hub**: [https://hub.docker.com/r/rossonet/waldot](https://hub.docker.com/r/rossonet/waldot)
- **Maven Central**: [https://central.sonatype.com/search?q=net.rossonet.waldot](https://central.sonatype.com/search?q=net.rossonet.waldot)

### Community e Supporto

- **Issue Tracker**: [https://github.com/rossonet/waldot/issues](https://github.com/rossonet/waldot/issues)
- **Discussions**: [https://github.com/rossonet/waldot/discussions](https://github.com/rossonet/waldot/discussions)

---

## Sponsor del Progetto

[![Rossonet s.c.a r.l.](https://raw.githubusercontent.com/rossonet/images/main/artwork/rossonet-logo/png/rossonet-logo_280_115.png)](https://www.rossonet.net)

**WaldOT** è sviluppato e mantenuto da **Rossonet s.c.a r.l.**

---

*Documentazione WaldOT - Versione 0.6.1*  
*Copyright © 2024 Rossonet s.c.a r.l. - Licensed under Apache License 2.0*
