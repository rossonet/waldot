# WaldOT Integration Guide: Writing a Plugin

This guide explains how to extend WaldOT with a **plugin**: a Java library that
adds new *active* vertex types, edge types and commands to the graph. Every
example on this page has been compiled against the libraries of the
`rossonet/waldot` image and executed in a container.

The built-in plugins of the `waldot-app` distribution are the reference
implementations:

| Plugin | Module | Vertex types | Edge types | Commands (folder) |
| ------ | ------ | ------------ | ---------- | ----------------- |
| Generator | `plugins/waldot-plugin-generator` | `sim:generator`, `sim:event-generator` | — | `create generator`, `create event generator` (`simulator`) |
| Rules engine | `plugins/waldot-plugin-rules-engine` | `rules:rule`, `rules:compute` | `rules:execute` | `create rule`, `create compute`, `create compute monitored edge`, showcases (`rules`) |
| TinkerPop | `plugins/waldot-plugin-tinkerpop` | `gremlin` | — | `create gremlin server` (`tinkerpop`) |

---

## 1. What a plugin can do

In WaldOT every vertex of the graph is also an OPC UA object and every vertex
property is an OPC UA variable. A plugin contributes:

- **vertex types**: a Java class (subclass of `AbstractOpcVertex`) plus an OPC UA
  `ObjectType`. When a vertex is created with `type = <your label>` WaldOT asks
  your plugin to build it. The vertex can run code: react to property writes,
  receive notifications through `fire` edges, run background threads, publish
  OPC UA events;
- **edge types**: labels for which your plugin is notified when an edge is
  added or removed, with the possibility to veto the creation;
- **commands**: OPC UA methods under the `Commands` folder that are also
  callable from bootstrap scripts, rule actions and the query console.

Everything a plugin does is visible from any OPC UA client: the vertex
variables, the events it posts, the commands it exposes.

---

## 2. Discovery and lifecycle

### Discovery

At startup `WaldotOpcUaServer` scans the class path for **top-level classes**
annotated with `@WaldotPlugin` in these packages (and their sub-packages):

```
net.rossonet.waldot    plugin    plugins
```

The class must implement `PluginListener` (in practice: extend
`AbstractPluginListener`) and have a **public no-argument constructor**.

!!! warning
    A class in any other package (for example `com.mycompany.waldot`) is
    **not** discovered. Use `plugins.<vendor>.<name>` for third-party plugins.

The log confirms the discovery:

```
INFO net.rossonet.waldot.opc.WaldotOpcUaServer - Found plugin: plugins.example.ThresholdPlugin
INFO net.rossonet.waldot.namespaces.HomunculusNamespace - Registering commands from plugin ThresholdPlugin
```

### Lifecycle

| Phase | Method | What to do |
| ----- | ------ | ---------- |
| Registration | `initialize(WaldotNamespace)` | Register vertex types, edge types and commands. Called once, before the OPC UA server starts. |
| Command registration | `getCommands()` | Returns the commands to publish (filled by `registerCommand(...)`). Read right after `initialize`. |
| Start | `start()` | Called just before the bootstrap file is executed (and again after a namespace reset). |
| Vertex creation | `containsVertexType(label)`, `getVertexTypeNode(label)`, `containsVertexTypeNode(nodeId)`, `createVertex(...)` | Handled by `AbstractPluginListener` once the type is registered. |
| Edge creation / removal | `containsEdgeType(label)`, `notifyAddEdge(...)`, `notifyRemoveEdge(...)` | Called only for the edge types you registered. Throw `PluginBlockingException` to refuse an edge. |
| Reset | `reset()` | Namespace reset (`cmd.reset()`): every vertex and edge has already been removed (your vertices received `notifyRemoveVertex()`, your edges `notifyRemoveEdge(...)`); clear the remaining plugin state. `start()` and the bootstrap run again afterwards. |
| Shutdown | `stop()` | Stop background activity of the plugin. |

---

## 3. Project setup

All WaldOT modules target **Java 26**. A plugin only needs `waldot-api` at
compile time: it brings Eclipse Milo, Apache TinkerPop and the WaldOT base
classes.

### Option A: module inside the WaldOT repository

