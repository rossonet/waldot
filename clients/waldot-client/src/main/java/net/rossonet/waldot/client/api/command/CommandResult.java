package net.rossonet.waldot.client.api.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

import net.rossonet.waldot.client.api.WaldotClientException;

/**
 * Result of a command call: OPC UA status and output values (in the order of
 * the output arguments).
 *
 * @param command the command
 * @param status  status of the call
 * @param values  output values
 */
public record CommandResult(CommandDescriptor command, StatusCode status, List<Object> values) {

	/** Name of the output that the WaldOT commands use for the errors. */
	public static final String ERROR_OUTPUT = "error";

	public CommandResult {
		values = values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
	}

	/** @return the output values by argument name (unnamed outputs by index) */
	public Map<String, Object> asMap() {
		final Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < values.size(); i++) {
			final String name = command != null && i < command.outputs().size() ? command.outputs().get(i).name()
					: null;
			map.put(name == null ? String.valueOf(i) : name, values.get(i));
		}
		return map;
	}

	/** @return the output values as strings (null stays null) */
	public List<String> asStrings() {
		final List<String> strings = new ArrayList<>(values.size());
		for (final Object value : values) {
			strings.add(value == null ? null : value.toString());
		}
		return strings;
	}

	/**
	 * @return the {@code error} output when present and not empty, or the
	 *         status when the call failed
	 */
	public Optional<String> error() {
		if (status != null && !status.isGood()) {
			return Optional.of("call failed with status " + status);
		}
		final Object error = get(ERROR_OUTPUT);
		if (error != null && !error.toString().isBlank()) {
			return Optional.of(error.toString());
		}
		return Optional.empty();
	}

	/** @return the value of an output by argument name, null if missing */
	public Object get(final String outputName) {
		final int index = command == null ? -1 : command.outputIndex(outputName);
		return index >= 0 && index < values.size() ? values.get(index) : null;
	}

	/** @return the value of an output by index, null if missing */
	public Object get(final int index) {
		return index >= 0 && index < values.size() ? values.get(index) : null;
	}

	/** @return an output as string, empty string if missing */
	public String getString(final String outputName) {
		final Object value = get(outputName);
		return value == null ? "" : value.toString();
	}

	/** @return true if the call succeeded and the {@code error} output is empty */
	public boolean isSuccess() {
		return error().isEmpty();
	}

	/**
	 * @return this result
	 * @throws WaldotClientException if the command failed
	 */
	public CommandResult orThrow() {
		final Optional<String> error = error();
		if (error.isPresent()) {
			throw new WaldotClientException("command '" + (command == null ? "?" : command.name()) + "' failed: "
					+ error.get(), status == null || status.isGood() ? StatusCode.BAD : status, null);
		}
		return this;
	}
}
