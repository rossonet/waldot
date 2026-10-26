# WaldOT Runner

Shared bootstrap module for WaldOT server applications.

## Scope

`waldot-runner` contains `net.rossonet.agent.AbstractWaldotRunner`, the base
class that:

- declares every server option with [Picocli](https://picocli.info/)
  (network, identity, security, root nodes, built-in commands, rules-engine
  fact defaults, bootstrap URL);
- maps the options onto `DefaultOpcUaConfiguration` and
  `DefaultHomunculusConfiguration`;
- creates `WaldotOpcUaServer` and `HomunculusNamespace` with the default
  strategies (anonymous / identity / X.509 validators,
  `SingleFileBootstrapStrategy`, `BaseConsoleStrategy`,
  `BaseClientManagementStrategy`);
- starts the server and waits for completion.

Concrete applications only add the Picocli `@Command` annotation and a `main`
method. In this repository the implementation is
`waldot-app/src/main/java/net/rossonet/agent/WaldotRunner.java`; other
distributions (for example `clarabella` and `logica`) extend the same class.

## Extension points

| Method | Default | Override to |
| ------ | ------- | ----------- |
| `createMiloStrategy()` | `OpcFactory.createMiloStrategy()` (`optimized`, or `base` with `-Dwaldot.milo.strategy=base`) | provide a custom Milo strategy |
| `createHistoryStrategy()` | `LoggerHistoryStrategy` | persist history elsewhere (e.g. InfluxDB, SQL) |

Minimal custom runner:

```java
@Command(name = "MyServer", mixinStandardHelpOptions = true)
public class MyRunner extends AbstractWaldotRunner {

	@Override
	protected HistoryStrategy createHistoryStrategy() {
		return new MyHistoryStrategy();
	}

	public static void main(final String... args) {
		System.exit(new CommandLine(new MyRunner()).execute(args));
	}
}
```

Always start the runner through `CommandLine.execute(...)`: calling
`runWaldot()` directly skips Picocli parsing and leaves the options without
their default values.

## Options

The complete list of options with defaults is maintained in the
[Configuration Reference](../docs/guide/docs/CONFIGURATION.md).

> Configuration is done only through command-line options: environment
> variables are not read. Boolean options accept an optional value
> (`--anonymous-access false`, `--exec-command-executable false`).

## Build

```bash
./gradlew :waldot-runner:build
```

Option parsing is covered by `WaldotRunnerPicocliTest` in `waldot-app`.
