
package net.rossonet.agent;

import net.rossonet.cmd.BasePicocliRunner;

/**
 * Classe main per avvio applicazione
 *
 * @author Andrea Ambrosini
 */
public class MainAgent {

	private static BasePicocliRunner instance = null;

	public static void main(final String[] args) {
		instance = BasePicocliRunner.getNewInstance();
		instance.execute(args);
		System.exit(instance.waitCompletionAndGetReturnCode());
	}
}
