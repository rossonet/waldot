package net.rossonet.waldot.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SystemCommandHelper is a utility class that provides methods to execute
 * system commands and handle quoted strings in command lines. It includes a
 * tokenizer for parsing quoted strings and a stream gobbler for consuming
 * process output.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class SystemCommandHelper {

	public static class QuotedStringTokenizer {

		private final String line;
		private final List<String> tokens = new ArrayList<>();

		private final Matcher matcher;

		private int index;

		public QuotedStringTokenizer(final String line) {
			this.line = line.trim();
			matcher = QUOTED_PATTERN.matcher(line);
			while (!matcher.hitEnd()) {
				String data;
				if (matcher.find()) {
					if (matcher.group(1) != null) {
						data = matcher.group(1);
					} else {
						data = matcher.group(2);
					}
					tokens.add(data);
				}
			}
		}

		public String getLine() {
			return line;
		}

		public List<String> getTokens() {
			return tokens;
		}

		public boolean hasMoreTokens() {
			return index < tokens.size();
		}

		public String nextToken() {
			final String data = tokens.get(index);
			index++;
			return data;
		}

	}

	public static class StreamGobbler implements Runnable {
		private final InputStream inputStream;
		private final Consumer<String> consumer;
		private final InputStream errorStream;

		public StreamGobbler(final InputStream inputStream, final InputStream errorStream,
				final Consumer<String> consumer) {
			this.inputStream = inputStream;
			this.errorStream = errorStream;
			this.consumer = consumer;
		}

		@Override
		public void run() {
			new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
			new BufferedReader(new InputStreamReader(errorStream)).lines().forEach(consumer);
		}
	}

	private final static Pattern QUOTED_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

	public static void executeSystemCommandAndWait(final File baseDirectory, final String[] command,
			final Consumer<String> consumer, final int timeoutMilliSeconds)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		final ProcessBuilder builder = new ProcessBuilder();
		builder.command(command);
		builder.directory(baseDirectory);
		final Process process = builder.start();
		final StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), process.getErrorStream(),
				consumer);
		final Future<?> future = Executors.newSingleThreadExecutor().submit(streamGobbler);
		if (timeoutMilliSeconds != 0) {
			future.get(timeoutMilliSeconds, TimeUnit.MILLISECONDS);
		} else {
			future.get();
		}
	}

	private SystemCommandHelper() {
		throw new UnsupportedOperationException("Just for static usage");
	}

}
