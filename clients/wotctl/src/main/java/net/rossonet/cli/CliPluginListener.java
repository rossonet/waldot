package net.rossonet.cli;

import java.util.Map;

import picocli.CommandLine;

public interface CliPluginListener {

	Map<String, CommandLine> getCommands();

}
