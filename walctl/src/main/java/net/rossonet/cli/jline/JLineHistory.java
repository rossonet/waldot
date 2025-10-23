package net.rossonet.cli.jline;

import static org.jline.reader.impl.ReaderUtils.getBoolean;
import static org.jline.reader.impl.ReaderUtils.isSet;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import java.util.logging.Logger;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.utils.Log;

import net.rossonet.cli.BrandSelector;
import net.rossonet.cli.jline.history.HistoryStorage;
import net.rossonet.cli.jline.history.HistoryStorageEntry;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.waldot.utils.SystemCommandHelper.QuotedStringTokenizer;

public class JLineHistory implements History {

	private static final Logger LOGGER = Logger.getLogger(JLineHistory.class.getName());

	private static String obfuscateUserEncode(String line) {
		try {
			final QuotedStringTokenizer tokens = new QuotedStringTokenizer(line);
			if (tokens.hasMoreTokens() && tokens.nextToken().equals("user") && tokens.hasMoreTokens()
					&& tokens.nextToken().equals("encode")) {
				line = "user encode xxxx";
			}
		} catch (final Exception a) {
			LOGGER.severe(LogHelper.stackTraceToString(a));
		}
		return line;
	}

	private int index = 0;

	private LinkedList<Entry> items = new LinkedList<>();

	private int offset = 0;

	private LineReader reader;

	private final HistoryStorage storageProvider;

	private final String tty;

	private final String username;

	public JLineHistory(final HistoryStorage storageProvider, String username, String tty) {
		this.storageProvider = storageProvider;
		this.username = username != null ? username : System.getProperty("user.name", "unknown");
		this.tty = tty != null ? tty : "unknown";
	}

	@Override
	public void add(final Instant time, String line) {
		line = obfuscateUserEncode(line);
		Objects.requireNonNull(time);
		Objects.requireNonNull(line);
		if (getBoolean(reader, LineReader.DISABLE_HISTORY, false)) {
			return;
		}
		if (isSet(reader, LineReader.Option.HISTORY_IGNORE_SPACE) && line.startsWith(" ")) {
			return;
		}
		if (isSet(reader, LineReader.Option.HISTORY_REDUCE_BLANKS)) {
			line = line.trim();
		}
		if (isSet(reader, LineReader.Option.HISTORY_IGNORE_DUPS)) {
			if (!items.isEmpty() && line.equals(items.getLast().line())) {
				return;
			}
		}

		internalAdd(time, line);
		if (isSet(reader, LineReader.Option.HISTORY_INCREMENTAL)) {
			try {
				save();
			} catch (final IOException e) {
				Log.warn("Failed to save history", e);
			}
		}

	}

	@Override
	public void append(final Path file, final boolean incremental) throws IOException {
		internalWrite(incremental);

	}

	@Override
	public void attach(final LineReader reader) {
		if (this.reader != reader) {
			this.reader = reader;
			try {
				load();
				moveToEnd();
			} catch (IllegalArgumentException | IOException e) {
				LOGGER.severe("Failed to load history" + e.toString());
			}
		}

	}

	@Override
	public String current() {
		if (index >= size()) {
			return "";
		}
		return items.get(index).line();
	}

	private void deleteData() {
		getStorageProvider().remove();

	}

	@Override
	public int first() {
		return offset;
	}

	private void forceSaveData() {
		getStorageProvider().saveState(items);
	}

	@Override
	public String get(final int index) {
		final int idx = index - offset;
		if (idx >= items.size() || idx < 0) {
			throw new IllegalArgumentException("IndexOutOfBounds: Index:" + idx + ", Size:" + items.size());
		}
		return items.get(idx).line();
	}

	public String getCommandHistory(final boolean colored, final BrandSelector brandSelector) {
		final StringBuilder sb = new StringBuilder();
		sb.append("commands history\n\n");
		for (final Entry item : items) {
			final HistoryStorageEntry line = (HistoryStorageEntry) item;
			sb.append(line.index() + "\t" + (colored ? brandSelector.getHistoryUserColor() : "") + line.getUser()
					+ (colored ? TerminalHelper.ANSI_RESET : "") + "@"
					+ (colored ? brandSelector.getHistoryRemoteAddressColor() : "") + line.getTty()
					+ (colored ? TerminalHelper.ANSI_RESET : "") + " at [" + line.time() + "] -> "
					+ (colored ? brandSelector.getHistoryLineColor() : "") + line.line()
					+ (colored ? TerminalHelper.ANSI_RESET : "") + "\n");
		}
		return sb.toString();
	}

	private HistoryStorage getStorageProvider() {
		return storageProvider;
	}

	@Override
	public int index() {
		return offset + index;
	}

	private void internalAdd(final Instant time, final String line) {
		internalAdd(time, line, false);
	}

	private void internalAdd(final Instant time, final String line, final boolean checkDuplicates) {
		final Entry entry = new HistoryStorageEntry(username, tty, offset + items.size(), time, line);
		if (checkDuplicates) {
			for (final Entry e : items) {
				if (e.line().trim().equals(line.trim())) {
					return;
				}
			}
		}
		items.add(entry);
	}

	private void internalRead(final boolean checkDuplicates) {
		final LinkedList<Entry> storageDatas = getStorageProvider().getValues();
		items = storageDatas != null ? storageDatas : new LinkedList<>();
	}

	private void internalWrite(final boolean incremental) {
		getStorageProvider().saveState(incremental, items);
	}

	@Override
	public ListIterator<Entry> iterator(final int index) {
		return items.listIterator(index - offset);
	}

	@Override
	public int last() {
		return offset + items.size() - 1;
	}

	@Override
	public void load() throws IOException {
		internalRead(false);

	}

	@Override
	public boolean moveTo(int index) {
		index -= offset;
		if (index >= 0 && index < size()) {
			this.index = index;
			return true;
		}
		return false;
	}

	@Override
	public void moveToEnd() {
		index = size();
	}

	@Override
	public boolean moveToFirst() {
		if (size() > 0 && index != 0) {
			index = 0;
			return true;
		}
		return false;
	}

	@Override
	public boolean moveToLast() {
		final int lastEntry = size() - 1;
		if (lastEntry >= 0 && lastEntry != index) {
			index = size() - 1;
			return true;
		}

		return false;
	}

	@Override
	public boolean next() {
		if (index >= size()) {
			return false;
		}
		index++;
		return true;
	}

	@Override
	public boolean previous() {
		if (index <= 0) {
			return false;
		}
		index--;
		return true;
	}

	@Override
	public void purge() throws IOException {
		offset = 0;
		index = 0;
		deleteData();
		items.clear();

	}

	@Override
	public void read(final Path file, final boolean checkDuplicates) throws IOException {
		internalRead(checkDuplicates);

	}

	@Override
	public void resetIndex() {
		index = Math.min(index, items.size());

	}

	@Override
	public void save() throws IOException {
		internalWrite(false);
		forceSaveData();
	}

	@Override
	public int size() {
		return items.size();
	}

	@Override
	public void write(final Path file, final boolean incremental) throws IOException {
		internalWrite(incremental);
		forceSaveData();
	}

}
