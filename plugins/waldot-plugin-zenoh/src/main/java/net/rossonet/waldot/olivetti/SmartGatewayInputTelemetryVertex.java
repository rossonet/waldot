package net.rossonet.waldot.olivetti;

import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zenoh.sample.Sample;
import net.rossonet.waldot.WaldotZenohPlugin;
import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;
import net.rossonet.waldot.zenoh.ZenohSubscriberVertex;

public class SmartGatewayInputTelemetryVertex extends AbstractOpcVertex
		implements AutoCloseable, ZenohSubscriberVertex {
	public static void generateParameters(WaldotNamespace waldotNamespace, UaObjectTypeNode dockerTypeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				WaldotZenohPlugin.BASE_SMARTGATEWAY_ZENOH_PATH_FIELD, NodeIds.String);
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				WaldotZenohPlugin.BASE_SMARTGATEWAY_INPUT_DIRECTORY_FIELD, NodeIds.String);
	}

	private String baseDirectory;
	private String baseRegistryDirectory;
	private final QualifiedProperty<String> baseRegistryDirectoryProperty;
	private String baseZenohPath;
	private final QualifiedProperty<String> baseZenohPathProperty;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final WaldotNamespace waldotNamespace;

	public SmartGatewayInputTelemetryVertex(WaldotGraph graph, UaNodeContext context, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName, LocalizedText description, UInteger writeMask,
			UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		waldotNamespace = graph.getWaldotNamespace();
		baseDirectory = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				MiloStrategy.DIRECTORY_PARAMETER.toLowerCase());
		if (baseDirectory == null || baseDirectory.isEmpty()) {
			baseDirectory = "smartgateway";
		}
		baseZenohPath = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotZenohPlugin.BASE_SMARTGATEWAY_ZENOH_PATH_FIELD.toLowerCase());
		baseZenohPathProperty = new QualifiedProperty<String>(getNamespace().getNamespaceUri(),
				WaldotZenohPlugin.BASE_SMARTGATEWAY_ZENOH_PATH_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				String.class);
		setProperty(baseZenohPathProperty, baseZenohPath);
		baseRegistryDirectory = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotZenohPlugin.BASE_SMARTGATEWAY_INPUT_DIRECTORY_FIELD.toLowerCase());
		baseRegistryDirectoryProperty = new QualifiedProperty<String>(getNamespace().getNamespaceUri(),
				WaldotZenohPlugin.BASE_SMARTGATEWAY_INPUT_DIRECTORY_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				String.class);
		if (baseRegistryDirectory == null || baseRegistryDirectory.isEmpty()) {
			baseRegistryDirectory = "smartgateway/input-telemetry";
		}
		setProperty(baseRegistryDirectoryProperty, baseRegistryDirectory);
	}

	@Override
	public Object clone() {
		return new SmartGatewayInputTelemetryVertex(graph, getNodeContext(), getNodeId(), getBrowseName(),
				getDisplayName(), getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(),
				getPropertiesAsStringArray());
	}

	@Override
	public void close() throws Exception {
		// TODO: implement any necessary cleanup logic here
	}

	@Override
	public String getSubscriptionTopic() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void notifyPropertyValueChanging(String label, DataValue value) {
		super.notifyPropertyValueChanging(label, value);
		if (label.equals(WaldotZenohPlugin.BASE_SMARTGATEWAY_ZENOH_PATH_FIELD.toLowerCase())) {
			baseZenohPath = value.getValue().getValue().toString();
			setProperty(baseZenohPathProperty, baseZenohPath);
		}
		if (label.equals(WaldotZenohPlugin.BASE_SMARTGATEWAY_INPUT_DIRECTORY_FIELD.toLowerCase())) {
			baseRegistryDirectory = value.getValue().getValue().toString();
			setProperty(baseRegistryDirectoryProperty, baseRegistryDirectory);
		}

	}

	@Override
	public void run(Sample message) {
		// TODO Auto-generated method stub

	}

}
