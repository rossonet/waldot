package net.rossonet.zenoh.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONObject;

import com.google.common.reflect.ClassPath;
import com.jsoniter.output.JsonStream;

import io.zenoh.config.ZenohId;
import net.rossonet.waldot.dtdl.DigitalTwinModelIdentifier;
import net.rossonet.waldot.dtdl.DtdlHandler;
import net.rossonet.waldot.utils.ThreadHelper;
import net.rossonet.zenoh.annotation.AnnotatedAgentController.ConfigurationChangeType;
import net.rossonet.zenoh.api.AgentCommandMetadata;
import net.rossonet.zenoh.api.AgentCommandParameter;
import net.rossonet.zenoh.api.AgentConfigurationMetadata;
import net.rossonet.zenoh.api.AgentControlHandler;
import net.rossonet.zenoh.api.AgentProperty;
import net.rossonet.zenoh.api.ExportedCommandData;
import net.rossonet.zenoh.api.ExportedParameterData;
import net.rossonet.zenoh.api.InternalLogMessage;
import net.rossonet.zenoh.api.TelemetryData;
import net.rossonet.zenoh.api.message.RpcCommand;
import net.rossonet.zenoh.api.message.RpcConfiguration;
import net.rossonet.zenoh.api.message.TelemetryMessage;
import net.rossonet.zenoh.exception.ExecutionCommandException;

/**
 * An abstract implementation of AgentControlHandler that uses annotations to
 * define commands, properties, and configurations.
 */
public abstract class AbstractAgentAnnotationControlHandler implements AgentControlHandler {

	private static final int DEFAULT_VERSION_API = 1;

	private final List<JSONObject> acknowledgeMessages = new ArrayList<>();

	private volatile boolean active;

	private final String basePackage;

	private final Map<Long, RpcConfiguration> configurations = new HashMap<>();

	private boolean dataFlowActive = false;

	private long dataFlowStartedAtMs;

	private long dataFlowStoppedAtMs;

	private Duration definedWaitTimeMs;

	private AnnotatedAgentController flowController;

	private volatile long lastControlParameterEpoch = 0;

	private long maxWaitExecutionCycleTime;

	private final BlockingQueue<InternalLogMessage> messageQueue = new LinkedBlockingQueue<>();

	private List<ZenohId> peersZid;

	private volatile boolean registered;

	private final Map<String, AgentCommandMetadata> registeredCommands = new HashMap<>();

	private final Map<String, AgentConfigurationMetadata> registeredConfigurations = new HashMap<>();

	private final Map<Long, TelemetryData> registeredInputTelemetries = new HashMap<>();

	private final Map<Long, TelemetryData> registeredInternalTelemetries = new HashMap<>();

	private final Map<String, AgentProperty> registeredProperties = new HashMap<>();

	private final Map<Long, TelemetryData> registeredTelemetries = new HashMap<>();

	private List<ZenohId> routersZid;

	private long sessionCreatedMs;

	private ZenohId sessionZid;

	private int threadPriority;

	private Thread workerThread;

	public AbstractAgentAnnotationControlHandler(final String basePackage) {
		this(basePackage, Thread.NORM_PRIORITY, Duration.ofMillis(20), Duration.ofSeconds(10));
	}

	public AbstractAgentAnnotationControlHandler(final String basePackage, final int threadPriority,
			final Duration definedWaitTimeMs, final Duration maxWaitExecutionCycleTime) {
		this.threadPriority = threadPriority;
		this.basePackage = basePackage;
		this.maxWaitExecutionCycleTime = maxWaitExecutionCycleTime.toMillis();
		this.definedWaitTimeMs = definedWaitTimeMs;
		try {
			elaborateAnnotations();
		} catch (final IOException e) {
			messageQueue.offer(
					new InternalLogMessage(InternalLogMessage.MessageType.ERROR, "Error elaborating annotations", e));
		}
		startWorkerThread();
		messageQueue.offer(
				new InternalLogMessage(InternalLogMessage.MessageType.INFO, "AgentControlHandler started", null));
	}

