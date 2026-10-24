package net.rossonet.agent;

import picocli.CommandLine.Command;

/**
 * WaldOT standard runner. All options and the bootstrap logic live in
 * {@link AbstractWaldotRunner} (module waldot-runner).
 */
@Command(name = "WaldOT", mixinStandardHelpOptions = true, version = { "${COMMAND-NAME} 1.0",
		"JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
		"OS: ${os.name} ${os.version} ${os.arch}" }, description = "WaldOT OPC UA server with TinkerPop graph integration", footer = "Powered by Zona Autonoma srl - https://www.rossonet.net", showEndOfOptionsDelimiterInUsageHelp = true, showAtFileInUsageHelp = true)
public class WaldotRunner extends AbstractWaldotRunner {

	/**
	 * Main entry point for the WaldOT application.
	 *
	 * @param args command-line arguments
	 */
	public static void main(final String[] args) {
		final WaldotRunner waldotRunner = new WaldotRunner();
		try {
			waldotRunner.runWaldot();
			logger.info("bye, bye from WaldOT");
			System.exit(0);
		} catch (final Exception e) {
			try {
				waldotRunner.close();
			} catch (final Exception e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
}
