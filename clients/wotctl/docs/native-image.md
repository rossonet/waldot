# wotctl come binario nativo

Valutazione e realizzazione di un eseguibile di `wotctl` che non richiede Java,
partendo da un progetto che gira su Java 25.

## Esito in breve

| | jar (`java -jar wotctl.jar`) | binario nativo GraalVM |
|---|---|---|
| Requisiti sul sistema | JRE 25 | nessuno con il binario statico (amd64); `libc` e `libz` con quello dinamico |
| Dimensione | 45 MB (fat jar) + JRE | 77 MB (30 MB nel `.tar.gz`) |
| `wotctl --version` | 0,22 s, 107 MB RSS | **0,01 s**, 48 MB RSS |
| `wotctl query "g.V().count()"` (connessione, query, chiusura) | 1,3 s, 255 MB RSS | **0,18 s**, 96 MB RSS |
| Build | ~20 s | ~1 min 45 s, picco di 5,5 GB di RAM |

Misure fatte su un server WaldOT locale, Linux x86-64, GraalVM 25.0.4.

Per una CLI pensata per gli script, che apre una connessione a ogni invocazione,
il guadagno è netto: ogni comando è circa 7 volte più veloce e usa meno di metà
della memoria. In un ciclo di shell che invoca `wotctl` centinaia di volte la
differenza è di minuti.

## Strategia scelta: GraalVM Native Image

Il binario si produce con `native-image` di GraalVM per JDK 25, tramite il
plugin Gradle `org.graalvm.buildtools.native`. Si sono considerate tre
alternative:

| Strategia | Indipendente da Java | Avvio | Compatibilità | Note |
|---|---|---|---|---|
| **GraalVM Native Image** (scelta) | sì | ~10 ms | richiede metadati per reflection e risorse | un binario per OS/architettura |
| jlink + jpackage | sì (runtime incluso) | come la JVM (~0,2 s + JIT) | totale | ~50-70 MB, nessun guadagno di velocità |
| AOT cache del JDK 25 (JEP 483/514/515) | no | migliore della JVM pura | totale | utile nell'immagine Docker, non elimina Java |

`jlink`/`jpackage` resta la soluzione di riserva se una dipendenza diventasse
incompatibile con native-image. Si ottiene un'installazione autonoma ma non più
veloce. L'AOT cache del JDK si può aggiungere in futuro all'immagine Docker
basata sul jar.

## Come si costruisce

Prerequisito: GraalVM per JDK 25, versione **25.0.2 o successiva**. Le versioni
precedenti non contengono lo schema `reachability-metadata` richiesto dal
repository dei metadati usato dal plugin 1.1.x. Non serve nella build normale:
solo i task `native*` la usano, tramite `GRAALVM_HOME`.

```bash
export GRAALVM_HOME=~/.local/jvm/graalvm-jdk-25.0.4+7.1
./gradlew --max-workers=1 :wotctl:nativeCompile     # build/native/nativeCompile/wotctl
./gradlew --max-workers=1 :wotctl:nativeSmokeTest   # test sul binario
./gradlew --max-workers=1 :wotctl:nativeDist        # build/distributions/wotctl-<versione>-linux-x86_64.tar.gz
```

Il builder usa 6 GB di heap; il valore si cambia con `-PnativeImageXmx=8g`. Con
`--max-workers=1` Gradle non esegue altri task in parallelo alla compilazione.

L'archivio contiene `bin/wotctl`, lo script `completion/wotctl_completion` e la
licenza.

Con `-PnativeStatic=true` si ottiene un eseguibile **totalmente statico**
(`--static --libc=musl`), che non dipende da alcuna libreria di sistema e gira
anche in un container `scratch`. Richiede la toolchain musl di Oracle nel `PATH`
(`x86_64-linux-musl-gcc`, con zlib) ed è supportato da GraalVM solo su
linux-amd64. La build Docker qui sotto la installa da sola.

## Build in Docker e workflow GitHub

[`docker/Dockerfile.build-wotctl`](../../../../docker/Dockerfile.build-wotctl)
esegue l'intera build in un container con Oracle GraalVM per JDK 25
(`container-registry.oracle.com/graalvm/native-image:25`, immagini amd64 e
arm64). Il risultato viene salvato in uno stage `scratch`, da cui buildx lo
estrae. Non serve GraalVM sulla macchina.

