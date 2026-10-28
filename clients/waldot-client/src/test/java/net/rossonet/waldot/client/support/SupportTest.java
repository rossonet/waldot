package net.rossonet.waldot.client.support;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ulong;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedEdge;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedProperty;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertex;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.client.api.command.ArgumentDescriptor;
import net.rossonet.waldot.client.api.graph.ElementDescription;

/** Unit tests of the support classes: backoff, conversions, JSON parsing. */
class SupportTest {

	private static ArgumentDescriptor arg(final NodeId type) {
		return new ArgumentDescriptor("a", type, -1, null);
	}

	@Test
	void argumentConversionScalars() {
		assertEquals("12", ArgumentConverter.convert(12, arg(NodeIds.String)));
		assertEquals("hi", ArgumentConverter.convert(LocalizedText.english("hi"), arg(NodeIds.String)));
		assertEquals(true, ArgumentConverter.convert("true", arg(NodeIds.Boolean)));
		assertEquals(false, ArgumentConverter.convert(false, arg(NodeIds.Boolean)));
		assertEquals(1.5d, ArgumentConverter.convert("1.5", arg(NodeIds.Double)));
		assertEquals(2.0f, ArgumentConverter.convert(2, arg(NodeIds.Float)));
		assertEquals(7L, ArgumentConverter.convert("7", arg(NodeIds.Int64)));
		assertEquals(7, ArgumentConverter.convert(7L, arg(NodeIds.Int32)));
		assertEquals(1, ArgumentConverter.convert(true, arg(NodeIds.Int32)));
		assertEquals((short) 3, ArgumentConverter.convert(3, arg(NodeIds.Int16)));
		assertEquals((byte) 4, ArgumentConverter.convert(4, arg(NodeIds.SByte)));
		assertEquals(ubyte(5), ArgumentConverter.convert(5, arg(NodeIds.Byte)));
		assertEquals(ushort(6), ArgumentConverter.convert("6", arg(NodeIds.UInt16)));
		assertEquals(uint(8), ArgumentConverter.convert(8, arg(NodeIds.UInt32)));
		assertEquals(ulong(9), ArgumentConverter.convert(9, arg(NodeIds.UInt64)));
		assertEquals(new NodeId(2, "x"), ArgumentConverter.convert("ns=2;s=x", arg(NodeIds.NodeId)));
		assertEquals(LocalizedText.english("t"), ArgumentConverter.convert("t", arg(NodeIds.LocalizedText)));
		// unknown types and missing descriptors keep the value
		assertEquals(3, ArgumentConverter.convert(3, arg(NodeIds.BaseDataType)));
		assertEquals(3, ArgumentConverter.convert(3, null));
		assertNull(ArgumentConverter.convert(null, arg(NodeIds.Int32)));
		assertThrows(IllegalArgumentException.class, () -> ArgumentConverter.convert("x", arg(NodeIds.Int32)));
	}

	@Test
	void argumentConversionArraysAndVariants() {
		final ArgumentDescriptor strings = new ArgumentDescriptor("a", NodeIds.String, 1, null);
		assertArrayEquals(new String[] { "1", "2" },
				(String[]) ArgumentConverter.convert(List.of(1, 2), strings));
		assertArrayEquals(new Integer[] { 1, 2 }, (Integer[]) ArgumentConverter.convert(new int[] { 1, 2 },
				new ArgumentDescriptor("a", NodeIds.Int32, 1, null)));
		assertArrayEquals(new String[] { "x" }, (String[]) ArgumentConverter.convert("x", strings));
		assertArrayEquals(new String[] { "a" }, (String[]) ArgumentConverter.convert(new String[] { "a" },
				new ArgumentDescriptor("a", NodeIds.String, 0, null)));
		final Variant[] variants = ArgumentConverter.toVariants(new Object[] { 1, new Variant("v"), "x" },
				List.of(arg(NodeIds.String)));
		assertEquals("1", variants[0].getValue());
		assertEquals("v", variants[1].getValue());
		assertEquals("x", variants[2].getValue());
		assertEquals(0, ArgumentConverter.toVariants(null, null).length);
		assertEquals(List.of("a", "b"), ArgumentConverter.fromVariant(new Variant(new String[] { "a", "b" })));
		assertEquals(List.of(1, 2), ArgumentConverter.fromVariant(new Variant(new int[] { 1, 2 })));
		assertEquals("s", ArgumentConverter.fromVariant(new Variant("s")));
		assertNull(ArgumentConverter.fromVariant(null));
	}

	@Test
	void backoffGrowsUpToTheMaximum() {
		final ReconnectBackoff backoff = new ReconnectBackoff(100, 1000, 3);
		assertEquals(Duration.ofMillis(100), backoff.delay(1));
		assertEquals(Duration.ofMillis(200), backoff.delay(2));
		assertEquals(Duration.ofMillis(400), backoff.delay(3));
		assertEquals(Duration.ofMillis(800), backoff.delay(4));
		assertEquals(Duration.ofMillis(1000), backoff.delay(5));
		assertEquals(Duration.ofMillis(1000), backoff.delay(500));
		assertEquals(Duration.ofMillis(100), backoff.delay(0));
		assertFalse(backoff.exhausted(2));
		assertTrue(backoff.exhausted(3));
		assertFalse(new ReconnectBackoff(1, 1, 0).exhausted(Integer.MAX_VALUE));
		assertThrows(IllegalArgumentException.class, () -> new ReconnectBackoff(0, 1, 0));
		assertThrows(IllegalArgumentException.class, () -> new ReconnectBackoff(10, 1, 0));
		assertThrows(IllegalArgumentException.class, () -> new ReconnectBackoff(1, 1, -1));
	}

