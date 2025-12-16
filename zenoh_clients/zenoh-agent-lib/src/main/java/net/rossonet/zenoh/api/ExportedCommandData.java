package net.rossonet.zenoh.api;

import java.lang.reflect.Method;

import net.rossonet.zenoh.annotation.ExportedCommand;

public final class ExportedCommandData {
	private final ExportedCommand exportedCommand;
	public final Method method;
	public final String name;

	public ExportedCommandData(String name, Method method, ExportedCommand exportedCommand) {
		this.name = name;
		this.method = method;
		this.exportedCommand = exportedCommand;
	}

	public ExportedCommand getExportedCommand() {
		return exportedCommand;
	}

	public Method getMethod() {
		return method;
	}

	public String getName() {
		return name;
	}

}