1. Create `plugins/waldot-plugin-<name>/` with `src/main/java/...`.
2. Register it in `settings.gradle`:

    ```groovy
    include 'waldot-plugin-<name>'
    project(':waldot-plugin-<name>').projectDir = file('plugins/waldot-plugin-<name>')
    ```

3. Minimal `build.gradle` (same layout as the built-in plugins):

    ```groovy
    group = 'net.rossonet.waldot'
    version = "${rootProject.version}"

    apply plugin: 'java-library'

    java {
        sourceCompatibility = JavaVersion.VERSION_26
        targetCompatibility = JavaVersion.VERSION_26
    }

    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }

    dependencies {
        implementation project(':waldot-deps')
        api project(':waldot-api')
        testImplementation project(':waldot-namespace')
        testImplementation testFixtures(project(':waldot-client'))
        testImplementation(platform('org.junit:junit-bom:5.13.1'))
        testImplementation('org.junit.jupiter:junit-jupiter')
        testRuntimeOnly('org.junit.platform:junit-platform-launcher')
    }
    ```

4. Add it to the application in `waldot-app/build.gradle`:

    ```groovy
    implementation project(':waldot-plugin-<name>')
    ```

    The next `docker buildx build` produces an image that contains it.

### Option B: external jar added to an existing image

The image starts WaldOT with the class path `/app/lib/*`: any jar placed in
`/app/lib` is loaded and scanned for plugins.

1. Compile against the WaldOT libraries (for example the jars of the image):

    ```bash
    mkdir -p lib
    docker run --rm --entrypoint sh -v "$PWD/lib:/out" rossonet/waldot:latest \
      -c 'cp /app/lib/*.jar /out/'
    javac --release 26 -cp "lib/*" -d out $(find src -name '*.java')
    jar --create --file my-plugin.jar -C out .
    ```

2. Mount the jar (development):

    ```bash
    docker run -p 12686:12686 \
      -v "$PWD/my-plugin.jar:/app/lib/my-plugin.jar:ro" \
      -v "$PWD/boot.conf:/waldot/boot.conf:ro" \
      rossonet/waldot:latest
    ```

    or build a derived image (production):

    ```dockerfile
    FROM rossonet/waldot:latest
    COPY my-plugin.jar /app/lib/
    ```

Third-party libraries not already present in `/app/lib` must be shipped with
the plugin (extra jars in `/app/lib` or a shaded jar).

---

## 4. Step by step: a threshold plugin

The example adds the vertex type `example:threshold`. The vertex compares its
`value` with its `limit`, keeps `alarm` and `exceeded-count` up to date and
posts an OPC UA event on every transition. `value` can be written by an OPC UA
client, by a rule, by a Gremlin query or, more interestingly, by a `link-to`
edge coming from another vertex.

```
src/main/java/plugins/example/
├── ThresholdPlugin.java         plugin: registers type and command
├── ThresholdVertex.java         active vertex
└── CreateThresholdCommand.java  OPC UA / console command
```

### 4.1 The plugin class

```java
package plugins.example;

import net.rossonet.waldot.api.annotation.WaldotPlugin;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.plugin.AbstractPluginListener;

@WaldotPlugin
public class ThresholdPlugin extends AbstractPluginListener {

	/** Value of the "type" property that selects this plugin. */
	public static final String TYPE_LABEL = "example:threshold";

	/** Folder of the plugin commands under the OPC UA "Commands" node. */
	public static final String COMMAND_DIRECTORY = "example";

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		super.initialize(waldotNamespace);
		commands.clear();
		// OPC UA ObjectType + Java class + type label
		registerTypeNode(ThresholdVertex.class, "ExampleThresholdObjectType",
				"Example Threshold", TYPE_LABEL);
		waldotNamespace.getOpcuaServer().updateReferenceTypeTree();
		// OPC UA method Commands/example/"create threshold",
		// console function example_create_threshold
		registerCommand(new CreateThresholdCommand(waldotNamespace));
		logger.info("Example threshold plugin initialized");
	}
}
```

`registerTypeNode(vertexClass, typeNodeId, displayName, typeLabel)`:

