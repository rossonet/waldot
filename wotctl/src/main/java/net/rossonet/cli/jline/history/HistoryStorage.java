package net.rossonet.cli.jline.history;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.jline.reader.History.Entry;

import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.TextHelper;

public class HistoryStorage {

	@SuppressWarnings("unused")
	private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

	private static byte[] encHistoryKey = "2ee7eDdtH6h45_7_.9?g!!.8".getBytes();

	private final static Logger LOGGER = Logger.getLogger(HistoryStorage.class.getName());

	public static String decrypt(final String algorithm, final String cipherText, final SecretKey key,
			final IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		final Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.DECRYPT_MODE, key, iv);
		final byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
		return new String(plainText);
	}

	public static String encrypt(final String algorithm, final String input, final SecretKey key,
			final IvParameterSpec iv) throws NoSuchPaddingException, NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
		final Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		final byte[] cipherText = cipher.doFinal(input.getBytes());
		return Base64.getEncoder().encodeToString(cipherText);
	}

	public static final IvParameterSpec generateIv() {
		final byte[] iv = new byte[16];
		new SecureRandom(encHistoryKey).nextBytes(iv);
		return new IvParameterSpec(iv);
	}

	public static SecretKey generateKey() {
		try {
			final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(128, new SecureRandom(encHistoryKey));
			final SecretKey key = keyGenerator.generateKey();
			return key;
		} catch (final NoSuchAlgorithmException e) {
			LOGGER.severe(LogHelper.stackTraceToString(e));
			return null;
		}
	}

	public static byte[] getENC_HISTORY_KEY() {
		return encHistoryKey;
	}

	public static void setENC_HISTORY_KEY(byte[] eNC_HISTORY_KEY) {
		encHistoryKey = eNC_HISTORY_KEY;
	}

	@SuppressWarnings("unused")
	private final IvParameterSpec ivParameterSpec = generateIv();

	@SuppressWarnings("unused")
	private final SecretKey key = generateKey();

	private boolean loaded = false;

	private final Path storagePath;

	public HistoryStorage(final Path storagePath) {
		this.storagePath = storagePath;
	}

	public Path getStoragePath() {
		return storagePath;
	}

	public LinkedList<Entry> getValues() {
		final LinkedList<Entry> values = new LinkedList<>();
		if (!loaded && Files.exists(storagePath)) {
			try {
				LOGGER.fine("history storage at " + storagePath.toString() + " exists");
				values.clear();
				final Stream<String> lines = Files.lines(storagePath);
				for (final String line : lines.collect(Collectors.toList())) {
					// final String decrypt = decrypt(ALGORITHM, line.replace("\n", ""), key,
					// ivParameterSpec);
					final HistoryStorageEntry entry = TextHelper.objectFromString(line, HistoryStorageEntry.class);
					values.add(entry);
				}
				LOGGER.finer("load " + values.size() + " records");
				lines.close();
				loaded = true;
			} catch (final Throwable e) {
				LOGGER.severe(LogHelper.stackTraceToString(e));
			}
		}
		return values;
	}

	public void remove() {
		try {
			Files.delete(storagePath);
		} catch (final Exception e) {
			LOGGER.severe(LogHelper.stackTraceToString(e));
		}

	}

	public void saveState(final boolean incremental, final LinkedList<Entry> items) {
		try {
			LOGGER.finer("try to save " + items.size() + " lines");
			final FileWriter fileWriter = new FileWriter(storagePath.toAbsolutePath().normalize().toString(),
					incremental);
			final PrintWriter printWriter = new PrintWriter(fileWriter);
			for (final Entry e : items) {
				final HistoryStorageEntry storageEntry = (HistoryStorageEntry) e;
				final String data = TextHelper.objectToString(storageEntry);
				printWriter.write(data + "\n");
				printWriter.flush();
			}
			printWriter.close();
		} catch (final Exception e) {
			LOGGER.severe(LogHelper.stackTraceToString(e));
		}

	}

	public void saveState(final LinkedList<Entry> items) {
		saveState(false, items);
	}
}
