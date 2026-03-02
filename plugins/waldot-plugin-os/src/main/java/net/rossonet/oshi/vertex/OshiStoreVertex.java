package net.rossonet.oshi.vertex;

import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotVertex;

public interface OshiStoreVertex {

	void notifyAddFunctionalEdge(WaldotEdge edge, WaldotVertex sourceVertex, WaldotVertex targetVertex, String label,
			String type, Object[] propertyKeyValues);

	void notifyRemoveFunctionalEdge(WaldotEdge edge);
}