- creates the OPC UA `ObjectType` `ObjectTypes/<typeNodeId>` (subtype of
  `BaseObjectType`) with a `Label` variable;
- calls the **static** method `generateParameters(WaldotNamespace,
  UaObjectTypeNode)` of the vertex class (by reflection) to describe the other
  variables of the type;
- binds `typeLabel` to the class: `AbstractPluginListener.createVertex(...)`
  then instantiates the class through the standard constructor shown below.

If the vertex needs extra constructor arguments (for example a shared
executor), override `createVertex(...)` as `WaldotGeneratorPlugin` does for
`sim:generator`.

### 4.2 The vertex class

```java
package plugins.example;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractOpcVertex;

public class ThresholdVertex extends AbstractOpcVertex implements AutoCloseable {

	public static final String ALARM = "alarm";
	public static final String COUNT = "exceeded-count";
	public static final String LIMIT = "limit";
	public static final String VALUE = "value";

	/** Called by registerTypeNode(...): variables of the OPC UA ObjectType. */
	public static void generateParameters(final WaldotNamespace namespace,
			final UaObjectTypeNode typeNode) {
		PluginListener.addParameterToTypeNode(namespace, typeNode, LIMIT, NodeIds.Double, "alarm threshold");
		PluginListener.addParameterToTypeNode(namespace, typeNode, VALUE, NodeIds.Double, "monitored value");
		PluginListener.addParameterToTypeNode(namespace, typeNode, ALARM, NodeIds.Boolean, "true when value > limit");
		PluginListener.addParameterToTypeNode(namespace, typeNode, COUNT, NodeIds.Int64,
				"number of times the limit was exceeded");
	}

	private boolean inAlarm = false;

	/** Constructor signature required by AbstractPluginListener.createVertex(...). */
	public ThresholdVertex(final WaldotGraph graph, final UaNodeContext context, final NodeId nodeId,
			final QualifiedName browseName, final LocalizedText displayName, final LocalizedText description,
			final UInteger writeMask, final UInteger userWriteMask, final UByte eventNotifier, final long version,
			final Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask,
				eventNotifier, version);
		// managed properties: typed, with default value and optional validation
		addManagedProperty(LIMIT, NodeIds.Double, Double.class, false, 100.0, (label, oldValue, newValue) -> {
			if (newValue == null || newValue.isNaN()) {
				throw new PropertyChangeException("limit must be a number");
			}
		}, false, propertyKeyValues);
		addManagedProperty(VALUE, NodeIds.Double, Double.class, false, 0.0, null, false, propertyKeyValues);
		addManagedProperty(ALARM, NodeIds.Boolean, Boolean.class, false, false, null, false, propertyKeyValues);
		addManagedProperty(COUNT, NodeIds.Int64, Long.class, false, 0L, null, false, propertyKeyValues);
	}

	private synchronized void check() {
		final Double value = getRuntimeParameter(VALUE);
		final Double limit = getRuntimeParameter(LIMIT);
		if (value == null || limit == null) {
			return;
		}
		final boolean alarm = value > limit;
		if (alarm != inAlarm) {
			inAlarm = alarm;
			updatePropertyIfChanged(ALARM, alarm);
			if (alarm) {
				final Long count = getRuntimeParameter(COUNT);
				updatePropertyIfChanged(COUNT, (count == null ? 0L : count) + 1);
			}
			// OPC UA event, visible to every client subscribed to the vertex
			sendLogEvent("threshold", (alarm ? "limit exceeded: " : "back to normal: ") + value + " / " + limit,
					alarm ? 700 : 100);
		}
	}

	/** Required by GremlinElement. */
	@Override
	public Object clone() {
		return new ThresholdVertex(graph, getNodeContext(), getNodeId(), getBrowseName(), getDisplayName(),
				getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(),
				getPropertiesAsStringArray());
	}

	@Override
	public void close() {
		// release threads, sockets, files... (nothing to do here)
	}

	/** A "fire" edge pointing to this vertex delivers source events here. */
	@Override
	public void fireEvent(final UaNode node, final BaseEventType event, final int priority) {
		check();
	}

	/** A "fire" edge pointing to this vertex delivers source property changes here. */
	@Override
	public void fireProperty(final UaNode node, final String propertyLabel, final DataValue value,
			final int priority) {
		check();
	}

	/** Every property write (OPC UA client, Gremlin, rule, link edge) passes here. */
	@Override
	public void notifyPropertyValueChanging(final String label, final DataValue value) {
		super.notifyPropertyValueChanging(label, value); // keeps managed properties in sync
		if (VALUE.equals(label) || LIMIT.equals(label)) {
			check();
		}
	}

	@Override
	public void notifyRemoveVertex() {
		close();
	}
}
```

