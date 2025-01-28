package net.rossonet.agent;

public class ConsoleRunner implements AutoCloseable {
	// TODO completare avvio da linea di comando annotando la classe con picocli

	public static void main(String[] args) {
		final ConsoleRunner waldotRunner = new ConsoleRunner();
		try {
			// waldotRunner.runWaldot();
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

	@Override
	public void close() throws Exception {

	}

}