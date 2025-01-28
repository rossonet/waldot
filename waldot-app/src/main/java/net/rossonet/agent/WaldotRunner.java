package net.rossonet.agent;

import java.util.concurrent.ExecutionException;

import net.rossonet.waldot.WaldotOpcUaServer;
import net.rossonet.waldot.configuration.HomunculusConfiguration;
import net.rossonet.waldot.configuration.OpcUaConfiguration;

public class WaldotRunner implements AutoCloseable {
	// TODO completare avvio da linea di comando annotando la classe con picocli

	public static void main(String[] args) {
		final WaldotRunner waldotRunner = new WaldotRunner();
		try {
			waldotRunner.runWaldot();
			System.out.println("bye, bye from WaldOT");
			System.exit(0);
		} catch (final Exception e) {
			if (waldotRunner != null) {
				try {
					waldotRunner.close();
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		}
	}

	private WaldotOpcUaServer waldot;

	@Override
	public void close() throws Exception {
		if (waldot != null) {
			waldot.shutdown();
		}
	}

	private void runWaldot() throws InterruptedException, ExecutionException {
		// TODO costruire classe e builder per integrare come libreria di TinkerPop
		final HomunculusConfiguration configuration = HomunculusConfiguration.getDefault();
		final OpcUaConfiguration serverConfiguration = OpcUaConfiguration.getDefault();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					if (waldot != null) {
						waldot.shutdown();
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		});
		waldot = new WaldotOpcUaServer(configuration, serverConfiguration);
		waldot.startup().get();
		waldot.waitCompletion();
	}

}