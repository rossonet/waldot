package net.rossonet.waldot.plugin;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.exception.PluginBlockingException;

public interface EdgeMonitoredVertex {

	default void notifyAddSourceMonitoredEdge(WaldotEdge edge,
			WaldotVertex sourceVertex, String label, String type,
			Object[] propertyKeyValues) throws PluginBlockingException {
	};

	default void notifyAddTargetMonitoredEdge(WaldotEdge edge,
			WaldotVertex targetVertex, String label, String type,
			Object[] propertyKeyValues) throws PluginBlockingException {
	};

	default void notifyRemoveSourceMonitoredEdge(WaldotEdge edge) {
	};

	default void notifyRemoveTargetMonitoredEdge(WaldotEdge edge) {
	};

}
