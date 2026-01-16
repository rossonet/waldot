package net.rossonet.cli;

import java.util.HashMap;
import java.util.Map;

import picocli.CommandLine;

public final class CommandRegister {

	private final Map<String, CommandLine> commands = new HashMap<>();

	public void clearCommands() {
		commands.clear();
	}

	public Map<String, CommandLine> getCommands() {
		return commands;
	}

	public int getRegisteredCommandsCount() {
		return commands.size();
	}

	public boolean isCommandRegistered(CommandLine command) {
		return commands.containsValue(command);
	}

	public CommandLine registerCommand(String name, CommandLine command) {
		commands.put(name, command);
		return command;
	}

	public boolean unregisterCommand(String name) {
		if (!commands.containsKey(name)) {
			return false;
		} else {
			commands.remove(name);
			return true;
		}
	}

}
