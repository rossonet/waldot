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

	// snapshots (GraphSON v3, single block or chunked transfer)
	GraphSnapshot snapshot = client.snapshots().exportGraph();
	client.snapshots().restore(snapshot); // clear graph + import graph

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

Note: the properties of the vertices are `BaseDataType` variables, so the server refuses
absolute/percent deadbands on them (`Bad_FilterNotAllowed`); the deadband works on numeric
variables.
