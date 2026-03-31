package net.rossonet.waldot.gremlin.opcgraph.strategies.boot;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import net.rossonet.waldot.api.annotation.WaldotBootstrapStrategy;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.BootstrapStrategy;
import net.rossonet.waldot.utils.LogHelper;

@WaldotBootstrapStrategy
public class SingleFileBootstrapStrategy implements BootstrapStrategy {

	private AgentStatus agentStatus = AgentStatus.INIT;
	private Logger logger;
	private WaldotNamespace waldotNamespace;

	@Override
	public void close() throws Exception {
		// nothing to close
	}

	/**
	 * Detects if the configuration should be executed as a complete script or
	 * line-by-line.
	 * 
	 * <p>
	 * Script mode is used when the content contains:
	 * </p>
	 * <ul>
	 * <li>Variable assignments: varName = value</li>
	 * <li>Function definitions: def functionName</li>
	 * <li>Groovy comments: double-slash or slash-star</li>
	 * <li>Multi-line method chains with proper indentation</li>
	 * <li>Control structures: if, for, while</li>
	 * </ul>
	 * 
	 * <p>
	 * Line-by-line mode is used for simple configurations with one command per line
	 * and hash comments (legacy format).
	 * </p>
	 * 
	 * @param content The full configuration file content
	 * @return true if script mode should be used, false for line-by-line mode
	 */
	private boolean detectScriptMode(final String content) {
		// Rimuovi commenti '#' per l'analisi (sono legacy line-mode)
		final String cleanContent = content.replaceAll("#[^\n]*\n", "\n");

		// Indicatori di script mode:

		// 1. Presenza di commenti Groovy (//, /* */)
		if (cleanContent.contains("//") || cleanContent.contains("/*")) {
			return true;
		}

		// 2. Definizioni di funzioni
		if (cleanContent.matches("(?s).*\\bdef\\s+\\w+\\s*\\(.*")) {
			return true;
		}

		// 3. Assegnazioni di variabili (ma non proprietà di oggetti)
		// Cerca pattern come "varName = " all'inizio di riga
		if (java.util.regex.Pattern.compile("^\\s*\\w+\\s*=(?!=)", java.util.regex.Pattern.MULTILINE)
				.matcher(cleanContent).find()) {
			return true;
		}

		// 4. Catene di metodi multi-linea con indentazione
		// Cerca pattern come ".property(...)\n .property(...)"
		if (cleanContent.matches("(?s).*\\.\\w+\\([^)]*\\)\\s*\n\\s+\\.\\w+\\(.*")) {
			return true;
		}

		// 5. Strutture di controllo
		if (cleanContent.matches("(?s).*(\\bif\\s*\\(|\\bfor\\s*\\(|\\bwhile\\s*\\(|\\btimes\\s*\\{).*")) {
			return true;
		}

		// 6. Closure/blocchi { }
		if (cleanContent.matches("(?s).*\\{[^}]{20,}\\}.*")) { // Blocchi con contenuto significativo
			return true;
		}

		// Default: usa line-by-line mode per retrocompatibilità
		return false;
	}

	@Override
	public AgentStatus getAgentStatus() {
		return agentStatus;
	}

