package net.rossonet.waldot.rules.vertices;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.util.UUID;

import org.apache.commons.jexl3.JexlScript;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventType;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.jexl.ClonableMapContext;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;
import net.rossonet.waldot.rules.WaldotRulesEnginePlugin;
import net.rossonet.waldot.rules.events.FireableAction;
import net.rossonet.waldot.utils.LogHelper;

public class RuleVertex extends FireableAbstractOpcVertex {

	public enum DebugEventType {
		AFTER_ACTION_COMPILE, AFTER_ACTION_COMPILE_EXCEPTION, AFTER_ACTION_EXECUTION, AFTER_ACTION_EXECUTION_EXCEPTION,
		AFTER_CONDITION_COMPILE, AFTER_CONDITION_COMPILE_EXCEPTION, AFTER_CONDITION_EXECUTION,
		AFTER_CONDITION_EXECUTION_EXCEPTION, BEFORE_ACTION_COMPILE, BEFORE_ACTION_EXECUTION, BEFORE_CONDITION_COMPILE,
		BEFORE_CONDITION_EXECUTION
	}

	public final class RuleVertexFireableAction extends FireableAction {

		@Override
		public void run() {
			Thread.currentThread().setName(getNodeId().toParseableString());
			if (!closed) {
				try {
					if (compiledCondition == null) {
						try {
							sendDebugEvent(DebugEventType.BEFORE_CONDITION_COMPILE, null);
							compiledCondition = waldotRulesEnginePlugin.getJexlEngine().createScript(condition);
							sendDebugEvent(DebugEventType.AFTER_CONDITION_COMPILE, null);
						} catch (final Throwable e) {
							sendDebugEvent(DebugEventType.AFTER_CONDITION_COMPILE_EXCEPTION,
									LogHelper.stackTraceToString(e, 2 + debug));
							logger.error("Error compiling rule vertex condition", e);
							return;
						}
					}
					boolean conditionResult = false;
					try {
						sendDebugEvent(DebugEventType.BEFORE_CONDITION_EXECUTION, null);
						final Object conditionResultObject = compiledCondition.execute(getJexlContext());
						if (conditionResultObject instanceof Boolean) {
							conditionResult = (Boolean) conditionResultObject;
							sendDebugEvent(DebugEventType.AFTER_CONDITION_EXECUTION, conditionResultObject.toString());
						} else {
							logger.warn("Condition script did not return a boolean value, result: '{}'",
									conditionResultObject);
							sendDebugEvent(DebugEventType.AFTER_CONDITION_EXECUTION_EXCEPTION,
									"Condition script did not return a boolean value, result: '" + conditionResultObject
											+ "'");
						}
					} catch (final Throwable e) {
						sendDebugEvent(DebugEventType.AFTER_CONDITION_EXECUTION_EXCEPTION,
								LogHelper.stackTraceToString(e, 2 + debug));
						logger.error("Error executing rule vertex condition", e);
					}
					if (!conditionResult) {
						return;
					}
					if (compiledAction == null) {
						try {
							sendDebugEvent(DebugEventType.BEFORE_ACTION_COMPILE, null);
							compiledAction = waldotRulesEnginePlugin.getJexlEngine().createScript(action);
							sendDebugEvent(DebugEventType.AFTER_ACTION_COMPILE, null);
						} catch (final Throwable e) {
							sendDebugEvent(DebugEventType.AFTER_ACTION_COMPILE_EXCEPTION,
									LogHelper.stackTraceToString(e, 2 + debug));
							logger.error("Error compiling rule vertex condition", e);
							return;
						}
					}
					try {
						sendDebugEvent(DebugEventType.BEFORE_ACTION_EXECUTION, null);
						final Object actionResultObject = compiledAction.execute(getJexlContext());

						sendDebugEvent(DebugEventType.AFTER_ACTION_EXECUTION_EXCEPTION,
								"Action executed successfully, result: '" + actionResultObject + "'");

					} catch (final Throwable e) {
						sendDebugEvent(DebugEventType.AFTER_ACTION_EXECUTION_EXCEPTION,
								LogHelper.stackTraceToString(e, 2 + debug));
						logger.error("Error executing rule vertex action", e);
					}
				} catch (final Throwable t) {
					logger.error("Error executing rule vertex action", t);
				}
			}
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(RuleVertex.class);

	public static void generateParameters(WaldotNamespace waldotNamespace, UaObjectTypeNode dockerTypeNode) {
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode, WaldotRulesEnginePlugin.CONDITION_FIELD,
				NodeIds.String);
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode, WaldotRulesEnginePlugin.ACTION_FIELD,
				NodeIds.String);
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode, WaldotRulesEnginePlugin.QUEUE_SIZE_LABEL,
				NodeIds.Int64);
		PluginListener.addParameterToTypeNode(waldotNamespace, dockerTypeNode,
				WaldotRulesEnginePlugin.DEBUG_LEVEL_LABEL, NodeIds.Int16);
	}

	private String action = WaldotRulesEnginePlugin.DEFAULT_ACTION_VALUE;

	private final QualifiedProperty<String> actionProperty;

	private String baseDirectory;

	private boolean closed = false;
	private transient JexlScript compiledAction;
	private transient JexlScript compiledCondition;
	private String condition = WaldotRulesEnginePlugin.DEFAULT_CONDITION_VALUE;
	private final QualifiedProperty<String> conditionProperty;
	private int debug = 0;
	private final QualifiedProperty<Integer> debugProperty;
	private ClonableMapContext jexlContext;
	private final QualifiedProperty<Long> queueSizeProperty;
	protected final WaldotNamespace waldotNamespace;

	private final WaldotRulesEnginePlugin waldotRulesEnginePlugin;

	public RuleVertex(WaldotRulesEnginePlugin waldotRulesEnginePlugin, WaldotGraph graph, UaNodeContext context,
			NodeId nodeId, QualifiedName browseName, LocalizedText displayName, LocalizedText description,
			UInteger writeMask, UInteger userWriteMask, UByte eventNotifier, long version, Object[] propertyKeyValues) {
		super(graph, context, nodeId, browseName, displayName, description, writeMask, userWriteMask, eventNotifier,
				version);
		waldotNamespace = graph.getWaldotNamespace();
		this.waldotRulesEnginePlugin = waldotRulesEnginePlugin;
		baseDirectory = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				MiloStrategy.DIRECTORY_PARAMETER.toLowerCase());
		if (baseDirectory == null || baseDirectory.isEmpty()) {
			baseDirectory = "rules";
		}
		final String keyValuesPropertyAction = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotRulesEnginePlugin.ACTION_FIELD.toLowerCase());
		if (keyValuesPropertyAction != null && !keyValuesPropertyAction.isEmpty()) {
			action = keyValuesPropertyAction;
		} else {
			action = WaldotRulesEnginePlugin.DEFAULT_ACTION_VALUE;
		}
		actionProperty = new QualifiedProperty<String>(getNamespace().getNamespaceUri(),
				WaldotRulesEnginePlugin.ACTION_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				String.class);
		setProperty(actionProperty, action);
		final String keyValuesPropertyCondition = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotRulesEnginePlugin.CONDITION_FIELD.toLowerCase());
		if (keyValuesPropertyCondition != null && !keyValuesPropertyCondition.isEmpty()) {
			condition = keyValuesPropertyCondition;
		} else {
			condition = WaldotRulesEnginePlugin.DEFAULT_CONDITION_VALUE;
		}
		conditionProperty = new QualifiedProperty<String>(getNamespace().getNamespaceUri(),
				WaldotRulesEnginePlugin.CONDITION_FIELD,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				String.class);
		setProperty(conditionProperty, condition);
		final String keyValuesPropertyDebug = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				WaldotRulesEnginePlugin.DEBUG_LEVEL_LABEL.toLowerCase());
		if (keyValuesPropertyDebug != null && !keyValuesPropertyDebug.isEmpty()) {
			debug = Integer.valueOf(keyValuesPropertyDebug);
		} else {
			debug = 0;
		}
		debugProperty = new QualifiedProperty<Integer>(getNamespace().getNamespaceUri(),
				WaldotRulesEnginePlugin.DEBUG_LEVEL_LABEL,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				Integer.class);
		setProperty(debugProperty, 0);
		queueSizeProperty = new QualifiedProperty<Long>(getNamespace().getNamespaceUri(),
				WaldotRulesEnginePlugin.QUEUE_SIZE_LABEL,
				MiloSingleServerBaseReferenceNodeBuilder.labelVertexTypeNode.getNodeId().expanded(), ValueRanks.Scalar,
				Long.class);
	}

	@Override
	public Object clone() {
		return new RuleVertex(waldotRulesEnginePlugin, graph, getNodeContext(), getNodeId(), getBrowseName(),
				getDisplayName(), getDescription(), getWriteMask(), getUserWriteMask(), getEventNotifier(), version(),
				getPropertiesAsStringArray());
	}

	@Override
	public void close() throws Exception {
		closed = true;
	}

	private FireableAction generateRunnableEvent() {
		return new RuleVertexFireableAction();
	}

	private ClonableMapContext getJexlContext() {
		if (jexlContext == null) {
			jexlContext = new ClonableMapContext(waldotRulesEnginePlugin.baseJexlContext());
			jexlContext.set(ConsoleStrategy.SELF_LABEL, this);
		}
		return jexlContext;
	}

	@Override
	protected FireableAction getRunnableEvent(UaNode node, BaseEventType event) {
		return generateRunnableEvent();
	}

	@Override
	protected FireableAction getRunnablePropertyEvent(UaNode node, String propertyLabel) {
		return generateRunnableEvent();
	}

	@Override
	public void notifyPropertyValueChanging(String label, DataValue value) {
		super.notifyPropertyValueChanging(label, value);
		if (label.equals(WaldotRulesEnginePlugin.ACTION_FIELD.toLowerCase())) {
			action = value.getValue().getValue().toString();
			compiledAction = null;
			setProperty(actionProperty, action);
		}
		if (label.equals(WaldotRulesEnginePlugin.CONDITION_FIELD.toLowerCase())) {
			condition = value.getValue().getValue().toString();
			compiledCondition = null;
			setProperty(conditionProperty, condition);
		}
		if (label.equals(WaldotRulesEnginePlugin.DEBUG_LEVEL_LABEL.toLowerCase())) {
			debug = Integer.valueOf(value.getValue().getValue().toString());
			setProperty(debugProperty, debug);
		}
		if (label.equals(WaldotRulesEnginePlugin.QUEUE_SIZE_LABEL.toLowerCase())) {
			final Long newQueueSize = Long.valueOf(value.getValue().getValue().toString());
			setProperty(queueSizeProperty, newQueueSize);
		}
	}

	public void sendDebugEvent(DebugEventType eventType, String message) {
		if (debug > 0) {
			try {
				final UUID randomUUID = UUID.randomUUID();
				final BaseEventTypeNode eventNode = waldotNamespace.getEventFactory()
						.createEvent(waldotNamespace.generateNodeId(randomUUID), NodeIds.BaseEventType);
				eventNode.setBrowseName(waldotNamespace.generateQualifiedName(eventType.name()));
				eventNode.setDisplayName(LocalizedText.english(eventType.name()));
				eventNode.setEventId(ByteString.of(randomUUID.toString().getBytes()));
				eventNode.setEventType(NodeIds.BaseEventType);
				eventNode.setSourceNode(getNodeId());
				eventNode.setSourceName(getBrowseName().getName());
				eventNode.setTime(DateTime.now());
				eventNode.setReceiveTime(DateTime.NULL_VALUE);
				if (message == null || message.isEmpty()) {
					eventNode.setMessage(LocalizedText.english(eventType.name()));
				} else {
					eventNode.setMessage(LocalizedText.english(message));
				}
				eventNode.setSeverity(ushort(2));
				postEvent(eventNode);
			} catch (final Throwable e) {
				logger.error("Error sending debug event", e);
			}
		}
	}

}
