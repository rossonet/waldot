package net.rossonet.zenoh.annotation;

public interface AnnotatedAgentController {

	public enum ConfigurationChangeType {
		OBJECT_CREATED, OBJECT_DELETED, PARAMETER_CHANGED
	}

	void notifyObjectConfigurationChanged(String objectName, ConfigurationChangeType changeType);

	void notifyParameterChanged(String parameterName);

	void startDataFlow();

	void stopDataFlow();

}
