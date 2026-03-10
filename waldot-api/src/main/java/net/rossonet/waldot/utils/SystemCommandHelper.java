package net.rossonet.waldot.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
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

	/**
	 * Tokenizer for parsing command lines with quoted strings.
	 * 
	 * @author Andrea Ambrosini - Rossonet s.c.a.r.l.
	 */
	public static class QuotedStringTokenizer {

		private int index;
		private final String line;

		private final Matcher matcher;

		private final List<String> tokens = new ArrayList<>();

		/**
		 * Creates a QuotedStringTokenizer that parses the given line.
		 * 
		 * @param line the line to tokenize
		 */
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

		/**
		 * Returns the original line without trimming.
		 * 
		 * @return the original line
		 */
		public String getLine() {
			return line;
		}

		/**
		 * Returns the list of tokens extracted from the line.
		 * 
		 * @return list of string tokens
		 */
		public List<String> getTokens() {
			return tokens;
		}

		/**
		 * Checks if there are more tokens available.
		 * 
		 * @return true if more tokens exist, false otherwise
		 */
		public boolean hasMoreTokens() {
			return index < tokens.size();
		}

		/**
		 * Returns the next token and advances the index.
		 * 
		 * @return the next token
		 * @throws IndexOutOfBoundsException if there are no more tokens
		 */
		public String nextToken() {
			final String data = tokens.get(index);
			index++;
			return data;
		}

	}

	/**
	 * Runnable that consumes input and error streams from a process.
	 * 
	 * @author Andrea Ambrosini - Rossonet s.c.a.r.l.
	 */
	public static class StreamGobbler implements Runnable {
		private final Consumer<String> consumer;
		private final InputStream errorStream;
		private final InputStream inputStream;

		/**
		 * Creates a StreamGobbler to consume process streams.
		 * 
		 * @param inputStream  the standard output stream
		 * @param errorStream  the standard error stream
		 * @param consumer     the consumer to process each line
		 */
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

	/**
	 * Executes a system command and waits for completion.
	 * 
	 * @param baseDirectory       the working directory for the command
	 * @param command            the command and arguments to execute
	 * @param consumer           consumer for processing command output lines
	 * @param timeoutMilliSeconds timeout in milliseconds, or 0 for no timeout
	 * @throws IOException          if an I/O error occurs
	 * @throws InterruptedException if the operation is interrupted
	 * @throws ExecutionException   if the computation threw an exception
	 * @throws TimeoutException     if the timeout expired
	 */
	public static void executeSystemCommandAndWait(final File baseDirectory, final String[] command,
			final Consumer<String> consumer, final int timeoutMilliSeconds)
			throws IOException, InterruptedException, ExecutionException, TimeoutException {
		final ProcessBuilder builder = new ProcessBuilder();
		builder.command(command);
		builder.directory(baseDirectory);
		final Process process = builder.start();
		final StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), process.getErrorStream(),
				consumer);
		final Future<?> future = ThreadHelper.newVirtualThreadExecutor().submit(streamGobbler);
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