### 4.3 The creation command

```java
package plugins.example;

import java.util.List;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractCreateVertexCommand;

public class CreateThresholdCommand extends AbstractCreateVertexCommand {

	public CreateThresholdCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace, "create threshold", "Create an example threshold vertex",
				ThresholdPlugin.COMMAND_DIRECTORY, ThresholdPlugin.TYPE_LABEL, Language.ENGLISH,
				List.of(VertexParameter.optional(ThresholdVertex.LIMIT, VariableNodeTypes.Double,
						"alarm threshold, default 100")));
	}

	@Override
	protected String validate(final List<Object> keyValues) {
		final String limit = findValue(keyValues, ThresholdVertex.LIMIT);
		if (limit != null && Double.parseDouble(limit) < 0) {
			return "the limit must not be negative";
		}
		return null;
	}
}
```

### 4.4 Using the new vertex

Bootstrap file (script mode, JEXL syntax):

```javascript
// a generator feeds the threshold vertex through a link-to edge
gen = graph.addVertex('id', 'gen-1', 'type', 'sim:generator', 'label', 'gen-1', 'directory', 'example',
    'algorithm', 'sinusoidal', 'min', '0', 'max', '200', 'delay', '300');
thr = graph.addVertex('id', 'thr-1', 'type', 'example:threshold', 'label', 'thr-1', 'directory', 'example',
    'limit', '150');
gen.addEdge('feeds', thr, 'type', 'link-to', 'properties-to-monitor', 'data', 'data', 'value');

// the same vertex type created through the plugin command
log.info('created by command: ' + example_create_threshold.exec('thr-2', 'example', '50')[0]);
log.info('invalid: ' + example_create_threshold.exec('thr-3', 'example', '-1')[0]);
```

Observed result:

```
INFO AbstractOpcCommand - create vertex with parameters: [type, example:threshold, label, thr-2, directory, example, limit, 50]
INFO CONSOLE - invalid: error: the limit must not be negative
```

Reading `ns=2;s=thr-1` with an OPC UA client every two seconds:

```
{'limit': 150.0, 'value': 66.17,  'alarm': False, 'exceeded-count': 20, 'type': 'example:threshold'}
{'limit': 150.0, 'value': 136.32, 'alarm': False, 'exceeded-count': 20, 'type': 'example:threshold'}
{'limit': 150.0, 'value': 188.59, 'alarm': True,  'exceeded-count': 22, 'type': 'example:threshold'}
```

and the folder `Commands/example` contains the method `create threshold`.

---

## 5. Writing vertices

### Constructor and identity

- The constructor must have exactly the signature shown above
  (`WaldotGraph, UaNodeContext, NodeId, QualifiedName, LocalizedText,
  LocalizedText, UInteger, UInteger, UByte, long, Object[]`) when you rely on
  `registerTypeNode(...)`.
- `clone()` is abstract in `GremlinElement`: implement it with the same
  constructor.
- `propertyKeyValues` contains the key/value pairs passed to
  `graph.addVertex(...)` or to the command.
- Reserved keys handled by WaldOT: `id` (OPC UA identifier, for example
  `ns=2;s=thr-1`), `type`, `label`, `directory` (OPC UA folder, levels
  separated by `/`), `name`, `description`, history flags. `label` and `type`
  cannot be changed after creation.
- Use **lowercase** property keys: keys arrive lowercase in
  `notifyPropertyValueChanging` and the built-in plugins register their
  managed properties in lowercase.

### Managed properties

```java
addManagedProperty(label, opcUaDataType, javaType, isRequired, defaultValue,
		callbackOnChange, isNecessaryRestartVertex, propertyKeyValues);
```

