package net.rossonet.waldot.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class TextHelperTest {

	@Test
	void testConvertByteArrayToHexString() {
		final byte[] byteArray = { 0x0F, 0x1A, 0x2B };
		final String hexString = TextHelper.convertByteArrayToHexString(byteArray);
		assertEquals("0f1a2b", hexString);
	}

	@Test
	void testEncryptAndDecryptData() throws Exception {
		final byte[] key = "1234567890123456".getBytes();
		final byte[] data = "TestData".getBytes();

		final byte[] encryptedData = TextHelper.encryptData(data, key);
		assertNotNull(encryptedData);

		final byte[] decryptedData = TextHelper.decryptData(encryptedData, key);
		assertArrayEquals(data, decryptedData);
	}

	@Test
	void testGetJsonFromMap() {
		final Map<String, String> map = new HashMap<>();
		map.put("key1", "value1");
		map.put("key2", "value2");

		final JSONObject json = TextHelper.getJsonFromMap(map);
		assertEquals("value1", json.getString("key1"));
		assertEquals("value2", json.getString("key2"));
	}

	@Test
	void testGetMapFromJson() {
		final JSONObject json = new JSONObject();
		json.put("key1", "value1");
		json.put("key2", "value2");

		final Map<String, String> map = TextHelper.getMapFromJson(json);
		assertEquals("value1", map.get("key1"));
		assertEquals("value2", map.get("key2"));
	}

	@Test
	void testJoinCollection() {
		final List<String> data = Arrays.asList("one", "two", "three");
		final String result = TextHelper.joinCollection(data, ", ");
		assertEquals("one, two, three", result);
	}

	@Test
	void testObjectToStringAndBack() throws Exception {
		final Serializable object = "TestString";
		final String serialized = TextHelper.objectToString(object);
		assertNotNull(serialized);

		final String deserialized = TextHelper.objectFromString(serialized, String.class);
		assertEquals(object, deserialized);
	}

	@Test
	void testSplitFixSize() {
		final String text = "1234567890";
		final List<String> chunks = TextHelper.splitFixSize(text, 3);
		assertEquals(Arrays.asList("123", "456", "789", "0"), chunks);
	}
}