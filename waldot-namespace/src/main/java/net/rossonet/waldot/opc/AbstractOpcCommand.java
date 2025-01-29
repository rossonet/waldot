package net.rossonet.waldot.opc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;

import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.gremlin.opcgraph.structure.AbstractOpcGraph;

public abstract class AbstractOpcCommand extends OpcCommand implements WaldotCommand {

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

	protected final WaldotGraph graph;
	protected final List<Argument> inputArguments = new ArrayList<>();

	protected final List<Argument> outputArguments = new ArrayList<>();

	protected ByteString icon;
	protected final WaldotNamespace waldotNamespace;

	public AbstractOpcCommand(WaldotGraph graph, WaldotNamespace waldotNamespace, String command, String description,
			UInteger writeMask, UInteger userWriteMask, Boolean executable, Boolean userExecutable) {
		super(waldotNamespace.getOpcUaNodeContext(), waldotNamespace.generateNodeId(command),
				waldotNamespace.generateQualifiedName(command), LocalizedText.english(command),
				LocalizedText.english(description), writeMask, userWriteMask, executable, userExecutable);
		this.waldotNamespace = waldotNamespace;
		this.graph = graph;
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

	public WaldotGraph getGraph() {
		return graph;
	}

	@Override
	public ByteString getIcon() {
		return icon;
	}

	@Override
	public WaldotNamespace getNamespace() {
		return graph.getWaldotNamespace();
	}

	@Override
	public Graph graph() {
		return this.graph;
	}

	@Override
	public <V> Iterator<? extends Property<V>> properties(String... propertyKeys) {
		return Collections.emptyIterator();
	}

	@Override
	public <V> Property<V> property(String key, V value) {
		return Property.empty();
	}

	@Override
	public Object[] runCommand(String[] methodInputs) {
		return runCommand(null, methodInputs);
	}

	@Override
	public void setIcon(ByteString icon) {
		this.icon = icon;

	}

	@Override
	public String toString() {
		return AbstractOpcGraph.V + AbstractOpcGraph.L_BRACKET + getNodeId().toParseableString();
	}

}
