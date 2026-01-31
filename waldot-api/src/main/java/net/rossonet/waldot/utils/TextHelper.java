package net.rossonet.waldot.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;

import net.rossonet.waldot.utils.text.PlaceHolder;

/**
 * TextHelper provides utility methods for text manipulation, encryption, and
 * decryption. It includes methods for handling placeholders in text, converting
 * byte arrays to hex strings, and performing encryption and decryption using a
 * specified algorithm.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public final class TextHelper {

	// Regular Colors
	public static final String ANSI_BLACK = "\033[0;30m"; // BLACK
	// Background
	public static final String ANSI_BLACK_BACKGROUND = "\033[40m"; // BLACK
	// High Intensity backgrounds
	public static final String ANSI_BLACK_BACKGROUND_BRIGHT = "\033[0;100m";// BLACK
	// Bold
	public static final String ANSI_BLACK_BOLD = "\033[1;30m"; // BLACK
	// Bold High Intensity
	public static final String ANSI_BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
	// High Intensity
	public static final String ANSI_BLACK_BRIGHT = "\033[0;90m"; // BLACK
	// Underline
	public static final String ANSI_BLACK_UNDERLINED = "\033[4;30m"; // BLACK
	public static final String ANSI_BLUE = "\033[0;34m"; // BLUE
	public static final String ANSI_BLUE_BACKGROUND = "\033[44m"; // BLUE

	public static final String ANSI_BLUE_BACKGROUND_BRIGHT = "\033[0;104m";// BLUE
	public static final String ANSI_BLUE_BOLD = "\033[1;34m"; // BLUE
	public static final String ANSI_BLUE_BOLD_BRIGHT = "\033[1;94m"; // BLUE
	public static final String ANSI_BLUE_BRIGHT = "\033[0;94m"; // BLUE
	public static final String ANSI_BLUE_UNDERLINED = "\033[4;34m"; // BLUE
	public static final String ANSI_CYAN = "\033[0;36m"; // CYAN
	public static final String ANSI_CYAN_BACKGROUND = "\033[46m"; // CYAN
	public static final String ANSI_CYAN_BACKGROUND_BRIGHT = "\033[0;106m"; // CYAN

	public static final String ANSI_CYAN_BOLD = "\033[1;36m"; // CYAN
	public static final String ANSI_CYAN_BOLD_BRIGHT = "\033[1;96m"; // CYAN
	public static final String ANSI_CYAN_BRIGHT = "\033[0;96m"; // CYAN
	public static final String ANSI_CYAN_UNDERLINED = "\033[4;36m"; // CYAN
	public static final String ANSI_GREEN = "\033[0;32m"; // GREEN
	public static final String ANSI_GREEN_BACKGROUND = "\033[42m"; // GREEN
	public static final String ANSI_GREEN_BACKGROUND_BRIGHT = "\033[0;102m";// GREEN
	public static final String ANSI_GREEN_BOLD = "\033[1;32m"; // GREEN

	public static final String ANSI_GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
	public static final String ANSI_GREEN_BRIGHT = "\033[0;92m"; // GREEN
	public static final String ANSI_GREEN_UNDERLINED = "\033[4;32m"; // GREEN
	public static final String ANSI_PURPLE = "\033[0;35m"; // PURPLE
	public static final String ANSI_PURPLE_BACKGROUND = "\033[45m"; // PURPLE
	public static final String ANSI_PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
	public static final String ANSI_PURPLE_BOLD = "\033[1;35m"; // PURPLE
	public static final String ANSI_PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE

	public static final String ANSI_PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
	public static final String ANSI_PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
	public static final String ANSI_RED = "\033[0;31m"; // RED
	public static final String ANSI_RED_BACKGROUND = "\033[41m"; // RED
	public static final String ANSI_RED_BACKGROUND_BRIGHT = "\033[0;101m";// RED
	public static final String ANSI_RED_BOLD = "\033[1;31m"; // RED
	public static final String ANSI_RED_BOLD_BRIGHT = "\033[1;91m"; // RED
	public static final String ANSI_RED_BRIGHT = "\033[0;91m"; // RED

	public static final String ANSI_RED_UNDERLINED = "\033[4;31m"; // RED
	// Reset
	public static final String ANSI_RESET = "\033[0m"; // Text Reset
	public static final String ANSI_WHITE = "\033[0;37m"; // WHITE
	public static final String ANSI_WHITE_BACKGROUND = "\033[47m"; // WHITE
	public static final String ANSI_WHITE_BACKGROUND_BRIGHT = "\033[0;107m"; // WHITE
	public static final String ANSI_WHITE_BOLD = "\033[1;37m"; // WHITE
	public static final String ANSI_WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE
	public static final String ANSI_WHITE_BRIGHT = "\033[0;97m"; // WHITE

	public static final String ANSI_WHITE_UNDERLINED = "\033[4;37m"; // WHITE
	public static final String ANSI_YELLOW = "\033[0;33m"; // YELLOW
	public static final String ANSI_YELLOW_BACKGROUND = "\033[43m"; // YELLOW
	public static final String ANSI_YELLOW_BACKGROUND_BRIGHT = "\033[0;103m";// YELLOW
	public static final String ANSI_YELLOW_BOLD = "\033[1;33m"; // YELLOW
	public static final String ANSI_YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
	public static final String ANSI_YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
	public static final String ANSI_YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW

	private static String encryptionAlgorithm = "AES";

	public static String cleanText(String dirtyText) {
		return dirtyText.toLowerCase().trim().replaceAll("[^\\w;:]+", "_").replaceAll("[ ]+", "_");
	}

	public static String convertByteArrayToHexString(final byte[] arrayBytes) {
		final StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < arrayBytes.length; i++) {
			stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return stringBuffer.toString();
	}

	public static byte[] decryptData(final byte[] encryptedData, final byte[] key) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		return decryptData(encryptedData, key, encryptionAlgorithm);
	}

	public static byte[] decryptData(final byte[] encryptedData, final byte[] key, final String encryptionAlgorithm)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException {
		final Cipher c = Cipher.getInstance(encryptionAlgorithm);
		final SecretKeySpec k = new SecretKeySpec(key, encryptionAlgorithm);
		c.init(Cipher.DECRYPT_MODE, k);
		return c.doFinal(encryptedData);
	}

	public static byte[] encryptData(final byte[] dataToEncrypt, final byte[] key) throws IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		return encryptData(dataToEncrypt, key, encryptionAlgorithm);
	}

	public static byte[] encryptData(final byte[] dataToEncrypt, final byte[] key, final String encryptionAlgorithm)
			throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException {
		final Cipher c = Cipher.getInstance(encryptionAlgorithm);
		final SecretKeySpec k = new SecretKeySpec(key, encryptionAlgorithm);
		c.init(Cipher.ENCRYPT_MODE, k);
		return c.doFinal(dataToEncrypt);
	}

	public static Map<String, PlaceHolder> extractPlaceHolderFromText(final String originalText,
			final String startPlaceholderText, final String stopPlaceholderText) {
		final Pattern pattern = Pattern.compile(startPlaceholderText + ".+?" + stopPlaceholderText);
		return extractPlaceHolderFromText(originalText, startPlaceholderText, stopPlaceholderText, pattern);
	}

	public static Map<String, PlaceHolder> extractPlaceHolderFromText(final String originalText,
			final String startPlaceholderText, final String stopPlaceholderText, final Pattern pattern) {
		final Matcher m = pattern.matcher(originalText);
		final Map<String, PlaceHolder> placeHolders = new HashMap<>();
		if (m.find()) {
			do {
				final String placeholder = m.group();
				final PlaceHolder dataPlaceholder = new PlaceHolder(placeholder, startPlaceholderText,
						stopPlaceholderText);
				if (dataPlaceholder.getDataWithoutPlaceholderTag() != null
						&& !dataPlaceholder.getDataWithoutPlaceholderTag().isEmpty()) {
					if (!placeHolders.containsKey(dataPlaceholder.getDataWithoutPlaceholderTag())) {
						placeHolders.put(dataPlaceholder.getDataWithoutPlaceholderTag(), dataPlaceholder);
					}
				}
			} while (m.find());
		}
		return placeHolders;
	}

	public static String getDefaultEncryptionAlgorithm() {
		return encryptionAlgorithm;
	}

	public static JSONObject getJsonFromMap(final Map<String, String> map) {
		final JSONObject json = new JSONObject();
		for (final String key : map.keySet()) {
			json.put(key, map.get(key));
		}
		return json;
	}

	public static Map<String, String> getMapFromJson(final JSONObject jsonMap) {
		final Map<String, String> map = new HashMap<>();
		for (final String key : jsonMap.keySet()) {
			map.put(key, jsonMap.getString(key));
		}
		return map;
	}

	public static Map<String, String> getParametersInUrlQuery(final String query) {
		final String[] params = query.split("&");
		final Map<String, String> map = new HashMap<String, String>();

		for (final String param : params) {
			final String name = param.split("=")[0];
			final String value = param.split("=")[1];
			map.put(name, value);
		}
		return map;

	}

	public static boolean isDirtyValue(final String value) {
		return !value.matches("[^\\w+]+") && !value.matches("[ ]+");
	}

	public static String joinCollection(final Collection<?> data, final String separator) {
		if (data.isEmpty()) {
			return "";
		}
		final StringBuilder result = new StringBuilder();
		boolean first = true;
		for (final Object d : data.toArray()) {
			if (first) {
				first = false;
			} else {
				result.append(separator);
			}
			result.append(d);
		}
		return result.toString();
	}

	@SuppressWarnings("unchecked")
	public static <O extends Serializable> O objectFromString(final String string, final Class<O> clazz)
			throws IOException, ClassNotFoundException {
		final byte[] data = Base64.getDecoder().decode(string);
		final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		final Object o = ois.readObject();
		ois.close();
		return (O) o;
	}

	public static String objectToString(final Serializable object) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(object);
		oos.close();
		return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	public static String popolateTextPlaceholdersFromData(final String originalText, final Map<String, String> data,
			final String startPlaceholderText, final String stopPlaceholderText) {
		final Pattern pattern = Pattern.compile(startPlaceholderText + ".+?" + stopPlaceholderText);
		return popolateTextPlaceholdersFromData(originalText, data, startPlaceholderText, stopPlaceholderText, pattern);
	}

	public static String popolateTextPlaceholdersFromData(final String originalText, final Map<String, String> data,
			final String startPlaceholderText, final String stopPlaceholderText, final Pattern PATTERN) {
		final Matcher m = PATTERN.matcher(originalText);
		final StringBuilder reply = new StringBuilder();
		if (m.find()) {
			int indexStart = 0;
			do {
				reply.append(originalText.substring(indexStart, m.start()));
				final String placeHoldername = m.group().replaceAll("^" + startPlaceholderText, "")
						.replaceAll(stopPlaceholderText + "$", "");
				if (data.containsKey(placeHoldername)) {
					reply.append(data.get(placeHoldername));
				} else {
					new UnsupportedOperationException("found placeholder without key: " + placeHoldername);
				}
				indexStart = m.end();
			} while (m.find());
			reply.append(originalText.substring(indexStart, originalText.length()));
		} else {
			reply.append(originalText);
		}
		return reply.toString();
	}

	public static void setDefaultEncryptionAlgorithm(final String encryptionAlgorithm) {
		TextHelper.encryptionAlgorithm = encryptionAlgorithm;
	}

	public static List<String> splitFixSize(final String s, final int chunkSize) {
		final List<String> chunks = new ArrayList<>();
		for (int i = 0; i < s.length(); i += chunkSize) {
			chunks.add(s.substring(i, Math.min(s.length(), i + chunkSize)));
		}
		return chunks;
	}

	private TextHelper() {
		throw new UnsupportedOperationException("Just for static usage");
	}

}