	private void addCommandMetadata(final ExportedCommandData commandData) {
		final String commandName = commandData.getName();
		final String methodName = commandData.getMethod().getName();
		final ExportedCommand annotation = commandData.getExportedCommand();
		final List<AgentCommandParameter> commandParameters = new ArrayList<>();
		int order = 0;
		for (final Parameter methodParameter : commandData.getMethod().getParameters()) {
			if (methodParameter.isAnnotationPresent(ExportedMethodParameter.class)) {
				final ExportedMethodParameter methodParamAnnotation = methodParameter
						.getAnnotation(ExportedMethodParameter.class);
				String parameterName = methodParamAnnotation.name();
				if (parameterName.isEmpty()) {
					parameterName = methodParameter.getName();
				}
				commandParameters
						.add(new AgentCommandParameter(parameterName, methodParameter, methodParamAnnotation, order));
				order++;
			} else {
				messageQueue.offer(new InternalLogMessage(
						InternalLogMessage.MessageType.ERROR, "Method parameter " + methodParameter.getName()
								+ " of command " + commandName + " is missing ExportedMethodParameter annotation",
						null));
			}
		}
		final String methodReturnType = commandData.getMethod().getReturnType().getSimpleName();
		registeredCommands.put(commandName, new AgentCommandMetadata(commandName, flowController, methodName,
				methodReturnType, annotation, commandParameters));
	}

	private void addConfigurationObjectMetadata(final Class<?> configClass,
			final Collection<ExportedParameterData> annotatedFields) {
		final String configurationClassName = configClass.getName();
		String configurationName = configClass.getSimpleName();
		boolean unique = true;
		String description = "";
		for (final Annotation annotation : configClass.getAnnotations()) {
			if (annotation instanceof ExportedObject) {
				final ExportedObject exportedObject = (ExportedObject) annotation;
				if (!exportedObject.name().isEmpty()) {
					configurationName = exportedObject.name();
				}
				description = exportedObject.description();
				unique = exportedObject.unique();
				break;
			}
		}
		final Map<String, AgentProperty> properties = new HashMap<>();
		for (final ExportedParameterData parameterData : annotatedFields) {
			final String fieldName = parameterData.getField().getName();
			final String propertyName = parameterData.getName();
			final ExportedParameter annotation = parameterData.getExportedParameter();
			properties.put(parameterData.getName(),
					new AgentProperty(propertyName, flowController, fieldName, annotation));
		}
		registeredConfigurations.put(configurationName, new AgentConfigurationMetadata(configurationName,
				configurationClassName, description, properties, unique));

	}

	private void addPropertyMetadata(final ExportedParameterData parameterData) {
		final String fieldName = parameterData.getField().getName();
		final String propertyName = parameterData.getName();
		final ExportedParameter annotation = parameterData.getExportedParameter();
		registeredProperties.put(parameterData.getName(),
				new AgentProperty(propertyName, flowController, fieldName, annotation));
	}

	@Override
	public void addUpdateOrDeleteConfigurationObjects(final RpcConfiguration configuration) {
		if (configurations.containsKey(configuration.getUniqueId())) {
			updateOrDeleteConfiguration(configuration);
			flowController.notifyConfigurationChanged(configuration.getConfigurationId(),
					ConfigurationChangeType.OBJECT_CREATED);
		} else {
			configurations.put(configuration.getUniqueId(), configuration);
		}
	}

	@Override
	public void close() throws Exception {
		active = false;
		flowController.stopDataFlow();
		doShutdown();
	}

	@Override
	public void delConfigurationObjects(final long uniqueUid) {
		configurations.remove(uniqueUid);
	}

	protected abstract void doShutdown();

