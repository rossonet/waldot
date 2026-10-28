package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.history;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadResult;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateDetails;
import org.eclipse.milo.opcua.stack.core.types.structured.HistoryUpdateResult;
import org.junit.jupiter.api.Test;

/**
 * {@link LoggerHistoryStrategy} only logs: history read and update must answer
 * Bad_HistoryOperationUnsupported for every requested item.
 */
class LoggerHistoryStrategyTest {

	private final LoggerHistoryStrategy strategy = new LoggerHistoryStrategy();

	@Test
	void historyReadIsUnsupportedForEveryItem() {
		final HistoryReadValueId id = new HistoryReadValueId(new NodeId(2, "v"), null, QualifiedName.NULL_VALUE,
				null);
		final List<HistoryReadResult> results = strategy.historyRead(null, null, TimestampsToReturn.Both,
				List.of(id, id, id));
		assertEquals(3, results.size());
		results.forEach(result -> assertEquals(StatusCodes.Bad_HistoryOperationUnsupported,
				result.getStatusCode().getValue()));
		assertTrue(strategy.historyRead(null, null, TimestampsToReturn.Both, List.of()).isEmpty());
	}

	@Test
	void historyUpdateIsUnsupportedForEveryItem() {
		final List<HistoryUpdateResult> results = strategy.historyUpdate(null,
				java.util.Arrays.asList((HistoryUpdateDetails) null, (HistoryUpdateDetails) null));
		assertEquals(2, results.size());
		results.forEach(result -> assertEquals(StatusCodes.Bad_HistoryOperationUnsupported,
				result.getStatusCode().getValue()));
	}

	@Test
	void notificationsOnlyLog() {
		assertDoesNotThrow(() -> {
			strategy.setNamespace(null);
			strategy.onDataItemsCreated(List.of());
			strategy.onDataItemsModified(List.of());
			strategy.onDataItemsDeleted(List.of());
			strategy.registerHistoryRecord("context", null, "temperature", new DataValue(new Variant(21.5)));
			strategy.close();
		});
	}
}
