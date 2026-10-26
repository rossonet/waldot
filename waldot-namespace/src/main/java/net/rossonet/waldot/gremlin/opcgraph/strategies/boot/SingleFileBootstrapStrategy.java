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
	 * <li>Function definitions: def functionName (detected, but rejected by JEXL)</li>
	 * <li>Comments: double-slash or slash-star</li>
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
		// Remove '#' comments for detection (legacy line-mode syntax).
		final String cleanContent = content.replaceAll("#[^\n]*\n", "\n");

		// Script mode indicators:

		// 1. Comments (//, /* */)
		if (cleanContent.contains("//") || cleanContent.contains("/*")) {
			return true;
		}

		// 2. Function definitions
		if (cleanContent.matches("(?s).*\\bdef\\s+\\w+\\s*\\(.*")) {
			return true;
		}

		// 3. Variable assignments (excluding object property syntax).
		// Look for patterns such as "varName = " at line start.
		if (java.util.regex.Pattern.compile("^\\s*\\w+\\s*=(?!=)", java.util.regex.Pattern.MULTILINE)
				.matcher(cleanContent).find()) {
			return true;
		}

		// 4. Multi-line method chains with indentation.
		// Look for patterns such as ".property(...)\n  .property(...)".
		if (cleanContent.matches("(?s).*\\.\\w+\\([^)]*\\)\\s*\n\\s+\\.\\w+\\(.*")) {
			return true;
		}

		// 5. Control structures
		if (cleanContent.matches("(?s).*(\\bif\\s*\\(|\\bfor\\s*\\(|\\bwhile\\s*\\(|\\btimes\\s*\\{).*")) {
			return true;
		}

		// 6. Closures/blocks with meaningful content.
		if (cleanContent.matches("(?s).*\\{[^}]{20,}\\}.*")) {
			return true;
		}

		// Default: line-by-line mode for backward compatibility.
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
	 * Each non-comment, non-empty line is executed as a separate JEXL expression.
	 * Lines starting with '#' are treated as comments and skipped.
	 * </p>
	 * 
	 * <pre>{@code
	 * # This is a comment
	 * graph.addVertex('id', 'sensor1', 'label', 'sensor1')
	 * graph.addVertex('id', 'sensor2', 'label', 'sensor2')
	 * }</pre>
	 * 
	 * <h3>Format 2: JEXL script</h3>
	 * <p>
	 * If the file contains multi-line constructs (variable assignments, control
	 * structures, comments), the entire content is executed as a single JEXL
	 * script by the console engine. Statements must end with ';'.
	 * </p>
	 * 
	 * <pre>{@code
	 * // function stored in a variable
	 * var createSensor = function(name, algorithm) {
	 *   return graph.addVertex('id', name, 'type', 'sim:generator', 'label', name,
	 *       'algorithm', algorithm);
	 * };
	 * createSensor('temp-sensor', 'sinusoidal');
	 * createSensor('pressure-sensor', 'random');
	 * log.info('Sensors configured');
	 * }</pre>
	 * 
	 * <p>
	 * The method automatically detects which format to use by analyzing the
	 * content (see {@link #detectScriptMode(String)}).
	 * </p>
	 * 
	 * <p>
	 * <b>Remote URLs</b>: The configuration can be loaded from:
	 * <ul>
	 * <li>Local files: {@code file:///path/to/boot.conf}</li>
	 * <li>HTTP/HTTPS: {@code https://example.com/boot.conf}</li>
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

			// First pass: read the full content.
			while ((line = reader.readLine()) != null) {
				fullContent.append(line).append("\n");

				// Keep per-line commands for legacy mode (backward compatibility),
				// ignoring '#' comments and empty lines.
				if (!line.trim().startsWith("#") && !line.trim().isEmpty()) {
					individualLines.add(line.trim());
				}
			}
			reader.close();

			final String content = fullContent.toString();

			// Decide whether to use script mode or line-by-line mode.
			final boolean useScriptMode = detectScriptMode(content);

			if (useScriptMode) {
				// Script mode: return the whole content as a single expression.
				logger.info("Bootstrap configuration loaded in SCRIPT MODE from URL: {}", bootstrapUrl);
				logger.debug("Script content length: {} characters", content.length());
				return new String[] { content };
			} else {
				// Line-by-line mode: backward compatibility path.
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