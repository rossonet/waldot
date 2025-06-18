package net.rossonet.waldot.jexl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.rossonet.waldot.api.models.WaldotNamespace;

public class jexlWaldotCommandHelper {

	protected final WaldotNamespace waldotNamespace;

	public jexlWaldotCommandHelper(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
	}

	public String echo(final Object message) {
		if (message == null) {
			return "null";
		}
		return message.toString();
	}

	public void exit() {
		exit(0);
	}

	public void exit(final int exitCode) {
		System.exit(exitCode);
	}

	public int getEdgesCount() {
		return waldotNamespace.getEdgesCount();
	}

	public int getVerticesCount() {
		return waldotNamespace.getVerticesCount();
	}

	public String help() {
		final Method[] allMethods = this.getClass().getDeclaredMethods();
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (final Method method : allMethods) {
			if (Modifier.isPublic(method.getModifiers())) {
				if (!first) {
					sb.append(", ");
				}
				sb.append(method.getName());
				first = false;
			}
		}
		return sb.toString();
	}

	public void reset() {
		waldotNamespace.resetNameSpace();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("jexl Command Helper [");
		if (waldotNamespace != null) {
			builder.append("waldotNamespace=");
			builder.append(waldotNamespace);
		}
		builder.append("]");
		return builder.toString();
	}

}