- The initial value is taken from `propertyKeyValues` and converted to
  `javaType` (`String`, `Integer`, `Long`, `Boolean`, `Double`, `Float`, `Byte`,
  `Instant`, `JSONObject`); if absent or not convertible the default is used.
- Every later write (OPC UA client, Gremlin `property(...)`, rule, link edge)
  goes through the conversion and, if present, through `callbackOnChange`;
  throwing `PropertyChangeException` rejects the value.
- `getRuntimeParameter(label)` returns the current typed value;
  `setRuntimeParameter(label, value)` updates it without notifications.
- `property(key, value)` / `updatePropertyIfChanged(key, value)` write a graph
  property: the OPC UA variable is updated and observers (`link-*` and `fire`
  edges, history) are notified.

### Reacting to the graph

| Hook | Triggered by |
| ---- | ------------ |
| `notifyPropertyValueChanging(label, value)` | Any write of a property of this vertex. **Always call `super`** first: it keeps label/type immutable, updates managed properties, notifies observers and history. |
| `fireProperty(node, label, value, priority)` | A `fire` edge whose source changed one of its `properties-to-monitor`. |
| `fireEvent(node, event, priority)` | A `fire` edge whose source posted an OPC UA event. |
| `notifyRemoveVertex()` | The vertex is being removed: stop threads and release resources. |

`link-to` and `link-from` edges write directly into the target (or source)
properties, so a vertex fed by a link edge sees the change in
`notifyPropertyValueChanging`. These hooks run on the caller thread: keep them
short and move long or blocking work to a thread of the vertex.

### Background work

Active vertices that poll a device or produce data run their own loop on a
**virtual thread**, started in the constructor and stopped in `close()` /
`notifyRemoveVertex()`:

```java
runner = Thread.ofVirtual().name("my-vertex-" + getNodeId().toParseableString()).unstarted(this::loop);
runner.start();
```

Read the parameters at every cycle (as `sim:event-generator` does) so that
OPC UA clients and rules can change them at runtime.

### Publishing OPC UA events

- `sendLogEvent(category, message, severity)` posts a `SystemEventType` event
  whose source is the vertex (severity 1–1000).
- `postEvent(BaseEventTypeNode)` posts an event built with
  `getNamespace().getEventFactory()`.

Events reach subscribed OPC UA clients and every `fire` edge leaving the
vertex, so they can trigger rules.

---

## 6. Custom edge types

Built-in edge behaviors (`link-to`, `link-from`, `fire`) are provided by the
namespace. A plugin declares its own edge labels with
`registerTypeEdge("<prefix>:<name>")` in `initialize(...)`. For those labels:

- `notifyAddEdge(edge, source, target, label, type, keyValues)` is called after
  the edge is created; throwing `PluginBlockingException` removes it and the
  caller receives an error;
- `notifyRemoveEdge(edge)` is called on removal;
- `AbstractPluginListener` forwards both notifications to source and target
  vertices that implement `EdgeMonitoredVertex`
  (`notifyAddTargetMonitoredEdge`, `notifyAddSourceMonitoredEdge`, ...).

The rules engine uses this mechanism for `rules:execute`: `ComputeVertex`
implements `EdgeMonitoredVertex` and refuses the edge when the target is not a
rule:

```java
@Override
public void notifyAddTargetMonitoredEdge(WaldotEdge edge, WaldotVertex targetVertex,
		String label, String type, Object[] propertyKeyValues) throws PluginBlockingException {
	if (!RulesEngineConstants.EXECUTE_EDGE_LABEL.equals(type)) {
		return;
	}
	if (!(targetVertex instanceof ComputableFireableAbstractOpcVertex)) {
		throw new PluginBlockingException("The target of a " + type
				+ " edge must be a rule vertex, but it is " + targetVertex);
	}
	// ... start serving the rule
}
```

---

## 7. Commands

### Vertex creation commands

`AbstractCreateVertexCommand` builds a complete OPC UA method from a list of
`VertexParameter`s:

- inputs: `label`, `directory`, then one argument per parameter
  (`VertexParameter.optional(...)` / `VertexParameter.required(...)` with an
  OPC UA type from `VariableNodeTypes`);
