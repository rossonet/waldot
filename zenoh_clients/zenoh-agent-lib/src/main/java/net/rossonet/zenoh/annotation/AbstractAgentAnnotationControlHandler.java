package net.rossonet.zenoh.annotation;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.google.common.reflect.ClassPath;

import io.zenoh.config.ZenohId;
import net.rossonet.waldot.dtdl.DigitalTwinModelIdentifier;
import net.rossonet.waldot.dtdl.DtdlHandler;
import net.rossonet.waldot.utils.ThreadHelper;
import net.rossonet.zenoh.api.AgentCommand;
import net.rossonet.zenoh.api.AgentCommandParameter;
import net.rossonet.zenoh.api.AgentConfigurationObject;
import net.rossonet.zenoh.api.AgentControlHandler;
import net.rossonet.zenoh.api.AgentProperty;
import net.rossonet.zenoh.api.TelemetryData;
import net.rossonet.zenoh.api.message.TelemetryMessage;

public abstract class AbstractAgentAnnotationControlHandler implements AgentControlHandler {

	private final class ExportedCommandData {
		private final ExportedCommand exportedCommand;
		public final Method method;
		public final String name;

		public ExportedCommandData(String name, Method method, ExportedCommand exportedCommand) {
			this.name = name;
			this.method = method;
			this.exportedCommand = exportedCommand;
		}

		public ExportedCommand getExportedCommand() {
			return exportedCommand;
		}

		public Method getMethod() {
			return method;
		}

		public String getName() {
			return name;
		}

	}

	private final class ExportedParameterData {
		private final ExportedParameter exportedParameter;
		public final Field field;
		public final String name;

		public ExportedParameterData(String name, Field field, ExportedParameter exportedParameter) {
			this.name = name;
			this.field = field;
			this.exportedParameter = exportedParameter;
		}

		public ExportedParameter getExportedParameter() {
			return exportedParameter;
		}

		public Field getField() {
			return field;
		}

		public String getName() {
			return name;
		}
	}

	public static final class InternalLogMessage {
		public final Throwable exception;
		public final String message;

		public InternalLogMessage(String message, Throwable exception) {
			this.message = message;
			this.exception = exception;
		}

		public Throwable getException() {
			return exception;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "InternalLogMessage [message=" + message + ", exception=" + exception + "]";
		}
	}

	private JSONObject acknowledgeMessage;

	private boolean active;

	private final String basePackage;

	private final Map<String, Object> configurations = new HashMap<>();

	private boolean dataFlowActive = false;

	private long dataFlowStartedAtMs;

	private long dataFlowStoppedAtMs;

	private Duration definedWaitTimeMs;

	private final ConcurrentLinkedQueue<InternalLogMessage> errorQueue = new ConcurrentLinkedQueue<>();

	private AnnotatedAgentController flowController;

	private final ConcurrentLinkedQueue<InternalLogMessage> infoQueue = new ConcurrentLinkedQueue<>();

	private long maxWaitExecutionCycleTime;

	private List<ZenohId> peersZid;

	private boolean registered;

	private final Map<String, AgentCommand> registeredCommands = new HashMap<>();

	private final Map<String, AgentConfigurationObject> registeredConfigurationObjects = new HashMap<>();

	private final Map<String, AgentProperty> registeredProperties = new HashMap<>();

	private final Map<String, TelemetryData> registeredTelemetries = new HashMap<>();

	private List<ZenohId> routersZid;

	private long sessionCreatedMs;

	private ZenohId sessionZid;

	private final ConcurrentLinkedQueue<TelemetryMessage<?>> telemetryQueue = new ConcurrentLinkedQueue<>();

	private int threadPriority;

	private Thread workerThread;

	public AbstractAgentAnnotationControlHandler(String basePackage) {
		this(basePackage, Thread.NORM_PRIORITY, Duration.ofMillis(20), Duration.ofSeconds(10));
	}

	public AbstractAgentAnnotationControlHandler(String basePackage, int threadPriority, Duration definedWaitTimeMs,
			Duration maxWaitExecutionCycleTime) {
		this.threadPriority = threadPriority;
		this.basePackage = basePackage;
		this.maxWaitExecutionCycleTime = maxWaitExecutionCycleTime.toMillis();
		this.definedWaitTimeMs = definedWaitTimeMs;
		try {
			elaborateAnnotations();
		} catch (final IOException e) {
			errorQueue.offer(new InternalLogMessage("Error elaborating annotations", e));
		}
		startWorkerThread();
		infoQueue.offer(new InternalLogMessage("AgentControlHandler started", null));
	}

