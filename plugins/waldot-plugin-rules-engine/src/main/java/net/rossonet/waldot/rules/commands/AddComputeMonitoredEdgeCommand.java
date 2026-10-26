package net.rossonet.waldot.rules.commands;

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

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.rules.RulesEngineConstants;

/**
 * Creates a <code>rules:execute</code> edge: the compute vertex (source) will execute the rule (target).
 */
public class AddComputeMonitoredEdgeCommand extends AbstractOpcCommand {
	private static final String CONTEXT_DIRECTORY = RulesEngineConstants.BASE_CMD_DIRECTORY
			+ "/edges";
	private static final String CREATE_COMPUTE_MONITORED_EDGE_DESCRIPTION = "create an execute edge: the source compute vertex runs the actions of the target rule";
	private static final String CREATE_COMPUTE_MONITORED_EDGE_NAME = "create compute monitored edge";
	public static final UInteger DEFAULT_WRITE_MASK = UInteger
			.valueOf(WriteMask.Executable.getValue());
	private static final String LABEL_RESULT = "result";
	protected final static Logger logger = LoggerFactory
			.getLogger(AddComputeMonitoredEdgeCommand.class);
	private static final String LONG_LABEL_RESULT = "result of the update operation";
	public AddComputeMonitoredEdgeCommand(
			final WaldotNamespace waldotNamespace) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				CREATE_COMPUTE_MONITORED_EDGE_NAME,
				CREATE_COMPUTE_MONITORED_EDGE_DESCRIPTION, DEFAULT_WRITE_MASK,
				DEFAULT_WRITE_MASK, true, true);
		addInputArgument(MiloStrategy.LABEL_FIELD,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("Label for the edge"));
		addInputArgument(MiloStrategy.SOURCE_NODE,
				VariableNodeTypes.NodeId.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("Compute vertex that executes the rule"));
		addInputArgument(MiloStrategy.TARGET_NODE,
				VariableNodeTypes.NodeId.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("Rule vertex to execute"));
		addInputArgument(MiloStrategy.DIRECTORY_PARAMETER,
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("Directory for the edge"));
		addInputArgument(RulesEngineConstants.PRIORITY_FIELD,
				VariableNodeTypes.Int32.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(
						"Priority of the rule in the compute vertex: the higher, the sooner it is executed"));
		addOutputArgument(LABEL_RESULT, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null,
				LocalizedText.english(LONG_LABEL_RESULT));
		this.addReference(
				new Reference(this.getNodeId(), NodeIds.HasModellingRule,
						NodeIds.ModellingRule_Mandatory.expanded(), true));

	}

	@Override
	public Object clone() {
		return new AddComputeMonitoredEdgeCommand(this.waldotNamespace);
	}

	private void enrichParameters(final String[] inputValues,
			final List<String> parameters, int i, String field) {
		if (i < inputValues.length && inputValues[i] != null
				&& !inputValues[i].isEmpty()) {
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
		try {
			final List<String> parameters = new ArrayList<>();
			parameters.add(MiloStrategy.TYPE_FIELD.toLowerCase());
			parameters.add(RulesEngineConstants.EXECUTE_EDGE_LABEL);
			enrichParameters(inputValues, parameters, 3,
					MiloStrategy.DIRECTORY_PARAMETER.toLowerCase());
			enrichParameters(inputValues, parameters, 4,
					RulesEngineConstants.PRIORITY_FIELD);
			final Object[] arrayParameters = parameters.toArray(new String[0]);
			logger.info("create execute edge with parameters: "
					+ Arrays.toString(arrayParameters));
			final Vertex source = resolveVertexArgument(
					inputValues.length > 1 ? inputValues[1] : null,
					"compute");
			final Vertex target = resolveVertexArgument(
					inputValues.length > 2 ? inputValues[2] : null, "rule");
			// l'edge va dal compute (source) alla regola (target)
			final Edge edge = source.addEdge(inputValues[0], target,
					arrayParameters);
			return new String[]{nodeIdToString(edge.id())};
		} catch (final RuntimeException e) {
			logger.warn("create compute monitored edge failed: {}",
					e.getMessage());
			return new String[]{"error: " + e.getMessage()};
		}
	}

}