- output: `result`, the NodeId of the new vertex or a message starting with
  `error: `;
- empty inputs are skipped (the vertex uses its defaults), numeric and boolean
  values are checked, then `validate(keyValues)` runs your own checks;
- `clone()` uses the constructor with the `WaldotNamespace` as only argument.

### Where commands appear

| Access | Name |
| ------ | ---- |
| OPC UA | method `Commands/<directory>/<command name>`, e.g. `Commands/example/create threshold` |
| Bootstrap scripts, rule actions, `query` console | object `<directory>_<command name>` with spaces replaced by `_`, invoked with `.exec(...)`: `example_create_threshold.exec('thr-2', 'example', '50')` |

For commands that are not vertex factories extend `AbstractOpcCommand` and
implement `runCommand(InvocationContext, String[])`. Rules for the inputs:

- every input arrives as a `String`. NodeId arguments arrive in their
  parseable form (`ns=2;s=pump-1`), the same form the console uses;
- resolve vertex arguments with `resolveVertexArgument(value, "source")`: it
  accepts `ns=2;s=pump-1` and the plain id `pump-1` and throws an
  `IllegalArgumentException` with a clear message when the vertex does not
  exist;
- return ids with `nodeIdToString(edge.id())` (parseable form);
- catch the runtime exceptions and return `error: <message>`: an exception
  escaping from `runCommand` reaches the OPC UA client as a generic
  `BadInternalError`.

```java
@Override
public String[] runCommand(final InvocationContext context, final String[] inputs) {
	try {
		final Vertex source = resolveVertexArgument(inputs[1], "source");
		final Vertex target = resolveVertexArgument(inputs[2], "target");
		final Edge edge = source.addEdge(inputs[0], target, "type", "example:watch");
		return new String[] { nodeIdToString(edge.id()) };
	} catch (final RuntimeException e) {
		return new String[] { "error: " + e.getMessage() };
	}
}
```

---

## 8. Testing

Plugin tests start a complete in-process namespace with `OpcFactory` (module
`waldot-namespace`) and the test fixtures of `waldot-client`; the plugin is
discovered from the test class path like in production:

```java
package plugins.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.test.WaldotTestEnvironment;

public class ThresholdVertexTest {

	private WaldotGraph g;

	@BeforeEach
	public void beforeEach() throws Exception {
		Files.deleteIfExists(WaldotTestEnvironment.bootFile());
		g = OpcFactory.getOpcGraph(WaldotTestEnvironment.bootUrl(), new LoggerHistoryStrategy());
	}

	@AfterEach
	public void afterEach() throws Exception {
		WaldotTestEnvironment.cleanup(g, null);
	}

	@Test
	public void alarmFollowsValue() {
		final Vertex v = g.addVertex("type", "example:threshold", "label", "t", "limit", "10");
		v.property("value", 20.0);
		assertEquals(true, v.property("alarm").value());
		assertEquals(1L, v.property("exceeded-count").value());
	}
}
```

See `plugins/waldot-plugin-generator/src/test/java` for complete examples,
including OPC UA client checks with `WaldotTestClientHandler`. Property writes
are processed synchronously, so the assertions can follow the write directly;
only values produced by background threads need polling.

---

## 9. Checklist

- [ ] Plugin class annotated with `@WaldotPlugin`, top-level, public no-arg
      constructor, in a package under `net.rossonet.waldot`, `plugin` or
      `plugins`.
- [ ] Type labels with a unique prefix (`vendor:type`) to avoid clashes with
      `sim:*`, `rules:*` and other plugins.
- [ ] Vertex class with the standard constructor, static
      `generateParameters(...)`, `clone()`.
- [ ] Lowercase property keys; `super.notifyPropertyValueChanging(...)` called
      first.
- [ ] Threads stopped in `close()` / `notifyRemoveVertex()`; plugin state
      cleared in `reset()`.
- [ ] Commands created through `AbstractCreateVertexCommand` with
      `validate(...)` for cross-parameter checks.
- [ ] Compiled for Java 26; extra dependencies shipped in `/app/lib`.
- [ ] Behavior verified with an OPC UA client: variables, events, commands.