```bash
# dalla radice del repository
docker buildx build --platform linux/amd64 -f docker/Dockerfile.build-wotctl \
  --output type=local,dest=./output .
# ./output/wotctl, ./output/wotctl_completion, ./output/LICENSE

docker buildx build --platform linux/amd64,linux/arm64 -f docker/Dockerfile.build-wotctl \
  --output type=local,dest=./output .
# ./output/linux_amd64/..., ./output/linux_arm64/...
```

| Piattaforma | Binario |
|---|---|
| linux/amd64 | statico (musl): nessuna dipendenza, gira anche in `scratch` |
| linux/arm64 | dinamico (glibc e zlib): le immagini statiche musl sono supportate da GraalVM solo su x86-64 |

Il binario statico è stato provato in un container `scratch` vuoto contro un
server WaldOT reale. Funziona, ma lo scratch non ha `/tmp`, dove il client crea
per default il keystore applicativo (`/tmp/waldot-client.ks`). Con sicurezza
`None` la connessione riesce lo stesso, ma su stderr compaiono errori. In un
container minimale va indicato un percorso scrivibile con `--keystore` (o
`WALDOT_KEYSTORE`), oppure va montato un volume su `/tmp`.

Dopo la compilazione il Dockerfile verifica il binario: `--version`, `--help`,
`completion` e una connessione rifiutata con codice di uscita 3, che inizializza
a runtime Milo, Netty e BouncyCastle.

Il workflow GitHub
[`publish-wotctl-release.yml`](../../../../.github/workflows/publish-wotctl-release.yml)
(avvio manuale, *Run workflow*) usa lo stesso Dockerfile con buildx e QEMU per
le due piattaforme. Pubblica gli artifact `wotctl-linux-amd64` e
`wotctl-linux-arm64`. Gli artifact di GitHub non conservano il bit di
esecuzione: dopo il download serve `chmod +x wotctl`. Sotto emulazione QEMU la
build arm64 è molto più lenta di quella amd64, per questo il job ha un timeout
di 6 ore.

## Problemi incontrati e soluzioni

### 1. Il plugin va dichiarato una sola volta nel build

`clarabella`, `logica` e `wotctl` usano tutti `org.graalvm.buildtools.native`.
Se ogni modulo lo dichiara con la propria versione nel blocco `plugins`, Gradle
lo carica in classloader diversi e il servizio condiviso del plugin fallisce
(`GraalVMReachabilityMetadataService$Inject_ cannot be cast…`). Lo stesso accade
anche con la stessa versione.

Soluzione: il plugin è dichiarato con `apply false` nei due build root
(`build.gradle` e `waldot/build.gradle`), alla versione 1.1.18. I moduli lo
applicano senza versione.

### 2. Out of memory nell'analisi: gli inizializzatori `NodeIds*` di Milo

Con la configurazione predefinita la build esauriva anche 10 GB di heap in un
minuto, e su una macchina con poca memoria libera ha bloccato il sistema.
L'istogramma della heap mostrava 57 milioni di `HashMap$TreeNode` (3,2 GB).
L'heap dump analizzato con Eclipse MAT ha indicato il metodo in analisi:
`org.eclipse.milo.opcua.stack.core.NodeIds0.<clinit>()`.

Le classi `NodeIds0`…`NodeIds5` di Milo sono generate e contengono migliaia di
costanti `NodeId`. Native-image analizza il loro inizializzatore statico come
codice e le strutture dell'analisi points-to crescono in modo quadratico con la
dimensione del metodo.

Soluzione: queste classi, e i valori immutabili che contengono (package
`org.eclipse.milo.opcua.stack.core.types.builtin`), sono inizializzate **a tempo
di build** con `--initialize-at-build-time`. Le costanti diventano dati
nell'image heap e l'analisi scende a circa 20 s e 2 GB.

### 3. `commons-logging` e `Log4JLogger`

`waldot-deps` porta `commons-logging`, che cerca le sue implementazioni con
`Class.forName` su nomi costanti. native-image registra quindi
`org.apache.commons.logging.impl.Log4JLogger`, che dipende da log4j 1.x, assente,
e la build fallisce con `Type not found during analysis`.

