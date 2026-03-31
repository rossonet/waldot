
package net.rossonet.agent;

import picocli.CommandLine;

/**
 * Main entry point for the WaldOT application.
 * 
 * <p>This class serves as the bootstrap for the WaldOT OPC UA server application.
 * It initializes the Picocli command-line interface and delegates execution to
 * {@link WaldotRunner}, which handles all server configuration and startup.</p>
 * 
 * <p><b>Execution Flow</b>:</p>
 * <ol>
 *   <li>Parses command-line arguments using Picocli</li>
 *   <li>Creates and configures {@link WaldotRunner} instance</li>
 *   <li>Executes the runner to start the OPC UA server</li>
 *   <li>Exits with appropriate status code</li>
 * </ol>
 * 
 * <p><b>Usage Examples</b>:</p>
 * <pre>{@code
 * // Run with default configuration
 * java -cp /app/lib/* net.rossonet.agent.MainAgent
 * 
 * // Run with custom TCP port
 * java -cp /app/lib/* net.rossonet.agent.MainAgent --tcp-port 4840
 * 
 * // Show help
 * java -cp /app/lib/* net.rossonet.agent.MainAgent --help
 * 
 * // Docker execution
 * docker run rossonet/waldot --tcp-port 4840 --anonymous-access false
 * }</pre>
 * 
 * <p><b>Exit Codes</b>:</p>
 * <ul>
 *   <li>0 - Successful execution</li>
 *   <li>1 - Configuration error</li>
 *   <li>2 - Runtime error</li>
 * </ul>
 * 
 * @author Andrea Ambrosini - Rossonet s.c.a.r.l.
 * @see WaldotRunner
 * @see <a href="https://picocli.info/">Picocli Documentation</a>
 */
public class MainAgent {
	
	/**
	 * Default bootstrap configuration file path.
	 * 
	 * <p>Percorso predefinito del file di configurazione bootstrap che viene
	 * caricato all'avvio del server per inizializzare la struttura del grafo.</p>
	 * 
	 * <p>This path is used as the default value for the {@code --boot-url} parameter
	 * in {@link WaldotRunner}. The file at this location should contain graph
	 * initialization commands in the supported format.</p>
	 * 
	 * <p><b>Docker Note</b>: In containerized deployments, mount a custom boot
	 * configuration file at this path or override with {@code --boot-url}.</p>
	 */
	public static final String DEFAULT_FILE_CONFIGURATION_PATH = "file:///waldot/boot.conf";

	/**
	 * Application main entry point.
	 * 
	 * <p>Questo metodo è il punto di ingresso dell'applicazione. Viene chiamato
	 * quando l'applicazione viene avviata da linea di comando o in un container Docker.</p>
	 * 
	 * <p>The method performs the following operations:</p>
	 * <ol>
	 *   <li>Creates a new {@link WaldotRunner} instance</li>
	 *   <li>Wraps it in a Picocli {@link CommandLine} for argument parsing</li>
	 *   <li>Executes the command with provided arguments</li>
	 *   <li>Prints the exit code</li>
	 *   <li>Terminates the JVM with the exit code</li>
	 * </ol>
	 * 
	 * <p><b>Command-Line Arguments</b>:</p>
	 * <p>All arguments are passed to {@link WaldotRunner} where they are processed
	 * by Picocli annotations. See {@link WaldotRunner} for available options.</p>
	 * 
	 * <p><b>Examples</b>:</p>
	 * <pre>{@code
	 * // Start with defaults
	 * MainAgent.main();
	 * 
	 * // Start with custom port
	 * MainAgent.main("--tcp-port", "4840");
	 * 
	 * // Multiple options
	 * MainAgent.main("--tcp-port", "4840", "--anonymous-access", "false");
	 * }</pre>
	 * 
	 * @param args command-line arguments passed to the application
	 */
	public static void main(final String... args) {
		// Crea l'istanza del runner che gestisce la configurazione e l'avvio del server
		final int exitCode = new CommandLine(new WaldotRunner()).execute(args);
		
		// Stampa il codice di uscita per debugging
		System.out.println("Exit code: " + exitCode);
		
		// Termina la JVM con il codice di uscita appropriato
		System.exit(exitCode);
	}
}
