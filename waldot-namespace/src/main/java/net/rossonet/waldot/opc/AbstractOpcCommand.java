package net.rossonet.waldot.opc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcVertex;

public abstract class AbstractOpcCommand extends OpcVertex implements WaldotCommand {

	public enum VariableNodeTypes {
		Boolean(Identifiers.Boolean), Byte(Identifiers.Byte), SByte(Identifiers.SByte), Integer(Identifiers.Integer),
		Int16(Identifiers.Int16), Int32(Identifiers.Int32), Int64(Identifiers.Int64), UInteger(Identifiers.UInteger),
		UInt16(Identifiers.UInt16), UInt32(Identifiers.UInt32), UInt64(Identifiers.UInt64), Float(Identifiers.Float),
		Double(Identifiers.Double), String(Identifiers.String), DateTime(Identifiers.DateTime), Guid(Identifiers.Guid),
		ByteString(Identifiers.ByteString), XmlElement(Identifiers.XmlElement),
		LocalizedText(Identifiers.LocalizedText), QualifiedName(Identifiers.QualifiedName), NodeId(Identifiers.NodeId),
		Variant(Identifiers.BaseDataType), Duration(Identifiers.Duration), UtcTime(Identifiers.UtcTime);

		private final NodeId nodeId;

		VariableNodeTypes(NodeId nodeId) {
			this.nodeId = nodeId;
		}

		public NodeId getNodeId() {
			return nodeId;
		}
	}

	private final List<Argument> inputArguments = new ArrayList<>();

	private final List<Argument> outputArguments = new ArrayList<>();

	private final WaldotNamespace waldotNamespace;

	private final UaMethodNode insideCommand;

	public AbstractOpcCommand(WaldotGraph graph, WaldotNamespace waldotNamespace, String command, String description,
			UInteger writeMask, UInteger userWriteMask, Boolean executable, Boolean userExecutable) {
		super(graph, waldotNamespace.getOpcUaNodeContext(), waldotNamespace.generateNodeId(command),
				waldotNamespace.generateQualifiedName(command), LocalizedText.english(command),
				LocalizedText.english(description), writeMask, userWriteMask, null, 0);
		insideCommand = new UaMethodNode(waldotNamespace.getOpcUaNodeContext(), waldotNamespace.generateNodeId(command),
				waldotNamespace.generateQualifiedName(command), LocalizedText.english(command),
				LocalizedText.english(description), writeMask, userWriteMask, executable, userExecutable);
		this.waldotNamespace = waldotNamespace;
	}

	@Override
	public void addInputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description) {
		final Argument arg = new Argument(name, dataType, valueRank, arrayDimensions, description);
		inputArguments.add(arg);
		waldotNamespace.registerMethodInputArgument(this, inputArguments);
	}

	@Override
	public void addOutputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description) {
		final Argument arg = new Argument(name, dataType, valueRank, arrayDimensions, description);
		outputArguments.add(arg);
		waldotNamespace.registerMethodOutputArguments(this, outputArguments);
	}

	@Override
	public Argument[] getInputArguments() {
		return inputArguments.toArray(new Argument[0]);
	}

	@Override
	public MethodInvocationHandler getInvocationHandler() {
		return new AbstractMethodInvocationHandler(insideCommand) {

			@Override
			public Argument[] getInputArguments() {
				return inputArguments.toArray(new Argument[0]);
			}

			@Override
			public Argument[] getOutputArguments() {
				return outputArguments.toArray(new Argument[0]);
			}

			@Override
			protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {
				final String[] input = new String[inputValues.length];
				for (int i = 0; i < inputValues.length; i++) {
					input[i] = inputValues[i].getValue().toString();
				}
				final String[] runCommand = runCommand(invocationContext, input);
				final Variant[] reply = new Variant[runCommand.length];
				for (int i = 0; i < runCommand.length; i++) {
					reply[i] = new Variant(runCommand[i]);
				}
				return reply;
			}

		};

	}

	@Override
	public Argument[] getOutputArguments() {
		return outputArguments.toArray(new Argument[0]);
	}

	@Override
	public Boolean isExecutable() {
		return insideCommand.isExecutable();
	}

	@Override
	public Boolean isUserExecutable() {
		return insideCommand.isUserExecutable();
	}

	// per dsl gremlin
	@Override
	public Object runCommand(String[] methodInputs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setExecutable(Boolean executable) {
		insideCommand.setExecutable(executable);

	}

	@Override
	public void setInputArguments(Argument[] array) {
		insideCommand.setInputArguments(array);

	}

	@Override
	public void setOutputArguments(Argument[] array) {
		insideCommand.setOutputArguments(array);

	}

	@Override
	public void setUserExecutable(Boolean userExecutable) {
		insideCommand.setUserExecutable(userExecutable);

	}

}
