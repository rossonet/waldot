package net.rossonet.waldot.cli.support;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Unit tests of the support classes (no server).
 */
class SupportTest {

	record Sample(String name, NodeId id, List<Integer> values) {
	}

	private static String pem(final String type, final byte[] data) {
		return "-----BEGIN " + type + "-----\n" + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(data)
				+ "\n-----END " + type + "-----\n";
	}

	@TempDir
	Path tmp;

	@Test
	void durations() {
		assertEquals(Duration.ofMillis(500), Durations.parse("500ms"));
		assertEquals(Duration.ofSeconds(10), Durations.parse("10s"));
		assertEquals(Duration.ofSeconds(10), Durations.parse("10"));
		assertEquals(Duration.ofMillis(2500), Durations.parse("2.5"));
		assertEquals(Duration.ofMinutes(2), Durations.parse("2m"));
		assertEquals(Duration.ofSeconds(90), Durations.parse("1m30s"));
		assertEquals(Duration.ofHours(1), Durations.parse("1h"));
		assertEquals(Duration.ofDays(1), Durations.parse("1d"));
		assertEquals(Duration.ofSeconds(10), Durations.parse("PT10S"));
		assertThrows(IllegalArgumentException.class, () -> Durations.parse("dieci"));
		assertThrows(IllegalArgumentException.class, () -> Durations.parse("10x"));
		assertThrows(IllegalArgumentException.class, () -> Durations.parse(""));
	}

	@Test
	void jsonOfOpcUaTypes() {
		assertEquals("\"ns=2;s=sensor-1\"", Json.compact(Json.toNode(new NodeId(2, "sensor-1"))));
		assertEquals("\"testo\"", Json.compact(Json.toNode(LocalizedText.english("testo"))));
		assertEquals("\"2:Name\"", Json.compact(Json.toNode(new QualifiedName(2, "Name"))));
		assertEquals("\"Good\"", Json.compact(Json.toNode(StatusCode.GOOD)));
		assertEquals("\"Bad_Timeout\"", Json.compact(Json.toNode(new StatusCode(StatusCodes.Bad_Timeout))));
		assertEquals("42", Json.compact(Json.toNode(UInteger.valueOf(42))));
		assertEquals("18446744073709551615", Json.compact(Json.toNode(ULong.valueOf("18446744073709551615"))));
		assertEquals("\"AQI=\"", Json.compact(Json.toNode(new ByteString(new byte[] { 1, 2 }))));
		final Instant now = Instant.parse("2026-10-28T10:00:00Z");
		assertEquals("\"2026-10-28T10:00:00Z\"", Json.compact(Json.toNode(new DateTime(now))));
		final JsonNode value = Json.toNode(new DataValue(new Variant(21.5), StatusCode.GOOD, new DateTime(now)));
		assertEquals(21.5, value.get("value").asDouble());
		assertEquals("Good", value.get("status").asText());
		assertEquals("2026-10-28T10:00:00Z", value.get("sourceTime").asText());
		final JsonNode record = Json.toNode(new Sample("a", new NodeId(0, 85), List.of(1, 2)));
		assertEquals("{\"name\":\"a\",\"id\":\"i=85\",\"values\":[1,2]}", Json.compact(record));
		final Map<Object, Object> map = new LinkedHashMap<>();
		map.put(new NodeId(2, "k"), new int[] { 3 });
		assertEquals("{\"ns=2;s=k\":[3]}", Json.compact(Json.toNode(map)));
		assertEquals("NaN", Json.toNode(Double.NaN).asText());
	}

	@Test
	void jsonScalars() {
		assertEquals("", Json.scalar(null));
		assertEquals("abc", Json.scalar("abc"));
		assertEquals("12", Json.scalar(12));
		assertEquals("true", Json.scalar(true));
		assertEquals("[1,2]", Json.scalar(List.of(1, 2)));
		assertEquals("ns=2;s=x", Json.scalar(new NodeId(2, "x")));
		assertEquals("\"a\\nb\"", Json.scalarLine("a\nb"));
		assertEquals("a b", Json.scalarLine("a b"));
	}

	@Test
	void pemPkcs8AndPkcs1() throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		final KeyPair pair = generator.generateKeyPair();
		final byte[] pkcs8 = pair.getPrivate().getEncoded();
		final Path pkcs8File = tmp.resolve("key8.pem");
		Files.writeString(pkcs8File, pem("PRIVATE KEY", pkcs8));
		final PrivateKey read8 = Pem.privateKey(pkcs8File);
		assertArrayEquals(pkcs8, read8.getEncoded());
		// the PKCS#1 key is the content of the last OCTET STRING of the PKCS#8 encoding
		final byte[] pkcs1 = Arrays.copyOfRange(pkcs8, 26, pkcs8.length);
		assertArrayEquals(pkcs8, Pem.wrapRsa(pkcs1));
		final Path pkcs1File = tmp.resolve("key1.pem");
		Files.writeString(pkcs1File, pem("RSA PRIVATE KEY", pkcs1));
		assertArrayEquals(pkcs8, Pem.privateKey(pkcs1File).getEncoded());
		final Path encrypted = tmp.resolve("enc.pem");
		Files.writeString(encrypted, pem("ENCRYPTED PRIVATE KEY", new byte[] { 1, 2, 3 }));
		assertThrows(java.security.GeneralSecurityException.class, () -> Pem.privateKey(encrypted));
	}

	@Test
	void printerFormatsAndClosedOutput() {
		final StringWriter text = new StringWriter();
		new Printer(new PrintWriter(text), OutputFormat.jsonl).list(List.of(1, 2), v -> Map.of("v", v),
				String::valueOf, v -> v);
		assertEquals("{\"v\":1}\n{\"v\":2}\n", text.toString());
		final StringWriter values = new StringWriter();
		new Printer(new PrintWriter(values), OutputFormat.value).list(List.of("a", "b"), v -> v, v -> "x" + v,
				v -> v);
		assertEquals("a\nb\n", values.toString());
		final StringWriter json = new StringWriter();
		new Printer(new PrintWriter(json), OutputFormat.json).list(List.of(1), v -> v, String::valueOf, v -> v);
		assertTrue(json.toString().startsWith("["));
		final OutputStream broken = new OutputStream() {
			@Override
			public void write(final int b) throws IOException {
				throw new IOException("Broken pipe");
			}
		};
		final Printer closed = new Printer(
				new PrintWriter(new java.io.OutputStreamWriter(broken, StandardCharsets.UTF_8)), OutputFormat.text);
		assertThrows(OutputClosedException.class, () -> closed.line("x"));
	}

	@Test
	void typedValues() {
		assertEquals("abc", Values.literal("abc"));
		assertEquals(5, Values.literal("int32:5"));
		assertEquals(0.5, Values.literal("double:0.5"));
		assertEquals(Boolean.TRUE, Values.literal("bool:true"));
		assertEquals("int32:5", Values.literal("string:int32:5"));
		assertEquals("ns=2;s=a:b", Values.literal("ns=2;s=a:b"));
		assertEquals(new NodeId(2, "x"), Values.literal("nodeid:ns=2;s=x"));
		assertEquals(UInteger.valueOf(7), Values.convert("7", "uint32"));
		assertThrows(IllegalArgumentException.class, () -> Values.convert("forse", "bool"));
		assertThrows(IllegalArgumentException.class, () -> Values.convert("1", "complex"));
		assertThrows(IllegalArgumentException.class, () -> Values.literal("int32:abc"));
	}
}
