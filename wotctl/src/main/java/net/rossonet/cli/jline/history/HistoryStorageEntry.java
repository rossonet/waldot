package net.rossonet.cli.jline.history;

import java.io.Serializable;
import java.time.Instant;

import org.jline.reader.History.Entry;

public class HistoryStorageEntry implements Entry, Serializable {

	private static final long serialVersionUID = 4962136314405484784L;
	private final int index;
	private final String line;
	private final Instant time;
	private final String tty;
	private final String user;

	public HistoryStorageEntry(final String user, String tty, final int index, final Instant time, final String line) {
		this.user = user;
		this.index = index;
		this.time = time;
		this.line = line;
		this.tty = tty;
	}

	public String getTty() {
		return tty;
	}

	public String getUser() {
		return user;
	}

	@Override
	public int index() {
		return index;
	}

	@Override
	public String line() {
		return line;
	}

	@Override
	public Instant time() {
		return time;
	}

	@Override
	public String toString() {
		return String.format("%s@%d: %s", user, index, line);
	}

}
