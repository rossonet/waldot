# Quickstart

## 1. Crea il compute

```java
Vertex compute = graph.addVertex("type", "rules:compute", "label", "main-compute", "threads", "2");
```

## 2. Crea la regola

```java
Vertex rule = graph.addVertex("type", "rules:rule", "label", "temp-alarm",
    "condition", "g.V().has('id','sensor').next().property('temperature').value() > 80.0",
    "action", "log.warn('Temperature alarm')");
```

Se `condition` e `action` mancano valgono `true` e `log.info('action fired')`.

## 3. Collega compute e regola

```java
compute.addEdge("runner", rule, "type", "rules:execute", "priority", "100");
```

La direzione è **compute → regola**. Senza questo edge la regola accoda eventi ma nessuno li esegue.

## 4. Collega la sorgente

```java
sensor.addEdge("watch", rule, "type", "fire", "properties-to-monitor", "temperature");
```

## 5. Verifica

Aggiorna la proprietà e controlla i contatori della regola:

```java
sensor.property("temperature", "90");
rule.property("total").value();     // eventi valutati
rule.property("executed").value();  // azioni riuscite
rule.property("errors").value();    // errori
rule.property("queue").value();     // eventi in attesa
```

## 6. Modifiche a runtime

`condition`, `action`, `debug`, `hysteresis` (regola) e `threads`, `execution-timeout-ms`, `factor` (compute) si modificano scrivendo la proprietà. Gli script vengono ricompilati alla prima esecuzione successiva.

## Da console

Comandi: `create compute`, `create rule`, `create compute monitored edge` (parametri come nel README).

## Problemi frequenti

| Sintomo | Causa |
|---------|-------|
| `queue` cresce e `executed` no | Manca l'edge `rules:execute` o ha direzione inversa |
| `errors` cresce | Script non compilabile, eccezione o condizione non booleana: usa `debug` ≥ 1 |
| Eventi ripetuti scartati | `hysteresis` troppo alto |
| Regola sempre in coda | `threads` troppo basso o azioni lente (vedi `execution-timeout-ms`) |
