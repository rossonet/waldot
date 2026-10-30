# wotctl – `waldot`, la riga di comando di WaldOT

`wotctl` contiene `waldot`, l'interfaccia a riga di comando di WaldOT. Non è
interattiva ed è pensata per gli script bash, le pipe e i container. Si collega
a un server WaldOT via OPC UA con il client v2 (`waldot-client`) e ne espone
tutte le funzioni:

| Area                    | Comandi                                                        |
|-------------------------|----------------------------------------------------------------|
| server                  | `about`                                                        |
| grafo                   | `query` (`--text`, `--graphson`), `describe`                   |
| comandi WaldOT          | `commands list`, `commands call` (globali o `--element`)       |
| modifica del grafo      | `vertex add/delete`, `edge add/delete`, `property get/set`     |
| backup                  | `snapshot export`, `snapshot import [--clear]`, `snapshot clear --yes` |
| disegno del grafo       | `render` (SVG o Graphviz DOT, con filtri e layout)             |
| spazio degli indirizzi  | `browse`, `read`, `write`, `call`, `resolve`                   |
| sottoscrizioni          | `subscribe` (proprietà `-p`, nodi `-n`, eventi `-e`)           |

La guida completa, in italiano, è integrata nel comando:

```bash
waldot --help
waldot help subscribe
waldot snapshot import --help
```

## Compilazione ed esecuzione

```bash
./gradlew :wotctl:shadowJar                     # fat jar eseguibile
java -jar waldot/clients/wotctl/build/libs/waldot-cli.jar --help

./gradlew :wotctl:installDist                   # distribuzione con lo script 'waldot'
waldot/clients/wotctl/build/install/waldot/bin/waldot about
```

Un alias comodo: `alias waldot='java -jar /percorso/waldot-cli.jar'`.

## Regole per gli script

- **stdout contiene solo i dati**, una riga alla volta e con flush immediato.
  Log, avvisi ed errori vanno su **stderr**. Il livello predefinito è `warn`;
  `-v`, `-vv` e `-vvv` aumentano il dettaglio, e lo stesso vale per la
  variabile `WALDOT_LOG_LEVEL`.
- **Formato** con `-o`:
  - `text`: leggibile (predefinito);
  - `json`: un unico documento;
  - `jsonl`: un oggetto per riga;
  - `value`: solo i valori;
  - `topic`: solo con `subscribe`.
- **Codici di uscita**:

  | Codice | Significato                                                  |
  |--------|--------------------------------------------------------------|
  | 0      | successo, anche se il processo a valle ha chiuso la pipe     |
  | 1      | errore del comando sul server                                |
  | 2      | uso errato                                                   |
  | 3      | connessione fallita, persa o scaduta                         |
  | 130    | interrotto con SIGINT (Ctrl-C)                               |

- I NodeId in forma OPC UA contengono `;` e nella shell vanno tra apici:
  `'ns=2;s=sensor-1'`. In alternativa basta l'identificatore (`sensor-1`).

## Connessione

Le opzioni di connessione vanno dopo il nome del comando. Ognuna ha anche una
variabile d'ambiente, usata quando l'opzione non è indicata (comodo con
`docker run -e`):

