# WaldOT Client library


## Versions

- `WaldOTAgentClient.withConfiguration(configuration)`: first version (`client.v1`), a
  control thread checks the session periodically. Kept for compatibility.
- `WaldotClient.create(configuration)`: second version (`client.v2`) with the extended API.

## Client v2

```java
WaldOTAgentClientConfiguration configuration = WaldotClientConfigurationBuilder.create()
		.endpoint("opc.tcp://localhost:12686/waldot")
		.username("user", "secret")
		.reconnect(Duration.ofMillis(500), Duration.ofSeconds(30), 0) // backoff, 0 = retry forever
		.build();
try (WaldotClient client = WaldotClient.create(configuration)) {
	client.addListener(new WaldotClientListener() {
		@Override
		public void onStatusChanged(Status previous, Status current) { /* ... */ }
	});
	client.connect(Duration.ofSeconds(10));

	// graph (commands "query json" and "describe element", typed GraphSON v3)
	List<Vertex> sensors = client.graph().vertices("g.V().hasLabel('sensor')");
	ElementDescription sensor = client.graph().describe("sensor-1");

	// base and plugin commands (input arguments converted to the declared types)
	client.commands().setProperty(sensor.id(), "color", "red");
	client.commands().invokeOnElement(sensor.id(), "delete");

	// subscriptions: groups = OPC UA subscriptions, every parameter can change at runtime
	SubscriptionGroup group = client.subscriptions().createGroup("fast",
			SubscriptionGroupOptions.withPublishingInterval(100));
	MonitoredDataItem value = group.subscribeData(client.graph().propertyNodeId(sensor.id(), "value"),
			DataItemOptions.sampling(50).queue(10, true), (item, dataValue) -> { /* ... */ });
	value.modify(value.options().trigger(DataChangeTrigger.StatusValueTimestamp));
	group.subscribeEvents(sensor.id(), EventItemOptions.defaults(), (item, event) -> { /* ... */ });
	// many values at once: one synchronization (CreateMonitoredItems in batches), the items
	// refused by the server stay in the group and are retried
	List<MonitoredDataItem> values = group.subscribeData(List.of(
			new DataItemRequest(client.graph().propertyNodeId(sensor.id(), "value"), null, listener),
			new DataItemRequest(client.graph().propertyNodeId(sensor.id(), "status"), null, listener)));

	// snapshots (GraphSON v3, single block or chunked transfer)
	GraphSnapshot snapshot = client.snapshots().exportGraph();
	client.snapshots().restore(snapshot); // clear graph + import graph

	// drawing of the graph (command "render graph": SVG laid out by Eclipse ELK, or DOT)
	GraphRenderOptions view = new GraphRenderOptions();
	view.vertices().roots(List.of("plc-1")).depth(2).includeEphemeral(true);
	view.properties().vertex(List.of("unit", "status")).includeRuntime(true);
	view.layout().groupBy(GraphRenderOptions.GroupBy.DIRECTORY);
	Files.write(Path.of("plc-1.svg"), client.graph().render(view).data());

	// plain OPC UA address space
	client.addressSpace().browse(client.addressSpace().waldotNodeId("cmd"));
}
```

Connection: the first connection and the connections rebuilt after a failure use the
exponential backoff of the configuration (`FAULTED`, then `UNRECOVERABLE_FAULTED` after
`reconnectMaxAttempts`); a lost session is restored by Milo (`RECONNECTING`). The
subscription groups that cannot be transferred to the new session are created again
(`onRecreated`), and the items refused by the server (e.g. nodes not yet created after a
restart) are retried with the same backoff.

Many items: `subscribeData(List<DataItemRequest>)` creates the items of the list with a single
synchronization (Milo splits the request by `MaxMonitoredItemsPerCall`), while the method for a
single item makes a request per item. The batch method does not discard the items refused by the
server: it retries them with the backoff of the client, so a property of WaldOT written for the
first time later (a node that does not exist yet) is monitored as soon as it appears; the result
of the last attempt is in `status()`. The single-item method keeps throwing
`WaldotClientException` on a refused node.

Note: the properties of the vertices are `BaseDataType` variables, so the server refuses
absolute/percent deadbands on them (`Bad_FilterNotAllowed`); the deadband works on numeric
variables.
