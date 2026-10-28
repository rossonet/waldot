package net.rossonet.waldot.client.api.command;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * An input or output argument of a command.
 *
 * @param name        name of the argument
 * @param dataType    OPC UA data type
 * @param valueRank   value rank (-1 scalar)
 * @param description description (may be null)
 */
public record ArgumentDescriptor(String name, NodeId dataType, int valueRank, String description) {
}
