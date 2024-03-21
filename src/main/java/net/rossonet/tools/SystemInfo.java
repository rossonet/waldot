package net.rossonet.tools;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SystemInfo {

	public static Map<String, Object> getSystemInfo() {
		final Map<String, Object> output = new HashMap<>();
		output.put("Available processors (cores)", Runtime.getRuntime().availableProcessors());
		output.put("Free memory (bytes)", Runtime.getRuntime().freeMemory());
		final long maxMemory = Runtime.getRuntime().maxMemory();
		output.put("Maximum memory (bytes)", (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));
		output.put("Total memory available to JVM (bytes)", Runtime.getRuntime().totalMemory());
		final File[] roots = File.listRoots();
		final Map<String, Object> fs = new HashMap<>();
		for (final File root : roots) {
			fs.put("File system root", root.getAbsolutePath());
			fs.put("Total space (bytes)", root.getTotalSpace());
			fs.put("Free space (bytes)", root.getFreeSpace());
			fs.put("Usable space (bytes)", root.getUsableSpace());
		}
		output.put("File systems", fs);
		return output;
	}

	private SystemInfo() {
// static use only
	}

}