	private void addCommandMetadata(ExportedCommandData commandData) {
		final String commandName = commandData.getName();
		final String methodName = commandData.getMethod().getName();
		final ExportedCommand annotation = commandData.getExportedCommand();
		final Set<AgentCommandParameter> commandParameters = new HashSet<>();
		for (final Parameter methodParameter : commandData.getMethod().getParameters()) {
			if (methodParameter.isAnnotationPresent(ExportedMethodParameter.class)) {
				final ExportedMethodParameter methodParamAnnotation = methodParameter
						.getAnnotation(ExportedMethodParameter.class);
				String parameterName = methodParamAnnotation.name();
				if (parameterName.isEmpty()) {
					parameterName = methodParameter.getName();
				}
				commandParameters.add(new AgentCommandParameter(parameterName, methodParameter, methodParamAnnotation));
			} else {
				errorQueue.offer(new InternalLogMessage("Method parameter " + methodParameter.getName() + " of command "
						+ commandName + " is missing ExportedMethodParameter annotation", null));
			}
		}
		final String methodRetunType = commandData.getMethod().getReturnType().getSimpleName();
		registeredCommands.put(commandName, new AgentCommand(commandName, flowController, methodName, methodRetunType,
				annotation, commandParameters));
	}

	private void addConfigurationObjectMetadata(Class<?> configClass,
			Collection<ExportedParameterData> annotatedFields) {
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
		registeredConfigurationObjects.put(configurationName, new AgentConfigurationObject(configurationName,
				configurationClassName, description, properties, unique));

	}

	@Override
	public void addConfigurationObjects(String name, JSONObject configurationObject) {
		configurations.put(name, generateConfigurationObjectFromJson(name, configurationObject));
	}

	private void addPropertyMetadata(ExportedParameterData parameterData) {
		final String fieldName = parameterData.getField().getName();
		final String propertyName = parameterData.getName();
		final ExportedParameter annotation = parameterData.getExportedParameter();
		registeredProperties.put(parameterData.getName(),
				new AgentProperty(propertyName, flowController, fieldName, annotation));
	}

	@Override
	public void close() throws Exception {
		active = false;
		flowController.stopDataFlow();
		shutdown();
	}

	@Override
	public void delConfigurationObjects(String name) {
		configurations.remove(name);
	}

	private void elaborateAnnotations() throws IOException {
		final ClassPath cp = ClassPath.from(Thread.currentThread().getContextClassLoader());
		final Set<Class<?>> configurationObjects = new HashSet<>();
		for (final ClassPath.ClassInfo classInfo : cp.getTopLevelClassesRecursive(basePackage)) {
			final Class<?> clazz = classInfo.load();
			if (clazz.isAnnotationPresent(ExportedController.class)) {
				infoQueue.offer(
						new InternalLogMessage("Found AnnotatedAgentController class: " + clazz.getName(), null));
				if (flowController != null) {
					errorQueue.offer(new InternalLogMessage(
							"Multiple AnnotatedAgentController classes found. Only one is allowed. Class "
									+ clazz.getName() + " will be ignored.",
							null));
				}
				try {
					flowController = (AnnotatedAgentController) clazz.getConstructor().newInstance();
				} catch (final Exception e) {
					errorQueue.offer(new InternalLogMessage(
							"Error instantiating AnnotatedAgentController from class " + clazz.getName(), e));
				}

			}
			if (clazz.isAnnotationPresent(ExportedObject.class)) {
				configurationObjects.add(clazz);
				infoQueue.offer(
						new InternalLogMessage("Found AgentConfigurationObject class: " + clazz.getName(), null));
			}
		}
		if (flowController == null) {
			errorQueue.offer(new InternalLogMessage("No AnnotatedAgentController class found.", null));
		} else {
			elaborateControllerAnnotations();
		}
		if (configurationObjects.isEmpty()) {
			errorQueue.offer(new InternalLogMessage("No AgentConfigurationObject classes found.", null));
		} else {
			for (final Class<?> configClass : configurationObjects) {
				elaborateConfigurationObjectAnnotations(configClass);
			}
		}
	}

