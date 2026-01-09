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

	@Override
	public AgentStatus getAgentStatus() {
		return agentStatus;
	}

	private String[] getConfigurationfromUrl(final String bootstrapUrl) {
		try {
			final InputStream is = new URI(bootstrapUrl).toURL().openConnection().getInputStream();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			final List<String> lines = new ArrayList<>();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#") || line.trim().isEmpty()) {
					continue; // Skip comments and empty lines
				}
				lines.add(line.trim());
			}
			reader.close();
			if (lines.isEmpty()) {
				logger.warn("No valid bootstrap configuration found at URL: {}", bootstrapUrl);
				return new String[0];
			} else {
				logger.info("Bootstrap configuration loaded from URL: {}", bootstrapUrl);
				return lines.toArray(new String[0]);
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
