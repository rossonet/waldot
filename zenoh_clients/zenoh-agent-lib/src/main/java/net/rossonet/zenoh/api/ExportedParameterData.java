package net.rossonet.zenoh.api;

import java.lang.reflect.Field;

import net.rossonet.zenoh.annotation.ExportedParameter;

public final class ExportedParameterData {
	private final ExportedParameter exportedParameter;
	public final Field field;
	public final String name;

	public ExportedParameterData(String name, Field field, ExportedParameter exportedParameter) {
		this.name = name;
		this.field = field;
		this.exportedParameter = exportedParameter;
	}

	public ExportedParameter getExportedParameter() {
		return exportedParameter;
	}

	public Field getField() {
		return field;
	}

	public String getName() {
		return name;
	}
}