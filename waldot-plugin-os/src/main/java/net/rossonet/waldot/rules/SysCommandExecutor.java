package net.rossonet.waldot.rules;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import net.rossonet.waldot.utils.SystemCommandHelper;
import net.rossonet.waldot.utils.SystemCommandHelper.QuotedStringTokenizer;

public class SysCommandExecutor {

	public String exec(final String command) {
		final String[] commandArray = new QuotedStringTokenizer(command).getTokens().toArray(new String[0]);
		final StringBuilder sb = new StringBuilder();
		final Consumer<String> consumer = new Consumer<String>() {
			@Override
			public void accept(final String line) {
				sb.append(line + System.lineSeparator());
			}
		};
		try {
			SystemCommandHelper.executeSystemCommandAndWait(new File(System.getProperty("user.home")), commandArray,
					consumer, 10000);
			return sb.toString();
		} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
			return "Error executing command: " + e.getMessage();
		}
	}

	@Override
	public String toString() {
		return "use the exec() method to execute system commands";
	}
}