	private void elaborateConfigurationObjectAnnotations(Class<?> configClass) {
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

	protected abstract void elaborateTelemetryUpdate(TelemetryMessage<?> telemetry);

	@Override
	public void executeCommand(AgentCommand agentCommand, JSONObject message) {
		// TODO: eseguire il comando su flowController
	}

	private Object generateConfigurationObjectFromJson(String name, JSONObject configurationObject) {
		// TODO Auto-generated method stub
		return null;
	}

	public JSONObject getAcknowledgeMessage() {
		return acknowledgeMessage;
	}

	protected abstract String getAgentDescription();

	protected abstract String getAgentDisplayName();

	public String getBasePackage() {
		return basePackage;
	}

	@Override
	public Map<String, AgentCommand> getCommandMetadatas() {
		return registeredCommands;
	}

	@Override
	public Map<String, AgentConfigurationObject> getConfigurationObjectMetadatas() {
		return registeredConfigurationObjects;
	}

	@Override
	public Map<String, Object> getConfigurationObjects() {
		return configurations;
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
		for (final AgentConfigurationObject configObject : registeredConfigurationObjects.values()) {
			dtdlHandler.addRelationship(configObject.generateDtmlRelationshipObject().toMap());
		}
		for (final AgentProperty property : registeredProperties.values()) {
			dtdlHandler.addProperty(property.generateDtmlPropertyObject().toMap());
		}
		for (final TelemetryData telemetry : registeredTelemetries.values()) {
			dtdlHandler.addTelemetry(telemetry.generateDtmlTelemetryObject().toMap());
		}
		for (final AgentCommand command : registeredCommands.values()) {
			dtdlHandler.addCommand(command.generateDtmlCommandObject().toMap());
		}
		return dtdlHandler.toDtdlV2Json();
	}

	public AnnotatedAgentController getFlowController() {
		return flowController;
	}

	public long getMaxWaitExecutionCycleTime() {
		return maxWaitExecutionCycleTime;
	}

	public List<ZenohId> getPeersZid() {
		return peersZid;
	}

	public Map<String, AgentCommand> getRegisteredCommands() {
		return registeredCommands;
	}

	public Map<String, AgentConfigurationObject> getRegisteredConfigurationObjects() {
		return registeredConfigurationObjects;
	}

	public Map<String, AgentProperty> getRegisteredProperties() {
		return registeredProperties;
	}

	public Map<String, TelemetryData> getRegisteredTelemetries() {
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

	public int getThreadPriority() {
		return threadPriority;
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
	public void notifyAcknowledgeCommandReceived(JSONObject message) {
		registered = true;
		acknowledgeMessage = message;
		infoQueue.offer(new InternalLogMessage("Acknowledge command received: " + message.toString(), null));
	}

	@Override
	public void notifyDataFlowStartCommandReceived(JSONObject message) {
		dataFlowActive = true;
		flowController.startDataFlow();
		dataFlowStartedAtMs = System.currentTimeMillis();
		infoQueue.offer(new InternalLogMessage("Data flow started command received: " + message.toString(), null));
	}

	@Override
	public void notifyDataFlowStopCommandReceived(JSONObject message) {
		dataFlowActive = false;
		flowController.stopDataFlow();
		dataFlowStoppedAtMs = System.currentTimeMillis();
		infoQueue.offer(new InternalLogMessage("Data flow stopped command received: " + message.toString(), null));
	}

	@Override
	public void notifyError(String message, Throwable exception) {
		errorQueue.offer(new InternalLogMessage(message, exception));
	}

	@Override
	public void notifyTelemetry(TelemetryMessage<?> telemetry) {
		telemetryQueue.offer(telemetry);
	}

	@Override
	public void notifyZenohSessionCreated(ZenohId zid, List<ZenohId> routersZid, List<ZenohId> peersZid) {
		this.sessionCreatedMs = System.currentTimeMillis();
		this.sessionZid = zid;
		this.routersZid = routersZid;
		this.peersZid = peersZid;
		infoQueue.offer(new InternalLogMessage(
				"Zenoh session created with ZID: " + zid + ", routers: " + routersZid + ", peers: " + peersZid, null));
	}

	public void setDefinedWaitTimeMs(Duration definedWaitTimeMs) {
		this.definedWaitTimeMs = definedWaitTimeMs;
	}

	public void setMaxWaitExecutionCycleTime(long maxWaitExecutionCycleTime) {
		this.maxWaitExecutionCycleTime = maxWaitExecutionCycleTime;
	}

	public void setThreadPriority(int threadPriority) {
		this.threadPriority = threadPriority;
	}

	protected abstract void shutdown();

	private void startWorkerThread() {
		active = true;
		infoQueue.offer(new InternalLogMessage("worker thread started", null));
		workerThread = new Thread(() -> {
			while (active) {
				try {
					ThreadHelper.runWithTimeout(() -> {
						while (!errorQueue.isEmpty()) {
							final InternalLogMessage errorMessage = errorQueue.poll();
							try {
								elaborateErrorMessage(errorMessage);
							} catch (final Throwable t) {
								System.err.println("Error elaborating error message: " + errorMessage.toString());
								t.printStackTrace();
							}
						}
						while (!telemetryQueue.isEmpty()) {
							final TelemetryMessage<?> telemetry = telemetryQueue.poll();
							try {
								elaborateTelemetryUpdate(telemetry);
							} catch (final Throwable t) {
								errorQueue.offer(new InternalLogMessage(
										"Error elaborating telemetry update: " + telemetry.toString(), t));
							}
						}
						while (!infoQueue.isEmpty()) {
							try {
								final InternalLogMessage infoMessage = infoQueue.poll();
								elaborateInfoMessage(infoMessage);
							} catch (final Throwable t) {
								errorQueue.offer(new InternalLogMessage("Error elaborating info message", t));
							}
						}
						try {
							Thread.sleep(definedWaitTimeMs);
						} catch (final InterruptedException e) {
							errorQueue.offer(new InternalLogMessage("Worker thread interrupted", e));
						}
					}, maxWaitExecutionCycleTime, TimeUnit.MILLISECONDS);
				} catch (final Exception e) {
					errorQueue.offer(new InternalLogMessage("Worker thread timeout", e));
				}
			}
			infoQueue.offer(new InternalLogMessage("worker thread stopped", null));
		}, "queue-thread");
		workerThread.setPriority(threadPriority);
		workerThread.start();
	}

	@Override
	public void updateConfigurationObjects(String name, JSONObject configurationObject) {
		// TODO aggiorna la configurazione partendo dal JSON

	}

}
