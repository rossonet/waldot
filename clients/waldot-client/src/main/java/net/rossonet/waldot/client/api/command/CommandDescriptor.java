package net.rossonet.waldot.client.api.command;

import java.util.List;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * A command (OPC UA method) discovered on the server.
 *
 * @param objectId    object (folder or element) the method belongs to
 * @param methodId    id of the method node
 * @param name        browse name of the method (e.g. {@code query json})
 * @param path        browse path of the folder (e.g. {@code information}), the
 *                    element id for element commands
 * @param description description of the command
 * @param inputs      input arguments
 * @param outputs     output arguments
 */
public record CommandDescriptor(NodeId objectId, NodeId methodId, String name, String path, String description,
		List<ArgumentDescriptor> inputs, List<ArgumentDescriptor> outputs) {

	public CommandDescriptor {
		inputs = inputs == null ? List.of() : List.copyOf(inputs);
		outputs = outputs == null ? List.of() : List.copyOf(outputs);
	}

	/** @return the index of an output argument, -1 if missing */
	public int outputIndex(final String outputName) {
		for (int i = 0; i < outputs.size(); i++) {
			if (outputs.get(i).name() != null && outputs.get(i).name().equals(outputName)) {
				return i;
			}
		}
		return -1;
	}
}
