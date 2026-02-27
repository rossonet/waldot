package net.rossonet.waldot.gremlins;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.naming.ConfigurationException;

import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.NamespaceListener;
import net.rossonet.waldot.api.PropertyObserver;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.client.utils.WaldotTestClientHandler;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history.LoggerHistoryStrategy;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.NetworkHelper;

@TestMethodOrder(OrderAnnotation.class)
public class WritePropertiesAndEventsTests {
	protected int error;
	private WaldotGraph g;
	private final NamespaceListener listener = new TestNamespaceListener();
	private WaldotTestClientHandler waldotTestClientHandler;

	@AfterEach
	public void afterEach() {
		clean();
		System.out.println("Test completed");

	}

	@BeforeEach
	public void beforeEach() {
		System.out.println("Starting test...");
		clean();
	}

	private void bootstrapUrlServerInit(String url)
			throws ConfigurationException, InterruptedException, ExecutionException {
		LogHelper.changeJulLogLevel("fine");
		g = OpcFactory.getOpcGraph(url, new LoggerHistoryStrategy());
		g.getWaldotNamespace().addListener(listener);
		Thread.sleep(500);
		waldotTestClientHandler = new WaldotTestClientHandler(g);
	}

	private void clean() {
		try {
			Files.deleteIfExists(Path.of("/tmp/boot.conf"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(Path.of("/tmp/index.txt"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		if (waldotTestClientHandler != null) {
			try {
				waldotTestClientHandler.disconnect();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		if (g != null && g.getWaldotNamespace() != null) {
			try {

				g.getWaldotNamespace().close();
				System.out.println("Graph namespace closed");
				g = null;
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		try {
			while (!NetworkHelper.checkLocalPortAvailable(12686)) {
				System.out.println("Waiting for server shutdown");
				Thread.sleep(5_000);
			}
			Thread.sleep(500);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		try {
			Files.deleteIfExists(Path.of("/tmp/waldot-client.ks"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void simpleServerInit() throws ConfigurationException, InterruptedException, ExecutionException {
		bootstrapUrlServerInit("file:///tmp/boot.conf");
	}

	@Test
	public void testEventWhileWriteProperties() throws Exception {
		simpleServerInit();
		final String firstValue = UUID.randomUUID().toString();
		final AbstractOpcVertex v = (AbstractOpcVertex) g.addVertex("id", "test-id", "label", "test-node",
				"string-value", firstValue);
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "string-value", firstValue);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "string-value", firstValue);

		class TestPropertyObserver implements PropertyObserver {

			int counter = 0;

			private String expected;

			public int getCounter() {
				return counter;
			}

			@Override
			public void propertyChanged(UaNode node, String propertyName, Object value) {
				if (!(value instanceof DataValue) || !((DataValue) value).getValue().getValue().equals(expected)) {
					System.out.println(node + ": property changed: " + propertyName + " new value: " + value
							+ " expected value: " + expected);
					throw new RuntimeException("Unexpected property value");
				} else {
					counter++;
				}
			}

			public void setExpected(String newValue) {
				expected = newValue;

			}
		}

		final TestPropertyObserver propertyObserver = new TestPropertyObserver();
		v.addPropertyObserver(propertyObserver);

		for (int i = 0; i < 20; i++) {
			final String newValue = UUID.randomUUID().toString();
			propertyObserver.setExpected(newValue);
			g.traversal().V().has("id", "test-id").property("string-value", newValue).iterate();
			assert g.getWaldotNamespace().getVerticesCount() == 1;
			assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "string-value", newValue);
			assert waldotTestClientHandler.checkVertexValueEquals("test-id", "string-value", newValue);
		}
		assert propertyObserver.getCounter() == 20;
	}

	@Test
	public void testEventWhileWritePropertiesWithOpc() throws Exception {
		simpleServerInit();
		final String firstValue = "primo dato";
		final AbstractOpcVertex v = (AbstractOpcVertex) g.addVertex("id", "test-id", "label", "test-node",
				"string-value", firstValue);
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "string-value", firstValue);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "string-value", firstValue);

		class TestPropertyObserver implements PropertyObserver {

			int counter = 0;

			private String expected;

			public int getCounter() {
				return counter;
			}

			@Override
			public void propertyChanged(UaNode node, String propertyName, Object value) {
				if (!(value instanceof DataValue) || !((DataValue) value).getValue().getValue().equals(expected)) {
					System.out.println(node + ": property changed: " + propertyName + " new value: " + value
							+ " expected value: " + expected);
					throw new RuntimeException("Unexpected property value");
				} else {
					counter++;
				}
			}

			public void setExpected(String newValue) {
				expected = newValue;

			}
		}

		final TestPropertyObserver propertyObserver = new TestPropertyObserver();
		v.addPropertyObserver(propertyObserver);

		for (int i = 0; i < 20; i++) {
			final String newValue = UUID.randomUUID().toString();
			propertyObserver.setExpected(newValue);
			waldotTestClientHandler.writeOpcUaVertexValue("test-id", "string-value", newValue);
			assert g.getWaldotNamespace().getVerticesCount() == 1;
			assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "string-value", newValue);
			assert waldotTestClientHandler.checkVertexValueEquals("test-id", "string-value", newValue);
		}
		System.out.println("Property observer was triggered " + propertyObserver.getCounter() + " times");
		assert propertyObserver.getCounter() == 20;
	}

	@Test
	public void testOpcUaEvent() throws Exception {
		simpleServerInit();
		final AbstractOpcVertex v = (AbstractOpcVertex) g.addVertex("id", "test-id", "label", "test-node",
				"event-notifier", "true");
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");

		class TestEventObserver implements EventObserver {
			int v = 0;

			@Override
			public void fireEvent(UaNode node, BaseEventType event) {
				System.out.println(node + ": event fired: " + event.getMessage().getText());
				final int check = Integer.valueOf(event.getMessage().getText());
				if (check != v) {
					throw new RuntimeException("Unexpected event message, expected: " + v + " actual: " + check);
				} else {
					v++;
				}
			}
		}

		final TestEventObserver eventObserver = new TestEventObserver();
		v.addEventObserver(eventObserver);

		final WaldotTestClientHandler.EventObserver observer = new WaldotTestClientHandler.EventObserver() {
			int v = 0;

			@Override
			public void onEvent(String nodeId, String eventType, Object value) {
				try {
					System.out.println(nodeId + ": event fired from OPCUA: " + value.toString());
					final int check = Integer.valueOf(((LocalizedText) value).getText());
					if (check != v) {
						throw new RuntimeException("Unexpected event message, expected: " + v + " actual: " + check);
					} else {
						v++;
					}
				} catch (final Exception e) {
					e.printStackTrace();
					error++;
				}
			}

		};
		error = 0;
		waldotTestClientHandler.subscribeToOpcUaVertexEventsMessage("test-id", observer);

		for (int i = 0; i < 20; i++) {
			final BaseEventTypeNode eventNode = g.getWaldotNamespace().getOpcuaServer().getServer().getEventFactory()
					.createEvent(g.getWaldotNamespace().generateNodeId(UUID.randomUUID()), NodeIds.BaseEventType);
			eventNode.setBrowseName(new QualifiedName(1, "message " + i));
			eventNode.setDisplayName(LocalizedText.english("message " + i));
			eventNode.setEventId(ByteString.of(new byte[] { 0, 1, 2, 3 }));
			eventNode.setEventType(NodeIds.BaseEventType);
			eventNode.setSourceNode(v.getNodeId());
			eventNode.setSourceName(v.getDisplayName().text());
			eventNode.setTime(DateTime.now());
			eventNode.setReceiveTime(DateTime.NULL_VALUE);
			eventNode.setMessage(LocalizedText.english(String.valueOf(i)));
			eventNode.setSeverity(ushort(3));
			v.postEvent(eventNode);
			Thread.sleep(1000);
			eventNode.delete();
		}
		assert eventObserver.v == 20;
		assert error == 0;
	}

	@Test
	public void testWriteProperties() throws Exception {
		simpleServerInit();
		final String firstValue = UUID.randomUUID().toString();
		g.addVertex("id", "test-id", "label", "test-node", "string-value", firstValue);
		assert g.getWaldotNamespace().getVerticesCount() == 1;
		assert waldotTestClientHandler.checkOpcUaVertexExists("test-id");
		assert waldotTestClientHandler.checkVertexExists("test-id");
		assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "string-value", firstValue);
		assert waldotTestClientHandler.checkVertexValueEquals("test-id", "string-value", firstValue);
		for (int i = 0; i < 20; i++) {
			final String newValue = UUID.randomUUID().toString();
			g.traversal().V().has("id", "test-id").property("string-value", newValue).iterate();
			assert g.getWaldotNamespace().getVerticesCount() == 1;
			assert waldotTestClientHandler.checkOpcUaVertexValueEquals("test-id", "string-value", newValue);
			assert waldotTestClientHandler.checkVertexValueEquals("test-id", "string-value", newValue);
		}
	}
}