	@Test
	void describeJsonIsParsed() throws IOException {
		final String json = "{\"id\":\"ns=2;s=v1\",\"kind\":\"vertex\",\"label\":\"person\",\"browseName\":\"v1\","
				+ "\"displayName\":\"V 1\",\"description\":null,\"eventNotifier\":1,\"type\":\"vertex\","
				+ "\"properties\":[{\"key\":\"age\",\"nodeId\":\"ns=2;s=v1/age\",\"value\":29,\"valueType\":\"Integer\"},"
				+ "{\"key\":\"big\",\"nodeId\":\"ns=2;s=v1/big\",\"value\":12345678901,\"valueType\":\"Long\"},"
				+ "{\"key\":\"w\",\"nodeId\":null,\"value\":0.5,\"valueType\":\"Double\"},"
				+ "{\"key\":\"ok\",\"nodeId\":\"\",\"value\":true,\"valueType\":\"Boolean\"},"
				+ "{\"key\":\"n\",\"nodeId\":\"ns=2;s=v1/n\",\"value\":null,\"valueType\":null},"
				+ "{\"key\":\"s\",\"nodeId\":\"ns=2;s=v1/s\",\"value\":\"x\",\"valueType\":\"String\"}],"
				+ "\"outEdges\":[{\"id\":\"ns=2;s=e1\",\"label\":\"knows\",\"type\":\"knows\",\"source\":\"ns=2;s=v1\","
				+ "\"target\":\"ns=2;s=v2\"}],\"inEdges\":[],"
				+ "\"commands\":[{\"name\":\"delete\",\"nodeId\":\"ns=2;s=v1.delete\",\"description\":\"d\","
				+ "\"inputs\":[],\"outputs\":[\"output\",null]}]}";
		final ElementDescription description = ElementDescriptionParser.parse(json);
		assertEquals(new NodeId(2, "v1"), description.id());
		assertEquals("vertex", description.kind());
		assertEquals("person", description.label());
		assertEquals("V 1", description.displayName());
		assertNull(description.description());
		assertEquals(1, description.eventNotifier());
		assertEquals(29, description.property("age").value());
		assertEquals(12345678901L, description.property("big").value());
		assertEquals(0.5, description.property("w").value());
		assertNull(description.property("w").nodeId());
		assertNull(description.property("ok").nodeId());
		assertEquals(true, description.property("ok").value());
		assertNull(description.property("n").value());
		assertEquals("x", description.property("s").value());
		assertEquals(new NodeId(2, "v1/age"), description.property("age").nodeId());
		assertEquals(1, description.outEdges().size());
		assertEquals(new NodeId(2, "v2"), description.outEdges().get(0).target());
		assertTrue(description.inEdges().isEmpty());
		assertEquals("delete", description.commands().get(0).name());
		assertEquals(java.util.Arrays.asList("output", null), description.commands().get(0).outputs());
		assertNull(description.source());

		final ElementDescription edge = ElementDescriptionParser
				.parse("{\"id\":\"ns=2;s=e1\",\"kind\":\"edge\",\"source\":\"ns=2;s=a\",\"target\":\"ns=2;s=b\"}");
		assertTrue(edge.isEdge());
		assertEquals(new NodeId(2, "a"), edge.source());
		assertNull(edge.eventNotifier());
		assertTrue(edge.properties().isEmpty());
		assertThrows(IOException.class, () -> ElementDescriptionParser.parse("[1]"));
		assertThrows(IOException.class, () -> ElementDescriptionParser.parse("{bad"));
	}

	@Test
	void graphSonRoundTripWithNodeIds() throws IOException {
		final DetachedVertex a = DetachedVertex.build().setId(new NodeId(2, "a")).setLabel("person")
				.addProperty(DetachedVertexProperty.build().setId(new NodeId(2, "a/name")).setLabel("name")
						.setValue("a").create())
				.create();
		final DetachedVertex b = DetachedVertex.build().setId(new NodeId(2, "b")).setLabel("person").create();
		final DetachedEdge e = DetachedEdge.build().setId(new NodeId(2, "e")).setLabel("knows").setOutV(a).setInV(b)
				.addProperty(new DetachedProperty<>("weight", 0.5d)).create();
		final String json = GraphSonSupport
				.write(List.of(a, e, Map.of("count", 2L), new NodeId(2, 7)));
		assertTrue(json.contains("tinker:nodeId"), json);
		final List<Object> values = GraphSonSupport.toList(GraphSonSupport.read(json));
		assertEquals(4, values.size());
		final Vertex vertex = assertInstanceOf(Vertex.class, values.get(0));
		assertEquals(new NodeId(2, "a"), vertex.id());
		assertEquals("a", vertex.value("name"));
		final Edge edge = assertInstanceOf(Edge.class, values.get(1));
		assertEquals(new NodeId(2, "e"), edge.id());
		assertEquals(new NodeId(2, "b"), edge.inVertex().id());
		assertEquals(Map.of("count", 2L), values.get(2));
		assertEquals(new NodeId(2, 7), values.get(3));
		assertNull(GraphSonSupport.read(" "));
		assertTrue(GraphSonSupport.toList(null).isEmpty());
		assertEquals(List.of("x"), GraphSonSupport.toList("x"));
	}
}
