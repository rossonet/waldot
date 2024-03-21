package net.rossonet.cmd.commands;

import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import net.rossonet.tools.SystemInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "test-command", mixinStandardHelpOptions = true, version = { "${COMMAND-NAME} 1.0",
		"Picocli " + picocli.CommandLine.VERSION,
		"JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
		"OS: ${os.name} ${os.version} ${os.arch}" }, description = "Prints the information about the system to STDOUT.", footer = "powered by Rossonet s.c.a r.l.", showEndOfOptionsDelimiterInUsageHelp = true, showAtFileInUsageHelp = true)
public class SystemInfoCommand implements Callable<Integer> {

	static enum OutputFormat {
		human, json, yaml
	}

	@Spec
	CommandSpec spec;

	@Option(names = { "-t",
			"--output-format" }, description = "print output in specified format [human, json or yaml], default value is \"${DEFAULT-VALUE}\"", defaultValue = "human")
	private OutputFormat outputFormat = OutputFormat.human;

	@Override
	public Integer call() throws Exception {
		final Map<String, Object> systemInfo = SystemInfo.getSystemInfo();
		if (outputFormat.equals(OutputFormat.human)) {
			final StringBuilder output = new StringBuilder();
			for (final Entry<String, Object> entry : systemInfo.entrySet()) {
				output.append(entry.getKey() + " - " + entry.getValue() + "\n");
			}
			spec.commandLine().getOut().println(output);
		} else if (outputFormat.equals(OutputFormat.json)) {
			final JSONObject json = new JSONObject();
			for (final Entry<String, Object> entry : systemInfo.entrySet()) {
				json.put(entry.getKey(), entry.getValue());
			}
			spec.commandLine().getOut().println(json.toString(2));
		} else if (outputFormat.equals(OutputFormat.yaml)) {
			final Yaml yaml = new Yaml();
			final StringWriter writer = new StringWriter();
			yaml.dump(systemInfo, writer);
			spec.commandLine().getOut().println(writer.toString());
		} else {
			return 9;
		}
		return 0;
	}

	protected String getOutputFormatToString() {
		return outputFormat.toString();
	}

	protected void setOutputFormatFromString(String outputFormat) {
		this.outputFormat = OutputFormat.valueOf(outputFormat);
	}

}
