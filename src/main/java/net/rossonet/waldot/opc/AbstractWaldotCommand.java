package net.rossonet.waldot.opc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

import net.rossonet.waldot.api.WaldotCommand;
import net.rossonet.waldot.namespaces.HomunculusNamespace;

public abstract class AbstractWaldotCommand extends UaMethodNode implements WaldotCommand {

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

	private final HomunculusNamespace waldotNamespace;

	public AbstractWaldotCommand(HomunculusNamespace waldotNamespace, String command, String description,
			UInteger writeMask, UInteger userWriteMask, Boolean executable, Boolean userExecutable) {
		super(waldotNamespace.getOpcUaNodeContext(), waldotNamespace.generateNodeId(command),
				waldotNamespace.generateQualifiedName(command), LocalizedText.english(command),
				LocalizedText.english(description), writeMask, userWriteMask, executable, userExecutable);
		this.waldotNamespace = waldotNamespace;
	}

	@Override
	public Edge addEdge(String label, Vertex inVertex, Object... keyValues) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addInputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description) {
		final Argument arg = new Argument(name, dataType, valueRank, arrayDimensions, description);
		inputArguments.add(arg);
		waldotNamespace.registerMethodInputArgument(this, inputArguments);
	}

	public void addOutputArgument(String name, NodeId dataType, Integer valueRank, UInteger[] arrayDimensions,
			LocalizedText description) {
		final Argument arg = new Argument(name, dataType, valueRank, arrayDimensions, description);
		outputArguments.add(arg);
		waldotNamespace.registerMethodOutputArguments(this, outputArguments);
	}

	@Override
	public void attributeChanged(UaNode node, AttributeId attributeId, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public Iterator<Edge> edges(Direction direction, String... edgeLabels) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UByte getEventNotifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Argument[] getInputArguments() {
		return inputArguments.toArray(new Argument[0]);
	}

	@Override
	public MethodInvocationHandler getInvocationHandler() {
		return new AbstractMethodInvocationHandler(this) {

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

	public HomunculusNamespace getWaldotNamespace() {
		return waldotNamespace;
	}

	@Override
	public Graph graph() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String label() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> Iterator<VertexProperty<V>> properties(String... propertyKeys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <V> VertexProperty<V> property(Cardinality cardinality, String key, V value, Object... keyValues) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub

	}

	// per dsl gremlin
	public Object runCommand(String[] methodInputs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEventNotifier(UByte eventNotifier) {
		// TODO Auto-generated method stub

	}

	@Override
	public Iterator<Vertex> vertices(Direction direction, String... edgeLabels) {
		// TODO Auto-generated method stub
		return null;
	}

}
