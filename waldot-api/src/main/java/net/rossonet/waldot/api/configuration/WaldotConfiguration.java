package net.rossonet.waldot.api.configuration;

import java.io.Serializable;
import java.util.Map;

public interface WaldotConfiguration extends Serializable, Map<String, String> {

	boolean getAnonymousAccessAllowed();

	String getAssetRootNodeBrowseName();

	String getAssetRootNodeDisplayName();

	String getAssetRootNodeId();

	String getFactoryPassword();

	String getFactoryUsername();

	String getInterfaceRootNodeBrowseName();

	String getInterfaceRootNodeDisplayName();

	String getInterfaceRootNodeId();

	String getRootNodeBrowseName();

	String getRootNodeDisplayName();

	String getRootNodeId();

}