Soluzione: nel modulo `commons-logging` è escluso e sostituito dal bridge
`org.slf4j:jcl-over-slf4j`. I log delle librerie che usano commons-logging
finiscono su slf4j-simple, quindi su stderr come gli altri, anche nel jar.

### 4. Metadati di reflection

Picocli (annotazioni dei comandi), Jackson e la serializzazione dei record in
`Json`, BouncyCastle e i provider di sicurezza del JDK usano la reflection. I
metadati si raccolgono con il **tracing agent** di GraalVM durante i test
unitari, che coprono oltre il 95% delle righe e attraversano quindi quasi tutto
il codice dei comandi:

```bash
./gradlew :wotctl:test -Pagent --rerun-tasks   # i test girano sulla JVM di GRAALVM_HOME
./gradlew :wotctl:nativeMetadata               # copia filtrata
```

`nativeMetadata` copia `reachability-metadata.json` in
`src/main/resources/META-INF/native-image/net.rossonet.waldot/wotctl/`,
togliendo le voci dei test (JUnit, Mockito, ByteBuddy, classi di test). Netty,
Milo e le altre librerie note sono coperti anche dal GraalVM Reachability
Metadata Repository, attivato dal plugin.

I metadati vanno rigenerati quando si aggiungono comandi o tipi serializzati.

### 5. Versione nel binario

Il binario nativo non ha il manifest del jar. La versione si legge dalla risorsa
`net/rossonet/waldot/cli/wotctl-version.properties`, generata dal build.
`wotctl --version` indica anche quando si tratta del binario nativo.

### 6. Campi `final` delle opzioni

Alcune opzioni picocli erano campi `private final` con un valore costante (es.
`--depth`, `--block-size`, `--topic-prefix`). javac sostituisce queste costanti
direttamente nel codice, quindi il valore scritto da picocli via reflection
veniva ignorato anche con la JVM. In più, un'immagine nativa non può scrivere
campi `final`. Ora i valori predefiniti sono in `defaultValue` e i campi non sono
`final`.

## Test del binario

`src/nativeSmokeTest` contiene `NativeBinarySmokeTest`, eseguito dal task
`nativeSmokeTest` che dipende da `nativeCompile`. Il binario gira come processo
separato con un ambiente ripulito (nessun `JAVA_HOME`, `PATH` minimo). I test
verificano:
- guida e resource bundle in italiano, versione, script di completamento;
- codici di uscita 2 (uso errato) e 3 (connessione rifiutata: inizializza a
  runtime Milo, Netty e BouncyCastle);
- log su stderr con `-vv` (slf4j-simple configurato a runtime);
- con `WOTCTL_SMOKE_ENDPOINT` (server WaldOT reale), il percorso completo:
  - `about`, `commands list`;
  - `vertex add/delete`, `property get`;
  - `query` (GraphSON), `describe`;
  - `snapshot export` (anche gzip), `render` SVG;
  - `browse`, `read`, `subscribe`.

Esito con GraalVM 25.0.4 e un server embedded di `waldot-namespace`: 9 test su 9
superati.

## Limiti e lavoro futuro

- **Niente cross-compilazione**: native-image produce un binario per il sistema
  su cui gira. Per Linux aarch64, macOS o Windows serve una build su quella
  piattaforma, per esempio una matrice di runner CI o un build Docker
  multi-arch.
- **arm64 non statico**: GraalVM supporta `--static --libc=musl` solo su
  x86-64, quindi il binario arm64 è collegato a glibc e zlib. Funziona sulle
  distribuzioni comuni ma non su Alpine (musl) né in un container `scratch`.
- **arm64 emulato**: nel workflow il binario arm64 si compila con QEMU, molte
  volte più lento della build nativa. Un runner arm64 nativo
  (`ubuntu-24.04-arm`) ridurrebbe il tempo a pochi minuti.
- **Memoria di build**: circa 5,5 GB di picco. Sulla macchina di sviluppo va
  lanciata con `--max-workers=1` e senza altre build pesanti in parallelo.
- **Immagine Docker della CLI**: `docker/Dockerfile.waldot-client` usa ancora
  il jar. Con il binario statico amd64 basterebbe un'immagine `scratch` (o
  distroless/static con i certificati e un utente) da circa 80 MB, senza JRE.
