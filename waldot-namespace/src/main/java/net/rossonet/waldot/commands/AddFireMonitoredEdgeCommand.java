package net.rossonet.waldot.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.MonitoredEdge;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcCommand;

public class AddFireMonitoredEdgeCommand extends AbstractOpcCommand {
	private static final String CONTEXT_DIRECTORY = MiloStrategy.MANAGE_CMD_DIRECTORY
			+ "/edges";
	private static final String CREATE_LINK_MONITORED_EDGE_DESCRIPTION = "create a monitored edge observing the source vertex and firing events or properties to the target vertex";
	private static final String CREATE_LINK_MONITORED_EDGE_NAME = "create fire monitored edge";
	public static final UInteger DEFAULT_WRITE_MASK = UInteger
			.valueOf(WriteMask.Executable.getValue());
	private static final String LABEL_RESULT = "result";
	protected final static Logger logger = LoggerFactory
			.getLogger(AddFireMonitoredEdgeCommand.class);
	private static final String LONG_LABEL_RESULT = "result of the update operation";
	public AddFireMonitoredEdgeCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				CREATE_LINK_MONITORED_EDGE_NAME,
				CREATE_LINK_MONITORED_EDGE_DESCRIPTION, DEFAULT_WRITE_MASK,
				DEFAULT_WRITE_MASK, true, true);
		addInputArgument(MiloStrategy.LABEL_FIELD,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("Label for the edge"));
		addInputArgument(MiloStrategy.SOURCE_NODE,
				VariableNodeTypes.NodeId.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("Source vertex for the edge"));
		addInputArgument(MiloStrategy.TARGET_NODE,
				VariableNodeTypes.NodeId.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("Target vertex for the edge"));
		addInputArgument(MiloStrategy.DIRECTORY_PARAMETER,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("Directory for the edge"));
		addInputArgument(MonitoredEdge.MONITORED_PROPERTIES_LABEL,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(
						MonitoredEdge.MONITORED_PROPERTIES_DESCRIPTION));
		addInputArgument(MonitoredEdge.PROPERTY_ACTIVE_LABEL,
				VariableNodeTypes.Boolean.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText
						.english(MonitoredEdge.PROPERTY_ACTIVE_DESCRIPTION));
		addInputArgument(MonitoredEdge.DEADBAND_TYPE_LABEL,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(MonitoredEdge.DEADBAND_TYPE_DESCRIPTION));
		addInputArgument(MonitoredEdge.DEADBAND_LABEL,
				VariableNodeTypes.UInt32.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(MonitoredEdge.DEADBAND_DESCRIPTION));
		addInputArgument(MonitoredEdge.EVENT_ACTIVE_LABEL,
				VariableNodeTypes.Boolean.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(MonitoredEdge.EVENT_ACTIVE_DESCRIPTION));
		addOutputArgument(LABEL_RESULT, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null,
				LocalizedText.english(LONG_LABEL_RESULT));
		this.addReference(
				new Reference(this.getNodeId(), NodeIds.HasModellingRule,
						NodeIds.ModellingRule_Mandatory.expanded(), true));

	}

	@Override
	public Object clone() {
		return new AddFireMonitoredEdgeCommand(this.waldotNamespace);
	}

	private void enrichParameters(final String[] inputValues,
			final List<String> parameters, int i, String field) {
		if (inputValues[i] != null && !inputValues[i].isEmpty()) {
			parameters.add(field);
			parameters.add(inputValues[i]);
		}
	}

	@Override
	public String getDirectory() {
		return CONTEXT_DIRECTORY;
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext,
			final String[] inputValues) {
		final List<String> parameters = new ArrayList<>();
		parameters.add(MiloStrategy.TYPE_FIELD.toLowerCase());
		parameters.add(MiloStrategy.FIRE_EDGE_TYPE);
		enrichParameters(inputValues, parameters, 0,
				MiloStrategy.LABEL_FIELD.toLowerCase());
		enrichParameters(inputValues, parameters, 3,
				MiloStrategy.DIRECTORY_PARAMETER.toLowerCase());
		enrichParameters(inputValues, parameters, 4,
				MonitoredEdge.MONITORED_PROPERTIES_LABEL.toLowerCase());
		enrichParameters(inputValues, parameters, 5,
				MonitoredEdge.PROPERTY_ACTIVE_LABEL.toLowerCase());
		enrichParameters(inputValues, parameters, 6,
				MonitoredEdge.DEADBAND_TYPE_LABEL.toLowerCase());
		enrichParameters(inputValues, parameters, 7,
				MonitoredEdge.DEADBAND_LABEL.toLowerCase());
		enrichParameters(inputValues, parameters, 8,
				MonitoredEdge.EVENT_ACTIVE_LABEL.toLowerCase());
		final Object[] arrayParameters = parameters.toArray(new String[0]);
		logger.info("create vertex with parameters: "
				+ Arrays.toString(arrayParameters));
		final Vertex source = waldotNamespace
				.getVertexNode(waldotNamespace.generateNodeId(inputValues[1]));
		final Vertex target = waldotNamespace
				.getVertexNode(waldotNamespace.generateNodeId(inputValues[2]));
		final Edge edge = target.addEdge(inputValues[0], source,
				arrayParameters);
		return new String[]{edge.id().toString()};
	}

}