	/**
	 * Loads and parses the bootstrap configuration from a URL.
	 * 
	 * <p>
	 * This method supports two configuration formats for maximum flexibility:
	 * </p>
	 * 
	 * <h3>Format 1: Line-by-line execution (legacy, backward compatible)</h3>
	 * <p>
	 * Each non-comment, non-empty line is executed as a separate Groovy expression.
	 * Lines starting with '#' are treated as comments and skipped.
	 * </p>
	 * 
	 * <pre>{@code
	 * # This is a comment
	 * graph.addVertex('id', 'sensor1', 'label', 'temp-sensor')
	 * graph.addVertex('id', 'sensor2', 'label', 'pressure-sensor')
	 * }</pre>
	 * 
	 * <h3>Format 2: Multi-line Groovy script (new format)</h3>
	 * <p>
	 * If the file contains multi-line constructs (variable assignments, functions,
	 * control structures), the entire content is treated as a single Groovy script.
	 * Comments with '//' or '/*' are preserved for the Groovy parser.
	 * </p>
	 * 
	 * <pre>{@code
	 * // Function to create a sensor
	 * def createSensor(name, type) {
	 *   g.addV('generator')
	 *     .property('type', 'generator')
	 *     .property('label', name)
	 *     .property('Algorithm', type)
	 *     .next()
	 * }
	 * 
	 * // Create multiple sensors
	 * createSensor('temp-sensor', 'sinusoidal')
	 * createSensor('pressure-sensor', 'random')
	 * 
	 * log.info("Sensors configured")
	 * }</pre>
	 * 
	 * <p>
	 * The method automatically detects which format to use by analyzing the
	 * content:
	 * <ul>
	 * <li>If it finds variable assignments (=), function definitions (def), or
	 * multi-line constructs, it uses script mode</li>
	 * <li>Otherwise, it uses line-by-line mode for backward compatibility</li>
	 * </ul>
	 * 
	 * <p>
	 * <b>Remote URLs</b>: The configuration can be loaded from:
	 * <ul>
	 * <li>Local files: {@code file:///path/to/boot.conf}</li>
	 * <li>HTTP/HTTPS: {@code https://example.com/config.groovy}</li>
	 * <li>Any URL supported by Java's URL class</li>
	 * </ul>
	 * 
	 * @param bootstrapUrl URL pointing to the configuration file
	 * @return Array of configuration strings to execute. In script mode, returns a
	 *         single-element array with the entire script. In line mode, returns an
	 *         array with one expression per element.
	 */
	private String[] getConfigurationfromUrl(final String bootstrapUrl) {
		try {
			final InputStream is = new URI(bootstrapUrl).toURL().openConnection().getInputStream();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			final StringBuilder fullContent = new StringBuilder();
			final List<String> individualLines = new ArrayList<>();
			String line;

			// Prima passata: leggi tutto il contenuto
			while ((line = reader.readLine()) != null) {
				fullContent.append(line).append("\n");

				// Mantieni le righe per il modo legacy (retrocompatibilità)
				// Solo se non sono commenti '#' o vuote
				if (!line.trim().startsWith("#") && !line.trim().isEmpty()) {
					individualLines.add(line.trim());
				}
			}
			reader.close();

			final String content = fullContent.toString();

			// Determina se usare il modo script o il modo line-by-line
			final boolean useScriptMode = detectScriptMode(content);

			if (useScriptMode) {
				// Modo script: ritorna tutto il contenuto come un singolo elemento
				logger.info("Bootstrap configuration loaded in SCRIPT MODE from URL: {}", bootstrapUrl);
				logger.debug("Script content length: {} characters", content.length());
				return new String[] { content };
			} else {
				// Modo line-by-line: retrocompatibilità
				if (individualLines.isEmpty()) {
					logger.warn("No valid bootstrap configuration found at URL: {}", bootstrapUrl);
					return new String[0];
				} else {
					logger.info("Bootstrap configuration loaded in LINE-BY-LINE MODE from URL: {}", bootstrapUrl);
					logger.debug("Number of commands: {}", individualLines.size());
					return individualLines.toArray(new String[0]);
				}
			}
		} catch (final Exception e) {
			logger.error("Error reading bootstrap configuration from URL: " + bootstrapUrl + "\n"
					+ LogHelper.stackTraceToString(e, 5));
			agentStatus = AgentStatus.FAULT;
		}
		return new String[0];
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		agentStatus = AgentStatus.INIT;
		logger = waldotNamespace.getBootLogger();
	}

	private void runBootConfiguration(final String[] configuration) {
		if (configuration != null && configuration.length > 0) {
			int num = 0;
			for (final String line : configuration) {
				if (line.startsWith("#") || line.isEmpty()) {
					continue;
				}
				num++;
				try {
					final Object runExpression = waldotNamespace.runExpression(line);
					if (runExpression != null) {
						logger.info("[" + num + "]: " + line + " ->\n" + runExpression + "\n");
					} else {
						logger.info("[" + num + "]: " + line + " -> no result\n");
					}
				} catch (final Exception e) {
					logger.error("Error executing command '" + line + "': " + e.getMessage());
				}
			}
			agentStatus = AgentStatus.READY;
			logger.info("Bootstrap procedure completed successfully.");
		} else {
			logger.warn("the bootstrap configuration is empty or null. No commands to execute.");
		}
	}

	@Override
	public void runBootstrapProcedure() {
		final String[] configuration = getConfigurationfromUrl(waldotNamespace.getBootstrapUrl());
		agentStatus = AgentStatus.BOOT;
		runBootConfiguration(configuration);

	}

}
