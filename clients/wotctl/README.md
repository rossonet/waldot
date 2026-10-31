# wotctl – la riga di comando di WaldOT

`wotctl` è l'interfaccia a riga di comando di WaldOT. Non è interattiva ed è
pensata per gli script bash, le pipe e i container. Si collega a un server
WaldOT via OPC UA con il client v2 (`waldot-client`) e ne espone tutte le
funzioni:

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
| shell                   | `completion` (script di autocompletamento bash/zsh)            |
| plugin                  | comandi aggiunti da altri moduli ([Plugin](#plugin))           |

La guida completa, in italiano, è integrata nel comando:

```bash
wotctl --help
wotctl help subscribe
wotctl snapshot import --help
```

## Compilazione ed esecuzione

```bash
./gradlew :wotctl:shadowJar                     # fat jar eseguibile
java -jar waldot/clients/wotctl/build/libs/wotctl.jar --help

./gradlew :wotctl:installDist                   # distribuzione con lo script 'wotctl'
waldot/clients/wotctl/build/install/wotctl/bin/wotctl about
```

Un alias comodo: `alias wotctl='java -jar /percorso/wotctl.jar'`.

Per un eseguibile che non richiede Java vedi [Binario nativo](#binario-nativo).

## Autocompletamento

`wotctl completion` scrive su stdout lo script di autocompletamento, generato da
picocli a partire dai comandi. Completa:
- comandi, sottocomandi e opzioni;
- i valori delle opzioni con un insieme fisso di valori (`-o`,
  `--security-policy`, `--security-mode`, `--type`, le opzioni di `render`,
  `--trigger`, `--attribute`, …);
- i nomi dei file.

```bash
# solo nella shell corrente
source <(wotctl completion)

# per l'utente corrente (bash)
mkdir -p ~/.local/share/bash-completion/completions
wotctl completion > ~/.local/share/bash-completion/completions/wotctl

# zsh (in ~/.zshrc)
autoload -U +X compinit && compinit
autoload -U +X bashcompinit && bashcompinit
source <(wotctl completion)
```

Lo script si trova già pronto anche nelle distribuzioni:
- `completion/wotctl_completion` in `installDist`/`distZip`;
- `completion/wotctl_completion` nell'archivio del binario nativo.

Il task `./gradlew :wotctl:completionScript` lo genera in `build/completion/`.

## Plugin

Altri moduli possono aggiungere comandi a `wotctl` senza modificarlo, con la SPI
`net.rossonet.waldot.cli.spi`:

```java
public class AcmePlugin implements WotctlPlugin {
    public String name() { return "acme"; }
    public void register(final WotctlCommands commands) {
        commands.add(AcmeCommand.class)                    // wotctl acme ...
                .add("snapshot", AcmeCheckCommand.class);  // wotctl snapshot acme-check
    }
}
```

- **Scoperta**: i plugin si dichiarano in
  `META-INF/services/net.rossonet.waldot.cli.spi.WotctlPlugin` e si trovano con `ServiceLoader`.
  Un nome già usato è un errore.
- **Comportamento**: i comandi sono normali comandi picocli. Estendono `ClientCommand`
  (connessione, `-o`, codici di uscita) o `CommandGroup`, e ricevono la guida in italiano, i
  codici di uscita e l'autocompletamento del core. `wotctl --version` elenca i plugin.
- **Distribuzione**: il binario nativo non carica jar a runtime, quindi i plugin entrano in build.
  Un modulo di distribuzione dipende da `wotctl` e dai plugin e applica `gradle/wotctl-cli.gradle`,
  ottenendo fat jar, installDist con lo script di completamento, binario nativo e archivio.
- **Moduli dei plugin**: applicano `gradle/wotctl-plugin.gradle`, che fornisce la dipendenza
  `compileOnly` da wotctl, le test fixtures (`CliTestSupport`, `CommandTestBase`, `MockClient`)
  e il task `nativeMetadata` per i metadati di native-image raccolti con l'agent.
- **Esempi**: un plugin di riferimento e una distribuzione si trovano in wot-addons
  (`wotctl-addons/`).

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
  | 2      | uso errato (rilevato prima di collegarsi al server)          |
  | 3      | connessione fallita, persa o scaduta                         |
  | 130    | interrotto con SIGINT (Ctrl-C)                               |

- I NodeId in forma OPC UA contengono `;` e nella shell vanno tra apici:
  `'ns=2;s=sensor-1'`. In alternativa basta l'identificatore (`sensor-1`).

## Connessione

Le opzioni di connessione vanno dopo il nome del comando. Ognuna ha anche una
variabile d'ambiente `WALDOT_*`, usata quando l'opzione non è indicata (comodo
con `docker run -e`):

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
wotctl query "g.V().count()" -o value
wotctl query -o jsonl "g.V().hasLabel('sensor')" | jq -r .id
wotctl describe sensor-1 -o json

# modifica del grafo
wotctl vertex add --id sensor-1 --label sensor -P zone=A
wotctl edge add --label feeds --from pump-3 --to tank-1
wotctl property set sensor-1 setpoint 21.5
wotctl property get sensor-1 setpoint -o value
wotctl vertex delete sensor-1

# backup e ripristino
wotctl snapshot export -f grafo.json.gz
wotctl snapshot import --clear -f grafo.json.gz

# disegno del grafo (SVG con layout Eclipse ELK calcolato dal server)
wotctl render -f impianto.svg --group-by directory
wotctl render -f plc.svg --root plc-1 --depth 2 --include-runtime --include-ephemeral
wotctl render -f regole.svg --edge-label fire --edge-label link-to --no-isolated
wotctl render -f grafo.dot && dot -Tpng grafo.dot -o grafo.png

# spazio degli indirizzi OPC UA
wotctl browse --depth 2
wotctl read sensor-1/temperature -o value
wotctl write sensor-1/setpoint 22
wotctl resolve Server ServerStatus CurrentTime

# sottoscrizioni verso MQTT (mosquitto_pub -l pubblica una riga di stdin per messaggio)
wotctl subscribe -p sensor-1:value -o value | mosquitto_pub -t waldot/sensor-1 -l
wotctl subscribe -p s1:value -p s2:value -e s1 -o topic --topic-prefix waldot \
  | while read -r t p; do mosquitto_pub -t "$t" -m "$p"; done

# fine dopo 10 notifiche o dopo un'ora
wotctl subscribe -p s1:value --count 10 -o jsonl
wotctl subscribe -p s1:value --duration 1h -o jsonl >> registro.jsonl
```

## Docker

L'immagine si costruisce dalla radice del repository con
[`docker/Dockerfile.waldot-client`](../../../docker/Dockerfile.waldot-client).
L'`ENTRYPOINT` è `java -jar /app/wotctl.jar`, quindi gli argomenti di
`docker run` arrivano direttamente alla CLI:

```bash
docker build -f docker/Dockerfile.waldot-client -t rossonet/waldot-client .
docker run --rm -e WALDOT_ENDPOINT=opc.tcp://server:12686/waldot rossonet/waldot-client subscribe -p s1:value
docker run --rm -v "$PWD:/data" -e WALDOT_ENDPOINT=opc.tcp://server:12686/waldot \
  rossonet/waldot-client snapshot export -f /data/grafo.json.gz
```

## Binario nativo

`wotctl` si può compilare con GraalVM Native Image in un eseguibile che non
richiede Java. Analisi, alternative e misure sono in
[docs/native-image.md](docs/native-image.md).

Prerequisito: GraalVM per JDK 25, **25.0.2 o successivo**, indicato con
`GRAALVM_HOME`. La build normale e i test non richiedono GraalVM.

```bash
export GRAALVM_HOME=~/.local/jvm/graalvm-jdk-25.0.4+7.1
./gradlew --max-workers=1 :wotctl:nativeCompile     # build/native/nativeCompile/wotctl
./gradlew --max-workers=1 :wotctl:nativeSmokeTest   # test sul binario prodotto
./gradlew --max-workers=1 :wotctl:nativeDist        # build/distributions/wotctl-<versione>-linux-<arch>.tar.gz

# test anche contro un server WaldOT reale
WOTCTL_SMOKE_ENDPOINT=opc.tcp://localhost:12686/waldot ./gradlew :wotctl:nativeSmokeTest
```

Senza GraalVM sulla macchina, con Docker (binario statico per amd64, dinamico
glibc per arm64), dalla radice del repository:

```bash
docker buildx build --platform linux/amd64 -f docker/Dockerfile.build-wotctl \
  --output type=local,dest=./output .
```

Il workflow GitHub `publish-wotctl-release.yml` (avvio manuale) compila con lo
stesso Dockerfile i binari amd64 e arm64 e li pubblica come artifact.

Aggiornamento dei metadati di reflection dopo modifiche al codice, raccolti con
il tracing agent di GraalVM durante i test unitari:

```bash
./gradlew :wotctl:test -Pagent --rerun-tasks
./gradlew :wotctl:nativeMetadata   # src/main/resources/META-INF/native-image/net.rossonet.waldot/wotctl/
```

## Struttura del codice

- `net.rossonet.waldot.cli.WaldotCli`: main, gestione degli errori e dei codici di
  uscita, configurazione del log (slf4j-simple su stderr).
- `net.rossonet.waldot.cli.support`:
  - opzioni di connessione (`ConnectionOptions`, mappate su
    `WaldotClientConfigurationBuilder`);
  - output (`Printer`, `Json`);
  - durate, PEM, valori tipizzati;
  - candidati dell'autocompletamento (`Completions`).
- `net.rossonet.waldot.cli.commands`: un comando picocli per classe.

Test:
- i test unitari sono in questo modulo (`./gradlew :wotctl:test`). I comandi
  girano su un client Mockito (`MockClient`, `CommandTestBase`); il report di
  copertura JaCoCo è in `build/reports/jacoco/test/html/`;
- quelli con il server embedded sono in `waldot-namespace`
  (`net.rossonet.waldot.client.integration.WaldotCliIntegrationTest`);
- quelli sul binario nativo sono in `src/nativeSmokeTest` (task `nativeSmokeTest`).

Il modulo dipende solo da `waldot-client` (e da `waldot-deps`). Non deve
dipendere da `waldot-namespace`: Eclipse segnalerebbe un ciclo, perché
`waldot-namespace` usa `wotctl` nei suoi test.

## Licenza

Apache License 2.0, come tutto il progetto WaldOT. Vedi il file [LICENSE](LICENSE).