| Opzione | Variabile | Note |
|---|---|---|
| `--endpoint` | `WALDOT_ENDPOINT` | predefinito `opc.tcp://localhost:12686/waldot` |
| `--security-policy` | `WALDOT_SECURITY_POLICY` | None, Basic128Rsa15, Basic256, Basic256Sha256, Aes128_Sha256_RsaOaep, Aes256_Sha256_RsaPss |
| `--security-mode` | `WALDOT_SECURITY_MODE` | None, Sign, SignAndEncrypt |
| `--username` | `WALDOT_USERNAME` | password da `--password-file` o da `WALDOT_PASSWORD` (non esiste `--password`) |
| `--password-file` | `WALDOT_PASSWORD_FILE` | prima riga del file, `-` = stdin |
| `--identity-cert`, `--identity-key` | `WALDOT_IDENTITY_CERT`, `WALDOT_IDENTITY_KEY` | identità X509 in PEM (chiave PKCS#8 o PKCS#1 non cifrata) |
| `--keystore`, `--keystore-password` | `WALDOT_KEYSTORE`, `WALDOT_KEYSTORE_PASSWORD` | certificato applicativo del client |
| `--trust-all` | `WALDOT_TRUST_ALL` | accetta qualunque certificato del server (solo test) |
| `--force-endpoint-url` | `WALDOT_FORCE_ENDPOINT_URL` | server dietro NAT o in un container |
| `--application-uri`, `--agent-name` | `WALDOT_APPLICATION_URI`, `WALDOT_AGENT_NAME` | |
| `--connect-timeout`, `--request-timeout`, `--session-timeout`, `--keepalive-timeout` | `WALDOT_CONNECT_TIMEOUT`, … | durate: `500ms`, `10s`, `2m`, `1m30s`, `PT10S`, `10` |
| `--reconnect-initial`, `--reconnect-max`, `--reconnect-attempts` | `WALDOT_RECONNECT_*` | backoff esponenziale; 0 tentativi = nessun limite |
| `--namespace-uri`, `--commands-root` | `WALDOT_NAMESPACE_URI`, `WALDOT_COMMANDS_ROOT` | |

Altre variabili:
- `WALDOT_OUTPUT`: formato predefinito;
- `WALDOT_LOG_LEVEL`: livello di log.

## Esempi

```bash
# interrogazioni
waldot query "g.V().count()" -o value
waldot query -o jsonl "g.V().hasLabel('sensor')" | jq -r .id
waldot describe sensor-1 -o json

# modifica del grafo
waldot vertex add --id sensor-1 --label sensor -P zone=A
waldot edge add --label feeds --from pump-3 --to tank-1
waldot property set sensor-1 setpoint 21.5
waldot property get sensor-1 setpoint -o value
waldot vertex delete sensor-1

# backup e ripristino
waldot snapshot export -f grafo.json.gz
waldot snapshot import --clear -f grafo.json.gz

# disegno del grafo (SVG con layout Eclipse ELK calcolato dal server)
waldot render -f impianto.svg --group-by directory
waldot render -f plc.svg --root plc-1 --depth 2 --include-runtime --include-ephemeral
waldot render -f regole.svg --edge-label fire --edge-label link-to --no-isolated
waldot render -f grafo.dot && dot -Tpng grafo.dot -o grafo.png

# spazio degli indirizzi OPC UA
waldot browse --depth 2
waldot read sensor-1/temperature -o value
waldot write sensor-1/setpoint 22
waldot resolve Server ServerStatus CurrentTime

# sottoscrizioni verso MQTT (mosquitto_pub -l pubblica una riga di stdin per messaggio)
waldot subscribe -p sensor-1:value -o value | mosquitto_pub -t waldot/sensor-1 -l
waldot subscribe -p s1:value -p s2:value -e s1 -o topic --topic-prefix waldot \
  | while read -r t p; do mosquitto_pub -t "$t" -m "$p"; done

# fine dopo 10 notifiche o dopo un'ora
waldot subscribe -p s1:value --count 10 -o jsonl
waldot subscribe -p s1:value --duration 1h -o jsonl >> registro.jsonl
```

## Docker

L'immagine si costruisce dalla radice del repository con
[`docker/Dockerfile.waldot-client`](../../../docker/Dockerfile.waldot-client).
L'`ENTRYPOINT` è `java -jar /app/waldot-cli.jar`, quindi gli argomenti di
`docker run` arrivano direttamente alla CLI:

```bash
docker build -f docker/Dockerfile.waldot-client -t rossonet/waldot-client .
docker run --rm -e WALDOT_ENDPOINT=opc.tcp://server:12686/waldot rossonet/waldot-client subscribe -p s1:value
docker run --rm -v "$PWD:/data" -e WALDOT_ENDPOINT=opc.tcp://server:12686/waldot \
  rossonet/waldot-client snapshot export -f /data/grafo.json.gz
```

## Struttura del codice

- `net.rossonet.waldot.cli.WaldotCli`: main, gestione degli errori e dei codici di
  uscita, configurazione del log (slf4j-simple su stderr).
- `net.rossonet.waldot.cli.support`: opzioni di connessione (`ConnectionOptions`,
  mappate su `WaldotClientConfigurationBuilder`), output (`Printer`, `Json`), durate,
  PEM, valori tipizzati.
- `net.rossonet.waldot.cli.commands`: un comando picocli per classe.

Test:
- i test unitari sono in questo modulo (`./gradlew :wotctl:test`);
- quelli con il server embedded sono in `waldot-namespace`
  (`net.rossonet.waldot.client.integration.WaldotCliIntegrationTest`).

Il modulo dipende solo da `waldot-client` (e da `waldot-deps`). Non deve
dipendere da `waldot-namespace`: Eclipse segnalerebbe un ciclo, perché
`waldot-namespace` usa `wotctl` nei suoi test.

## Licenza

Apache License 2.0, come tutto il progetto WaldOT. Vedi il file [LICENSE](LICENSE).