	private void elaborateAnnotations() throws IOException {
		final ClassPath cp = ClassPath.from(Thread.currentThread().getContextClassLoader());
		final Set<Class<?>> configurationObjects = new HashSet<>();
		for (final ClassPath.ClassInfo classInfo : cp.getTopLevelClassesRecursive(basePackage)) {
			final Class<?> clazz = classInfo.load();
			if (clazz.isAnnotationPresent(ExportedController.class)) {
				messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO,
						"Found AnnotatedAgentController class: " + clazz.getName(), null));
				if (flowController != null) {
					messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.ERROR,
							"Multiple AnnotatedAgentController classes found. Only one is allowed. Class "
									+ clazz.getName() + " will be ignored.",
							null));
				}
				try {
					flowController = (AnnotatedAgentController) clazz.getConstructor().newInstance();
				} catch (final Exception e) {
					messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.ERROR,
							"Error instantiating AnnotatedAgentController from class " + clazz.getName(), e));
				}

			}
			if (clazz.isAnnotationPresent(ExportedObject.class)) {
				configurationObjects.add(clazz);
				messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO,
						"Found AgentConfigurationMetadata class: " + clazz.getName(), null));
			}
		}
		if (flowController == null) {
			messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.ERROR,
					"No AnnotatedAgentController class found.", null));
		} else {
			elaborateControllerAnnotations();
		}
		if (configurationObjects.isEmpty()) {
			messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.ERROR,
					"No AgentConfigurationMetadata classes found.", null));
		} else {
			for (final Class<?> configClass : configurationObjects) {
				elaborateConfigurationObjectAnnotations(configClass);
			}
		}
	}

	private void elaborateConfigurationObjectAnnotations(final Class<?> configClass) {
		final Set<ExportedParameterData> annotatedFields = new HashSet<>();
		final Field[] fields = configClass.getDeclaredFields();
		for (final Field field : fields) {
			final Annotation[] annotations = field.getAnnotations();
			for (final Annotation annotation : annotations) {
				if (annotation instanceof ExportedParameter) {
					final ExportedParameter exportedParameter = (ExportedParameter) annotation;
					String parameterName = exportedParameter.name();
					if (parameterName.isEmpty()) {
						parameterName = field.getName();
					}
					annotatedFields.add(new ExportedParameterData(parameterName, field, exportedParameter));
					break;
				}
			}
		}
		addConfigurationObjectMetadata(configClass, annotatedFields);
	}

	private void elaborateControllerAnnotations() {
		final Set<ExportedParameterData> annotatedFields = new HashSet<>();
		final Class<? extends AnnotatedAgentController> classToScan = flowController.getClass();
		final Field[] fields = classToScan.getDeclaredFields();
		for (final Field field : fields) {
			final Annotation[] annotations = field.getAnnotations();
			for (final Annotation annotation : annotations) {
				if (annotation instanceof ExportedParameter) {
					final ExportedParameter exportedParameter = (ExportedParameter) annotation;
					String parameterName = exportedParameter.name();
					if (parameterName.isEmpty()) {
						parameterName = field.getName();
					}
					annotatedFields.add(new ExportedParameterData(parameterName, field, exportedParameter));
					break;
				}
			}
		}
		for (final ExportedParameterData parameterData : annotatedFields) {
			addPropertyMetadata(parameterData);
		}
		final Method[] methods = classToScan.getMethods();
		final Set<ExportedCommandData> annotatedCommands = new HashSet<>();
		for (final Method method : methods) {
			final Annotation[] annotations = method.getAnnotations();
			for (final Annotation annotation : annotations) {
				if (annotation instanceof ExportedCommand) {
					final ExportedCommand exportedCommand = (ExportedCommand) annotation;
					String commandName = exportedCommand.name();
					if (commandName.isEmpty()) {
						commandName = method.getName();
					}
					annotatedCommands.add(new ExportedCommandData(commandName, method, exportedCommand));
					break;
				}
			}
		}
		for (final ExportedCommandData commandData : annotatedCommands) {
			addCommandMetadata(commandData);
		}

	}

	protected abstract void elaborateErrorMessage(InternalLogMessage errorMessage);

	protected abstract void elaborateInfoMessage(InternalLogMessage errorMessage);

	protected abstract void elaborateTelemetryUpdate(TelemetryData node, TelemetryMessage<?> telemetry);

	private RpcCommand execMethod(final RpcCommand command)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		final AgentCommandMetadata commandMetadata = registeredCommands.get(command.getCommandId());
		final Method method = flowController.getClass().getMethod(commandMetadata.getMethodName(),
				commandMetadata.getParameterTypes());
		final Object[] parameters = command.getParameterInputValues();
		final Object result = method.invoke(flowController, parameters);
		final String serialize = JsonStream.serialize(result);
		final RpcCommand response = new RpcCommand(command, new JSONObject(serialize));
		return response;
	}

	@Override
	public RpcCommand executeCommand(final RpcCommand command) {
		if (!registeredCommands.containsKey(command.getCommandId())) {
			final String message = "Received command with unknown command ID: " + command.getCommandId();
			final ExecutionCommandException ExecutionCommandException = new ExecutionCommandException(message);
			messageQueue.offer(
					new InternalLogMessage(InternalLogMessage.MessageType.ERROR, message, ExecutionCommandException));
			final RpcCommand response = new RpcCommand(command, ExecutionCommandException);
			return response;
		}
		try {
			return execMethod(command);
		} catch (final Throwable e) {
			final String message = "Exception executing command with ID: " + command.getCommandId() + " - "
					+ e.getMessage();
			final ExecutionCommandException ExecutionCommandException = new ExecutionCommandException(message);
			messageQueue.offer(
					new InternalLogMessage(InternalLogMessage.MessageType.ERROR, message, ExecutionCommandException));
			final RpcCommand response = new RpcCommand(command, ExecutionCommandException);
			return response;
		}

	}

	public List<JSONObject> getAcknowledgeMessages() {
		return acknowledgeMessages;
	}

	protected abstract String getAgentDescription();

	protected abstract String getAgentDisplayName();

	public String getBasePackage() {
		return basePackage;
	}

	@Override
	public Map<String, AgentCommandMetadata> getCommandMetadatas() {
		return registeredCommands;
	}

	@Override
	public Map<String, AgentConfigurationMetadata> getConfigurationMetadatas() {
		return registeredConfigurations;
	}

	@Override
	public Collection<RpcConfiguration> getConfigurations() {
		return configurations.values();
	}

	public long getDataFlowStartedAtMs() {
		return dataFlowStartedAtMs;
	}

	public long getDataFlowStoppedAtMs() {
		return dataFlowStoppedAtMs;
	}

	public Duration getDefinedWaitTimeMs() {
		return definedWaitTimeMs;
	}

	protected abstract String getDigitalTwinModelPath();

	protected abstract String getDigitalTwinModelVersion();

	@Override
	public JSONObject getDtdlJson() {
		final DtdlHandler dtdlHandler = new DtdlHandler();
		final String digitalTwinModelIdentifier = "dtmi:" + getDigitalTwinModelPath() + ";"
				+ getDigitalTwinModelVersion();
		final DigitalTwinModelIdentifier digitalTwinId = DigitalTwinModelIdentifier
				.fromString(digitalTwinModelIdentifier);
		dtdlHandler.setId(digitalTwinId);
		dtdlHandler.setDisplayName(getAgentDisplayName());
		dtdlHandler.setDescription(getAgentDescription());
		for (final AgentConfigurationMetadata configObject : registeredConfigurations.values()) {
			dtdlHandler.addRelationship(configObject.generateDtmlRelationshipObject().toMap());
		}
		for (final AgentProperty property : registeredProperties.values()) {
			dtdlHandler.addProperty(property.generateDtmlPropertyObject().toMap());
		}
		for (final TelemetryData telemetry : registeredTelemetries.values()) {
			dtdlHandler.addTelemetry(telemetry.generateDtmlTelemetryObject().toMap());
		}
		for (final AgentCommandMetadata command : registeredCommands.values()) {
			dtdlHandler.addCommand(command.generateDtmlCommandObject().toMap());
		}
		return dtdlHandler.toDtdlV2Json();
	}

	public AnnotatedAgentController getFlowController() {
		return flowController;
	}

	@Override
	public TelemetryData getIngressTelemetryMetadata(final TelemetryMessage<?> telemetryData) {
		return registeredInputTelemetries.get(telemetryData.getTelemetryDataId());
	}

	@Override
	public Collection<TelemetryData> getIngressTelemetryMetadatas() {
		return registeredInputTelemetries.values();
	}

	@Override
	public TelemetryData getInternalTelemetryMetadata(final TelemetryMessage<?> telemetryData) {
		return registeredInternalTelemetries.get(telemetryData.getTelemetryDataId());
	}

	@Override
	public Collection<TelemetryData> getInternalTelemetryMetadatas() {
		return registeredTelemetries.values();
	}

	public long getMaxWaitExecutionCycleTime() {
		return maxWaitExecutionCycleTime;
	}

	public List<ZenohId> getPeersZid() {
		return peersZid;
	}

	public Map<String, AgentCommandMetadata> getRegisteredCommands() {
		return registeredCommands;
	}

	public Map<String, AgentConfigurationMetadata> getRegisteredConfigurationObjects() {
		return registeredConfigurations;
	}

	public Map<String, AgentProperty> getRegisteredProperties() {
		return registeredProperties;
	}

	public Map<Long, TelemetryData> getRegisteredTelemetries() {
		return registeredTelemetries;
	}

	public List<ZenohId> getRoutersZid() {
		return routersZid;
	}

	public long getSessionCreatedMs() {
		return sessionCreatedMs;
	}

	public ZenohId getSessionZid() {
		return sessionZid;
	}

	@Override
	public TelemetryData getTelemetryMetadata(final TelemetryMessage<?> telemetryData) {
		return registeredTelemetries.get(telemetryData.getTelemetryDataId());
	}

	@Override
	public Collection<TelemetryData> getTelemetryMetadatas() {
		return registeredTelemetries.values();
	}

	public int getThreadPriority() {
		return threadPriority;
	}

	@Override
	public int getVersionApi() {
		return DEFAULT_VERSION_API;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isDataFlowActive() {
		return dataFlowActive;
	}

	public boolean isRegistered() {
		return registered;
	}

	@Override
	public void notifyAcknowledgeMessageReceived(final JSONObject message) {
		registered = true;
		acknowledgeMessages.add(message);
		messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO,
				"Acknowledge command received: " + message.toString(), null));
	}

	@Override
	public void notifyDataFlowStartCommandReceived() {
		dataFlowActive = true;
		flowController.startDataFlow();
		dataFlowStartedAtMs = System.currentTimeMillis();
		messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO,
				"Data flow started command received", null));
	}

	@Override
	public void notifyDataFlowStopCommandReceived() {
		dataFlowActive = false;
		flowController.stopDataFlow();
		dataFlowStoppedAtMs = System.currentTimeMillis();
		messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO,
				"Data flow stopped command received", null));
	}

	@Override
	public void notifyError(final String message, final Throwable exception) {
		messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.ERROR, message, exception));
	}

	@Override
	public void notifyInputTelemetry(final TelemetryMessage<?> telemetry) {
		messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.TELEMETRY, telemetry));
	}

	@Override
	public void notifyZenohSessionCreated(final ZenohId zid, final List<ZenohId> routersZid,
			final List<ZenohId> peersZid) {
		this.sessionCreatedMs = System.currentTimeMillis();
		this.sessionZid = zid;
		this.routersZid = routersZid;
		this.peersZid = peersZid;
		messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO,
				"Zenoh session created with ZID: " + zid + ", routers: " + routersZid + ", peers: " + peersZid, null));
	}

	public void setDefinedWaitTimeMs(final Duration definedWaitTimeMs) {
		this.definedWaitTimeMs = definedWaitTimeMs;
	}

	public void setMaxWaitExecutionCycleTime(final long maxWaitExecutionCycleTime) {
		this.maxWaitExecutionCycleTime = maxWaitExecutionCycleTime;
	}

	public void setThreadPriority(final int threadPriority) {
		this.threadPriority = threadPriority;
	}

	private void startWorkerThread() {
		active = true;
		messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO, "worker thread started", null));
		workerThread = ThreadHelper.ofVirtual().name("queue-worker", 0).unstarted(() -> {
			while (active) {
				try {
					while (!messageQueue.isEmpty()) {
						final InternalLogMessage message = messageQueue.take();
						switch (message.getType()) {
						case INFO:
							elaborateInfoMessage(message);
							break;
						case ERROR:
							elaborateErrorMessage(message);
							break;
						case TELEMETRY:
							elaborateTelemetryUpdate(getTelemetryMetadata(message.getTelemetry()),
									message.getTelemetry());
							break;
						default:
							// non dovrebbe mai capitare
							throw new IllegalStateException("Unexpected value: " + message.getType());
						}
					}
				} catch (final Throwable e) {
					messageQueue.offer(
							new InternalLogMessage(InternalLogMessage.MessageType.ERROR, "Worker thread exception", e));
				}
			}
			messageQueue
					.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO, "worker thread stopped", null));

		});
		workerThread.setPriority(threadPriority);
		workerThread.start();
	}

	private void updateControlParameter(final String key, final Object value) {
		if (!registeredProperties.containsKey(key)) {
			messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.ERROR,
					"Received control parameter update for unknown parameter key: " + key, null));
			return;
		}
		final String fieldName = registeredProperties.get(key).getFieldName();
		try {
			final Field field = flowController.getClass().getDeclaredField(fieldName);
			// field.setAccessible(true);
			field.set(flowController, value);
			messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO,
					"Control parameter updated: " + key + " = " + value + " (field: " + fieldName + ")", null));
			flowController.notifyParameterChanged(key);
		} catch (final NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.ERROR,
					"Error updating control parameter: " + key + " = " + value + " (field: " + fieldName + ")", e));
		}

	}

	@Override
	public void updateControlParameters(final RpcConfiguration configuration) {
		if (configuration.getEpoch() > lastControlParameterEpoch) {
			for (final Entry<String, Object> parameter : configuration.getValues().entrySet()) {
				updateControlParameter(parameter.getKey(), parameter.getValue());
			}
			lastControlParameterEpoch = configuration.getEpoch();
		}
	}

	private void updateOrDeleteConfiguration(final RpcConfiguration configuration) {
		if (configuration.isDeleteMessage()) {
			configurations.remove(configuration.getUniqueId());
			flowController.notifyConfigurationChanged(configuration.getConfigurationId(),
					ConfigurationChangeType.OBJECT_DELETED);
		} else {
			if (configurations.get(configuration.getUniqueId()).equals(configuration)) {
				if (configuration.getEpoch() > configurations.get(configuration.getUniqueId()).getEpoch()) {
					configurations.put(configuration.getUniqueId(), configuration);
					flowController.notifyConfigurationChanged(configuration.getConfigurationId(),
							ConfigurationChangeType.OBJECT_UPDATED);
				} else if (configuration.getEpoch() == configurations.get(configuration.getUniqueId()).getEpoch()) {
					messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO,
							"Ignoring configuration update for " + configuration.getConfigurationId()
									+ " with same epoch: " + configuration.getEpoch(),
							null));
				} else {
					messageQueue.offer(new InternalLogMessage(InternalLogMessage.MessageType.INFO,
							"Ignoring configuration update for " + configuration.getConfigurationId()
									+ " with older or same epoch: " + configuration.getEpoch(),
							null));
				}
			}
		}
	}
}
